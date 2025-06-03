/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.components;

import com.contrastsecurity.plugin.annotation.CustomLineMarkerProvider;
import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.fetchers.Fetcher;
import com.contrastsecurity.plugin.inspection.EditorWidgetActionProvider;
import com.contrastsecurity.plugin.models.ConfigurationDTO;
import com.contrastsecurity.plugin.models.ScanVulnerabilityDTO;
import com.contrastsecurity.plugin.persistent.CredentialDetailsService;
import com.contrastsecurity.plugin.scheduler.Scheduler;
import com.contrastsecurity.plugin.service.CacheDataService;
import com.contrastsecurity.plugin.toolwindow.ContrastToolWindow;
import com.contrastsecurity.plugin.tree.CurrentFileTree;
import com.contrastsecurity.plugin.tree.VulnerabilityReportTree;
import com.contrastsecurity.plugin.utility.ComponentUtil;
import com.contrastsecurity.plugin.utility.CredentialUtil;
import com.contrastsecurity.plugin.utility.LocalizationUtil;
import com.contrastsecurity.plugin.utility.PopupUtil;
import com.contrastsecurity.plugin.utility.ScanVulnerabilityUtil;
import com.contrastsecurity.scan.dto.ProjectVulnerabilities;
import com.contrastsecurity.scan.dto.Vulnerability;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ScanComponent extends JBPanel {
  private final transient Project project;
  private final transient PopupUtil popupUtil;
  private JBPanel<?> filterContainer;
  private JBPanel<?> currentFileContainer;
  private JBPanel<?> vulnerabilityReportContainer;
  private final JBTabbedPane scanTabs;
  private final CacheDataService cacheDataService;
  private final FilterComponent filterComponent;
  private final LocalizationUtil localizationUtil;
  private final ContrastToolWindow contrastToolWindow;
  private JButton runButton;
  @Getter private boolean isCalling;
  @Getter private String actionUrl;

  @Getter private String projectId;
  private String limitMessage;

  private transient SwingWorker<Void, Void> worker;

  private final transient MyFileEditorListener myFileEditorListener;
  private final transient EditorWidgetActionProvider editorWidgetActionProvider;
  private final transient CustomLineMarkerProvider customLineMarkerProvider;
  private final Scheduler scheduler;

  @Getter @Setter private int flag;

  public ScanComponent(Project project, ContrastToolWindow toolWindow, Scheduler scheduler) {
    flag = -1;
    this.project = project;
    this.contrastToolWindow = toolWindow;
    this.popupUtil = new PopupUtil(project);
    this.scheduler = scheduler;
    myFileEditorListener = new MyFileEditorListener(project, this);
    editorWidgetActionProvider = new EditorWidgetActionProvider();
    customLineMarkerProvider = new CustomLineMarkerProvider();
    isCalling = false;
    actionUrl = StringUtils.EMPTY;
    projectId = StringUtils.EMPTY;
    localizationUtil = LocalizationUtil.getInstance();
    filterComponent = new FilterComponent(false);
    cacheDataService = new CacheDataService();
    scanTabs = new JBTabbedPane();
    filterContainer = new JBPanel<>();
    currentFileContainer = new JBPanel<>(new BorderLayout());
    vulnerabilityReportContainer = new JBPanel<>(new BorderLayout());
    runButton = new JButton(localizationUtil.getMessage(Constants.RUN_BUTTON));
    configureFilterContainer();
    configureDefaultCurrentFileContainer();
    configureVulnerabilityReportContainer();
    configureTabsPanel();
    ComponentUtil.defaultToPanelOnMouseClick(filterContainer);
    ComponentUtil.defaultToPanelOnMouseClick(currentFileContainer);
    ComponentUtil.defaultToPanelOnMouseClick(vulnerabilityReportContainer);
  }

  private void configureTabsPanel() {
    JBScrollPane filterScrollPane = new JBScrollPane(filterContainer);
    filterScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scanTabs.addTab(localizationUtil.getMessage(Constants.TITLE.FILTER), filterScrollPane);
    scanTabs.addTab(
        localizationUtil.getMessage(Constants.TITLE.CURRENT_FILE_TITLE), currentFileContainer);
    scanTabs.addTab(
        localizationUtil.getMessage(Constants.TITLE.VULNERABILITY_REPORT_TITLE),
        vulnerabilityReportContainer);
    setLayout(new BorderLayout());
    add(scanTabs, BorderLayout.CENTER);
    scanTabs.addChangeListener(e -> {});
  }

  public void setFilterTabAsSelected() {
    scanTabs.setSelectedIndex(0);
  }

  /** Selects the current file tab from the Assess screen */
  public void showCurrentFileContainer() {
    scanTabs.setSelectedIndex(1); // Show currentFileContainer tab
  }

  private void configureFilterContainer() {
    filterContainer.setLayout(new BoxLayout(filterContainer, BoxLayout.Y_AXIS));
    filterContainer.add(filterComponent);
    filterContainer.add(configureProcessingContainer());
  }

  private void configureDefaultCurrentFileContainer() {
    loadDefaultCurrentFile();
  }

  private void configureVulnerabilityReportContainer() {
    vulnerabilityReportContainer.removeAll();
    vulnerabilityReportContainer.add(
        new JBLabel(localizationUtil.getMessage(Constants.MESSAGES.NO_VULNERABILITIES_FOUND)));
    // Refresh the UI
    vulnerabilityReportContainer.revalidate();
    vulnerabilityReportContainer.repaint();
  }

  private JBPanel<?> configureProcessingContainer() {
    JBPanel<?> processingContainer = new JBPanel<>(new FlowLayout(FlowLayout.TRAILING));
    runButton.setPreferredSize(new Dimension(120, 30));
    runButton.addActionListener(actionEvent -> runButtonOnClick());
    JButton clearButton = new JButton(localizationUtil.getMessage(Constants.CLEAR_BUTTON));
    clearButton.setPreferredSize(new Dimension(120, 30));
    clearButton.addActionListener(e -> clearButtonOnClick());
    processingContainer.add(runButton);
    processingContainer.add(clearButton);
    return processingContainer;
  }

  /** Performs the action of scan run button when clicked */
  public void runButtonOnClick() {
    if (project != null) {
      ConfigurationDTO dto =
          CredentialDetailsService.getInstance()
              .getSavedConfigDataByName(project.getName(), Constants.SCAN);
      if (dto != null) {
        dto = CredentialUtil.decryptDTO(dto);
        Fetcher fetcher =
            new Fetcher(
                dto.getUserName(),
                dto.getContrastURL(),
                dto.getOrgId(),
                dto.getApiKey(),
                dto.getServiceKey());
        projectId = dto.getAppOrProjectID();
        actionUrl =
            dto.getContrastURL()
                + Constants.UTILS.APPEND_ACTION
                + dto.getOrgId()
                + Constants.UTILS.SCAN_ACTION
                + projectId;

        Map<String, String> appliedScanFilters = filterComponent.getAppliedScanFilters();
        if (appliedScanFilters != null && MapUtils.isNotEmpty(appliedScanFilters)) {
          appliedScanFilters.replaceAll((key, value) -> value.toUpperCase());
        }
        run(appliedScanFilters, fetcher);
      } else {
        if (StringUtils.isNotEmpty(projectId)) {
          cacheDataService.clearCacheById(projectId);
          projectId = StringUtils.EMPTY;
        }
        loadDefaultCurrentFile();
        loadDefaultVulnerabilityReport();
        myFileEditorListener.reopenActiveFile();
        customLineMarkerProvider.refresh(project);
        editorWidgetActionProvider.refresh(project);
        showErrorPopup(
            localizationUtil.getMessage(Constants.MESSAGES.NO_CREDENTIAL_CONFIGURE_FOR_PROJECT));
      }
    } else {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.NO_PROJECT_OPEN));
    }
  }

  private void clearButtonOnClick() {
    filterComponent.clearScanFilters();
  }

  /**
   * Loads the current file based on the provided value
   *
   * @param cacheData ScanVulnerabilityDTO
   * @param openFile String
   */
  public void loadCurrentFileReport(ScanVulnerabilityDTO cacheData, String openFile) {
    if (cacheData != null) {
      CurrentFileTree currentFileTree = new CurrentFileTree(currentFileContainer, this);
      currentFileTree.loadScanCurrentFile(cacheData.getMappedVulnerability(), openFile, project);
    } else {
      loadDefaultCurrentFile();
    }
  }

  private void run(Map<String, String> appliedFilters, Fetcher fetcher) {
    if (worker != null && !worker.isDone()) {
      worker.cancel(true);
    }
    worker =
        new SwingWorker<Void, Void>() {
          @Override
          protected Void doInBackground() throws Exception {
            contrastToolWindow.getAssessComponent().updateState(false);
            isCalling = true;
            runButton.setEnabled(false);
            showPersistingInfoPopup(
                localizationUtil.getMessage(Constants.MESSAGES.RETRIEVING_VULNERABILITIES)
                    + " - "
                    + Constants.SCAN);
            ProjectVulnerabilities projectVulnerabilities =
                fetcher.getVulnerabilitiesByAppliedFilter(projectId, appliedFilters);
            disposePersistingPopup();

            ConfigurationDTO creds =
                CredentialDetailsService.getInstance().getSavedConfigDataByID(projectId);
            if (creds != null) {
              startScheduler(
                  creds.getRefreshCycleMinutes(), creds.getAppOrProjectID(), appliedFilters);
              if (projectVulnerabilities != null) {
                limitMessage = projectVulnerabilities.getMessage();
                List<Vulnerability> allVulnerabilities =
                    projectVulnerabilities.getVulnerabilities();
                if (CollectionUtils.isNotEmpty(allVulnerabilities)) {
                  showSuccessPopup(
                      localizationUtil.getMessage(Constants.MESSAGES.FETCHED_VULNERABILITIES)
                          + " - "
                          + Constants.SCAN);
                  ScanVulnerabilityDTO cacheDTO = new ScanVulnerabilityDTO();
                  cacheDTO.setTotalVulnerabilities(allVulnerabilities.size());
                  cacheDTO.setMappedVulnerability(
                      ScanVulnerabilityUtil.getMappedVulnerabilities(allVulnerabilities));
                  cacheDataService.add(projectId, cacheDTO);
                  loadVulnerabilityReport(projectId);
                  if (StringUtils.isNotEmpty(contrastToolWindow.getAssessComponent().getAppId())) {
                    cacheDataService.clearCacheById(
                        contrastToolWindow.getAssessComponent().getAppId());
                  }
                  if (StringUtils.isNotEmpty(limitMessage)) {
                    showInfoPopup(limitMessage);
                  }
                  updateFlag();
                  SwingUtilities.invokeLater(
                      () -> {
                        myFileEditorListener.reopenActiveFile();
                        customLineMarkerProvider.refresh(project);
                        editorWidgetActionProvider.refresh(project);
                      });
                } else {
                  loadDefaultCurrentFile();
                  loadDefaultVulnerabilityReport();
                  showInfoPopup(
                      localizationUtil.getMessage(Constants.MESSAGES.NO_VULNERABILITIES_FOUND));
                }
              } else {
                loadDefaultCurrentFile();
                loadDefaultVulnerabilityReport();
                showInfoPopup(
                    localizationUtil.getMessage(Constants.MESSAGES.NO_VULNERABILITIES_FOUND));
              }
            }
            return null;
          }

          @Override
          protected void done() {
            contrastToolWindow.getAssessComponent().updateState(true);
            contrastToolWindow.getAssessComponent().resetToDefault();
            isCalling = false;
            runButton.setEnabled(true);
          }
        };
    worker.execute();
  }

  private void startScheduler(
      int refreshCycle, String projectId, Map<String, String> appliedFilters) {
    scheduler.setRefreshCycleValue(refreshCycle);
    scheduler.scheduleTask(cacheDataService, projectId, this, appliedFilters);
  }

  public void loadVulnerabilityReport(String projectId) {
    ScanVulnerabilityDTO cacheData = (ScanVulnerabilityDTO) cacheDataService.get(projectId);
    if (cacheData != null) {
      VulnerabilityReportTree reportTree =
          new VulnerabilityReportTree(vulnerabilityReportContainer, this);
      reportTree.loadScanVulnerabilityReport(
          cacheData.getTotalVulnerabilities(), cacheData.getMappedVulnerability());
    } else {
      loadDefaultVulnerabilityReport();
    }
  }

  /** Loads the Default Current File */
  public void loadDefaultCurrentFile() {
    currentFileContainer.removeAll();
    JBLabel defaultLabel =
        new JBLabel(localizationUtil.getMessage(Constants.MESSAGES.NO_VULNERABILITIES_FOUND));
    currentFileContainer.add(defaultLabel, BorderLayout.CENTER);
    currentFileContainer.revalidate();
    currentFileContainer.repaint();
  }

  /** Loads the Default Vulnerability Report */
  public void loadDefaultVulnerabilityReport() {
    vulnerabilityReportContainer.removeAll();
    JBLabel defaultLabel =
        new JBLabel(localizationUtil.getMessage(Constants.MESSAGES.NO_VULNERABILITIES_FOUND));
    vulnerabilityReportContainer.add(defaultLabel, BorderLayout.CENTER);
    vulnerabilityReportContainer.revalidate();
    vulnerabilityReportContainer.repaint();
  }

  public void updateState(boolean isEnabled) {
    runButton.setEnabled(isEnabled);
  }

  public void resetToDefault() {
    loadDefaultCurrentFile();
    loadDefaultVulnerabilityReport();
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
    if (worker != null) {
      worker.cancel(true);
    }
  }

  public void updateFlag() {
    flag = 0;
    contrastToolWindow.getAssessComponent().setFlag(-1);
  }
}
