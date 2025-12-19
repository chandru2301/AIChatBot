package com.ai.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingConfig {
    private String modelName;
    private int chunkSize;
    private int chunkOverlap;
    private int vectorDimension;
}


