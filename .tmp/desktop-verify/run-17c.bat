@echo off
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
set "PATH=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin;%PATH%"
call "C:\Users\131fgh\Documents\NetBeansProjects\S_fixed\desktop\build\install\desktop\bin\desktop.bat"
> "C:\Users\131fgh\Documents\NetBeansProjects\S_fixed\.tmp\desktop-verify\desktop-17c.exitcode.txt" echo %ERRORLEVEL%
