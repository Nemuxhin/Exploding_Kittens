package easv.dal;

import easv.bll.QAService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationDAO {
    private final DatabaseConnection databaseConnection;

    public NotificationDAO() {
        this(new DatabaseConnection());
    }

    public NotificationDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public void create(int userId, UUID reviewId, String title, String message) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO user_notifications (
                         id,
                         user_id,
                         qa_review_id,
                         title,
                         message,
                         created_at,
                         read_at
                     ) VALUES (?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setInt(2, userId);
            if (reviewId == null) {
                statement.setNull(3, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, reviewId.toString());
            }
            statement.setString(4, title == null ? "" : title.trim());
            statement.setString(5, message == null ? "" : message.trim());
            statement.setTimestamp(6, Timestamp.from(Instant.now()));
            statement.setNull(7, java.sql.Types.TIMESTAMP);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create notification.", e);
        }
    }

    public List<QAService.NotificationSnapshot> findByUser(int userId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id,
                            user_id,
                            qa_review_id,
                            title,
                            message,
                            created_at,
                            read_at
                     FROM user_notifications
                     WHERE user_id = ?
                     ORDER BY created_at DESC
                     """)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QAService.NotificationSnapshot> notifications = new ArrayList<>();
                while (resultSet.next()) {
                    notifications.add(new QAService.NotificationSnapshot(
                            UUID.fromString(resultSet.getString("id")),
                            resultSet.getInt("user_id"),
                            parseUuid(resultSet.getString("qa_review_id")),
                            resultSet.getString("title"),
                            resultSet.getString("message"),
                            resultSet.getTimestamp("created_at").toInstant(),
                            resultSet.getTimestamp("read_at") == null
                                    ? null
                                    : resultSet.getTimestamp("read_at").toInstant()
                    ));
                }
                return notifications;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read notifications.", e);
        }
    }

    public void markAllRead(int userId) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE user_notifications
                     SET read_at = ?
                     WHERE user_id = ?
                       AND read_at IS NULL
                     """)) {
            statement.setTimestamp(1, Timestamp.from(Instant.now()));
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark notifications as read.", e);
        }
    }

    public void deleteByReviewId(Connection connection, UUID reviewId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM user_notifications
                WHERE qa_review_id = ?
                """)) {
            statement.setString(1, reviewId.toString());
            statement.executeUpdate();
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }
}
