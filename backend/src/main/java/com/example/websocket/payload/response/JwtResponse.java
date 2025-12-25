package com.example.websocket.payload.response;

public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String id;
    private String username;

    public JwtResponse(String accessToken, String id, String username) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
    }

    // Getters and Setters
    public String getAccessToken() {
        return token;
    }

    public void setAccessToken(String accessToken) {
        this.token = accessToken;
    }

    public String getTokenType() {
        return type;
    }

    public void setTokenType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
