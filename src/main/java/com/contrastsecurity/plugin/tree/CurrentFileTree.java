/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.tree;

import com.contrastsecurity.plugin.components.ScanComponent;
import com.contrastsecurity.plugin.models.TraceNodeDTO;
import com.contrastsecurity.plugin.utility.TreeUtil;
import com.contrastsecurity.scan.dto.InnerLocation;
import com.contrastsecurity.scan.dto.Vulnerability;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import java.util.List;
import java.util.Map;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

public class CurrentFileTree {

  private final JBPanel<?> panel;
  private final ScanComponent scanComponent;

  public CurrentFileTree(JBPanel<?> panel, ScanComponent scanComponent) {
    this.panel = panel;
    this.scanComponent = scanComponent;
  }

  public void loadScanCurrentFile(
      Map<String, List<Vulnerability>> mappedVulnerability, String openFile, Project project) {
    if (MapUtils.isNotEmpty(mappedVulnerability)) {
      for (Map.Entry<String, List<Vulnerability>> entry : mappedVulnerability.entrySet()) {
        String key = entry.getKey();
        List<Vulnerability> value = entry.getValue();
        if (StringUtils.contains(openFile, key) && CollectionUtils.isNotEmpty(value)) {
          DefaultMutableTreeNode fileNode = TreeUtil.getFileNode(key, value.size());
          addScanCurrentFileVulnerabilityToFileNode(fileNode, value);
          Tree tree = TreeUtil.getCurrentFileTree(fileNode, project);
          addTreeToPanel(tree);
          break;
        } else {
          scanComponent.loadDefaultCurrentFile();
        }
      }
    } else {
      scanComponent.loadDefaultCurrentFile();
    }
  }

  private void addScanCurrentFileVulnerabilityToFileNode(
      DefaultMutableTreeNode fileNode, List<Vulnerability> fileVulnerability) {
    for (Vulnerability vulnerability : fileVulnerability) {
      TraceNodeDTO traceNodeDTO = new TraceNodeDTO();
      traceNodeDTO.setTitle(vulnerability.getMessage().getText());
      traceNodeDTO.setSeverity(vulnerability.getSeverity());
      int lineNumber = -1;
      for (InnerLocation location : vulnerability.getLocations()) {
        if (location.getPhysicalLocation() != null
            && location.getPhysicalLocation().getRegion() != null) {
          lineNumber = location.getPhysicalLocation().getRegion().getStartLine();
          break;
        }
      }
      if (lineNumber > 0) {
        traceNodeDTO.setLineNumber(String.valueOf(lineNumber));
      } else {
        traceNodeDTO.setLineNumber(StringUtils.EMPTY);
      }
      fileNode.add(new DefaultMutableTreeNode(traceNodeDTO));
    }
  }

  private void addTreeToPanel(Tree tree) {
    JBScrollPane treePane = new JBScrollPane(tree);
    treePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    panel.removeAll();
    panel.add(treePane);
    panel.revalidate();
    panel.repaint();
  }
}
