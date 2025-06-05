/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.service;

import com.contrastsecurity.plugin.components.AssessComponent;
import com.contrastsecurity.plugin.components.ScanComponent;
import com.contrastsecurity.plugin.toolwindow.ContrastToolWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import java.util.Objects;
import javax.swing.JComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class ProjectCloseListener implements ProjectManagerListener {

  @Override
  public void projectClosing(@NotNull Project project) {
    ToolWindowManager instance = ToolWindowManager.getInstance(Objects.requireNonNull(project));
    ToolWindow contrastWindow = instance.getToolWindow("Contrast");
    if (contrastWindow != null) {
      Content content = contrastWindow.getContentManager().getContent(0); // Assuming the first tab
      if (Objects.nonNull(content)) {
        JComponent component = content.getComponent();
        if (component instanceof ContrastToolWindow contrastToolWindow) {
          stopAPICalls(contrastToolWindow);
          clearCaches(contrastToolWindow, new CacheDataService());
        }
      }
    }
  }

  private void stopAPICalls(ContrastToolWindow contrastToolWindow) {
    AssessComponent assessComponent = contrastToolWindow.getAssessComponent();
    if (assessComponent != null && assessComponent.isCalling()) {
      assessComponent.stopAPICall();
    }

    ScanComponent scanComponent = contrastToolWindow.getScanComponent();
    if (scanComponent != null && scanComponent.isCalling()) {
      scanComponent.stopAPICall();
    }
  }

  private void clearCaches(
      ContrastToolWindow contrastToolWindow, CacheDataService cacheDataService) {
    AssessComponent assessComponent = contrastToolWindow.getAssessComponent();
    if (StringUtils.isNotEmpty(assessComponent.getAppId())) {
      cacheDataService.clearCacheById(assessComponent.getAppId());
    }

    ScanComponent scanComponent = contrastToolWindow.getScanComponent();
    if (StringUtils.isNotEmpty(scanComponent.getProjectId())) {
      cacheDataService.clearCacheById(scanComponent.getProjectId());
    }
  }
}
