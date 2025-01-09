/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.utility;

import com.contrastsecurity.plugin.constants.Constants;
import com.intellij.DynamicBundle;
import java.io.Serializable;
import java.util.Locale;
import java.util.ResourceBundle;
import org.apache.commons.lang3.StringUtils;

public class LocalizationUtil implements Serializable {
  private static final String BUNDLE_NAME = Constants.UTILS.MESSAGES;
  private static LocalizationUtil instance;
  private ResourceBundle resourceBundle;

  private LocalizationUtil() {
    Locale locale = DynamicBundle.getLocale();
    if (StringUtils.equals(locale.getLanguage(), "ja")) {
      resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.JAPANESE);
    } else {
      resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
    }
  }

  /**
   * Returns a single ton object of the LocalizationUtil
   *
   * @return LocalizationUtil
   */
  public static LocalizationUtil getInstance() {
    if (instance == null) {
      instance = new LocalizationUtil();
    }
    return instance;
  }

  /**
   * sets the locale System locale based on the specified locale the resource bundle will be loaded
   * for the UI
   *
   * @param locale Object of Locale
   */
  public void setResourceBundle(Locale locale) {
    resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
  }

  /**
   * Returns the value from the resource bundle for the provided key
   *
   * @param key The Key where the value is mapped to
   * @return String value based on the provided key
   */
  public String getMessage(String key) {
    return resourceBundle.getString(key);
  }
}
