package com.ai.chatbot.service;

import com.ai.chatbot.config.LlamaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlamaService {
    
    private final LlamaConfig config;
    private final WebClient.Builder webClientBuilder;
    
    public String generateResponse(String prompt) {
        log.info("Generating response with LLaMA model: {}", config.getModelName());
        
        try {
            WebClient webClient = webClientBuilder.build();
            
            Map<String, Object> requestBody = Map.of(
                "model", config.getModelName(),
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                    "temperature", config.getTemperature(),
                    "num_predict", config.getMaxTokens()
                )
            );
            
            Mono<Map> response = webClient.post()
                    .uri(config.getModelUrl())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class);
            
            Map result = response.block();
            
            if (result != null && result.containsKey("response")) {
                String responseText = (String) result.get("response");
                log.info("Successfully generated response with {} characters", responseText.length());
                return responseText;
            } else {
                log.error("Failed to generate response. Result: {}", result);
                throw new RuntimeException("Failed to generate response from LLaMA model");
            }
            
        } catch (Exception e) {
            log.error("Error generating response from LLaMA model", e);
            throw new RuntimeException("Error generating response from LLaMA model", e);
        }
    }
    
    public String buildRagPrompt(String question, String context) {
        return String.format("""
            Context:
            %s
            
            Question:
            %s
            
            Answer based only on the context and question:
            """, context, question);
    }
}


