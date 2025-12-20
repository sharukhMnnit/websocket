package com.example.websocket.controller;

import com.example.websocket.model.User;
import com.example.websocket.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<String>> getAllUsers() {
        // Fetch all users from DB
        List<User> users = userRepository.findAll();
        
        // Extract only the usernames (we don't want to send passwords!)
        List<String> usernames = users.stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        return ResponseEntity.ok(usernames);
    }
}
