package com.example.ATSCIRCLE.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Models.UserManagement.Employee;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JwtService {
    private static final String SECRET_KEY = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    // ✅ Generate token for Admin (Users table)
 public String generateToken(Users user) {
    return Jwts.builder()
            .setSubject(user.getEmail())
            .claim("id", user.getId())
            .claim("organizationId", user.getId())
            .claim("roles", List.of(user.getRole().name()))
            .claim("type", user.getOrganizationType())  // ← MAP ORGANIZATION TYPE HERE
            .claim("organizationName", user.getOrganizationName())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
}
public String generateTokenForEmployee(Employee employee, String organizationId) {
    return Jwts.builder()
            .setSubject(employee.getEmail())
            .claim("id", employee.getId())
            .claim("organizationId", organizationId)
            // .claim("role", employee.getRole().name())
            .claim("roles", List.of(employee.getRole().name())) // ✅ ADD THIS
            .claim("type", "EMPLOYEE")
            .claim("createdBy", employee.getCreatedBy())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
}

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ✅ Extract specific claims
    public String extractId(String token) {
        return extractClaim(token, claims -> claims.get("id", String.class));
    }

    public String extractOrganizationId(String token) {
        return extractClaim(token, claims -> claims.get("organizationId", String.class));
    }

    public String extractType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
}