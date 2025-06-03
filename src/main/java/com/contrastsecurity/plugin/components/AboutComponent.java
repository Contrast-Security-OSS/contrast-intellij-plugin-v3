/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.components;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.utility.DeviceDetailsUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.table.JBTable;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.lang3.StringUtils;

public class AboutComponent extends JBPanel {

  private static final String[] columns = {StringUtils.EMPTY, StringUtils.EMPTY};

  private final String[][] details;

  private final JBPanel<?> firstHeadingContainer;
  private final JBPanel<?> detailsContainer;
  private final JBPanel<?> secondHeadingContainer;
  private final JBPanel<?> contentContainer;

  public AboutComponent() {
    firstHeadingContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    detailsContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    secondHeadingContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    contentContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));

    details =
        new String[][] {
          {Constants.PLUGIN_NAME, DeviceDetailsUtil.getPluginName()},
          {Constants.PLUGIN_RELEASE_VERSION, DeviceDetailsUtil.getPluginVersion()},
          {Constants.IDE_VERSION, DeviceDetailsUtil.getIDEVersion()},
          {
            Constants.OS_VERSION,
            DeviceDetailsUtil.getOsName() + "-" + DeviceDetailsUtil.getOsVersion()
          },
          // {Constants.DESCRIPTION, DeviceDetailsUtil.getPluginName()},
          {Constants.PLATFORM, DeviceDetailsUtil.getIDEName()}
        };

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    configureAboutContainer();
  }

  private void configureAboutContainer() {
    configureFirstHeadingContainer();
    configureDetailsContainer();
    configureContentContainer();

    add(firstHeadingContainer);
    add(detailsContainer);
    add(secondHeadingContainer);
    add(contentContainer);
  }

  private void configureFirstHeadingContainer() {
    JBLabel headingLabel =
        new JBLabel(
            Constants.UTILS.OPEN_HTML
                + "<h2>"
                + Constants.CONTRAST_PLUGIN
                + "</h2>"
                + Constants.UTILS.CLOSE_HTML);
    firstHeadingContainer.add(headingLabel);
  }

  private void configureDetailsContainer() {
    DefaultTableModel tableModel =
        new DefaultTableModel(details, columns) {
          @Override
          public boolean isCellEditable(int row, int column) {
            return false;
          }
        };
    JBTable detailTable = new JBTable(tableModel);
    detailTable.setRowSelectionAllowed(false);
    detailTable.setColumnSelectionAllowed(false);
    detailTable.setCellSelectionEnabled(false);
    detailTable.setFocusable(false);
    detailTable.setRowHeight(40);
    detailTable.setPreferredSize(new Dimension(500, 200));
    Border tableBorder = BorderFactory.createLineBorder(JBColor.BLACK);
    detailTable.setGridColor(JBColor.BLACK);
    detailTable.setBorder(tableBorder);
    detailsContainer.add(detailTable);
  }

  private void configureContentContainer() {
    JBLabel contentLabel =
        new JBLabel(
            Constants.UTILS.OPEN_HTML
                + "<h2>"
                + Constants.ABOUT_PLUGIN
                + "</h2>"
                + DeviceDetailsUtil.getHtmlContent()
                + Constants.UTILS.CLOSE_HTML);

    contentContainer.add(contentLabel);
  }
}
