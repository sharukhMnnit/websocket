package com.example.websocket.controller; // 1. CHANGE THIS to match your package

import com.example.websocket.model.User; // 2. Check your User class package
import com.example.websocket.Repository.UserRepository; // 3. Check your Repo package

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    @Autowired private UserRepository userRepository;

    // 1. Send Friend Request
    @PostMapping("/add/{targetUsername}")
    public ResponseEntity<?> sendRequest(@PathVariable String targetUsername, Principal principal) {
        User sender = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("Sender not found"));
        User target = userRepository.findByUsername(targetUsername).orElseThrow(() -> new RuntimeException("Target user not found"));

        if (sender.getUsername().equals(targetUsername)) {
            return ResponseEntity.badRequest().body("You cannot add yourself!");
        }

        // Add sender's ID to target's "requests" list
        target.getFriendRequests().add(sender.getId());
        userRepository.save(target);

        return ResponseEntity.ok("Request sent to " + targetUsername);
    }

    // 2. Accept Friend Request
    @PostMapping("/accept/{senderUsername}")
    public ResponseEntity<?> acceptRequest(@PathVariable String senderUsername, Principal principal) {
        User me = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("Current user not found"));
        User sender = userRepository.findByUsername(senderUsername).orElseThrow(() -> new RuntimeException("Sender not found"));

        // Check if request actually exists
        if (!me.getFriendRequests().contains(sender.getId())) {
            return ResponseEntity.badRequest().body("No request found from this user.");
        }

        // The Logic: Move ID from "Requests" to "Friends"
        me.getFriendRequests().remove(sender.getId());
        me.getFriendIds().add(sender.getId());
        
        // Also add ME to HIS friend list (Mutual friendship)
        sender.getFriendIds().add(me.getId());

        userRepository.save(me);
        userRepository.save(sender);

        return ResponseEntity.ok("Friend request accepted!");
    }
    //// In FriendController.java

@GetMapping("/requests")
public ResponseEntity<List<String>> getFriendRequests(Principal principal) {
    User me = userRepository.findByUsername(principal.getName()).orElseThrow();
    // Return just the list of Usernames who sent requests
    // (In a real app, you'd return DTOs with images, but strings work for now)
    List<String> senderNames = new ArrayList<>();
    for (String id : me.getFriendRequests()) {
        userRepository.findById(id).ifPresent(u -> senderNames.add(u.getUsername()));
    }
    return ResponseEntity.ok(senderNames);
}
}