package com.example.websocket.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String password; // This will be encrypted (e.g., "$2a$10$...")
    private Set<String> friendIds = new HashSet<>();
    private Set<String> friendRequests = new HashSet<>();

    // Constructors
    public User() {}
    
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<String> getFriendIds() {
        return friendIds;
    }

    public void setFriendIds(Set<String> friendIds) {
        this.friendIds = friendIds;
    }

    public Set<String> getFriendRequests() {
        return friendRequests;
    }

    public void setFriendRequests(Set<String> friendRequests) {
        this.friendRequests = friendRequests;
    }
}