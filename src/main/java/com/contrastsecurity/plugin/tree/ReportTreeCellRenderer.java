/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.tree;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.contrastsecurity.plugin.models.TraceNodeDTO;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.apache.commons.lang3.StringUtils;

public class ReportTreeCellRenderer extends DefaultTreeCellRenderer {

  private transient Icon icon;

  public ReportTreeCellRenderer() {}

  public ReportTreeCellRenderer(Icon icon) {
    this.icon = icon;
  }

  @Override
  public Component getTreeCellRendererComponent(
      JTree tree,
      Object value,
      boolean selected,
      boolean expanded,
      boolean leaf,
      int row,
      boolean hasFocus) {

    Component component =
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, false);

    if (row == 0) setIcon(null);

    if (value instanceof DefaultMutableTreeNode node) {
      Object userObject = node.getUserObject();
      if (userObject instanceof TraceNodeDTO dto) {
        setIcon(getSeverityIcon(dto.getSeverity()));
        if (StringUtils.isNotEmpty(dto.getLineNumber())) {
          setText(" (" + dto.getLineNumber() + ") " + dto.getTitle());
        } else {
          setText(StringUtils.SPACE + dto.getTitle());
        }
      }
    } else {
      setIcon(icon);
    }

    setBackground(null); // Ensure no background color is applied
    setBackgroundNonSelectionColor(null);
    setBackgroundSelectionColor(null);
    setOpaque(false);

    return component;
  }

  private Icon getSeverityIcon(String severity) {
    return switch (severity.toLowerCase()) {
      case Constants.UTILS.CRITICAL -> ContrastIcons.CRITICAL_SEVERITY_ICON;
      case Constants.UTILS.HIGH -> ContrastIcons.HIGH_SEVERITY_ICON;
      case Constants.UTILS.MEDIUM -> ContrastIcons.MEDIUM_SEVERITY_ICON;
      case Constants.UTILS.LOW -> ContrastIcons.LOW_SEVERITY_ICON;
      case Constants.UTILS.NOTE -> ContrastIcons.NOTE_SEVERITY_ICON;
      default -> null;
    };
  }
}
