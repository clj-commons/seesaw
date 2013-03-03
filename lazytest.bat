@echo off

setlocal enableextensions

rem leiningen version 2 assumed to be in PATH and named lein.bat (don't use " quotes even when LFN)
set lein2=lein.bat
rem java.exe assumed to be in PATH (don't use " quotes even when LFN)
set java=java
set JAVA_OPTS=-XX:MaxPermSize=512m -Xmx512m

for /f "tokens=*" %%a in (
'"%lein2%" classpath'
) do (
set leinCP=%%a
)

set cmd="%java%" %JAVA_OPTS% -classpath "%leinCP%" lazytest.main test
echo %cmd%
%cmd%
endlocal 
rem pause only when errors?
rem if errorlevel 1 pause
rem or always pause
pause
