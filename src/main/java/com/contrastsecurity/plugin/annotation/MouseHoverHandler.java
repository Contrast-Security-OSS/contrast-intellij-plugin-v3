/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
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
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
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

  private static final int POPUP_DELAY_MS = 2000; // 2 second delay

  private Project project;

  public MouseHoverHandler(Map<Integer, AnnotationPopupDTO> popupDTOMap, Project project) {
    this.project = project;
    this.popupDTOMap = popupDTOMap;
    annotationPopup = new AnnotationPopup();
    popupTimer =
        new Timer(
            POPUP_DELAY_MS, e -> showPopupForEvent(currentEvent)); // Show popup after the delay
    popupTimer.setRepeats(false); // Timer should not repeat
  }

  /** Adds Annotation popup messages for the provided Editor object */
  public void addMouseHoverListener() {

    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

    if (editor != null) {
      editor.addEditorMouseMotionListener(
          new EditorMouseMotionListener() {
            @Override
            public void mouseMoved(@NotNull EditorMouseEvent e) {
              int offset = e.getOffset();
              int line = editor.getDocument().getLineNumber(offset);

              // Check if there's a popup for this line
              if (popupDTOMap.containsKey(line)) {
                currentEvent = e;

                // Start the popup timer if the popup is not already shown
                if (!isPopupShown) {
                  popupTimer.start();
                }
              } else {
                // Hide popup if the mouse is neither on the vulnerable line nor inside the popup
                hidePopupIfShown();
                popupTimer.stop(); // Stop the timer if moving away
              }
            }
          });

      editor.addEditorMouseListener(
          new EditorMouseListener() {
            @Override
            public void mouseClicked(@NotNull EditorMouseEvent e) {
              // Hide the popup if clicked outside of it
              hidePopupIfShown();
              popupTimer.stop(); // Stop the timer on mouse click
            }
          });
    }
  }

  private void showPopupForEvent(EditorMouseEvent e) {
    if (popupDTOMap.isEmpty()) {
      return;
    }
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
    } else {
      log.warn("No popup found");
    }
  }

  private boolean isMouseOverLine(EditorMouseEvent e) {
    int offset = e.getOffset();
    Editor editor = e.getEditor();
    int line = editor.getDocument().getLineNumber(offset);
    return popupDTOMap.containsKey(line);
  }

  private void hidePopupIfShown() {
    if (isPopupShown && !isMouseOverPopup) {
      annotationPopup.hidePopUp();
      isPopupShown = false;
      popupTimer.stop(); // Stop the timer when hiding the popup
    }
  }

  private void loadScanAdvice(AnnotationPopupDTO dto, EditorMouseEvent e) {
    // Check in cache
    SubMenuCacheService subMenuCacheService = new SubMenuCacheService();
    Object cache = subMenuCacheService.get(dto.getProjectId() + "-" + dto.getVulnerabilityId());
    if (cache != null) {
      Vulnerability vulnerability = (Vulnerability) cache;
      String risk = vulnerability.getRisk();
      if (StringUtils.isNotEmpty(risk)) {
        dto.setAdvice(risk);
      } else {
        dto.setAdvice("No Advice found");
      }
      invokePopup(dto, e);
    } else {
      // Make API call
      makeAPICall(dto.getProjectId(), dto.getVulnerabilityId(), dto, e);
    }
  }

  private void invokePopup(AnnotationPopupDTO dto, EditorMouseEvent e) {
    annotationPopup.hidePopUp(); // Hide any existing popup
    annotationPopup.showAnnotationPopup(
        dto,
        e.getMouseEvent(),
        new MouseAdapter() {
          @Override
          public void mouseEntered(MouseEvent e) {
            // Mouse entered the popup, keep the popup visible
            isMouseOverPopup = true;
          }

          @Override
          public void mouseExited(MouseEvent e) {
            // Mouse exited the popup, hide it only if the mouse is not on the line
            isMouseOverPopup = false;
            if (!isMouseOverLine(currentEvent)) {
              hidePopupIfShown();
            }
          }
        });
    isPopupShown = true;
  }

  private void makeAPICall(
      String projectID, String vulnerabilityID, AnnotationPopupDTO dto, EditorMouseEvent e) {
    if (worker != null) {
      return;
    }
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
          new SwingWorker<Void, Void>() {
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
      log.error("No saved creds found");
    }
  }
}
