package easv.dal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectionTest {

    @Test
    void extractsSqlServerDatabaseNameFromJdbcUrl() {
        String databaseName = DatabaseConnection.extractSqlServerDatabaseName(
                "jdbc:sqlserver://localhost:1433;encrypt=true;databaseName=Exploding_Kittens;trustServerCertificate=true");

        assertEquals("Exploding_Kittens", databaseName);
    }

    @Test
    void rejectsSqlServerConnectionWithoutDatabaseName() {
        DataAccessException exception = assertThrows(DataAccessException.class, () ->
                DatabaseConnection.validateDatabaseTarget(
                        null,
                        "jdbc:sqlserver://localhost:1433;encrypt=true;trustServerCertificate=true"));

        assertTrue(exception.getMessage().contains("databaseName=<your_database>"));
    }

    @Test
    void rejectsSqlServerConnectionPointingAtMaster() {
        DataAccessException exception = assertThrows(DataAccessException.class, () ->
                DatabaseConnection.validateDatabaseTarget(
                        "master",
                        "jdbc:sqlserver://localhost:1433;encrypt=true;databaseName=Exploding_Kittens"));

        assertTrue(exception.getMessage().contains("master"));
    }
}
