/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.models;

import com.contrastsecurity.assess.v3.dto.Event;
import com.contrastsecurity.assess.v3.dto.HowToFixVulnerability;
import com.contrastsecurity.assess.v3.dto.Recommendation;
import com.contrastsecurity.assess.v3.dto.Story;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class SubMenuDTO {
  private String firstSeen;
  private String lastSeen;
  private String traceID;
  private String appID;
  private String redirectionURL;
  private Story story;
  private HowToFixVulnerability howToFixVulnerability;
  private Recommendation recommendation;
  private List<Event> event;
}
