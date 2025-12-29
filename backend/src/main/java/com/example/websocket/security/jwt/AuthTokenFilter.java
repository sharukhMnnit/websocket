package com.example.websocket.security.jwt;

import com.example.websocket.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {
  @Autowired
  private JwtUtils jwtUtils;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
@Override
  protected void doFilterInternal(HttpServletRequest request,
  HttpServletResponse response, FilterChain filterChain)
  throws ServletException, IOException {
  try {
  // 1. Get JWT from the HTTP Header
  String jwt = parseJwt(request);

  // 2. Validate the JWT
  if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
  String username = jwtUtils.getUserNameFromJwtToken(jwt);

  // 3. Load the User details from Database
  UserDetails userDetails = userDetailsService.loadUserByUsername(username);

  // 4. Set the "Authentication" in Spring Security Context
  // (This effectively "logs them in" for this request)
  UsernamePasswordAuthenticationToken authentication =
  new UsernamePasswordAuthenticationToken(
  userDetails,
  null,
  userDetails.getAuthorities());
  authentication.setDetails(new
  WebAuthenticationDetailsSource().buildDetails(request));

  SecurityContextHolder.getContext().setAuthentication(authentication);
  }
  } catch (Exception e) {
  logger.error("Cannot set user authentication: {}", e.getMessage());
  }

  // 5. Continue the filter chain
  filterChain.doFilter(request, response);
  }
  // @Override

  // protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
  //     throws ServletException, IOException {
  //   try {
  //     String jwt = parseJwt(request);
  //     if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
  //       // PRINT 3: Confirm successful login for this request
  //       String username = jwtUtils.getUserNameFromJwtToken(jwt);
  //       System.out.println("AUTH SUCCESS: User " + username + " is accessing " + request.getRequestURI());

  //       UserDetails userDetails = userDetailsService.loadUserByUsername(username);
  //       // ... existing logic to set security context ...
  //     }
  //   } catch (Exception e) {
  //     System.out.println("AUTH FAILED for " + request.getRequestURI() + " Error: " + e.getMessage());
  //   }
  //   filterChain.doFilter(request, response);
  // }

  // Helper method to remove "Bearer " from the token string
  private String parseJwt(HttpServletRequest request) {
    String headerAuth = request.getHeader("Authorization");

    if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
      return headerAuth.substring(7);
    }

    return null;
  }

}