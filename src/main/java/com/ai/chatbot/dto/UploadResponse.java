package com.ai.chatbot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResponse {
    
    private String message;
    private String fileName;
    private int chunksCreated;
    private LocalDateTime timestamp;
    private boolean success;
}


