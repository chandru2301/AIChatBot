@echo off
echo 🚀 Starting RAG Chatbot Application
echo ==================================

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker is not running. Please start Docker first.
    pause
    exit /b 1
)

REM Check if Ollama is installed
ollama --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Ollama is not installed. Please install Ollama from https://ollama.ai/download
    pause
    exit /b 1
)

echo 📦 Starting PostgreSQL with pgvector...
docker-compose up -d db

echo ⏳ Waiting for database to be ready...
timeout /t 10 /nobreak >nul

echo 🤖 Checking LLaMA model...
ollama list | findstr "llama3.2:3b" >nul
if %errorlevel% neq 0 (
    echo 📥 Downloading LLaMA 3.2:3B model...
    ollama pull llama3.2:3b
)

echo 🔨 Building application...
call mvn clean package -DskipTests

echo 🚀 Starting RAG Chatbot...
java -jar target/AI-chatbot-0.0.1-SNAPSHOT.jar

echo ✅ Application started! Visit http://localhost:8080
echo 📚 API Documentation:
echo    - Upload: POST http://localhost:8080/api/upload
echo    - Chat: POST http://localhost:8080/api/chat
echo    - Health: GET http://localhost:8080/api/health

pause


