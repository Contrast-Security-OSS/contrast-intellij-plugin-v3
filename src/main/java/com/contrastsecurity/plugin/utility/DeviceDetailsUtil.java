/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.utility;

import com.contrastsecurity.DeviceDetails;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;

public class DeviceDetailsUtil {

  private static final String PLUGIN_ID = "com.contrastsecurity.ide";

  private static DeviceDetails deviceDetails;

  private DeviceDetailsUtil() {}

  public static DeviceDetails getDetails() {
    if (deviceDetails == null) {
      deviceDetails =
          new DeviceDetails(
              getOsName(),
              getOsVersion(),
              getPluginName(),
              getPluginVersion(),
              getIDEVersion(),
              getHtmlContent(),
              getIDEName());
    }
    return deviceDetails;
  }

  public static String getOsName() {
    return System.getProperty("os.name");
  }

  public static String getOsVersion() {
    return System.getProperty("os.version");
  }

  public static String getPluginName() {
    return PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID)).getName();
  }

  public static String getPluginVersion() {
    return PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID)).getVersion();
  }

  public static String getIDEName() {
    String edition;
    String platformName = ApplicationInfo.getInstance().getFullApplicationName();
    String buildNumber = ApplicationInfo.getInstance().getBuild().asString();
    if (buildNumber.contains("IC")) {
      edition = "Community Edition";
    } else {
      edition = "Ultimate Edition";
    }
    return platformName + " " + edition;
  }

  public static String getIDEVersion() {
    return ApplicationInfo.getInstance().getFullVersion();
  }

  public static String getHtmlContent() {
    return PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID)).getDescription();
  }
}
