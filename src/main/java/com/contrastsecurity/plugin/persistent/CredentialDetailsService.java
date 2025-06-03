/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.persistent;

import com.contrastsecurity.plugin.constants.Constants;
import com.contrastsecurity.plugin.models.ConfigurationDTO;
import com.contrastsecurity.plugin.service.CacheDataService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "com.contrastsecurity.plugin.persistent.CredentialDetailsService",
    storages = @Storage("credentialDetails.xml"))
public class CredentialDetailsService
    implements PersistentStateComponent<CredentialDetailsService.State> {
  CacheDataService cacheDataService = new CacheDataService();

  public static class State {
    @NonNls public Set<ConfigurationDTO> configurationDTOS = new LinkedHashSet<>();
  }

  private State state = new State();

  @Nullable @Override
  public State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
  }

  /**
   * Returns a single ton instance of {@link CredentialDetailsService}
   *
   * @return CredentialDetailsService
   */
  public static CredentialDetailsService getInstance() {
    return ApplicationManager.getApplication().getService(CredentialDetailsService.class);
  }

  /**
   * Saves the Provided ConfigurationDTO as a new entry in local
   *
   * @param dto CredentialDTO with new creds
   * @return boolean value
   */
  public boolean save(ConfigurationDTO dto) {
    if (dto != null) {
      if (state.configurationDTOS.contains(dto)) {
        return false;
      } else {
        state.configurationDTOS.add(dto);
        return true;
      }
    }
    return false;
  }

  /**
   * Update the existing configuration with update data
   *
   * @param updatedDTO Updated configuration data
   * @return boolean
   */
  public boolean update(ConfigurationDTO updatedDTO, String selectedItem) {
    if (updatedDTO != null) {
      ConfigurationDTO dto = getSavedConfigDataByName(selectedItem, updatedDTO.getSource());
      if (dto != null) {
        cacheDataService.clearCacheById(dto.getAppOrProjectID());
        state.configurationDTOS.remove(dto);
        return state.configurationDTOS.add(updatedDTO);
      } else {
        return save(updatedDTO);
      }
    }
    return false;
  }

  /**
   * Deletes the saved configuration data based on the provided app or project name
   *
   * @param appOrProjectName App or Project name
   * @return boolean
   */
  public boolean delete(String appOrProjectName, String source) {
    ConfigurationDTO dto = getSavedConfigDataByName(appOrProjectName, source);
    if (Objects.nonNull(dto)) {
      cacheDataService.clearCacheById(dto.getAppOrProjectID());
      state.configurationDTOS.remove(dto);
      return true;
    }
    return false;
  }

  /**
   * Returns ConfigurationDTO object based on the provided App or Project Name
   *
   * @param name Selected Application or Project Name
   * @return ConfigurationDTO object for the provided name
   */
  public ConfigurationDTO getSavedConfigDataByName(String name, String source) {
    if (CollectionUtils.isNotEmpty(getSavedConfigData())) {
      for (ConfigurationDTO dto : getSavedConfigData()) {
        if (StringUtils.equals(name, dto.getAppOrProject())
            && StringUtils.equals(source, dto.getSource())) {
          return dto;
        }
      }
    }
    return null;
  }

  public ConfigurationDTO getSavedConfigDataByID(String appOrProjectID) {
    if (CollectionUtils.isNotEmpty(getSavedConfigData())) {
      for (ConfigurationDTO dto : getSavedConfigData()) {
        if (StringUtils.equals(dto.getAppOrProjectID(), appOrProjectID)) {
          return dto;
        }
      }
    }
    return null;
  }

  /**
   * Returns all the saved configuration values from the local
   *
   * @return Set of ConfigurationDTO
   */
  public Set<ConfigurationDTO> getSavedConfigData() {
    return state.configurationDTOS;
  }

  /**
   * Returns the list of saved application names
   *
   * @return List of Names
   */
  public List<String> getAllApplicationNames() {
    List<String> appNames = new ArrayList<>();
    for (ConfigurationDTO dto : state.configurationDTOS) {
      if (StringUtils.equals(dto.getSource(), Constants.ASSESS)) {
        appNames.add(dto.getAppOrProject());
      }
    }
    return appNames;
  }

  /**
   * Returns boolean value based on the provided name
   *
   * @param appOrProjectName App or Project name
   * @return boolean
   */
  public boolean doesConfigExists(String appOrProjectName, String source) {
    for (ConfigurationDTO dto : state.configurationDTOS) {
      if (StringUtils.equals(appOrProjectName, dto.getAppOrProject())
          && StringUtils.equals(source, dto.getSource())) {
        return true;
      }
    }
    return false;
  }
}
