package com.example.urlshortener.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.urlshortener.model.User;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Base64;
import javax.crypto.KeyGenerator;

@Service
public class JwtService {
    private KeyGenerator keyGen;
    private byte[] secretKey;
    private final String SECRET_KEY;
    private final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds

    public JwtService() throws NoSuchAlgorithmException {
        keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // for example
        secretKey = keyGen.generateKey().getEncoded();
        SECRET_KEY = Base64.getEncoder().encodeToString(secretKey); // You should store this securely.
    }

    public String generateToken(User user) {
        Date expirationDate = new Date(System.currentTimeMillis() + EXPIRATION_TIME);
        return JWT.create()
                .withSubject(user.getId().toString())
                .withExpiresAt(expirationDate)
                .sign(Algorithm.HMAC512(SECRET_KEY));
    }

    public DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC512(SECRET_KEY))
                .build()
                .verify(token);
    }
}
