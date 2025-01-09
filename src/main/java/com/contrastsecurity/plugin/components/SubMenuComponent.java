/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.components;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.contrastsecurity.plugin.models.SubMenuDTO;
import com.contrastsecurity.plugin.service.CacheDataService;
import com.contrastsecurity.plugin.utility.LocalizationUtil;
import com.contrastsecurity.plugin.utility.PopupUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ScrollPaneConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class SubMenuComponent extends JBPanel {

  private JBTabbedPane subMenuTabs;
  private JBPanel<?> redirectionContainer;
  private LocalizationUtil localizationUtil;

  private CacheDataService cacheDataService;

  // Tabs
  private OverviewComponent overviewComponent;
  private HowToFixComponent howToFixComponent;
  private HTTPRequestComponent httpRequestComponent;
  private EventsComponent eventsComponent;
  private TagComponent tagComponent;
  private MarkAsComponent markAsComponent;

  private transient SubMenuDTO subMenuDTO;
  private transient Icon redirectIcon;

  private final transient PopupUtil popupUtil;

  public SubMenuComponent(SubMenuDTO subMenuDTO, Project project) {
    cacheDataService = new CacheDataService();
    this.subMenuDTO = subMenuDTO;
    popupUtil = new PopupUtil(project);
    subMenuTabs = new JBTabbedPane();
    redirectionContainer = new JBPanel<>(new BorderLayout());
    localizationUtil = LocalizationUtil.getInstance();
    String appID = StringUtils.EMPTY;
    String traceID = StringUtils.EMPTY;
    if (this.subMenuDTO != null) {
      appID = this.subMenuDTO.getAppID();
      traceID = this.subMenuDTO.getTraceID();
    }
    overviewComponent = new OverviewComponent();
    howToFixComponent = new HowToFixComponent();
    httpRequestComponent = new HTTPRequestComponent();
    eventsComponent = new EventsComponent();
    tagComponent = new TagComponent(getTagsInOrganization(), appID, traceID, project);
    markAsComponent = new MarkAsComponent(getStatus(), appID, traceID, project);

    if (JBColor.isBright()) {
      redirectIcon = ContrastIcons.REDIRECTION_DARK;
    } else {
      redirectIcon = ContrastIcons.REDIRECTION;
    }

    configureTabbedPanels();

    setLayout(new BorderLayout());
    add(subMenuTabs, BorderLayout.CENTER);
    configureRedirectionContainer();
    add(redirectionContainer, BorderLayout.EAST);
  }

  private void configureTabbedPanels() {
    subMenuTabs.addTab(
        localizationUtil.getMessage(Constants.TITLE.OVERVIEW),
        new JBScrollPane(
            overviewComponent,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
    subMenuTabs.addTab(
        localizationUtil.getMessage(Constants.TITLE.HOW_TO_FIX),
        new JBScrollPane(
            howToFixComponent,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
    subMenuTabs.addTab(
        localizationUtil.getMessage(Constants.TITLE.EVENTS), eventsComponent.getScrollPane());
    subMenuTabs.addTab(
        localizationUtil.getMessage(Constants.TITLE.HTTP_REQUEST),
        new JBScrollPane(
            httpRequestComponent,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
    subMenuTabs.addTab(StringUtils.EMPTY, ContrastIcons.TAG_ICON, tagComponent.getScrollPane());
    subMenuTabs.addTab(
        localizationUtil.getMessage(Constants.TITLE.MARK_AS), markAsComponent.getScrollPane());

    subMenuTabs.addChangeListener(e -> this.refresh());
  }

  private void loadDataForOverviewTab() {
    if (subMenuDTO.getStory() != null) {
      overviewComponent.loadOverView(
          subMenuDTO.getFirstSeen(),
          subMenuDTO.getLastSeen(),
          subMenuDTO.getStory().getRisk().getText(),
          subMenuDTO.getStory().getChapters());
    } else {
      log.warn(Constants.LOGS.UNABLE_LOAD_DATA);
    }
  }

  private void loadDataForHowToFixTab() {
    if (subMenuDTO.getHowToFixVulnerability() != null) {
      howToFixComponent.loadHowToFix(subMenuDTO.getHowToFixVulnerability());
    } else {
      log.warn(Constants.LOGS.UNABLE_LOAD_DATA);
    }
  }

  private void loadDataForHttpRequestTab() {
    if (subMenuDTO != null && subMenuDTO.getRecommendation().getText() != null) {
      httpRequestComponent.loadHttpRequest(subMenuDTO.getRecommendation().getText());
    } else {
      log.warn(Constants.LOGS.UNABLE_LOAD_DATA);
    }
  }

  private void loadDataForEventTab() {
    if (!subMenuDTO.getEvent().isEmpty()) {
      eventsComponent.removeAll();
      eventsComponent.buildEventPanel(subMenuDTO.getEvent());
    } else {
      log.warn(Constants.LOGS.UNABLE_LOAD_DATA);
    }
  }

  private void loadTagData() {
    tagComponent.refresh(
        getTagsInVulnerability(),
        getTagsInOrganization(),
        subMenuDTO.getAppID(),
        subMenuDTO.getTraceID());
  }

  private void loadMarkAsData() {
    if (subMenuDTO != null) {
      markAsComponent.refresh(getStatus(), subMenuDTO.getAppID(), subMenuDTO.getTraceID());
    }
  }

  private void configureRedirectionContainer() {
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    AnAction redirectAction =
        new AnAction(redirectIcon) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            if (StringUtils.isNotEmpty(subMenuDTO.getRedirectionURL())) {
              try {
                Desktop.getDesktop().browse(new URI(getRedirectionURL()));
              } catch (Exception ex) {
                log.error(ex.getMessage());
              }
            } else {
              showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.FAILED_TO_REDIRECT));
            }
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };

    actionGroup.add(redirectAction);
    ActionToolbar actionToolbar =
        ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, false);
    actionToolbar.setTargetComponent(actionToolbar.getComponent());
    redirectionContainer.add(actionToolbar.getComponent());
    redirectionContainer.setPreferredSize(new Dimension(30, 30));
  }

  /** Refreshes the entire submenu panels based on the selected vulnerability */
  public void refresh() {
    if (subMenuDTO != null) {
      switch (subMenuTabs.getSelectedIndex()) {
        case 0:
          loadDataForOverviewTab();
          break;
        case 1:
          loadDataForHowToFixTab();
          break;
        case 2:
          loadDataForEventTab();
          break;
        case 3:
          loadDataForHttpRequestTab();
          break;
        case 4:
          loadTagData();
          break;
        case 5:
          loadMarkAsData();
          break;
        default:
      }
    } else {
      log.warn(Constants.LOGS.UNABLE_LOAD_DATA);
    }
  }

  private String getRedirectionURL() {
    StringBuilder builder = new StringBuilder(subMenuDTO.getRedirectionURL());
    builder.append("/vulns/");
    builder.append(subMenuDTO.getTraceID());
    return builder.toString();
  }

  private List<String> getTagsInOrganization() {
    if (subMenuDTO != null && StringUtils.isNotEmpty(subMenuDTO.getAppID())) {
      Object cache =
          cacheDataService.get(subMenuDTO.getAppID() + "-" + Constants.UTILS.EXISTING_TAGS_IN_ORG);
      if (cache != null) {
        return (List<String>) cache;
      }
    }
    return new ArrayList<>();
  }

  private List<String> getTagsInVulnerability() {
    if (subMenuDTO != null && StringUtils.isNotEmpty(subMenuDTO.getTraceID())) {
      Object cache =
          cacheDataService.get(subMenuDTO.getTraceID() + "-" + Constants.UTILS.TAGS_IN_VUL);
      if (cache != null) {
        return (List<String>) cache;
      }
    }
    return new ArrayList<>();
  }

  private void showErrorPopup(String message) {
    popupUtil.disposePopup();
    popupUtil.showFadingPopupOnRootPane(message, PopupUtil.PopupType.ERROR);
  }

  private String getStatus() {
    if (subMenuDTO != null && StringUtils.isNotEmpty(subMenuDTO.getTraceID())) {
      Object cache =
          cacheDataService.get(subMenuDTO.getTraceID() + "-" + Constants.UTILS.MARK_AS_STATUS);
      if (cache != null) {
        return (String) cache;
      }
    }
    return StringUtils.EMPTY;
  }
}
