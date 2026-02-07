package com.abdelrahman.service;

import com.abdelrahman.model.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final String secretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    public JwtService() {
        this.secretKey = generateSecretKey(); //generate secret key once
    }

    // Generate Token
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        UserPrincipal userPrincipal = (UserPrincipal) userDetails;
        claims.put("id", userPrincipal.getUser().getId()); // Adding custom claim
        claims.put("username", userPrincipal.getUser().getName()); // Adding custom claim

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(generateKey())
                .compact();
    }

    private Key generateKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateSecretKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            SecretKey secretKey = keyGen.generateKey();
            System.out.println("Secret key generated: " + secretKey.toString());
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error Generating Secret Key", e);
        }
    }


    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }


    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    public Long getExpirationTime() {
        return jwtExpiration;
    }


    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    private Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parser()
                    .verifyWith((SecretKey) generateKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException ex) {
            throw new SignatureException("Invalid JWT signature");
        } catch (ExpiredJwtException ex) {
            throw new ExpiredJwtException(null, null, "JWT token has expired");
        } catch (MalformedJwtException ex) {
            throw new MalformedJwtException("Invalid JWT token format");
        } catch (UnsupportedJwtException ex) {
            throw new UnsupportedJwtException("JWT token is not supported");
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("JWT token is empty or null");
        }
    }
}