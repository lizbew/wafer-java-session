package com.viifly.wafer.util;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created on 2018/1/1.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

public class AesUtils {
    private static Cipher cipher;

    public static String aesDecrypt(String sessionKey, String iv, String encryptedData) throws Exception {
        if (cipher == null) {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }

        byte[] sessionKeyB = Base64.decodeBase64(sessionKey);
        byte[] ivB = Base64.decodeBase64(iv);
        byte[] encryptedDataB = Base64.decodeBase64(encryptedData);

        SecretKeySpec secretKey = new SecretKeySpec(sessionKeyB, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivB));
        return new String(cipher.doFinal(encryptedDataB));
    }
}
