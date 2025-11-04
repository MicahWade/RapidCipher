#!/bin/bash
# This script is executed by the browser.
# It MUST point to your compiled .jar file.

# --- PLEASE EDIT THIS PATH ---
JAR_PATH="/path/to/your/RapidCipher-0.0.1-SNAPSHOT.jar"
# ---------------------------

# Use a log file for debugging
LOG_FILE="/tmp/rapidcipher-bridge.log"
echo "Bridge host started at $(date)" >> $LOG_FILE
echo "Running: java -jar $JAR_PATH --bridge" >> $LOG_FILE

# Execute the Java application in bridge mode
java -jar "$JAR_PATH" --bridge 2>> $LOG_FILE
