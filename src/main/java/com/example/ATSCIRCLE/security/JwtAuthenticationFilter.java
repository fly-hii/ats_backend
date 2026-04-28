// package com.example.ATSCIRCLE.security;

// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
// import org.springframework.stereotype.Component;
// import org.springframework.web.filter.OncePerRequestFilter;

// import io.jsonwebtoken.Claims;

// import java.io.IOException;
// import java.util.Collections;
// import java.util.List;
// import java.util.stream.Collectors;

// @Component
// public class JwtAuthenticationFilter extends OncePerRequestFilter {

//     @Autowired
//     private JwtService jwtService;

//     @Autowired
//     private UserInfoDetailsService userService;

//     @Override
//     protected void doFilterInternal(HttpServletRequest request,
//                                     HttpServletResponse response,
//                                     FilterChain filterChain)
//             throws ServletException, IOException {

//         String requestPath = request.getRequestURI();
        
//         // ✅ Skip JWT validation for WebSocket connections
//         if (requestPath.startsWith("/ws") || 
//             requestPath.startsWith("/topic") || 
//             requestPath.startsWith("/app") ||
//             requestPath.contains("/websocket") ||
//             requestPath.contains("/sockjs-node")) {
//             filterChain.doFilter(request, response);
//             return;
//         }
        
//         final String authHeader = request.getHeader("Authorization");
//         final String jwt;
//         final String userEmail;

//         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//             filterChain.doFilter(request, response);
//             return;
//         }

//         jwt = authHeader.substring(7);
        
//         try {
//             userEmail = jwtService.extractUsername(jwt);
            
//             if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                 UserDetails userDetails = userService.loadUserByUsername(userEmail);

//                 if (jwtService.isTokenValid(jwt, userDetails)) {
//                     Claims claims = jwtService.extractAllClaims(jwt);

//                     // ✅ Extract claims
//                     List<String> roles = claims.get("roles", List.class);
//                     String orgType = claims.get("organizationType", String.class);
//                     String userId = claims.get("userId", String.class);
//                     String userType = claims.get("userType", String.class);

//                     // ✅ FIXED: Add "ROLE_" prefix for Spring Security
//                     List<SimpleGrantedAuthority> authorities = roles != null
//                             ? roles.stream()
//                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
//                                    .collect(Collectors.toList())
//                             : Collections.emptyList();

//                     // ✅ Add organization type as authority (optional)
//                     if (orgType != null) {
//                         authorities.add(new SimpleGrantedAuthority("ORG_" + orgType));
//                     }

//                     // ✅ Create a custom authentication token that stores the JWT
// UsernamePasswordAuthenticationToken authToken =
//         new UsernamePasswordAuthenticationToken(
//                 userDetails,
//                 jwt,  // ✅ CHANGED: Pass JWT here instead of null
//                 authorities
//         );

//                     authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

//                     // ✅ Store additional info in request attributes
//                     request.setAttribute("userId", userId);
//                     request.setAttribute("userType", userType);

//                     SecurityContextHolder.getContext().setAuthentication(authToken);
                    
//                     // ✅ Debug log
//                     System.out.println("✅ Authenticated: " + userEmail + " | Type: " + userType + " | Roles: " + authorities);
//                 }
//             }
//         } catch (Exception e) {
//             System.err.println("❌ JWT Authentication Error: " + e.getMessage());
//             e.printStackTrace();
//         }

//         filterChain.doFilter(request, response);
//     }
// }

package com.example.ATSCIRCLE.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserInfoDetailsService userService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ws") ||
               path.startsWith("/topic") ||
               path.startsWith("/app") ||
               path.contains("/websocket") ||
               path.startsWith("/api/interview-slots/")||
               path.contains("/sockjs-node") ||
               // ✅ Skip JWT filter entirely for all OAuth routes

               path.startsWith("/api/oauth2/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        
        try {
            userEmail = jwtService.extractUsername(jwt);
            
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    Claims claims = jwtService.extractAllClaims(jwt);

                    List<String> roles = claims.get("roles", List.class);
                    String orgType = claims.get("organizationType", String.class);
                    String userId = claims.get("userId", String.class);
                    String userType = claims.get("userType", String.class);

                    List<SimpleGrantedAuthority> authorities = roles != null
                            ? roles.stream()
                                   .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                   .collect(Collectors.toList())
                            : Collections.emptyList();

                    if (orgType != null) {
                        authorities.add(new SimpleGrantedAuthority("ORG_" + orgType));
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    jwt,
                                    authorities
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    request.setAttribute("userId", userId);
                    request.setAttribute("userType", userType);

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println("✅ Authenticated: " + userEmail + " | Type: " + userType + " | Roles: " + authorities);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ JWT Authentication Error: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}