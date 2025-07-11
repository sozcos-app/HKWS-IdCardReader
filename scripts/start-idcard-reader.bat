@echo off
echo current path: "%~dp0"
powershell -WindowStyle Hidden -ExecutionPolicy Bypass -File "%~dp0run-idcard-reader.ps1"
pause