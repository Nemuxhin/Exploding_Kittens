package easv.bll;

import easv.be.AuditLog;
import easv.be.PageImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * These tests make logging precise without depending on the real database.
 * A fixed Clock is used so the timestamp is predictable and testable.
 */
class AuditLogManagerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-05-08T12:30:00Z");

    private AuditLogManager auditLogManager;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));
        auditLogManager = new AuditLogManager(fixedClock);
    }

    @Test
    void logFileCreated_shouldStoreWhoWhenAndWhichFileWasCreated() {
        PageImage page = new PageImage(7, 2, "page-007.png");

        AuditLog entry = auditLogManager.logFileCreated("jennifer", page);

        assertAll(
                () -> assertEquals("FILE_CREATED", entry.getActionType()),
                () -> assertEquals("jennifer", entry.getUsername()),
                () -> assertEquals(FIXED_TIME, entry.getTimestamp()),
                () -> assertEquals(7, entry.getReferenceId()),
                () -> assertEquals("page-007.png", entry.getFileName()),
                () -> assertEquals(List.of(entry), auditLogManager.getEntries(), "The created log entry should be stored for later persistence.")
        );
    }

    @Test
    void logFileDeleted_shouldStoreWhoWhenAndWhichFileWasDeleted() {
        PageImage page = new PageImage(3, 1, "page-003.png");

        AuditLog entry = auditLogManager.logFileDeleted("samuele", page);

        assertAll(
                () -> assertEquals("FILE_DELETED", entry.getActionType()),
                () -> assertEquals("samuele", entry.getUsername()),
                () -> assertEquals(FIXED_TIME, entry.getTimestamp()),
                () -> assertEquals(3, entry.getReferenceId()),
                () -> assertEquals("page-003.png", entry.getFileName())
        );
    }

    @Test
    void logging_shouldRejectBlankUsernameBecauseWhoDidItIsRequired() {
        PageImage page = new PageImage(1, 1, "page-001.png");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> auditLogManager.logFileCreated(null, page)),
                () -> assertThrows(IllegalArgumentException.class, () -> auditLogManager.logFileCreated("", page)),
                () -> assertThrows(IllegalArgumentException.class, () -> auditLogManager.logFileCreated("   ", page))
        );
    }

    @Test
    void logging_shouldRejectMissingPageBecauseAFileActionNeedsAFile() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> auditLogManager.logFileCreated("jennifer", null)),
                () -> assertThrows(IllegalArgumentException.class, () -> auditLogManager.logFileDeleted("jennifer", null))
        );
    }
}
