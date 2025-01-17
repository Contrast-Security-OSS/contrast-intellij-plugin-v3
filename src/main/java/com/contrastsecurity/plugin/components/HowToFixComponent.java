/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.components;

import com.contrastsecurity.assess.v3.dto.HowToFixVulnerability;
import com.contrastsecurity.plugin.constants.Constants;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import java.awt.BorderLayout;
import org.apache.commons.lang3.StringUtils;

public class HowToFixComponent extends JBPanel {

  private JBLabel contentLabel;

  public HowToFixComponent() {
    setLayout(new BorderLayout());
    contentLabel = new JBLabel();
    add(contentLabel, BorderLayout.NORTH);
  }

  /**
   * Loads How to Fix panels content based on the selected vulnerability
   *
   * @param howToFixVulnerability data from cache
   */
  public void loadHowToFix(HowToFixVulnerability howToFixVulnerability) {
    StringBuilder builder = new StringBuilder();
    builder.append(Constants.HTML.OPEN_HTML_CONTENT);
    builder.append(howToFixVulnerability.getRecommendation().getText());
    builder.append(Constants.HTML.BREAK);
    builder.append(StringUtils.SPACE);
    builder.append(Constants.HTML.BREAK);

    builder.append(howToFixVulnerability.getCwe());
    builder.append(Constants.HTML.BREAK);
    builder.append(StringUtils.SPACE);
    builder.append(Constants.HTML.BREAK);

    builder.append(howToFixVulnerability.getOwasp());
    builder.append(Constants.HTML.BREAK);
    builder.append(StringUtils.SPACE);
    builder.append(Constants.HTML.BREAK);

    builder.append(howToFixVulnerability.getRule_references().getText());
    builder.append(Constants.HTML.CLOSE_HTML_CONTENT);
    contentLabel.setText(builder.toString());
  }
}
