/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.tree;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.contrastsecurity.plugin.models.EventSubMenuDTO;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
public class EventTreeCellRenderer extends DefaultTreeCellRenderer {
  private transient Icon icon;

  @Override
  public Component getTreeCellRendererComponent(
      JTree tree,
      Object value,
      boolean selected,
      boolean expanded,
      boolean leaf,
      int row,
      boolean hasFocus) {
    Component c =
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    if (row == 0) {
      setIcon(null);
    }
    if (value instanceof DefaultMutableTreeNode node) {
      Object userObject = node.getUserObject();

      // Check if the node contains a EventNode object
      if (userObject instanceof EventSubMenuDTO eventDescriptionDTO) {

        String type = eventDescriptionDTO.getType();
        // Set the icon based on type
        if (type != null) {
          switch (type) {
            case Constants.UTILS.CREATION:
              setIcon(ContrastIcons.MEDIUM_SEVERITY_ICON);
              break;
            case Constants.UTILS.TRIGGER:
              setIcon(ContrastIcons.CRITICAL_SEVERITY_ICON);
              break;
            case Constants.UTILS.TAG:
              setIcon(ContrastIcons.LOW_SEVERITY_ICON);
              break;
            default:
              setIcon(ContrastIcons.GREEN_SERVERITY);
          }
        }
        if (StringUtils.isNotEmpty(eventDescriptionDTO.getDescription())) {
          setText(eventDescriptionDTO.getDescription());
        }
      } else {
        setIcon(null);
      }
    }
    setFocusable(false);
    setBackground(null); // Ensure no background color is applied
    setBackgroundNonSelectionColor(null);
    setBackgroundSelectionColor(null);
    setOpaque(false);

    return c;
  }
}
