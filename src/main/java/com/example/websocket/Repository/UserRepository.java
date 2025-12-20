package com.example.websocket.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.websocket.model.User;

public interface UserRepository extends MongoRepository<User, String> {
    // This finds a user by name (Needed for Login)
    Optional<User> findByUsername(String username);
    
    // This checks if a username exists (Needed for Registration)
    Boolean existsByUsername(String username);
}