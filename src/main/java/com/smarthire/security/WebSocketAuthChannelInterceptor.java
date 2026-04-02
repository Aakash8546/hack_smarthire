package com.smarthire.security;

import java.security.Principal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if ((StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand()))
                && accessor.getUser() == null) {
            authenticate(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = resolveAuthorizationHeader(accessor);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            SecurityUser securityUser = (SecurityUser) userDetails;
            if (jwtService.isTokenValid(token, securityUser)) {
                Principal principal = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                accessor.setUser(principal);
            }
        } catch (Exception exception) {
            log.debug("WebSocket JWT authentication failed: {}", exception.getMessage());
        }
    }

    private String resolveAuthorizationHeader(StompHeaderAccessor accessor) {
        List<String> authorizationHeaders = accessor.getNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
            return authorizationHeaders.get(0);
        }
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            String token = tokenHeaders.get(0);
            return token.startsWith("Bearer ") ? token : "Bearer " + token;
        }
        if (accessor.getSessionAttributes() != null) {
            Object sessionToken = accessor.getSessionAttributes().get(WebSocketHandshakeAuthInterceptor.WEBSOCKET_TOKEN_ATTRIBUTE);
            if (sessionToken != null) {
                return String.valueOf(sessionToken);
            }
        }
        return null;
    }
}
