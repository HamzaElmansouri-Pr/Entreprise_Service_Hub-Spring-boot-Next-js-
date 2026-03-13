@echo off
title Enterprise Service Hub - Development Environment

echo =======================================================
echo     Enterprise Service Hub - Development Startup
echo =======================================================

echo.
echo [1/3] Checking if Docker is running...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    echo.
    pause
    exit /b
)

echo.
echo [1/3] Starting Database, pgAdmin, and Cache via Docker...
docker compose -f docker-compose.yml up -d

echo.
echo [2/3] Starting Backend (Spring Boot)...
:: Using start to open a new terminal window for the backend
start "Backend (Spring Boot)" cmd /k "cd enterprise-service-hub && mvnw spring-boot:run"

echo.
echo [3/3] Starting Frontend (Next.js)...
:: Using start to open a new terminal window for the frontend
start "Frontend (Next.js)" cmd /k "cd nova-frontend && npm run dev"

echo.
echo All services are starting in separate windows!
echo - pgAdmin (DB UI): http://localhost:5050 (admin@esh.com / admin)
echo - Backend API:     http://localhost:8080
echo - Frontend UI:     http://localhost:3000
echo.
echo You can close this window. To stop the databases later, run:
echo docker compose down
echo =======================================================
pause
