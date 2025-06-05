/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.fetchers;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.contrastsecurity.assess.v3.dto.Server;
import com.contrastsecurity.assess.v3.dto.TraceFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FetchersTest {

  @Mock public Fetcher fetcher;

  @Test
  void testGetOrgName_Success() {
    when(fetcher.getOrgName()).thenReturn("TestOrg");
    String orgName = fetcher.getOrgName();
    Assertions.assertNotEquals(StringUtils.EMPTY, orgName);
    Assertions.assertEquals("TestOrg", orgName);
  }

  @Test
  void testGetOrgName_Failure() {
    when(fetcher.getOrgName()).thenReturn(StringUtils.EMPTY);
    String orgName = fetcher.getOrgName();
    Assertions.assertEquals(StringUtils.EMPTY, orgName);
  }

  @Test
  void testGetApplicationNames_Success() {
    when(fetcher.getApplicationNames()).thenReturn(List.of("App1", "App2", "App3"));
    List<String> applicationNames = fetcher.getApplicationNames();
    Assertions.assertEquals(3, applicationNames.size());
  }

  @Test
  void testGetApplicationNames_Failure() {
    when(fetcher.getApplicationNames()).thenReturn(new ArrayList<>());
    List<String> applicationNames = fetcher.getApplicationNames();
    Assertions.assertEquals(0, applicationNames.size());
  }

  @Test
  void testGetProjectNames_Success() {
    when(fetcher.getProjectNames()).thenReturn(List.of("Project1", "Project2", "Project3"));
    List<String> projectNames = fetcher.getProjectNames();
    Assertions.assertEquals(3, projectNames.size());
  }

  @Test
  void testGetProjectNames_Failure() {
    when(fetcher.getProjectNames()).thenReturn(new ArrayList<>());
    List<String> projectNames = fetcher.getProjectNames();
    Assertions.assertEquals(0, projectNames.size());
  }

  @Test
  void testGetApplicationIdByName_Success() {
    when(fetcher.getApplicationIdByName(anyString())).thenReturn("appId1234");
    String appID = fetcher.getApplicationIdByName(anyString());
    Assertions.assertNotEquals(StringUtils.EMPTY, appID);
  }

  @Test
  void testGetApplicationIdByName_Failure() {
    when(fetcher.getApplicationIdByName(anyString())).thenReturn(StringUtils.EMPTY);
    String appName = "app1";
    String appID = fetcher.getApplicationIdByName(appName);
    Assertions.assertEquals(StringUtils.EMPTY, appID);
  }

  @Test
  void testFetchVulnerabilitiesByFilter_Success() {

    TraceFile traceFile = new TraceFile();
    traceFile.setTotalVulnerabilities(5);
    traceFile.setFileVulnerabilitiesData(new HashMap<>());
    traceFile.setUnMappedTrace(new ArrayList<>());

    when(fetcher.fetchVulnerabilitiesByFilter(anyString(), anyMap())).thenReturn(traceFile);

    Map<String, String> filterMap = new HashMap<>();
    String appId = "123";
    TraceFile traceFile1 = fetcher.fetchVulnerabilitiesByFilter(appId, filterMap);
    Assertions.assertEquals(5, traceFile1.getTotalVulnerabilities());
  }

  @Test
  void testFetchVulnerabilitiesByFilter_Failure() {
    when(fetcher.fetchVulnerabilitiesByFilter(anyString(), anyMap())).thenReturn(null);
    Map<String, String> filterMap = new HashMap<>();
    String appId = "123";
    TraceFile traceFile = fetcher.fetchVulnerabilitiesByFilter(appId, filterMap);
    Assertions.assertEquals(null, traceFile);
  }

  @Test
  void testGetServersByApplicationID_Success() {

    List<Server> servers = new ArrayList<>();
    when(fetcher.getServersByApplicationID(anyString())).thenReturn(servers);
    List<Server> servers1 = fetcher.getServersByApplicationID("appId");
    Assertions.assertEquals(0, servers1.size());
  }

  @Test
  void testGetServersByApplicationID_Failure() {
    when(fetcher.getServersByApplicationID(anyString())).thenReturn(new ArrayList<>());
    List<Server> servers = fetcher.getServersByApplicationID(anyString());
    Assertions.assertEquals(0, servers.size());
  }

  @Test
  void testGetBuildNumbersByApplicationID_Success() {
    when(fetcher.getBuildNumbersByApplicationID(anyString()))
        .thenReturn(List.of("buildNo1", "buildNo2", "buildNo3"));
    List<String> buildNumbers = fetcher.getBuildNumbersByApplicationID(anyString());
    Assertions.assertEquals(3, buildNumbers.size());
  }

  @Test
  void testGetBuildNumbersByApplicationID_Failure() {
    when(fetcher.getBuildNumbersByApplicationID(anyString())).thenReturn(new ArrayList<>());
    List<String> buildNumbers = fetcher.getBuildNumbersByApplicationID(anyString());
    Assertions.assertEquals(0, buildNumbers.size());
  }
}
