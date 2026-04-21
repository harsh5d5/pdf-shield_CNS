package com.cns.project;

import org.springframework.stereotype.Service;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Service
public class SignatureService {

    public String signHash(String hash, String privateKeyStr) throws Exception {
        // 1. Convert the Base64 Private Key string back to a real PrivateKey object
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(spec);

        // 2. Initialize the Signing tool (SHA-256 with RSA)
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);

        // 3. Apply the signature to the hash bytes
        signature.update(hash.getBytes());
        byte[] digitalSignature = signature.sign();

        // 4. Return as Base64 for the UI
        return Base64.getEncoder().encodeToString(digitalSignature);
    }
}
