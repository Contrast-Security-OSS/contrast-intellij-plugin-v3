/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.inspection;

import com.contrastsecurity.plugin.components.AssessComponent;
import com.contrastsecurity.plugin.components.ScanComponent;
import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.contrastsecurity.plugin.toolwindow.ContrastToolWindow;
import com.contrastsecurity.plugin.utility.LocalizationUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.markup.InspectionWidgetActionProvider;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import java.util.List;
import javax.swing.JComponent;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditorWidgetActionProvider implements InspectionWidgetActionProvider {

  private DefaultActionGroup cachedActionGroup;
  private boolean isAssess;

  @Override
  public @Nullable AnAction createAction(@NotNull Editor editor) {
    if (editor.getEditorKind() == EditorKind.MAIN_EDITOR) {
      EditorWidgetDataProvider dataProvider;
      ToolWindowManager instance = ToolWindowManager.getInstance(editor.getProject());
      ToolWindow contrastWindow = instance.getToolWindow("Contrast");
      if (contrastWindow != null) {
        Content content =
            contrastWindow.getContentManager().getContent(0); // Assuming the first tab
        if (content != null) {
          JComponent component = content.getComponent();
          if (component instanceof ContrastToolWindow contrastToolWindow) {
            if (isAssess) {
              AssessComponent assessComponent = contrastToolWindow.getAssessComponent();
              dataProvider = new EditorWidgetDataProvider(assessComponent, editor.getProject());
              loadActions(
                  dataProvider.getActions(), getAssessAction(contrastWindow, assessComponent));
            }
            if (!isAssess) {
              ScanComponent scanComponent = contrastToolWindow.getScanComponent();
              dataProvider = new EditorWidgetDataProvider(scanComponent, editor.getProject());
              loadActions(dataProvider.getActions(), getScanAction(contrastWindow, scanComponent));
            }
          }
        }
      }
      return cachedActionGroup;
    }
    return null;
  }

  private AnAction getAssessAction(ToolWindow toolWindow, AssessComponent assessComponent) {
    AnAction assessAction =
        new AnAction(ContrastIcons.ASSESS_ICON) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            toolWindow.show(assessComponent::showCurrentFileContainer);
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    assessAction
        .getTemplatePresentation()
        .setText(LocalizationUtil.getInstance().getMessage(Constants.TOOL_TIPS.ASSESS));
    return assessAction;
  }

  private AnAction getScanAction(ToolWindow toolWindow, ScanComponent scanComponent) {
    AnAction scanAction =
        new AnAction(ContrastIcons.SCAN_ICON) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            toolWindow.show(scanComponent::showCurrentFileContainer);
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    scanAction
        .getTemplatePresentation()
        .setText(LocalizationUtil.getInstance().getMessage(Constants.TOOL_TIPS.SCAN));
    return scanAction;
  }

  private void loadActions(List<AnAction> actions, AnAction sourceAction) {
    cachedActionGroup = new DefaultActionGroup();
    if (CollectionUtils.isEmpty(actions)) {
      cachedActionGroup.add(new Action(ContrastIcons.CONTRAST));
      return;
    }
    cachedActionGroup.add(sourceAction);
    actions.forEach(
        action -> {
          cachedActionGroup.add(action);
        });
  }

  /** Refreshes the loaded Inspection Widgets based on the updated value */
  public void refresh(Project project) {
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null && editor.getEditorKind() == EditorKind.MAIN_EDITOR) {
      // Recreate the action group based on new data
      AnAction newAction = createAction(editor);
      if (newAction != null && newAction != cachedActionGroup) {
        cachedActionGroup = (DefaultActionGroup) newAction; // Update the cached action group
        editor.getComponent().revalidate(); // Revalidate and repaint the editor to reflect changes
        editor.getComponent().repaint();
        editor.getComponent().updateUI();
      }
    }
  }

  public void refresh(Project project, boolean isAssess) {
    this.isAssess = isAssess;
    refresh(project);
  }
}
