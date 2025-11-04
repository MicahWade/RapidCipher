const contentArea = document.getElementById('content-area');
const statusBar = document.getElementById('status-bar');
const themeToggle = document.getElementById('theme-toggle');
const themeIcon = document.getElementById('theme-icon');

let currentTheme = 'light';

// --- Theme Management ---

function applyTheme(theme) {
    currentTheme = theme;
    document.documentElement.setAttribute('data-theme', theme);
    if (theme === 'dark') {
        themeIcon.className = 'material-icons';
        themeIcon.textContent = 'light_mode'; // Sunny icon
    } else {
        themeIcon.className = 'material-icons';
        themeIcon.textContent = 'dark_mode'; // Night icon
    }
    chrome.storage.local.set({ theme: theme });
}

themeToggle.addEventListener('click', () => {
    applyTheme(currentTheme === 'light' ? 'dark' : 'light');
});

// Load saved theme
chrome.storage.local.get('theme', (data) => {
    applyTheme(data.theme || 'light');
});


// --- Native Bridge Communication ---

// 1. On popup open, check the status
document.addEventListener('DOMContentLoaded', checkAppStatus);

function checkAppStatus() {
    showLoading();

    // Send message to background.js
    chrome.runtime.sendMessage({ command: "getStatus" }, (response) => {
        if (chrome.runtime.lastError) {
            showError("Could not connect to RapidCipher. Is the app running?");
            console.error(chrome.runtime.lastError.message);
            return;
        }

        if (response.status === "success") {
            if (response.data.status === "unlocked") {
                // If unlocked, get logins
                fetchLogins();
            } else {
                // If locked, show unlock button
                showLocked();
            }
        } else {
            showError(response.message);
        }
    });
}

function fetchLogins() {
    showLoading();
    chrome.runtime.sendMessage({ command: "getLogins" }, (response) => {
        if (response.status === "success") {
            renderLogins(response.data.logins);
            setStatusBar(`Vault unlocked. ${response.data.logins.length} logins found.`);
        } else {
            showError(response.message);
        }
    });
}

function requestUnlock() {
    showLoading("Waiting for unlock...");
    setStatusBar("Please use the RapidCipher app to unlock your vault.");

    chrome.runtime.sendMessage({ command: "requestUnlock" }, (response) => {
        if (response.status === "success" && response.data.status === "unlocked") {
            // Success! Now fetch logins
            fetchLogins();
        } else {
            // Failed or cancelled
            showLocked();
            setStatusBar("Unlock failed or was cancelled.");
        }
    });
}

// --- UI Rendering ---

function showLoading(message = "Connecting...") {
    contentArea.innerHTML = `
    <div class="status-area">
    <p>${message}</p>
    </div>
    `;
}

function showError(message) {
    contentArea.innerHTML = `
    <div class="status-area">
    <p style="color: var(--error-color);">${message}</p>
    </div>
    `;
    setStatusBar("Error");
}

function showLocked() {
    contentArea.innerHTML = `
    <div class="status-area">
    <p>Vault is Locked</p>
    <button id="unlock-button">Unlock with App</button>
    </div>
    `;
    document.getElementById('unlock-button').addEventListener('click', requestUnlock);
    setStatusBar("Vault is locked.");
}

function renderLogins(logins) {
    if (logins.length === 0) {
        contentArea.innerHTML = `
        <div class="status-area">
        <p>No logins found.</p>
        </div>
        `;
        return;
    }

    contentArea.innerHTML = '<ul class="login-list" id="login-list"></ul>';
    const list = document.getElementById('login-list');

    // Filter logins for the current tab's URL
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        const currentUrl = tabs[0] ? tabs[0].url : "";
        let relevantLogins = [];
        let otherLogins = [];

        if (currentUrl) {
            try {
                const currentHost = new URL(currentUrl).hostname.replace('www.', '');
                logins.forEach(login => {
                    if (login.url && login.url.includes(currentHost)) {
                        relevantLogins.push(login);
                    } else {
                        otherLogins.push(login);
                    }
                });
            } catch (e) {
                otherLogins = logins; // Fallback if URL is invalid
            }
        } else {
            otherLogins = logins;
        }

        // Add relevant logins first
        relevantLogins.forEach(login => list.appendChild(createLoginItem(login)));
        // Add a separator if both lists have items
        if (relevantLogins.length > 0 && otherLogins.length > 0) {
            const separator = document.createElement('li');
            separator.innerHTML = `<hr style="border-color: var(--dark-shadow);">`;
            list.appendChild(separator);
        }
        // Add other logins
        otherLogins.forEach(login => list.appendChild(createLoginItem(login)));
    });
}

function createLoginItem(login) {
    const item = document.createElement('li');
    item.className = 'login-item';
    item.innerHTML = `
    <div class="login-item-name">${login.name}</div>
    <div class="login-item-user">${login.username}</div>
    `;
    item.addEventListener('click', () => {
        // On click, send login to content script
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
            chrome.tabs.sendMessage(tabs[0].id, {
                action: "autofill",
                login: login
            }, (response) => {
                if (response && response.success) {
                    setStatusBar(`Filled: ${login.name}`);
                } else {
                    setStatusBar(`Error: Could not find fields to fill.`);
                }
                window.close(); // Close popup after autofill
            });
        });
    });
    return item;
}

function setStatusBar(message) {
    statusBar.textContent = message;
}
