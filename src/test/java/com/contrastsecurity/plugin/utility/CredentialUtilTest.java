package com.contrastsecurity.plugin.utility;

import com.contrastsecurity.plugin.models.ConfigurationDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CredentialUtilTest {
  private ConfigurationDTO configDTO;
  private final String apiKey = "";
  private final String serviceKey = "";

  @BeforeEach
  public void setup() {
    configDTO = new ConfigurationDTO();
    configDTO.setApiKey(apiKey);
    configDTO.setServiceKey(serviceKey);
    String appOrProjectID = "";
    configDTO.setAppOrProjectID(appOrProjectID);
  }

  @Test
  void testEncryptDTO() {
    // Encrypt data
    CredentialUtil.encryptDTO(configDTO);

    // Ensure encryption changes original data
    Assertions.assertNotEquals(
        apiKey, configDTO.getApiKey(), "API Key should be encrypted and different from original.");
    Assertions.assertNotEquals(
        serviceKey,
        configDTO.getServiceKey(),
        "Service Key should be encrypted and different from original.");
  }

  @Test
  void testDecryptDTO() {
    CredentialUtil.encryptDTO(configDTO);
    // Decrypt data
    ConfigurationDTO decryptedDTO = CredentialUtil.decryptDTO(configDTO);

    // Ensure decrypted data remains same
    Assertions.assertEquals(apiKey, decryptedDTO.getApiKey(), "API key should be decrypted");
    Assertions.assertEquals(
        serviceKey, decryptedDTO.getServiceKey(), "Service key should be decrypted");
  }
}
