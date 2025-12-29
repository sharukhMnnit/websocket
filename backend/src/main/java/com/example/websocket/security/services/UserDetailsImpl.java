package com.example.websocket.security.services;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.websocket.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore; // Import for empty list

public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    @JsonIgnore
    private String password;

    public UserDetailsImpl(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    // This converts your DB User -> Spring Security User
    public static UserDetailsImpl build(User user) {
        return new UserDetailsImpl(
            user.getId(), 
            user.getUsername(), 
            user.getPassword());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // We are not doing Roles (Admin/User) yet, so return empty
    }
    // ... inside UserDetailsImpl class ...
    public String getId() {
        return id;
    }
    
    // ... keep the other methods ...

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    
}
