package easv.dal;

import easv.be.User;
import easv.bll.PasswordHasher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for reading and writing stored accounts.
 * The storage format is intentionally simple so it is easy to study.
 *
 * Each line in the file looks like this:
 * username;passwordHash;role;active
 */
public class UserDAO {

    private final Path usersFilePath;

    public UserDAO() {
        this(Path.of("data", "users.txt"));
    }

    public UserDAO(Path usersFilePath) {
        this.usersFilePath = usersFilePath;
    }

    public User findByUsername(String username) throws IOException {
        for (User user : getAllUsers()) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }

        return null;
    }

    public List<User> getAllUsers() throws IOException {
        ensureStorageExists();

        List<String> lines = Files.readAllLines(usersFilePath, StandardCharsets.UTF_8);
        List<User> users = new ArrayList<>();

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Empty lines are ignored so the file stays easy to edit by hand.
            if (trimmedLine.isEmpty()) {
                continue;
            }

            String[] parts = trimmedLine.split(";");

            // A malformed line is skipped instead of crashing the whole app.
            if (parts.length != 4) {
                continue;
            }

            String username = parts[0].trim();
            String passwordHash = parts[1].trim();
            String role = parts[2].trim();
            boolean active = Boolean.parseBoolean(parts[3].trim());

            users.add(new User(username, passwordHash, role, active));
        }

        return users;
    }

    public void saveUser(User user) throws IOException {
        List<User> users = getAllUsers();
        users.add(user);
        writeUsers(users);
    }

    public User updatePasswordHash(String username, String newPasswordHash) throws IOException {
        List<User> users = getAllUsers();
        List<User> updatedUsers = new ArrayList<>();
        User updatedUser = null;

        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                updatedUser = new User(user.getUsername(), newPasswordHash, user.getRole(), user.isActive());
                updatedUsers.add(updatedUser);
            } else {
                updatedUsers.add(user);
            }
        }

        writeUsers(updatedUsers);
        return updatedUser;
    }

    private void ensureStorageExists() throws IOException {
        Path parentFolder = usersFilePath.getParent();
        if (parentFolder != null) {
            Files.createDirectories(parentFolder);
        }

        if (Files.notExists(usersFilePath)) {
            saveDefaultUsers();
        }
    }

    private void saveDefaultUsers() throws IOException {
        List<String> defaultUsers = List.of(
                buildUserLine("admin", "admin123", "ADMIN", true),
                buildUserLine("scanner", "user123", "USER", true),
                buildUserLine("inactive", "inactive123", "USER", false)
        );

        Files.write(usersFilePath, defaultUsers, StandardCharsets.UTF_8);
    }

    private void writeUsers(List<User> users) throws IOException {
        List<String> lines = new ArrayList<>();

        for (User user : users) {
            lines.add(user.getUsername() + ";" + user.getPasswordHash() + ";" + user.getRole() + ";" + user.isActive());
        }

        Files.write(usersFilePath, lines, StandardCharsets.UTF_8);
    }

    private String buildUserLine(String username, String plainTextPassword, String role, boolean active) {
        String passwordHash = PasswordHasher.hash(plainTextPassword);
        return username + ";" + passwordHash + ";" + role + ";" + active;
    }
}
