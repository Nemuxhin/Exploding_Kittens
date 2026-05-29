package easv.be;

import java.time.Instant;
import java.util.UUID;

public record ExportRecord(
        UUID id,
        UUID sessionId,
        UUID documentId,
        Integer exportedByUserId,
        String exportFormat,
        String exportStatus,
        String fileName,
        String filePath,
        Instant exportedAt,
        String errorMessage
) {
    public ExportRecord {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        if (exportFormat == null || exportFormat.isBlank()) {
            throw new IllegalArgumentException("exportFormat must not be blank");
        }
        if (exportStatus == null || exportStatus.isBlank()) {
            throw new IllegalArgumentException("exportStatus must not be blank");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath must not be blank");
        }
        exportedAt = exportedAt == null ? Instant.now() : exportedAt;
        exportFormat = exportFormat.trim();
        exportStatus = exportStatus.trim();
        fileName = fileName.trim();
        filePath = filePath.trim();
        errorMessage = errorMessage == null ? null : errorMessage.trim();
    }
}
