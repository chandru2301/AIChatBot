# RAG-based Chat System

A Spring Boot application that implements a Retrieval-Augmented Generation (RAG) chat system using local LLaMA models, document parsing with LlamaParse, and vector storage with PostgreSQL and pgvector.

## Features

- **Document Upload & Processing**: Support for PDF, TXT, and DOCX files
- **Document Parsing**: Integration with LlamaParse API for content extraction
- **Text Chunking**: Intelligent text splitting with configurable chunk sizes
- **Embedding Generation**: Using BGE-small-en-v1.5 model for semantic embeddings
- **Vector Storage**: PostgreSQL with pgvector extension for similarity search
- **RAG Chat**: Question answering using retrieved context and LLaMA 3.2:3B
- **Chat History**: Persistent storage of conversations
- **Structured Logging**: Comprehensive logging with logback

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   File Upload   │───▶│  LlamaParse API │───▶│  Text Chunking  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                           │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Chat History  │◀───│   LLaMA Model   │◀───│  RAG Prompt     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         ▲                       ▲                       │
         │                       │                       │
         │              ┌─────────────────┐              │
         └──────────────│  Vector Search  │◀─────────────┘
                        └─────────────────┘
                                ▲
                                │
                        ┌─────────────────┐
                        │  PostgreSQL +   │
                        │    pgvector     │
                        └─────────────────┘
```

## Prerequisites

- Java 17+
- Maven 3.6+
- Docker and Docker Compose
- Ollama (for running LLaMA models locally)

## Quick Start

### 1. Start the Database

```bash
# Start PostgreSQL with pgvector
docker-compose up -d db

# Verify the database is running
docker-compose ps
```

### 2. Install and Run LLaMA Model

```bash
# Install Ollama (if not already installed)
# Visit: https://ollama.ai/download

# Pull and run LLaMA 3.2:3B model
ollama pull llama3.2:3b
ollama run llama3.2:3b
```

### 3. Build and Run the Application

```bash
# Build the application
mvn clean package

# Run the application
java -jar target/AI-chatbot-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
# LlamaParse API Configuration
llamaparse:
  api-key: your-key
  base-url: https://api.llamaindex.ai/api/parsing/upload

# LLaMA Model Configuration
llama:
  model-url: http://localhost:11434/api/generate
  model-name: llama3.2:3b
  max-tokens: 2048
  temperature: 0.7

# Embedding Configuration
embedding:
  model-name: BAAI/bge-small-en-v1.5
  chunk-size: 500
  chunk-overlap: 50
  vector-dimension: 384

# RAG Configuration
rag:
  top-k-chunks: 5
  similarity-threshold: 0.7
```

## API Endpoints

### File Upload

**POST** `/api/upload`

Upload and process a document for RAG.

```bash
curl -X POST http://localhost:8080/api/upload \
  -F "file=@document.pdf"
```

**Response:**
```json
{
  "message": "Document processed successfully",
  "fileName": "document.pdf",
  "chunksCreated": 15,
  "timestamp": "2024-01-15T10:30:00",
  "success": true
}
```

### Chat

**POST** `/api/chat`

Ask a question and get a RAG-based answer.

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What is object-oriented programming?"}'
```

**Response:**
```json
{
  "answer": "Object-oriented programming (OOP) is a programming paradigm...",
  "retrievedChunks": [
    "Object-oriented programming is a programming paradigm...",
    "In OOP, objects are instances of classes..."
  ],
  "timestamp": "2024-01-15T10:30:00",
  "chatId": 123
}
```

### Chat History

**GET** `/api/chat/history`

Retrieve recent chat history.

```bash
curl http://localhost:8080/api/chat/history
```

### Health Check

**GET** `/api/health`

Check if the application is running.

```bash
curl http://localhost:8080/api/health
```

## Database Schema

### Document Chunks Table

```sql
CREATE TABLE document_chunks (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    embedding VECTOR(384) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    chunk_index INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Chat History Table

```sql
CREATE TABLE chat_history (
    id SERIAL PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE chat_history_retrieved_chunks (
    chat_history_id BIGINT REFERENCES chat_history(id) ON DELETE CASCADE,
    chunk_content TEXT NOT NULL
);
```

## Development

### Project Structure

```
src/main/java/com/ai/chatbot/
├── config/           # Configuration classes
├── controller/       # REST controllers
├── dto/             # Data transfer objects
├── entity/          # JPA entities
├── repository/      # Data access layer
└── service/         # Business logic
    ├── EmbeddingService.java
    ├── LlamaParseService.java
    ├── LlamaService.java
    ├── RagService.java
    └── VectorDbService.java
```

### Running Tests

```bash
# Run all tests
mvn test

# Run with TestContainers (requires Docker)
mvn test -Dspring.profiles.active=test
```

### Logging

The application uses structured logging with logback. Logs are written to:

- Console (JSON format)
- `logs/rag-chatbot.log` (main application logs)
- `logs/rag-chatbot-error.log` (error logs only)

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Ensure PostgreSQL container is running: `docker-compose ps`
   - Check database logs: `docker-compose logs db`

2. **LLaMA Model Not Responding**
   - Verify Ollama is running: `ollama list`
   - Check if model is loaded: `ollama run llama3.2:3b`

3. **Embedding Model Loading**
   - First run may take time to download the BGE model
   - Check internet connection for HuggingFace model downloads

4. **Memory Issues**
   - Increase JVM heap size: `java -Xmx4g -jar target/AI-chatbot-0.0.1-SNAPSHOT.jar`

### Performance Tuning

1. **Vector Search Performance**
   - Adjust `ivfflat` index lists parameter in `init.sql`
   - Consider using `hnsw` index for better performance

2. **Chunk Size Optimization**
   - Adjust `chunk-size` and `chunk-overlap` in configuration
   - Monitor retrieval quality vs. performance

3. **LLaMA Model Parameters**
   - Adjust `temperature` and `max-tokens` for different use cases
   - Consider using different model variants

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review the logs for detailed error information


