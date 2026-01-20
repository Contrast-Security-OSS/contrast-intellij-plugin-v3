/*******************************************************************************
 * Copyright © 2026 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.service;

import com.contrastsecurity.plugin.fetchers.Fetcher;
import com.contrastsecurity.plugin.models.AnnotationPopupDTO;
import com.contrastsecurity.plugin.models.ConfigurationDTO;
import com.contrastsecurity.plugin.persistent.CredentialDetailsService;
import com.contrastsecurity.plugin.utility.CredentialUtil;
import com.contrastsecurity.scan.dto.Vulnerability;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.Map;
import javax.swing.SwingWorker;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

public class ScanBackgroundLoader {

  private SwingWorker<Void, Void> backGroundWorker;
  private final String projectId;
  private final Fetcher fetcher;
  private final SubMenuCacheService subMenuCacheService;

  public ScanBackgroundLoader(String projectId) {
    this.projectId = projectId;
    this.subMenuCacheService = new SubMenuCacheService();
    ConfigurationDTO configurationDTO = CredentialDetailsService.getInstance().getSavedConfigDataByID(projectId);
    if (configurationDTO != null) {
      configurationDTO = CredentialUtil.decryptDTO(configurationDTO);
      this.fetcher = new Fetcher(
              configurationDTO.getUserName(),
              configurationDTO.getContrastURL(),
              configurationDTO.getOrgId(),
              configurationDTO.getApiKey(),
              configurationDTO.getServiceKey()
      );
    } else {
      this.fetcher = null;
    }
  }

  public void startBackgroundLoading(Map<VirtualFile, Map<Integer, AnnotationPopupDTO>> hoverPopupMap) {
    if (MapUtils.isEmpty(hoverPopupMap) || fetcher == null) {
      return;
    }
    if (backGroundWorker != null) {
      backGroundWorker.cancel(true);
      backGroundWorker = null;
    }
    backGroundWorker =
            new SwingWorker<>() {
              @Override
              protected Void doInBackground() throws Exception {
                for (Map<Integer, AnnotationPopupDTO> fileMap : hoverPopupMap.values()) {
                  if (MapUtils.isEmpty(fileMap)) continue;
                  for (AnnotationPopupDTO dto : fileMap.values()) {
                    Object cache = subMenuCacheService.get(dto.getProjectId() + "-" + dto.getVulnerabilityId());
                    if (cache != null) {
                      continue;
                    }
                    if (!StringUtils.equals(projectId, dto.getProjectId())) {
                      continue;
                    }
                    Vulnerability vulnerability = fetcher.getProjectVulnerabilityById(projectId, dto.getVulnerabilityId());
                    if (vulnerability != null) {
                      subMenuCacheService.add(dto.getProjectId() + "-" + dto.getVulnerabilityId(), vulnerability);
                      dto.setAdvice(vulnerability.getRisk());
                    }
                  }
                }
                return null;
              }
            };
    backGroundWorker.execute();
  }
}
