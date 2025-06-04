/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.constants;

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

public class ContrastIcons {

  private ContrastIcons() {}

  public static final Icon SETTINGS_ICON =
      IconLoader.getIcon("/icons/settings.png", ContrastIcons.class);
  public static final Icon ASSESS_ICON =
      IconLoader.getIcon("icons/assess.png", ContrastIcons.class);
  public static final Icon SCAN_ICON = IconLoader.getIcon("icons/scan.png", ContrastIcons.class);
  public static final Icon EDIT_ICON = IconLoader.getIcon("icons/edit.png", ContrastIcons.class);
  public static final Icon DELETE_ICON =
      IconLoader.getIcon("icons/delete.png", ContrastIcons.class);
  public static final Icon CONNECTED_ICON =
      IconLoader.getIcon("icons/connected.png", ContrastIcons.class);
  public static final Icon CRITICAL_SEVERITY_ICON =
      IconLoader.getIcon("icons/critical.png", ContrastIcons.class);
  public static final Icon HIGH_SEVERITY_ICON =
      IconLoader.getIcon("icons/high.png", ContrastIcons.class);
  public static final Icon MEDIUM_SEVERITY_ICON =
      IconLoader.getIcon("icons/medium.png", ContrastIcons.class);
  public static final Icon LOW_SEVERITY_ICON =
      IconLoader.getIcon("icons/low.png", ContrastIcons.class);
  public static final Icon NOTE_SEVERITY_ICON =
      IconLoader.getIcon("icons/note.png", ContrastIcons.class);
  public static final Icon JAVA_ICON_DARK =
      IconLoader.getIcon("icons/java.png", ContrastIcons.class);
  public static final Icon REFRESH = IconLoader.getIcon("icons/refresh.png", ContrastIcons.class);
  public static final Icon CONTRAST =
      IconLoader.getIcon("icons/toolwindowIcon.png", ContrastIcons.class);
  public static final Icon CONTRAST_DARK =
      IconLoader.getIcon("icons/toolwindowIcon.png", ContrastIcons.class);
  public static final Icon GREEN_SERVERITY =
      IconLoader.getIcon("icons/greenSeverity.png", ContrastIcons.class);
  public static final Icon REDIRECTION_DARK =
      IconLoader.getIcon("icons/export.png", ContrastIcons.class);
  public static final Icon REDIRECTION =
      IconLoader.getIcon("icons/export_dark.png", ContrastIcons.class);
  public static final Icon TAG_ICON = IconLoader.getIcon("icons/tag.png", ContrastIcons.class);
  public static final Icon INFO = IconLoader.getIcon("icons/info.svg", ContrastIcons.class);
}
