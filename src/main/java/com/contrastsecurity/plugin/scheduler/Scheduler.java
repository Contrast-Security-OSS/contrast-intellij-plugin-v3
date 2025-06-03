/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.scheduler;

import com.contrastsecurity.assess.v3.dto.TraceFile;
import com.contrastsecurity.plugin.components.AssessComponent;
import com.contrastsecurity.plugin.components.MyFileEditorListener;
import com.contrastsecurity.plugin.components.ScanComponent;
import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.fetchers.Fetcher;
import com.contrastsecurity.plugin.inspection.EditorWidgetActionProvider;
import com.contrastsecurity.plugin.models.ConfigurationDTO;
import com.contrastsecurity.plugin.models.ScanVulnerabilityDTO;
import com.contrastsecurity.plugin.persistent.CredentialDetailsService;
import com.contrastsecurity.plugin.service.CacheDataService;
import com.contrastsecurity.plugin.utility.CredentialUtil;
import com.contrastsecurity.plugin.utility.LocalizationUtil;
import com.contrastsecurity.plugin.utility.PopupUtil;
import com.contrastsecurity.plugin.utility.ScanVulnerabilityUtil;
import com.contrastsecurity.scan.dto.ProjectVulnerabilities;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Slf4j
public class Scheduler implements Serializable {

  private static final String TASK_TIMER = "TASK-TIMER";
  private transient Timer timer = new Timer(TASK_TIMER);
  private Date lastRunDate;
  private boolean flag = true;
  int refreshCycleValue;
  private final transient Project project;
  private transient MyFileEditorListener myFileEditorListener;
  private final transient PopupUtil popupUtil;
  private final transient CredentialDetailsService credentialDetailsService =
      CredentialDetailsService.getInstance();
  private final transient EditorWidgetActionProvider editorWidgetActionProvider;

  public Scheduler(Project project) {
    this.project = project;
    popupUtil = new PopupUtil(project);
    myFileEditorListener = new MyFileEditorListener(project);
    editorWidgetActionProvider = new EditorWidgetActionProvider();
  }

  // schedule the task as per the refresh cycle minutes
  public void scheduleTask(
      CacheDataService cacheDataService,
      String appOrProjectId,
      JBPanel<?> panel,
      Map<String, String> appliedFilter) {
    try {
      if (!flag) {
        timer.cancel();
        timer = new Timer(TASK_TIMER);
      }
      lastRunDate = new Date();
      flag = false;
      TimerTask task =
          new TimerTask() {
            @Override
            public void run() {
              if (panel instanceof AssessComponent assessComponent) {
                fetchAssess(cacheDataService, appOrProjectId, appliedFilter, assessComponent);
              } else if (panel instanceof ScanComponent scanComponent) {
                fetchScan(cacheDataService, appOrProjectId, appliedFilter, scanComponent);
              }
              lastRunDate = getDate(0, new Date());
            }
          };
      timer.schedule(
          task, getDate(getRefreshCycleValue(), lastRunDate), getRefreshCycleValue() * 60000L);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  public void fetchScan(
      CacheDataService cacheDataService,
      String projectId,
      Map<String, String> appliedFilter,
      ScanComponent scanComponent) {
    ConfigurationDTO savedConfigDataByID =
        credentialDetailsService.getSavedConfigDataByID(projectId);
    if (Objects.nonNull(savedConfigDataByID)) {
      savedConfigDataByID = CredentialUtil.decryptDTO(savedConfigDataByID);
      ScanVulnerabilityDTO cacheDTO = new ScanVulnerabilityDTO();
      ProjectVulnerabilities vulnerabilitiesByAppliedFilter =
          getFetcher(savedConfigDataByID)
              .getVulnerabilitiesByAppliedFilter(projectId, appliedFilter);
      if (CollectionUtils.isNotEmpty(vulnerabilitiesByAppliedFilter.getVulnerabilities())) {
        cacheDTO.setTotalVulnerabilities(
            vulnerabilitiesByAppliedFilter.getVulnerabilities().size());
        cacheDTO.setMappedVulnerability(
            ScanVulnerabilityUtil.getMappedVulnerabilities(
                vulnerabilitiesByAppliedFilter.getVulnerabilities()));
        cacheDataService.clearCacheById(projectId);
        cacheDataService.add(projectId, cacheDTO);
        scanComponent.loadVulnerabilityReport(projectId);
        scanComponent.updateFlag();
        myFileEditorListener.reopenActiveFile();
        editorWidgetActionProvider.refresh(project);
      }
      showPopup(Constants.SCAN);
    } else {
      cancel();
    }
  }

  // Fetch the vulnerabilities as per applied filter and application
  public void fetchAssess(
      CacheDataService cacheDataService,
      String appId,
      Map<String, String> appliedFilter,
      AssessComponent assessComponent) {
    ConfigurationDTO savedConfigDataByID = credentialDetailsService.getSavedConfigDataByID(appId);
    if (Objects.nonNull(savedConfigDataByID)) {
      savedConfigDataByID = CredentialUtil.decryptDTO(savedConfigDataByID);
      TraceFile traceFile =
          getFetcher(savedConfigDataByID).fetchVulnerabilitiesByFilter(appId, appliedFilter);
      cacheDataService.add(appId, traceFile);
      if (Objects.nonNull(traceFile)) {
        assessComponent.loadVulnerabilityReport(
            traceFile.getTotalVulnerabilities(),
            traceFile.getFileVulnerabilitiesData(),
            traceFile.getUnMappedTrace());
        assessComponent.updateFlag();
        myFileEditorListener.reopenActiveFile();
        editorWidgetActionProvider.refresh(project);
      }
      showPopup(Constants.ASSESS);
    } else {
      cancel();
    }
  }

  private Fetcher getFetcher(ConfigurationDTO dto) {
    return new Fetcher(
        dto.getUserName(),
        dto.getContrastURL(),
        dto.getOrgId(),
        dto.getApiKey(),
        dto.getServiceKey());
  }

  private Date getDate(int refreshCycleValue, Date lastRunTime) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(lastRunTime);
    cal.set(Calendar.SECOND, 0);
    if (refreshCycleValue != 0) cal.add(Calendar.MINUTE, refreshCycleValue);
    return cal.getTime();
  }

  public void cancel() {
    if (Objects.nonNull(timer)) {
      timer.cancel();
    }
  }

  private void showPopup(String source) {
    popupUtil.disposePopup();
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(Constants.CONTRAST).append(":");
    messageBuilder.append(StringUtils.SPACE);
    messageBuilder.append(
        LocalizationUtil.getInstance().getMessage(Constants.MESSAGES.SYNC_PROCESS));
    messageBuilder.append(":");
    messageBuilder.append(StringUtils.SPACE);
    LocalDateTime currentDateTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMMM-yyyy HH:mm:ss");
    String formattedDate = currentDateTime.format(formatter).toUpperCase();
    messageBuilder.append(formattedDate);
    messageBuilder.append(" - ");
    messageBuilder.append(source);
    popupUtil.showFadingPopupOnRootPane(messageBuilder.toString(), PopupUtil.PopupType.INFO);
  }
}
