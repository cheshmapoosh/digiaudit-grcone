@echo off
setlocal
title GRCONE Fresh Test
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0grcone-app.test.fresh.ps1" %*
set "GRCONE_EXIT=%ERRORLEVEL%"
if not "%GRCONE_EXIT%"=="0" pause
exit /b %GRCONE_EXIT%
