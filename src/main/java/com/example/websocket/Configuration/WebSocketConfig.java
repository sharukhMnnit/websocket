// package com.example.websocket.Configuration;

// import org.springframework.context.annotation.Configuration;
// import org.springframework.messaging.simp.config.MessageBrokerRegistry;
// import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
// import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
// import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// @Configuration
// @EnableWebSocketMessageBroker
// public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

//     @Override
//     public void registerStompEndpoints(StompEndpointRegistry registry) {
//         // This is the endpoint your HTML "Connect" button hits
//         registry.addEndpoint("/ws")
//                 .setAllowedOriginPatterns("*")
//                 .withSockJS();
//     }

// @Override
//     public void configureMessageBroker(MessageBrokerRegistry config) {
//         // FIX: Add "/queue" here! 
//         // "/topic" is for public chat, "/queue" is for private messages.
//         config.enableSimpleBroker("/topic", "/queue");
        
//         config.setApplicationDestinationPrefixes("/app");
        
//         // This is the default, but good to be explicit for private routing:
//         config.setUserDestinationPrefix("/user"); 
//     }
// }


package com.example.websocket.configuration;

import com.example.websocket.security.jwt.JwtUtils;
import com.example.websocket.security.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    // --- THIS IS THE MISSING PART ---
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // 1. Check if it is a CONNECT command
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    
                    // 2. Extract the Token from the STOMP header
                    String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
                    
                    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                        String jwt = authorizationHeader.substring(7);
                        
                        // 3. Validate Token
                        if (jwtUtils.validateJwtToken(jwt)) {
                            String username = jwtUtils.getUserNameFromJwtToken(jwt);
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            
                            // 4. Create Authentication
                            UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            
                            // 5. ATTACH THE USER TO THE WEBSOCKET SESSION
                            accessor.setUser(authentication);
                            System.out.println("WS AUTH SUCCESS: User " + username + " connected to WebSocket!");
                        }
                    }
                }
                return message;
            }
        });
    }
}