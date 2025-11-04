# **RapidCipher \- A Modern Desktop Password Manager**

RapidCipher is a secure, flexible password manager built with JavaFX. It provides a clean, modern interface for storing and managing your sensitive login credentials. All data is encrypted using strong AES-256 and PBKDF2, ensuring your information remains private, whether stored locally or on a remote database.

## **Features**

* **Master Password Protection:** A single, secure master password encrypts and decrypts your entire vault.  
* **Strong Encryption:** Uses AES-256 (via AES/CBC/PKCS5Padding) and PBKDF2 for key derivation to protect your data.  
* **Flexible Database Support:** Connect to a local SQLite file (default) or a remote MySQL or PostgreSQL database. See [MYSQL\_SETUP.md](https://www.google.com/search?q=MYSQL_SETUP.md) for a guide on setting up MySQL.  
* **Database Migration:** Easily migrate all your encrypted data from one database type to another via the in-app settings.  
* **Modern Neumorphic UI:** A clean, minimal interface built with JavaFX.  
* **Light/Dark Mode:** Automatically detects your system's theme (Windows, macOS, Linux) and provides a manual toggle to override it.  
* **Password Generator:** Create strong, complex passwords and memorable passphrases from within the app.  
* **Core Functionality:**  
  * Add new login entries (name, username, password, URL, notes).  
  * View all saved logins in a filterable list.  
  * Delete existing logins.  
  * **One-Click Copy:** Securely copy usernames and passwords to the clipboard.

## **Tech Stack**

* **Java:** Built on JDK 21\.  
* **JavaFX:** Used for the entire graphical user interface.  
* **Maven:** For project management and dependencies.  
* **Ikonli:** For vector-based icons in the UI.  
* **Database Drivers:**  
  * SQLite-JDBC  
  * MySQL Connector/J  
  * PostgreSQL JDBC Driver

## **How to Run**

### **Prerequisites**

* Java JDK or JRE 21 or later  
* Apache Maven (only required for building from source)

### **Running the Compiled .jar File (Recommended)**

1. Build the project one time using Maven (if you haven't already):  
   mvn clean package

2. This creates a RapidCipher-0.0.1-SNAPSHOT.jar file in the target/ directory.  
3. You can run the application by double-clicking the .jar file or from the command line:  
   java \-jar target/RapidCipher-0.0.1-SNAPSHOT.jar

### **Running from Source (For Development)**

1. **Clone the repository:**  
   git clone \[https://github.com/MicahWade/RapidCipher\](https://github.com/MicahWade/RapidCipher)  
   cd rapidcipher

2. **Compile and build the project:**  
   mvn clean package

3. **Run the application:**  
   mvn javafx:run

### **First-Time Setup**

On the first run, RapidCipher will prompt you to create a new master password. This password is used to generate the encryption key, so **do not forget it\!** It cannot be recovered.  
By default, the application will create and use a local **SQLite** database file located at \[Your Home Directory\]/Documents/RapidCipher/main.db. You can change this to a remote database from the **Settings** menu inside the application.

## **License**

This project is licensed under the GNU Affero General Public License v3.0 \- see the [LICENSE](https://www.google.com/search?q=LICENSE) file for details.
