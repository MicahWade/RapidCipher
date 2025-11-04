@echo off
REM This script is executed by the browser.
REM It MUST point to your compiled .jar file.

REM --- PLEASE EDIT THIS PATH ---
set JAR_PATH="C:\path\to\your\RapidCipher-0.0.1-SNAPSHOT.jar"
REM ---------------------------

REM Use a log file for debugging
set LOG_FILE="%TEMP%\rapidcipher-bridge.log"
echo "Bridge host started at %DATE% %TIME%" >> %LOG_FILE%
echo "Running: java -jar %JAR_PATH% --bridge" >> %LOG_FILE%

REM Execute the Java application in bridge mode
java -jar %JAR_PATH% --bridge 2>> %LOG_FILE%
