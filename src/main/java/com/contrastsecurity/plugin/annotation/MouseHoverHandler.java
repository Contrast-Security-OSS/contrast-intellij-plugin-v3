/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
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
import com.intellij.openapi.project.Project;
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
  private Map<Integer, AnnotationPopupDTO> popupDTOMap;
  private EditorMouseEvent currentEvent;
  private Timer popupTimer;
  private SwingWorker<Void, Void> worker;
  private final AnnotationPopup annotationPopup;
  private static final int POPUP_DELAY_MS = 2000;

  private final Project project;

  public MouseHoverHandler(Map<Integer, AnnotationPopupDTO> popupDTOMap, Project project) {
    this.project = project;
    this.popupDTOMap = popupDTOMap;
    this.annotationPopup = new AnnotationPopup();
    this.popupTimer = new Timer(POPUP_DELAY_MS, e -> showPopupForEvent(currentEvent));
    popupTimer.setRepeats(false);
  }

  /** Adds listeners to all editors including detached windows */
  public void addMouseHoverListener() {
    EditorFactory factory = EditorFactory.getInstance();

    // 1. Attach to all currently open editors
    for (Editor editor : factory.getAllEditors()) {
      if (editor.getProject() == project) {
        addHoverListenersToEditor(editor);
      }
    }

    // 2. Attach to all future editors
    factory.addEditorFactoryListener(
        new EditorFactoryListener() {
          @Override
          public void editorCreated(@NotNull EditorFactoryEvent event) {
            Editor editor = event.getEditor();
            if (editor.getProject() == project) {
              addHoverListenersToEditor(editor);
            }
          }

          @Override
          public void editorReleased(@NotNull EditorFactoryEvent event) {
            // Optional: clean up if needed
          }
        },
        project // ensures listener is removed when project is disposed
        );
  }

  private void addHoverListenersToEditor(Editor editor) {
    editor.addEditorMouseMotionListener(
        new EditorMouseMotionListener() {
          @Override
          public void mouseMoved(@NotNull EditorMouseEvent e) {
            int offset = e.getOffset();
            int line = editor.getDocument().getLineNumber(offset);

            if (popupDTOMap.containsKey(line)) {
              currentEvent = e;
              if (!isPopupShown) {
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
    if (popupDTOMap.isEmpty()) return;

    int offset = e.getOffset();
    Editor editor = e.getEditor();
    int line = editor.getDocument().getLineNumber(offset);

    AnnotationPopupDTO dto = popupDTOMap.get(line);
    if (dto != null) {
      if (StringUtils.isNotEmpty(dto.getAdvice())) {
        invokePopup(dto, e);
      } else {
        loadScanAdvice(dto, e);
      }
    }
  }

  private void hidePopupIfShown() {
    if (isPopupShown && !isMouseOverPopup) {
      annotationPopup.hidePopUp();
      isPopupShown = false;
      popupTimer.stop();
    }
  }

  private void loadScanAdvice(AnnotationPopupDTO dto, EditorMouseEvent e) {
    SubMenuCacheService subMenuCacheService = new SubMenuCacheService();
    Object cache = subMenuCacheService.get(dto.getProjectId() + "-" + dto.getVulnerabilityId());
    if (cache instanceof Vulnerability vulnerability) {
      String risk = vulnerability.getRisk();
      dto.setAdvice(StringUtils.defaultIfEmpty(risk, "No Advice found"));
      invokePopup(dto, e);
    } else {
      makeAPICall(dto.getProjectId(), dto.getVulnerabilityId(), dto, e);
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
    int offset = e.getOffset();
    Editor editor = e.getEditor();
    int line = editor.getDocument().getLineNumber(offset);
    return popupDTOMap.containsKey(line);
  }

  private void makeAPICall(
      String projectID, String vulnerabilityID, AnnotationPopupDTO dto, EditorMouseEvent e) {
    if (worker != null) return;

    ConfigurationDTO savedConfigDataByID =
        CredentialDetailsService.getInstance().getSavedConfigDataByID(projectID);
    if (savedConfigDataByID != null) {
      savedConfigDataByID = CredentialUtil.decryptDTO(savedConfigDataByID);
      Fetcher fetcher =
          new Fetcher(
              savedConfigDataByID.getUserName(),
              savedConfigDataByID.getContrastURL(),
              savedConfigDataByID.getOrgId(),
              savedConfigDataByID.getApiKey(),
              savedConfigDataByID.getServiceKey());

      worker =
          new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
              Vulnerability vulnerability =
                  fetcher.getProjectVulnerabilityById(projectID, vulnerabilityID);
              if (vulnerability != null) {
                SubMenuCacheService subMenuCacheService = new SubMenuCacheService();
                subMenuCacheService.add(projectID + "-" + vulnerabilityID, vulnerability);
                dto.setAdvice(vulnerability.getRisk());
                invokePopup(dto, e);
              }
              return null;
            }
          };
      worker.execute();
    } else {
      log.error("No saved credentials found for project: {}", projectID);
    }
  }
}
