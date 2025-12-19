#!/bin/bash

echo "🚀 Starting RAG Chatbot Application"
echo "=================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if Ollama is installed
if ! command -v ollama &> /dev/null; then
    echo "❌ Ollama is not installed. Please install Ollama from https://ollama.ai/download"
    exit 1
fi

echo "📦 Starting PostgreSQL with pgvector..."
docker-compose up -d db

echo "⏳ Waiting for database to be ready..."
sleep 10

echo "🤖 Checking LLaMA model..."
if ! ollama list | grep -q "llama3.2:3b"; then
    echo "📥 Downloading LLaMA 3.2:3B model..."
    ollama pull llama3.2:3b
fi

echo "🔨 Building application..."
mvn clean package -DskipTests

echo "🚀 Starting RAG Chatbot..."
java -jar target/AI-chatbot-0.0.1-SNAPSHOT.jar

echo "✅ Application started! Visit http://localhost:8080"
echo "📚 API Documentation:"
echo "   - Upload: POST http://localhost:8080/api/upload"
echo "   - Chat: POST http://localhost:8080/api/chat"
echo "   - Health: GET http://localhost:8080/api/health"


