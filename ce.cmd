@echo off
setlocal
set "ROOT=%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%tools\ce.ps1" %*
exit /b %ERRORLEVEL%
