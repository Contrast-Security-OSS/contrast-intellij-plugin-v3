/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.components;

import com.contrastsecurity.plugin.constants.Constants;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import java.awt.BorderLayout;

public class HTTPRequestComponent extends JBPanel {

  private JBLabel contentLabel;

  public HTTPRequestComponent() {
    setLayout(new BorderLayout());
    contentLabel = new JBLabel();
    add(contentLabel, BorderLayout.NORTH);
  }

  public void loadHttpRequest(String content) {
    StringBuilder builder = new StringBuilder();
    builder.append(Constants.HTML.OPEN_HTML_CONTENT);
    builder.append(Constants.HTML.OPEN_PARA);
    builder.append(content.replace("\n", "<br>"));
    builder.append(Constants.HTML.CLOSE_PARA);
    builder.append(Constants.HTML.CLOSE_HTML_CONTENT);
    contentLabel.setText(builder.toString());
  }
}
