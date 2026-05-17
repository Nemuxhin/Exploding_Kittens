package easv.dal;

import easv.be.Box;
import easv.be.Client;
import easv.be.Document;
import easv.be.PageImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentDAOTest {
    private static final String DATABASE_PROPERTIES_FILE = "database.properties";
    private static final String DEFAULT_JDBC_URL =
            "jdbc:sqlserver://10.176.111.34:1433;databaseName=Exploding_Kittens;encrypt=true;trustServerCertificate=true";
    private static final String DEFAULT_USERNAME = "";
    private static final String DEFAULT_PASSWORD = "";
    private static final String TEST_DATABASE_NAME = "Exploding_Kittens_JUnit";
    private DatabaseConnection databaseConnection;

    @BeforeEach
    void resetDatabase() {
        databaseConnection = createTestDatabaseConnection();
    }

    @Test
    void persistsAddMoveDeleteAndRestorePageOperations() {
        PageImageDAO pageImageDAO = new PageImageDAO(databaseConnection);
        DocumentDAO documentDAO = new DocumentDAO(databaseConnection, pageImageDAO);
        CaseFileDAO caseFileDAO = new CaseFileDAO(databaseConnection, documentDAO);
        ClientDAO clientDAO = new ClientDAO(databaseConnection);
        BoxDAO boxDAO = new BoxDAO(databaseConnection);

        Client client = clientDAO.saveOrGetExisting("CLIENT-1", "Northwind");
        Box box = boxDAO.saveOrGetExisting("BOX-1", "Shelf A");
        var caseFile = caseFileDAO.saveOrGetExisting("CASE-1", client, box);

        Document storedDocument = documentDAO.saveOrGetExisting(new Document(
                "ITEM-1",
                List.of(
                        new PageImage(1, PageImage.PageType.TIFF, "page-1.tiff"),
                        new PageImage(2, PageImage.PageType.TIFF, "page-2.tiff")
                )
        ), caseFile.getId());

        PageImage insertedPage = new PageImage(1, PageImage.PageType.TIFF, "inserted-page.tiff");
        documentDAO.addPage(storedDocument.getId(), insertedPage, 2);
        Document afterInsert = documentDAO.findBySourceItemId("ITEM-1").orElseThrow();
        assertEquals(List.of("page-1.tiff", "inserted-page.tiff", "page-2.tiff"),
                afterInsert.getPages().stream().map(PageImage::getSourceReference).toList());

        documentDAO.movePage(afterInsert.getId(), insertedPage.getId(), 3);
        Document afterMove = documentDAO.findBySourceItemId("ITEM-1").orElseThrow();
        assertEquals(List.of("page-1.tiff", "page-2.tiff", "inserted-page.tiff"),
                afterMove.getPages().stream().map(PageImage::getSourceReference).toList());

        documentDAO.deletePage(afterMove.getId(), insertedPage.getId());
        Document afterDelete = documentDAO.findBySourceItemId("ITEM-1").orElseThrow();
        assertEquals(2, afterDelete.getPages().size());
        assertTrue(afterDelete.getDeletedPages().stream().anyMatch(page -> page.getId().equals(insertedPage.getId())));

        documentDAO.restorePage(afterDelete.getId(), insertedPage.getId(), 1);
        Document afterRestore = documentDAO.findBySourceItemId("ITEM-1").orElseThrow();
        assertEquals(List.of("inserted-page.tiff", "page-1.tiff", "page-2.tiff"),
                afterRestore.getPages().stream().map(PageImage::getSourceReference).toList());
    }

    private DatabaseConnection createTestDatabaseConnection() {
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
