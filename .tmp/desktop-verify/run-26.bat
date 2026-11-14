@echo off
set "JAVA_HOME=C:\Program Files\Apache NetBeans\jdk"
set "PATH=C:\Program Files\Apache NetBeans\jdk\bin;%PATH%"
call "C:\Users\131fgh\Documents\NetBeansProjects\S_fixed\desktop\build\install\desktop\bin\desktop.bat"
> "C:\Users\131fgh\Documents\NetBeansProjects\S_fixed\.tmp\desktop-verify\desktop-26.exitcode.txt" echo %ERRORLEVEL%
