@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-wrapper.ps1"
if errorlevel 1 (
  echo.
  echo Failed to download gradle-wrapper.jar.
  echo Copy gradle/wrapper/gradle-wrapper.jar from the official Fabric example-mod 1.21.1 branch.
  exit /b 1
)
echo.
echo Wrapper is ready. You can now run: gradlew.bat build
endlocal
