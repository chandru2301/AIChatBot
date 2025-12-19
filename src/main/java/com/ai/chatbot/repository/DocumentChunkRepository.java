package com.ai.chatbot.repository;

import com.ai.chatbot.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    
    @Query(value = "SELECT * FROM document_chunks ORDER BY embedding::vector <-> CAST(?1 AS vector) LIMIT ?2", nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(String embedding, int limit);
    
    @Query(value = "SELECT * FROM document_chunks WHERE embedding::vector <-> CAST(?1 AS vector) < ?2 ORDER BY embedding::vector <-> CAST(?1 AS vector) LIMIT ?3", nativeQuery = true)
    List<DocumentChunk> findSimilarChunksWithThreshold(String embedding, double threshold, int limit);
    
    @Query(value = "SELECT * FROM document_chunks ORDER BY embedding::vector <=> CAST(?1 AS vector) LIMIT ?2", nativeQuery = true)
    List<DocumentChunk> findSimilarChunksCosine(String embedding, int limit);
    
    List<DocumentChunk> findByDocumentNameOrderByChunkIndex(String documentName);
    
    void deleteByDocumentName(String documentName);
}
