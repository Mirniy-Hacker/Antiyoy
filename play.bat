@echo off
rem Launcher for the desktop build. Double-click to play.
rem
rem ASCII only on purpose: cmd.exe reads a batch file in the console code
rem page, so Cyrillic comments break parsing before the first command runs.
rem Everything else in the project keeps Russian comments.
rem
rem Gradle needs JAVA_HOME and it is usually not set on this machine,
rem so gradlew fails with "JAVA_HOME is not set" before building anything.
rem
rem Arguments are passed to the game:
rem   play.bat --selftest
rem   play.bat --sim 100 --seed 1
rem   play.bat --help

setlocal

if not defined JAVA_HOME (
  for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%d"
)

if not defined JAVA_HOME (
  echo JDK 17 not found. Install Temurin 17 or set JAVA_HOME.
  pause
  exit /b 1
)

echo JDK: %JAVA_HOME%

rem The trailing backslash in %~dp0 would escape the closing quote,
rem so a dot is appended to keep the path a valid argument.
cd /d "%~dp0."

if "%~1"=="" (
  call "%~dp0gradlew.bat" lwjgl3:run
) else (
  call "%~dp0gradlew.bat" lwjgl3:run -Pargs="%*"
)

if errorlevel 1 pause
