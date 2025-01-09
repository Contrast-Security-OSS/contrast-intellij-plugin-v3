/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.components;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.contrastsecurity.plugin.fetchers.Fetcher;
import com.contrastsecurity.plugin.models.ConfigurationDTO;
import com.contrastsecurity.plugin.persistent.CredentialDetailsService;
import com.contrastsecurity.plugin.service.CacheDataService;
import com.contrastsecurity.plugin.utility.CredentialUtil;
import com.contrastsecurity.plugin.utility.LocalizationUtil;
import com.contrastsecurity.plugin.utility.PopupUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class TagComponent extends JBPanel {

  @Getter private JBScrollPane scrollPane;

  private JBPanel<?> existingTagContainer;
  private JBPanel<?> newTagContainer;
  private JBPanel<?> appliedTagContainer;
  private JBPanel<?> processingContainer;

  private LocalizationUtil localizationUtil;

  private ComboBox existingTagComboBox;
  private JBTextField createField;
  private DefaultTableModel tableModel;
  private JBTable tagsTable;
  private JBScrollPane tableScrollPane;
  private JButton okButton;
  private boolean enable;

  private List<String> tagsInOrg;
  private List<String> tagsInOrgCopy;
  private List<String> tagsInVulnerability;
  private List<String> tagsToRemove;

  private boolean isPopulated;

  private String appId;
  private String traceId;

  private transient SwingWorker<Void, Void> worker;

  private final transient PopupUtil popupUtil;

  public TagComponent(
      List<String> tagsInOrganization, String appID, String traceID, Project project) {
    tagsToRemove = new ArrayList<>();
    isPopulated = false;
    this.tagsInOrgCopy = tagsInOrganization;
    this.tagsInOrg = tagsInOrganization;
    this.appId = appID;
    this.traceId = traceID;
    popupUtil = new PopupUtil(project);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    existingTagContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    newTagContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    appliedTagContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    processingContainer = new JBPanel<>(new FlowLayout(FlowLayout.TRAILING));

    localizationUtil = LocalizationUtil.getInstance();
    createField = new JBTextField();
    okButton = new JButton("Ok");

    String[] columnName = {"Tag"};

    tableModel =
        new DefaultTableModel(columnName, 0) {
          @Override
          public boolean isCellEditable(int row, int column) {
            return false;
          }
        };
    tagsTable = new JBTable(tableModel);
    tagsTable.getTableHeader().setReorderingAllowed(false);
    tagsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tagsTable
        .getSelectionModel()
        .addListSelectionListener(e -> enable = tagsTable.getSelectedRow() >= 0);
    tableScrollPane = new JBScrollPane(tagsTable);
    enable = false;

    configureTagContainers();
    scrollPane = new JBScrollPane(this);
  }

  private void configureTagContainers() {
    configureAppliedTagContainer();
    configureExistingTagContainer();
    configureNewTagContainer();
    configureProcessingContainer();
    add(existingTagContainer);
    add(newTagContainer);
    add(appliedTagContainer);
    add(processingContainer);
  }

  private void configureExistingTagContainer() {
    existingTagContainer.removeAll();
    JBLabel existingTagLabel =
        new JBLabel(localizationUtil.getMessage(Constants.EXISTING_TAG_LABEL));
    existingTagLabel.setPreferredSize(new Dimension(200, 30));
    existingTagContainer.add(existingTagLabel);

    existingTagComboBox = new ComboBox();
    existingTagComboBox.addActionListener(
        e -> {
          if (isPopulated) return;
          Object selectedItem = existingTagComboBox.getSelectedItem();
          if (selectedItem != null) {
            if (tagsToRemove.contains(selectedItem.toString()))
              tagsToRemove.remove(selectedItem.toString());
            if (!doesExistsInTable(selectedItem.toString())) {
              tableModel.addRow(new Object[] {selectedItem.toString()});
              existingTagComboBox.removeItem(selectedItem);
            }
            createField.setText(StringUtils.EMPTY);
            tagsTable.clearSelection();
          }
        });
    loadComboBoxWithTags();
    existingTagComboBox.setPreferredSize(new Dimension(200, 30));
    existingTagContainer.add(existingTagComboBox);
    existingTagContainer.revalidate();
    existingTagContainer.repaint();
  }

  private void configureNewTagContainer() {
    newTagContainer.removeAll();
    JBLabel createLabel = new JBLabel(localizationUtil.getMessage(Constants.CREATE_TAG_LABEL));
    createLabel.setPreferredSize(new Dimension(200, 30));
    newTagContainer.add(createLabel);

    createField.setPreferredSize(new Dimension(200, 30));
    newTagContainer.add(createField);

    JButton createButton = new JButton(localizationUtil.getMessage(Constants.CREATE_BUTTON));
    createButton.addActionListener(e -> createButtonOnClick());
    newTagContainer.add(createButton);

    DefaultActionGroup actions = new DefaultActionGroup();
    AnAction deleteAction =
        new AnAction(ContrastIcons.DELETE_ICON) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            deleteActionOnClick();
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }

          @Override
          public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(enable);
          }
        };
    actions.add(deleteAction);
    ActionToolbar actionToolbar =
        ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actions, true);
    actionToolbar.setTargetComponent(actionToolbar.getComponent());
    newTagContainer.add(actionToolbar.getComponent());

    newTagContainer.revalidate();
    newTagContainer.repaint();
  }

  private void configureAppliedTagContainer() {
    appliedTagContainer.removeAll();

    JBLabel appliedLabel = new JBLabel(localizationUtil.getMessage(Constants.APPLIED_TAG_LABEL));
    appliedLabel.setPreferredSize(new Dimension(200, 30));
    appliedTagContainer.add(appliedLabel);
    loadTableWithAppliedTags();
    tableScrollPane.setPreferredSize(new Dimension(300, 200));
    appliedTagContainer.add(tableScrollPane);
  }

  private void configureProcessingContainer() {
    processingContainer.removeAll();

    okButton.addActionListener(e -> okButtonOnClick());
    processingContainer.add(okButton);

    JButton clearButton = new JButton(localizationUtil.getMessage(Constants.CLEAR_BUTTON));
    clearButton.addActionListener(e -> clearButtonOnClick());
    processingContainer.add(clearButton);

    processingContainer.revalidate();
    processingContainer.repaint();
  }

  private void createButtonOnClick() {
    String inputTag = StringUtils.normalizeSpace(createField.getText()).trim();
    if (inputTag.length() > 72) {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.TAG_LENGTH_EXCEEDED));
    } else if (doesExistsInTable(inputTag)) {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.TAG_ALREADY_APPLIED));
    } else if (tagsInOrg.contains(inputTag)) {
      //
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.SELECT_TAG_FROM_DROPDOWN));
    } else if (StringUtils.isNotEmpty(inputTag)) {
      tableModel.addRow(new Object[] {inputTag});
    }
    createField.setText(StringUtils.EMPTY);
    tagsTable.clearSelection();
  }

  private void deleteActionOnClick() {
    int selectedRow = tagsTable.getSelectedRow();
    if (selectedRow != -1) {
      String selectedTag = tableModel.getValueAt(selectedRow, 0).toString();
      if (tagsInOrgCopy.contains(selectedTag)) {
        existingTagComboBox.removeItem(selectedTag);
        existingTagComboBox.addItem(selectedTag);
      }
      tagsInVulnerability.remove(selectedTag);
      if (!tagsToRemove.contains(selectedTag)) {
        tagsToRemove.add(selectedTag);
      }
      tableModel.removeRow(selectedRow);
      tagsTable.clearSelection();
    }
  }

  private void okButtonOnClick() {
    if (worker != null) {
      worker.cancel(true);
      worker = null;
    }

    ConfigurationDTO dto = CredentialDetailsService.getInstance().getSavedConfigDataByID(appId);
    if (dto != null) {
      dto = CredentialUtil.decryptDTO(dto);
      Fetcher fetcher =
          new Fetcher(
              dto.getUserName(),
              dto.getContrastURL(),
              dto.getOrgId(),
              dto.getApiKey(),
              dto.getServiceKey());
      String requestBody = getTagRequestBody();
      worker =
          new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
              okButton.setEnabled(false);
              if (fetcher.tagVulnerability(requestBody)) {
                showSuccessPopup(
                    localizationUtil.getMessage(Constants.MESSAGES.TAGGED_VULNERABILITY));
              } else {
                showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.FAILED_TO_TAG));
              }
              return null;
            }

            @Override
            protected void done() {
              okButton.setEnabled(true);
              CacheDataService cacheDataService = new CacheDataService();
              cacheDataService.add(
                  traceId + "-" + Constants.UTILS.TAGS_IN_VUL, tagsInVulnerability);
              cacheDataService.add(
                  appId + "-" + Constants.UTILS.EXISTING_TAGS_IN_ORG, tagsInOrgCopy);
            }
          };
    } else {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.FAILED_TO_TAG));
    }
    worker.execute();
  }

  private void clearButtonOnClick() {
    createField.setText(StringUtils.EMPTY);
    tagsTable.clearSelection();
  }

  private void loadTableWithAppliedTags() {
    tableModel.setRowCount(0);
    if (CollectionUtils.isNotEmpty(tagsInVulnerability)) {
      tagsInVulnerability.forEach(
          e -> {
            if (StringUtils.isNotEmpty(e.trim())) {
              tableModel.addRow(new Object[] {StringUtils.normalizeSpace(e.trim())});
              tagsInOrg.remove(e);
            }
          });
    }
  }

  private void loadComboBoxWithTags() {
    isPopulated = true;
    existingTagComboBox.removeAllItems();
    if (CollectionUtils.isNotEmpty(tagsInOrg)) {
      tagsInOrg.forEach(
          e -> {
            if (StringUtils.isNotEmpty(e.trim())) {
              existingTagComboBox.addItem(StringUtils.normalizeSpace(e.trim()));
            }
          });
    }
    isPopulated = false;
  }

  private boolean doesExistsInTable(String tag) {
    for (int i = 0; i < tableModel.getRowCount(); i++) {
      Object valueAt = tableModel.getValueAt(i, 0);
      if (valueAt != null && StringUtils.equals(valueAt.toString(), tag)) {
        return true;
      }
    }
    return false;
  }

  private String getTagRequestBody() {
    StringBuilder builder = new StringBuilder();
    builder.append("{\n");
    builder.append("\"traces_uuid\": [\n");
    builder.append("\"").append(traceId).append("\"");
    builder.append("],\n");
    builder.append("\"tags\": [\n");
    // append new tags
    appendAppliedTags(builder);
    builder.append("],\n");
    builder.append("\"tags_remove\": [\n");
    // append remove tags
    appendRemovedTags(builder);
    builder.append("]\n");
    builder.append("}");
    return builder.toString();
  }

  private void appendAppliedTags(StringBuilder builder) {
    for (int i = 0; i < tableModel.getRowCount(); i++) {
      Object valueAt = tableModel.getValueAt(i, 0);
      if (valueAt != null) {
        if (!tagsInVulnerability.contains(valueAt.toString())) {
          tagsInVulnerability.add(valueAt.toString());
        }
        if (!tagsInOrgCopy.contains(valueAt.toString())) {
          tagsInOrgCopy.add(valueAt.toString());
        }
        builder.append("\"").append(valueAt.toString()).append("\"");
        if (i != tableModel.getRowCount() - 1) {
          builder.append(",");
        }
      }
    }
  }

  private void appendRemovedTags(StringBuilder builder) {
    if (CollectionUtils.isNotEmpty(tagsToRemove)) {
      for (int i = 0; i < tagsToRemove.size(); i++) {
        builder.append("\"").append(tagsToRemove.get(i)).append("\"");
        if (i != tagsToRemove.size() - 1) {
          builder.append(",");
        }
      }
    }
  }

  public void refresh(
      List<String> tagsInVulnerability, List<String> tagsInOrg, String appID, String traceID) {
    tagsToRemove = new ArrayList<>();
    this.traceId = traceID;
    this.appId = appID;
    this.tagsInVulnerability = tagsInVulnerability;
    loadTableWithAppliedTags();
    this.tagsInOrg = tagsInOrg;
    this.tagsInOrgCopy = tagsInOrg;
    loadComboBoxWithTags();
  }

  private void showSuccessPopup(String message) {
    popupUtil.disposePopup();
    popupUtil.showFadingPopupOnRootPane(message, PopupUtil.PopupType.SUCCESS);
  }

  private void showErrorPopup(String message) {
    popupUtil.disposePopup();
    popupUtil.showFadingPopupOnRootPane(message, PopupUtil.PopupType.ERROR);
  }
}
