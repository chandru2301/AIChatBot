package com.ai.chatbot.service;

import com.ai.chatbot.config.LlamaParseConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlamaParseService {
    
    private final LlamaParseConfig config;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    private static final String LLAMA_API_BASE_URL = "https://api.cloud.llamaindex.ai/api/v1/parsing";
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 2000;
    
    public String parseDocument(byte[] fileContent, String fileName) {
        log.info("Starting document parsing for file: {}", fileName);
        
        try {
            WebClient webClient = webClientBuilder.build();
            
            // Step 1: Upload document and get job ID
            String jobId = uploadDocument(webClient, fileContent, fileName);
            log.info("Document uploaded successfully. Job ID: {}", jobId);
            
            // Step 2: Wait for job completion
            waitForJobCompletion(webClient, jobId);
            log.info("Document parsing completed for job: {}", jobId);
            
            // Step 3: Fetch parsed content
            String parsedContent = fetchParsedContent(webClient, jobId);
            log.info("Successfully retrieved parsed content for file: {} with {} characters", fileName, parsedContent.length());
            
            return parsedContent;
            
        } catch (Exception e) {
            log.error("Error parsing document: {}", fileName, e);
            throw new RuntimeException("Error parsing document: " + fileName, e);
        }
    }
    
    private String uploadDocument(WebClient webClient, byte[] fileContent, String fileName) {
        ByteArrayResource resource = new ByteArrayResource(fileContent) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        
        Mono<Map> response = webClient.post()
                .uri(LLAMA_API_BASE_URL + "/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", resource))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("LlamaParse API error: Status {}, Body: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("LlamaParse API error: " + clientResponse.statusCode() + " - " + errorBody));
                                }))
                .bodyToMono(Map.class);
        
        Map result = response.block();
        
        if (result != null && result.containsKey("id")) {
            return (String) result.get("id");
        } else {
            log.error("Failed to upload document. Response: {}", result);
            throw new RuntimeException("Failed to upload document: " + fileName);
        }
    }
    
    private void waitForJobCompletion(WebClient webClient, String jobId) {
        log.info("Waiting for parsing job {} to complete...", jobId);
        
        boolean completed = false;
        int attempts = 0;
        
        while (!completed && attempts < MAX_RETRIES) {
            attempts++;
            
            try {
                Mono<Map> response = webClient.get()
                        .uri(LLAMA_API_BASE_URL + "/job/" + jobId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                clientResponse -> clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.error("LlamaParse job status API error: Status {}, Body: {}", clientResponse.statusCode(), errorBody);
                                            return Mono.error(new RuntimeException("LlamaParse job status API error: " + clientResponse.statusCode() + " - " + errorBody));
                                        }))
                        .bodyToMono(Map.class);
                
                Map result = response.block();
                
                if (result != null && result.containsKey("status")) {
                    String status = (String) result.get("status");
                    log.info("Job {} status: {}", jobId, status);
                    
                    if ("SUCCESS".equalsIgnoreCase(status)) {
                        completed = true;
                        break;
                    } else if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status)) {
                        throw new RuntimeException("Document parsing job failed for job ID: " + jobId);
                    }
                }
                
                if (!completed) {
                    Thread.sleep(RETRY_DELAY_MS);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for job completion", e);
            } catch (Exception e) {
                log.warn("Error checking job status (attempt {}): {}", attempts, e.getMessage());
                if (attempts >= MAX_RETRIES) {
                    throw new RuntimeException("Document parsing timed out after " + MAX_RETRIES + " attempts", e);
                }
            }
        }
        
        if (!completed) {
            throw new RuntimeException("Document parsing timed out after " + MAX_RETRIES + " attempts");
        }
    }
    
    private String fetchParsedContent(WebClient webClient, String jobId) {
        Mono<Map> response = webClient.get()
                .uri(LLAMA_API_BASE_URL + "/job/" + jobId + "/result/markdown")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("LlamaParse result API error: Status {}, Body: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("LlamaParse result API error: " + clientResponse.statusCode() + " - " + errorBody));
                                }))
                .bodyToMono(Map.class);
        
        Map result = response.block();
        
        if (result != null && result.containsKey("markdown")) {
            return (String) result.get("markdown");
        } else {
            log.error("Failed to fetch parsed content. Response: {}", result);
            throw new RuntimeException("Failed to fetch parsed content for job ID: " + jobId);
        }
    }
}
