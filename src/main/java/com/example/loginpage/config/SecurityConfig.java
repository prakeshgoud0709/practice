package com.example.loginpage.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.example.loginpage.service.CustomUserDetailsServices;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsServices customUserDetailsServices;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // Disable CSRF protection for simplicity; enable in production
            .authorizeRequests()
                .requestMatchers("/api/auth/**").permitAll() // Allow access to authentication endpoints
                .anyRequest().authenticated() // All other requests require authentication
            .and()
            .formLogin() // Configure form-based login
                .loginPage("/signin") // Custom sign-in page
                .successHandler((request, response, authentication) -> {
                    // Redirect based on role after successful login
                    var authorities = authentication.getAuthorities();
                    String redirectUrl = authorities.stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ADMIN"))
                            ? "/adminwelcome"
                            : "/employeewelcome"; // Redirect to different dashboards for admin and employee
                    response.sendRedirect(redirectUrl);
                })
                .permitAll() // Allow all to access sign-in page
            .and()
            .logout() // Configure logout
                .logoutSuccessUrl("/signin") // Redirect to sign-in page on logout
                .invalidateHttpSession(true) // Invalidate session on logout
                .permitAll(); // Allow all to logout

        return http.build();
    }

    
    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
            .userDetailsService(customUserDetailsServices)
            .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }
}
