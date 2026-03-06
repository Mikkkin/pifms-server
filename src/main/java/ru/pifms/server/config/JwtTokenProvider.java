package ru.pifms.server.config;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private final String issuer;
    private final Key key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    
    public JwtTokenProvider(
        @Value("${jwt.issuer}") String issuer,
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access.ttl-seconds}") long accessTtlSeconds,
        @Value("${jwt.refresh.ttl-seconds}") long refreshTtlSeconds
    ) {
        this.issuer = issuer;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }


    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
            .setIssuer(issuer)
            .setSubject(subject)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(accessTtlSeconds)))
            .addClaims(claims) 
            .claim("typ", "access")
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }


    public String generateRefreshToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
            .setIssuer(issuer)
            .setSubject(subject)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
            .addClaims(claims) 
            .claim("typ", "refresh")
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }


    public Instant calculateAccessTokenExpiry() {
        return Instant.now().plusSeconds(accessTtlSeconds);
    }


    public Instant calculateRefreshTokenExpiry() {
        return Instant.now().plusSeconds(refreshTtlSeconds);
    }


    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parserBuilder()
            .requireIssuer(issuer)
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token);
    }


    public boolean isAccessToken(Jws<Claims> jws) {
        Object t = jws.getBody().get("typ");
        return t != null && "access".equals(t.toString());
    }


    public boolean isRefreshToken(Jws<Claims> jws) {
        Object t = jws.getBody().get("typ");
        return t != null && "refresh".equals(t.toString());
    }


    public Instant getExpiry(Jws<Claims> jws) {
        return jws.getBody().getExpiration().toInstant();
    }
}
