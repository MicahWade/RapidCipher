package gui;

public class LoginEntry {
    private final String id;
    private final String name;
    private final String username;
    private final String password;
    private final String url;
    private final String notes;

    public LoginEntry(String id, String name, String username, String password, String url, String notes) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.url = url;
        this.notes = notes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getUrl() { return url; }
    public String getNotes() { return notes; }
}