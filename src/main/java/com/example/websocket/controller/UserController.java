package com.example.websocket.controller;

import com.example.websocket.model.User;
import com.example.websocket.Repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;

import com.example.websocket.payload.UserSummary;
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> getFriends(Principal principal) {
    // 1. Get the logged-in user
    User currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

    // 2. Get the list of Friend IDs
    Set<String> friendIds = currentUser.getFriendIds();

    // 3. Fetch details ONLY for those IDs
    List<User> friends = (List<User>) userRepository.findAllById(friendIds);

    // 4. Convert to simple list (hide passwords/sensitive data)
    List<UserSummary> response = friends.stream()
            .map(user -> new UserSummary(user.getUsername())) // Assuming UserSummary takes username
            .collect(Collectors.toList());

    return ResponseEntity.ok(response);
}
    // DTO for user summary
    @GetMapping("/search")
    public List<UserSummary> search(@RequestParam String query) {
    List<User> users = userRepository.findByUsernameContainingIgnoreCase(query);
    return users.stream()
        .map(u -> new UserSummary(u.getUsername()))
        .collect(Collectors.toList());
}  
} 