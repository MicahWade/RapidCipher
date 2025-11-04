#!/bin/bash
# Installer script for Linux and macOS

# WARNING: Edit rapidcipher_native_host.json before running this.

set -e

HOST_NAME="com.rapidcipher.bridge"
MANIFEST_FILE="rapidcipher_native_host.json"
HOST_PATH=$(pwd)/$MANIFEST_FILE

# --- macOS ---
if [ "$(uname)" == "Darwin" ]; then
  CHROME_DIR_USER="$HOME/Library/Application Support/Google/Chrome/NativeMessagingHosts"
  FIREFOX_DIR_USER="$HOME/Library/Application Support/Mozilla/NativeMessagingHosts"
# --- Linux ---
else
  CHROME_DIR_USER="$HOME/.config/google-chrome/NativeMessagingHosts"
  FIREFOX_DIR_USER="$HOME/.mozilla/native-messaging-hosts"
fi

# --- Install for Chrome (User) ---
mkdir -p "$CHROME_DIR_USER"
cp "$HOST_PATH" "$CHROME_DIR_USER/$HOST_NAME.json"
echo "RapidCipher host installed for Chrome."

# --- Install for Firefox (User) ---
mkdir -p "$FIREFOX_DIR_USER"
cp "$HOST_PATH" "$FIREFOX_DIR_USER/$HOST_NAME.json"
echo "RapidCipher host installed for Firefox."

echo "Installation complete. Remember to edit the 'allowed_extensions' in $HOST_NAME.json!"
