package com.ai.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chat_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "question", columnDefinition = "TEXT")
    private String question;
    
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;
    
    @ElementCollection
    @CollectionTable(name = "chat_history_retrieved_chunks", 
                     joinColumns = @JoinColumn(name = "chat_history_id"))
    @Column(name = "chunk_content", columnDefinition = "TEXT")
    private List<String> retrievedChunks;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}


