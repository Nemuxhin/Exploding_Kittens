package easv.dal;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class DatabaseConnection {
    private static final String DATABASE_PROPERTIES_FILE = "database.properties";
    private static final String DEFAULT_JDBC_URL =
            "jdbc:sqlserver://10.176.111.34:1433;databaseName=Exploding_Kittens;encrypt=true;trustServerCertificate=true";
    private static final String DEFAULT_USERNAME = "";
    private static final String DEFAULT_PASSWORD = "";
    private static final Properties FILE_PROPERTIES = loadFileProperties();
    private static final HikariDataSource SHARED_DATA_SOURCE = createSharedDataSource();

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final boolean useSharedDataSource;

    public DatabaseConnection() {
        this(
                readConfiguredValue("EXPLODING_KITTENS_DB_URL", "exploding-kittens.db.url", DEFAULT_JDBC_URL),
                readConfiguredValue("EXPLODING_KITTENS_DB_USER", "exploding-kittens.db.user", DEFAULT_USERNAME),
                readConfiguredValue("EXPLODING_KITTENS_DB_PASSWORD", "exploding-kittens.db.password", DEFAULT_PASSWORD),
                true
        );
    }

    public DatabaseConnection(String jdbcUrl, String username, String password) {
        this(jdbcUrl, username, password, false);
    }

    private DatabaseConnection(String jdbcUrl, String username, String password, boolean useSharedDataSource) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.useSharedDataSource = useSharedDataSource;
    }

    public Connection getConnection() throws SQLException {
        if (useSharedDataSource) {
            return SHARED_DATA_SOURCE.getConnection();
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    static boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<String> schemaCandidates = getSchemaCandidates(connection);
        for (String candidate : new String[]{tableName, tableName.toUpperCase(Locale.ROOT), tableName.toLowerCase(Locale.ROOT)}) {
            for (String schemaCandidate : schemaCandidates) {
                try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), schemaCandidate, candidate, new String[]{"TABLE"})) {
                    if (resultSet.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<String> schemaCandidates = getSchemaCandidates(connection);
        String[] tableCandidates = new String[]{tableName, tableName.toUpperCase(Locale.ROOT), tableName.toLowerCase(Locale.ROOT)};
        String[] columnCandidates = new String[]{columnName, columnName.toUpperCase(Locale.ROOT), columnName.toLowerCase(Locale.ROOT)};

        for (String tableCandidate : tableCandidates) {
            for (String columnCandidate : columnCandidates) {
                for (String schemaCandidate : schemaCandidates) {
                    try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), schemaCandidate, tableCandidate, columnCandidate)) {
                        if (resultSet.next()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static List<String> getSchemaCandidates(Connection connection) throws SQLException {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String currentSchema = connection.getSchema();
        if (currentSchema != null && !currentSchema.isBlank()) {
            candidates.add(currentSchema);
        }
        candidates.add("PUBLIC");
        candidates.add("dbo");
        return new ArrayList<>(candidates);
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
        } catch (IOException exception) {
            throw new DataAccessException("Failed to load " + DATABASE_PROPERTIES_FILE, exception);
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
        } catch (IOException exception) {
            throw new DataAccessException("Failed to load classpath " + DATABASE_PROPERTIES_FILE, exception);
        }
    }

    private static HikariDataSource createSharedDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(readConfiguredValue("EXPLODING_KITTENS_DB_URL", "exploding-kittens.db.url", DEFAULT_JDBC_URL));
        config.setUsername(readConfiguredValue("EXPLODING_KITTENS_DB_USER", "exploding-kittens.db.user", DEFAULT_USERNAME));
        config.setPassword(readConfiguredValue("EXPLODING_KITTENS_DB_PASSWORD", "exploding-kittens.db.password", DEFAULT_PASSWORD));
        config.setPoolName("exploding-kittens-db");
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(Math.max(4, Runtime.getRuntime().availableProcessors()));
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(2000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(600000);
        config.setAutoCommit(true);
        return new HikariDataSource(config);
    }
}
