package com.ai.chatbot.controller;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ai.chatbot.dto.UploadResponse;
import com.ai.chatbot.service.RagService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UploadController {
    
    private final RagService ragService;
    
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("Received file upload request: {}", file.getOriginalFilename());
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(UploadResponse.builder()
                                .message("File is empty")
                                .fileName(file.getOriginalFilename())
                                .timestamp(LocalDateTime.now())
                                .success(false)
                                .build());
            }
            
            // Check file type
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".pdf") && 
                                   !fileName.toLowerCase().endsWith(".txt") && 
                                   !fileName.toLowerCase().endsWith(".docx"))) {
                return ResponseEntity.badRequest()
                        .body(UploadResponse.builder()
                                .message("Unsupported file type. Please upload PDF, TXT, or DOCX files")
                                .fileName(fileName)
                                .timestamp(LocalDateTime.now())
                                .success(false)
                                .build());
            }
            
            // Process document
            byte[] fileContent = file.getBytes();
            UploadResponse response = ragService.processDocument(fileContent, fileName);
            
            if (response.isSuccess()) {
                log.info("Successfully processed document: {}", fileName);
                return ResponseEntity.ok(response);
            } else {
                log.error("Failed to process document: {}", fileName);
                return ResponseEntity.internalServerError().body(response);
            }
            
        } catch (IOException e) {
            log.error("Error reading file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError()
                    .body(UploadResponse.builder()
                            .message("Error reading file: " + e.getMessage())
                            .fileName(file.getOriginalFilename())
                            .timestamp(LocalDateTime.now())
                            .success(false)
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error processing file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError()
                    .body(UploadResponse.builder()
                            .message("Unexpected error: " + e.getMessage())
                            .fileName(file.getOriginalFilename())
                            .timestamp(LocalDateTime.now())
                            .success(false)
                            .build());
        }
    }
}
