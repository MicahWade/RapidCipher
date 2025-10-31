import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Gui extends Application {
    private static Gui instance;
    private Database database;
    private ObservableList<LoginEntry> loginData;

    private TableView<LoginEntry> tableView;
    private TextField nameField, usernameField, urlField, notesField;
    private PasswordField passwordField;
    private Label statusLabel;

    private Gui() {
        try {
            database = Database.getInstance();
            loginData = FXCollections.observableArrayList();
            loadDataFromDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized Gui getInstance() {
        if (instance == null) {
            instance = new Gui();
        }
        return instance;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Password Manager");

        // TableView setup
        tableView = new TableView<>();
        TableColumn<LoginEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<LoginEntry, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        TableColumn<LoginEntry, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(new PropertyValueFactory<>("url"));
        TableColumn<LoginEntry, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        tableView.getColumns().addAll(nameCol, userCol, urlCol, notesCol);
        tableView.setItems(loginData);

        // Input form
        nameField = new TextField();
        nameField.setPromptText("Name");
        usernameField = new TextField();
        usernameField.setPromptText("Username");
        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        urlField = new TextField();
        urlField.setPromptText("URL");
        notesField = new TextField();
        notesField.setPromptText("Notes");

        Button addButton = new Button("Add Login");
        addButton.setOnAction(e -> addLogin());

        statusLabel = new Label();

        HBox form = new HBox(10);
        form.setPadding(new Insets(10));
        form.getChildren().addAll(nameField, usernameField, passwordField, urlField, notesField, addButton);

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(tableView, form, statusLabel);

        Scene scene = new Scene(root, 900, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadDataFromDatabase() {
        loginData.clear();
        try {
            ResultSet rs = database.searchLogins("");
            while (rs != null && rs.next()) {
                loginData.add(new LoginEntry(
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("url"),
                        rs.getString("notes")
                ));
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addLogin() {
        String name = nameField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String url = urlField.getText();
        String notes = notesField.getText();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Name, Username, and Password are mandatory.");
            return;
        }

        boolean success = database.createLogin(name, username, password, url, notes);
        if (success) {
            statusLabel.setText("Login added successfully.");
            loginData.add(new LoginEntry(name, username, password, url, notes));
            clearForm();
        } else {
            statusLabel.setText("Failed to add login.");
        }
    }

    private void clearForm() {
        nameField.clear();
        usernameField.clear();
        passwordField.clear();
        urlField.clear();
        notesField.clear();
    }

    // Helper class to represent login data in TableView
    public static class LoginEntry {
        private final String name;
        private final String username;
        private final String password; // Note: For secure apps, do not display or store passwords in plaintext like this
        private final String url;
        private final String notes;

        public LoginEntry(String name, String username, String password, String url, String notes) {
            this.name = name;
            this.username = username;
            this.password = password;
            this.url = url;
            this.notes = notes;
        }

        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getUrl() { return url; }
        public String getNotes() { return notes; }
    }
}
