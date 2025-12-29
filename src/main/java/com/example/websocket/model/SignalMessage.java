package com.example.websocket.model;

public class SignalMessage {
    private String type;      // "offer", "answer", "candidate"
    private String sender;    // "Sharukh"
    private String receiver;  // "Shavez"
    private Object data;      // The SDP JSON or ICE Candidate JSON

    // Constructors
    public SignalMessage() {}

    public SignalMessage(String type, String sender, String receiver, Object data) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.data = data;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}