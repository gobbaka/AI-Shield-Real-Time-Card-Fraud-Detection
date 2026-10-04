@echo off
TITLE AI Shield - Automated Launcher
COLOR 0B

echo ===============================================================================
echo     ___     ____        ____    __      _          __        __ 
echo    /   \   l    j      /    T  l  T    l j        /  ]      /  T
echo   / /^\ \   l  T      Y   __j  l  l    l l       /  /      /  / 
echo  / /---\ \  l  l  __  l  T  l  l  _    l l  __  /  /   __ /  /  
echo / /     \ \ l  l l  j l  l_ j  l  l_   l l l  j/   \_ l  j  /   
echo \/       \/ l__j l__j \____j  l____j   l_jl__j \_____jl__j_/    
echo ===============================================================================
echo   AI Shield - Real-Time Card Fraud Detection & Step-Up Verification
echo ===============================================================================
echo.

echo [1/3] Checking environment dependencies...
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in PATH! Please install JDK 17+.
    pause
    exit /b 1
)

where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Node.js is not installed or not in PATH! Please install Node.js 18+.
    pause
    exit /b 1
)

echo [OK] Java and Node.js detected successfully.
echo.

echo [2/3] Launching Spring Boot Backend (Port 8080)...
start "AI Shield Backend [Spring Boot : 8080]" cmd /k "cd /d %~dp0backend && mvnw.cmd compile spring-boot:run"

echo [3/3] Launching Angular Frontend (Port 4200)...
start "AI Shield Frontend [Angular : 4200]" cmd /k "cd /d %~dp0frontend\fraud-detection-ui && npm start"

echo.
echo ===============================================================================
echo Both Backend and Frontend services are launching in separate windows!
echo.
echo Backend:   http://localhost:8080  (REST APIs & ONNX ML Pipeline)
echo Frontend:  http://localhost:4200  (Interactive AI Shield UI)
echo.
echo Demo Credentials:
echo   - Admin:      pradeep@example.com / password123 (OTP sent via Fast2SMS)
echo   - Cardholder: priya@example.com   / password123 (OTP sent via Fast2SMS)
echo.
echo Opening browser in 8 seconds...
echo ===============================================================================

timeout /t 8 >nul
start http://localhost:4200

echo.
echo AI Shield is now active! You may minimize this window.
pause
