/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
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
