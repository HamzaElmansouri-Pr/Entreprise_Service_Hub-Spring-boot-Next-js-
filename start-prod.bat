@echo off
title Enterprise Service Hub - Production Environment

echo =======================================================
echo     Enterprise Service Hub - Production Docker Build
echo =======================================================

echo.
echo Starting all services in production mode...
echo This will build the frontend and backend Docker images (this may take a few minutes the first time).
echo.
docker compose -f docker-compose.prod.yml up --build -d

echo.
echo Production services started in background!
echo - pgAdmin (DB UI): http://localhost:5050 (admin@esh.com / admin)
echo - Backend API:     http://localhost:8080
echo - Frontend UI:     http://localhost:3000
echo.
echo To stop services, run: docker compose -f docker-compose.prod.yml down
echo To view logs, run: docker compose -f docker-compose.prod.yml logs -f
echo =======================================================
pause
