/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class ContrastToolWindowFactory implements ToolWindowFactory {

  public static final Map<Project, ContrastToolWindow> TOOL_WINDOWS = new HashMap<>();

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    ContrastToolWindow toolWindowUI;
    if (TOOL_WINDOWS.containsKey(project)) {
      toolWindowUI = TOOL_WINDOWS.get(project);
    } else {
      toolWindowUI = new ContrastToolWindow(project);
    }
    ContentFactory factory = ContentFactory.getInstance();
    Content content = factory.createContent(toolWindowUI, StringUtils.EMPTY, false);
    SwingUtilities.invokeLater(() -> toolWindow.getContentManager().addContent(content));
  }
}
