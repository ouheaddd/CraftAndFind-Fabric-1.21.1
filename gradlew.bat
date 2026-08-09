@echo off
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  echo Missing gradle\wrapper\gradle-wrapper.jar
  echo Run setup-wrapper.bat, then run gradlew.bat runClient or gradlew.bat build.
  exit /b 1
)
java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
