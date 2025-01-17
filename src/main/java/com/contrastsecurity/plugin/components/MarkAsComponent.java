/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.components;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.fetchers.Fetcher;
import com.contrastsecurity.plugin.models.ConfigurationDTO;
import com.contrastsecurity.plugin.persistent.CredentialDetailsService;
import com.contrastsecurity.plugin.service.CacheDataService;
import com.contrastsecurity.plugin.utility.CredentialUtil;
import com.contrastsecurity.plugin.utility.LocalizationUtil;
import com.contrastsecurity.plugin.utility.PopupUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class MarkAsComponent extends JBPanel {

  private LocalizationUtil localizationUtil;
  private JBPanel<?> markAsContainer;
  private JBPanel<?> reasonContainer;
  private JBPanel<?> commentContainer;
  private JBPanel<?> processingContainer;

  private ComboBox<?> markAsComboBox;
  private ComboBox<?> reasonComboBox;
  private JEditorPane commentArea;
  private JButton okButton;

  @Getter private JBScrollPane scrollPane;

  private String status;
  private String appID;
  private String traceID;

  private transient SwingWorker<Void, Void> worker;

  private CacheDataService cacheDataService;

  private final transient PopupUtil popupUtil;

  public MarkAsComponent(String status, String appID, String traceID, Project project) {
    this.status = status;
    this.appID = appID;
    this.traceID = traceID;
    popupUtil = new PopupUtil(project);
    cacheDataService = new CacheDataService();
    localizationUtil = LocalizationUtil.getInstance();
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    markAsContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    reasonContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    commentArea = new JEditorPane();
    commentArea.setEditable(true);
    commentArea.setText(StringUtils.EMPTY);
    commentContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    processingContainer = new JBPanel<>(new FlowLayout(FlowLayout.TRAILING));
    markAsComboBox = new ComboBox<>(Constants.UTILS.MARK_AS_COMBOBOX);
    reasonComboBox = new ComboBox<>(Constants.UTILS.REASON_COMBOBOX);
    okButton = new JButton("OK");
    configureContainers();
    scrollPane =
        new JBScrollPane(
            this,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }

  private void configureContainers() {
    configureMarkAsContainer();
    configureReasonContainer();
    configureCommentContainer();
    configureProcessingContainer();
    add(markAsContainer);
    add(reasonContainer);
    add(commentContainer);
    add(processingContainer);
  }

  private void configureMarkAsContainer() {
    markAsContainer.removeAll();
    JBLabel markAsLabel = new JBLabel(localizationUtil.getMessage(Constants.TITLE.MARK_AS));
    markAsLabel.setPreferredSize(new Dimension(120, 30));
    markAsComboBox.setPreferredSize(new Dimension(160, 30));
    if (StringUtils.isNotEmpty(status)) {
      markAsComboBox.setSelectedItem(status);
      if (StringUtils.equalsIgnoreCase(status, Constants.CHECKBOXES.NOT_A_PROBLEM)) {
        Object subStatus = cacheDataService.get(traceID + "-" + Constants.UTILS.MARK_AS_SUB_STATUS);
        reasonComboBox.setEnabled(true);
        if (subStatus != null) {
          reasonComboBox.setSelectedItem(subStatus);
        }
      } else {
        reasonComboBox.setEnabled(false);
      }
    }
    markAsComboBox.addActionListener(
        e -> {
          String selectedItem = markAsComboBox.getSelectedItem().toString();
          reasonComboBox.setEnabled(
              StringUtils.equalsIgnoreCase(selectedItem, Constants.CHECKBOXES.NOT_A_PROBLEM));
        });
    markAsContainer.add(markAsLabel);
    markAsContainer.add(markAsComboBox);
    markAsContainer.revalidate();
    markAsContainer.repaint();
  }

  private void configureReasonContainer() {
    reasonContainer.removeAll();
    JBLabel reasonLabel = new JBLabel(localizationUtil.getMessage(Constants.REASON_LABEL));
    reasonLabel.setPreferredSize(new Dimension(120, 30));
    reasonComboBox.setPreferredSize(new Dimension(320, 30));
    reasonContainer.add(reasonLabel);
    reasonContainer.add(reasonComboBox);
    reasonContainer.revalidate();
    reasonContainer.repaint();
  }

  private void configureCommentContainer() {
    commentContainer.removeAll();
    JBLabel commentLabel = new JBLabel(localizationUtil.getMessage(Constants.ADD_COMMENT));
    commentLabel.setPreferredSize(new Dimension(120, 30));
    commentContainer.add(commentLabel);
    commentArea.setText(StringUtils.EMPTY);
    if (JBColor.isBright()) {
      commentArea.setBackground(Color.WHITE);
      commentArea.setForeground(Color.BLACK);
    } else {
      commentArea.setBackground(Color.DARK_GRAY);
      commentArea.setForeground(Color.WHITE);
    }
    commentArea.setPreferredSize(new Dimension(360, 150));
    commentContainer.add(commentArea);
    commentContainer.revalidate();
    commentContainer.repaint();
  }

  private void configureProcessingContainer() {
    processingContainer.removeAll();
    okButton.addActionListener(e -> okButtonOnClick());
    JButton clearButton = new JButton(localizationUtil.getMessage(Constants.CLEAR_BUTTON));
    clearButton.addActionListener(e -> clearButtonOnClick());
    processingContainer.add(okButton);
    processingContainer.add(clearButton);
    processingContainer.revalidate();
    processingContainer.repaint();
  }

  private void okButtonOnClick() {
    if (worker != null && !worker.isDone()) {
      worker.cancel(true);
      okButton.setEnabled(true);
    }
    worker =
        new SwingWorker<Void, Void>() {
          @Override
          protected Void doInBackground() {
            if (commentArea.getText().trim().length() > 256) {
              showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.COMMENT_LIMIT));
              commentArea.setText(StringUtils.EMPTY);
              return null;
            }
            okButton.setEnabled(false);
            try {
              CredentialDetailsService credentialDetailsService =
                  CredentialDetailsService.getInstance();
              ConfigurationDTO dto = credentialDetailsService.getSavedConfigDataByID(appID);
              dto = CredentialUtil.decryptDTO(dto);
              if (dto != null) {
                Fetcher fetcher =
                    new Fetcher(
                        dto.getUserName(),
                        dto.getContrastURL(),
                        dto.getOrgId(),
                        dto.getApiKey(),
                        dto.getServiceKey());
                if (fetcher.markVulnerability(getUIDataAsRequestHeader())) {
                  cacheDataService.add(
                      traceID + "-" + Constants.UTILS.MARK_AS_STATUS,
                      markAsComboBox.getSelectedItem().toString());
                  if (StringUtils.equalsIgnoreCase(
                      markAsComboBox.getSelectedItem().toString(),
                      Constants.CHECKBOXES.NOT_A_PROBLEM)) {
                    cacheDataService.add(
                        traceID + "-" + Constants.UTILS.MARK_AS_SUB_STATUS,
                        reasonComboBox.getSelectedItem().toString());
                  }
                  showSuccessPopup(
                      localizationUtil.getMessage(Constants.MESSAGES.MARKED_VULNERABILITY));
                } else {
                  showErrorPopup(localizationUtil.getMessage(Constants.MESSAGES.FAILED_TO_MARK));
                }
              } else {
                log.error(Constants.LOGS.NO_CREDENTIALS_CONFIGURED);
              }
            } catch (Exception e) {
              log.error(Constants.LOGS.FAILED_TO_MARK_VULNERABILITY);
            }
            return null;
          }

          @Override
          protected void done() {
            okButton.setEnabled(true);
          }
        };
    worker.execute();
  }

  private void clearButtonOnClick() {
    commentArea.setText(StringUtils.EMPTY);
    revalidate();
    repaint();
  }

  private String getUIDataAsRequestHeader() {
    StringBuilder requestBody = new StringBuilder();
    requestBody.append("{\n");
    requestBody.append("\"traces\": [\"");
    requestBody.append(traceID);
    requestBody.append("\"],\n");
    requestBody.append("\"status\": \"");
    requestBody.append(getStatusApiKeyValues(markAsComboBox.getSelectedItem().toString()));
    requestBody.append("\",\n");
    if (StringUtils.equalsIgnoreCase(
        markAsComboBox.getSelectedItem().toString(), Constants.CHECKBOXES.NOT_A_PROBLEM)) {
      appendSubStatus(requestBody);
    }
    requestBody.append("\"note\": \"");
    requestBody.append(StringUtils.normalizeSpace(commentArea.getText()).trim());
    requestBody.append("\"\n}");
    return requestBody.toString();
  }

  private void appendSubStatus(StringBuilder requestBody) {
    requestBody.append("\"substatus\": \"");
    requestBody.append(getSubStatusApiKeyValues(reasonComboBox.getSelectedItem().toString()));
    requestBody.append("\",\n");
  }

  /**
   * Reinitialize the current object values and reconfigures the UI
   *
   * @param status Status to be selected
   * @param appID Application ID
   * @param traceID Vulnerability ID
   */
  public void refresh(String status, String appID, String traceID) {
    this.status = status;
    this.appID = appID;
    this.traceID = traceID;
    configureContainers();
  }

  private String getStatusApiKeyValues(String status) {
    if (StringUtils.equalsIgnoreCase(status, Constants.CHECKBOXES.NOT_A_PROBLEM)) {
      return Constants.API.NOT_A_PROBLEM;
    }
    return status;
  }

  private String getSubStatusApiKeyValues(String subStatus) {
    return switch (subStatus) {
      case Constants.UTILS.FALSE_POSITIVE -> Constants.API.FALSE_POSITIVE;
      case Constants.UTILS.IS_CONTROL -> Constants.API.IS_CONTROL;
      case Constants.UTILS.ES_CONTROL -> Constants.API.ES_CONTROL;
      case Constants.UTILS.URL_ACCESS_LIMITED -> Constants.API.URL_ACCESS_LIMITED;
      case Constants.UTILS.OTHER -> Constants.API.OTHER;
      default -> StringUtils.EMPTY;
    };
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
