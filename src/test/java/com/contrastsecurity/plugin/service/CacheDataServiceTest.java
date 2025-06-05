/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contrastsecurity.assess.v3.dto.FileVulnerabilities;
import com.contrastsecurity.assess.v3.dto.Trace;
import com.contrastsecurity.assess.v3.dto.TraceFile;
import com.contrastsecurity.assess.v3.dto.VulnerabilityDetails;
import com.contrastsecurity.plugin.configuration.ContrastCacheConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CacheDataServiceTest {

  private CacheDataService cacheDataService;

  @BeforeEach
  void setUp() {
    this.cacheDataService = new CacheDataService();
    // Initialize the cache manager before all tests
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
    ContrastCacheConfiguration.setCacheManager(
        cacheManager); // Assume you modify CacheUtil to set the CacheManager
  }

  @AfterEach
  void tearDown() {
    // Ensure the cache manager is closed properly
    cacheDataService.close();
  }

  @Test
  void testCache() {
    // Arrange
    String key = "applicationKey";
    TraceFile traceFile = Mockito.mock(TraceFile.class);
    Map<String, FileVulnerabilities> mockMap = new HashMap<>();
    FileVulnerabilities fileVulnerabilities = mock(FileVulnerabilities.class);
    VulnerabilityDetails vulnsVulnerabilityDetails = mock(VulnerabilityDetails.class);
    Trace trace = mock(Trace.class);
    when(trace.getTitle()).thenReturn("Malware");
    when(vulnsVulnerabilityDetails.getTrace()).thenReturn(trace);
    when(fileVulnerabilities.getVulnerabilityDetailsData())
        .thenReturn(List.of(vulnsVulnerabilityDetails));
    mockMap.put("123", fileVulnerabilities);
    mockMap.put("345", fileVulnerabilities);
    when(traceFile.getFileVulnerabilitiesData()).thenReturn(mockMap);
    // Act
    cacheDataService.addAll(key, traceFile);
    TraceFile retrievedValue = (TraceFile) cacheDataService.get(key);

    // Assert
    assertEquals(
        "Malware",
        retrievedValue
            .getFileVulnerabilitiesData()
            .get("123")
            .getVulnerabilityDetailsData()
            .get(0)
            .getTrace()
            .getTitle());

    // Act
    retrievedValue = (TraceFile) cacheDataService.get("nonExistentKey");
    // Assert
    assertNull(retrievedValue);

    // Assert
    retrievedValue = (TraceFile) cacheDataService.get(key);

    assertEquals(2, retrievedValue.getFileVulnerabilitiesData().size());

    // Act
    cacheDataService.delete(key);

    assertNull(cacheDataService.get(key));

    // clear
    cacheDataService.clear();
  }
}
