package com.example.websocket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages") // 1. Tells MongoDB to save this in the "messages" collection
public class ChatMessage {

    @Id // 2. MongoDB needs a unique ID for every single message
    private String id;

    private MessageType type; // CHAT, JOIN, or LEAVE
    private String content;
    private String sender;
    private String receiver;  // 3. Added for Private Messaging (1-to-1)
    private String timestamp; // 4. To save WHEN the message was sent

    // Enum to track what kind of message this is
    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }

    // --- Constructors ---
    public ChatMessage() {
    }

    public ChatMessage(String content, String sender, MessageType type) {
        this.content = content;
        this.sender = sender;
        this.type = type;
    }

    // --- Getters and Setters ---
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public MessageType getType() {
        return type;
    }
// Inside com.example.websocket.model.ChatMessage

public void setType(String type) {
    try {
        this.type = MessageType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
        // Fallback to a default or log the error
        this.type = MessageType.CHAT; 
    }
}

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}