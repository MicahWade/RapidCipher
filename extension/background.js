// --- Native Host ---
const NATIVE_HOST_NAME = "com.rapidcipher.bridge";
let port = null;

// --- State ---
let hostState = {
    isHostConnected: false,
    isUnlocked: false,
    logins: [],
    hostError: null
};

// --- Connection ---

function connect() {
    console.log("Connecting to native host:", NATIVE_HOST_NAME);
    try {
        port = chrome.runtime.connectNative(NATIVE_HOST_NAME);
        port.onMessage.addListener(onNativeMessage);
        port.onDisconnect.addListener(onDisconnected);

        hostState.isHostConnected = true;
        hostState.hostError = null;
        console.log("Connected to NativeRelay.");

        // Check status on connect
        sendMessageToHost({ command: "getStatus" });
    } catch (e) {
        console.error("Failed to connect:", e.message);
        hostState.isHostConnected = false;
        hostState.hostError = "Failed to start native host. Is RapidCipher installed and the native host manifest configured correctly?";
    }
}

function onDisconnected() {
    console.warn("Disconnected from native host.");
    port = null;
    hostState = {
        isHostConnected: false,
        isUnlocked: false,
        logins: [],
        // Updated error message to be more specific
        hostError: "Connection failed. Is RapidCipher running and the Browser Bridge enabled in Settings?"
    };

    // Clear popup state
    chrome.runtime.sendMessage({ type: "nativeResponse", data: { status: "error", message: hostState.hostError }});

    // Attempt to reconnect after a delay
    setTimeout(connect, 5000);
}

function onNativeMessage(response) {
    console.log("Native -> Background:", response);

    if (response.status === "error") {
        // Don't update hostError here, as this is a command error, not a connection error
        // But if it's an auth error, we are locked
        if (response.message.includes("locked")) {
            hostState.isUnlocked = false;
        }
    }

    if (response.status === "unlocked") {
        hostState.isUnlocked = true;
    }

    if (response.status === "locked") {
        hostState.isUnlocked = false;
    }

    if (response.logins) {
        hostState.isUnlocked = true;
        hostState.logins = response.logins;
    }

    // Forward the full response to the popup
    chrome.runtime.sendMessage({ type: "nativeResponse", data: response });
}

function sendMessageToHost(msg) {
    if (port) {
        try {
            console.log("Background -> Native:", msg);
            port.postMessage(msg);
        } catch (e) {
            console.error("Failed to send message to host:", e);
            onDisconnected();
        }
    } else {
        console.error("Cannot send message, port is not connected.");
        // Try to connect again
        if (!hostState.isHostConnected) {
            connect();
        }
        // Send an error to the popup
        chrome.runtime.sendMessage({
            type: "nativeResponse",
            data: { status: "error", message: hostState.hostError || "Native host is not connected." }
        });
    }
}

// --- Message Listener for Popup ---

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    // Make sure it's a popup request
    if (sender.tab) {
        return; // Not from our extension
    }

    const { type, payload } = message;

    if (type === "getPopupState") {
        // Popup is asking for the current state
        sendResponse(hostState);

        // If we're not connected, try to connect now
        if (!hostState.isHostConnected) {
            connect();
        } else {
            // If we are connected, refresh the status just in case
            // This catches cases where the app was locked *after* connection
            sendMessageToHost({ command: "getStatus" });
        }
    }
    else if (type === "nativeRequest") {
        // Popup is asking us to send a message to the host
        sendMessageToHost(payload);

        // If it was a direct unlock, clear logins on success
        if (payload.command === 'unlockAndGetLoginsDirectly' && hostState.isUnlocked) {
            // The response will be handled by onNativeMessage
        }
        // If it was a bridge unlock, clear logins and update state
        else if (payload.command === 'requestUnlock') {
            hostState.isUnlocked = false;
            hostState.logins = [];
        }
    }

    // Return true to indicate we will respond asynchronously
    return true;
});

// --- Initial Connection ---
connect();

