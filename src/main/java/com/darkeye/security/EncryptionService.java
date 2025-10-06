package com.darkeye.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for secure data storage
 */
public class EncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 12; // 96 bits for GCM
    private static final int TAG_LENGTH = 16; // 128 bits for GCM
    
    private final SecretKey secretKey;
    private final SecureRandom secureRandom;
    
    public EncryptionService() {
        this.secureRandom = new SecureRandom();
        this.secretKey = generateKey();
    }
    
    public EncryptionService(String base64Key) {
        this.secureRandom = new SecureRandom();
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * Generate a new AES-256 key
     */
    private SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
    
    /**
     * Encrypt a string using AES-256-GCM
     * @param plaintext the text to encrypt
     * @return base64 encoded encrypted data with IV
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        
        try {
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedData = encrypt(plaintextBytes);
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Encrypt byte array using AES-256-GCM
     * @param plaintext the bytes to encrypt
     * @return encrypted data with IV prepended
     */
    public byte[] encrypt(byte[] plaintext) {
        if (plaintext == null) {
            return null;
        }
        
        try {
            // Generate random IV
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            // Encrypt
            byte[] encrypted = cipher.doFinal(plaintext);
            
            // Prepend IV to encrypted data
            byte[] result = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt a base64 encoded encrypted string
     * @param encryptedBase64 the base64 encoded encrypted data
     * @return decrypted plaintext
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null) {
            return null;
        }
        
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedBase64);
            byte[] decryptedBytes = decrypt(encryptedData);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * Decrypt byte array (with IV prepended)
     * @param encryptedData the encrypted data with IV
     * @return decrypted bytes
     */
    public byte[] decrypt(byte[] encryptedData) {
        if (encryptedData == null || encryptedData.length < IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }
        
        try {
            // Extract IV and encrypted data
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[encryptedData.length - IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, IV_LENGTH);
            System.arraycopy(encryptedData, IV_LENGTH, encrypted, 0, encrypted.length);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // Decrypt
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * Get the current encryption key as base64 string
     * @return base64 encoded key
     */
    public String getKeyAsBase64() {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
    
    /**
     * Generate a new key and return it as base64 string
     * @return base64 encoded new key
     */
    public static String generateNewKeyAsBase64() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate new key", e);
        }
    }
}
