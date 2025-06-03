/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AnnotationPopupDTO {
  private String projectId;
  private String vulnerabilityId;
  private String message;
  private String severity;
  private String advice;
  private String lastDetected;
  private String status;
  private String actions;
}
