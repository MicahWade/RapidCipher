# **RapidCipher \- A Modern Desktop Password Manager**

RapidCipher is a secure, local-first password manager built with JavaFX. It provides a clean, modern interface for storing and managing your sensitive login credentials. All data is encrypted and stored locally on your machine, ensuring your information remains private.

## **Features**

* **Master Password Protection:** A single, secure master password encrypts and decrypts your entire vault.  
* **Strong Encryption:** Uses AES-256 (via AES/CBC/PKCS5Padding) and PBKDF2 for key derivation to protect your data.  
* **Local-First Database:** All credentials are stored in a local SQLite database (main.db) in your user's Documents/RapidCipher directory.  
* **Modern Neumorphic UI:** A clean, minimal interface built with JavaFX.  
* **Light/Dark Mode:** Automatically detects your system's theme (Windows, macOS, Linux) and provides a manual toggle to override it.  
* **Core Functionality:**  
  * Add new login entries (name, username, password, URL, notes).  
  * View all saved logins in a filterable list.  
  * Delete existing logins.  
  * **One-Click Copy:** Securely copy usernames and passwords to the clipboard.

## **Tech Stack**

* **Java:** Built on JDK 21\.  
* **JavaFX:** Used for the entire graphical user interface.  
* **SQLite:** For the local database storage.  
* **Maven:** For project management and dependencies.  
* **Ikonli:** For vector-based icons in the UI.

## **How to Run**

### **Prerequisites**

* Java JDK 21 or later  
* Apache Maven

### **Running from Source**

1. **Clone the repository:**  
   git clone https://github.com/MicahWade/RapidCipher  
   cd rapidcipher

2. **Compile and build the project:**  
   mvn clean package

3. **Run the application:**  
   mvn javafx:run

### **First-Time Setup**

On the first run, RapidCipher will prompt you to create a new master password. This password is used to generate the encryption key, so **do not forget it\!** It cannot be recovered.  
Subsequent runs will require you to enter this master password to unlock your vault.

## **License**

This project is licensed under the GNU Affero General Public License v3.0 \- see the [LICENSE](https://www.google.com/search?q=LICENSE) file for details.
