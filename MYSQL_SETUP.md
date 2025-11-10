# **MySQL Setup Guide**

You can configure RapidCipher to use a local MySQL database instead of the default SQLite file. This is useful if you want to access your passwords from multiple computers on your local network.

### **1. Install and Start MySQL**

First, you need a MySQL server running on your machine.

* **Windows/macOS:** The easiest way is to install [suspicious link removed] or use a package like **XAMPP** or **MAMP**, which bundles MySQL, Apache, and PHP.
* **Linux (Ubuntu):** You can install it via the terminal:
    `sudo apt update`
    `sudo apt install mysql-server`
    `sudo service mysql start`

### **2. Create the Database and User**

Next, you need to log in to MySQL as the root user and create a dedicated database and user for RapidCipher.

1.  Open a terminal or command prompt and log in to MySQL:
    `mysql -u root -p`

    (Enter your MySQL root password when prompted).
2.  Run the following SQL commands. You can change `rapidcipher`, `rapid_user`, and `your_strong_password` to anything you like, but remember what you choose.
    `-- 1. Create the database`
    `CREATE DATABASE rapidcipher CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`

    `-- 2. Create a new user and set their password`
    `CREATE USER 'rapid_user'@'localhost' IDENTIFIED BY 'your_strong_password';`

    `-- 3. Give the new user full permissions on the new database`
    `GRANT ALL PRIVILEGES ON rapidcipher.* TO 'rapid_user'@'localhost';`

    `-- 4. Apply the changes`
    `FLUSH PRIVILEGES;`

    `-- 5. Exit`
    `EXIT;`

### **3. Configure RapidCipher**

Now, open the RapidCipher application and log in with your master password.

1.  Click the **Settings** icon (cog wheel) in the top bar.
2.  Change the **Database Type** dropdown to `MYSQL`.
3.  Fill in the fields with the credentials you just created:
    * **Host:** `localhost` (or `127.0.0.1`)
    * **Port:** `3306` (this is the default for MySQL)
    * **DB Name/Path:** `rapidcipher`
    * **User:** `rapid_user`
    * **Password:** `your_strong_password`
4.  If you have existing data in your SQLite database, check the **"Copy data from current DB to new DB"** box to migrate your passwords.
5.  Click **"Save Database Settings"**.
6.  **Restart the application** for the changes to take effect.

### **A Note on SSL/TLS**

The application's MySQL driver attempts to connect using SSL by default (`useSSL=true&requireSSL=true`). A standard local MySQL installation may not have SSL configured.
If you get a connection error after restarting, you may need to:

1.  Disable SSL in your MySQL server configuration.
2.  Or, more simply, modify the connection string in `src/main/java/database/MySqlDriver.java` from `?useSSL=true&requireSSL=true` to `?useSSL=false` and re-build the project (`mvn clean package`).