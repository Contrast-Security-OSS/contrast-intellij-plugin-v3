/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.utility;

import com.contrastsecurity.plugin.constants.Constants;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class CheckBoxGroup {

  private List<JBCheckBox> checkBoxesList;
  private List<String> selectedCheckBoxes;

  public CheckBoxGroup(String[] checkBoxes) {
    checkBoxesList = new ArrayList<>();
    selectedCheckBoxes = new ArrayList<>();
    for (String checkBox : checkBoxes) {
      checkBoxesList.add(new JBCheckBox(checkBox));
    }
  }

  /**
   * Loads the list of provided checkboxes into the provided JBPanel
   *
   * @param panel JBPanel
   */
  public void loadCheckBoxesToPanel(JBPanel panel) {
    for (JBCheckBox checkBox : checkBoxesList) {
      panel.add(checkBox);
      String filter = checkBox.getText();
      if (StringUtils.equals(Constants.CHECKBOXES.CRITICAL, filter)
          || StringUtils.equals(Constants.CHECKBOXES.HIGH, filter)
          || StringUtils.equals(Constants.CHECKBOXES.MEDIUM, filter)
          || StringUtils.equals(Constants.CHECKBOXES.REPORTED, filter)
          || StringUtils.equals(Constants.CHECKBOXES.CONFIRMED, filter)
          || StringUtils.equals(Constants.CHECKBOXES.SUSPICIOUS, filter)) {
        checkBox.setSelected(true);
      }
    }
  }

  /**
   * Returns the List of selected checkboxes from the GUI, return data will be specific to the Panel
   * used for loading the checkboxes initially
   *
   * @return List of selectedCheckBoxes
   */
  public List<String> getSelectedCheckBoxes() {
    selectedCheckBoxes.clear();
    for (JBCheckBox checkBox : checkBoxesList) {
      if (checkBox.isSelected()) {
        selectedCheckBoxes.add(checkBox.getText());
      }
    }
    return selectedCheckBoxes;
  }

  /** Clears the selected checkbox based on the object reference */
  public void clearCheckBoxSelection() {
    for (JBCheckBox checkBox : checkBoxesList) {
      checkBox.setSelected(false);
    }
  }
}
