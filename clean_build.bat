@echo off
echo Cleaning build directories...
rmdir /s /q .gradle 2>nul
rmdir /s /q app\build 2>nul
rmdir /s /q build 2>nul
echo Clean complete!
echo.
echo Please rebuild the project in Android Studio:
echo 1. Build -^> Clean Project
echo 2. Build -^> Rebuild Project
pause
