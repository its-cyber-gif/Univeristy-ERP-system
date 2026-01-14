package edu.univ.erp.domain;

public class User {
    private int userId;
    private String username;
    private String role;

    public User() {}
    public User(int userId, String username, String role) {
        this.userId = userId; this.username = username; this.role = role;
    }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}

