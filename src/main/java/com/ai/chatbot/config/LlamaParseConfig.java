package com.ai.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "llamaparse")
public class LlamaParseConfig {
    private String apiKey;
    private String baseUrl;
}


