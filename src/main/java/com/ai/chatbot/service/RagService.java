package com.ai.chatbot.service;

import com.ai.chatbot.dto.ChatResponse;
import com.ai.chatbot.dto.UploadResponse;
import com.ai.chatbot.entity.ChatHistory;
import com.ai.chatbot.entity.DocumentChunk;
import com.ai.chatbot.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
    
    private final LlamaParseService llamaParseService;
    private final EmbeddingService embeddingService;
    private final VectorDbService vectorDbService;
    private final LlamaService llamaService;
    private final ChatHistoryRepository chatHistoryRepository;
    
    @Transactional
    public UploadResponse processDocument(byte[] fileContent, String fileName) {
        log.info("Processing document: {}", fileName);
        
        try {
            // Step 1: Parse document using LlamaParse
            String parsedText = llamaParseService.parseDocument(fileContent, fileName);
            log.info("Parsed document: {} with {} characters", fileName, parsedText.length());
            
            // Step 2: Chunk the text
            List<String> chunks = embeddingService.chunkText(parsedText);
            log.info("Created {} chunks for document: {}", chunks.size(), fileName);
            
            // Step 3: Generate embeddings for each chunk
            List<float[]> embeddings = chunks.stream()
                    .map(embeddingService::generateEmbedding)
                    .collect(Collectors.toList());
            log.info("Generated {} embeddings for document: {}", embeddings.size(), fileName);
            
            // Step 4: Store chunks and embeddings in vector database
            vectorDbService.storeDocumentChunks(fileName, chunks, embeddings);
            log.info("Stored chunks and embeddings for document: {}", fileName);
            
            return UploadResponse.builder()
                    .message("Document processed successfully")
                    .fileName(fileName)
                    .chunksCreated(chunks.size())
                    .timestamp(LocalDateTime.now())
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing document: {}", fileName, e);
            return UploadResponse.builder()
                    .message("Error processing document: " + e.getMessage())
                    .fileName(fileName)
                    .chunksCreated(0)
                    .timestamp(LocalDateTime.now())
                    .success(false)
                    .build();
        }
    }
    
    @Transactional
    public ChatResponse chat(String question) {
        log.info("Processing chat question: {}", question);
        
        try {
            // Step 1: Generate embedding for the question
            float[] questionEmbedding = embeddingService.generateEmbedding(question);
            log.info("Generated embedding for question");
            
            // Step 2: Find similar chunks
            List<DocumentChunk> similarChunks = vectorDbService.findSimilarChunksWithThreshold(questionEmbedding);
            log.info("Found {} similar chunks", similarChunks.size());
            
            // Step 3: Build context from chunks
            String context = vectorDbService.buildContextFromChunks(similarChunks);
            log.info("Built context with {} characters", context.length());
            
            // Step 4: Build RAG prompt
            String prompt = llamaService.buildRagPrompt(question, context);
            log.info("Built RAG prompt with {} characters", prompt.length());
            
            // Step 5: Generate response using LLaMA
            String answer = llamaService.generateResponse(prompt);
            log.info("Generated answer with {} characters", answer.length());
            
            // Step 6: Store chat history
            List<String> chunkContents = vectorDbService.getChunkContents(similarChunks);
            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setQuestion(question);
            chatHistory.setAnswer(answer);
            chatHistory.setRetrievedChunks(chunkContents);
            
            ChatHistory savedHistory = chatHistoryRepository.save(chatHistory);
            log.info("Stored chat history with ID: {}", savedHistory.getId());
            
            return ChatResponse.builder()
                    .answer(answer)
                    .retrievedChunks(chunkContents)
                    .timestamp(LocalDateTime.now())
                    .chatId(savedHistory.getId())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing chat question: {}", question, e);
            throw new RuntimeException("Error processing chat question", e);
        }
    }
    
    public List<ChatHistory> getChatHistory() {
        return chatHistoryRepository.findTop10ByOrderByCreatedAtDesc();
    }
}


