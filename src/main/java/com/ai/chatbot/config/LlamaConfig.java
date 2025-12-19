package com.ai.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "llama")
public class LlamaConfig {
    private String modelUrl;
    private String modelName;
    private int maxTokens;
    private double temperature;
}


