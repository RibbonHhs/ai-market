@REM Maven Wrapper start script (Windows)
@echo off
setlocal
set DIR=%~dp0
set WRAPPER_JAR=%DIR%.mvn\wrapper\maven-wrapper.jar
"%JAVA_HOME%\bin\java.exe" %MAVEN_OPTS% -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
endlocal
