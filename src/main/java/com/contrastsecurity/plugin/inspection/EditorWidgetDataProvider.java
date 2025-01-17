/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.inspection;

import com.contrastsecurity.assess.v3.dto.FileVulnerabilities;
import com.contrastsecurity.assess.v3.dto.Trace;
import com.contrastsecurity.assess.v3.dto.TraceFile;
import com.contrastsecurity.assess.v3.dto.VulnerabilityDetails;
import com.contrastsecurity.plugin.components.AssessComponent;
import com.contrastsecurity.plugin.components.ScanComponent;
import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.contrastsecurity.plugin.models.ScanVulnerabilityDTO;
import com.contrastsecurity.plugin.service.CacheDataService;
import com.contrastsecurity.scan.dto.Vulnerability;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class EditorWidgetDataProvider {
  private String cacheKey;
  @Getter private String source;
  private Project project;

  public EditorWidgetDataProvider() {}

  public EditorWidgetDataProvider(AssessComponent assessComponent, Project project) {
    String appId = assessComponent.getAppId();
    this.project = project;
    if (StringUtils.isNotEmpty(appId)) {
      cacheKey = appId;
      source = Constants.ASSESS;
    }
  }

  public EditorWidgetDataProvider(ScanComponent scanComponent, Project project) {
    String projectId = scanComponent.getProjectId();
    this.project = project;
    if (StringUtils.isNotEmpty(projectId)) {
      cacheKey = projectId; // need to be changed after implementing scan
      source = Constants.SCAN;
    }
  }

  /**
   * Returns a list of actions based on the current open file
   *
   * @return List of Action
   */
  public List<AnAction> getActions() {
    List<AnAction> actionList = new ArrayList<>();
    CacheDataService cacheDataService = new CacheDataService();
    if (StringUtils.isNotEmpty(cacheKey)) {
      Object cache = cacheDataService.get(cacheKey);
      if (cache instanceof TraceFile) {
        loadAssessCache((TraceFile) cacheDataService.get(cacheKey), actionList);
      } else if (cache instanceof ScanVulnerabilityDTO) {
        loadScanCache((ScanVulnerabilityDTO) cacheDataService.get(cacheKey), actionList);
      }
    }
    return actionList;
  }

  private void loadAssessCache(TraceFile traceFile, List<AnAction> actionList) {
    if (traceFile != null) {
      String currentFile = getCurrentFileName().replace("/", ".");
      Map<String, FileVulnerabilities> tracesByFile = traceFile.getFileVulnerabilitiesData();
      for (Map.Entry<String, FileVulnerabilities> entry : tracesByFile.entrySet()) {
        String key = entry.getKey();
        FileVulnerabilities value = entry.getValue();
        if (StringUtils.contains(currentFile, key)) {
          Map<String, Icon> actionDetails =
              getAssessActionData(value.getVulnerabilityDetailsData());
          for (Map.Entry<String, Icon> actionData : actionDetails.entrySet()) {
            String countKey = actionData.getKey();
            Action action = new Action(getCountFromKey(countKey), actionData.getValue(), source);
            actionList.add(action);
          }
          break;
        }
      }
    }
  }

  private void loadScanCache(ScanVulnerabilityDTO dto, List<AnAction> actionList) {
    if (dto != null) {
      String currentFileName = getCurrentFileName();
      for (Map.Entry<String, List<Vulnerability>> entry : dto.getMappedVulnerability().entrySet()) {
        String key = entry.getKey();
        if (StringUtils.contains(currentFileName, key)) {
          Map<String, Icon> actionDetails = getScanActionData(entry.getValue());
          for (Map.Entry<String, Icon> actionData : actionDetails.entrySet()) {
            String countKey = actionData.getKey();
            Action action = new Action(getCountFromKey(countKey), actionData.getValue(), source);
            actionList.add(action);
          }
          break;
        }
      }
    }
  }

  private String getCountFromKey(String key) {
    return key.substring(key.lastIndexOf(".") + 1);
  }

  private String getCurrentFileName() {
    VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
    if (selectedFiles.length > 0) {
      VirtualFile file = selectedFiles[0];
      return file.getPath();
    }
    return StringUtils.EMPTY;
  }

  private Map<String, Icon> getScanActionData(List<Vulnerability> vulnerabilityList) {
    Map<String, Icon> actionMap = new HashMap<>();
    int criticalCount = 0;
    int highCount = 0;
    int mediumCount = 0;
    int lowCount = 0;
    int noteCount = 0;
    for (Vulnerability vulnerability : vulnerabilityList) {
      String severity = vulnerability.getSeverity();
      switch (severity.toLowerCase()) {
        case Constants.UTILS.CRITICAL:
          criticalCount++;
          break;
        case Constants.UTILS.HIGH:
          highCount++;
          break;
        case Constants.UTILS.MEDIUM:
          mediumCount++;
          break;
        case Constants.UTILS.LOW:
          lowCount++;
          break;
        case Constants.UTILS.NOTE:
          noteCount++;
          break;
        default:
      }
    }
    if (criticalCount > 0) {
      actionMap.put(
          Constants.UTILS.CRITICAL + "." + criticalCount, ContrastIcons.CRITICAL_SEVERITY_ICON);
    }
    if (highCount > 0) {
      actionMap.put(Constants.UTILS.HIGH + "." + highCount, ContrastIcons.HIGH_SEVERITY_ICON);
    }
    if (mediumCount > 0) {
      actionMap.put(Constants.UTILS.MEDIUM + "." + mediumCount, ContrastIcons.MEDIUM_SEVERITY_ICON);
    }
    if (lowCount > 0) {
      actionMap.put(Constants.UTILS.LOW + "." + lowCount, ContrastIcons.LOW_SEVERITY_ICON);
    }
    if (noteCount > 0) {
      actionMap.put(Constants.UTILS.NOTE + "." + noteCount, ContrastIcons.NOTE_SEVERITY_ICON);
    }
    return actionMap;
  }

  private Map<String, Icon> getAssessActionData(List<VulnerabilityDetails> trcaeDetailList) {
    Map<String, Icon> actionMap = new HashMap<>();
    int criticalCount = 0;
    int highCount = 0;
    int mediumCount = 0;
    int lowCount = 0;
    int noteCount = 0;
    for (VulnerabilityDetails details : trcaeDetailList) {
      Trace trace = details.getTrace();
      switch (trace.getSeverity()) {
        case Constants.CHECKBOXES.CRITICAL:
          criticalCount++;
          break;
        case Constants.CHECKBOXES.HIGH:
          highCount++;
          break;
        case Constants.CHECKBOXES.MEDIUM:
          mediumCount++;
          break;
        case Constants.CHECKBOXES.LOW:
          lowCount++;
          break;
        case Constants.CHECKBOXES.NOTE:
          noteCount++;
          break;
        default:
      }
    }
    if (criticalCount > 0) {
      actionMap.put(
          Constants.UTILS.CRITICAL + "." + criticalCount, ContrastIcons.CRITICAL_SEVERITY_ICON);
    }
    if (highCount > 0) {
      actionMap.put(Constants.UTILS.HIGH + "." + highCount, ContrastIcons.HIGH_SEVERITY_ICON);
    }
    if (mediumCount > 0) {
      actionMap.put(Constants.UTILS.MEDIUM + "." + mediumCount, ContrastIcons.MEDIUM_SEVERITY_ICON);
    }
    if (lowCount > 0) {
      actionMap.put(Constants.UTILS.LOW + "." + lowCount, ContrastIcons.LOW_SEVERITY_ICON);
    }
    if (noteCount > 0) {
      actionMap.put(Constants.UTILS.NOTE + "." + noteCount, ContrastIcons.NOTE_SEVERITY_ICON);
    }
    return actionMap;
  }
}
