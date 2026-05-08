package easv.dal;

import easv.be.AuditLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Simple audit log storage.
 * Later this class can be replaced with database code without changing the BLL.
 */
public class AuditLogDAO {

    private final List<AuditLog> auditLogs = new ArrayList<>();
    private final Path storagePath;
    private int nextId = 1;

    public AuditLogDAO() {
        this(defaultStoragePath());
    }

    private AuditLogDAO(Path storagePath) {
        this.storagePath = storagePath;
        loadFromFile();
    }

    public static AuditLogDAO inMemory() {
        return new AuditLogDAO(null);
    }

    public int nextId() {
        return nextId++;
    }

    public void save(AuditLog auditLog) {
        auditLogs.add(auditLog);
        saveToFile();
    }

    public List<AuditLog> findAll() {
        return new ArrayList<>(auditLogs);
    }

    public void clear() {
        auditLogs.clear();
        nextId = 1;
        saveToFile();
    }

    private void loadFromFile() {
        if (storagePath == null || !Files.exists(storagePath)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(storagePath, StandardCharsets.UTF_8)) {
                AuditLog auditLog = fromLine(line);

                if (auditLog != null) {
                    auditLogs.add(auditLog);
                    nextId = Math.max(nextId, auditLog.getId() + 1);
                }
            }
        } catch (IOException ignored) {
            // If the demo log file cannot be read, the app can still continue.
        }
    }

    private void saveToFile() {
        if (storagePath == null) {
            return;
        }

        try {
            Files.createDirectories(storagePath.getParent());
            List<String> lines = auditLogs.stream()
                    .map(this::toLine)
                    .toList();
            Files.write(storagePath, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // The UI should not crash if local demo persistence fails.
        }
    }

    private String toLine(AuditLog auditLog) {
        return String.join("\t",
                String.valueOf(auditLog.getId()),
                encode(auditLog.getTimestamp().toString()),
                encode(auditLog.getType()),
                encode(auditLog.getActor()),
                encode(auditLog.getAction()),
                encode(auditLog.getTarget()),
                encode(auditLog.getStatus()),
                encode(auditLog.getDescription()),
                encodeDetails(auditLog.getDetails())
        );
    }

    private AuditLog fromLine(String line) {
        String[] parts = line.split("\t", -1);

        if (parts.length != 9) {
            return null;
        }

        try {
            return new AuditLog(
                    Integer.parseInt(parts[0]),
                    LocalDateTime.parse(decode(parts[1])),
                    decode(parts[2]),
                    decode(parts[3]),
                    decode(parts[4]),
                    decode(parts[5]),
                    decode(parts[6]),
                    decode(parts[7]),
                    decodeDetails(parts[8])
            );
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String encodeDetails(List<AuditLog.AuditLogDetail> details) {
        List<String> encodedDetails = new ArrayList<>();

        for (AuditLog.AuditLogDetail detail : details) {
            encodedDetails.add(encode(detail.getLabel()) + ":" + encode(detail.getValue()));
        }

        return String.join(",", encodedDetails);
    }

    private List<AuditLog.AuditLogDetail> decodeDetails(String value) {
        List<AuditLog.AuditLogDetail> details = new ArrayList<>();

        if (value == null || value.isBlank()) {
            return details;
        }

        for (String item : value.split(",")) {
            String[] parts = item.split(":", 2);

            if (parts.length == 2) {
                details.add(new AuditLog.AuditLogDetail(decode(parts[0]), decode(parts[1])));
            }
        }

        return details;
    }

    private String encode(String value) {
        String safeValue = value == null ? "" : value;
        return Base64.getEncoder().encodeToString(safeValue.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static Path defaultStoragePath() {
        return Path.of(System.getProperty("user.home"), ".prismlager", "audit-log.tsv");
    }
}
