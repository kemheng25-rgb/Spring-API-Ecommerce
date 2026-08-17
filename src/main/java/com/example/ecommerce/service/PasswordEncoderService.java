package com.example.ecommerce.service;

import com.example.ecommerce.exception.InvalidOperationException;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordEncoderService {
    
    private static final SecureRandom random = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 512;
    
    public String encode(String password) {
        try {
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
            for (int i = 1; i < ITERATIONS; i++) {
                md.reset();
                md.update(hashedPassword);
                hashedPassword = md.digest();
            }
            
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);
            
            return String.format("%s:%s", saltBase64, hashBase64);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new InvalidOperationException("SHA-256 algorithm not available", e);
        }
    }
    
    public boolean matches(String password, String encodedPassword) {
        try {
            String[] parts = encodedPassword.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String storedHash = parts[1];
            
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
            for (int i = 1; i < ITERATIONS; i++) {
                md.reset();
                md.update(hashedPassword);
                hashedPassword = md.digest();
            }
            
            String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);
            return hashBase64.equals(storedHash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new InvalidOperationException("SHA-256 algorithm not available", e);
        }
    }
}
