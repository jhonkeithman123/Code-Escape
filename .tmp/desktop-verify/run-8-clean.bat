@echo off
set "JAVA_HOME=C:\Users\131fgh\.gradle\jdks\temurin-8-amd64-windows\jdk8u512-b09"
set "PATH=C:\Users\131fgh\.gradle\jdks\temurin-8-amd64-windows\jdk8u512-b09\bin;%PATH%"
call "C:\Users\131fgh\Documents\NetBeansProjects\S_fixed\desktop\build\install\desktop\bin\desktop.bat"
> "C:\Users\131fgh\Documents\NetBeansProjects\S_fixed\.tmp\desktop-verify\desktop-8-clean.exitcode.txt" echo %ERRORLEVEL%
