package com.aplazo.challenge.aplazo_fullstack_challenge.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.JwtDecoderInitializationException;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.JwtGenerationException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Component
public class JwtService {

    private final RSAPrivateKey privateKey;

    public JwtService() {
        try {
            InputStream is = getClass().getResourceAsStream("/private_key.pem");
            String privateKeyPEM = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            this.privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new JwtDecoderInitializationException("Error loading private key", e);
        }
    }

    public String generateToken(String subject, long validitySeconds) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + validitySeconds * 1000L))
                    .issuer("Aplazo")
                    .build();

            SignedJWT signedJWT = new SignedJWT(new com.nimbusds.jose.JWSHeader(JWSAlgorithm.RS256), claims);
            signedJWT.sign(new RSASSASigner(privateKey));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new JwtGenerationException("Error generating JWT", e);
        }
    }

    public String extractSubject(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return claims.getSubject();
        } catch (ParseException e) {
            throw new JwtDecoderInitializationException("Error extracting subject from JWT", e);
        }
    }

    public Boolean isTokenExpired(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            return expirationTime != null && expirationTime.before(new Date());
        } catch (ParseException e) {
            throw new JwtDecoderInitializationException("Error checking token expiration", e);
        }
    }
}
