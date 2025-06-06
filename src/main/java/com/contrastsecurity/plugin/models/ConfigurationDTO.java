/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ConfigurationDTO {
  private String source;
  private String contrastURL;
  private String userName;
  private String serviceKey;
  private String apiKey;
  private String orgId;
  private String appOrProject;
  private String appOrProjectID;
  private String orgName;
  private int refreshCycleMinutes;
}
