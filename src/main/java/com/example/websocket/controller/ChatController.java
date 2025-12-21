// package com.example.websocket.controller;

// import com.example.websocket.model.ChatMessage;
// import com.example.websocket.Repository.ChatRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.messaging.handler.annotation.MessageMapping;
// import org.springframework.messaging.handler.annotation.Payload;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.stereotype.Controller;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.ResponseBody;

// import java.time.LocalDateTime;
// import java.util.List;

// @Controller
// public class ChatController {

//     @Autowired
//     private ChatRepository chatRepository;

//     @Autowired
//     private SimpMessagingTemplate messagingTemplate;

//     @MessageMapping("/chat.sendMessage")
//     public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        
//         // 1. Stamp and Save the message to Cloud
//         chatMessage.setTimestamp(LocalDateTime.now().toString());
//         chatRepository.save(chatMessage);

//         // 2. Routing Logic
//         if (chatMessage.getReceiver() != null && !chatMessage.getReceiver().isEmpty()) {
//             // PRIVATE MODE
//             System.out.println("Private message to: " + chatMessage.getReceiver());
            
//             // Send to Receiver's Private Box
//             messagingTemplate.convertAndSend("/topic/private-" + chatMessage.getReceiver(), chatMessage);
            
//             // Also send to Sender's Private Box (so they can see what they sent)
//             messagingTemplate.convertAndSend("/topic/private-" + chatMessage.getSender(), chatMessage);
//         } else {
//             // PUBLIC MODE
//             System.out.println("Public message");
//             messagingTemplate.convertAndSend("/topic/public", chatMessage);
//         }
        
//         return chatMessage;
//     }

//     // // 3. Secure History Endpoint
//     // @GetMapping("/api/history")
//     // @ResponseBody
//     // public List<ChatMessage> getChatHistory(@RequestParam String user) {
//     //     // Only load messages relevant to THIS specific user
//     //     return chatRepository.findMyChatHistory(user);
//     // }
//     //aded method to get chat history between two users
//     @GetMapping("/api/messages/{user1}/{user2}")
//     public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String user1, @PathVariable String user2) {
//         // Find messages where (Sender=A AND Receiver=B) OR (Sender=B AND Receiver=A)
//         List<ChatMessage> messages = chatRepository.findBySenderAndReceiverOrSenderAndReceiver(
//                 user1, user2, 
//                 user2, user1
//         );
//         return ResponseEntity.ok(messages);
// }
// }
// package com.example.websocket.controller;

// import com.example.websocket.model.ChatMessage;
// import com.example.websocket.Repository.ChatRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.messaging.handler.annotation.MessageMapping;
// import org.springframework.messaging.handler.annotation.Payload;
// import org.springframework.messaging.handler.annotation.SendTo;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.stereotype.Controller;

// @Controller
// public class ChatController {

//     @Autowired
//     private SimpMessagingTemplate messagingTemplate;

//     @Autowired
//     private ChatRepository chatRepository; // 1. We need this repository

//     // --- PUBLIC CHAT ---
//     @MessageMapping("/chat.sendMessage")
//     @SendTo("/topic/public")
//     public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
//         // 2. SAVE TO DB
//         chatRepository.save(chatMessage);
        
//         return chatMessage;
//     }

//     @MessageMapping("/chat.addUser")
//     @SendTo("/topic/public")
//     public ChatMessage addUser(@Payload ChatMessage chatMessage) {
//         // We don't save "Join" messages to history, just return them
//         return chatMessage;
//     }

//     // --- PRIVATE CHAT ---
//     @MessageMapping("/chat.private")
//     public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
//         // 3. SAVE TO DB (This was missing!)
//         chatRepository.save(chatMessage);

//         messagingTemplate.convertAndSendToUser(
//                 chatMessage.getReceiver(),
//                 "/queue/messages",
//                 chatMessage
//         );
//     }
// }
package com.example.websocket.controller;

import com.example.websocket.model.ChatMessage;
import com.example.websocket.Repository.ChatRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatRepository chatRepository;

    // --- 1. PUBLIC CHAT (Websocket) ---
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        chatRepository.save(chatMessage); // Save every public message to DB
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage) {
        return chatMessage;
    }

    // --- 2. PRIVATE CHAT (Websocket) ---
@MessageMapping("/chat.private")
public void receivePrivateMessage(@Payload ChatMessage chatMessage) {
    // PRINT 1: Check the whole object
    System.out.println("--- NEW PRIVATE MESSAGE ARRIVED ---");
    System.out.println("Sender: " + chatMessage.getSender());
    System.out.println("Receiver: " + chatMessage.getReceiver());
    System.out.println("Content: " + chatMessage.getContent());

    if (chatMessage.getReceiver() == null || chatMessage.getReceiver().isEmpty()) {
        System.out.println("!!! ERROR: Receiver is NULL. Stopping send. !!!");
        return;
    }

    // Send instantly
    messagingTemplate.convertAndSendToUser(
        chatMessage.getReceiver(), 
        "/queue/messages", 
        chatMessage
    );
    System.out.println("Message pushed to /user/" + chatMessage.getReceiver() + "/queue/messages");

    // Save to database
    chatRepository.save(chatMessage);
    System.out.println("Message saved to MongoDB.");
}

    // --- 3. CHAT HISTORY API (REST) ---
    // This is the link your browser calls to load old messages
    @GetMapping("/api/messages/{user1}/{user2}")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable String user1, 
            @PathVariable String user2) {
        
        // Find messages where (Sender=A AND Receiver=B) OR (Sender=B AND Receiver=A)
        List<ChatMessage> messages = chatRepository.findBySenderAndReceiverOrSenderAndReceiver(
                user1, user2, 
                user2, user1
        );
        return ResponseEntity.ok(messages);
    }
}