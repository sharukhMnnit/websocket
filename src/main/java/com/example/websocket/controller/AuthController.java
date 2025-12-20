package com.example.websocket.controller;

import com.example.websocket.model.User;
import com.example.websocket.payload.request.LoginRequest;
import com.example.websocket.payload.request.SignupRequest;
import com.example.websocket.payload.response.JwtResponse;
import com.example.websocket.payload.response.MessageResponse;
import com.example.websocket.Repository.UserRepository;
import com.example.websocket.security.jwt.JwtUtils;
import com.example.websocket.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {

        // 1. Check Username and Password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // 2. Set the Authentication in Context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 3. Generate JWT Token
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        // 4. Get User Details
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 5. Return the Token
        return ResponseEntity.ok(new JwtResponse(jwt, 
                                                 userDetails.getId(), 
                                                 userDetails.getUsername()));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        // 1. Check if username exists
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        // 2. Create new User account (Encode the password!)
        User user = new User(signUpRequest.getUsername(), 
                             encoder.encode(signUpRequest.getPassword()));

        // 3. Save to DB
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
