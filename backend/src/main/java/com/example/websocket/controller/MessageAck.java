/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
//  */

package com.example.websocket.controller;

import com.example.websocket.model.ChatMessage.MessageStatus;

public class MessageAck {

    private String messageId;
    private MessageStatus status;

    // --- Constructors ---
    public MessageAck() {
    }

    public MessageAck(String messageId, MessageStatus status) {
        this.messageId = messageId;
        this.status = status;
    }

    // --- Getters and Setters ---
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }
}
