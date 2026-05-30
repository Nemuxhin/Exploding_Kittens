package easv.bll;

import easv.be.AuditLog;
import easv.be.Document;
import easv.be.ExportRecord;
import easv.be.TiffExportItem;
import easv.be.TiffExportPlan;
import easv.dal.AuditLogDAO;
import easv.dal.ExportDAO;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ExportService {
    private final TiffExportManager tiffExportManager;
    private final ExportDAO exportDAO;
    private final AuditLogDAO auditLogDAO;

    public ExportService() {
        this(new TiffExportManager(), new ExportDAO(), new AuditLogDAO());
    }

    ExportService(TiffExportManager tiffExportManager, ExportDAO exportDAO, AuditLogDAO auditLogDAO) {
        this.tiffExportManager = tiffExportManager == null ? new TiffExportManager() : tiffExportManager;
        this.exportDAO = exportDAO == null ? new ExportDAO() : exportDAO;
        this.auditLogDAO = auditLogDAO == null ? new AuditLogDAO() : auditLogDAO;
    }

    public TiffExportManager.ExportResult exportPlan(
            UUID sessionId,
            String profileName,
            String boxId,
            TiffExportPlan plan,
            Path outputDirectory,
            List<Document> sourceDocuments
    ) throws IOException {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory must not be null");
        }

        UserSessionUser currentUser = currentUser();
        String target = buildTarget(profileName, boxId);

        try {
            TiffExportManager.ExportResult result = tiffExportManager.exportPlan(plan, outputDirectory);
            List<TiffExportItem> items = plan.getItems();
            for (int index = 0; index < items.size(); index++) {
                TiffExportItem item = items.get(index);
                Path writtenPath = index < result.writtenFiles().size()
                        ? result.writtenFiles().get(index)
                        : outputDirectory.resolve(item.getFileName());
                ExportRecord record = new ExportRecord(
                        UUID.randomUUID(),
                        sessionId,
                        resolveDocumentId(item.getDocumentId(), sourceDocuments),
                        currentUser.userId(),
                        "TIFF",
                        "SUCCESS",
                        item.getFileName(),
                        writtenPath.toString(),
                        Instant.now(),
                        null
                );
                exportDAO.save(record);
                auditLogDAO.saveAuditLog(new AuditLog(
                        auditLogDAO.nextAuditLogId(),
                        LocalDateTime.now(),
                        "Exports",
                        currentUser.actor(),
                        "EXPORT_COMPLETED",
                        target,
                        "Success",
                        "TIFF export completed: " + item.getFileName(),
                        List.of()
                ), record.id());
            }
            return result;
        } catch (IOException | RuntimeException exception) {
            String failedFileName = plan.getItems().isEmpty() ? fallbackFileName(profileName, boxId) : plan.getItems().get(0).getFileName();
            ExportRecord failedRecord = new ExportRecord(
                    UUID.randomUUID(),
                    sessionId,
                    null,
                    currentUser.userId(),
                    "TIFF",
                    "FAILED",
                    failedFileName,
                    outputDirectory.resolve(failedFileName).toString(),
                    Instant.now(),
                    cleanMessage(exception.getMessage())
            );
            try {
                exportDAO.save(failedRecord);
                auditLogDAO.saveAuditLog(new AuditLog(
                        auditLogDAO.nextAuditLogId(),
                        LocalDateTime.now(),
                        "Exports",
                        currentUser.actor(),
                        "EXPORT_FAILED",
                        target,
                        "Failed",
                        cleanMessage(exception.getMessage()),
                        List.of()
                ), failedRecord.id());
            } catch (RuntimeException ignored) {
                // Keep the original export failure as the primary signal to the caller.
            }
            throw exception;
        }
    }

    private UUID resolveDocumentId(String exportDocumentId, List<Document> sourceDocuments) {
        if (exportDocumentId == null || exportDocumentId.isBlank() || sourceDocuments == null) {
            return null;
        }

        return sourceDocuments.stream()
                .filter(document -> exportDocumentId.equals(document.getSourceItemId()))
                .map(Document::getId)
                .findFirst()
                .orElse(null);
    }

    private String buildTarget(String profileName, String boxId) {
        String safeProfile = cleanMessage(profileName);
        String safeBox = cleanMessage(boxId);
        if (safeProfile.isBlank() && safeBox.isBlank()) {
            return "Export";
        }
        if (safeProfile.isBlank()) {
            return safeBox;
        }
        if (safeBox.isBlank()) {
            return safeProfile;
        }
        return safeProfile + " / " + safeBox;
    }

    private String fallbackFileName(String profileName, String boxId) {
        String safeProfile = cleanMessage(profileName).replace(' ', '_');
        String safeBox = cleanMessage(boxId).replace(' ', '_');
        if (safeProfile.isBlank() && safeBox.isBlank()) {
            return "export-batch.tiff";
        }
        if (safeProfile.isBlank()) {
            return safeBox + ".tiff";
        }
        if (safeBox.isBlank()) {
            return safeProfile + ".tiff";
        }
        return safeProfile + "_" + safeBox + ".tiff";
    }

    private String cleanMessage(String value) {
        if (value == null || value.isBlank()) {
            return "No additional details.";
        }
        return value.trim();
    }

    private UserSessionUser currentUser() {
        easv.be.User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            return new UserSessionUser(null, "SYSTEM");
        }
        return new UserSessionUser(currentUser.getId(), currentUser.getUsername());
    }

    private record UserSessionUser(Integer userId, String actor) {
    }
}
