/*
 * Copyright 2016 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.flute.licensing;

import javax.crypto.Cipher;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CertificateSpike
{
    public static void main(String[] args) throws Exception
    {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final PrivateKey privateKey = keyPair.getPrivate();
        final PublicKey publicKey = keyPair.getPublic();

        final String start = "The lazy programmer goes to StackOverflow";
        final String encoded = encrypt(privateKey, start);
        final String tampered = encoded.replace('X', 'x');
        final String decoded = decrypt(publicKey, encoded);

        System.out.println(start);
        System.out.println(encoded);
        System.out.println(tampered);
        System.out.println(decoded);

        final String signature = sign(privateKey, start);

        System.out.println("Signature: " + signature);

        verify(publicKey, start, signature);
        verify(publicKey, start, signature.replace('R', 'r'));
    }

    static String encrypt(final Key key, final String data) throws Exception
    {

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key.getEncoded());

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePrivate(keySpec));

        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(UTF_8)));
    }

    static String decrypt(final Key key, final String encoded) throws Exception
    {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key.getEncoded());

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(keySpec));

        return new String(cipher.doFinal(Base64.getDecoder().decode(encoded)), UTF_8);
    }

    static String sign(final Key key, final String data) throws Exception
    {
        final Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        final byte[] digest = md5.digest(data.getBytes(UTF_8));

        return Base64.getEncoder().encodeToString(cipher.doFinal(digest));
    }

    static void verify(final Key key, final String data, final String signature) throws Exception
    {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted =  cipher.doFinal(Base64.getDecoder().decode(signature.getBytes(UTF_8)));

        MessageDigest md5 = MessageDigest.getInstance("MD5");

        md5.update(data.getBytes(UTF_8));
        byte[] digest = md5.digest();

        if(!Arrays.equals(decrypted, digest))
        {
            throw new RuntimeException();
        }
    }
}
