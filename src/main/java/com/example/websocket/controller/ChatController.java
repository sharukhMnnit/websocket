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
import com.example.websocket.model.ChatMessage.MessageStatus;
import com.example.websocket.Repository.ChatRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatRepository chatRepository;

    // --- 1. PUBLIC CHAT (WebSocket) ---
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        chatRepository.save(chatMessage);
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage) {
        return chatMessage;
    }

    // --- 2. PRIVATE CHAT (Updated for Echo/Optimistic UI) ---
    @MessageMapping("/chat.private")
    public void receivePrivateMessage(@Payload ChatMessage chatMessage) {
        
        // A. Set Defaults
        chatMessage.setStatus(MessageStatus.SENT);
        chatMessage.setTimestamp(LocalDateTime.now().toString());

        // B. Save to DB FIRST (Generates the real unique ID)
        ChatMessage saved = chatRepository.save(chatMessage);

        // C. Send to Receiver (So they see the message with the Real ID)
        if (chatMessage.getReceiver() != null && !chatMessage.getReceiver().isEmpty()) {
            messagingTemplate.convertAndSendToUser(
                chatMessage.getReceiver(), 
                "/queue/messages", 
                saved 
            );
        }

        // D. Send to Sender (THE ECHO)
        // This gives the Sender the real DB ID so they can link Blue Ticks later
        messagingTemplate.convertAndSendToUser(
            chatMessage.getSender(), 
            "/queue/messages", 
            saved 
        );
        
        System.out.println("Message processed & saved with ID: " + saved.getId());
    }

    // --- 3. MESSAGE READ ACKNOWLEDGMENT (Blue Ticks) ---
    @MessageMapping("/chat.ack")
    public void acknowledgeMessage(@Payload MessageAck ack) {
        System.out.println("--- ACK RECEIVED ---");
        System.out.println("Message " + ack.getMessageId() + " status updated to " + ack.getStatus());

        // 1. Find the message in DB
        ChatMessage msg = chatRepository.findById(ack.getMessageId()).orElse(null);
        
        if(msg != null) {
            // 2. Update Status to READ/DELIVERED
            msg.setStatus(ack.getStatus());
            chatRepository.save(msg);
            
            // 3. Notify the SENDER that their message was read (Update their UI ticks)
            messagingTemplate.convertAndSendToUser(
                msg.getSender(),
                "/queue/ack",
                ack // Sends { messageId: "123", status: "READ" }
            );
        }
    }

    // --- 4. CHAT HISTORY API (REST) ---
    @GetMapping("/api/messages/{user1}/{user2}")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable String user1, 
            @PathVariable String user2) {
        
        List<ChatMessage> messages = chatRepository.findBySenderAndReceiverOrSenderAndReceiver(
                user1, user2, 
                user2, user1
        );
        return ResponseEntity.ok(messages);
    }
}