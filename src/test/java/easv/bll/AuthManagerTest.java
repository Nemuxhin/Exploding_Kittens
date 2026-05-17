package easv.bll;

import easv.be.User;
import easv.dal.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import easv.dal.DatabaseConnection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthManagerTest {
    private static final String DATABASE_PROPERTIES_FILE = "database.properties";
    private static final String DEFAULT_JDBC_URL =
            "jdbc:sqlserver://10.176.111.34:1433;databaseName=Exploding_Kittens;encrypt=true;trustServerCertificate=true";
    private static final String DEFAULT_USERNAME = "";
    private static final String DEFAULT_PASSWORD = "";
    private static final String TEST_DATABASE_NAME = "Exploding_Kittens_JUnit";
    private static final String ACTIVE_USERNAME = "authmanager_test_active";
    private static final String ACTIVE_PASSWORD = "admin123";
    private static final String INACTIVE_USERNAME = "authmanager_test_inactive";
    private static final String INACTIVE_PASSWORD = "inactive123";

    private DatabaseConnection databaseConnection;

    @BeforeEach
    void seedUsers() {
        databaseConnection = resetDatabase();
        deleteUser(ACTIVE_USERNAME);
        deleteUser(INACTIVE_USERNAME);
        insertUser("Auth Test Active", ACTIVE_USERNAME, "auth.active@example.com", ACTIVE_PASSWORD, "ADMIN",
                "ACTIVE");
        insertUser("Auth Test Inactive", INACTIVE_USERNAME, "auth.inactive@example.com", INACTIVE_PASSWORD, "USER",
                "INACTIVE");
    }

    @AfterEach
    void clearSession() {
        UserSession.clearCurrentUser();
        if (databaseConnection != null) {
            deleteUser(ACTIVE_USERNAME);
            deleteUser(INACTIVE_USERNAME);
        }
    }

    @Test
    void loginSucceedsForValidActiveUser() {
        UserDAO userDAO = new UserDAO(databaseConnection);
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login(ACTIVE_USERNAME, ACTIVE_PASSWORD);

        assertTrue(authResult.isSuccess());
        assertNotNull(authResult.getUser());
        assertEquals(ACTIVE_USERNAME, authResult.getUser().getUsername());
        assertTrue(UserSession.hasCurrentUser());
    }

    @Test
    void loginFailsForWrongPassword() {
        UserDAO userDAO = new UserDAO(databaseConnection);
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login(ACTIVE_USERNAME, "wrong-password");

        assertFalse(authResult.isSuccess());
        assertEquals("Invalid username or password.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }

    @Test
    void loginFailsForInactiveAccount() {
        UserDAO userDAO = new UserDAO(databaseConnection);
        AuthManager authManager = new AuthManager(userDAO);

        AuthResult authResult = authManager.login(INACTIVE_USERNAME, INACTIVE_PASSWORD);

        assertFalse(authResult.isSuccess());
        assertEquals("This account is inactive and cannot log in.", authResult.getMessage());
        assertFalse(UserSession.hasCurrentUser());
    }

    private void insertUser(String name, String username, String email, String password, String roleName, String status) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO dbo.users
                     (name, username, email, password_hash, role_id, status, is_current_user, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, SYSUTCDATETIME(), SYSUTCDATETIME())
                     """)) {
            statement.setString(1, name);
            statement.setString(2, username);
            statement.setString(3, email);
            statement.setString(4, PasswordHasher.hash(password));
            statement.setInt(5, findRoleId(roleName));
            statement.setString(6, status);
            statement.setBoolean(7, false);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to insert SQL test user " + username + ".", exception);
        }
    }

    private void deleteUser(String username) {
        User existingUser = new UserDAO(databaseConnection).findByUsername(username);
        if (existingUser == null) {
            return;
        }

        new UserDAO(databaseConnection).deleteUser(existingUser.getId());
    }

    private int findRoleId(String roleName) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id
                     FROM dbo.roles
                     WHERE UPPER(name) = UPPER(?)
                     """)) {
            statement.setString(1, roleName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load SQL role " + roleName + ".", exception);
        }

        throw new IllegalStateException("Role does not exist for AuthManagerTest: " + roleName);
    }

    private DatabaseConnection resetDatabase() {
        DatabaseConfig config = loadConfig();
        String sourceDatabaseName = extractDatabaseName(config.jdbcUrl());
        String masterUrl = replaceDatabaseName(config.jdbcUrl(), "master");
        String testUrl = replaceDatabaseName(config.jdbcUrl(), TEST_DATABASE_NAME);

        try (Connection connection = DriverManager.getConnection(masterUrl, config.username(), config.password());
             Statement statement = connection.createStatement()) {
            statement.execute("IF DB_ID('" + TEST_DATABASE_NAME + "') IS NULL CREATE DATABASE " + TEST_DATABASE_NAME);

            dropTableIfExists(statement, "document_pages");
            dropTableIfExists(statement, "documents");
            dropTableIfExists(statement, "case_files");
            dropTableIfExists(statement, "boxes");
            dropTableIfExists(statement, "clients");
            dropTableIfExists(statement, "audit_logs");
            dropTableIfExists(statement, "metadata_review_records");
            dropTableIfExists(statement, "metadata_template_profile_assignments");
            dropTableIfExists(statement, "metadata_fields");
            dropTableIfExists(statement, "metadata_templates");
            dropTableIfExists(statement, "user_profile_assignments");
            dropTableIfExists(statement, "users");
            dropTableIfExists(statement, "scan_profiles");
            dropTableIfExists(statement, "roles");

            copyAllRows(statement, sourceDatabaseName, "roles");
            copyAllRows(statement, sourceDatabaseName, "users");
            copyAllRows(statement, sourceDatabaseName, "user_profile_assignments");
            copyAllRows(statement, sourceDatabaseName, "scan_profiles");
            copyAllRows(statement, sourceDatabaseName, "metadata_templates");
            copyAllRows(statement, sourceDatabaseName, "metadata_fields");
            copyAllRows(statement, sourceDatabaseName, "metadata_template_profile_assignments");
            copyNoRows(statement, sourceDatabaseName, "metadata_review_records");
            copyNoRows(statement, sourceDatabaseName, "audit_logs");
            createClientsTable(statement);
            createBoxesTable(statement);
            createCaseFilesTable(statement);
            createDocumentsTable(statement);
            createDocumentPagesTable(statement);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to reset the SQL Server test database.", exception);
        }

        return new DatabaseConnection(testUrl, config.username(), config.password());
    }

    private void dropTableIfExists(Statement statement, String tableName) throws SQLException {
        statement.execute("IF OBJECT_ID('" + TEST_DATABASE_NAME + ".dbo." + tableName
                + "', 'U') IS NOT NULL DROP TABLE " + TEST_DATABASE_NAME + ".dbo." + tableName);
    }

    private void copyAllRows(Statement statement, String sourceDatabaseName, String tableName) throws SQLException {
        statement.execute("SELECT * INTO " + TEST_DATABASE_NAME + ".dbo." + tableName
                + " FROM " + sourceDatabaseName + ".dbo." + tableName);
    }

    private void copyNoRows(Statement statement, String sourceDatabaseName, String tableName) throws SQLException {
        statement.execute("SELECT TOP 0 * INTO " + TEST_DATABASE_NAME + ".dbo." + tableName
                + " FROM " + sourceDatabaseName + ".dbo." + tableName);
    }

    private void createClientsTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE Exploding_Kittens_JUnit.dbo.clients (
                    id UNIQUEIDENTIFIER PRIMARY KEY,
                    client_number NVARCHAR(255) NOT NULL UNIQUE,
                    name NVARCHAR(255) NOT NULL
                )
                """);
    }

    private void createBoxesTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE Exploding_Kittens_JUnit.dbo.boxes (
                    id UNIQUEIDENTIFIER PRIMARY KEY,
                    box_id NVARCHAR(255) NOT NULL UNIQUE,
                    description NVARCHAR(255) NOT NULL
                )
                """);
    }

    private void createCaseFilesTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE Exploding_Kittens_JUnit.dbo.case_files (
                    id UNIQUEIDENTIFIER PRIMARY KEY,
                    case_reference NVARCHAR(255) NOT NULL UNIQUE,
                    client_id UNIQUEIDENTIFIER NOT NULL,
                    box_id UNIQUEIDENTIFIER NOT NULL
                )
                """);
    }

    private void createDocumentsTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE Exploding_Kittens_JUnit.dbo.documents (
                    id UNIQUEIDENTIFIER PRIMARY KEY,
                    source_item_id NVARCHAR(255) NOT NULL UNIQUE,
                    case_file_id UNIQUEIDENTIFIER NOT NULL
                )
                """);
    }

    private void createDocumentPagesTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE Exploding_Kittens_JUnit.dbo.document_pages (
                    id UNIQUEIDENTIFIER PRIMARY KEY,
                    document_id UNIQUEIDENTIFIER NOT NULL,
                    page_number INT NOT NULL,
                    page_order INT NOT NULL,
                    page_type NVARCHAR(50) NOT NULL,
                    source_reference NVARCHAR(255) NOT NULL,
                    reference_id INT NOT NULL,
                    rotation_degrees INT NOT NULL,
                    display_content NVARCHAR(MAX) NULL,
                    deleted_at DATETIME2 NULL
                )
                """);
    }

    private DatabaseConfig loadConfig() {
        Properties properties = new Properties();
        Path propertiesPath = Path.of(DATABASE_PROPERTIES_FILE);

        if (Files.exists(propertiesPath)) {
            try (InputStream inputStream = Files.newInputStream(propertiesPath)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                throw new RuntimeException("Failed to load " + DATABASE_PROPERTIES_FILE + ".", exception);
            }
        }

        return new DatabaseConfig(
                readConfiguredValue(properties, "EXPLODING_KITTENS_DB_URL", "exploding-kittens.db.url", DEFAULT_JDBC_URL),
                readConfiguredValue(properties, "EXPLODING_KITTENS_DB_USER", "exploding-kittens.db.user", DEFAULT_USERNAME),
                readConfiguredValue(properties, "EXPLODING_KITTENS_DB_PASSWORD", "exploding-kittens.db.password",
                        DEFAULT_PASSWORD)
        );
    }

    private String readConfiguredValue(Properties properties, String envName, String propertyName, String fallback) {
        String property = System.getProperty(propertyName);
        if (property != null && !property.isBlank()) {
            return property;
        }

        String fileProperty = properties.getProperty(propertyName);
        if (fileProperty != null && !fileProperty.isBlank()) {
            return fileProperty;
        }

        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }

        return fallback;
    }

    private String extractDatabaseName(String jdbcUrl) {
        String token = "databaseName=";
        int startIndex = jdbcUrl.indexOf(token);
        if (startIndex < 0) {
            throw new IllegalStateException("The JDBC URL does not contain a databaseName parameter.");
        }

        int valueStart = startIndex + token.length();
        int valueEnd = jdbcUrl.indexOf(';', valueStart);
        if (valueEnd < 0) {
            valueEnd = jdbcUrl.length();
        }

        return jdbcUrl.substring(valueStart, valueEnd);
    }

    private String replaceDatabaseName(String jdbcUrl, String databaseName) {
        String currentDatabaseName = extractDatabaseName(jdbcUrl);
        return jdbcUrl.replace("databaseName=" + currentDatabaseName, "databaseName=" + databaseName);
    }

    private record DatabaseConfig(String jdbcUrl, String username, String password) {
    }
}
