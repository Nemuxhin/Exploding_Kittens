package easv.dal;

import easv.be.Box;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class BoxDAO {
    private final DatabaseConnection databaseConnection;

    public BoxDAO() {
        this(new DatabaseConnection());
    }

    public BoxDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public Box saveOrGetExisting(String boxId, String description) {
        validateKey(boxId, "boxId");
        validateKey(description, "description");
        return findByBoxId(boxId)
                .orElseGet(() -> insert(boxId, description));
    }

    public Optional<Box> findByBoxId(String boxId) {
        validateKey(boxId, "boxId");
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, box_id, description FROM boxes WHERE box_id = ?")) {
            statement.setString(1, boxId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapBox(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch box " + boxId, e);
        }
    }

    public Collection<Box> findAll() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, box_id, description FROM boxes ORDER BY box_id");
             ResultSet resultSet = statement.executeQuery()) {
            Collection<Box> boxes = new ArrayList<>();
            while (resultSet.next()) {
                boxes.add(mapBox(resultSet));
            }
            return boxes;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch boxes", e);
        }
    }

    private static void validateKey(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private Box insert(String boxId, String description) {
        Box box = new Box(UUID.randomUUID(), boxId, description);
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO boxes (id, box_id, description) VALUES (?, ?, ?)")) {
            statement.setString(1, box.getId().toString());
            statement.setString(2, box.getBoxId());
            statement.setString(3, box.getDescription());
            statement.executeUpdate();
            return box;
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                return findByBoxId(boxId).orElseThrow();
            }
            throw new DataAccessException("Failed to store box " + boxId, e);
        }
    }

    private Box mapBox(ResultSet resultSet) throws SQLException {
        return new Box(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("box_id"),
                resultSet.getString("description")
        );
    }

    private boolean isUniqueViolation(SQLException e) {
        return e.getSQLState() != null && e.getSQLState().startsWith("23");
    }
}
