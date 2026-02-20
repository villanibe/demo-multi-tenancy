@ECHO OFF
SETLOCAL

SET BASEDIR=%~dp0

"%BASEDIR%\.mvn\wrapper\apache-maven-3.9.9\bin\mvn.cmd" %*
