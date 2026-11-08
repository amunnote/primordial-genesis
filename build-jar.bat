@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
set "JAVA_HOME="
set "FALLBACK="
echo Searching for JDK 17 ...
for %%D in ("%USERPROFILE%\.gradle\jdks" "%USERPROFILE%\.jdks" "%ProgramFiles%\Java" "%ProgramFiles%\Eclipse Adoptium" "%ProgramFiles%\Microsoft" "%ProgramFiles%\Zulu" "%ProgramFiles%\BellSoft" "%ProgramFiles%\Amazon Corretto" "%ProgramFiles%\Pylo" "%LOCALAPPDATA%\Programs\Eclipse Adoptium" "%USERPROFILE%\.mcreator" "%ProgramFiles(x86)%\Java") do (
  if exist "%%~D" for /f "delims=" %%J in ('dir /b /s "%%~D\javac.exe" 2^>nul') do call :check "%%J"
)
if not defined JAVA_HOME if defined FALLBACK set "JAVA_HOME=%FALLBACK%"
if not defined JAVA_HOME (
  echo.
  echo No JDK found. Install JDK 17 from https://adoptium.net/temurin/releases/?version=17 then run this file again.
  pause
  exit /b 1
)
echo Using JAVA_HOME=%JAVA_HOME%
echo Building, this can take several minutes. Output goes to build-log.txt ...
call gradlew.bat build > build-log.txt 2>&1
if errorlevel 1 (
  echo BUILD FAILED. Send build-log.txt to Claude.
  start notepad build-log.txt
  pause
  exit /b 1
)
echo BUILD OK. The jar is in build\libs
start explorer build\libs
pause
exit /b 0

:check
set "BIN=%~dp1"
set "HOME_DIR=%BIN:~0,-5%"
"%~1" -version 2>&1 | findstr /b /c:"javac 17" >nul
if not errorlevel 1 (
  if not defined JAVA_HOME set "JAVA_HOME=%HOME_DIR%"
) else (
  "%~1" -version 2>&1 | findstr /b /c:"javac 21" /c:"javac 18" /c:"javac 19" /c:"javac 20" >nul
  if not errorlevel 1 if not defined FALLBACK set "FALLBACK=%HOME_DIR%"
)
exit /b 0
