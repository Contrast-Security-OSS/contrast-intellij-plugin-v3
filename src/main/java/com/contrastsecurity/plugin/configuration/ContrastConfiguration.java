/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.configuration;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nullable;

public class ContrastConfiguration implements Configurable {

  private ContrastConfigurationGUI configurationGUI;

  public ContrastConfiguration() {
    configurationGUI = ContrastConfigurationGUI.getInstance();
  }

  @Override
  public @NlsContexts.ConfigurableName String getDisplayName() {
    return "Contrast";
  }

  @Override
  public @Nullable JComponent createComponent() {
    return configurationGUI;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() {
    configurationGUI.setEnableApply(false);
  }

  @Override
  public void reset() {
    apply();
  }

  @Override
  public void disposeUIResources() {
    configurationGUI = null;
  }
}
