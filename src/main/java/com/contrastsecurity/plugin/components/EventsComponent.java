/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.components;

import com.contrastsecurity.assess.v3.dto.Event;
import com.contrastsecurity.assess.v3.dto.Line;
import com.contrastsecurity.plugin.models.EventSubMenuDTO;
import com.contrastsecurity.plugin.tree.EventTreeCellRenderer;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class EventsComponent extends JBPanel {
  private JBPanel<?> innerPanel;
  @Getter private JBScrollPane scrollPane;

  public EventsComponent() {
    setLayout(new BorderLayout());
    innerPanel = new JBPanel<>();
    innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
    scrollPane =
        new JBScrollPane(
            innerPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    add(scrollPane, BorderLayout.CENTER);
  }

  /**
   * Builds the Event panel based on the provided vulnerability data
   *
   * @param events event in the vulnerability
   */
  public void buildEventPanel(List<Event> events) {
    innerPanel.removeAll(); // Clear existing components before adding new ones
    innerPanel.add(new JBLabel());
    DefaultMutableTreeNode eventRootNode = new DefaultMutableTreeNode("Events");
    for (Event event : events) {
      EventSubMenuDTO eventDescriptionDTO = new EventSubMenuDTO();
      eventDescriptionDTO.setDescription(event.getDescription());
      eventDescriptionDTO.setType(event.getType());
      DefaultMutableTreeNode descriptionNode = new DefaultMutableTreeNode(eventDescriptionDTO);
      for (Line line : event.getCodeView().getLines()) {
        if (line.getText() != null && !line.getText().isEmpty()) {
          descriptionNode.add(new DefaultMutableTreeNode(line.getText()));
        }
      }
      for (Line line : event.getDataView().getLines()) {
        if (line.getText() != null && !line.getText().isEmpty()) {
          descriptionNode.add(new DefaultMutableTreeNode(line.getText()));
        }
      }
      eventRootNode.add(descriptionNode);
    }
    // Create the JTree and set its properties
    Tree eventTree = getEventTree(eventRootNode);
    // Add the tree to a scroll pane
    JBScrollPane treeScrollPane = new JBScrollPane(eventTree);
    innerPanel.add(treeScrollPane);
  }

  private @NotNull Tree getEventTree(DefaultMutableTreeNode rootNode) {
    TreeModel treeModel = new DefaultTreeModel(rootNode);
    Tree eventTree = new Tree(treeModel);
    eventTree.setRootVisible(true);
    eventTree.setShowsRootHandles(true); // Show expand/collapse icons
    eventTree.setCellRenderer(new EventTreeCellRenderer());
    return eventTree;
  }
}
