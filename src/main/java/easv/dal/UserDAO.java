package easv.dal;

import easv.be.User;
import easv.util.Strings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserDAO {

    private final DatabaseConnection databaseConnection;

    public UserDAO() {
        this(new DatabaseConnection());
    }

    public UserDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection == null ? new DatabaseConnection() : databaseConnection;
    }

    public User findByUsername(String username) {
        String cleanedUsername = Strings.clean(username);

        if (cleanedUsername.isBlank()) {
            return null;
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT u.id,
                            u.name,
                            u.username,
                            u.email,
                            u.password_hash,
                            COALESCE(r.name, '') AS role,
                            u.status,
                            u.is_current_user,
                            u.must_change_password
                     FROM users u
                     LEFT JOIN roles r ON r.id = u.role_id
                     WHERE LOWER(u.username) = LOWER(?)
                     """)) {
            statement.setString(1, cleanedUsername);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                int userId = resultSet.getInt("id");
                return readUser(resultSet, loadAssignedProfileNames(connection, userId));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to find user " + cleanedUsername, exception);
        }
    }

    public int nextUserId() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(MAX(id), 0) + 1 FROM users");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to calculate next user id.", exception);
        }
    }

    public User saveUser(User user) {
        return saveUser(user, List.of());
    }

    public User saveUser(User user, List<Integer> assignedProfileIds) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                User savedUser = insertUser(connection, user);
                replaceProfileAssignmentsForUser(connection, savedUser.getId(), assignedProfileIds);
                connection.commit();
                return savedUser;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to save user " + user.getUsername(), exception);
        }
    }

    public User updateUser(User user) {
        return updateUser(user, List.of());
    }

    public User updateUser(User user, List<Integer> assignedProfileIds) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                updateUserRow(connection, user);
                replaceProfileAssignmentsForUser(connection, user.getId(), assignedProfileIds);
                connection.commit();
                return new User(
                        user.getId(),
                        user.getName(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPasswordHash(),
                        displayRole(user.getRole()),
                        displayStatus(user.getStatus()),
                        user.getAssignedProfiles(),
                        user.isCurrentUser(),
                        user.isMustChangePassword()
                );
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update user " + user.getUsername(), exception);
        }
    }

    public void updatePasswordHash(int userId, String passwordHash) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive.");
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE users
                     SET password_hash = ?,
                         updated_at = CURRENT_TIMESTAMP
                     WHERE id = ?
                     """)) {
            statement.setString(1, passwordHash);
            statement.setInt(2, userId);

            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new SQLException("No user row was updated for id " + userId + ".");
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to update password hash for user " + userId, exception);
        }
    }

    public void deleteUser(int userId) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement deleteAssignments = connection.prepareStatement("""
                         DELETE FROM user_profile_assignments
                         WHERE user_id = ?
                         """);
                 PreparedStatement deleteUser = connection.prepareStatement("""
                         DELETE FROM users
                         WHERE id = ?
                         """)) {
                deleteAssignments.setInt(1, userId);
                deleteAssignments.executeUpdate();

                deleteUser.setInt(1, userId);
                deleteUser.executeUpdate();

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to delete user " + userId, exception);
        }
    }

    public List<User> getAllUsers() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT u.id,
                            u.name,
                            u.username,
                            u.email,
                            u.password_hash,
                            COALESCE(r.name, '') AS role,
                            u.status,
                            u.is_current_user,
                            u.must_change_password
                     FROM users u
                     LEFT JOIN roles r ON r.id = u.role_id
                     ORDER BY u.id
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            Map<Integer, List<String>> assignedProfilesByUser = loadAssignedProfileNamesByUser(connection);
            List<User> users = new ArrayList<>();

            while (resultSet.next()) {
                int userId = resultSet.getInt("id");
                users.add(readUser(resultSet, assignedProfilesByUser.getOrDefault(userId, List.of())));
            }

            return users;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to read users.", exception);
        }
    }

    public Map<Integer, Set<Integer>> getProfileAssignments() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT scan_profile_id, user_id
                     FROM user_profile_assignments
                     ORDER BY scan_profile_id, user_id
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            Map<Integer, Set<Integer>> assignments = new HashMap<>();

            while (resultSet.next()) {
                assignments
                        .computeIfAbsent(resultSet.getInt("scan_profile_id"), ignored -> new HashSet<>())
                        .add(resultSet.getInt("user_id"));
            }

            return assignments;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to read profile assignments.", exception);
        }
    }

    public void replaceProfileAssignments(Map<Integer, Set<Integer>> assignments) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM user_profile_assignments")) {
                deleteStatement.executeUpdate();
                insertProfileAssignments(connection, assignments);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to save profile assignments.", exception);
        }
    }

    private User insertUser(Connection connection, User user) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO users
                (name, username, email, password_hash, role_id, status, is_current_user, must_change_password, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPasswordHash());
            statement.setInt(5, findRoleId(connection, user.getRole()));
            statement.setString(6, user.getStatus());
            statement.setBoolean(7, user.isCurrentUser());
            statement.setBoolean(8, user.isMustChangePassword());
            statement.executeUpdate();

            return new User(
                    readGeneratedIntId(statement),
                    user.getName(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    displayRole(user.getRole()),
                    displayStatus(user.getStatus()),
                    user.getAssignedProfiles(),
                    user.isCurrentUser(),
                    user.isMustChangePassword()
            );
        }
    }

    private void updateUserRow(Connection connection, User user) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE users
                SET name = ?,
                    username = ?,
                    email = ?,
                    password_hash = ?,
                    role_id = ?,
                    status = ?,
                    is_current_user = ?,
                    must_change_password = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPasswordHash());
            statement.setInt(5, findRoleId(connection, user.getRole()));
            statement.setString(6, user.getStatus());
            statement.setBoolean(7, user.isCurrentUser());
            statement.setBoolean(8, user.isMustChangePassword());
            statement.setInt(9, user.getId());
            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                throw new SQLException("No user row was updated for id " + user.getId() + ".");
            }
        }
    }

    private int findRoleId(Connection connection, String roleName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id
                FROM roles
                WHERE LOWER(name) = LOWER(?)
                """)) {
            statement.setString(1, Strings.clean(roleName));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        throw new DataAccessException("Role does not exist in the database: " + Strings.clean(roleName), null);
    }

    private void replaceProfileAssignmentsForUser(Connection connection, int userId, List<Integer> profileIds)
            throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement("""
                     DELETE FROM user_profile_assignments
                     WHERE user_id = ?
                     """)) {
            deleteStatement.setInt(1, userId);
            deleteStatement.executeUpdate();
        }

        Map<Integer, Set<Integer>> assignments = new HashMap<>();

        for (Integer profileId : profileIds == null ? List.<Integer>of() : profileIds) {
            if (profileId != null) {
                assignments.computeIfAbsent(profileId, ignored -> new HashSet<>()).add(userId);
            }
        }

        insertProfileAssignments(connection, assignments);
    }

    private void insertProfileAssignments(Connection connection, Map<Integer, Set<Integer>> assignments)
            throws SQLException {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_profile_assignments
                (user_id, scan_profile_id)
                VALUES (?, ?)
                """)) {
            for (Map.Entry<Integer, Set<Integer>> assignment : assignments.entrySet()) {
                int profileId = assignment.getKey();

                for (Integer userId : assignment.getValue()) {
                    if (userId == null) {
                        continue;
                    }

                    statement.setInt(1, userId);
                    statement.setInt(2, profileId);
                    statement.addBatch();
                }
            }

            statement.executeBatch();
        }
    }

    private Map<Integer, List<String>> loadAssignedProfileNamesByUser(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                     SELECT upa.user_id, sp.name
                     FROM user_profile_assignments upa
                     JOIN scan_profiles sp ON sp.id = upa.scan_profile_id
                     ORDER BY sp.name
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            Map<Integer, List<String>> profileNamesByUser = new HashMap<>();

            while (resultSet.next()) {
                profileNamesByUser
                        .computeIfAbsent(resultSet.getInt("user_id"), ignored -> new ArrayList<>())
                        .add(resultSet.getString("name"));
            }

            return profileNamesByUser;
        }
    }

    private List<String> loadAssignedProfileNames(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                     SELECT sp.name
                     FROM user_profile_assignments upa
                     JOIN scan_profiles sp ON sp.id = upa.scan_profile_id
                     WHERE upa.user_id = ?
                     ORDER BY sp.name
                     """)) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> profileNames = new ArrayList<>();

                while (resultSet.next()) {
                    profileNames.add(resultSet.getString("name"));
                }

                return profileNames;
            }
        }
    }

    private User readUser(ResultSet resultSet, List<String> assignedProfileNames) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                displayRole(resultSet.getString("role")),
                displayStatus(resultSet.getString("status")),
                assignedProfileNames,
                resultSet.getBoolean("is_current_user"),
                resultSet.getBoolean("must_change_password")
        );
    }

    private int readGeneratedIntId(Statement statement) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        }

        throw new SQLException("Database did not return a generated user id.");
    }

    private String displayRole(String role) {
        String cleanedRole = Strings.clean(role);

        if (cleanedRole.equalsIgnoreCase("admin")) {
            return "Admin";
        }

        if (cleanedRole.equalsIgnoreCase("user")) {
            return "User";
        }

        if (cleanedRole.equalsIgnoreCase("qa")) {
            return "QA";
        }

        return titleCase(cleanedRole);
    }

    private String displayStatus(String status) {
        return titleCase(Strings.clean(status));
    }

    private String titleCase(String value) {
        if (value.isBlank()) {
            return "";
        }

        String lowerCase = value.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }

}
