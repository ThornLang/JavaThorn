package com.thorn.stdlib;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.*;

/**
 * Cryptographic operations module for ThornLang
 * Provides hashing, encryption, signing, and other crypto functions.
 */
public class Crypto {
    
    /**
     * Compute MD5 hash
     * @param data Data to hash (string or byte list)
     * @return Hex string of hash
     */
    public static String md5(Object data) {
        return hash("MD5", data);
    }
    
    /**
     * Compute SHA-1 hash
     * @param data Data to hash
     * @return Hex string of hash
     */
    public static String sha1(Object data) {
        return hash("SHA-1", data);
    }
    
    /**
     * Compute SHA-256 hash
     * @param data Data to hash
     * @return Hex string of hash
     */
    public static String sha256(Object data) {
        return hash("SHA-256", data);
    }
    
    /**
     * Compute SHA-384 hash
     * @param data Data to hash
     * @return Hex string of hash
     */
    public static String sha384(Object data) {
        return hash("SHA-384", data);
    }
    
    /**
     * Compute SHA-512 hash
     * @param data Data to hash
     * @return Hex string of hash
     */
    public static String sha512(Object data) {
        return hash("SHA-512", data);
    }
    
    /**
     * Generic hash function
     * @param algorithm Hash algorithm name
     * @param data Data to hash
     * @return Hex string of hash
     */
    private static String hash(String algorithm, Object data) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] bytes = toBytes(data);
            byte[] hash = md.digest(bytes);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new StdlibException("Unsupported hash algorithm: " + algorithm);
        }
    }
    
    /**
     * Create a hash object for incremental hashing
     * @param algorithm Hash algorithm name
     * @return Hash object
     */
    public static Hash createHash(String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return new Hash(md);
        } catch (NoSuchAlgorithmException e) {
            throw new StdlibException("Unsupported hash algorithm: " + algorithm);
        }
    }
    
    /**
     * Hash object for incremental hashing
     */
    public static class Hash {
        private final MessageDigest digest;
        
        Hash(MessageDigest digest) {
            this.digest = digest;
        }
        
        public void update(Object data) {
            digest.update(toBytes(data));
        }
        
        public List<Object> digest() {
            byte[] hash = digest.digest();
            return bytesToList(hash);
        }
        
        public String hexdigest() {
            byte[] hash = digest.digest();
            return bytesToHex(hash);
        }
        
        public Hash copy() {
            try {
                MessageDigest newDigest = (MessageDigest) digest.clone();
                return new Hash(newDigest);
            } catch (CloneNotSupportedException e) {
                throw new StdlibException("Cannot clone hash object");
            }
        }
    }
    
    /**
     * Compute HMAC
     * @param key Secret key
     * @param data Data to authenticate
     * @param algorithm Hash algorithm (default SHA-256)
     * @return HMAC hex string
     */
    public static String hmac(Object key, Object data, String algorithm) {
        if (algorithm == null) algorithm = "SHA-256";
        String macAlgorithm = "Hmac" + algorithm.replace("-", "");
        
        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            SecretKeySpec secretKey = new SecretKeySpec(toBytes(key), macAlgorithm);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(toBytes(data));
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            throw new StdlibException("HMAC error: " + e.getMessage());
        }
    }
    
    /**
     * Generate a secure random key for encryption
     * @param algorithm Encryption algorithm (default AES-256)
     * @return Key bytes
     */
    public static List<Object> generateKey(String algorithm) {
        if (algorithm == null) algorithm = "AES-256";
        
        int keySize;
        String keyAlgorithm;
        
        if (algorithm.startsWith("AES-")) {
            keyAlgorithm = "AES";
            String bits = algorithm.substring(4);
            keySize = Integer.parseInt(bits) / 8;
        } else {
            throw new StdlibException("Unsupported key algorithm: " + algorithm);
        }
        
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(keyAlgorithm);
            keyGen.init(keySize * 8);
            SecretKey key = keyGen.generateKey();
            return bytesToList(key.getEncoded());
        } catch (Exception e) {
            throw new StdlibException("Key generation error: " + e.getMessage());
        }
    }
    
    /**
     * Encrypt data using AES-GCM
     * @param data Data to encrypt
     * @param key Encryption key
     * @param algorithm Algorithm (default AES-256-GCM)
     * @return Map with ciphertext, nonce, and tag
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> encrypt(Object data, Object key, String algorithm) {
        if (algorithm == null) algorithm = "AES-256-GCM";
        
        if (!algorithm.contains("GCM")) {
            throw new StdlibException("Only GCM mode supported currently");
        }
        
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(toBytes(key), "AES");
            
            // Generate random nonce
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce);
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] ciphertext = cipher.doFinal(toBytes(data));
            
            Map<String, Object> result = new HashMap<>();
            result.put("ciphertext", bytesToList(ciphertext));
            result.put("nonce", bytesToList(nonce));
            result.put("algorithm", algorithm);
            
            return result;
        } catch (Exception e) {
            throw new StdlibException("Encryption error: " + e.getMessage());
        }
    }
    
    /**
     * Decrypt data encrypted with AES-GCM
     * @param encrypted Encrypted data map
     * @param key Decryption key
     * @return Decrypted bytes
     */
    @SuppressWarnings("unchecked")
    public static List<Object> decrypt(Map<String, Object> encrypted, Object key) {
        try {
            List<Object> ciphertext = (List<Object>) encrypted.get("ciphertext");
            List<Object> nonce = (List<Object>) encrypted.get("nonce");
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(toBytes(key), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, listToBytes(nonce));
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] plaintext = cipher.doFinal(listToBytes(ciphertext));
            
            return bytesToList(plaintext);
        } catch (Exception e) {
            throw new StdlibException("Decryption error: " + e.getMessage());
        }
    }
    
    /**
     * Generate cryptographically secure random bytes
     * @param n Number of bytes
     * @return Random bytes
     */
    public static List<Object> randomBytes(double n) {
        int count = (int) n;
        byte[] bytes = new byte[count];
        new SecureRandom().nextBytes(bytes);
        return bytesToList(bytes);
    }
    
    /**
     * Generate a secure random token
     * @param length Token length (default 32)
     * @return URL-safe base64 token
     */
    public static String randomToken(Double length) {
        int len = length != null ? length.intValue() : 32;
        byte[] bytes = new byte[len * 3 / 4 + 1];
        new SecureRandom().nextBytes(bytes);
        
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return token.substring(0, len);
    }
    
    /**
     * Generate salt for password hashing
     * @param length Salt length (default 16)
     * @return Salt bytes
     */
    public static List<Object> generateSalt(Double length) {
        int len = length != null ? length.intValue() : 16;
        return randomBytes(len);
    }
    
    /**
     * Base64 encode
     * @param data Data to encode
     * @return Base64 string
     */
    public static String base64Encode(Object data) {
        return Base64.getEncoder().encodeToString(toBytes(data));
    }
    
    /**
     * Base64 decode
     * @param data Base64 string
     * @return Decoded bytes
     */
    public static List<Object> base64Decode(String data) {
        byte[] bytes = Base64.getDecoder().decode(data);
        return bytesToList(bytes);
    }
    
    /**
     * URL-safe Base64 encode
     * @param data Data to encode
     * @return URL-safe base64 string
     */
    public static String base64urlEncode(Object data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(toBytes(data));
    }
    
    /**
     * URL-safe Base64 decode
     * @param data Base64 string
     * @return Decoded bytes
     */
    public static List<Object> base64urlDecode(String data) {
        byte[] bytes = Base64.getUrlDecoder().decode(data);
        return bytesToList(bytes);
    }
    
    /**
     * Hex encode
     * @param data Data to encode
     * @return Hex string
     */
    public static String hexEncode(Object data) {
        return bytesToHex(toBytes(data));
    }
    
    /**
     * Hex decode
     * @param data Hex string
     * @return Decoded bytes
     */
    public static List<Object> hexDecode(String data) {
        int len = data.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(data.charAt(i), 16) << 4)
                                 + Character.digit(data.charAt(i+1), 16));
        }
        return bytesToList(bytes);
    }
    
    /**
     * Constant-time comparison of byte arrays
     * @param a First byte array
     * @param b Second byte array
     * @return true if equal
     */
    public static boolean compareDigest(Object a, Object b) {
        byte[] bytesA = toBytes(a);
        byte[] bytesB = toBytes(b);
        
        if (bytesA.length != bytesB.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < bytesA.length; i++) {
            result |= bytesA[i] ^ bytesB[i];
        }
        
        return result == 0;
    }
    
    // Helper methods
    
    private static byte[] toBytes(Object data) {
        if (data instanceof String) {
            return ((String) data).getBytes(StandardCharsets.UTF_8);
        } else if (data instanceof List) {
            return listToBytes((List<?>) data);
        } else {
            throw new StdlibException("Data must be string or byte list");
        }
    }
    
    private static byte[] listToBytes(List<?> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof Double) {
                bytes[i] = ((Double) item).byteValue();
            } else {
                throw new StdlibException("Byte list must contain numbers");
            }
        }
        return bytes;
    }
    
    private static List<Object> bytesToList(byte[] bytes) {
        List<Object> list = new ArrayList<>();
        for (byte b : bytes) {
            list.add((double)(b & 0xFF));
        }
        return list;
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}