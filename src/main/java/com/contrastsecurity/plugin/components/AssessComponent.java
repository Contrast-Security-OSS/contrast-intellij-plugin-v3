/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.components;

import com.contrastsecurity.assess.v3.dto.AgentSession;
import com.contrastsecurity.assess.v3.dto.Chapter;
import com.contrastsecurity.assess.v3.dto.Event;
import com.contrastsecurity.assess.v3.dto.FileVulnerabilities;
import com.contrastsecurity.assess.v3.dto.HowToFixVulnerability;
import com.contrastsecurity.assess.v3.dto.Recommendation;
import com.contrastsecurity.assess.v3.dto.Server;
import com.contrastsecurity.assess.v3.dto.Story;
import com.contrastsecurity.assess.v3.dto.Trace;
import com.contrastsecurity.assess.v3.dto.TraceFile;
import com.contrastsecurity.assess.v3.dto.VulnerabilityDetails;
import com.contrastsecurity.plugin.annotation.CustomLineMarkerProvider;
import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.contrastsecurity.plugin.fetchers.Fetcher;
import com.contrastsecurity.plugin.inspection.EditorWidgetActionProvider;
import com.contrastsecurity.plugin.models.ConfigurationDTO;
import com.contrastsecurity.plugin.models.CustomSessionMetadataDTO;
import com.contrastsecurity.plugin.models.SubMenuDTO;
import com.contrastsecurity.plugin.models.TraceNodeDTO;
import com.contrastsecurity.plugin.persistent.CredentialDetailsService;
import com.contrastsecurity.plugin.scheduler.Scheduler;
import com.contrastsecurity.plugin.service.CacheDataService;
import com.contrastsecurity.plugin.service.SubMenuCacheService;
import com.contrastsecurity.plugin.toolwindow.ContrastToolWindow;
import com.contrastsecurity.plugin.tree.ReportTreeCellRenderer;
import com.contrastsecurity.plugin.utility.ComponentUtil;
import com.contrastsecurity.plugin.utility.CredentialUtil;
import com.contrastsecurity.plugin.utility.LocalizationUtil;
import com.contrastsecurity.plugin.utility.PopupUtil;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class AssessComponent extends JBPanel {

  private final transient Project project;
  private final transient PopupUtil popupUtil;
  private final ContrastToolWindow contrastToolWindow;

  private Fetcher fetcher;

  private final JBPanel<?> retrieveVulnerabilityContainer;
  private final JBPanel<?> currentFileContainer;
  private final JBPanel<?> vulnerabilityReportContainer;

  private final ComboBox<String> appComboBox;
  private final ComboBox<String> serverComboBox;
  private final ComboBox<String> buildNumberComboBox;
  private final ComboBox<String> propertyComboBox;
  private final ComboBox<String> valueComboBox;

  private final FilterComponent filterComponent;

  private final LocalizationUtil localizationUtil;

  private final CacheDataService cacheDataService;
  private final Scheduler scheduler;
  @Getter private String appId;
  @Getter private String actionURL;
  private transient SwingWorker<Void, Void> currentWorker;
  private transient SwingWorker<Void, Void> fetchWorker;

  private final JBRadioButton noneSession;
  private final JBRadioButton recentSession;
  private final JBRadioButton customSession;
  private String lastSelectedApp;
  private final JBTabbedPane assessTabs;
  private final JButton runButton;
  private final JButton clearButton;
  private final JButton clearButton2;
  private final JButton refreshButton;

  private AgentSession agentSession;
  private transient Map<String, CustomSessionMetadataDTO> customMetadata;
  private final Map<String, Integer> serverMap = new HashMap<>();
  private final Map<TraceNodeDTO, SubMenuDTO> subMenuDTOMap = new HashMap<>();

  private transient CustomLineMarkerProvider customLineMarkerProvider;
  private boolean isJava;
  @Getter private boolean isCalling;

  private final transient MyFileEditorListener myFileEditorListener;
  private final transient EditorWidgetActionProvider editorWidgetActionProvider;

  @Getter @Setter private int flag;

  public AssessComponent(Project project, ContrastToolWindow toolWindow, Scheduler scheduler) {
    this.flag = -1;
    this.project = project;
    this.contrastToolWindow = toolWindow;
    this.scheduler = scheduler;
    myFileEditorListener = new MyFileEditorListener(project, this);
    editorWidgetActionProvider = new EditorWidgetActionProvider();
    popupUtil = new PopupUtil(project);
    isCalling = false;
    filterComponent = new FilterComponent(true);
    localizationUtil = LocalizationUtil.getInstance();
    cacheDataService = new CacheDataService();
    retrieveVulnerabilityContainer = new JBPanel<>();
    currentFileContainer = new JBPanel<>(new BorderLayout());
    vulnerabilityReportContainer = new JBPanel<>();
    runButton = new JButton(localizationUtil.getMessage(Constants.RUN_BUTTON));
    clearButton = new JButton(localizationUtil.getMessage(Constants.CLEAR_BUTTON));
    clearButton2 = new JButton(localizationUtil.getMessage(Constants.CLEAR_BUTTON));
    refreshButton = new JButton(localizationUtil.getMessage(Constants.REFRESH_BUTTON));

    isJava = false;

    vulnerabilityReportContainer.setLayout(
        new BoxLayout(vulnerabilityReportContainer, BoxLayout.X_AXIS));
    assessTabs = new JBTabbedPane();

    customLineMarkerProvider = new CustomLineMarkerProvider();
    appComboBox = new ComboBox<>();
    appComboBox.setPreferredSize(new Dimension(500, 30));
    serverComboBox = new ComboBox<>();
    serverComboBox.setPreferredSize(new Dimension(500, 30));
    buildNumberComboBox = new ComboBox<>();
    noneSession = new JBRadioButton(Constants.NONE_SESSION);
    recentSession = new JBRadioButton(Constants.MOST_RECENT_SESSION);
    customSession = new JBRadioButton(Constants.CUSTOM_SESSION);
    propertyComboBox = new ComboBox<>();
    propertyComboBox.addItem(Constants.PLACE_HOLDERS.SYSTEM_PROPERTY);
    propertyComboBox.setEnabled(false);
    propertyComboBox.setPreferredSize(new Dimension(500, 30));
    valueComboBox = new ComboBox<>();
    valueComboBox.addItem(Constants.PLACE_HOLDERS.VALUE);
    valueComboBox.setEnabled(false);
    valueComboBox.setPreferredSize(new Dimension(500, 30));
    configureRetrieveVulnerabilityContainer();
    configureCurrentFileContainer();
    configureVulnerabilityReportPanel();
    configureTabsPanel();
    ComponentUtil.defaultToPanelOnMouseClick(retrieveVulnerabilityContainer);
    ComponentUtil.defaultToPanelOnMouseClick(currentFileContainer);
    ComponentUtil.defaultToPanelOnMouseClick(vulnerabilityReportContainer);
  }

  private void configureTabsPanel() {
    JBScrollPane retrieveFiltersScrollPane = new JBScrollPane(retrieveVulnerabilityContainer);
    retrieveFiltersScrollPane.setVerticalScrollBarPolicy(
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    assessTabs.addTab(
        localizationUtil.getMessage(Constants.TITLE.RETRIEVE_VULNERABILITIES_TITLE),
        retrieveFiltersScrollPane);
    assessTabs.addTab(
        localizationUtil.getMessage(Constants.TITLE.CURRENT_FILE_TITLE), currentFileContainer);
    assessTabs.addTab(
        localizationUtil.getMessage(Constants.TITLE.VULNERABILITY_REPORT_TITLE),
        vulnerabilityReportContainer);
    setLayout(new BorderLayout());
    add(assessTabs, BorderLayout.CENTER);

    assessTabs.addChangeListener(
        e -> {
          if (StringUtils.isNotEmpty(appId)) {
            Object cache = cacheDataService.get(appId);
            if (cache != null) {
              TraceFile vulnerabilities = (TraceFile) cache;
              loadVulnerabilityReport(
                  vulnerabilities.getTotalVulnerabilities(),
                  vulnerabilities.getFileVulnerabilitiesData(),
                  vulnerabilities.getUnMappedTrace());
            }
          }
        });
  }

  public void setFilterTabAsSelected() {
    assessTabs.setSelectedIndex(0);
  }

  /** Selects the current file tab from the Assess screen */
  public void showCurrentFileContainer() {
    assessTabs.setSelectedIndex(1); // Show currentFileContainer tab
  }

  private void configureRetrieveVulnerabilityContainer() {

    retrieveVulnerabilityContainer.setLayout(
        new BoxLayout(retrieveVulnerabilityContainer, BoxLayout.Y_AXIS));

    // Application Container
    retrieveVulnerabilityContainer.add(configureAppContainer());

    // Server Container
    retrieveVulnerabilityContainer.add(configureServerContainer());

    // Build Number Container
    retrieveVulnerabilityContainer.add(configureBuildNumberContainer());

    // Filter Container
    retrieveVulnerabilityContainer.add(filterComponent);

    // SessionMetadata container
    retrieveVulnerabilityContainer.add(configureSessionMetaDataContainer());

    // Processing Container
    retrieveVulnerabilityContainer.add(configureProcessingContainer());
  }

  private void configureCurrentFileContainer() {
    loadDefaultCurrentFilePanel();
  }

  /** Removes all the current file elements and resets the current file to default state */
  public void loadDefaultCurrentFilePanel() {
    // Clear the container before adding new components
    currentFileContainer.removeAll();

    currentFileContainer.add(
        new JBLabel(localizationUtil.getMessage(Constants.MESSAGES.NO_VULNERABILITIES_FOUND)),
        BorderLayout.CENTER);

    currentFileContainer.revalidate();
    currentFileContainer.repaint();
  }

  /**
   * Loads Vulnerability from cache based on the provided file name
   *
   * @param fileName File opened and active currently
   * @param fileVulnerabilities Vulnerabilities from Cache
   */
  public void loadCurrentFileReport(
      String fileName, String path, Map<String, FileVulnerabilities> fileVulnerabilities) {
    currentFileContainer.removeAll();
    if (MapUtils.isNotEmpty(fileVulnerabilities)) {
      boolean hasVulnerability = false;
      for (Map.Entry<String, FileVulnerabilities> entry : fileVulnerabilities.entrySet()) {
        String key = entry.getKey();
        FileVulnerabilities value = entry.getValue();
        if (StringUtils.contains(path, key)) {
          DefaultMutableTreeNode currentFileNode =
              getFileNode(fileName, value.getTotalVulnerabilities());
          addVulnerabilitiesToRootNode(value.getVulnerabilityDetailsData(), currentFileNode);
          Tree tree = getVulnerabilityTree(currentFileNode);
          JBScrollPane scrollPane = new JBScrollPane(tree);
          currentFileContainer.add(scrollPane, BorderLayout.CENTER);
          hasVulnerability = true;
          break;
        }
      }
      if (!hasVulnerability) {
        loadDefaultCurrentFilePanel();
      }
    } else {
      loadDefaultCurrentFilePanel();
    }
    currentFileContainer.revalidate();
    currentFileContainer.repaint();
  }

  private @NotNull Tree getVulnerabilityTree(DefaultMutableTreeNode rootNode) {
    TreeModel treeModel = new DefaultTreeModel(rootNode);
    Tree vulnerabilityTree = new Tree(treeModel);
    vulnerabilityTree.setRootVisible(true);
    vulnerabilityTree.setShowsRootHandles(true); // Show expand/collapse icons
    vulnerabilityTree.setCellRenderer(new ReportTreeCellRenderer(ContrastIcons.JAVA_ICON_DARK));

    vulnerabilityTree.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
              Tree tree = (Tree) e.getSource();
              int row = tree.getRowForLocation(e.getX(), e.getY());
              if (row > 0) {
                DefaultMutableTreeNode selectedNode =
                    (DefaultMutableTreeNode) tree.getPathForRow(row).getLastPathComponent();
                if (selectedNode.getUserObject() instanceof TraceNodeDTO dto) {
                  int lineNumber = Integer.parseInt(dto.getLineNumber());
                  moveToLine(
                      Objects.requireNonNull(
                          FileEditorManager.getInstance(project).getSelectedTextEditor()),
                      lineNumber);
                }
              }
            }
          }
        });
    return vulnerabilityTree;
  }

  private void addVulnerabilitiesToRootNode(
      List<VulnerabilityDetails> vulnerabilities, DefaultMutableTreeNode rootNode) {
    String lineNumber;
    for (VulnerabilityDetails vulnerability : vulnerabilities) {
      for (Chapter chapter : vulnerability.getStory().getChapters()) {
        Map<String, String> bodyFormatVariables = chapter.getBodyFormatVariables();
        lineNumber = bodyFormatVariables.get(Constants.UTILS.LINE_NUMBER);
        if (StringUtils.isNotEmpty(lineNumber)) {
          TraceNodeDTO traceNodeDTO = new TraceNodeDTO();
          traceNodeDTO.setSeverity(vulnerability.getTrace().getSeverity());
          traceNodeDTO.setTitle(vulnerability.getTrace().getTitle());
          traceNodeDTO.setLineNumber(lineNumber);
          DefaultMutableTreeNode traceNode = new DefaultMutableTreeNode(traceNodeDTO);
          rootNode.add(traceNode);
        }
      }
    }
  }

  // Helper method to move the caret to a specific line number
  private void moveToLine(Editor editor, int lineNumber) {
    CaretModel caretModel = editor.getCaretModel();
    if (lineNumber > 0 && lineNumber <= editor.getDocument().getLineCount()) {
      int lineStartOffset =
          editor.getDocument().getLineStartOffset(lineNumber - 1); // Convert 1-based to 0-based
      caretModel.moveToOffset(lineStartOffset);
      editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    } else {
      log.error(Constants.LOGS.UNABLE_TO_MOVE_TO_THE_LINE_NUMBER);
    }
  }

  public void configureVulnerabilityReportPanel() {
    // Clear the container before adding new components
    vulnerabilityReportContainer.removeAll();

    vulnerabilityReportContainer.add(
        new JBLabel(localizationUtil.getMessage(Constants.MESSAGES.NO_VULNERABILITIES_FOUND)));

    // Refresh the UI
    vulnerabilityReportContainer.revalidate();
    vulnerabilityReportContainer.repaint();
  }

  private void openSubMenuComponent(SubMenuDTO subMenuDTO) {
    ConfigurationDTO dto = CredentialDetailsService.getInstance().getSavedConfigDataByID(appId);
    if (Objects.nonNull(dto)) {
      Object cache =
          cacheDataService.get(subMenuDTO.getTraceID() + "-" + Constants.UTILS.TAGS_IN_VUL);
      if (cache == null) {
        dto = CredentialUtil.decryptDTO(dto);
        Fetcher subMenuFetcher =
            new Fetcher(
                dto.getUserName(),
                dto.getContrastURL(),
                dto.getOrgId(),
                dto.getApiKey(),
                dto.getServiceKey());
        List<String> tagForVulnerability =
            subMenuFetcher.getAppliedTagForVulnerability(subMenuDTO.getTraceID());
        cacheDataService.add(
            subMenuDTO.getTraceID() + "-" + Constants.UTILS.TAGS_IN_VUL, tagForVulnerability);
      }
      SubMenuComponent subMenu = new SubMenuComponent(subMenuDTO, project);
      subMenu.setPreferredSize(new Dimension(700, 500));
      subMenu.refresh();
      if (vulnerabilityReportContainer.getComponentCount() > 1) {
        vulnerabilityReportContainer.remove(1);
      }
      vulnerabilityReportContainer.add(subMenu);
    } else {
      showErrorPopup("Credentials not found");
      refreshApplications();
    }
  }

  private JBPanel<?> configureServerContainer() {
    JBPanel<?> serverContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    JBLabel serverLabel = new JBLabel(localizationUtil.getMessage(Constants.SERVER_FILTER_LABEL));
    serverLabel.setPreferredSize(new Dimension(160, 30));
    serverComboBox.addItem(localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_SERVERS_FOUND));
    serverContainer.add(serverLabel);
    serverContainer.add(serverComboBox);
    return serverContainer;
  }

  private JBPanel<?> configureAppContainer() {
    JBPanel<?> appContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    JBLabel appLabel = new JBLabel(localizationUtil.getMessage(Constants.APPLICATION_FILTER_LABEL));
    appLabel.setPreferredSize(new Dimension(160, 30));
    appContainer.add(appLabel);
    loadSavedApplicationsInComboBox();
    appContainer.add(appComboBox);
    appComboBox.addItemListener(
        e -> {
          if (e.getStateChange() == ItemEvent.SELECTED && appComboBox.getSelectedItem() != null) {
            String selectedApp = appComboBox.getSelectedItem().toString();
            if (StringUtils.isNotEmpty(selectedApp)) {
              appComboBox.setToolTipText(selectedApp);
            }
            loadServersAndBuildNoForSelectedApp(selectedApp);
          }
        });
    // Load servers for the default application on initialization
    if (appComboBox.getItemCount() > 0 && appComboBox.getSelectedItem() != null) {
      loadServersAndBuildNoForSelectedApp(appComboBox.getSelectedItem().toString());
    }
    return appContainer;
  }

  private void loadSavedApplicationsInComboBox() {
    List<String> savedApplicationNames =
        CredentialDetailsService.getInstance().getAllApplicationNames();
    if (CollectionUtils.isNotEmpty(savedApplicationNames)) {
      appComboBox.setEnabled(true);
      for (String name : savedApplicationNames) {
        appComboBox.addItem(name);
      }
    } else {
      appComboBox.addItem(
          localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_APPLICATIONS_FOUND));
      appComboBox.setEnabled(false);
      serverComboBox.setEnabled(false);
      buildNumberComboBox.setEnabled(false);
      log.info(Constants.LOGS.NO_PERSISTED_DATA_FOUND);
    }
  }

  private void loadServersAndBuildNoForSelectedApp(String selectedApp) {
    if (StringUtils.isNotEmpty(appId)
        && CredentialDetailsService.getInstance().getSavedConfigDataByID(appId) == null) {
      if (!fetchWorker.isDone()) {
        fetchWorker.cancel(true);
        showErrorPopup("Credentials deleted");
      }
    } else if (fetchWorker != null && !fetchWorker.isDone()) {
      return;
    }
    if (selectedApp == null
        || StringUtils.equals(
            selectedApp,
            localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_APPLICATIONS_FOUND))) {
      appComboBox.setEnabled(false);
      runButton.setEnabled(true);
      refreshButton.setEnabled(true);
      clearButton2.setEnabled(true);
      clearButton.setEnabled(true);
      return;
    }
    appComboBox.setEnabled(false);
    runButton.setEnabled(false);
    refreshButton.setEnabled(false);
    clearButton2.setEnabled(false);
    if (currentWorker != null && !currentWorker.isDone()) {
      currentWorker.cancel(true);
    }
    lastSelectedApp = selectedApp;
    showPersistingInfoPopup(localizationUtil.getMessage(Constants.MESSAGES.LOADING_FILTERS));

    clearAndDisableComboBoxes();

    currentWorker =
        new SwingWorker<>() {
          @Override
          protected Void doInBackground() throws Exception {
            isCalling = true;
            try {
              ConfigurationDTO savedConfigDto =
                  CredentialDetailsService.getInstance()
                      .getSavedConfigDataByName(selectedApp, Constants.ASSESS);
              savedConfigDto = CredentialUtil.decryptDTO(savedConfigDto);
              fetcher =
                  new Fetcher(
                      savedConfigDto.getUserName(),
                      savedConfigDto.getContrastURL(),
                      savedConfigDto.getOrgId(),
                      savedConfigDto.getApiKey(),
                      savedConfigDto.getServiceKey());
              String applicationId = savedConfigDto.getAppOrProjectID();

              updateSessionFilterState(false);
              updateSessionFilterStateComboBoxes(false);
              List<Server> servers = fetcher.getServersByApplicationID(applicationId);
              List<String> buildNumbers = fetcher.getBuildNumbersByApplicationID(applicationId);
              agentSession = fetcher.getMostRecentSession(applicationId);
              if (agentSession != null) {
                customMetadata =
                    fetcher.getCustomMetadata(applicationId, agentSession.getAgentSessionId());
              }

              // Update the UI components only if this is still the last selected application
              if (!isCancelled() && lastSelectedApp.equals(selectedApp)) {
                SwingUtilities.invokeLater(
                    () -> {
                      updateComboBox(
                          serverComboBox, servers, serverMap, Server::getName, Server::getServerId);
                      updateComboBox(
                          buildNumberComboBox, buildNumbers, null, String::toString, null);
                    });
              }
            } catch (Exception e) {
              log.error(Constants.LOGS.ERROR_WHILE_LOADING_SERVERS_AND_BUILD_NUMBERS, e);
            }
            return null;
          }

          @Override
          protected void done() {
            if (agentSession != null && MapUtils.isNotEmpty(customMetadata)) {
              propertyComboBox.removeAllItems();
              valueComboBox.removeAllItems();
              for (Map.Entry<String, CustomSessionMetadataDTO> entry : customMetadata.entrySet()) {
                propertyComboBox.addItem(entry.getKey());
              }
              updateSessionFilterState(true);
            } else {
              propertyComboBox.removeAllItems();
              valueComboBox.removeAllItems();
              propertyComboBox.addItem(Constants.PLACE_HOLDERS.SYSTEM_PROPERTY);
              valueComboBox.addItem(Constants.PLACE_HOLDERS.VALUE);
              updateSessionFilterState(false);
              updateSessionFilterStateComboBoxes(false);
            }
            isCalling = false;
            // Check if the task was not cancelled
            if (!isCancelled() && lastSelectedApp.equals(selectedApp)) {
              SwingUtilities.invokeLater(AssessComponent.this::disposePersistingPopup);
            } else {
              log.info(Constants.LOGS.LOADING_WAS_CANCELLED_OR_ANOTHER_APP_WAS_SELECTED);
            }
            appComboBox.setEnabled(true);
            runButton.setEnabled(true);
            refreshButton.setEnabled(true);
            clearButton2.setEnabled(true);
            clearButton.setEnabled(true);
          }
        };

    // Start the SwingWorker
    currentWorker.execute();
  }

  private <T, K, V> void updateComboBox(
      JComboBox<String> comboBox,
      List<T> items,
      Map<K, V> map,
      Function<T, String> nameMapper,
      Function<T, V> idMapper) {
    comboBox.removeAllItems();
    if (CollectionUtils.isNotEmpty(items)) {
      for (T item : items) {
        comboBox.addItem(nameMapper.apply(item));
        if (map != null && idMapper != null) {
          map.put((K) nameMapper.apply(item), idMapper.apply(item));
        }
      }
      comboBox.setEnabled(true);
    } else {
      comboBox.addItem(
          comboBox == serverComboBox
              ? localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_SERVERS_FOUND)
              : localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_BUILD_NUMBERS_FOUND));
      comboBox.setEnabled(false);
    }
  }

  private void clearAndDisableComboBoxes() {
    // Clear and disable both server and build number combo boxes
    serverComboBox.removeAllItems();
    buildNumberComboBox.removeAllItems();
    serverComboBox.setEnabled(false);
    buildNumberComboBox.setEnabled(false);
  }

  private JBPanel<?> configureBuildNumberContainer() {
    JBPanel<?> buildContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    JBLabel buildLabel =
        new JBLabel(localizationUtil.getMessage(Constants.BUILD_NUMBER_FILTER_LABEL));
    buildLabel.setPreferredSize(new Dimension(160, 30));
    refreshButton.setPreferredSize(new Dimension(100, 30));
    refreshButton.setToolTipText(
        localizationUtil.getMessage(Constants.TOOL_TIPS.REFRESH_SERVERS_AND_BUILD_NUMBERS));
    refreshButton.addActionListener(
        actionEvent -> {
          String selectedApp = appComboBox.getSelectedItem().toString().trim();
          if (StringUtils.isNotEmpty(selectedApp)
              && !StringUtils.equals(
                  selectedApp,
                  localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_APPLICATIONS_FOUND))) {
            loadServersAndBuildNoForSelectedApp(selectedApp);
          } else {
            showErrorPopup(
                localizationUtil.getMessage(Constants.MESSAGES.NO_APPLICATION_CONFIGURED));
          }
        });
    clearButton2.setPreferredSize(new Dimension(100, 30));
    clearButton2.setToolTipText(
        localizationUtil.getMessage(Constants.TOOL_TIPS.CLEARS_SERVERS_AND_BUILD_NUMBERS));
    clearButton2.addActionListener(
        actionEvent -> {
          String selectedApp = appComboBox.getSelectedItem().toString().trim();
          if (StringUtils.isNotEmpty(selectedApp)
              && !StringUtils.equals(
                  selectedApp,
                  localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_APPLICATIONS_FOUND))) {
            serverComboBox.removeAllItems();
            buildNumberComboBox.removeAllItems();
            serverComboBox.addItem(
                localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_SERVERS_FOUND));
            buildNumberComboBox.addItem(
                localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_BUILD_NUMBERS_FOUND));
            serverComboBox.setEnabled(false);
            buildNumberComboBox.setEnabled(false);
          } else {
            showErrorPopup(
                localizationUtil.getMessage(Constants.MESSAGES.NO_APPLICATION_CONFIGURED));
          }
        });
    buildNumberComboBox.addItem(
        localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_BUILD_NUMBERS_FOUND));
    buildContainer.add(buildLabel);
    buildContainer.add(buildNumberComboBox);
    buildContainer.add(refreshButton);
    buildContainer.add(clearButton2);
    return buildContainer;
  }

  private JBPanel<?> configureSessionMetaDataContainer() {
    JBPanel<?> sessionMetadataContainer = new JBPanel<>();
    sessionMetadataContainer.setLayout(new BoxLayout(sessionMetadataContainer, BoxLayout.Y_AXIS));
    JBPanel<?> radioButtonContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    // Configure Radio button container
    JBLabel sessionLabel =
        new JBLabel(localizationUtil.getMessage(Constants.SESSION_METADATA_LABEL));
    sessionLabel.setPreferredSize(new Dimension(160, 30));

    noneSession.addActionListener(e -> updateSessionFilterStateComboBoxes(false));
    recentSession.addActionListener(e -> updateSessionFilterStateComboBoxes(false));
    customSession.addActionListener(e -> updateSessionFilterStateComboBoxes(true));
    ButtonGroup radioButtonGroup = new ButtonGroup();
    radioButtonGroup.add(noneSession);
    radioButtonGroup.add(customSession);
    radioButtonGroup.add(recentSession);

    radioButtonContainer.add(sessionLabel);
    radioButtonContainer.add(noneSession);
    radioButtonContainer.add(customSession);
    radioButtonContainer.add(recentSession);
    updateSessionFilterState(false);

    JBPanel<?> formattedPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    JBLabel label = new JBLabel(StringUtils.SPACE); // Label for formating
    label.setPreferredSize(new Dimension(160, 60));
    formattedPanel.add(label);

    JBPanel<?> dropDownContainer = new JBPanel<>();
    dropDownContainer.setLayout(new BoxLayout(dropDownContainer, BoxLayout.Y_AXIS));

    propertyComboBox.addItemListener(
        e -> {
          valueComboBox.removeAllItems();
          if (e.getStateChange() == ItemEvent.SELECTED
              && propertyComboBox.getSelectedItem() != null
              && agentSession != null
              && MapUtils.isNotEmpty(customMetadata)) {
            List<String> values =
                customMetadata.get(propertyComboBox.getSelectedItem().toString()).getValues();
            if (CollectionUtils.isNotEmpty(values)) {
              values.forEach(valueComboBox::addItem);
            }
          }
        });

    dropDownContainer.add(propertyComboBox, BorderLayout.NORTH);
    dropDownContainer.add(valueComboBox, BorderLayout.SOUTH);
    formattedPanel.add(dropDownContainer);

    sessionMetadataContainer.add(radioButtonContainer);
    sessionMetadataContainer.add(formattedPanel);
    return sessionMetadataContainer;
  }

  private JBPanel<?> configureProcessingContainer() {
    JBPanel<?> processingContainer = new JBPanel<>(new FlowLayout(FlowLayout.TRAILING));
    runButton.setPreferredSize(new Dimension(120, 30));
    runButton.setToolTipText(
        localizationUtil.getMessage(Constants.TOOL_TIPS.FETCH_VULNERABILITIES));
    runButton.addActionListener(actionEvent -> runButtonOnClick());
    clearButton.setPreferredSize(new Dimension(120, 30));
    clearButton.setToolTipText(
        localizationUtil.getMessage(Constants.TOOL_TIPS.CLEARS_ALL_APPLIED_FILTERS));
    clearButton.addActionListener(actionEvent -> clearButtonOnClick());
    processingContainer.add(runButton);
    processingContainer.add(clearButton);
    return processingContainer;
  }

  /** Executes the functionality of the run button when clicked */
  public void runButtonOnClick() {
    String selectedApp = appComboBox.getSelectedItem().toString().trim();
    if (StringUtils.isNotEmpty(selectedApp)
        && !StringUtils.equals(
            selectedApp,
            localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_APPLICATIONS_FOUND))) {
      SwingUtilities.invokeLater(
          () -> {
            runButton.setEnabled(false);
            clearButton.setEnabled(false);
            appComboBox.setEnabled(false);
            buildNumberComboBox.setEnabled(false);
            serverComboBox.setEnabled(false);
            refreshButton.setEnabled(false);
            clearButton2.setEnabled(false);
          });
      run();
    } else {
      showInfoPopup(localizationUtil.getMessage(Constants.MESSAGES.NO_APPLICATION_CONFIGURED));
    }
    runButton.setEnabled(true);
  }

  private void clearButtonOnClick() {
    filterComponent.clearAppliedFilter();
    String selectedApp = appComboBox.getSelectedItem().toString().trim();
    noneSession.setSelected(true);
    propertyComboBox.setEnabled(false);
    valueComboBox.setEnabled(false);
    if (StringUtils.isNotEmpty(selectedApp)
        && !StringUtils.equals(
            selectedApp,
            localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_APPLICATIONS_FOUND))) {
      serverComboBox.removeAllItems();
      buildNumberComboBox.removeAllItems();
      serverComboBox.addItem(localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_SERVERS_FOUND));
      buildNumberComboBox.addItem(
          localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_BUILD_NUMBERS_FOUND));
      serverComboBox.setEnabled(false);
      buildNumberComboBox.setEnabled(false);
    }
  }

  private void run() {
    String selectedAppName = Objects.requireNonNull(appComboBox.getSelectedItem().toString());
    ConfigurationDTO savedConfigDto =
        CredentialDetailsService.getInstance()
            .getSavedConfigDataByName(selectedAppName, Constants.ASSESS);
    if (savedConfigDto != null) {
      savedConfigDto = CredentialUtil.decryptDTO(savedConfigDto);
      if (fetchWorker != null && !fetchWorker.isDone()) {
        fetchWorker.cancel(true);
      }
      // Display fetching message
      ConfigurationDTO finalSavedConfigDto = savedConfigDto;
      fetchWorker =
          new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
              contrastToolWindow.getScanComponent().updateState(false);
              isCalling = true;
              fetcher =
                  new Fetcher(
                      finalSavedConfigDto.getUserName(),
                      finalSavedConfigDto.getContrastURL(),
                      finalSavedConfigDto.getOrgId(),
                      finalSavedConfigDto.getApiKey(),
                      finalSavedConfigDto.getServiceKey());

              String applicationId = finalSavedConfigDto.getAppOrProjectID();
              if (StringUtils.isNotBlank(applicationId)) {
                appId = applicationId;
                actionURL =
                    finalSavedConfigDto.getContrastURL()
                        + Constants.UTILS.APPEND_ACTION
                        + finalSavedConfigDto.getOrgId()
                        + Constants.UTILS.APPLICATION_ACTION
                        + appId;

                new SubMenuCacheService().clear();
                showPersistingInfoPopup(
                    localizationUtil.getMessage(Constants.MESSAGES.RETRIEVING_VULNERABILITIES)
                        + " - "
                        + Constants.ASSESS);
                TraceFile vulnerabilitiesByFilter =
                    fetcher.fetchVulnerabilitiesByFilter(
                        applicationId, getAppliedFilter(filterComponent));
                disposePersistingPopup();
                if (CredentialDetailsService.getInstance()
                    .doesConfigExists(finalSavedConfigDto.getAppOrProject(), Constants.ASSESS)) {
                  startScheduler(
                      finalSavedConfigDto.getRefreshCycleMinutes(),
                      getAppliedFilter(filterComponent));
                  if (vulnerabilitiesByFilter != null
                      && vulnerabilitiesByFilter.getTotalVulnerabilities() > 0) {
                    cacheDataService.add(applicationId, vulnerabilitiesByFilter);
                    cacheDataService.add(
                        applicationId + "-" + Constants.UTILS.EXISTING_TAGS_IN_ORG,
                        vulnerabilitiesByFilter.getTagsInOrganization());

                    if (StringUtils.isNotEmpty(
                        contrastToolWindow.getScanComponent().getProjectId())) {
                      cacheDataService.clearCacheById(
                          contrastToolWindow.getScanComponent().getProjectId());
                    }
                    loadVulnerabilityReport(
                        vulnerabilitiesByFilter.getTotalVulnerabilities(),
                        vulnerabilitiesByFilter.getFileVulnerabilitiesData(),
                        vulnerabilitiesByFilter.getUnMappedTrace());
                    showSuccessPopup(
                        localizationUtil.getMessage(Constants.MESSAGES.FETCHED_VULNERABILITIES)
                            + " - "
                            + Constants.ASSESS);
                    updateFlag();
                    SwingUtilities.invokeLater(
                        () -> {
                          myFileEditorListener.reopenActiveFile();
                          customLineMarkerProvider.refresh(project);
                          editorWidgetActionProvider.refresh(project);
                        });
                  } else {
                    configureVulnerabilityReportPanel();
                    showInfoPopup(
                        localizationUtil.getMessage(Constants.MESSAGES.NO_VULNERABILITIES_FOUND));
                  }
                } else {
                  configureVulnerabilityReportPanel();
                  showInfoPopup(
                      localizationUtil.getMessage(Constants.MESSAGES.NO_VULNERABILITIES_FOUND));
                  log.error(Constants.MESSAGES.CONFIGURATION_DELETED);
                }
              } else {
                SwingUtilities.invokeLater(
                    () -> {
                      if (StringUtils.isNotEmpty(appId)) {
                        cacheDataService.clearCacheById(appId);
                        appId = StringUtils.EMPTY;
                      }
                      myFileEditorListener.reopenActiveFile();
                      customLineMarkerProvider.refresh(project);
                      editorWidgetActionProvider.refresh(project);
                    });
                showInfoPopup(
                    localizationUtil.getMessage(Constants.MESSAGES.NO_APPLICATION_CONFIGURED));
                log.info(Constants.LOGS.NO_CREDENTIALS_CONFIGURED);
              }
              return null;
            }

            @Override
            protected void done() {
              contrastToolWindow.getScanComponent().updateState(true);
              contrastToolWindow.getScanComponent().resetToDefault();
              isCalling = false;
              runButton.setEnabled(true);
              clearButton.setEnabled(true);
              appComboBox.setEnabled(true);
              refreshButton.setEnabled(true);
              clearButton2.setEnabled(true);
              buildNumberComboBox.setEnabled(
                  !StringUtils.equals(
                      buildNumberComboBox.getSelectedItem().toString(),
                      localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_BUILD_NUMBERS_FOUND)));
              serverComboBox.setEnabled(
                  !StringUtils.equals(
                      serverComboBox.getSelectedItem().toString(),
                      localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_SERVERS_FOUND)));
            }
          };
      fetchWorker.execute();
    } else {
      showErrorPopup("Credentials not found");
      refreshApplications();
    }
  }

  private void startScheduler(int refreshCycle, Map<String, String> appliedFilters) {
    scheduler.setRefreshCycleValue(refreshCycle);
    scheduler.scheduleTask(cacheDataService, appId, this, appliedFilters);
  }

  public boolean loadVulnerabilityReport(
      int totalVulnerabilities,
      Map<String, FileVulnerabilities> vulnerabilities,
      List<VulnerabilityDetails> unMappedVulnerabilities) {

    vulnerabilityReportContainer.removeAll();
    if (totalVulnerabilities > 0) {
      DefaultMutableTreeNode rootNode = getRootNodeWithNoOfIssues(totalVulnerabilities);
      if (MapUtils.isNotEmpty(vulnerabilities)) {
        addAllVulnerabilitiesToRootNode(rootNode, vulnerabilities);
      }
      if (CollectionUtils.isNotEmpty(unMappedVulnerabilities)) {
        loadUnmappedVulnerabilities(rootNode, unMappedVulnerabilities);
      }
      Tree vulnerabilityTree = getTree(rootNode);

      // Add the tree to a scroll pane
      JBScrollPane treeScrollPane = new JBScrollPane(vulnerabilityTree);
      treeScrollPane.setHorizontalScrollBarPolicy(
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      vulnerabilityReportContainer.add(treeScrollPane);
      // Refresh the UI
      vulnerabilityReportContainer.revalidate();
      vulnerabilityReportContainer.repaint();
      return true;
    } else {
      runButton.setEnabled(true);
      vulnerabilityReportContainer.add(
          new JBLabel(localizationUtil.getMessage(Constants.MESSAGES.NO_VULNERABILITIES_FOUND)),
          BorderLayout.CENTER);
      // Refresh the UI
      vulnerabilityReportContainer.revalidate();
      vulnerabilityReportContainer.repaint();
      return false;
    }
  }

  private @NotNull Tree getTree(DefaultMutableTreeNode rootNode) {
    TreeModel treeModel = new DefaultTreeModel(rootNode);
    Tree vulnerabilityTree = new Tree(treeModel);
    vulnerabilityTree.setRootVisible(true);
    vulnerabilityTree.setShowsRootHandles(true); // Show expand/collapse icons
    if (isJava) {
      vulnerabilityTree.setCellRenderer(new ReportTreeCellRenderer(ContrastIcons.JAVA_ICON_DARK));
    } else {
      vulnerabilityTree.setCellRenderer(new ReportTreeCellRenderer(null));
    }
    vulnerabilityTree.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
              Tree tree = (Tree) e.getSource();
              int row = tree.getRowForLocation(e.getX(), e.getY());
              if (row > 0) {
                DefaultMutableTreeNode selectedNode =
                    (DefaultMutableTreeNode) tree.getPathForRow(row).getLastPathComponent();
                if (selectedNode.getUserObject() instanceof TraceNodeDTO dto) {
                  openSubMenuComponent(subMenuDTOMap.get(dto));
                }
              }
            }
          }
        });
    return vulnerabilityTree;
  }

  public DefaultMutableTreeNode getRootNodeWithNoOfIssues(int totalIssues) {
    return new DefaultMutableTreeNode(
        Constants.UTILS.FOUND
            + StringUtils.SPACE
            + totalIssues
            + StringUtils.SPACE
            + Constants.UTILS.ISSUES);
  }

  public DefaultMutableTreeNode getFileNode(String fileName, int totalVulnerabilities) {
    return new DefaultMutableTreeNode(
        fileName
            + Constants.UTILS.OPEN_ROUND_BRACKET
            + totalVulnerabilities
            + StringUtils.SPACE
            + Constants.UTILS.ISSUES
            + Constants.UTILS.CLOSE_ROUND_BRACKET);
  }

  private void addAllVulnerabilitiesToRootNode(
      DefaultMutableTreeNode rootNode, Map<String, FileVulnerabilities> vulnerabilities) {
    Set<String> fileNames = vulnerabilities.keySet();
    for (String fileName : fileNames) {
      FileVulnerabilities fileVulnerabilities = vulnerabilities.get(fileName);
      List<VulnerabilityDetails> list = fileVulnerabilities.getVulnerabilityDetailsData();
      Story story1 = list.get(0).getStory();
      if (fileName.contains(".")) {
        fileName = getProcessedFileName(fileName, story1);
      }
      DefaultMutableTreeNode fileNode =
          getFileNode(fileName, fileVulnerabilities.getTotalVulnerabilities());

      List<VulnerabilityDetails> storyAndTracesList =
          fileVulnerabilities.getVulnerabilityDetailsData();
      for (VulnerabilityDetails details : storyAndTracesList) {
        Trace trace = details.getTrace();
        if (trace.getLanguage().equals("Java")) {
          isJava = true;
        }
        Story story = details.getStory();
        HowToFixVulnerability howToFixVulnerability = details.getHowToFixVulnerability();
        Recommendation recommendation = details.getRecommendation();
        List<Event> event = details.getEvent();
        String firstSeen = dateFormat(trace.getFirstTimeSeen());
        String lastSeen = dateFormat(trace.getLastTimeSeen());
        TraceNodeDTO traceNodeDTO = getPopulatedTraceNode(trace, story);
        DefaultMutableTreeNode traceNode = new DefaultMutableTreeNode(traceNodeDTO);
        fileNode.add(traceNode);
        SubMenuDTO subMenuDTO = new SubMenuDTO();
        subMenuDTO.setTraceID(trace.getUuid());
        subMenuDTO.setAppID(appId);
        subMenuDTO.setRedirectionURL(actionURL);
        cacheDataService.add(
            trace.getUuid() + "-" + Constants.UTILS.MARK_AS_STATUS, trace.getStatus());
        subMenuDTO.setStory(story);
        subMenuDTO.setHowToFixVulnerability(howToFixVulnerability);
        subMenuDTO.setRecommendation(recommendation);
        subMenuDTO.setEvent(event);
        subMenuDTO.setFirstSeen(firstSeen);
        subMenuDTO.setLastSeen(lastSeen);
        subMenuDTOMap.put(traceNodeDTO, subMenuDTO);
      }
      rootNode.add(fileNode);
    }
  }

  private String getProcessedFileName(String absoluteName, Story story) {
    StringBuilder fileName = new StringBuilder(StringUtils.EMPTY);
    List<Chapter> chapters = story.getChapters();
    for (Chapter chapter : chapters) {
      if (StringUtils.equals(chapter.getType(), Constants.UTILS.LOCATION)) {
        Map<String, String> bodyFormatVariables = chapter.getBodyFormatVariables();
        if (bodyFormatVariables.containsKey("className")) {
          String className = absoluteName.substring(absoluteName.lastIndexOf(".") + 1);
          fileName.append(className).append(".java");
          return fileName.toString();
        } else if (bodyFormatVariables.containsKey("fileName")) {
          String[] split = absoluteName.split("\\.");
          int length = split.length;
          if (length == 2) {
            return absoluteName;
          }
          fileName.append(split[length - 2]).append(".").append(split[length - 1]);
          return fileName.toString();
        }
      }
    }
    return fileName.toString();
  }

  private TraceNodeDTO getPopulatedTraceNode(Trace trace, Story story) {
    TraceNodeDTO dto = new TraceNodeDTO();
    dto.setSeverity(trace.getSeverity());
    dto.setTitle(trace.getTitle());
    for (Chapter chapter : story.getChapters()) {
      if (StringUtils.equals(chapter.getType(), Constants.UTILS.LOCATION)) {
        Map<String, String> bodyFormatVariables = chapter.getBodyFormatVariables();
        String lineNumber = bodyFormatVariables.get(Constants.UTILS.LINE_NUMBER);
        if (StringUtils.isNotEmpty(lineNumber)) {
          dto.setLineNumber(lineNumber);
        } else {
          dto.setLineNumber(StringUtils.EMPTY);
        }
      } else {
        log.info(Constants.LOGS.LOCATION_NOT_FOUND);
      }
    }
    return dto;
  }

  private void loadUnmappedVulnerabilities(
      DefaultMutableTreeNode rootNode, List<VulnerabilityDetails> traces) {

    if (CollectionUtils.isNotEmpty(traces)) {
      DefaultMutableTreeNode unMappedNode =
          new DefaultMutableTreeNode(Constants.UTILS.UN_MAPPED_TRACES);
      for (VulnerabilityDetails details : traces) {
        Story story = details.getStory();
        Trace trace = details.getTrace();
        Recommendation recommendation = details.getRecommendation();
        List<Event> event = details.getEvent();
        HowToFixVulnerability howToFixVulnerability = details.getHowToFixVulnerability();
        String firstSeen = dateFormat(trace.getFirstTimeSeen());
        String lastSeen = dateFormat(trace.getLastTimeSeen());
        TraceNodeDTO traceNodeDTO = new TraceNodeDTO();
        traceNodeDTO.setTitle(trace.getTitle());
        traceNodeDTO.setSeverity(trace.getSeverity());
        traceNodeDTO.setLineNumber(StringUtils.EMPTY);
        unMappedNode.add(new DefaultMutableTreeNode(traceNodeDTO));
        SubMenuDTO subMenuDTO = new SubMenuDTO();
        subMenuDTO.setTraceID(trace.getUuid());
        subMenuDTO.setAppID(appId);
        subMenuDTO.setRedirectionURL(actionURL);
        cacheDataService.add(
            trace.getUuid() + "-" + Constants.UTILS.MARK_AS_STATUS, trace.getStatus());
        subMenuDTO.setStory(story);
        subMenuDTO.setHowToFixVulnerability(howToFixVulnerability);
        subMenuDTO.setRecommendation(recommendation);
        subMenuDTO.setEvent(event);
        subMenuDTO.setFirstSeen(firstSeen);
        subMenuDTO.setLastSeen(lastSeen);
        subMenuDTOMap.put(traceNodeDTO, subMenuDTO);
      }
      rootNode.add(unMappedNode);
    }
    // Refresh the UI
    vulnerabilityReportContainer.revalidate();
    vulnerabilityReportContainer.repaint();
  }

  private String dateFormat(long timeStamp) {
    if (timeStamp != 0L) {
      Date date = new Date(timeStamp);
      SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
      return dateFormatter.format(date);
    }
    return StringUtils.EMPTY;
  }

  /** Loads the List of saved Application */
  public void refreshApplications() {
    appComboBox.removeAllItems();
    List<String> savedApplicationNames =
        CredentialDetailsService.getInstance().getAllApplicationNames();
    if (CollectionUtils.isNotEmpty(savedApplicationNames)) {
      for (String appName : savedApplicationNames) {
        appComboBox.addItem(appName);
      }
      appComboBox.setEnabled(true);
    } else {
      appComboBox.addItem(
          localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_APPLICATIONS_FOUND));
      appComboBox.setEnabled(false);
    }
    revalidate();
    repaint();
  }

  /**
   * Returns a Map of applied filter from the UI
   *
   * @return Map of String, String
   */
  public Map<String, String> getAppliedFilter(FilterComponent filterComponent) {
    Map<String, String> appliedFilter = new HashMap<>();
    addServerFilter(appliedFilter);
    addBuildNumberFilter(appliedFilter);
    filterComponent.addAppliedFilter(appliedFilter);
    addSessionMetadataFilter(appliedFilter);
    return appliedFilter;
  }

  private void addServerFilter(Map<String, String> appliedFilter) {
    Object selectedServer = serverComboBox.getSelectedItem();
    if (selectedServer != null) {
      Integer serverID = serverMap.get(selectedServer.toString());
      if (serverID != null) {
        appliedFilter.put(Constants.UTILS.SERVERS, serverID.toString());
      }
    } else {
      log.info(Constants.LOGS.NO_SERVER_FILTER_APPLIED);
    }
  }

  private void addSessionMetadataFilter(Map<String, String> appliedFilters) {
    if (noneSession.isSelected()) {
      return;
    }
    if (recentSession.isEnabled() && customSession.isEnabled()) {
      if (recentSession.isSelected() && agentSession != null) {
        appliedFilters.put(Constants.API.AGENT_SESSION_ID, agentSession.getAgentSessionId());
      } else if (customSession.isSelected() && MapUtils.isNotEmpty(customMetadata)) {
        appliedFilters.put(Constants.API.METADATA_FILTERS, getCustomSessionMetadataFilter());
      }
    }
  }

  private String getCustomSessionMetadataFilter() {
    CustomSessionMetadataDTO session = customMetadata.get(propertyComboBox.getSelectedItem());
    StringBuilder selectedCustomSession = new StringBuilder();

    selectedCustomSession.append("[\n");
    selectedCustomSession.append("{\n");
    selectedCustomSession.append("\"fieldID\" : ");
    selectedCustomSession.append("\"").append(session.getId()).append("\",");
    selectedCustomSession.append("\"values\" : [\n");
    selectedCustomSession.append("\"").append(valueComboBox.getSelectedItem()).append("\"");
    selectedCustomSession.append("]\n");
    selectedCustomSession.append("}");
    return selectedCustomSession.toString();
  }

  private void updateSessionFilterState(boolean enable) {
    noneSession.setSelected(true);
    recentSession.setEnabled(enable);
    customSession.setEnabled(enable);
  }

  private void updateSessionFilterStateComboBoxes(boolean enable) {
    propertyComboBox.setEnabled(enable);
    valueComboBox.setEnabled(enable);
  }

  private void addBuildNumberFilter(Map<String, String> appliedFilter) {
    Object selectedBuildNumber = buildNumberComboBox.getSelectedItem();
    if (selectedBuildNumber != null) {
      String buildNumber = selectedBuildNumber.toString();
      if (!StringUtils.equals(
          buildNumber,
          localizationUtil.getMessage(Constants.PLACE_HOLDERS.NO_BUILD_NUMBERS_FOUND))) {
        appliedFilter.put(Constants.UTILS.APP_VERSION_TAGS, buildNumber);
      }
    } else {
      log.info(Constants.LOGS.NO_BUILD_NUMBER_APPLIED);
    }
  }

  public void updateState(boolean isEnabled) {
    runButton.setEnabled(isEnabled);
  }

  public void resetToDefault() {
    loadDefaultCurrentFilePanel();
    configureVulnerabilityReportPanel();
  }

  private void showSuccessPopup(String message) {
    popupUtil.disposePopup();
    popupUtil.showFadingPopupOnRootPane(message, PopupUtil.PopupType.SUCCESS);
  }

  private void showInfoPopup(String message) {
    popupUtil.disposePopup();
    popupUtil.showFadingPopupOnRootPane(message, PopupUtil.PopupType.INFO);
  }

  private void showPersistingInfoPopup(String message) {
    popupUtil.disposePopup();
    popupUtil.showPersistingPopupOnRootPane(message, PopupUtil.PopupType.INFO);
  }

  private void showErrorPopup(String message) {
    popupUtil.disposePopup();
    popupUtil.showFadingPopupOnRootPane(message, PopupUtil.PopupType.ERROR);
  }

  private void disposePersistingPopup() {
    popupUtil.disposePopup();
  }

  public void stopAPICall() {
    if (fetchWorker != null) {
      fetchWorker.cancel(true);
    }
    if (currentWorker != null) {
      currentWorker.cancel(true);
    }
  }

  public void updateFlag() {
    flag = 0;
    contrastToolWindow.getScanComponent().setFlag(-1);
  }
}
