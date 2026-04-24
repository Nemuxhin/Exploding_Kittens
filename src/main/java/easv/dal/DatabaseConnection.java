package easv.dal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseConnection() {
        this(
                readConfiguredValue("PRISMSCAN_DB_URL", "prismscan.db.url",
                        "jdbc:h2:file:./data/prismscan;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"),
                readConfiguredValue("PRISMSCAN_DB_USER", "prismscan.db.user", "sa"),
                readConfiguredValue("PRISMSCAN_DB_PASSWORD", "prismscan.db.password", "")
        );
    }

    public DatabaseConnection(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        initializeSchema();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public void initializeSchema() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS clients (
                        id VARCHAR(36) PRIMARY KEY,
                        client_number VARCHAR(100) NOT NULL UNIQUE,
                        name VARCHAR(255) NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS boxes (
                        id VARCHAR(36) PRIMARY KEY,
                        box_id VARCHAR(100) NOT NULL UNIQUE,
                        description VARCHAR(255) NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS case_files (
                        id VARCHAR(36) PRIMARY KEY,
                        case_reference VARCHAR(100) NOT NULL UNIQUE,
                        client_id VARCHAR(36) NOT NULL,
                        box_id VARCHAR(36) NOT NULL,
                        FOREIGN KEY (client_id) REFERENCES clients(id),
                        FOREIGN KEY (box_id) REFERENCES boxes(id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS documents (
                        id VARCHAR(36) PRIMARY KEY,
                        source_item_id VARCHAR(100) NOT NULL UNIQUE,
                        case_file_id VARCHAR(36) NOT NULL,
                        FOREIGN KEY (case_file_id) REFERENCES case_files(id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS document_pages (
                        id VARCHAR(36) PRIMARY KEY,
                        document_id VARCHAR(36) NOT NULL,
                        page_number INT NOT NULL,
                        page_type VARCHAR(20) NOT NULL,
                        source_reference VARCHAR(255) NOT NULL,
                        UNIQUE (document_id, page_number),
                        FOREIGN KEY (document_id) REFERENCES documents(id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS scan_sessions (
                        id VARCHAR(36) PRIMARY KEY,
                        started_at TIMESTAMP NOT NULL,
                        box_id VARCHAR(36) NOT NULL,
                        FOREIGN KEY (box_id) REFERENCES boxes(id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS scan_session_documents (
                        session_id VARCHAR(36) NOT NULL,
                        document_id VARCHAR(36) NOT NULL,
                        PRIMARY KEY (session_id, document_id),
                        FOREIGN KEY (session_id) REFERENCES scan_sessions(id),
                        FOREIGN KEY (document_id) REFERENCES documents(id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS session_failures (
                        id VARCHAR(36) PRIMARY KEY,
                        session_id VARCHAR(36) NOT NULL,
                        message VARCHAR(500) NOT NULL,
                        FOREIGN KEY (session_id) REFERENCES scan_sessions(id)
                    )
                    """);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize database schema", e);
        }
    }

    private static String readConfiguredValue(String envName, String propertyName, String fallback) {
        String property = System.getProperty(propertyName);
        if (property != null && !property.isBlank()) {
            return property;
        }
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return fallback;
    }
}
