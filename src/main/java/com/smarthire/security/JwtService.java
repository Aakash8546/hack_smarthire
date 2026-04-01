package com.smarthire.security;

import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;

import com.smarthire.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtService {

    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=\\r\\n]+$");

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = buildSigningKey(jwtProperties.secret());
    }

    public String generateToken(SecurityUser user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(Map.of("role", user.getAuthorities().iterator().next().getAuthority()))
                .subject(user.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.accessTokenExpiration()))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, SecurityUser user) {
        Claims claims = extractClaims(token);
        return claims.getSubject().equalsIgnoreCase(user.getUsername()) && claims.getExpiration().after(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey buildSigningKey(String secretValue) {
        String normalizedSecret = secretValue == null ? "" : secretValue.trim();
        byte[] keyBytes = tryDecodeBase64(normalizedSecret);
        if (keyBytes == null) {
            keyBytes = normalizedSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        try {
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException exception) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 bytes for HS256. Configure app.jwt.secret/JWT_SECRET with a stronger secret.",
                    exception
            );
        }
    }

    private byte[] tryDecodeBase64(String value) {
        if (value.isBlank() || !BASE64_PATTERN.matcher(value).matches()) {
            return null;
        }
        try {
            return Decoders.BASE64.decode(value);
        } catch (IllegalArgumentException exception) {
            log.debug("JWT secret is not valid Base64, using raw text secret instead.");
            return null;
        }
    }
}
