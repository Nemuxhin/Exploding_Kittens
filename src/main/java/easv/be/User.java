package easv.be;

import java.util.List;

public class User {
    private final int id;
    private String name;
    private String username;
    private String email;
    private String passwordHash;
    private String role;
    private String status;
    private List<String> assignedProfiles;
    /**
     * Persisted flag from the {@code users.is_current_user} column — marks this
     * row as the workstation owner. NOT the runtime logged-in user.
     * See {@link #isCurrentUser()}.
     */
    private final boolean currentUser;

    public User(int id, String name, String username, String email, String role,
                String status, List<String> assignedProfiles, boolean currentUser) {
        this(id, name, username, email, "", role, status, assignedProfiles, currentUser);
    }

    public User(int id, String name, String username, String email, String passwordHash, String role,
                String status, List<String> assignedProfiles, boolean currentUser) {
        this.id = id;
        this.name = clean(name);
        this.username = clean(username);
        this.email = clean(email);
        this.passwordHash = clean(passwordHash);
        this.role = clean(role);
        this.status = clean(status);
        this.assignedProfiles = assignedProfiles == null ? List.of() : List.copyOf(assignedProfiles);
        this.currentUser = currentUser;
    }

    public User(String username, String passwordHash, String role, boolean active) {
        this(0, username, username, "", passwordHash, role, active ? "Active" : "Inactive", List.of(), false);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public List<String> getAssignedProfiles() { return assignedProfiles; }
    /**
     * Returns the persisted workstation-owner flag from the database row.
     *
     * <p><strong>This is NOT the logged-in user.</strong> For "who is logged in
     * right now in this JVM", use {@link easv.bll.UserSession#getCurrentUser()}.
     *
     * <p>This flag is checked by {@code AdminManager.deactivateUser()} and
     * {@code AdminManager.deleteUser()} so an admin cannot lock the workstation
     * owner out of their own machine. It is set once when the account is
     * provisioned on a PC and never auto-updated on login.
     */
    public boolean isCurrentUser() { return currentUser; }

    public boolean isActive() {
        return "Active".equalsIgnoreCase(status);
    }

    public void setName(String name) { this.name = clean(name); }
    public void setUsername(String username) { this.username = clean(username); }
    public void setEmail(String email) { this.email = clean(email); }
    public void setPasswordHash(String passwordHash) { this.passwordHash = clean(passwordHash); }
    public void setRole(String role) { this.role = clean(role); }
    public void setStatus(String status) { this.status = clean(status); }

    public void setAssignedProfiles(List<String> assignedProfiles) {
        this.assignedProfiles = assignedProfiles == null ? List.of() : List.copyOf(assignedProfiles);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
