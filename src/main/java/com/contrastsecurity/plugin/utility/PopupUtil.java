/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.utility;

import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import java.awt.Color;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.Timer;

public class PopupUtil {

  private static final int POPUP_TIME = 3000;
  private Balloon popup;
  private Timer timer;
  private final Project project;

  public PopupUtil(Project project) {
    this.project = project;
  }

  public enum PopupType {
    INFO,
    SUCCESS,
    ERROR
  }

  public void showPersistingPopupOnRootPane(String message, PopupType type) {
    JRootPane rootPane = WindowManager.getInstance().getFrame(project).getRootPane();
    buildPopup(message, type, false);
    invokePopup(rootPane);
  }

  public void showFadingPopupOnRootPane(String message, PopupType type) {
    JRootPane rootPane = WindowManager.getInstance().getFrame(project).getRootPane();
    buildPopup(message, type, true);
    invokePopup(rootPane);
    stopExistingTimer();
    timer = new Timer(POPUP_TIME, event -> disposePopup());
    timer.setRepeats(false);
    timer.start();
  }

  public void showFadingPopupOnCustomPane(JComponent component, String message, PopupType type) {
    buildPopup(message, type, true);
    invokePopup(component);
    stopExistingTimer();
    timer = new Timer(POPUP_TIME, e -> disposePopup());
    timer.setRepeats(false);
    timer.start();
  }

  public void disposePopup() {
    if (popup != null) {
      popup.dispose();
      popup = null;
    }
  }

  private void stopExistingTimer() {
    if (timer != null) {
      timer.stop();
      timer = null;
    }
  }

  private void buildPopup(String message, PopupType type, boolean hideOnClick) {
    Color borderColor = JBColor.namedColor("Popup.border", JBColor.border());
    Color backgroundColor = JBColor.background();
    popup =
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                "<html><body style='background-color: #"
                    + Integer.toHexString(backgroundColor.getRGB()).substring(2)
                    + ";'>"
                    + message
                    + "</body></html>",
                getPopupIcon(type),
                borderColor,
                null)
            .setFillColor(backgroundColor)
            .setBorderColor(borderColor)
            .setCloseButtonEnabled(false)
            .setHideOnClickOutside(hideOnClick)
            .setHideOnAction(false)
            .createBalloon();
  }

  private void invokePopup(JComponent component) {
    popup.show(RelativePoint.getSouthEastOf(component), Balloon.Position.below);
  }

  private static Icon getPopupIcon(PopupType type) {
    switch (type) {
      case ERROR:
        return ContrastIcons.CRITICAL_SEVERITY_ICON;
      case SUCCESS:
        return ContrastIcons.CONNECTED_ICON;
      case INFO:
      default:
        return ContrastIcons.INFO;
    }
  }
}
