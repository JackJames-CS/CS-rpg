@echo off
chcp 65001 >nul
javac -encoding UTF-8 *.java
if errorlevel 1 (
  echo Compile failed.
  pause
  exit /b 1
)
java -Dfile.encoding=UTF-8 RPG %*
