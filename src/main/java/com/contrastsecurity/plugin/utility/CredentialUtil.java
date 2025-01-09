/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/
package com.contrastsecurity.plugin.utility;

import com.contrastsecurity.plugin.models.ConfigurationDTO;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CredentialUtil {

  private static final String ALGORITHM = "AES/CBC/PKCS5PADDING";
  private static final String KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String AES_ALGORITHM = "AES";
  private static final int KEY_SIZE = 256;
  private static final int ITERATIONS = 65536;
  private static final int IV_SIZE = 16;
  private static final int SALT_SIZE = 16;

  private CredentialUtil() {}

  /**
   * Encrypts the sensitive information of provided DTO
   *
   * @param dto {@link ConfigurationDTO}
   */
  public static void encryptDTO(ConfigurationDTO dto) {
    // Encrypt API Key
    String encryptedAPIKey = encrypt(dto.getApiKey(), dto.getAppOrProjectID());
    dto.setApiKey(encryptedAPIKey);
    // Encrypt Service Key
    String encryptedServiceKey = encrypt(dto.getServiceKey(), dto.getAppOrProjectID());
    dto.setServiceKey(encryptedServiceKey);
  }

  /**
   * Decrypts the sensitive information of the provided {@link ConfigurationDTO}
   *
   * @param dto {@link ConfigurationDTO}
   * @return ConfigurationDTO
   */
  public static ConfigurationDTO decryptDTO(ConfigurationDTO dto) {
    ConfigurationDTO decryptedDTO = new ConfigurationDTO();
    decryptedDTO.setSource(dto.getSource());
    decryptedDTO.setContrastURL(dto.getContrastURL());
    decryptedDTO.setUserName(dto.getUserName());
    // Decrypt Service Key
    String decryptedServiceKey = decrypt(dto.getServiceKey(), dto.getAppOrProjectID());
    decryptedDTO.setServiceKey(decryptedServiceKey);
    // Decrypt API Key
    String decryptedAPIKey = decrypt(dto.getApiKey(), dto.getAppOrProjectID());
    decryptedDTO.setApiKey(decryptedAPIKey);
    decryptedDTO.setOrgId(dto.getOrgId());
    decryptedDTO.setAppOrProject(dto.getAppOrProject());
    decryptedDTO.setAppOrProjectID(dto.getAppOrProjectID());
    decryptedDTO.setOrgName(dto.getOrgName());
    decryptedDTO.setRefreshCycleMinutes(dto.getRefreshCycleMinutes());
    return decryptedDTO;
  }

  // Encrypts text using a secret key and returns Base64 encoded ciphertext with IV and salt
  private static String encrypt(String plaintext, String secret) {
    try {
      // Generate random salt and IV
      byte[] salt = generateRandomBytes(SALT_SIZE);
      byte[] iv = generateRandomBytes(IV_SIZE);
      IvParameterSpec ivSpec = new IvParameterSpec(iv);

      // Derive AES key from secret and salt
      SecretKeySpec secretKey = deriveKeyFromSecret(secret, salt);

      // Initialize cipher for encryption
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

      // Encrypt the plaintext
      byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Combine salt + IV + ciphertext and return Base64 encoded string
      byte[] combined = new byte[salt.length + iv.length + encryptedBytes.length];
      System.arraycopy(salt, 0, combined, 0, SALT_SIZE);
      System.arraycopy(iv, 0, combined, SALT_SIZE, IV_SIZE);
      System.arraycopy(encryptedBytes, 0, combined, SALT_SIZE + IV_SIZE, encryptedBytes.length);

      return Base64.getEncoder().encodeToString(combined);

    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | IllegalBlockSizeException
        | BadPaddingException e) {
      log.error(e.getMessage());
      throw new IllegalStateException("Failed to encrypt text", e);
    }
  }

  // Decrypts Base64 encoded ciphertext using secret key and returns the plaintext
  private static String decrypt(String encryptedText, String secret) {
    try {
      // Decode Base64 encoded input
      byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);

      // Extract salt, IV, and ciphertext
      byte[] salt = new byte[SALT_SIZE];
      byte[] iv = new byte[IV_SIZE];
      byte[] ciphertext = new byte[decodedBytes.length - SALT_SIZE - IV_SIZE];
      System.arraycopy(decodedBytes, 0, salt, 0, SALT_SIZE);
      System.arraycopy(decodedBytes, SALT_SIZE, iv, 0, IV_SIZE);
      System.arraycopy(decodedBytes, SALT_SIZE + IV_SIZE, ciphertext, 0, ciphertext.length);

      // Derive AES key from secret and salt
      SecretKeySpec secretKey = deriveKeyFromSecret(secret, salt);
      IvParameterSpec ivSpec = new IvParameterSpec(iv);

      // Initialize cipher for decryption
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

      // Decrypt and return plaintext
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);

    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | IllegalBlockSizeException
        | BadPaddingException e) {
      log.error(e.getMessage());
      throw new IllegalStateException("Failed to decrypt text", e);
    }
  }

  // Derives a SecretKeySpec for AES encryption from a secret and salt
  private static SecretKeySpec deriveKeyFromSecret(String secret, byte[] salt) {
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
      KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_SIZE);
      SecretKey tmp = factory.generateSecret(spec);
      return new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      log.error(e.getMessage());
      throw new IllegalStateException("Failed to derive key from secret", e);
    }
  }

  // Generates a secure random byte array of specified size
  private static byte[] generateRandomBytes(int size) {
    byte[] bytes = new byte[size];
    new SecureRandom().nextBytes(bytes);
    return bytes;
  }
}
