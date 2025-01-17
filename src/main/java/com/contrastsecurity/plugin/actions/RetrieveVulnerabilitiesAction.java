/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.actions;

import com.contrastsecurity.plugin.components.AssessComponent;
import com.contrastsecurity.plugin.components.ScanComponent;
import com.contrastsecurity.plugin.toolwindow.ContrastToolWindow;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import groovy.util.logging.Slf4j;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SwingWorker;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class RetrieveVulnerabilitiesAction extends AnAction {

  private SwingWorker<Void, Void> worker;

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ToolWindowManager instance =
        ToolWindowManager.getInstance(Objects.requireNonNull(e.getProject()));
    ToolWindow contrastWindow = instance.getToolWindow("Contrast");
    if (contrastWindow != null) {
      Content content = contrastWindow.getContentManager().getContent(0); // Assuming the first tab
      if (content != null) {
        JComponent component = content.getComponent();
        if (component instanceof ContrastToolWindow contrastToolWindow
            && contrastToolWindow.getScanComponent() != null) {
          retrieveAction(contrastToolWindow.getScanComponent());
        }
      }
    }
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    ToolWindowManager instance =
        ToolWindowManager.getInstance(Objects.requireNonNull(e.getProject()));
    ToolWindow contrastWindow = instance.getToolWindow("Contrast");
    if (contrastWindow != null) {
      Content content = contrastWindow.getContentManager().getContent(0); // Assuming the first tab
      if (content != null) {
        JComponent component = content.getComponent();
        if (component instanceof ContrastToolWindow contrastToolWindow) {
          AssessComponent assessComponent = contrastToolWindow.getAssessComponent();
          ScanComponent scanComponent = contrastToolWindow.getScanComponent();
          if (assessComponent.isCalling() || scanComponent.isCalling()) {
            e.getPresentation().setEnabled(false);
          } else {
            e.getPresentation().setEnabled(true);
          }
        }
      }
    }
  }

  private void retrieveAction(ScanComponent scanComponent) {
    if (worker != null) {
      worker.cancel(true);
      worker = null;
    }
    worker =
        new SwingWorker<Void, Void>() {
          @Override
          protected Void doInBackground() throws Exception {
            scanComponent.runButtonOnClick();
            return null;
          }
        };
    worker.execute();
  }
}
