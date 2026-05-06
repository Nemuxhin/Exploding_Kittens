package easv.dal;

import easv.be.User;
import easv.bll.AdminManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UserDAO {
    private final DatabaseConnection databaseConnection;

    public UserDAO() {
        this(new DatabaseConnection());
    }

    public UserDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public List<User> findAll() {
        String sql = """
                SELECT u.id,
                       u.name,
                       u.username,
                       u.email,
                       u.password_hash,
                       r.name AS role_name,
                       u.status,
                       u.is_current_user,
                       sp.name AS profile_name
                FROM users u
                JOIN roles r ON r.id = u.role_id
                LEFT JOIN user_profile_assignments upa ON upa.user_id = u.id
                LEFT JOIN scan_profiles sp ON sp.id = upa.scan_profile_id
                ORDER BY u.id, sp.name
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapUsers(resultSet);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch users", e);
        }
    }

    public User create(AdminManager.UserInput input) {
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users (name, username, email, password_hash, role_id, status, is_current_user)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, clean(input.getName()));
                    statement.setString(2, clean(input.getUsername()));
                    setNullable(statement, 3, input.getEmail());
                    setNullable(statement, 4, input.getPasswordHash());
                    statement.setInt(5, resolveRoleId(connection, input.getRole()));
                    statement.setString(6, normalizeStatus(input.getStatus()));
                    statement.setBoolean(7, false);
                    statement.executeUpdate();

                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new DataAccessException("Creating user did not return an id.");
                        }
                        userId = generatedKeys.getInt(1);
                    }
                }

                replaceAssignments(connection, userId, input.getAssignedProfiles());
                connection.commit();
                return findById(userId);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create user " + input.getUsername(), e);
        }
    }

    public User update(int userId, AdminManager.UserInput input) {
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE users
                        SET name = ?,
                            username = ?,
                            email = ?,
                            role_id = ?,
                            status = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """)) {
                    statement.setString(1, clean(input.getName()));
                    statement.setString(2, clean(input.getUsername()));
                    setNullable(statement, 3, input.getEmail());
                    statement.setInt(4, resolveRoleId(connection, input.getRole()));
                    statement.setString(5, normalizeStatus(input.getStatus()));
                    statement.setInt(6, userId);

                    if (statement.executeUpdate() == 0) {
                        throw new IllegalArgumentException("User could not be found.");
                    }
                }

                replaceAssignments(connection, userId, input.getAssignedProfiles());
                connection.commit();
                return findById(userId);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update user " + userId, e);
        }
    }

    public void delete(int userId) {
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement deleteAssignments = connection.prepareStatement(
                        "DELETE FROM user_profile_assignments WHERE user_id = ?")) {
                    deleteAssignments.setInt(1, userId);
                    deleteAssignments.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM users WHERE id = ?")) {
                    statement.setInt(1, userId);
                    if (statement.executeUpdate() == 0) {
                        throw new IllegalArgumentException("User could not be found.");
                    }
                }

                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete user " + userId, e);
        }
    }

    public boolean usernameExists(String username, Integer excludedUserId) {
        String sql = """
                SELECT 1
                FROM users
                WHERE LOWER(username) = ?
                  AND (? IS NULL OR id <> ?)
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clean(username).toLowerCase(Locale.ROOT));
            if (excludedUserId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, excludedUserId);
                statement.setInt(3, excludedUserId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check username uniqueness", e);
        }
    }

    public User findByUsername(String username) {
        String sql = """
                SELECT u.id,
                       u.name,
                       u.username,
                       u.email,
                       u.password_hash,
                       r.name AS role_name,
                       u.status,
                       u.is_current_user,
                       sp.name AS profile_name
                FROM users u
                JOIN roles r ON r.id = u.role_id
                LEFT JOIN user_profile_assignments upa ON upa.user_id = u.id
                LEFT JOIN scan_profiles sp ON sp.id = upa.scan_profile_id
                WHERE LOWER(u.username) = ?
                ORDER BY sp.name
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clean(username).toLowerCase(Locale.ROOT));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> users = mapUsers(resultSet);
                return users.isEmpty() ? null : users.get(0);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch user by username " + username, e);
        }
    }

    public Map<Integer, Set<Integer>> getProfileAssignments() {
        Map<Integer, Set<Integer>> assignments = new HashMap<>();

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT scan_profile_id, user_id
                     FROM user_profile_assignments
                     ORDER BY scan_profile_id, user_id
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                assignments.computeIfAbsent(resultSet.getInt("scan_profile_id"), ignored -> new HashSet<>())
                        .add(resultSet.getInt("user_id"));
            }
            return assignments;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch profile assignments", e);
        }
    }

    public void replaceProfileAssignments(Map<Integer, Set<Integer>> assignments) {
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement deleteStatement = connection.prepareStatement(
                        "DELETE FROM user_profile_assignments")) {
                    deleteStatement.executeUpdate();
                }

                try (PreparedStatement insertStatement = connection.prepareStatement("""
                        INSERT INTO user_profile_assignments (user_id, scan_profile_id)
                        VALUES (?, ?)
                        """)) {
                    for (Map.Entry<Integer, Set<Integer>> entry : assignments.entrySet()) {
                        Integer profileId = entry.getKey();
                        Collection<Integer> userIds = entry.getValue() == null ? List.of() : entry.getValue();

                        for (Integer userId : userIds) {
                            if (profileId == null || userId == null) {
                                continue;
                            }
                            insertStatement.setInt(1, userId);
                            insertStatement.setInt(2, profileId);
                            insertStatement.addBatch();
                        }
                    }

                    insertStatement.executeBatch();
                }

                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save profile assignments", e);
        }
    }

    private User findById(int userId) {
        String sql = """
                SELECT u.id,
                       u.name,
                       u.username,
                       u.email,
                       u.password_hash,
                       r.name AS role_name,
                       u.status,
                       u.is_current_user,
                       sp.name AS profile_name
                FROM users u
                JOIN roles r ON r.id = u.role_id
                LEFT JOIN user_profile_assignments upa ON upa.user_id = u.id
                LEFT JOIN scan_profiles sp ON sp.id = upa.scan_profile_id
                WHERE u.id = ?
                ORDER BY sp.name
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> users = mapUsers(resultSet);
                if (users.isEmpty()) {
                    throw new IllegalArgumentException("User could not be found.");
                }
                return users.get(0);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch user " + userId, e);
        }
    }

    private List<User> mapUsers(ResultSet resultSet) throws SQLException {
        Map<Integer, UserAccumulator> users = new LinkedHashMap<>();

        while (resultSet.next()) {
            int userId = resultSet.getInt("id");
            UserAccumulator accumulator = users.get(userId);
            if (accumulator == null) {
                accumulator = new UserAccumulator(
                        userId,
                        resultSetSafeString(resultSet, "name"),
                        resultSetSafeString(resultSet, "username"),
                        resultSetSafeString(resultSet, "email"),
                        resultSetSafeString(resultSet, "password_hash"),
                        displayRole(resultSetSafeString(resultSet, "role_name")),
                        displayStatus(resultSetSafeString(resultSet, "status")),
                        resultSet.getBoolean("is_current_user")
                );
                users.put(userId, accumulator);
            }

            String profileName = resultSetSafeString(resultSet, "profile_name");
            if (!profileName.isBlank()) {
                accumulator.assignedProfiles.add(profileName);
            }
        }

        List<User> mappedUsers = new ArrayList<>();
        for (UserAccumulator accumulator : users.values()) {
            mappedUsers.add(new User(
                    accumulator.id,
                    accumulator.name,
                    accumulator.username,
                    accumulator.email,
                    accumulator.passwordHash,
                    accumulator.role,
                    accumulator.status,
                    accumulator.assignedProfiles,
                    accumulator.currentUser
            ));
        }
        return mappedUsers;
    }

    private void replaceAssignments(Connection connection, int userId, List<String> assignedProfiles) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM user_profile_assignments WHERE user_id = ?")) {
            deleteStatement.setInt(1, userId);
            deleteStatement.executeUpdate();
        }

        if (assignedProfiles == null || assignedProfiles.isEmpty()) {
            return;
        }

        try (PreparedStatement profileLookup = connection.prepareStatement("""
                SELECT id
                FROM scan_profiles
                WHERE LOWER(name) = ?
                """);
             PreparedStatement insertStatement = connection.prepareStatement("""
                     INSERT INTO user_profile_assignments (user_id, scan_profile_id)
                     VALUES (?, ?)
                     """)) {
            for (String profileName : assignedProfiles) {
                String normalizedProfileName = clean(profileName).toLowerCase(Locale.ROOT);
                if (normalizedProfileName.isBlank()) {
                    continue;
                }

                profileLookup.setString(1, normalizedProfileName);
                try (ResultSet resultSet = profileLookup.executeQuery()) {
                    if (!resultSet.next()) {
                        continue;
                    }

                    insertStatement.setInt(1, userId);
                    insertStatement.setInt(2, resultSet.getInt("id"));
                    insertStatement.addBatch();
                }
            }
            insertStatement.executeBatch();
        }
    }

    private int resolveRoleId(Connection connection, String roleName) throws SQLException {
        String normalizedRoleName = normalizeRole(roleName);

        try (PreparedStatement selectStatement = connection.prepareStatement(
                "SELECT id FROM roles WHERE UPPER(name) = ?")) {
            selectStatement.setString(1, normalizedRoleName);
            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        try (PreparedStatement insertStatement = connection.prepareStatement("""
                INSERT INTO roles (name, description)
                VALUES (?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            insertStatement.setString(1, normalizedRoleName);
            insertStatement.setString(2, normalizedRoleName + " role");
            insertStatement.executeUpdate();

            try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new DataAccessException("Creating role did not return an id.");
                }
                return generatedKeys.getInt(1);
            }
        }
    }

    private String normalizeRole(String roleName) {
        return clean(roleName).toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        String normalized = clean(status).toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "ACTIVE" : normalized;
    }

    private String displayRole(String roleName) {
        String normalized = clean(roleName).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private String displayStatus(String status) {
        String normalized = clean(status).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void setNullable(PreparedStatement statement, int parameterIndex, String value) throws SQLException {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            statement.setNull(parameterIndex, java.sql.Types.VARCHAR);
            return;
        }
        statement.setString(parameterIndex, cleaned);
    }

    private String resultSetSafeString(ResultSet resultSet, String column) {
        try {
            String value = resultSet.getString(column);
            return value == null ? "" : value;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read user result set", e);
        }
    }

    private static final class UserAccumulator {
        private final int id;
        private final String name;
        private final String username;
        private final String email;
        private final String passwordHash;
        private final String role;
        private final String status;
        private final boolean currentUser;
        private final List<String> assignedProfiles = new ArrayList<>();

        private UserAccumulator(int id, String name, String username, String email, String passwordHash,
                                String role, String status, boolean currentUser) {
            this.id = id;
            this.name = name;
            this.username = username;
            this.email = email;
            this.passwordHash = passwordHash;
            this.role = role;
            this.status = status;
            this.currentUser = currentUser;
        }
    }
}
