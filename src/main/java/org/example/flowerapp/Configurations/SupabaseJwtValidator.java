package org.example.flowerapp.Configurations;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class SupabaseJwtValidator {

    @Value("${supabase.jwt.secret}")
    private String jwtSecret;

    public DecodedJWT validateToken(String token) {
        try {
            System.out.println("Validating token...");
            System.out.println("Token starts with: " + token.substring(0, Math.min(20, token.length())));
            System.out.println("JWT Secret configured: " + (jwtSecret != null && !jwtSecret.isEmpty()));

            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .build()
                    .verify(token);

            System.out.println("Token validated successfully!");
            System.out.println("User ID: " + decodedJWT.getSubject());
            return decodedJWT;
        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Invalid token", e);
        }
    }

    public String getUserIdFromToken(String token) {
        DecodedJWT jwt = validateToken(token);
        return jwt.getSubject(); // This is the user_id
    }
}