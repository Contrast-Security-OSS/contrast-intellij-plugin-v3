/*******************************************************************************
 * Copyright © 2026 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.annotation;

import com.contrastsecurity.plugin.fetchers.Fetcher;
import com.contrastsecurity.plugin.models.AnnotationPopupDTO;
import com.contrastsecurity.plugin.models.ConfigurationDTO;
import com.contrastsecurity.plugin.persistent.CredentialDetailsService;
import com.contrastsecurity.plugin.service.SubMenuCacheService;
import com.contrastsecurity.plugin.utility.CredentialUtil;
import com.contrastsecurity.scan.dto.Vulnerability;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class MouseHoverHandler {

  private boolean isPopupShown;
  private boolean isMouseOverPopup;
  private final Map<VirtualFile, Map<Integer, AnnotationPopupDTO>> popupDTOMap;
  private EditorMouseEvent currentEvent;
  private Editor currentEditor;
  private final Timer popupTimer;
  private SwingWorker<Void, Void> worker;
  private final AnnotationPopup annotationPopup;
  private static final int POPUP_DELAY_MS = 600;
  private final Project project;

  public MouseHoverHandler(Map<VirtualFile, Map<Integer, AnnotationPopupDTO>> popupDTOMap, Project project) {
    this.project = project;
    this.popupDTOMap = popupDTOMap;
    this.annotationPopup = new AnnotationPopup();
    this.popupTimer = new Timer(POPUP_DELAY_MS, e -> showPopupForEvent(currentEvent));
    this.popupTimer.setRepeats(false);
  }

  /** Adds hover listeners to all current and future editors */
  public void addMouseHoverListener() {
    EditorFactory factory = EditorFactory.getInstance();
    for (Editor editor : factory.getAllEditors()) {
      if (editor.getProject() == project) {
        addHoverListenersToEditor(editor);
      }
    }
    factory.addEditorFactoryListener(
            new EditorFactoryListener() {
              @Override
              public void editorCreated(@NotNull EditorFactoryEvent event) {
                Editor editor = event.getEditor();
                if (editor.getProject() == project) {
                  addHoverListenersToEditor(editor);
                }
              }
            },
            project);
  }

  private void addHoverListenersToEditor(Editor editor) {
    editor.addEditorMouseMotionListener(
            new EditorMouseMotionListener() {
              @Override
              public void mouseMoved(@NotNull EditorMouseEvent e) {
                VirtualFile file =
                        FileDocumentManager.getInstance().getFile(editor.getDocument());
                if (file == null) {
                  hidePopupIfShown();
                  return;
                }
                Map<Integer, AnnotationPopupDTO> fileMap = popupDTOMap.get(file);
                if (fileMap == null || fileMap.isEmpty()) {
                  hidePopupIfShown();
                  return;
                }
                int line = editor.xyToLogicalPosition(e.getMouseEvent().getPoint()).line;
                if (fileMap.containsKey(line)) {
                  currentEvent = e;
                  currentEditor = editor;
                  if (!popupTimer.isRunning() && !isPopupShown) {
                    popupTimer.restart();
                  }
                } else {
                  hidePopupIfShown();
                  popupTimer.stop();
                }
              }
            },
            project);
    editor.addEditorMouseListener(
            new EditorMouseListener() {
              @Override
              public void mouseClicked(@NotNull EditorMouseEvent e) {
                hidePopupIfShown();
                popupTimer.stop();
              }
            },
            project);
  }

  private void showPopupForEvent(EditorMouseEvent e) {
    if (e == null || currentEditor == null) return;
    Editor editor = e.getEditor();
    VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    VirtualFile expectedFile = FileDocumentManager.getInstance().getFile(currentEditor.getDocument());
    if (file == null || expectedFile == null || !file.equals(expectedFile)) {
      return;
    }
    Map<Integer, AnnotationPopupDTO> fileMap = popupDTOMap.get(file);
    if (fileMap == null || fileMap.isEmpty()) return;
    int line = editor.xyToLogicalPosition(e.getMouseEvent().getPoint()).line;
    AnnotationPopupDTO dto = fileMap.get(line);
    if (dto == null) return;
    if (StringUtils.isNotEmpty(dto.getAdvice())) {
      invokePopup(dto, e);
    } else {
      loadScanAdvice(dto, e);
    }
  }

  private void hidePopupIfShown() {
    if (isPopupShown && !isMouseOverPopup) {
      annotationPopup.hidePopUp();
      isPopupShown = false;
      popupTimer.stop();
    }
  }

  private void invokePopup(AnnotationPopupDTO dto, EditorMouseEvent e) {
    annotationPopup.hidePopUp();
    annotationPopup.showAnnotationPopup(
            dto,
            e.getMouseEvent(),
            new MouseAdapter() {
              @Override
              public void mouseEntered(MouseEvent e) {
                isMouseOverPopup = true;
              }

              @Override
              public void mouseExited(MouseEvent e) {
                isMouseOverPopup = false;
                if (!isMouseOverLine(currentEvent)) {
                  hidePopupIfShown();
                }
              }
            });
    isPopupShown = true;
  }

  private boolean isMouseOverLine(EditorMouseEvent e) {
    if (e == null) return false;
    Editor editor = e.getEditor();
    VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (file == null) return false;
    Map<Integer, AnnotationPopupDTO> fileMap = popupDTOMap.get(file);
    if (fileMap == null) return false;
    int line = editor.xyToLogicalPosition(e.getMouseEvent().getPoint()).line;
    return fileMap.containsKey(line);
  }

  private void loadScanAdvice(AnnotationPopupDTO dto, EditorMouseEvent e) {
    SubMenuCacheService subMenuCacheService = new SubMenuCacheService();
    Object cache = subMenuCacheService.get(dto.getProjectId() + "-" + dto.getVulnerabilityId());
    if (cache instanceof Vulnerability vulnerability) {
      dto.setAdvice(StringUtils.defaultIfEmpty(vulnerability.getRisk(), "No Advice found"));
      invokePopup(dto, e);
    } else {
      makeAPICall(dto.getProjectId(), dto.getVulnerabilityId(), dto, e);
    }
  }

  private void makeAPICall(
          String projectID,
          String vulnerabilityID,
          AnnotationPopupDTO dto,
          EditorMouseEvent e) {
    if (worker != null) return;
    ConfigurationDTO savedConfigDataByID = CredentialDetailsService.getInstance().getSavedConfigDataByID(projectID);
    if (savedConfigDataByID == null) {
      log.error("No saved credentials found for project: {}", projectID);
      return;
    }
    savedConfigDataByID = CredentialUtil.decryptDTO(savedConfigDataByID);
    Fetcher fetcher = new Fetcher(
            savedConfigDataByID.getUserName(),
            savedConfigDataByID.getContrastURL(),
            savedConfigDataByID.getOrgId(),
            savedConfigDataByID.getApiKey(),
            savedConfigDataByID.getServiceKey()
    );
    worker = new SwingWorker<>() {
      @Override
      protected Void doInBackground() throws Exception {
        Vulnerability vulnerability = fetcher.getProjectVulnerabilityById(projectID, vulnerabilityID);
        if (vulnerability != null) {
          new SubMenuCacheService().add(projectID + "-" + vulnerabilityID, vulnerability);
          dto.setAdvice(vulnerability.getRisk());
          invokePopup(dto, e);
        }
        return null;
      }
    };
    worker.execute();
  }
}
