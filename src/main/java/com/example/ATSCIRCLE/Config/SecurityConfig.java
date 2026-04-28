package com.example.ATSCIRCLE.Config;

import com.example.ATSCIRCLE.security.JwtAuthenticationFilter;
import com.example.ATSCIRCLE.security.UserInfoDetailsService;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter authFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserInfoDetailsService();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // ✅ WebSocket endpoints - MUST be first
                .requestMatchers("/ws/**", "/topic/**", "/app/**").permitAll()
                
                // ✅ Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/public/**").permitAll()
 
            // ── ADD THESE: OAuth2 routes need to be accessible ──
            .requestMatchers("/api/oauth2/**").permitAll()
                .requestMatchers("/api/interview-slots/**").permitAll()
                // ✅ CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // ✅ Admin only auth routes
                .requestMatchers(HttpMethod.GET, "/api/auth/users").hasRole("SUPERADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/auth/users/*/payment-status").hasRole("SUPERADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/auth/users/*/status").hasRole("SUPERADMIN")
                
                // ✅ Authenticated user profile route
                .requestMatchers(HttpMethod.GET, "/api/auth/myprofile").authenticated()
                
                .requestMatchers(HttpMethod.POST, "/api/employees/addEmployee").hasAnyRole("ADMIN", "USER_MANAGEMENT")
                .requestMatchers(HttpMethod.GET, "/api/employees/role/*").hasAnyRole("ADMIN", "USER_MANAGEMENT")
                .requestMatchers(HttpMethod.GET, "/api/employees/status/*").hasAnyRole("ADMIN", "USER_MANAGEMENT")
                .requestMatchers(HttpMethod.GET, "/api/employees/stats/count").hasAnyRole("ADMIN", "USER_MANAGEMENT")
                .requestMatchers(HttpMethod.PATCH, "/api/employees/*/activate").hasAnyRole("ADMIN", "USER_MANAGEMENT")
                .requestMatchers(HttpMethod.PATCH, "/api/employees/*/inactivate").hasAnyRole("ADMIN", "USER_MANAGEMENT")
                .requestMatchers(HttpMethod.PUT, "/api/employees/*/password").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/employees/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/employees/*").hasAnyRole("ADMIN", "USER_MANAGEMENT")
                .requestMatchers(HttpMethod.GET, "/api/employees").hasAnyRole("ADMIN", "USER_MANAGEMENT")
                .requestMatchers(HttpMethod.GET, "/api/employees/*").authenticated()
                
                // ✅ All other requests require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            if (!response.isCommitted()) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(
                    String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\",\"status\":401,\"path\":\"%s\"}", 
                        authException.getMessage(), 
                        request.getRequestURI())
                );
            }
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            if (!response.isCommitted()) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(
                    String.format("{\"error\":\"Access Denied\",\"message\":\"%s\",\"status\":403,\"path\":\"%s\"}", 
                        accessDeniedException.getMessage(), 
                        request.getRequestURI())
                );
            }
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}