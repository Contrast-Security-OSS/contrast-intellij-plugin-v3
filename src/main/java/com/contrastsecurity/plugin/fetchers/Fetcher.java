/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.fetchers;

import com.contrastsecurity.assess.v3.AssessV3APIClient;
import com.contrastsecurity.assess.v3.dto.AgentSession;
import com.contrastsecurity.assess.v3.dto.AppVersion;
import com.contrastsecurity.assess.v3.dto.Application;
import com.contrastsecurity.assess.v3.dto.MetadataFilter;
import com.contrastsecurity.assess.v3.dto.Server;
import com.contrastsecurity.assess.v3.dto.TraceFile;
import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.models.CustomSessionMetadataDTO;
import com.contrastsecurity.plugin.utility.DeviceDetailsUtil;
import com.contrastsecurity.scan.ScanAPIClient;
import com.contrastsecurity.scan.dto.Project;
import com.contrastsecurity.scan.dto.ProjectVulnerabilities;
import com.contrastsecurity.scan.dto.Vulnerability;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class Fetcher implements Serializable {

  private transient AssessV3APIClient assessV3APIClient;
  private transient ScanAPIClient scanAPIClient;

  public Fetcher(String userName, String url, String orgId, String apiKey, String serviceKey) {

    assessV3APIClient =
        new AssessV3APIClient(
            userName, url, orgId, apiKey, serviceKey, DeviceDetailsUtil.getDetails());
    scanAPIClient =
        new ScanAPIClient(userName, url, orgId, apiKey, serviceKey, DeviceDetailsUtil.getDetails());
  }

  /**
   * Returns Organization Name
   *
   * @return String
   */
  public String getOrgName() {
    try {
      return assessV3APIClient.getOrganizationDetails().getName();
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return StringUtils.EMPTY;
  }

  /**
   * Returns List of Licensed Application Names
   *
   * @return List
   */
  public List<String> getApplicationNames() {
    List<String> appNames = new ArrayList<>();
    try {
      for (Application application : getAllLicensedApplication()) {
        String appName = StringUtils.normalizeSpace(application.getName().trim());
        if (!appNames.contains(appName)) {
          appNames.add(appName);
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return appNames;
  }

  /**
   * Returns List of Project Names
   *
   * @return List
   */
  public List<String> getProjectNames() {
    List<String> projectNames = new ArrayList<>();
    try {
      for (Project project : getProjects()) {
        String projectName = StringUtils.normalizeSpace(project.getName().trim());
        if (!projectNames.contains(projectName)) {
          projectNames.add(projectName);
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return projectNames;
  }

  public boolean markVulnerability(String requestBody) {
    try {
      assessV3APIClient.markVulnerability(requestBody);
      return true;
    } catch (Exception e) {

      return false;
    }
  }

  private List<Project> getProjects() {
    List<Project> projects = new ArrayList<>();
    try {
      projects = scanAPIClient.getProjects();
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return projects;
  }

  /**
   * Returns Application ID based on the given application Name
   *
   * @param appName Application Name
   * @return String Application ID
   */
  public String getApplicationIdByName(String appName) {
    try {
      List<Application> applications = getAllLicensedApplication();
      for (Application application : applications) {
        if (StringUtils.equals(appName, StringUtils.normalizeSpace(application.getName().trim()))) {
          return application.getApp_id();
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return StringUtils.EMPTY;
  }

  /**
   * Returns Project ID based on the provided project name
   *
   * @param projectName {@link String}
   * @return String
   */
  public String getProjectIdByName(String projectName) {
    try {
      for (Project project : getProjects()) {
        if (StringUtils.equals(projectName, StringUtils.normalizeSpace(project.getName().trim()))) {
          return project.getId();
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return StringUtils.EMPTY;
  }

  /**
   * Returns TraceFile based on the provided app id and applied filter
   *
   * @param applicationId app id
   * @param appliedFilter applied filter
   * @return TraceFile
   */
  public TraceFile fetchVulnerabilitiesByFilter(
      String applicationId, Map<String, String> appliedFilter) {
    try {
      return assessV3APIClient.getVulnerabilityWithLineNo(applicationId, appliedFilter);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return null;
  }

  private List<Application> getAllLicensedApplication() {
    List<Application> applications = new ArrayList<>();
    Map<String, String> quickFilter = new HashMap<>();
    quickFilter.put(Constants.API_FILTERS.QUICK_FILTER, Constants.API_FILTERS.LICENSED);
    try {
      applications = assessV3APIClient.getApplicationsByFilter(quickFilter);
      return applications;
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return applications;
  }

  /**
   * Returns List of all the servers based on Application ID
   *
   * @return List of Servers
   */
  public List<Server> getServersByApplicationID(String applicationID) {
    List<String> apps = new ArrayList<>();
    apps.add(applicationID);
    try {
      return assessV3APIClient.getServers(apps);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return new ArrayList<>();
  }

  /**
   * Returns List of all the buildNumbers based on Application ID
   *
   * @return List of buildNumbers
   */
  public List<String> getBuildNumbersByApplicationID(String applicationID) {
    List<String> buildNumbers = new ArrayList<>();
    try {
      List<AppVersion> buildNos = assessV3APIClient.getBuildNumberByApplicationId(applicationID);
      for (AppVersion appVersion : buildNos) {
        buildNumbers.add(appVersion.getKeycode());
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return buildNumbers;
  }

  /**
   * Returns List of Vulnerability based on the provided project id and applied filter
   *
   * @param projectId project id
   * @param appliedFilter applied filter
   * @return List of Vulnerability
   */
  public ProjectVulnerabilities getVulnerabilitiesByAppliedFilter(
      String projectId, Map<String, String> appliedFilter) {
    try {
      return scanAPIClient.getVulnerabilityByProjectIdWithFilter(projectId, appliedFilter);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return null;
  }

  public Vulnerability getProjectVulnerabilityById(String projectId, String vulnerabilityId) {
    try {
      return scanAPIClient.getVulnerabilityByVulId(projectId, vulnerabilityId);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return null;
  }

  public List<String> getAppliedTagForVulnerability(String vulnerabilityId) {
    try {
      return assessV3APIClient.getAllTagsInVulnerabilityByVulId(vulnerabilityId);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return Collections.emptyList();
  }

  public boolean tagVulnerability(String requestBody) {
    try {
      assessV3APIClient.tagVulnerability(requestBody);
      return true;
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return false;
  }

  public AgentSession getMostRecentSession(String appID) {
    try {
      return assessV3APIClient.getAgentSessions(appID);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return null;
  }

  public Map<String, CustomSessionMetadataDTO> getCustomMetadata(
      String appId, String agentSessionId) {
    Map<String, CustomSessionMetadataDTO> customMetadata = new HashMap<>();
    try {
      List<MetadataFilter> metadataFilters =
          assessV3APIClient.postMetadataFilters(
              appId, getCustomSessionRequestBody(appId, agentSessionId));
      if (CollectionUtils.isNotEmpty(metadataFilters)) {
        metadataFilters.forEach(
            sessionMetadata -> {
              String key = sessionMetadata.getAgentLabel();
              CustomSessionMetadataDTO dto = new CustomSessionMetadataDTO();
              dto.setId(sessionMetadata.getId());

              List<String> values = new ArrayList<>();
              sessionMetadata.getValues().forEach(value -> values.add(value.getValueName()));
              dto.setValues(values);

              customMetadata.put(key, dto);
            });
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return customMetadata;
  }

  private String getCustomSessionRequestBody(String appId, String agentSessionId) {
    StringBuilder requestBody = new StringBuilder();
    requestBody.append("{\n");
    requestBody.append("\"agentSessionId\": ");
    requestBody.append("\"").append(agentSessionId).append("\"");
    requestBody.append(",\n");
    requestBody.append("\"quickFilter\" : \"ALL\",\n");
    requestBody.append("\"modules\": [");
    requestBody.append("\"").append(appId).append("\"\n");
    requestBody.append("]\n");
    requestBody.append("}");
    return requestBody.toString();
  }
}
