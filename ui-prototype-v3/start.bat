@echo off
echo ========================================
echo   IRAgent v3 UI Prototype
echo ========================================
echo.
echo Starting local server...
echo Open http://localhost:3000 in browser
echo Press Ctrl+C to stop
echo.

cd /d "%~dp0"
npx serve . -p 3000 --no-clipboard 2>nul

if %errorlevel% neq 0 (
    echo.
    echo npx serve failed. Trying Python...
    python -m http.server 8080
)

pause
