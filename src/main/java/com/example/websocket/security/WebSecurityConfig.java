package com.example.websocket.security;

import com.example.websocket.security.jwt.AuthEntryPointJwt;
import com.example.websocket.security.jwt.AuthTokenFilter;
import com.example.websocket.security.services.UserDetailsServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
  
  @Autowired
  UserDetailsServiceImpl userDetailsService;

  @Autowired
  private AuthEntryPointJwt unauthorizedHandler;

  @Bean
  public AuthTokenFilter authenticationJwtTokenFilter() {
    return new AuthTokenFilter();
  }

  // We use the full path to avoid "Constructor undefined" errors
  @Bean
  public org.springframework.security.authentication.dao.DaoAuthenticationProvider authenticationProvider() {
      org.springframework.security.authentication.dao.DaoAuthenticationProvider authProvider = 
          new org.springframework.security.authentication.dao.DaoAuthenticationProvider();
       
      authProvider.setUserDetailsService(userDetailsService);
      authProvider.setPasswordEncoder(passwordEncoder());
   
      return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> 
  auth
      // 1. Allow Public Pages & Resources
      .requestMatchers("/", "/index.html", "/login.html", "/favicon.ico", "/error").permitAll()
      .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
      
      // 2. Allow Auth APIs (Signup/Signin)
      .requestMatchers("/api/auth/**").permitAll()
      
      // 3. Allow WebSocket Connection
      .requestMatchers("/ws/**").permitAll()

      // 4. FIX: Allow Chat History & User List for logged-in users
      // This ensures the Bearer Token is checked for these paths
      .requestMatchers("/api/messages/**").authenticated() 
      .requestMatchers("/api/users/**").authenticated() 
      
      // 5. Block everything else
      .anyRequest().authenticated()
);

    http.authenticationProvider(authenticationProvider());
    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}