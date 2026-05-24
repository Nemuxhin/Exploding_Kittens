package easv.dal;

import easv.be.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class ClientDAO {
    private final DatabaseConnection databaseConnection;

    public ClientDAO() {
        this(new DatabaseConnection());
    }

    public ClientDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public Client saveOrGetExisting(String clientNumber, String name) {
        validateKey(clientNumber, "clientNumber");
        validateKey(name, "name");
        try (Connection connection = databaseConnection.getConnection()) {
            Optional<Client> existing = findByClientNumber(connection, clientNumber);
            if (existing.isPresent()) {
                return existing.get();
            }
            return insert(connection, clientNumber, name);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to store client " + clientNumber, e);
        }
    }

    public Optional<Client> findByClientNumber(String clientNumber) {
        validateKey(clientNumber, "clientNumber");
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, client_number, name FROM clients WHERE client_number = ?")) {
            statement.setString(1, clientNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapClient(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch client " + clientNumber, e);
        }
    }

    private Optional<Client> findByClientNumber(Connection connection, String clientNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, client_number, name FROM clients WHERE client_number = ?")) {
            statement.setString(1, clientNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapClient(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public Collection<Client> findAll() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, client_number, name FROM clients ORDER BY client_number");
             ResultSet resultSet = statement.executeQuery()) {
            Collection<Client> clients = new ArrayList<>();
            while (resultSet.next()) {
                clients.add(mapClient(resultSet));
            }
            return clients;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch clients", e);
        }
    }

    private static void validateKey(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private Client insert(Connection connection, String clientNumber, String name) throws SQLException {
        Client client = new Client(UUID.randomUUID(), clientNumber, name);
        try (PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO clients (id, client_number, name) VALUES (?, ?, ?)")) {
            statement.setString(1, client.getId().toString());
            statement.setString(2, client.getClientNumber());
            statement.setString(3, client.getName());
            statement.executeUpdate();
            return client;
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                return findByClientNumber(connection, clientNumber).orElseThrow();
            }
            throw e;
        }
    }

    private Client mapClient(ResultSet resultSet) throws SQLException {
        return new Client(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("client_number"),
                resultSet.getString("name")
        );
    }

    private boolean isUniqueViolation(SQLException e) {
        return e.getSQLState() != null && e.getSQLState().startsWith("23");
    }
}
