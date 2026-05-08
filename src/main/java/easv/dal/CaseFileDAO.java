package easv.dal;

import easv.be.Box;
import easv.be.CaseFile;
import easv.be.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class CaseFileDAO {
    private final DatabaseConnection databaseConnection;
    private final DocumentDAO documentDAO;

    public CaseFileDAO() {
        this(new DatabaseConnection(), new DocumentDAO());
    }

    public CaseFileDAO(DatabaseConnection databaseConnection, DocumentDAO documentDAO) {
        this.databaseConnection = databaseConnection;
        this.documentDAO = documentDAO;
    }

    public CaseFile saveOrGetExisting(String caseReference, Client client, Box box) {
        if (caseReference == null || caseReference.isBlank()) {
            throw new IllegalArgumentException("caseReference must not be blank");
        }
        return findByReference(caseReference)
                .orElseGet(() -> insert(caseReference, client, box));
    }

    public Optional<CaseFile> findByReference(String caseReference) {
        if (caseReference == null || caseReference.isBlank()) {
            throw new IllegalArgumentException("caseReference must not be blank");
        }
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT cf.id, cf.case_reference,
                            c.id AS client_id, c.client_number, c.name,
                            b.id AS box_pk, b.box_id, b.description
                     FROM case_files cf
                     JOIN clients c ON c.id = cf.client_id
                     JOIN boxes b ON b.id = cf.box_id
                     WHERE cf.case_reference = ?
                     """)) {
            statement.setString(1, caseReference);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapCaseFile(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch case file " + caseReference, e);
        }
    }

    public Collection<CaseFile> findAll() {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT cf.id, cf.case_reference,
                            c.id AS client_id, c.client_number, c.name,
                            b.id AS box_pk, b.box_id, b.description
                     FROM case_files cf
                     JOIN clients c ON c.id = cf.client_id
                     JOIN boxes b ON b.id = cf.box_id
                     ORDER BY cf.case_reference
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            Collection<CaseFile> caseFiles = new ArrayList<>();
            while (resultSet.next()) {
                caseFiles.add(mapCaseFile(resultSet));
            }
            return caseFiles;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch case files", e);
        }
    }

    private CaseFile insert(String caseReference, Client client, Box box) {
        CaseFile caseFile = new CaseFile(UUID.randomUUID(), caseReference, client, box);
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO case_files (id, case_reference, client_id, box_id) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, caseFile.getId().toString());
            statement.setString(2, caseFile.getCaseReference());
            statement.setString(3, client.getId().toString());
            statement.setString(4, box.getId().toString());
            statement.executeUpdate();
            return caseFile;
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                return findByReference(caseReference).orElseThrow();
            }
            throw new DataAccessException("Failed to store case file " + caseReference, e);
        }
    }

    private CaseFile mapCaseFile(ResultSet resultSet) throws SQLException {
        Client client = new Client(
                UUID.fromString(resultSet.getString("client_id")),
                resultSet.getString("client_number"),
                resultSet.getString("name")
        );
        Box box = new Box(
                UUID.fromString(resultSet.getString("box_pk")),
                resultSet.getString("box_id"),
                resultSet.getString("description")
        );
        UUID caseFileId = UUID.fromString(resultSet.getString("id"));
        CaseFile caseFile = new CaseFile(caseFileId, resultSet.getString("case_reference"), client, box);
        for (var document : documentDAO.findByCaseFileId(caseFileId)) {
            caseFile.addDocument(document);
        }
        return caseFile;
    }

    private boolean isUniqueViolation(SQLException e) {
        return e.getSQLState() != null && e.getSQLState().startsWith("23");
    }
}
