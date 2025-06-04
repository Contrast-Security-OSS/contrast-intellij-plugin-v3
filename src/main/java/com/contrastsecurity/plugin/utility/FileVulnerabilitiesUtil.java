/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.utility;

import com.contrastsecurity.assess.v3.dto.Chapter;
import com.contrastsecurity.assess.v3.dto.FileVulnerabilities;
import com.contrastsecurity.assess.v3.dto.Story;
import com.contrastsecurity.assess.v3.dto.Trace;
import com.contrastsecurity.assess.v3.dto.TraceFile;
import com.contrastsecurity.assess.v3.dto.VulnerabilityDetails;
import com.contrastsecurity.plugin.components.AssessComponent;
import com.contrastsecurity.plugin.components.ScanComponent;
import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.models.AnnotationPopupDTO;
import com.contrastsecurity.plugin.models.ScanVulnerabilityDTO;
import com.contrastsecurity.plugin.service.CacheDataService;
import com.contrastsecurity.scan.dto.InnerLocation;
import com.contrastsecurity.scan.dto.Vulnerability;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class FileVulnerabilitiesUtil {

  @Getter private List<Integer> lineNumbers = new ArrayList<>();
  @Getter private Map<Integer, AnnotationPopupDTO> popupDTOMap = new HashMap<>();
  private CacheDataService cacheDataService;
  private String appId;
  private String projectId;
  private ScanComponent scanComponent;
  private AssessComponent assessComponent;

  public FileVulnerabilitiesUtil(String fileName, AssessComponent assessComponent) {
    this.assessComponent = assessComponent;
    cacheDataService = new CacheDataService();
    appId = assessComponent.getAppId();
    loadAssessFileVulnerabilities(fileName.replace("/", "."));
  }

  public FileVulnerabilitiesUtil(String fileName, ScanComponent scanComponent) {
    this.scanComponent = scanComponent;
    cacheDataService = new CacheDataService();
    projectId = scanComponent.getProjectId();
    loadScanFileVulnerabilities(fileName);
  }

  private void loadScanFileVulnerabilities(String fileName) {
    ScanVulnerabilityDTO cacheData = (ScanVulnerabilityDTO) cacheDataService.get(projectId);
    if (cacheData != null) {
      Map<String, List<Vulnerability>> mappedVulnerability = cacheData.getMappedVulnerability();
      for (Map.Entry<String, List<Vulnerability>> entry : mappedVulnerability.entrySet()) {
        String key = entry.getKey();
        if (StringUtils.contains(fileName, key)) {
          List<Vulnerability> value = entry.getValue();
          for (Vulnerability vulnerability : value) {
            int lineNumber = -1;
            for (InnerLocation location : vulnerability.getLocations()) {
              if (location.getPhysicalLocation() != null
                  && location.getPhysicalLocation().getRegion() != null) {
                lineNumber = location.getPhysicalLocation().getRegion().getStartLine() - 1;
                break;
              }
            }
            AnnotationPopupDTO popupDTO = new AnnotationPopupDTO();
            popupDTO.setProjectId(projectId);
            popupDTO.setVulnerabilityId(vulnerability.getId());
            popupDTO.setMessage(vulnerability.getMessage().getText());
            popupDTO.setStatus(vulnerability.getStatus());
            popupDTO.setAdvice(vulnerability.getRisk());
            popupDTO.setSeverity(vulnerability.getSeverity());
            popupDTO.setLastDetected(vulnerability.getLastSeenTime());
            popupDTO.setAdvice(StringUtils.EMPTY);
            if (StringUtils.isNotEmpty(scanComponent.getActionUrl())) {
              popupDTO.setActions(
                  scanComponent.getActionUrl()
                      + Constants.UTILS.VULNERABILITIES_ACTION
                      + vulnerability.getId());
            } else {
              popupDTO.setActions(StringUtils.EMPTY);
            }
            if (popupDTOMap.get(lineNumber) != null) {
              addPopupBasedOnSeverity(popupDTO, lineNumber);
            } else {
              lineNumbers.add(lineNumber);
              popupDTOMap.put(lineNumber, popupDTO);
            }
          }
        }
      }
    }
  }

  private void loadAssessFileVulnerabilities(String currentFile) {
    if (StringUtils.isNotBlank(appId)) {
      TraceFile traceFile = (TraceFile) cacheDataService.get(appId);
      if (traceFile != null) {
        Map<String, FileVulnerabilities> fileVulnerabilitiesData =
            traceFile.getFileVulnerabilitiesData();
        if (MapUtils.isNotEmpty(fileVulnerabilitiesData)) {
          for (Map.Entry<String, FileVulnerabilities> entry : fileVulnerabilitiesData.entrySet()) {
            String key = entry.getKey();
            FileVulnerabilities value = entry.getValue();
            if (StringUtils.contains(currentFile, key)) {
              populateAssessVulnerabilityData(value);
              break;
            }
          }
        }
      }
    } else {
      log.warn("App ID is empty");
    }
  }

  private void populateAssessVulnerabilityData(FileVulnerabilities fileVulnerabilities) {
    if (fileVulnerabilities != null) {
      List<VulnerabilityDetails> vulnerabilityDetailsData =
          fileVulnerabilities.getVulnerabilityDetailsData();
      if (CollectionUtils.isNotEmpty(vulnerabilityDetailsData)) {
        for (VulnerabilityDetails details : vulnerabilityDetailsData) {
          fetchAndPopulateLineNumber(details.getStory(), details.getTrace());
        }
      }
    }
  }

  private void fetchAndPopulateLineNumber(Story story, Trace trace) {
    if (story != null) {
      List<Chapter> chapters = story.getChapters();
      if (CollectionUtils.isNotEmpty(chapters)) {
        for (Chapter chapter : chapters) {
          if (StringUtils.equals(chapter.getType(), Constants.UTILS.LOCATION)) {
            Map<String, String> location = chapter.getBodyFormatVariables();
            String lineNumber = location.get(Constants.UTILS.LINE_NUMBER);
            if (StringUtils.isNotEmpty(lineNumber)) {
              AnnotationPopupDTO dto = constructPopupMessage(trace, story);
              lineNumbers.add(Integer.parseInt(lineNumber) - 1);
              if (popupDTOMap.get(Integer.parseInt(lineNumber) - 1) != null) {
                addPopupBasedOnSeverity(dto, Integer.parseInt(lineNumber) - 1);
              } else {
                popupDTOMap.put(Integer.parseInt(lineNumber) - 1, dto);
              }
            }
          }
        }
      }
    }
  }

  private void addPopupBasedOnSeverity(AnnotationPopupDTO dto, int lineNumber) {
    AnnotationPopupDTO existingDTO = popupDTOMap.get(lineNumber);
    int newDTOLevel = getSeverityLevel(dto.getSeverity());
    int existingDTOLevel = getSeverityLevel(existingDTO.getSeverity());
    if (newDTOLevel > existingDTOLevel) {
      popupDTOMap.put(lineNumber, dto);
    }
  }

  private AnnotationPopupDTO constructPopupMessage(Trace trace, Story story) {
    AnnotationPopupDTO popupDTO = new AnnotationPopupDTO();
    popupDTO.setMessage(trace.getTitle().trim());
    popupDTO.setSeverity(trace.getSeverity().trim());
    popupDTO.setAdvice(story.getRisk().getText().trim());
    popupDTO.setLastDetected(convertLastDetected(trace.getLastTimeSeen()));
    popupDTO.setStatus(trace.getStatus().trim());
    if (StringUtils.isNotBlank(assessComponent.getActionURL())) {
      popupDTO.setActions(
          assessComponent.getActionURL() + Constants.UTILS.VULN_ACTION + trace.getUuid());
    } else {
      popupDTO.setActions(StringUtils.EMPTY);
    }
    return popupDTO;
  }

  private String convertLastDetected(long lastDetected) {
    // Use lastDetected directly as it's already in milliseconds
    LocalDateTime dateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(lastDetected), ZoneId.systemDefault());
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    return dateTime.format(formatter);
  }

  private int getSeverityLevel(String severity) {
    return switch (severity.toLowerCase()) {
      case Constants.UTILS.CRITICAL -> 5;
      case Constants.UTILS.HIGH -> 4;
      case Constants.UTILS.MEDIUM -> 3;
      case Constants.UTILS.LOW -> 2;
      case Constants.UTILS.NOTE -> 1;
      default -> 0;
    };
  }
}
