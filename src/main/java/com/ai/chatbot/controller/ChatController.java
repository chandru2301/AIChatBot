package com.ai.chatbot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.chatbot.dto.ChatRequest;
import com.ai.chatbot.dto.ChatResponse;
import com.ai.chatbot.entity.ChatHistory;
import com.ai.chatbot.service.RagService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {
    
    private final RagService ragService;
    
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getQuestion());
        
        try {
            ChatResponse response = ragService.chat(request.getQuestion());
            log.info("Successfully processed chat request");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing chat request: {}", request.getQuestion(), e);
            return ResponseEntity.internalServerError()
                    .body(ChatResponse.builder()
                            .answer("Sorry, I encountered an error while processing your question. Please try again.")
                            .timestamp(java.time.LocalDateTime.now())
                            .build());
        }
    }
    
    @GetMapping("/chat/history")
    public ResponseEntity<List<ChatHistory>> getChatHistory() {
        log.info("Retrieving chat history");
        
        try {
            List<ChatHistory> history = ragService.getChatHistory();
            log.info("Retrieved {} chat history entries", history.size());
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            log.error("Error retrieving chat history", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RAG Chatbot is running!");
    }
}


