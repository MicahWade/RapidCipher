@echo off
REM Installer script for Windows
REM WARNING: Edit rapidcipher_native_host.json before running this.

set HOST_NAME=com.rapidcipher.bridge
set MANIFEST_FILE_NAME=%HOST_NAME%.json
set MANIFEST_PATH=%~dp0rapidcipher_native_host.json

REM --- Install for Chrome and Firefox (User) ---
REM Both Chrome and Firefox on Windows can read from the same registry location.

set REG_KEY="HKEY_CURRENT_USER\SOFTWARE\Google\Chrome\NativeMessagingHosts\%HOST_NAME%"
REG ADD %REG_KEY% /ve /t REG_SZ /d "%MANIFEST_PATH%" /f

set REG_KEY_FF="HKEY_CURRENT_USER\SOFTWARE\Mozilla\NativeMessagingHosts\%HOST_NAME%"
REG ADD %REG_KEY_FF% /ve /t REG_SZ /d "%MANIFEST_PATH%" /f

echo RapidCipher host installed for Chrome and Firefox.
echo Remember to edit the 'allowed_origins' in rapidcipher_native_host.json!
pause
