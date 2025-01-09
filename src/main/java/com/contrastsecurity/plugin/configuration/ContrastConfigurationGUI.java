/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.configuration;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.constants.ContrastIcons;
import com.contrastsecurity.plugin.fetchers.Fetcher;
import com.contrastsecurity.plugin.models.ConfigurationDTO;
import com.contrastsecurity.plugin.persistent.CredentialDetailsService;
import com.contrastsecurity.plugin.toolwindow.ContrastToolWindow;
import com.contrastsecurity.plugin.utility.CredentialUtil;
import com.contrastsecurity.plugin.utility.LocalizationUtil;
import com.contrastsecurity.plugin.utility.PopupUtil;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class ContrastConfigurationGUI extends JBPanel {

  // Singleton structure
  private static ContrastConfigurationGUI configurationGUI;
  private final transient PopupUtil popupUtil;

  // Containers
  private JBPanel<?> sourceContainer;
  private JBPanel<?> contrastURLContainer;
  private JBPanel<?> userNameContainer;
  private JBPanel<?> serviceKeyContainer;
  private JBPanel<?> apiKeyContainer;
  private JBPanel<?> orgIdContainer;
  private JBPanel<?> retrieveContainer;
  private JBPanel<?> appOrProjectContainer;
  private JBPanel<?> refreshCycleContainer;
  private JBPanel<?> editingContainer;
  private JBPanel<?> orgTableContainer;
  private JBPanel<?> processingContainer;

  // Container Labels
  private JBLabel sourceLabel;
  private JBLabel contrastUrlLabel;
  private JBLabel userNameLabel;
  private JBLabel serviceKeyLabel;
  private JBLabel apiKeyLabel;
  private JBLabel orgIdLabel;
  private JBLabel appOrProjectLabel;
  private JBLabel vulnerabilityRefreshCycleLabel;
  private JBLabel minutesLabel;

  // Input Components
  private ComboBox<String> sourceComboBox;
  private JBTextField urlField;
  private JBTextField userNameField;
  private JBPasswordField serviceKeyField;
  private JBPasswordField apiKeyField;
  private JBTextField orgIdField;
  private ComboBox<String> appOrPojectsComboBox;
  private JBTextField minutesField;

  // Processing Components
  private JButton retrieveButton;
  private JButton addButton;
  private JButton cancelButton;

  private JBTable orgTable;
  private JBScrollPane scrollPane;
  private DefaultTableModel model;

  // Flags for Dynamic UI
  @Getter @Setter private boolean isAssess;
  private boolean enableAction;
  private boolean isEdited;
  @Getter @Setter private boolean enableApply;

  private LocalizationUtil localizationUtil;
  private transient CredentialDetailsService credentialDetailsService;

  private ContrastConfigurationGUI() {

    popupUtil = new PopupUtil(ProjectManager.getInstance().getOpenProjects()[0]);

    String[] sourceArray = new String[] {Constants.ASSESS, Constants.SCAN};

    localizationUtil = LocalizationUtil.getInstance();
    credentialDetailsService = CredentialDetailsService.getInstance();

    // Init Containers
    sourceContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    contrastURLContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    userNameContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    serviceKeyContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    apiKeyContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    orgIdContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    retrieveContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    appOrProjectContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    refreshCycleContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    editingContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    orgTableContainer = new JBPanel<>(new BorderLayout());
    processingContainer = new JBPanel<>(new FlowLayout(FlowLayout.TRAILING));

    // Init Labels
    sourceLabel = new JBLabel(localizationUtil.getMessage(Constants.SOURCE_LABEL));
    contrastUrlLabel = new JBLabel(localizationUtil.getMessage(Constants.CONTRAST_URL_LABEL));
    userNameLabel = new JBLabel(localizationUtil.getMessage(Constants.USER_NAME_LABEL));
    serviceKeyLabel = new JBLabel(localizationUtil.getMessage(Constants.SERVICE_KEY_LABEL));
    apiKeyLabel = new JBLabel(localizationUtil.getMessage(Constants.API_KEY_LABEL));
    orgIdLabel = new JBLabel(localizationUtil.getMessage(Constants.ORG_ID_LABEL));
    appOrProjectLabel = new JBLabel(localizationUtil.getMessage(Constants.APP_NAME_LABEL));
    String labelText = localizationUtil.getMessage(Constants.VULNERABILITY_REFRESH_CYCLE_LABEL);
    int firstSpaceIndex = labelText.indexOf(StringUtils.SPACE);
    if (firstSpaceIndex != -1) {
      String part1 = labelText.substring(0, firstSpaceIndex);
      String part2 = labelText.substring(firstSpaceIndex + 1);
      String formattedLabelText =
          Constants.UTILS.OPEN_HTML
              + part1
              + Constants.UTILS.BR
              + part2
              + Constants.UTILS.CLOSE_HTML;
      vulnerabilityRefreshCycleLabel = new JBLabel(formattedLabelText);
    } else {
      vulnerabilityRefreshCycleLabel = new JBLabel(labelText);
    }
    minutesLabel = new JBLabel(localizationUtil.getMessage(Constants.MINUTES_LABEL));

    // Init Input components
    sourceComboBox = new ComboBox<>(sourceArray);
    urlField = new JBTextField();
    userNameField = new JBTextField();
    serviceKeyField = new JBPasswordField();
    apiKeyField = new JBPasswordField();
    orgIdField = new JBTextField();
    appOrPojectsComboBox = new ComboBox<>();
    minutesField = new JBTextField();

    // Init Processing Components
    retrieveButton = new JButton(localizationUtil.getMessage(Constants.RETRIEVE_BUTTON));
    addButton = new JButton(localizationUtil.getMessage(Constants.ADD_BUTTON));
    cancelButton = new JButton(localizationUtil.getMessage(Constants.CANCEL_BUTTON));

    // Init table
    String[] columnName = {
      localizationUtil.getMessage(Constants.TITLE.ORGANIZATION),
      localizationUtil.getMessage(Constants.TITLE.CONFIGURED_DETAILS),
      localizationUtil.getMessage(Constants.TITLE.TYPE)
    };

    // Set Table not editable
    model =
        new DefaultTableModel(columnName, 0) {
          @Override
          public boolean isCellEditable(int row, int column) {
            // All cells are not editable
            return false;
          }
        };
    orgTable = new JBTable(model);
    orgTable.getTableHeader().setReorderingAllowed(false);
    orgTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    scrollPane = new JBScrollPane(orgTable);

    // Init flags
    isAssess = true;
    enableAction = false;
    isEdited = false;
    enableApply = false;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    configureMainPanel();
    initPlaceHolder();
    loadPersistedDataToTable();
    ApplicationManager.getApplication()
        .getMessageBus()
        .connect()
        .subscribe(
            LafManagerListener.TOPIC,
            (LafManagerListener)
                source -> SwingUtilities.updateComponentTreeUI(ContrastConfigurationGUI.this));
  }

  /**
   * Returns a Singleton instance of ContrastConfigurationGUI class
   *
   * @return ContrastConfigurationGUI
   */
  public static synchronized ContrastConfigurationGUI getInstance() {
    if (configurationGUI == null) {
      configurationGUI = new ContrastConfigurationGUI();
    }
    return configurationGUI;
  }

  private void configureMainPanel() {

    // Source
    configureSourceContainer();
    add(sourceContainer);

    // Contrast URL
    configureUrlContainer();
    add(contrastURLContainer);

    // UserName
    configureUserNameContainer();
    add(userNameContainer);

    // Org ID
    configureOrgIdContainer();
    add(orgIdContainer);

    // API Key
    configureApiKeyContainer();
    add(apiKeyContainer);

    // Service Key
    configureServiceKeyContainer();
    add(serviceKeyContainer);

    // Retrieve
    configureRetrieveContainer();
    add(retrieveContainer);

    // Project
    configureProjectContainer();
    add(appOrProjectContainer);

    // Refresh Cycle
    configureRefreshCycleContainer();
    add(refreshCycleContainer);

    // Editing Container
    configureEditingContainer();
    add(editingContainer);

    // Org Table
    configureOrgTableContainer();
    add(orgTableContainer);

    // Processing
    configureProcessingContainer();
    add(processingContainer);
  }

  private void configureSourceContainer() {
    sourceLabel.setPreferredSize(new Dimension(150, 30));
    sourceComboBox.setPreferredSize(new Dimension(150, 30));

    // Dynamic UI change based on selected source
    sourceComboBox.addActionListener(
        e -> {
          isAssess =
              Objects.requireNonNull(sourceComboBox.getSelectedItem())
                  .toString()
                  .equals(Constants.ASSESS);
          if (isAssess) {
            appOrProjectLabel.setText(localizationUtil.getMessage(Constants.APP_NAME_LABEL));
            appOrPojectsComboBox.removeAllItems();
          } else {
            appOrProjectLabel.setText(localizationUtil.getMessage(Constants.PROJECT_NAME_LABEL));
            appOrPojectsComboBox.removeAllItems();
          }
        });
    sourceContainer.add(sourceLabel);
    sourceContainer.add(sourceComboBox);
  }

  private void configureUrlContainer() {
    contrastUrlLabel.setPreferredSize(new Dimension(150, 30));
    urlField.setPreferredSize(new Dimension(450, 30));
    contrastURLContainer.add(contrastUrlLabel);
    contrastURLContainer.add(urlField);
  }

  private void configureUserNameContainer() {
    userNameLabel.setPreferredSize(new Dimension(150, 30));
    userNameField.setPreferredSize(new Dimension(450, 30));
    userNameContainer.add(userNameLabel);
    userNameContainer.add(userNameField);
  }

  private void configureServiceKeyContainer() {
    serviceKeyLabel.setPreferredSize(new Dimension(150, 30));
    serviceKeyField.setPreferredSize(new Dimension(450, 30));
    serviceKeyContainer.add(serviceKeyLabel);
    serviceKeyContainer.add(serviceKeyField);
  }

  private void configureApiKeyContainer() {
    apiKeyLabel.setPreferredSize(new Dimension(150, 30));
    apiKeyField.setPreferredSize(new Dimension(450, 30));
    apiKeyContainer.add(apiKeyLabel);
    apiKeyContainer.add(apiKeyField);
  }

  private void configureOrgIdContainer() {
    orgIdLabel.setPreferredSize(new Dimension(150, 30));
    orgIdField.setPreferredSize(new Dimension(450, 30));
    orgIdContainer.add(orgIdLabel);
    orgIdContainer.add(orgIdField);
  }

  private void configureRetrieveContainer() {
    JBLabel label = new JBLabel(StringUtils.EMPTY); // Empty label for alignment
    label.setPreferredSize(new Dimension(450, 30));
    retrieveContainer.add(label);
    retrieveButton.setPreferredSize(new Dimension(150, 30));
    retrieveButton.addActionListener(actionEvent -> retrieveButtonOnClick());
    retrieveContainer.add(retrieveButton);
  }

  private void configureProjectContainer() {
    appOrProjectLabel.setPreferredSize(new Dimension(150, 30));
    appOrPojectsComboBox.setPreferredSize(new Dimension(250, 30));
    appOrProjectContainer.add(appOrProjectLabel);
    appOrProjectContainer.add(appOrPojectsComboBox);
    appOrPojectsComboBox.addActionListener(
        actionEvent -> {
          addButton.setEnabled(true);
          cancelButton.setEnabled(true);
        });
  }

  private void configureRefreshCycleContainer() {
    vulnerabilityRefreshCycleLabel.setPreferredSize(new Dimension(150, 30));
    minutesField.setPreferredSize(new Dimension(50, 30));
    minutesField.setText(Constants.DEFAULT_VR_REFRESH_CYCLE_VALUE);
    minutesLabel.setPreferredSize(new Dimension(150, 30));
    refreshCycleContainer.add(vulnerabilityRefreshCycleLabel);
    refreshCycleContainer.add(minutesField);
    refreshCycleContainer.add(minutesLabel);
  }

  private void configureEditingContainer() {
    JBLabel label = new JBLabel(StringUtils.EMPTY); // Empty label for Alignment
    label.setPreferredSize(new Dimension(530, 30));
    editingContainer.add(label);
    DefaultActionGroup actions = new DefaultActionGroup();
    AnAction editAction =
        new AnAction(ContrastIcons.EDIT_ICON) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            editActionOnClick();
          }

          @Override
          public void update(@NotNull AnActionEvent e) {
            // Enable or disable the action based on the flag
            e.getPresentation().setEnabled(enableAction);
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    editAction
        .getTemplatePresentation()
        .setText(localizationUtil.getMessage(Constants.TOOL_TIPS.EDIT));
    AnAction deleteAction =
        new AnAction(ContrastIcons.DELETE_ICON) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            deleteActionOnClick();
            refreshApplications();
          }

          @Override
          public void update(@NotNull AnActionEvent e) {
            // Enable or disable the action based on the flag
            e.getPresentation().setEnabled(enableAction);
          }

          @Override
          public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
          }
        };
    deleteAction
        .getTemplatePresentation()
        .setText(localizationUtil.getMessage(Constants.TOOL_TIPS.DELETE));
    // Add action to actions group
    actions.add(editAction);
    actions.add(deleteAction);
    // Convert actions to action toolbar
    ActionToolbar actionToolbar =
        ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actions, true);
    actionToolbar.setTargetComponent(actionToolbar.getComponent());
    editingContainer.add(actionToolbar.getComponent());
  }

  private void configureOrgTableContainer() {
    orgTableContainer.add(scrollPane, BorderLayout.CENTER);
    orgTable
        .getSelectionModel()
        .addListSelectionListener(e -> enableAction = orgTable.getSelectedRow() != -1);
    JBLabel label = new JBLabel(StringUtils.EMPTY); // Empty label for alignment
    label.setPreferredSize(new Dimension(115, 30));
    orgTableContainer.add(label, BorderLayout.EAST);
  }

  private void configureProcessingContainer() {
    addButton.setPreferredSize(new Dimension(150, 30));
    cancelButton.setPreferredSize(new Dimension(150, 30));
    addButton.setEnabled(false);
    addButton.addActionListener(actionEvent -> addButtonOnClick());
    cancelButton.addActionListener(actionEvent -> resetConfigurationScreen());
    processingContainer.add(addButton);
    processingContainer.add(cancelButton);
  }

  private void retrieveButtonOnClick() {
    if (isInputFieldsEmpty()) {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.EMPTY_INPUTS_MESSAGE));
    } else {
      if (StringUtils.equals(
          Objects.requireNonNull(sourceComboBox.getSelectedItem()).toString(), Constants.ASSESS)) {
        // Assess
        retrieveApplications();
      } else {
        // Scan
        retrieveProjects();
      }
    }
  }

  private void addButtonOnClick() {
    if (isUserInputValid()) {
      // Save or Update configuration
      ConfigurationDTO dto = getConfigDTOWithAppOrProjectID();
      if (StringUtils.isNotEmpty(dto.getAppOrProjectID())
          && StringUtils.isNotEmpty(dto.getOrgName())) {
        if (isEdited) {
          int selectedRow = orgTable.getSelectedRow();
          int rowCount = model.getRowCount();
          boolean otherRowExists = false;
          int otherRowIndex = 0;
          for (int i = 0; i < rowCount; i++) {
            if (i != selectedRow
                && StringUtils.equals(model.getValueAt(i, 1).toString(), dto.getAppOrProject())
                && StringUtils.equals(model.getValueAt(i, 2).toString(), dto.getSource())) {
              otherRowExists = true;
              otherRowIndex = i;
            }
          }
          if (otherRowExists) {
            ConfigurationDTO otherRowDTO =
                credentialDetailsService.getSavedConfigDataByName(
                    model.getValueAt(otherRowIndex, 1).toString(),
                    model.getValueAt(otherRowIndex, 2).toString());
            if (StringUtils.equals(dto.getSource(), otherRowDTO.getSource())) {
              addButton.setText(localizationUtil.getMessage(Constants.ADD_BUTTON));
              isEdited = false;
              appOrPojectsComboBox.removeAllItems();
              showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.ALREADY_EXISTS));
            } else {
              update(dto);
              minutesField.setText(Constants.DEFAULT_VR_REFRESH_CYCLE_VALUE);
            }
          } else {
            update(dto);
            minutesField.setText(Constants.DEFAULT_VR_REFRESH_CYCLE_VALUE);
          }
        } else {
          if (credentialDetailsService.doesConfigExists(dto.getAppOrProject(), dto.getSource())) {
            showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.ALREADY_EXISTS));
          } else {
            save(dto);
            minutesField.setText(Constants.DEFAULT_VR_REFRESH_CYCLE_VALUE);
          }
          appOrPojectsComboBox.removeAllItems();
          sourceComboBox.setEnabled(true);
        }
      } else {
        showErrorPopup(
            localizationUtil.getMessage(Constants.MESSAGES.INVALID_CONFIGURATION_MESSAGE));
      }
      orgTable.setEnabled(true);
      orgTable.clearSelection();
    } else {
      log.info(Constants.LOGS.INVALID_INPUTS);
    }
  }

  private void editActionOnClick() {
    loadConfigurationDataToUI();
    isEdited = true;
    orgTable.setEnabled(false);
    enableAction = false;
    addButton.setText(localizationUtil.getMessage(Constants.UPDATE_BUTTON));
  }

  private void deleteActionOnClick() {
    if (JOptionPane.showConfirmDialog(
            null,
            localizationUtil.getMessage(Constants.MESSAGES.DELETE_CONFIRMATION_MESSAGE),
            localizationUtil.getMessage(Constants.TITLE.DELETE),
            JOptionPane.YES_NO_OPTION)
        == 0) {
      String appOrProjectName = model.getValueAt(orgTable.getSelectedRow(), 1).toString();
      String source = model.getValueAt(orgTable.getSelectedRow(), 2).toString();
      if (credentialDetailsService.delete(appOrProjectName, source)) {
        model.removeRow(orgTable.getSelectedRow());
        showSuccessfulPopup(localizationUtil.getMessage(Constants.MESSAGES.CONFIGURATION_DELETED));
        removeUserInputsFromUI();
        initPlaceHolder();
        enableApply = true;
      } else {
        showErrorPopup(
            localizationUtil.getMessage(Constants.MESSAGES.UNABLE_TO_DELETE_CONFIGURATION));
      }
      orgTable.clearSelection();
      enableAction = false;
    } else {
      orgTable.clearSelection();
      log.info(Constants.LOGS.DELETE_CANCELED);
    }
  }

  private boolean isUserInputValid() {
    if (isInputFieldsEmpty()) {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.EMPTY_INPUTS_MESSAGE));
      return false;
    } else if (sourceComboBox.getSelectedItem().toString().contains(Constants.ASSESS)
        && appOrPojectsComboBox.getSelectedItem() == null) {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.RETRIEVE_APP_NAME));
      return false;
    } else if (sourceComboBox.getSelectedItem().toString().contains(Constants.SCAN)
        && appOrPojectsComboBox.getSelectedItem() == null) {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.RETRIEVE_PROJECT_NAME));
      return false;
    } else if (minutesField.getText().isEmpty()) {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.REFRESH_CYCLE_CANNOT_BE_EMPTY));
      return false;
    } else if (!isRefreshCycleValid()) {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.REFRESH_CYCLE_NOT_VALID));
      return false;
    }
    return true;
  }

  /** Reset the configuration screen back to default when executed */
  public void resetConfigurationScreen() {
    removeUserInputsFromUI();
    initPlaceHolder();
    sourceComboBox.setEnabled(true);
    orgTable.clearSelection();
    addButton.setText(localizationUtil.getMessage(Constants.ADD_BUTTON));
    addButton.setEnabled(false);
    cancelButton.setEnabled(false);
    orgTable.setEnabled(true);
  }

  private ConfigurationDTO getConfigDTOWithAppOrProjectID() {
    Fetcher fetcher = getUserInputAsFetcher();
    String orgName = fetcher.getOrgName();
    String appOrProjectName =
        Objects.requireNonNull(appOrPojectsComboBox.getSelectedItem()).toString();
    if (StringUtils.equals(
        Objects.requireNonNull(sourceComboBox.getSelectedItem()).toString(), Constants.ASSESS)) {
      String appID = fetcher.getApplicationIdByName(appOrProjectName);
      return getConfigurationDTO(appID, orgName);
    } else {
      String projectID = fetcher.getProjectIdByName(appOrProjectName);
      return getConfigurationDTO(projectID, orgName);
    }
  }

  private void retrieveApplications() {
    Fetcher fetcher = getUserInputAsFetcher();
    List<String> appNames = fetcher.getApplicationNames();
    if (CollectionUtils.isNotEmpty(appNames)) {
      loadApplicationsOrProjectInComboBox(appNames);
      showSuccessfulPopup(localizationUtil.getMessage(Constants.MESSAGES.CONNECTED_MESSAGE));
      sourceComboBox.setEnabled(false);
      addButton.setEnabled(true);
      cancelButton.setEnabled(true);
    } else {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.INVALID_CONFIGURATION_MESSAGE));
    }
  }

  private void retrieveProjects() {
    Fetcher fetcher = getUserInputAsFetcher();
    List<String> projectNames = fetcher.getProjectNames();
    if (CollectionUtils.isNotEmpty(projectNames)) {
      loadApplicationsOrProjectInComboBox(projectNames);
      showSuccessfulPopup(localizationUtil.getMessage(Constants.MESSAGES.CONNECTED_MESSAGE));
      sourceComboBox.setEnabled(false);
      addButton.setEnabled(true);
      cancelButton.setEnabled(true);
    } else {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.INVALID_CONFIGURATION_MESSAGE));
    }
  }

  private void loadApplicationsOrProjectInComboBox(List<String> appsOrProjects) {
    appOrPojectsComboBox.removeAllItems();
    for (String name : appsOrProjects) {
      appOrPojectsComboBox.addItem(name);
    }
  }

  private void loadConfigurationDataToUI() {
    removeUserInputsFromUI();
    resetTextFieldState();
    String selectedItem = model.getValueAt(orgTable.getSelectedRow(), 1).toString();
    String source = model.getValueAt(orgTable.getSelectedRow(), 2).toString();
    ConfigurationDTO configurationDTO =
        credentialDetailsService.getSavedConfigDataByName(selectedItem, source);
    configurationDTO = CredentialUtil.decryptDTO(configurationDTO);
    sourceComboBox.setSelectedItem(configurationDTO.getSource());
    sourceComboBox.setEnabled(false);
    urlField.setText(configurationDTO.getContrastURL());
    userNameField.setText(configurationDTO.getUserName());
    serviceKeyField.setEchoChar('*');
    serviceKeyField.setText(configurationDTO.getServiceKey());
    apiKeyField.setEchoChar('*');
    apiKeyField.setText(configurationDTO.getApiKey());
    orgIdField.setText(configurationDTO.getOrgId());
    appOrPojectsComboBox.removeAllItems();
    appOrPojectsComboBox.addItem(configurationDTO.getAppOrProject());
    appOrPojectsComboBox.setSelectedItem(configurationDTO.getAppOrProject());
    minutesField.setText(String.valueOf(configurationDTO.getRefreshCycleMinutes()));
  }

  private void removeUserInputsFromUI() {
    sourceComboBox.setSelectedIndex(0);
    urlField.setText(StringUtils.EMPTY);
    userNameField.setText(StringUtils.EMPTY);
    serviceKeyField.setText(StringUtils.EMPTY);
    apiKeyField.setText(StringUtils.EMPTY);
    orgIdField.setText(StringUtils.EMPTY);
    minutesField.setText(Constants.DEFAULT_VR_REFRESH_CYCLE_VALUE);
    appOrPojectsComboBox.removeAllItems();
  }

  private boolean isInputFieldsEmpty() {
    return urlField.getText().isEmpty()
        || userNameField.getText().isEmpty()
        || new String(serviceKeyField.getPassword()).isEmpty()
        || new String(apiKeyField.getPassword()).isEmpty()
        || orgIdField.getText().isEmpty();
  }

  private boolean isRefreshCycleValid() {
    try {
      int refreshCycleRange = Integer.parseInt(minutesField.getText());
      return refreshCycleRange > 0 && refreshCycleRange <= 4320;
    } catch (Exception e) {
      return false;
    }
  }

  private void loadPersistedDataToTable() {
    Set<ConfigurationDTO> savedConfigData = credentialDetailsService.getSavedConfigData();
    if (CollectionUtils.isNotEmpty(savedConfigData)) {
      for (ConfigurationDTO dto : savedConfigData) {
        model.addRow(new Object[] {dto.getOrgName(), dto.getAppOrProject(), dto.getSource()});
      }
    } else {
      log.info(Constants.LOGS.NO_PERSISTED_DATA_FOUND);
    }
    preLoadUserConfiguration();
  }

  private Fetcher getUserInputAsFetcher() {
    return new Fetcher(
        userNameField.getText().trim(),
        urlField.getText().trim(),
        orgIdField.getText().trim(),
        new String(apiKeyField.getPassword()).trim(),
        new String(serviceKeyField.getPassword()).trim());
  }

  private void save(ConfigurationDTO dto) {
    CredentialUtil.encryptDTO(dto);
    if (credentialDetailsService.save(dto)) {
      model.addRow(new Object[] {dto.getOrgName(), dto.getAppOrProject().trim(), dto.getSource()});
      showSuccessfulPopup(localizationUtil.getMessage(Constants.MESSAGES.CONFIGURATION_SAVED));
      enableApply = true;
      refreshApplications();
    } else {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.UNABLE_TO_SAVE));
    }
  }

  private void update(ConfigurationDTO dto) {
    int selectedRow = orgTable.getSelectedRow();
    CredentialUtil.encryptDTO(dto);
    if (credentialDetailsService.update(dto, model.getValueAt(selectedRow, 1).toString())) {
      model.setValueAt(dto.getOrgName(), selectedRow, 0);
      model.setValueAt(dto.getAppOrProject(), selectedRow, 1);
      model.setValueAt(dto.getSource(), selectedRow, 2);
      appOrPojectsComboBox.removeAllItems();
      showSuccessfulPopup(localizationUtil.getMessage(Constants.MESSAGES.CONFIGURATION_UPDATED));
      addButton.setText(localizationUtil.getMessage(Constants.ADD_BUTTON));
      isEdited = false;
      enableApply = true;
      refreshApplications();
    } else {
      showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.UNABLE_TO_UPDATE));
    }
  }

  private ConfigurationDTO getConfigurationDTO(String appOrProjectID, String orgName) {
    return new ConfigurationDTO(
        Objects.requireNonNull(sourceComboBox.getSelectedItem()).toString(),
        urlField.getText(),
        userNameField.getText(),
        new String(serviceKeyField.getPassword()),
        new String(apiKeyField.getPassword()),
        orgIdField.getText(),
        Objects.requireNonNull(appOrPojectsComboBox.getSelectedItem()).toString(),
        appOrProjectID,
        orgName,
        Integer.parseInt(minutesField.getText()));
  }

  private void initPlaceHolder() {
    setPlaceHolder(urlField, localizationUtil.getMessage(Constants.PLACE_HOLDERS.ENTER_URL));
    setPlaceHolder(
        userNameField, localizationUtil.getMessage(Constants.PLACE_HOLDERS.ENTER_USERNAME));
    setPlaceHolder(
        serviceKeyField, localizationUtil.getMessage(Constants.PLACE_HOLDERS.ENTER_SERVICE_KEY));
    setPlaceHolder(apiKeyField, localizationUtil.getMessage(Constants.PLACE_HOLDERS.ENTER_API_KEY));
    setPlaceHolder(orgIdField, localizationUtil.getMessage(Constants.PLACE_HOLDERS.ENTER_ORG_ID));
  }

  private void resetTextFieldState() {
    urlField.setForeground(UIUtil.getActiveTextColor());
    userNameField.setForeground(UIUtil.getActiveTextColor());
    serviceKeyField.setForeground(UIUtil.getActiveTextColor());
    apiKeyField.setForeground(UIUtil.getActiveTextColor());
    orgIdField.setForeground(UIUtil.getActiveTextColor());
  }

  private void setPlaceHolder(JBTextField textField, String placeHolder) {
    textField.setText(placeHolder);
    textField.setForeground(UIUtil.getInactiveTextColor());
    textField.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            if (textField.getText().equals(placeHolder)) {
              textField.setText(StringUtils.EMPTY);
              textField.setForeground(
                  UIUtil.getLabelForeground()); // Adapt to theme for active text
            }
          }

          @Override
          public void focusLost(FocusEvent e) {
            if (textField.getText().isEmpty()) {
              textField.setText(placeHolder);
              textField.setForeground(
                  UIUtil.getInactiveTextColor()); // Use theme color for placeholder
            }
          }
        });
  }

  private void setPlaceHolder(JBPasswordField passwordField, String placeHolder) {
    passwordField.setText(placeHolder);
    passwordField.setEchoChar((char) 0);
    passwordField.setForeground(UIUtil.getInactiveTextColor());
    passwordField.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            if (new String(passwordField.getPassword()).equals(placeHolder)) {
              passwordField.setText(StringUtils.EMPTY);
              passwordField.setEchoChar('*');
              passwordField.setForeground(
                  UIUtil.getLabelForeground()); // Adapt to theme for active text
            }
          }

          @Override
          public void focusLost(FocusEvent e) {
            if (new String(passwordField.getPassword()).isEmpty()) {
              passwordField.setText(placeHolder);
              passwordField.setEchoChar((char) 0);
              passwordField.setForeground(
                  UIUtil.getInactiveTextColor()); // Use theme color for placeholder
            }
          }
        });
  }

  private void preLoadUserConfiguration() {
    if (model.getRowCount() > 0) {
      Object name = model.getValueAt(0, 1);
      Object source = model.getValueAt(0, 2);
      ConfigurationDTO dto =
          credentialDetailsService.getSavedConfigDataByName(name.toString(), source.toString());
      if (dto != null) {
        dto = CredentialUtil.decryptDTO(dto);
        sourceComboBox.setSelectedItem(dto.getSource());
        urlField.setText(dto.getContrastURL());
        urlField.setForeground(UIUtil.getLabelForeground());
        userNameField.setText(dto.getUserName());
        userNameField.setForeground(UIUtil.getLabelForeground());
        orgIdField.setText(dto.getOrgId());
        orgIdField.setForeground(UIUtil.getLabelForeground());
        apiKeyField.setText(dto.getApiKey());
        apiKeyField.setForeground(UIUtil.getLabelForeground());
        apiKeyField.setEchoChar('*');
        serviceKeyField.setText(dto.getServiceKey());
        serviceKeyField.setEchoChar('*');
        serviceKeyField.setForeground(UIUtil.getLabelForeground());
      }
    }
  }

  private void refreshApplications() {
    for (@NotNull Project openProject : ProjectManager.getInstance().getOpenProjects()) {
      ToolWindowManager instance = ToolWindowManager.getInstance(openProject);
      ToolWindow contrastWindow = instance.getToolWindow("Contrast");
      if (contrastWindow != null) {
        Content content =
            contrastWindow.getContentManager().getContent(0); // Assuming the first tab
        if (content != null) {
          JComponent component = content.getComponent();
          if (component instanceof ContrastToolWindow contrastToolWindow
              && contrastToolWindow.getAssessComponent() != null) {
            contrastToolWindow.getAssessComponent().refreshApplications();
          }
        }
      }
    }
  }

  private void showSuccessfulPopup(String message) {
    popupUtil.disposePopup();
    popupUtil.showFadingPopupOnCustomPane(this, message, PopupUtil.PopupType.SUCCESS);
  }

  private void showErrorPopup(String message) {
    popupUtil.disposePopup();
    popupUtil.showFadingPopupOnCustomPane(this, message, PopupUtil.PopupType.ERROR);
  }
}
