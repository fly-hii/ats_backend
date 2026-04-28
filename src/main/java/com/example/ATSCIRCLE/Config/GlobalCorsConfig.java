// package com.example.ATSCIRCLE.Config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
// import org.springframework.web.filter.CorsFilter;

// import java.util.List;

// @Configuration
// public class GlobalCorsConfig {

//     @Bean
//     public CorsFilter corsFilter() {
//         CorsConfiguration config = new CorsConfiguration();
//         config.setAllowCredentials(true);
        
//         // ✅ Add allowed origins
//         config.setAllowedOrigins(List.of(
//             "http://localhost:3000",  // React dev server
//             "https://www.atscircle.com"  // Your production domain
//         ));
        
//         config.setAllowedHeaders(List.of("*")); // Allow all headers
//         config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
//         config.setExposedHeaders(List.of("Authorization"));

//         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//         source.registerCorsConfiguration("/**", config);

//         return new CorsFilter(source);
//     }

    
// }

package com.example.ATSCIRCLE.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // ✅ Allow all origins using pattern
        config.setAllowedOriginPatterns(List.of("*"));
        
        // ⚠️ When using allowedOriginPatterns with "*", you must set allowCredentials to true
        config.setAllowCredentials(true);
        
        // ✅ Allow all headers
        config.setAllowedHeaders(List.of("*"));
        
        // ✅ Allow all HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // ✅ Expose headers that clients can access
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        
        // ✅ Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}