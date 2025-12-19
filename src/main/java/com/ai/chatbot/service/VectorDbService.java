package com.ai.chatbot.service;

import com.ai.chatbot.config.RagConfig;
import com.ai.chatbot.entity.DocumentChunk;
import com.ai.chatbot.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorDbService {
    
    private final DocumentChunkRepository repository;
    private final RagConfig config;
    
    @Transactional
    public void storeDocumentChunks(String documentName, List<String> chunks, List<float[]> embeddings) {
        log.info("Storing {} chunks for document: {}", chunks.size(), documentName);
        
        try {
            // Delete existing chunks for this document
            repository.deleteByDocumentName(documentName);
            
            // Create and save new chunks
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setContent(chunks.get(i));
                chunk.setEmbedding(formatVector(embeddings.get(i)));
                chunk.setDocumentName(documentName);
                chunk.setChunkIndex(i);
                
                repository.save(chunk);
            }
            
            log.info("Successfully stored {} chunks for document: {}", chunks.size(), documentName);
        } catch (Exception e) {
            log.error("Error storing document chunks for: {}", documentName, e);
            throw new RuntimeException("Error storing document chunks", e);
        }
    }
    
    public List<DocumentChunk> findSimilarChunks(float[] queryEmbedding) {
        log.info("Finding similar chunks with top-k: {}", config.getTopKChunks());
        
        try {
            String embeddingString = formatVector(queryEmbedding);
            List<DocumentChunk> chunks = repository.findSimilarChunks(embeddingString, config.getTopKChunks());
            log.info("Found {} similar chunks", chunks.size());
            return chunks;
        } catch (Exception e) {
            log.error("Error finding similar chunks", e);
            throw new RuntimeException("Error finding similar chunks", e);
        }
    }
    
    public List<DocumentChunk> findSimilarChunksWithThreshold(float[] queryEmbedding) {
        log.info("Finding similar chunks with threshold: {} and top-k: {}", 
                config.getSimilarityThreshold(), config.getTopKChunks());
        
        try {
            String embeddingString = formatVector(queryEmbedding);
            
            // Try with threshold first
            List<DocumentChunk> chunks = repository.findSimilarChunksWithThreshold(
                embeddingString, config.getSimilarityThreshold(), config.getTopKChunks());
            
            // Fallback: if no chunks found with threshold, get top-k without threshold
            if (chunks.isEmpty()) {
                log.info("No chunks found with threshold, falling back to top-k without threshold");
                chunks = repository.findSimilarChunks(embeddingString, Math.max(3, config.getTopKChunks()));
            }
            
            log.info("Found {} similar chunks (with fallback strategy)", chunks.size());
            return chunks;
        } catch (Exception e) {
            log.error("Error finding similar chunks with threshold, falling back to basic search", e);
            
            // Final fallback: basic search without threshold
            try {
                String embeddingString = formatVector(queryEmbedding);
                List<DocumentChunk> chunks = repository.findSimilarChunks(embeddingString, Math.max(3, config.getTopKChunks()));
                log.info("Fallback search found {} chunks", chunks.size());
                return chunks;
            } catch (Exception fallbackException) {
                log.error("Fallback search also failed", fallbackException);
                throw new RuntimeException("Error finding similar chunks", e);
            }
        }
    }
    
    public List<String> getChunkContents(List<DocumentChunk> chunks) {
        return chunks.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.toList());
    }
    
    public String buildContextFromChunks(List<DocumentChunk> chunks) {
        return chunks.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.joining("\n\n"));
    }
    
    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
