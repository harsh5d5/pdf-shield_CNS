package com.cns.project;

import org.springframework.stereotype.Service;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class KeyService {

    public Map<String, String> generateRSAKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        Map<String, String> keys = new HashMap<>();
        keys.put("privateKey", Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
        keys.put("publicKey", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        
        return keys;
    }
}
