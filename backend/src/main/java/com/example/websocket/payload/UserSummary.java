package com.example.websocket.payload; // Change package if needed

public class UserSummary {
    private String username;

    public UserSummary(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}