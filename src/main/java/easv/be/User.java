package easv.be;

/**
 * This class represents one account that can log into the system.
 * We keep only the fields that are required by your task.
 */
public class User {

    private final String username;
    private final String passwordHash;
    private final String role;
    private final boolean active;

    public User(String username, String passwordHash, String role, boolean active) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
