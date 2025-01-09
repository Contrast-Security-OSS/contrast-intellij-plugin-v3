/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.components;

import static com.contrastsecurity.plugin.constants.Constants.CALENDER_UTIL.TIME;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.utility.CheckBoxGroup;
import com.contrastsecurity.plugin.utility.LocalizationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class FilterComponent extends JBPanel {

  // Containers
  private JBPanel<?> severityContainer;
  private JBPanel<?> statusContainer;
  private JBPanel<?> dateAndTimeContainer;

  // Utilities
  @Getter private transient CheckBoxGroup severityCheckBoxGroup;
  @Getter private transient CheckBoxGroup statusCheckBoxGroup;

  // Components
  private ComboBox<String> durationComboBox;
  private JBLabel fromLabel;
  @Setter private JBTextField fromDateTextField;
  private JButton fromCalenderButton;
  private ComboBox<String> fromTimeComboBox;

  private JBLabel toLabel;
  private JBTextField toDateTextField;
  private JButton toCalenderButton;
  private ComboBox<String> toTimeComboBox;
  private JComponent[] filterComponents;
  private transient CalenderComponent calenderComponent;
  private boolean isAssessComponent;

  private LocalizationUtil localizationUtil;

  public FilterComponent(boolean isAssessComponent) {
    this.isAssessComponent = isAssessComponent;
    localizationUtil = LocalizationUtil.getInstance();
    this.calenderComponent = new CalenderComponent();
    // Init Containers
    severityContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    statusContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    dateAndTimeContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    severityCheckBoxGroup =
        new CheckBoxGroup(
            new String[] {
              Constants.CHECKBOXES.CRITICAL,
              Constants.CHECKBOXES.HIGH,
              Constants.CHECKBOXES.MEDIUM,
              Constants.CHECKBOXES.LOW,
              Constants.CHECKBOXES.NOTE
            });

    String[] commonStatusLabels;
    if (isAssessComponent) {
      commonStatusLabels =
          new String[] {
            Constants.CHECKBOXES.REPORTED,
            Constants.CHECKBOXES.CONFIRMED,
            Constants.CHECKBOXES.SUSPICIOUS,
            Constants.CHECKBOXES.NOT_A_PROBLEM,
            Constants.CHECKBOXES.REMEDIATED,
            Constants.CHECKBOXES.FIXED
          };
    } else {
      commonStatusLabels =
          new String[] {
            Constants.CHECKBOXES.REPORTED,
            Constants.CHECKBOXES.CONFIRMED,
            Constants.CHECKBOXES.SUSPICIOUS,
            Constants.CHECKBOXES.NOT_A_PROBLEM,
            Constants.CHECKBOXES.REMEDIATED,
            Constants.CHECKBOXES.REMEDIATED_AUTO_VERIFIED,
            Constants.CHECKBOXES.REOPENED
          };
    }

    statusCheckBoxGroup = new CheckBoxGroup(commonStatusLabels);
    // init components
    durationComboBox =
        new ComboBox<>(
            new String[] {
              Constants.UTILS.ALL,
              Constants.UTILS.LAST_HOUR,
              Constants.UTILS.LAST_DAY,
              Constants.UTILS.LAST_WEEK,
              Constants.UTILS.LAST_MONTH,
              Constants.UTILS.LAST_YEAR,
              Constants.UTILS.CUSTOM
            });
    durationComboBox.setPreferredSize(new Dimension(120, 30));
    durationComboBox.addActionListener(
        actionEvent -> {
          setComponentsEnabled(
              filterComponents,
              Objects.equals(
                  Objects.requireNonNull(durationComboBox.getSelectedItem()).toString(),
                  Constants.UTILS.CUSTOM));
          if (Objects.equals(
              Objects.requireNonNull(durationComboBox.getSelectedItem()).toString(),
              Constants.UTILS.CUSTOM)) {
            fromTimeComboBox.setEnabled(false);
            toTimeComboBox.setEnabled(false);
            toCalenderButton.setEnabled(false);
          } else {
            fromDateTextField.setText(StringUtils.EMPTY);
            toDateTextField.setText(StringUtils.EMPTY);
            fromTimeComboBox.removeAllItems();
            toTimeComboBox.removeAllItems();
            for (String time : TIME) {
              fromTimeComboBox.addItem(time);
              toTimeComboBox.addItem(time);
            }
          }
        });
    fromLabel = new JBLabel(localizationUtil.getMessage(Constants.FROM_FILTER_LABEL));
    fromLabel.setPreferredSize(new Dimension(50, 30));
    fromDateTextField = new JBTextField();
    fromDateTextField.setEditable(false);
    fromDateTextField.setPreferredSize(new Dimension(150, 30));
    fromCalenderButton = new JButton(AllIcons.Debugger.EvaluateExpression);
    fromCalenderButton.setPreferredSize(new Dimension(40, 30));
    toLabel = new JBLabel(localizationUtil.getMessage(Constants.TO_FILTER_LABEL));
    toLabel.setPreferredSize(new Dimension(50, 30));
    toDateTextField = new JBTextField();
    toDateTextField.setEditable(false);
    toDateTextField.setPreferredSize(new Dimension(150, 30));
    toCalenderButton = new JButton(AllIcons.Debugger.EvaluateExpression);
    toCalenderButton.setPreferredSize(new Dimension(40, 30));
    fromTimeComboBox = new ComboBox<>(TIME);
    toTimeComboBox = new ComboBox<>(TIME);
    filterComponents =
        new JComponent[] {
          fromLabel,
          fromDateTextField,
          fromCalenderButton,
          fromTimeComboBox,
          toLabel,
          toDateTextField,
          toCalenderButton,
          toTimeComboBox
        };
    setComponentsEnabled(filterComponents, false);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    configureContainers(isAssessComponent);
  }

  private void configureContainers(boolean isAssessComponent) {

    // Configure Severity container
    configureSeverityContainer();
    add(severityContainer);

    // Configure Status Container
    configureStatusContainer();
    add(statusContainer);

    // Configure Date and Time Container
    if (isAssessComponent) {
      configureDateAndTimeContainer();
      add(dateAndTimeContainer);
    }
  }

  private void configureSeverityContainer() {
    JBLabel severityLabel =
        new JBLabel(localizationUtil.getMessage(Constants.SEVERITY_FILTER_LABEL));
    severityLabel.setPreferredSize(new Dimension(160, 30));
    severityContainer.add(severityLabel);
    JBPanel<?> checkBoxContainer = new JBPanel<>(new GridLayout(2, 3, 126, 2));
    severityCheckBoxGroup.loadCheckBoxesToPanel(checkBoxContainer);
    checkBoxContainer.add(new JBLabel("")); // empty component for alignment
    severityContainer.add(checkBoxContainer);
  }

  private void configureStatusContainer() {
    JBLabel statusLabel = new JBLabel(localizationUtil.getMessage(Constants.STATUS_FILTER_LABEL));
    statusLabel.setPreferredSize(new Dimension(160, 30));
    statusContainer.add(statusLabel);
    JBPanel<?> checkBoxContainer;
    if (isAssessComponent) {
      checkBoxContainer = new JBPanel<>(new GridLayout(2, 3, 87, 2));
    } else {
      checkBoxContainer = new JBPanel<>(new GridLayout(3, 3, 10, 2));
    }
    statusCheckBoxGroup.loadCheckBoxesToPanel(checkBoxContainer);
    statusContainer.add(checkBoxContainer);
  }

  private void configureDateAndTimeContainer() {
    JBLabel filterLabel = new JBLabel(localizationUtil.getMessage(Constants.FILTER_FILTER_LABEL));
    filterLabel.setPreferredSize(new Dimension(160, 30));
    dateAndTimeContainer.add(filterLabel);
    dateAndTimeContainer.add(durationComboBox);
    JBLabel emptyLabel = new JBLabel("");
    emptyLabel.setPreferredSize(new Dimension(30, 30));
    dateAndTimeContainer.add(emptyLabel);
    JBPanel<?> fromToContainer = new JBPanel<>(new BorderLayout());
    JBPanel<?> fromContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    JBPanel<?> toContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEADING));
    fromCalenderButton.addActionListener(
        actionEvent -> calenderComponent = new CalenderComponent(this, true));
    toCalenderButton.addActionListener(
        actionEvent -> calenderComponent = new CalenderComponent(this, false));
    fromTimeComboBox.addItemListener(
        e -> {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            updateToTimeComboBox();
          }
        });
    fromContainer.add(fromLabel);
    fromContainer.add(fromDateTextField);
    fromContainer.add(fromCalenderButton);
    fromContainer.add(fromTimeComboBox);
    toContainer.add(toLabel);
    toContainer.add(toDateTextField);
    toContainer.add(toCalenderButton);
    toContainer.add(toTimeComboBox);
    fromToContainer.add(fromContainer, BorderLayout.NORTH);
    fromToContainer.add(toContainer, BorderLayout.SOUTH);
    dateAndTimeContainer.add(fromToContainer);
  }

  private void setComponentsEnabled(JComponent[] components, boolean enabled) {
    for (JComponent component : components) {
      if (component != null) {
        component.setEnabled(enabled);
      }
    }
  }

  /** Clears the applied filter for the filter panel */
  public void clearAppliedFilter() {
    clearScanFilters();
    durationComboBox.setSelectedIndex(0);
    fromDateTextField.setText(StringUtils.EMPTY);
    toDateTextField.setText(StringUtils.EMPTY);
    fromTimeComboBox.setSelectedIndex(0);
    toTimeComboBox.setSelectedIndex(0);
  }

  public void clearScanFilters() {
    severityCheckBoxGroup.clearCheckBoxSelection();
    statusCheckBoxGroup.clearCheckBoxSelection();
    severityContainer.revalidate();
    severityContainer.repaint();
    statusContainer.revalidate();
    statusContainer.repaint();
  }

  /**
   * Sets From date field in the UI
   *
   * @param date String
   */
  public void setFromDate(String date) {
    fromDateTextField.setText(date);
    if (!fromDateTextField.getText().trim().isEmpty()) {
      toCalenderButton.setEnabled(true);
      updateToTimeComboBox();
      updateFromTimeComboBox();
      fromTimeComboBox.requestFocus();
      fromTimeComboBox.setEnabled(true);
    }
  }

  /**
   * Get From date
   *
   * @return String
   */
  public String getFromDate() {
    return fromDateTextField.getText();
  }

  /**
   * Get To date
   *
   * @return String
   */
  public String getToDate() {
    return toDateTextField.getText();
  }

  /**
   * Sets To date field in the UI
   *
   * @param date String
   */
  public void setToDate(String date) {
    toDateTextField.setText(date);
    if (!toDateTextField.getText().trim().isEmpty()) {
      updateToTimeComboBox();
      toTimeComboBox.requestFocus();
      toTimeComboBox.setEnabled(true);
    }
  }

  /**
   * Returns a Map of start and end time selected from the UI filter
   *
   * @return Map of custom time
   */
  public Map<String, String> getCustomTime() {
    Map<String, String> customTime = new HashMap<>();
    String fromTime =
        formatCustomTime(
            fromDateTextField.getText(),
            Objects.requireNonNull(fromTimeComboBox.getSelectedItem()).toString());
    String toTime =
        formatCustomTime(
            toDateTextField.getText(),
            Objects.requireNonNull(toTimeComboBox.getSelectedItem()).toString());
    customTime.put(Constants.UTILS.START_DATE, fromTime);
    customTime.put(Constants.UTILS.END_DATE, toTime);
    return customTime;
  }

  private String getSelectedTime(String time) {
    switch (time) {
      case Constants.UTILS.LAST_HOUR:
        long lastHour =
            ZonedDateTime.now(ZoneId.systemDefault()).minusHours(1).toInstant().toEpochMilli();
        return String.valueOf(lastHour);
      case Constants.UTILS.LAST_DAY:
        long lastDay =
            ZonedDateTime.now(ZoneId.systemDefault()).minusDays(1).toInstant().toEpochMilli();
        return String.valueOf(lastDay);
      case Constants.UTILS.LAST_WEEK:
        long lastWeek =
            ZonedDateTime.now(ZoneId.systemDefault()).minusWeeks(1).toInstant().toEpochMilli();
        return String.valueOf(lastWeek);
      case Constants.UTILS.LAST_MONTH:
        long lastMonth =
            ZonedDateTime.now(ZoneId.systemDefault()).minusMonths(1).toInstant().toEpochMilli();
        return String.valueOf(lastMonth);
      case Constants.UTILS.LAST_YEAR:
        long lastYear =
            ZonedDateTime.now(ZoneId.systemDefault()).minusYears(1).toInstant().toEpochMilli();
        return String.valueOf(lastYear);
      default:
        return StringUtils.EMPTY;
    }
  }

  private String formatCustomTime(String date, String time) {
    String dateTimeString = date + " " + time.toUpperCase();
    DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy h:mma", Locale.ENGLISH);
    try {
      LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
      return String.valueOf(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    } catch (DateTimeParseException e) {
      log.error(e.getMessage());
      return null;
    }
  }

  private void updateFromTimeComboBox() {
    String fromDateString = fromDateTextField.getText();
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    LocalDate fromDate = LocalDate.parse(fromDateString, dateFormatter);
    LocalDate currentDate = LocalDate.now();
    LocalTime currentTime = LocalTime.now();

    fromTimeComboBox.removeAllItems();
    if (fromDate.equals(currentDate)) {
      for (String time : TIME) {
        LocalTime availableTime = convertToLocalTime(time);
        if (availableTime != null
            && (availableTime.isBefore(currentTime) || availableTime.equals(currentTime))) {
          fromTimeComboBox.addItem(time);
        }
      }
    } else {
      for (String time : TIME) {
        fromTimeComboBox.addItem(time);
      }
    }
  }

  private void updateToTimeComboBox() {
    String fromDateString = fromDateTextField.getText();
    String toDateString = toDateTextField.getText();
    toTimeComboBox.removeAllItems();

    if (isCurrentDate(fromDateString) && isCurrentDate(toDateString)) {
      populateToTimeComboBox(true);
    } else if (toDateString != null && isCurrentDate(toDateString)) {
      String currentTimeStr = getCurrentSystemTime();
      LocalTime currentTime = convertToLocalTime(currentTimeStr);

      for (String time : TIME) {
        LocalTime toTime = convertToLocalTime(time);
        if (toTime != null && toTime.isBefore(currentTime)) {
          toTimeComboBox.addItem(time);
        }
      }
    } else if (fromDateString != null
        && fromDateString.equals(toDateString)
        && !isCurrentDate(fromDateString)) {
      populateToTimeComboBox(false);
    } else {
      for (String time : TIME) {
        toTimeComboBox.addItem(time);
      }
    }
  }

  private LocalTime convertToLocalTime(String time) {
    DateTimeFormatter timeFormatter =
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ofPattern("h:mma"))
            .toFormatter();
    try {
      return LocalTime.parse(time, timeFormatter);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private boolean isCurrentDate(String date) {
    String currentDateStr = calenderComponent.getCurrentDate();
    if (date == null || date.isEmpty() || currentDateStr == null || currentDateStr.isEmpty()) {
      return false;
    }
    Date inputDate = calenderComponent.convertStringToDate(date);
    Date currentDate = calenderComponent.convertStringToDate(currentDateStr);
    return inputDate != null && currentDate != null && inputDate.equals(currentDate);
  }

  private String getCurrentSystemTime() {
    LocalTime currentTime = LocalTime.now();
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mma");
    return currentTime.format(timeFormatter);
  }

  private void populateToTimeComboBox(boolean isCurrentDay) {
    String selectedFromTime = (String) fromTimeComboBox.getSelectedItem();
    LocalTime fromTime = convertToLocalTime(selectedFromTime);
    LocalTime endToTime;

    if (isCurrentDay) {
      String currentTimeStr = getCurrentSystemTime();
      endToTime = convertToLocalTime(currentTimeStr);
    } else {
      endToTime = convertToLocalTime("11:00pm");
    }
    for (String time : TIME) {
      LocalTime toTime = convertToLocalTime(time);
      if (toTime != null && !toTime.isBefore(fromTime) && !toTime.isAfter(endToTime)) {
        toTimeComboBox.addItem(time);
      }
    }
  }

  /**
   * Adds the applied filter from the UI to the provided Map
   *
   * @param appliedFilter Map
   */
  public void addAppliedFilter(Map<String, String> appliedFilter) {
    addSeveritiesFilter(appliedFilter, severityCheckBoxGroup.getSelectedCheckBoxes());
    addStatusFilter(appliedFilter, statusCheckBoxGroup.getSelectedCheckBoxes());
    addTimeFilter(appliedFilter);
  }

  private void addSeveritiesFilter(
      Map<String, String> appliedFilter, List<String> selectedSeverities) {
    if (CollectionUtils.isNotEmpty(selectedSeverities)) {
      StringBuilder severityFilter = new StringBuilder(StringUtils.EMPTY);
      for (int i = 0; i < selectedSeverities.size(); i++) {
        if (i == selectedSeverities.size() - 1) {
          severityFilter.append(selectedSeverities.get(i).toUpperCase());
        } else {
          severityFilter.append(selectedSeverities.get(i).toUpperCase()).append(",");
        }
      }
      String severity = isAssessComponent ? Constants.UTILS.SEVERITIES : Constants.UTILS.SEVERITY;
      appliedFilter.put(severity, severityFilter.toString());
    }
  }

  private void addStatusFilter(Map<String, String> appliedFilter, List<String> selectedStatus) {
    if (CollectionUtils.isNotEmpty(selectedStatus)) {
      StringBuilder statusFilter = new StringBuilder(StringUtils.EMPTY);
      for (int i = 0; i < selectedStatus.size(); i++) {
        if (i == selectedStatus.size() - 1) {
          statusFilter.append(mapStatus(selectedStatus.get(i)));
        } else {
          statusFilter.append(mapStatus(selectedStatus.get(i))).append(",");
        }
      }
      appliedFilter.put(Constants.UTILS.STATUS, statusFilter.toString());
    }
  }

  private void addTimeFilter(Map<String, String> appliedFilter) {
    String selectedItem = durationComboBox.getSelectedItem().toString();
    if (StringUtils.equals(selectedItem, Constants.UTILS.CUSTOM)) {
      Map<String, String> appliedTimeFilter = getCustomTime();
      String startTime = appliedTimeFilter.get(Constants.UTILS.START_DATE);
      if (StringUtils.isNotEmpty(startTime)) {
        appliedFilter.put(Constants.UTILS.START_DATE, startTime);
      }
      String endTime = appliedTimeFilter.get(Constants.UTILS.END_DATE);
      if (StringUtils.isNotEmpty(endTime)) {
        appliedFilter.put(Constants.UTILS.END_DATE, endTime);
      }
    } else {
      String selectedDuration = durationComboBox.getSelectedItem().toString();
      String filteredDuration = getSelectedTime(selectedDuration);
      if (StringUtils.isNotEmpty(filteredDuration)) {
        appliedFilter.put(Constants.UTILS.START_DATE, filteredDuration);
        appliedFilter.put(
            Constants.UTILS.END_DATE,
            String.valueOf(ZonedDateTime.now().toInstant().toEpochMilli()));
      }
    }
  }

  private String mapStatus(String status) {
    return switch (status) {
      case Constants.CHECKBOXES.REPORTED -> Constants.CHECKBOXES.REPORTED;
      case Constants.CHECKBOXES.CONFIRMED -> Constants.CHECKBOXES.CONFIRMED;
      case Constants.CHECKBOXES.SUSPICIOUS -> Constants.CHECKBOXES.SUSPICIOUS;
      case Constants.CHECKBOXES.NOT_A_PROBLEM -> Constants.CHECKBOXES.NOT_A_PROBLEM_REQUEST_KEY;
      case Constants.CHECKBOXES.REMEDIATED -> Constants.CHECKBOXES.REMEDIATED;
      case Constants.CHECKBOXES.FIXED -> Constants.CHECKBOXES.FIXED;
      case Constants.CHECKBOXES.REOPENED -> Constants.CHECKBOXES.REOPENED;
      case Constants.CHECKBOXES.REMEDIATED_AUTO_VERIFIED -> Constants.CHECKBOXES.AUTO_REMEDIATED;
      default -> StringUtils.EMPTY;
    };
  }

  public Map<String, String> getAppliedScanFilters() {
    Map<String, String> filters = new HashMap<>();
    List<String> severityGroup = severityCheckBoxGroup.getSelectedCheckBoxes();
    if (CollectionUtils.isNotEmpty(severityGroup)) {
      StringBuilder severity = new StringBuilder();
      for (int i = 0; i < severityGroup.size(); i++) {
        severity.append(severityGroup.get(i));
        if (i != severityGroup.size() - 1) {
          severity.append(",");
        }
      }
      filters.put(Constants.UTILS.SEVERITY, severity.toString());
    }
    List<String> statusGroup = statusCheckBoxGroup.getSelectedCheckBoxes();
    if (CollectionUtils.isNotEmpty(statusGroup)) {
      StringBuilder status = new StringBuilder();
      for (int i = 0; i < statusGroup.size(); i++) {
        if (statusGroup.get(i).equalsIgnoreCase(Constants.CHECKBOXES.REMEDIATED_AUTO_VERIFIED)) {
          status.append(Constants.CHECKBOXES.REMEDIATED_AUTO_VERIFIED_REQUEST_KEY);
        } else if (statusGroup.get(i).equalsIgnoreCase(Constants.CHECKBOXES.NOT_A_PROBLEM)) {
          status.append(Constants.CHECKBOXES.NOT_A_PROBLEM_SCAN_REQUEST_KEY);
        } else {
          status.append(statusGroup.get(i));
        }
        if (i != statusGroup.size() - 1) {
          status.append(",");
        }
      }
      filters.put(Constants.UTILS.STATUS, status.toString());
    }
    return filters;
  }
}
