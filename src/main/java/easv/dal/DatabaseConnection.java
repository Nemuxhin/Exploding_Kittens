package easv.dal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseConnection {
    private static final String DEFAULT_JDBC_URL = "";
    private static final String DATABASE_PROPERTIES_FILE = "database.properties";
    private static final Pattern SQL_SERVER_DATABASE_PATTERN =
            Pattern.compile("(?i)(?:^|;)databaseName=([^;]+)");
    private static final Properties FILE_PROPERTIES = loadFileProperties();

    private static final List<String> SQL_SERVER_SCHEMA_STATEMENTS = List.of(
            """
            IF OBJECT_ID(N'dbo.clients', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.clients (
                    id VARCHAR(36) PRIMARY KEY,
                    client_number VARCHAR(100) NOT NULL UNIQUE,
                    name VARCHAR(255) NOT NULL,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.archives', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.archives (
                    id VARCHAR(36) PRIMARY KEY,
                    client_id VARCHAR(36) NOT NULL,
                    archive_code VARCHAR(100) NOT NULL UNIQUE,
                    name VARCHAR(255) NOT NULL,
                    description VARCHAR(1000) NULL,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    FOREIGN KEY (client_id) REFERENCES dbo.clients(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.boxes', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.boxes (
                    id VARCHAR(36) PRIMARY KEY,
                    archive_id VARCHAR(36) NULL,
                    box_id VARCHAR(100) NOT NULL UNIQUE,
                    description VARCHAR(255) NOT NULL,
                    location VARCHAR(255) NULL,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    FOREIGN KEY (archive_id) REFERENCES dbo.archives(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.case_files', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.case_files (
                    id VARCHAR(36) PRIMARY KEY,
                    case_reference VARCHAR(100) NOT NULL UNIQUE,
                    client_id VARCHAR(36) NOT NULL,
                    box_id VARCHAR(36) NOT NULL,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    FOREIGN KEY (client_id) REFERENCES dbo.clients(id),
                    FOREIGN KEY (box_id) REFERENCES dbo.boxes(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.documents', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.documents (
                    id VARCHAR(36) PRIMARY KEY,
                    source_item_id VARCHAR(100) NOT NULL UNIQUE,
                    case_file_id VARCHAR(36) NOT NULL,
                    title VARCHAR(255) NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'IMPORTED',
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    FOREIGN KEY (case_file_id) REFERENCES dbo.case_files(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.document_pages', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.document_pages (
                    id VARCHAR(36) PRIMARY KEY,
                    document_id VARCHAR(36) NOT NULL,
                    page_number INT NOT NULL,
                    page_type VARCHAR(20) NOT NULL,
                    source_reference VARCHAR(255) NOT NULL,
                    rotation_degrees INT NOT NULL DEFAULT 0,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    UNIQUE (document_id, page_number),
                    FOREIGN KEY (document_id) REFERENCES dbo.documents(id),
                    CHECK (page_type IN ('TIFF', 'BARCODE')),
                    CHECK (rotation_degrees IN (0, 90, 180, 270))
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.scan_sessions', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.scan_sessions (
                    id VARCHAR(36) PRIMARY KEY,
                    started_at DATETIME2 NOT NULL,
                    box_id VARCHAR(36) NOT NULL,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    FOREIGN KEY (box_id) REFERENCES dbo.boxes(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.scan_session_documents', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.scan_session_documents (
                    session_id VARCHAR(36) NOT NULL,
                    document_id VARCHAR(36) NOT NULL,
                    PRIMARY KEY (session_id, document_id),
                    FOREIGN KEY (session_id) REFERENCES dbo.scan_sessions(id),
                    FOREIGN KEY (document_id) REFERENCES dbo.documents(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.session_failures', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.session_failures (
                    id VARCHAR(36) PRIMARY KEY,
                    session_id VARCHAR(36) NOT NULL,
                    message VARCHAR(500) NOT NULL,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    FOREIGN KEY (session_id) REFERENCES dbo.scan_sessions(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.roles', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.roles (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    name VARCHAR(50) NOT NULL UNIQUE,
                    description VARCHAR(255) NULL,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.users', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.users (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    username VARCHAR(100) NOT NULL UNIQUE,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NULL,
                    role_id INT NOT NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                    is_current_user BIT NOT NULL DEFAULT 0,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    FOREIGN KEY (role_id) REFERENCES dbo.roles(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.scan_profiles', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.scan_profiles (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    code VARCHAR(100) NOT NULL UNIQUE,
                    description VARCHAR(1000) NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'Active',
                    metadata_template_name VARCHAR(255) NULL,
                    export_naming VARCHAR(255) NULL,
                    last_updated VARCHAR(255) NULL,
                    archived BIT NOT NULL DEFAULT 0,
                    barcode_splitting BIT NOT NULL DEFAULT 0,
                    barcode_detected_behavior VARCHAR(100) NULL,
                    barcode_page_behavior VARCHAR(100) NULL,
                    default_rotation VARCHAR(50) NULL,
                    brightness VARCHAR(50) NULL,
                    contrast VARCHAR(50) NULL,
                    deskew BIT NOT NULL DEFAULT 0,
                    export_format VARCHAR(50) NULL,
                    metadata_required_before_export BIT NOT NULL DEFAULT 0,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.metadata_templates', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.metadata_templates (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL UNIQUE,
                    description VARCHAR(1000) NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'Active',
                    last_updated VARCHAR(255) NULL,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.metadata_fields', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.metadata_fields (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    template_id INT NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    required BIT NOT NULL DEFAULT 0,
                    placeholder VARCHAR(255) NULL,
                    sort_order INT NOT NULL DEFAULT 0,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    UNIQUE (template_id, name),
                    FOREIGN KEY (template_id) REFERENCES dbo.metadata_templates(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.metadata_template_profile_assignments', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.metadata_template_profile_assignments (
                    metadata_template_id INT NOT NULL,
                    scan_profile_id INT NOT NULL,
                    PRIMARY KEY (metadata_template_id, scan_profile_id),
                    FOREIGN KEY (metadata_template_id) REFERENCES dbo.metadata_templates(id),
                    FOREIGN KEY (scan_profile_id) REFERENCES dbo.scan_profiles(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.user_profile_assignments', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.user_profile_assignments (
                    user_id INT NOT NULL,
                    scan_profile_id INT NOT NULL,
                    PRIMARY KEY (user_id, scan_profile_id),
                    FOREIGN KEY (user_id) REFERENCES dbo.users(id),
                    FOREIGN KEY (scan_profile_id) REFERENCES dbo.scan_profiles(id)
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.metadata_review_records', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.metadata_review_records (
                    id VARCHAR(100) PRIMARY KEY,
                    identity_value VARCHAR(255) NOT NULL,
                    client_name VARCHAR(255) NULL,
                    archive_name VARCHAR(255) NULL,
                    profile_name VARCHAR(255) NULL,
                    metadata_template_name VARCHAR(255) NULL,
                    metadata_status VARCHAR(100) NULL,
                    qa_status VARCHAR(100) NULL,
                    pages INT NOT NULL DEFAULT 0,
                    last_updated VARCHAR(255) NULL,
                    assigned_to VARCHAR(255) NULL,
                    scanned_by VARCHAR(255) NULL,
                    date_group VARCHAR(100) NULL,
                    warning BIT NOT NULL DEFAULT 0,
                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                )
            END
            """,
            """
            IF OBJECT_ID(N'dbo.audit_logs', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.audit_logs (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    timestamp DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                    type VARCHAR(100) NOT NULL,
                    actor VARCHAR(255) NOT NULL,
                    action VARCHAR(255) NOT NULL,
                    target VARCHAR(255) NOT NULL,
                    status VARCHAR(100) NOT NULL,
                    description VARCHAR(1000) NULL
                )
            END
            """
    );

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseConnection() {
        this(
                readConfiguredValue("EXPLODING_KITTENS_DB_URL", "exploding-kittens.db.url", DEFAULT_JDBC_URL),
                readConfiguredValue("EXPLODING_KITTENS_DB_USER", "exploding-kittens.db.user", ""),
                readConfiguredValue("EXPLODING_KITTENS_DB_PASSWORD", "exploding-kittens.db.password", "")
        );
    }

    public DatabaseConnection(String jdbcUrl, String username, String password) {
        this.jdbcUrl = clean(jdbcUrl);
        this.username = clean(username);
        this.password = password == null ? "" : password;
        initializeSchema();
    }

    public Connection getConnection() throws SQLException {
        if (jdbcUrl.isBlank()) {
            throw new DataAccessException("""
                    SQL Server connection URL is missing.
                    Set `exploding-kittens.db.url` in `database.properties` or `EXPLODING_KITTENS_DB_URL`.
                    """.stripIndent().trim());
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public void initializeSchema() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            validateDatabaseTarget(connection.getCatalog(), jdbcUrl);

            for (String ddl : SQL_SERVER_SCHEMA_STATEMENTS) {
                statement.executeUpdate(ddl);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize database schema", e);
        }
    }

    static void validateDatabaseTarget(String catalog, String jdbcUrl) {
        String effectiveDatabase = firstNonBlank(catalog, extractSqlServerDatabaseName(jdbcUrl));
        if (effectiveDatabase == null) {
            throw new DataAccessException("""
                    SQL Server connection is missing a target database.
                    Add `databaseName=<your_database>` to `exploding-kittens.db.url` or `EXPLODING_KITTENS_DB_URL`.
                    """.stripIndent().trim());
        }
        if ("master".equalsIgnoreCase(effectiveDatabase.trim())) {
            throw new DataAccessException("""
                    SQL Server connection is targeting the `master` database.
                    Use an application database instead by setting `databaseName=<your_database>` in `exploding-kittens.db.url` or `EXPLODING_KITTENS_DB_URL`.
                    """.stripIndent().trim());
        }
    }

    static String extractSqlServerDatabaseName(String jdbcUrl) {
        Matcher matcher = SQL_SERVER_DATABASE_PATTERN.matcher(clean(jdbcUrl));
        if (!matcher.find()) {
            return null;
        }

        String databaseName = matcher.group(1).trim();
        return databaseName.isEmpty() ? null : databaseName;
    }

    private static String readConfiguredValue(String envName, String propertyName, String fallback) {
        String property = System.getProperty(propertyName);
        if (property != null && !property.isBlank()) {
            return property;
        }

        String fileProperty = FILE_PROPERTIES.getProperty(propertyName);
        if (fileProperty != null && !fileProperty.isBlank()) {
            return fileProperty;
        }

        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }

        return fallback;
    }

    private static Properties loadFileProperties() {
        Properties properties = new Properties();
        loadPropertiesFromProjectFile(properties);
        loadPropertiesFromClasspath(properties);
        return properties;
    }

    private static void loadPropertiesFromProjectFile(Properties properties) {
        Path propertiesPath = Path.of(DATABASE_PROPERTIES_FILE);
        if (!Files.exists(propertiesPath)) {
            return;
        }

        try (InputStream inputStream = Files.newInputStream(propertiesPath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new DataAccessException("Failed to load database configuration from " + DATABASE_PROPERTIES_FILE, e);
        }
    }

    private static void loadPropertiesFromClasspath(Properties properties) {
        try (InputStream inputStream = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream(DATABASE_PROPERTIES_FILE)) {
            if (inputStream == null) {
                return;
            }

            Properties classpathProperties = new Properties();
            classpathProperties.load(inputStream);
            classpathProperties.forEach((key, value) ->
                    properties.putIfAbsent(String.valueOf(key), String.valueOf(value)));
        } catch (IOException e) {
            throw new DataAccessException("Failed to load database configuration from classpath " + DATABASE_PROPERTIES_FILE, e);
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
