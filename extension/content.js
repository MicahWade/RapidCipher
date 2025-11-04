chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === "autofill") {
        const login = request.login;

        // Find all password fields
        const passwordFields = Array.from(document.querySelectorAll('input[type="password"]'));
        if (passwordFields.length === 0) {
            sendResponse({ success: false, message: "No password field found." });
            return;
        }

        // For simplicity, we'll use the first visible password field
        const passField = passwordFields.find(f => f.offsetWidth > 0 || f.offsetHeight > 0) || passwordFields[0];

        // Find the username field
        // This is a common pattern: find the closest <form> and look for a text/email input
        let userField = null;
        const form = passField.closest('form');
        if (form) {
            userField = form.querySelector('input[type="text"], input[type="email"], input[type="tel"]');
        }

        // If no form, try to find an input before the password field
        if (!userField) {
            let allInputs = Array.from(document.querySelectorAll('input'));
            let passIndex = allInputs.indexOf(passField);
            for (let i = passIndex - 1; i >= 0; i--) {
                if (allInputs[i].type === 'text' || allInputs[i].type === 'email') {
                    userField = allInputs[i];
                    break;
                }
            }
        }

        if (!userField) {
            sendResponse({ success: false, message: "Could not find username field." });
            return;
        }

        // Fill the fields
        userField.value = login.username;
        passField.value = login.password;

        // Dispatch input events to notify frameworks like React/Vue
        userField.dispatchEvent(new Event('input', { bubbles: true }));
        passField.dispatchEvent(new Event('input', { bubbles: true }));

        sendResponse({ success: true });
    }
});
