/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.models;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class FileVulnerabilitiesDTO {
  private int totalVulnerabilities;
  private List<VulnerabilityDetailsDTO> vulnerabilityDetailsData = new ArrayList<>();

  public void addVulnerabilityDetailsData(VulnerabilityDetailsDTO details) {
    this.vulnerabilityDetailsData.add(details);
  }
}
