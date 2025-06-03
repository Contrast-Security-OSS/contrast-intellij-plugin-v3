/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.components;

import com.contrastsecurity.plugin.constants.Constants;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class CalenderComponent {

  private static JFrame frame;
  private JBLabel monthLabel;
  private JButton prevMonthButton;
  private JButton nextMonthButton;
  private JBPanel<?> calendarPanel;
  private int currentMonth;
  private int currentYear;
  private Calendar calendar;
  private FilterComponent component;
  private boolean isFrom;

  public CalenderComponent() {}

  public CalenderComponent(FilterComponent component, boolean isFrom) {
    this.isFrom = isFrom;
    this.component = component;

    if (frame != null && frame.isVisible()) {
      frame.dispose(); // Close the existing frame
    }

    frame = new JFrame(Constants.UTILS.CALENDER);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setSize(400, 400);

    // Initialize Calendar instance
    calendar = new GregorianCalendar();
    currentMonth = calendar.get(Calendar.MONTH);
    currentYear = calendar.get(Calendar.YEAR);

    // Create top panel with navigation buttons and month/year label
    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BorderLayout());

    prevMonthButton = new JButton(Constants.UTILS.PREVIOUS);
    nextMonthButton = new JButton(Constants.UTILS.NEXT);

    monthLabel = new JBLabel(StringUtils.EMPTY, SwingConstants.CENTER);
    updateMonthYearLabel();

    topPanel.add(prevMonthButton, BorderLayout.WEST);
    topPanel.add(monthLabel, BorderLayout.CENTER);
    topPanel.add(nextMonthButton, BorderLayout.EAST);

    frame.add(topPanel, BorderLayout.NORTH);

    // Add Action Listeners for buttons
    prevMonthButton.addActionListener(e -> goToPreviousMonth());
    nextMonthButton.addActionListener(e -> goToNextMonth());

    // Create calendar panel
    calendarPanel = new JBPanel<>();
    calendarPanel.setLayout(new GridLayout(0, 7)); // 7 columns for days of the week
    frame.add(calendarPanel, BorderLayout.CENTER);

    // Populate calendar panel with days of the month
    displayDaysOfMonth(currentYear, currentMonth);

    frame.setVisible(true);
    frame.setResizable(false);
  }

  private void updateMonthYearLabel() {
    String[] months = Constants.UTILS.MONTHS;
    monthLabel.setText(months[currentMonth] + " " + currentYear);
  }

  private void goToPreviousMonth() {
    currentMonth--;
    if (currentMonth < 0) {
      currentMonth = 11;
      currentYear--;
    }
    updateCalendarDisplay();
  }

  private void goToNextMonth() {
    currentMonth++;
    if (currentMonth > 11) {
      currentMonth = 0;
      currentYear++;
    }
    updateCalendarDisplay();
  }

  private void updateCalendarDisplay() {
    updateMonthYearLabel();
    displayDaysOfMonth(currentYear, currentMonth);
  }

  private void displayDaysOfMonth(int year, int month) {
    calendarPanel.removeAll();

    // Add day headers
    String[] headers = Constants.UTILS.WEEKS;
    for (String header : headers) {
      JLabel dayLabel = new JLabel(header, SwingConstants.CENTER);
      calendarPanel.add(dayLabel);
    }

    // Get first day of the month and number of days
    calendar.set(Calendar.MONTH, month);
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

    // Fill the empty days before the first day of the month
    for (int i = 1; i < firstDayOfWeek; i++) {
      calendarPanel.add(new JLabel(StringUtils.EMPTY)); // Empty labels for padding
    }

    // Add day numbers
    for (int day = 1; day <= daysInMonth; day++) {
      JButton dayButton = new JButton(String.valueOf(day));
      int finalDay = day;

      if (isFrom && isAfterCurrentDate(finalDay, month, year, getCurrentDate())) {
        dayButton.setEnabled(false);
      } else if (!isFrom && isBeforeFromDate(finalDay, month, year, component.getFromDate())) {
        dayButton.setEnabled(false);
      } else
        dayButton.setEnabled(
            !isFrom
                || component.getToDate().isEmpty()
                || (!isAfterCurrentDate(finalDay, month, year, component.getToDate())));

      dayButton.addActionListener(
          e -> {
            if (isFrom) {
              component.setFromDate(getSelectedDate(finalDay));
            } else {
              component.setToDate(getSelectedDate(finalDay));
            }
            frame.dispose();
          });
      calendarPanel.add(dayButton);
    }

    // Refresh panel
    calendarPanel.revalidate();
    calendarPanel.repaint();
  }

  private Date createNormalizedDate(int day, int month, int year) {
    Calendar calendarObject = Calendar.getInstance();
    calendarObject.set(year, month, day, 0, 0, 0);
    calendarObject.set(Calendar.MILLISECOND, 0);
    return calendarObject.getTime();
  }

  private boolean isBeforeFromDate(int day, int month, int year, String dateStr) {
    if (dateStr == null || dateStr.isEmpty()) {
      return false;
    }
    Date fromDate = convertStringToDate(dateStr);
    Date toDate = createNormalizedDate(day, month, year);
    return isAfterCurrentDate(day, month, year, getCurrentDate()) || toDate.before(fromDate);
  }

  private boolean isAfterCurrentDate(int day, int month, int year, String currentDate) {
    if (currentDate == null || currentDate.isEmpty()) {
      return false;
    }
    Date selectedDate = createNormalizedDate(day, month, year);
    return selectedDate.after(convertStringToDate(currentDate));
  }

  public String getCurrentDate() {
    return new SimpleDateFormat("dd/MM/yyyy").format(new Date());
  }

  public Date convertStringToDate(String toDateStr) {
    if (toDateStr == null || toDateStr.trim().isEmpty()) {
      return null;
    }
    try {
      return new SimpleDateFormat("dd/MM/yyyy").parse(toDateStr);
    } catch (ParseException e) {
      return null;
    }
  }

  private String getSelectedDate(int day) {
    // Format the selected day, month, and year as "dd/MM/yyyy"
    return String.format("%02d/%02d/%d", day, currentMonth + 1, currentYear);
  }
}
