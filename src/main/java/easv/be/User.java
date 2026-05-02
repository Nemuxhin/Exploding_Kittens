package easv.be;

import java.util.List;

public class User {
    private final int id;
    private String name;
    private String username;
    private String email;
    private String role;
    private String status;
    private List<String> assignedProfiles;
    private final boolean currentUser;

    public User(int id, String name, String username, String email, String role,
                String status, List<String> assignedProfiles, boolean currentUser) {
        this.id = id;
        this.name = clean(name);
        this.username = clean(username);
        this.email = clean(email);
        this.role = clean(role);
        this.status = clean(status);
        this.assignedProfiles = assignedProfiles == null ? List.of() : List.copyOf(assignedProfiles);
        this.currentUser = currentUser;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public List<String> getAssignedProfiles() { return assignedProfiles; }
    public boolean isCurrentUser() { return currentUser; }

    public void setName(String name) { this.name = clean(name); }
    public void setUsername(String username) { this.username = clean(username); }
    public void setEmail(String email) { this.email = clean(email); }
    public void setRole(String role) { this.role = clean(role); }
    public void setStatus(String status) { this.status = clean(status); }

    public void setAssignedProfiles(List<String> assignedProfiles) {
        this.assignedProfiles = assignedProfiles == null ? List.of() : List.copyOf(assignedProfiles);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
