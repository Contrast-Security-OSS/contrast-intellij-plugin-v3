package com.contrastsecurity.plugin.utility;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.models.TraceNodeDTO;
import com.contrastsecurity.plugin.tree.ReportTreeCellRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ALL")
public class TreeUtil {

  private TreeUtil() {}

  public static DefaultMutableTreeNode getRootNode(int totalVulnerabilities) {
    StringBuilder builder = new StringBuilder();
    builder.append(Constants.UTILS.FOUND);
    builder.append(StringUtils.SPACE);
    builder.append(totalVulnerabilities);
    builder.append(StringUtils.SPACE);
    builder.append(Constants.UTILS.ISSUES);
    return new DefaultMutableTreeNode(builder);
  }

  public static DefaultMutableTreeNode getFileNode(String fileName, int totalVulnerabilities) {
    StringBuilder builder = new StringBuilder(fileName);
    builder.append(Constants.UTILS.OPEN_ROUND_BRACKET);
    builder.append(totalVulnerabilities);
    builder.append(StringUtils.SPACE);
    builder.append(Constants.UTILS.ISSUES);
    builder.append(Constants.UTILS.CLOSE_ROUND_BRACKET);
    return new DefaultMutableTreeNode(builder.toString());
  }

  public static Tree getNormalTree(@NotNull DefaultMutableTreeNode rootNode) {
    TreeModel model = new DefaultTreeModel(rootNode);
    Tree tree = new Tree(model);
    tree.setRootVisible(true);
    tree.setShowsRootHandles(true);
    tree.setCellRenderer(new ReportTreeCellRenderer());
    return tree;
  }

  public static Tree getCurrentFileTree(@NotNull DefaultMutableTreeNode node, Project project) {
    TreeModel model = new DefaultTreeModel(node);
    Tree tree = new Tree(model);
    tree.setRootVisible(true);
    tree.setShowsRootHandles(true);
    tree.setCellRenderer(new ReportTreeCellRenderer());
    // Add logic to move to line
    tree.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
              Tree tree = (Tree) e.getSource();
              int row = tree.getRowForLocation(e.getX(), e.getY());
              if (row > 0) {
                DefaultMutableTreeNode selectedNode =
                    (DefaultMutableTreeNode) tree.getPathForRow(row).getLastPathComponent();
                if (selectedNode.getUserObject() instanceof TraceNodeDTO dto) {
                  int lineNumber = Integer.parseInt(dto.getLineNumber());
                  FileNavigator fileNavigator = new FileNavigator(project);
                  fileNavigator.moveToLineNumber(lineNumber);
                }
              }
            }
          }
        });
    return tree;
  }
}
