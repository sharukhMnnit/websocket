package com.example.websocket.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.websocket.model.ChatMessage;


import com.example.websocket.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatRepository extends MongoRepository<ChatMessage, String> {
    // This finds the conversation between two people
    List<ChatMessage> findBySenderAndReceiverOrSenderAndReceiver(
            String sender1, String receiver1, 
            String sender2, String receiver2
    );
}
