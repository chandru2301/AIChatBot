package com.ai.chatbot.repository;

import com.ai.chatbot.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    
    List<ChatHistory> findAllByOrderByCreatedAtDesc();
    
    List<ChatHistory> findTop10ByOrderByCreatedAtDesc();
}


