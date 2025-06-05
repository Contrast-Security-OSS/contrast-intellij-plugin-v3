/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.inspection;

import com.contrastsecurity.plugin.components.AssessComponent;
import com.contrastsecurity.plugin.components.ScanComponent;
import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.toolwindow.ContrastToolWindow;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class Action extends AnAction {

  private String source;

  public Action(Icon icon) {
    super(icon);
    this.source = StringUtils.EMPTY;
  }

  public Action(String text, Icon icon, String source) {
    super(text, StringUtils.EMPTY, icon);
    this.source = source;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ToolWindowManager instance = ToolWindowManager.getInstance(e.getProject());
    ToolWindow contrastWindow =
        instance.getToolWindow("Contrast"); // Fetch the 'Contrast' tool window
    if (contrastWindow != null) {
      Content content = contrastWindow.getContentManager().getContent(0); // Assuming the first tab
      if (content != null) {
        JComponent component = content.getComponent();
        if (component instanceof ContrastToolWindow contrastToolWindow) {
          if (StringUtils.equals(source, Constants.ASSESS)) {
            AssessComponent assessComponent = contrastToolWindow.getAssessComponent();
            contrastWindow.show(assessComponent::showCurrentFileContainer);
          } else if (StringUtils.equals(source, Constants.SCAN)) {
            ScanComponent scanComponent = contrastToolWindow.getScanComponent();
            contrastWindow.show(scanComponent::showCurrentFileContainer);
          } else {
            contrastWindow.show();
          }
        }
      }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    if (doseSupportsShowTextInToolbar()) {
      presentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true);
    }
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  private boolean doseSupportsShowTextInToolbar() {
    return ApplicationInfo.getInstance().getBuild().getBaselineVersion() > 242;
  }
}
