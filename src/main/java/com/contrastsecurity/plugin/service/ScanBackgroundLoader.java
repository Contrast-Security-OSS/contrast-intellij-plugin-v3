/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.service;

import com.contrastsecurity.plugin.fetchers.Fetcher;
import com.contrastsecurity.plugin.models.AnnotationPopupDTO;
import com.contrastsecurity.plugin.models.ConfigurationDTO;
import com.contrastsecurity.plugin.persistent.CredentialDetailsService;
import com.contrastsecurity.plugin.utility.CredentialUtil;
import com.contrastsecurity.scan.dto.Vulnerability;
import java.util.Map;
import javax.swing.SwingWorker;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

public class ScanBackgroundLoader {

  private SwingWorker<Void, Void> backGroundWorker;
  private String projectId;
  private Fetcher fetcher;
  private SubMenuCacheService subMenuCacheService;

  public ScanBackgroundLoader(String projectId) {
    this.projectId = projectId;
    subMenuCacheService = new SubMenuCacheService();
    ConfigurationDTO configurationDTO =
        CredentialDetailsService.getInstance().getSavedConfigDataByID(projectId);
    if (configurationDTO != null) {
      configurationDTO = CredentialUtil.decryptDTO(configurationDTO);
      fetcher =
          new Fetcher(
              configurationDTO.getUserName(),
              configurationDTO.getContrastURL(),
              configurationDTO.getOrgId(),
              configurationDTO.getApiKey(),
              configurationDTO.getServiceKey());
    } else {
      fetcher = null;
    }
  }

  public void startBackgroundLoading(Map<Integer, AnnotationPopupDTO> popupDTOMap) {
    if (MapUtils.isNotEmpty(popupDTOMap)) {
      if (backGroundWorker != null) {
        backGroundWorker.cancel(true);
        backGroundWorker = null;
      }
      backGroundWorker =
          new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
              for (Map.Entry<Integer, AnnotationPopupDTO> entry : popupDTOMap.entrySet()) {
                AnnotationPopupDTO value = entry.getValue();
                Object cache =
                    subMenuCacheService.get(
                        value.getProjectId() + "-" + value.getVulnerabilityId());
                if (cache != null) {
                  continue;
                }
                if (StringUtils.equals(projectId, value.getProjectId())) {
                  Vulnerability vulnerability =
                      fetcher.getProjectVulnerabilityById(projectId, value.getVulnerabilityId());
                  if (vulnerability != null) {
                    subMenuCacheService.add(
                        value.getProjectId() + "-" + value.getVulnerabilityId(), vulnerability);
                    value.setAdvice(vulnerability.getRisk());
                  }
                }
              }
              return null;
            }
          };
      backGroundWorker.execute();
    }
  }
}
