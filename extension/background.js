const NATIVE_HOST_NAME = "com.rapidcipher.bridge";
let nativePort = null;

function connectToNative() {
    console.log("Connecting to native host:", NATIVE_HOST_NAME);
    nativePort = chrome.runtime.connectNative(NATIVE_HOST_NAME);

    nativePort.onDisconnect.addListener(() => {
        console.log("Disconnected from native host.");
        if (chrome.runtime.lastError) {
            console.error("Disconnect error:", chrome.runtime.lastError.message);
        }
        nativePort = null;
    });
}

// Ensure connection is active
function getPort() {
    if (!nativePort) {
        connectToNative();
    }
    return nativePort;
}

// Listen for messages from popup.js
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    const port = getPort();
    if (!port) {
        sendResponse({ status: "error", message: "Failed to connect to native host." });
        return true; // Keep channel open for async response
    }

    // One-time listener for the response from the native app
    port.onMessage.addListener(function onNativeMessage(response) {
        console.log("Received native message:", response);
        if (response.status === "error") {
            sendResponse({ status: "error", message: response.message });
        } else {
            sendResponse({ status: "success", data: response });
        }
        // Remove this specific listener after it's been used
        port.onMessage.removeListener(onNativeMessage);
    });

    // Send the command to the native app
    console.log("Sending message to native:", message);
    port.postMessage(message);

    // Return true to indicate we will respond asynchronously
    return true;
});
