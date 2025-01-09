/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.toolwindow;

import com.contrastsecurity.plugin.components.AboutComponent;
import com.contrastsecurity.plugin.components.AssessComponent;
import com.contrastsecurity.plugin.components.ScanComponent;
import com.contrastsecurity.plugin.configuration.ContrastConfiguration;
import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.contrastsecurity.plugin.scheduler.Scheduler;
import com.contrastsecurity.plugin.utility.LocalizationUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ScrollPaneConstants;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class ContrastToolWindow extends JBPanel {

  private final transient Project project;
  private final LocalizationUtil localizationUtil;
  private final AboutComponent aboutComponent;
  @Getter private final AssessComponent assessComponent;
  @Getter private final ScanComponent scanComponent;
  private final Scheduler scheduler;

  public ContrastToolWindow(Project project) {
    this.project = project;
    scheduler = new Scheduler(project);

    localizationUtil = LocalizationUtil.getInstance();
    aboutComponent = new AboutComponent();
    assessComponent = new AssessComponent(project, this, scheduler);
    scanComponent = new ScanComponent(project, this, scheduler);

    setLayout(new BorderLayout());
    configureMainActions();
  }

  private void configureMainActions() {
    removeAll();
    JBPanel<?> actionsPanel = new JBPanel<>();
    DefaultActionGroup actions = new DefaultActionGroup();
    getMainActions().forEach(actions::add);
    ActionToolbar actionToolbar =
        ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actions, false);
    actionToolbar.setTargetComponent(actionToolbar.getComponent());
    actionsPanel.add(actionToolbar.getComponent());
    actionsPanel.setPreferredSize(new Dimension(30, 30));
    add(actionsPanel, BorderLayout.WEST);
    revalidate();
    repaint();
  }

  private void configureRefreshAction(AnAction sourceAction, AnAction refreshAction) {
    JBPanel<?> actionsPanel = new JBPanel<>();
    DefaultActionGroup actions = new DefaultActionGroup();
    actions.add(sourceAction);
    actions.add(refreshAction);
    ActionToolbar actionToolbar =
        ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actions, false);
    actionToolbar.setTargetComponent(actionToolbar.getComponent());
    actionsPanel.add(actionToolbar.getComponent());
    actionsPanel.setPreferredSize(new Dimension(30, 30));
    add(actionsPanel, BorderLayout.EAST);
  }

  private List<AnAction> getMainActions() {
    List<AnAction> mainActions = new ArrayList<>();

    AnAction aboutAction =
        new AnAction(ContrastIcons.INFO) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            loadAboutScreen();
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    aboutAction
        .getTemplatePresentation()
        .setText(localizationUtil.getMessage(Constants.TOOL_TIPS.ABOUT));

    AnAction settingsAction =
        new AnAction(ContrastIcons.SETTINGS_ICON) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ContrastConfiguration.class);
          }
        };
    settingsAction.getTemplatePresentation().setText(Constants.TOOL_TIPS.CONFIGURATIONS_SETTINGS);

    AnAction assessAction =
        new AnAction(ContrastIcons.ASSESS_ICON) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            loadAssessScreen();
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    assessAction
        .getTemplatePresentation()
        .setText(localizationUtil.getMessage(Constants.TOOL_TIPS.ASSESS));

    AnAction scanAction =
        new AnAction(ContrastIcons.SCAN_ICON) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            loadScanScreen();
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    scanAction
        .getTemplatePresentation()
        .setText(localizationUtil.getMessage(Constants.TOOL_TIPS.SCAN));
    mainActions.add(aboutAction);
    mainActions.add(settingsAction);
    mainActions.add(assessAction);
    mainActions.add(scanAction);
    return mainActions;
  }

  private void loadAboutScreen() {
    configureMainActions();
    add(
        new JBScrollPane(
            aboutComponent,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
        BorderLayout.CENTER);
  }

  private void loadAssessScreen() {
    configureMainActions();
    add(assessComponent, BorderLayout.CENTER);
    configureRefreshAction(getAssessAction(), getAssessRefreshAction());
  }

  private void loadScanScreen() {
    configureMainActions();
    add(scanComponent, BorderLayout.CENTER);
    configureRefreshAction(getScanAction(), getScanRefreshAction());
  }

  private AnAction getAssessRefreshAction() {
    AnAction refreshAction =
        new AnAction(ContrastIcons.REFRESH) {

          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            assessComponent.runButtonOnClick();
          }

          @Override
          public void update(@NotNull AnActionEvent e) {
            e.getPresentation()
                .setEnabled(!assessComponent.isCalling() && !scanComponent.isCalling());
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    refreshAction
        .getTemplatePresentation()
        .setText(localizationUtil.getMessage(Constants.TOOL_TIPS.REFRESH));
    return refreshAction;
  }

  private AnAction getScanRefreshAction() {
    AnAction refreshAction =
        new AnAction(ContrastIcons.REFRESH) {

          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            scanComponent.runButtonOnClick();
          }

          @Override
          public void update(@NotNull AnActionEvent e) {
            e.getPresentation()
                .setEnabled(!assessComponent.isCalling() && !scanComponent.isCalling());
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    refreshAction
        .getTemplatePresentation()
        .setText(localizationUtil.getMessage(Constants.TOOL_TIPS.REFRESH));
    return refreshAction;
  }

  private AnAction getAssessAction() {
    AnAction assessAction =
        new AnAction(ContrastIcons.ASSESS_ICON) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            assessComponent.setFilterTabAsSelected();
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    assessAction
        .getTemplatePresentation()
        .setText(localizationUtil.getMessage(Constants.TOOL_TIPS.ASSESS));
    return assessAction;
  }

  private AnAction getScanAction() {
    AnAction assessAction =
        new AnAction(ContrastIcons.SCAN_ICON) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            scanComponent.setFilterTabAsSelected();
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    assessAction
        .getTemplatePresentation()
        .setText(localizationUtil.getMessage(Constants.TOOL_TIPS.SCAN));
    return assessAction;
  }
}
