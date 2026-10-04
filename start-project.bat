@echo off
title AI Shield - Enterprise Credit Card Fraud Detection Launcher
color 0b

echo ===============================================================================
echo                AI SHIELD - REAL-TIME FRAUD DETECTION SYSTEM
echo ===============================================================================
echo.
echo [1/3] Starting Spring Boot REST Backend (Port 8080)...
start "AI Shield - Spring Boot Backend" cmd /k "cd /d %~dp0backend && mvnw.cmd spring-boot:run"

echo [2/3] Starting Angular Frontend UI (Port 4200)...
start "AI Shield - Angular Frontend" cmd /k "cd /d %~dp0frontend\fraud-detection-ui && npm start"

echo [3/3] Waiting for services to initialize...
timeout /t 10 /nobreak >nul

echo.
echo Opening AI Shield Admin Operations Center...
start http://localhost:4200/dashboard

echo Opening Interactive OpenAPI / Swagger UI Documentation...
start http://localhost:8080/swagger-ui/index.html

echo.
echo ===============================================================================
echo  SERVICES RUNNING:
echo  - Frontend Dashboard:      http://localhost:4200/dashboard
echo  - Cardholder SMS Portal:   http://localhost:4200/verify/VR-89102-X
echo  - Backend API Explorer:    http://localhost:8080/swagger-ui/index.html
echo  - H2 Database Console:     http://localhost:8080/h2-console
echo ===============================================================================
echo.
pause
