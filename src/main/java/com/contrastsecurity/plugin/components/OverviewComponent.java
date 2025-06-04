/*******************************************************************************
 * Copyright © 2025 Contrast Security, OSS.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.components;

import com.contrastsecurity.assess.v3.dto.Chapter;
import com.contrastsecurity.plugin.constants.Constants;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import java.awt.BorderLayout;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class OverviewComponent extends JBPanel {

  private JBLabel contentLabel;

  public OverviewComponent() {
    setLayout(new BorderLayout());
    contentLabel = new JBLabel();
    add(contentLabel, BorderLayout.NORTH);
  }

  public void loadOverView(String firstSeen, String lastSeen, String risk, List<Chapter> chapters) {
    StringBuilder builder = new StringBuilder();
    builder.append(Constants.HTML.OPEN_HTML_CONTENT);
    builder.append(Constants.HTML.OPEN_BOLD);
    builder.append(Constants.WHAT_HAPPENED);
    builder.append(Constants.HTML.CLOSE_BOLD);
    builder.append(Constants.HTML.BREAK);
    addWhatHappenedContent(builder, chapters);
    builder.append(Constants.HTML.BREAK);
    builder.append(StringUtils.SPACE);
    builder.append(Constants.HTML.BREAK);
    builder.append(Constants.HTML.OPEN_BOLD);
    builder.append(Constants.WHAT_IS_THE_RISK);
    builder.append(Constants.HTML.CLOSE_BOLD);
    builder.append(Constants.HTML.BREAK);
    builder.append(risk);
    builder.append(Constants.HTML.BREAK);
    builder.append(StringUtils.SPACE);
    builder.append(Constants.HTML.BREAK);
    builder.append(Constants.HTML.OPEN_BOLD);
    builder.append(Constants.FIRST_DETECTED_DATE);
    builder.append(Constants.HTML.CLOSE_BOLD);
    builder.append(Constants.HTML.BREAK);
    builder.append(firstSeen);
    builder.append(Constants.HTML.BREAK);
    builder.append(StringUtils.SPACE);
    builder.append(Constants.HTML.BREAK);
    builder.append(Constants.HTML.OPEN_BOLD);
    builder.append(Constants.LAST_DETECTED_DATE);
    builder.append(Constants.HTML.CLOSE_BOLD);
    builder.append(Constants.HTML.BREAK);
    builder.append(lastSeen);
    builder.append(Constants.HTML.BREAK);
    builder.append(Constants.HTML.CLOSE_HTML_CONTENT);
    contentLabel.setText(builder.toString());
  }

  private void addWhatHappenedContent(StringBuilder builder, List<Chapter> chapters) {
    chapters.forEach(
        chapter -> {
          builder.append(chapter.getIntroText());
          builder.append(chapter.getBody());
        });
  }
}
