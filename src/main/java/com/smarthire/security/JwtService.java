package com.smarthire.security;

import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import com.smarthire.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
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
}
