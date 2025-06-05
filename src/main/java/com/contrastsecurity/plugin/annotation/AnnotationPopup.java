/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.annotation;

import static com.contrastsecurity.plugin.constants.Constants.ANNOTATION_POPUP.ACTION_FORMAT;
import static com.contrastsecurity.plugin.constants.Constants.ANNOTATION_POPUP.ADVICE_HTML_FORMAT;
import static com.contrastsecurity.plugin.constants.Constants.ANNOTATION_POPUP.CLOSE_HTML_FORMAT;
import static com.contrastsecurity.plugin.constants.Constants.ANNOTATION_POPUP.EMPTY_SPACES;
import static com.contrastsecurity.plugin.constants.Constants.ANNOTATION_POPUP.LAST_DETECTED_FORMAT;
import static com.contrastsecurity.plugin.constants.Constants.ANNOTATION_POPUP.OPEN_HTML_FORMAT;
import static com.contrastsecurity.plugin.constants.Constants.ANNOTATION_POPUP.OR_FORMAT;
import static com.contrastsecurity.plugin.constants.Constants.ANNOTATION_POPUP.SQUARE_BRACKET_CLOSE;
import static com.contrastsecurity.plugin.constants.Constants.ANNOTATION_POPUP.SQUARE_BRACKET_OPEN;
import static com.contrastsecurity.plugin.constants.Constants.ANNOTATION_POPUP.STATUS_FORMAT;
import static com.contrastsecurity.plugin.constants.Constants.ONE_SPACE;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.contrastsecurity.plugin.models.AnnotationPopupDTO;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class AnnotationPopup {

  private Balloon popup;

  /** Hides the shown popup when called */
  public void hidePopUp() {
    if (popup != null) {
      popup.hide();
    }
  }

  /**
   * Shows a popup message based on the provided dto, message is shown based on mouse event and
   * popupMouseAdapter
   *
   * @param dto AnnotationDTO object
   * @param mouseEvent MouseEvent event
   * @param popupMouseAdapter MouseAdapter adapter
   */
  public void showAnnotationPopup(
      AnnotationPopupDTO dto, MouseEvent mouseEvent, MouseAdapter popupMouseAdapter) {
    JBPanel<?> panel = constructPopup(dto);

    panel.addMouseListener(popupMouseAdapter);

    // Create the Balloon popup
    popup =
        JBPopupFactory.getInstance()
            .createBalloonBuilder(panel)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .setBorderInsets(JBUI.emptyInsets())
            .setShadow(false)
            .createBalloon();
    popup.show(new RelativePoint(mouseEvent), Balloon.Position.below);
  }

  // Construct a Panel with the contents
  private JBPanel<?> constructPopup(AnnotationPopupDTO dto) {

    // Main Panel
    JBPanel<?> popupPanel = new JBPanel<>(new BorderLayout());
    popupPanel.setPreferredSize(new Dimension(500, 200));

    // Header or Message panel (Top part)
    JBPanel<?> messagePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    messagePanel.add(getFormattedMessage(dto.getMessage(), dto.getSeverity()));
    popupPanel.add(messagePanel, BorderLayout.NORTH);

    // Advice Panel (Middle part always in scroll)
    JBLabel adviceLabel = new JBLabel(ADVICE_HTML_FORMAT + dto.getAdvice() + CLOSE_HTML_FORMAT);
    adviceLabel.setPreferredSize(
        new Dimension(480, 200)); // Set a preferred width for the advice label

    // Always add adviceLabel into a scroll pane
    JBScrollPane adviceScrollPane = new JBScrollPane(adviceLabel);
    adviceScrollPane.setPreferredSize(new Dimension(480, 100)); // Fixed scrollable height
    popupPanel.add(adviceScrollPane, BorderLayout.CENTER);

    // Sub-panel (Bottom part)
    JBPanel<?> subPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    subPanel.add(new JBLabel(LAST_DETECTED_FORMAT + dto.getLastDetected()));
    subPanel.add(new JBLabel(OR_FORMAT));
    subPanel.add(new JBLabel(STATUS_FORMAT + dto.getStatus() + EMPTY_SPACES));

    // Create a clickable hyperlink for actions
    JBLabel actionLabel = getActionLabel(dto);

    subPanel.add(actionLabel);
    popupPanel.add(subPanel, BorderLayout.SOUTH);

    return popupPanel;
  }

  private static @NotNull JBLabel getActionLabel(AnnotationPopupDTO dto) {
    JBLabel actionLabel = new JBLabel();
    actionLabel.setText(ACTION_FORMAT);
    actionLabel.setForeground(JBColor.BLUE);
    actionLabel.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            try {
              Desktop.getDesktop().browse(new URI(dto.getActions()));
            } catch (Exception ex) {
              log.error(ex.getMessage());
            }
          }

          @Override
          public void mouseEntered(MouseEvent e) {
            actionLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          }

          @Override
          public void mouseExited(MouseEvent e) {
            actionLabel.setCursor(Cursor.getDefaultCursor());
          }
        });
    return actionLabel;
  }

  private static JBLabel getFormattedMessage(String message, String severity) {
    JBLabel vulnerabilityLabel = new JBLabel();
    if (StringUtils.isNotEmpty(severity)) {
      vulnerabilityLabel.setText(
          OPEN_HTML_FORMAT
              + SQUARE_BRACKET_OPEN
              + severity
              + SQUARE_BRACKET_CLOSE
              + ONE_SPACE
              + message
              + CLOSE_HTML_FORMAT);
      vulnerabilityLabel.setPreferredSize(new Dimension(480, 50));
      switch (severity.toLowerCase()) {
          // Add Icon as well
        case Constants.UTILS.CRITICAL:
          vulnerabilityLabel.setIcon(ContrastIcons.CRITICAL_SEVERITY_ICON);
          vulnerabilityLabel.setForeground(JBColor.RED);
          break;
        case Constants.UTILS.HIGH:
          vulnerabilityLabel.setIcon(ContrastIcons.HIGH_SEVERITY_ICON);
          vulnerabilityLabel.setForeground(JBColor.ORANGE);
          break;
        case Constants.UTILS.MEDIUM:
          vulnerabilityLabel.setIcon(ContrastIcons.MEDIUM_SEVERITY_ICON);
          vulnerabilityLabel.setForeground(JBColor.YELLOW);
          break;
        case Constants.UTILS.LOW:
          vulnerabilityLabel.setIcon(ContrastIcons.LOW_SEVERITY_ICON);
          vulnerabilityLabel.setForeground(JBColor.DARK_GRAY);
          break;
        case Constants.UTILS.NOTE:
          vulnerabilityLabel.setIcon(ContrastIcons.NOTE_SEVERITY_ICON);
          vulnerabilityLabel.setForeground(JBColor.LIGHT_GRAY);
          break;
        default:
      }
    }
    return vulnerabilityLabel;
  }
}
