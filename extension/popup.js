// --- Views ---
const mainView = document.getElementById('main-view');
const settingsView = document.getElementById('settings-view');

const lockedViewBridge = document.getElementById('locked-view-bridge');
const lockedViewDirect = document.getElementById('locked-view-direct');
const unlockedView = document.getElementById('unlocked-view');

const statusMessage = document.getElementById('status-message');
const loginsList = document.getElementById('logins-list');

// --- Buttons & Inputs ---
const settingsButton = document.getElementById('settings-button');
const backButton = document.getElementById('back-button');
const unlockBridgeButton = document.getElementById('unlock-bridge-button');
const unlockDirectButton = document.getElementById('unlock-direct-button');
const directPasswordField = document.getElementById('direct-password');
const searchBar = document.getElementById('search-bar');
const modeToggle = document.getElementById('mode-toggle');
const modeDescription = document.getElementById('mode-description');

// --- State ---
let currentMode = 'bridge';
let allLogins = [];
const MODE_DESCRIPTIONS = {
    bridge: "Connects to the running desktop app. Unlocking is handled by the app itself. (Recommended)",
    direct: "Sends your password from the extension to the app for a one-time unlock. (Less Secure)"
};

// --- Communication with Background Script ---

/**
 * Send a message to the background script
 */
function sendMessageToBackground(message) {
    return chrome.runtime.sendMessage(message);
}

/**
 * Handle responses and events from the background script
 */
chrome.runtime.onMessage.addListener((message) => {
    if (message.type === 'nativeResponse') {
        handleNativeResponse(message.data);
    }
});

/**
 * Process a response from the native host (via background script)
 */
function handleNativeResponse(response) {
    console.log("Native -> Popup:", response);
    unlockDirectButton.disabled = false;
    unlockDirectButton.textContent = "Unlock";
    unlockBridgeButton.disabled = false;
    unlockBridgeButton.textContent = "Unlock with Desktop App";

    if (response.status === "error") {
        showStatus(response.message, 'error');
        return;
    }

    if (response.status === "success") {
        showStatus(""); // Clear errors
        if (response.logins) {
            allLogins = response.logins;
            renderLogins(allLogins);
            showView('unlocked');
        }
    }

    if (response.status === "unlocked") {
        // Bridge unlock was successful, now fetch logins
        sendMessageToBackground({
            type: "nativeRequest",
            payload: { command: "getLogins" }
        });
    }

    if (response.status === "locked") {
        showView('locked');
    }
}


// --- View Management ---

function showView(viewName) {
    // Hide all main-view children
    [lockedViewBridge, lockedViewDirect, unlockedView].forEach(v => v.style.display = 'none');

    // Hide top-level views
    [mainView, settingsView].forEach(v => v.style.display = 'none');

    if (viewName === 'settings') {
        settingsView.style.display = 'flex';
    } else {
        mainView.style.display = 'flex';
        if (viewName === 'locked') {
            if (currentMode === 'bridge') {
                lockedViewBridge.style.display = 'block';
            } else {
                lockedViewDirect.style.display = 'block';
            }
        } else if (viewName === 'unlocked') {
            unlockedView.style.display = 'block';
        }
    }
}

function showStatus(message, type = 'error') {
    statusMessage.textContent = message;
    statusMessage.className = `status-message ${type}`;
}

// --- Logic & Rendering ---

function renderLogins(logins) {
    loginsList.innerHTML = ""; // Clear list
    if (logins.length === 0) {
        loginsList.innerHTML = "<p>No logins found.</p>";
        return;
    }

    for (const login of logins) {
        const item = document.createElement('div');
        item.className = 'login-item';
        item.innerHTML = `
        <div class="login-item-header">
        <span class="login-item-name">${escapeHTML(login.name)}</span>
        <div class="login-item-actions">
        <button class="copy-button" data-type="username" data-value="${escapeHTML(login.username)}" title="Copy Username">
        ${ICONS.user}
        </button>
        <button class="copy-button" data-type="password" data-value="${escapeHTML(login.password)}" title="Copy Password">
        ${ICONS.lock}
        </button>
        </div>
        </div>
        <div class="login-item-username">${escapeHTML(login.username)}</div>
        `;
        loginsList.appendChild(item);
    }

    // Add event listeners to new copy buttons
    loginsList.querySelectorAll('.copy-button').forEach(button => {
        button.addEventListener('click', onCopyClick);
    });
}

function onCopyClick(event) {
    const button = event.currentTarget;
    const value = button.dataset.value;
    const type = button.dataset.type;

    navigator.clipboard.writeText(value).then(() => {
        showStatus(`Copied ${type} to clipboard!`, 'success');
        setTimeout(() => showStatus(""), 2000);
    }).catch(err => {
        showStatus(`Failed to copy: ${err}`, 'error');
    });
}

function escapeHTML(str) {
    if (!str) return "";
    return str.replace(/[&<>"']/g, function(m) {
        return {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        }[m];
    });
}

// --- Event Handlers ---

settingsButton.addEventListener('click', () => {
    showView('settings');
});

backButton.addEventListener('click', () => {
    // Re-check state when going back
    initializePopup();
});

modeToggle.addEventListener('change', (e) => {
    currentMode = e.target.value;
    chrome.storage.local.set({ mode: currentMode });
    modeDescription.textContent = MODE_DESCRIPTIONS[currentMode];
});

searchBar.addEventListener('input', (e) => {
    const query = e.target.value.toLowerCase();
    const filteredLogins = allLogins.filter(login =>
    login.name.toLowerCase().includes(query) ||
    login.username.toLowerCase().includes(query) ||
    login.url.toLowerCase().includes(query)
    );
    renderLogins(filteredLogins);
});

// Bridge Unlock
unlockBridgeButton.addEventListener('click', () => {
    showStatus("Waiting for desktop app to unlock...", 'success');
    unlockBridgeButton.disabled = true;
    unlockBridgeButton.textContent = "Waiting...";
    sendMessageToBackground({
        type: "nativeRequest",
        payload: { command: "requestUnlock" }
    });
});

// Direct Unlock
unlockDirectButton.addEventListener('click', () => {
    const password = directPasswordField.value;
    if (!password) {
        showStatus("Please enter your Master Password.", 'error');
        return;
    }
    showStatus("Unlocking...", 'success');
    unlockDirectButton.disabled = true;
    unlockDirectButton.textContent = "Unlocking...";

    sendMessageToBackground({
        type: "nativeRequest",
        payload: {
            command: "unlockAndGetLoginsDirectly",
            password: password
        }
    });
});

// --- Initialization ---

async function initializePopup() {
    // 1. Get saved mode
    const settings = await chrome.storage.local.get('mode');
    currentMode = settings.mode || 'bridge';
    modeToggle.value = currentMode;
    modeDescription.textContent = MODE_DESCRIPTIONS[currentMode];

    // 2. Get state from background script
    const state = await sendMessageToBackground({ type: "getPopupState" });

    if (!state) {
        showStatus("Error communicating with background script.", 'error');
        return;
    }

    if (!state.isHostConnected) {
        showStatus(state.hostError, 'error');
        showView('locked'); // Show locked view, buttons will fail
        return;
    }

    if (state.isUnlocked) {
        allLogins = state.logins;
        renderLogins(allLogins);
        showView('unlocked');
    } else {
        showView('locked');
    }
}

// --- Icons (to avoid extra files) ---
const ICONS = {
    user: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M11 4C11 3.44772 11.4477 3 12 3C12.5523 3 13 3.44772 13 4V4.9934C14.725 5.25055 16.2163 6.13088 17.3069 7.42617C17.6583 7.82828 18 8.41492 18 9V17H20V9C20 7.91308 19.5398 6.94689 18.7997 6.20688C17.7013 5.10842 16.1422 4.29653 14.3333 4.05362V4C14.3333 3.44772 13.8856 3 13.3333 3H12C11.4477 3 11 3.44772 11 4V4.05362C9.19112 4.29653 7.63201 5.10842 6.53361 6.20688C5.7936 6.94689 5.3333 7.91308 5.3333 9V17H7.3333V9C7.3333 8.41492 7.67507 7.82828 8.02641 7.42617C9.11709 6.13088 10.6083 5.25055 12.3333 4.9934V4H13H12Z M4 15V19C4 20.1046 4.89543 21 6 21H18C19.1046 21 20 20.1046 20 19V15H18V19H6V15H4Z"></path></svg>`,
    lock: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M7 6C7 4.34315 8.34315 3 10 3H14C15.6569 3 17 4.34315 17 6V7H19C19.5523 7 20 7.44772 20 8V20C20 20.5523 19.5523 21 19 21H5C4.44772 21 4 20.5523 4 20V8C4 7.44772 4.44772 7 5 7H7V6ZM15 7V6C15 5.44772 14.5523 5 14 5H10C9.44772 5 9 5.44772 9 6V7H15ZM6 9V19H18V9H6Z"></path></svg>`
};

// Run initialization
initializePopup();
