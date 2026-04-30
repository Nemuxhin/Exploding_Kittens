package easv.gui;

import java.util.List;

public class UserPortalModel {

    public record NavigationItem(String label) {
    }

    public record ActionCard(String badge, String title, String subtitle, boolean accent) {
    }

    public record ScanRow(String boxId, String profile, String status, String date, int pages) {
    }

    public record ActivityLogEntry(String user, String action, String file, String document, String profile, String boxId, String date, String time) {
    }

    public record ExportRow(String fileName, String boxId, String profile, String dateCreated, String size, String status) {
    }

    public record ProfileSummary(String profileName, String autoRotate, String brightness, String removeBlankPages) {
    }

    public record ScanProfile(String title, String description, String settingsLine) {
    }

    public record ScanningFile(String badge, String fileName, String reference, String pages, int pageCount, boolean barcode) {
    }

    public record ScanningDocument(String title, String fileCountText, List<ScanningFile> files) {
    }

    public List<NavigationItem> sidebarItems() {
        return List.of(
                new NavigationItem("Dashboard"),
                new NavigationItem("Exports"),
                new NavigationItem("Profiles"),
                new NavigationItem("My Scans")
        );
    }

    public List<ActionCard> actionCards() {
        return List.of(
                new ActionCard("[]", "Start Scan", "Begin a new scanning session", true),
                new ActionCard("MY", "My Scans", "View scan history", false),
                new ActionCard("EX", "Exports", "Download your files", false)
        );
    }

    public List<ScanRow> recentScans() {
        return List.of(
                new ScanRow("BOX-2026-042", "Standard Scan", "Completed", "2026-04-24", 125),
                new ScanRow("BOX-2026-041", "High Quality", "Processing", "2026-04-24", 89),
                new ScanRow("BOX-2026-040", "Standard Scan", "Completed", "2026-04-23", 203),
                new ScanRow("BOX-2026-039", "Archive", "Failed", "2026-04-23", 0)
        );
    }

    public List<ActivityLogEntry> activityLog() {
        return List.of(
                new ActivityLogEntry("Mike", "Deleted", "File 05", "Old Contract", "Legal", "Box 18", "2026-04-23", "11:10"),
                new ActivityLogEntry("Admin", "Created User", "New Scanner User", "-", "-", "-", "2026-04-23", "11:30"),
                new ActivityLogEntry("Emma", "Exported", "Document A", "Invoice A", "Finance", "Box 12", "2026-04-23", "10:45"),
                new ActivityLogEntry("Emma", "QA Approved", "Document A", "Invoice A", "Finance", "Box 12", "2026-04-23", "10:30"),
                new ActivityLogEntry("John", "Sent to QA", "Document A", "Invoice A", "Finance", "Box 12", "2026-04-23", "10:00"),
                new ActivityLogEntry("Sara", "Updated", "File 03", "Contract B", "Legal", "Box 15", "2026-04-23", "09:25"),
                new ActivityLogEntry("Sara", "Moved", "File 03", "Contract B", "Legal", "Box 15", "2026-04-23", "09:20"),
                new ActivityLogEntry("JD", "Scanned", "File 03", "Invoice A", "Standard Scan", "BOX-2026-043", "2026-04-27", "09:08"),
                new ActivityLogEntry("JD", "Sent to QA", "File 02", "Invoice A", "Standard Scan", "BOX-2026-043", "2026-04-27", "09:18"),
                new ActivityLogEntry("JD", "Exported", "File 01", "Invoice A", "Standard Scan", "BOX-2026-043", "2026-04-27", "09:40")
        );
    }

    public List<ExportRow> exports() {
        return List.of(
                new ExportRow("StandardScan_BOX-2026-042.pdf", "BOX-2026-042", "Standard Scan", "2026-04-24", "45.2 MB", "Ready"),
                new ExportRow("HighQuality_BOX-2026-041.pdf", "BOX-2026-041", "High Quality", "2026-04-24", "89.7 MB", "Processing"),
                new ExportRow("StandardScan_BOX-2026-040.pdf", "BOX-2026-040", "Standard Scan", "2026-04-23", "67.3 MB", "Ready"),
                new ExportRow("Archive_BOX-2026-038.pdf", "BOX-2026-038", "Archive", "2026-04-22", "34.8 MB", "Ready")
        );
    }

    public List<ActivityLogEntry> myScanActivity() {
        return activityLog().stream()
                .filter(entry -> "JD".equals(entry.user()))
                .toList();
    }

    public ProfileSummary activeProfile() {
        return new ProfileSummary("Standard Scan", "5 deg", "+10%", "On");
    }

    public List<ScanProfile> scanProfiles() {
        return List.of(
                new ScanProfile(
                        "Finance Profile",
                        "Financial document intake and classification",
                        "Brightness: +10% | Remove blanks: On | OCR: On"
                ),
                new ScanProfile(
                        "HR Profile",
                        "Employee records and onboarding packets",
                        "Brightness: +8% | Remove blanks: On | OCR: On"
                ),
                new ScanProfile(
                        "Legal Profile",
                        "Legal files and case paperwork",
                        "Brightness: +12% | OCR: On | File split: Barcode"
                ),
                new ScanProfile(
                        "Medical Records Profile",
                        "Patient charts and healthcare forms",
                        "Brightness: +12% | OCR: On | Remove blanks: On"
                ),
                new ScanProfile(
                        "Contracts Profile",
                        "Signed agreements and supporting pages",
                        "Brightness: +15% | OCR: On | File split: Barcode"
                ),
                new ScanProfile(
                        "Invoice Profile",
                        "Fast structured invoice capture",
                        "Brightness: +10% | Remove blanks: On | OCR: On"
                )
        );
    }

    public List<ScanningDocument> scanningDocuments() {
        return List.of(
                                new ScanningDocument(
                        "Document 1",
                        "12 files",
                        List.of(
                                new ScanningFile("File 1", "Invoice_batch_01.tiff", "Ref: REF-001", "1 page", 1, false),
                                new ScanningFile("File 2", "Contract_batch_02.tiff", "Ref: REF-002", "1 page", 1, false),
                                new ScanningFile("File 3", "Cover_sheet_03.tiff", "Ref: REF-003", "2 pages", 2, false)
                        )
                ),
                new ScanningDocument(
                        "Document 2",
                        "8 files",
                        List.of(
                                new ScanningFile("File 1", "Archive_batch_04.tiff", "Ref: REF-004", "3 pages", 3, true),
                                new ScanningFile("File 2", "Archive_batch_05.tiff", "Ref: REF-005", "1 page", 1, false)
                        )
                ),
                new ScanningDocument(
                        "Document 3",
                        "15 files",
                        List.of(
                                new ScanningFile("File 1", "Medical_record_06.tiff", "Ref: REF-006", "4 pages", 4, false),
                                new ScanningFile("File 2", "Medical_record_07.tiff", "Ref: REF-007", "2 pages", 2, false),
                                new ScanningFile("File 3", "Separator_08.tiff", "Ref: REF-008", "1 page", 1, true)
                        )
                ),
                new ScanningDocument(
                        "Document 4",
                        "9 files",
                        List.of(
                                new ScanningFile("File 1", "Case_notes_09.tiff", "Ref: REF-009", "2 pages", 2, false),
                                new ScanningFile("File 2", "Case_notes_10.tiff", "Ref: REF-010", "1 page", 1, false)
                        )
                ),
                new ScanningDocument(
                        "Document 5",
                        "11 files",
                        List.of(
                                new ScanningFile("File 1", "Finance_doc_11.tiff", "Ref: REF-011", "2 pages", 2, false),
                                new ScanningFile("File 2", "Finance_doc_12.tiff", "Ref: REF-012", "2 pages", 2, false)
                        )
                )
        );
    }

    public ScanningDocument findScanningDocument(String title) {
        for (ScanningDocument document : scanningDocuments()) {
            if (document.title().equals(title)) {
                return document;
            }
        }
        return scanningDocuments().get(0);
    }
}
