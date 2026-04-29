package easv.gui.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class AdminDemoData {

    private AdminDemoData() {
    }

    static DashboardSummary dashboardSummary() {
        return new DashboardSummary(24, 3, 12, 8);
    }

    static NeedsAttention needsAttention() {
        return new NeedsAttention(3, 2, 1);
    }

    static WorkflowStatus workflowStatus() {
        return new WorkflowStatus(18, 8, 31);
    }

    static List<ManageUsersController.UserRow> users() {
        return List.of(
                new ManageUsersController.UserRow("John Doe", "john", "john@example.com", "Admin", "Active", List.of(), true),
                new ManageUsersController.UserRow("Sarah Smith", "sarah", "sarah@example.com", "User", "Active", List.of("Building Archive", "Technical Drawings", "Standard Scan"), false),
                new ManageUsersController.UserRow("Michael Johnson", "michael", "michael@example.com", "User", "Active", List.of("Building Archive", "Technical Drawings"), false),
                new ManageUsersController.UserRow("Emily Davis", "emily", "emily@example.com", "User", "Active", List.of("Standard Scan"), false),
                new ManageUsersController.UserRow("David Wilson", "david", "david@example.com", "User", "Active", List.of(), false),
                new ManageUsersController.UserRow("Olivia Brown", "olivia", "olivia@example.com", "User", "Active", List.of("Building Archive", "Technical Drawings", "Court Records", "Standard Scan"), false),
                new ManageUsersController.UserRow("Lucas Andersen", "lucas", "lucas@example.com", "Admin", "Active", List.of(), false),
                new ManageUsersController.UserRow("Sofia Nielsen", "sofia", "sofia@example.com", "User", "Active", List.of("Building Archive", "Standard Scan"), false)
        );
    }

    static List<ProfileOption> createUserProfileOptions() {
        return List.of(
                new ProfileOption("Building Archive", "Active"),
                new ProfileOption("Technical Drawings", "Active"),
                new ProfileOption("Court Records", "Draft"),
                new ProfileOption("Standard Scan", "Active")
        );
    }

    static List<ProfilesController.ProfileCardModel> profiles() {
        return List.of(
                new ProfilesController.ProfileCardModel(
                        "Building Archive",
                        "BuildingArchive",
                        "Used for municipal building archive scanning.",
                        "Active",
                        List.of(
                                new ProfilesController.ConfigChip("Barcode Split On", "chip-teal"),
                                new ProfilesController.ConfigChip("Auto Rotate On", "chip-blue"),
                                new ProfilesController.ConfigChip("Deskew", "chip-indigo")
                        ),
                        "Building Archive Metadata",
                        "{profileCode}_{boxId}",
                        5,
                        "Updated today",
                        false,
                        true,
                        "Start new document",
                        "Remove barcode page from final document",
                        "0Â°",
                        "Normal",
                        "Normal",
                        true,
                        "Multi-page TIFF",
                        true
                ),
                new ProfilesController.ProfileCardModel(
                        "Technical Drawings",
                        "TechnicalDrawings",
                        "Used for engineering and drawing archives.",
                        "Active",
                        List.of(
                                new ProfilesController.ConfigChip("Barcode Split On", "chip-teal"),
                                new ProfilesController.ConfigChip("OCR Enabled", "chip-purple"),
                                new ProfilesController.ConfigChip("Deskew", "chip-indigo"),
                                new ProfilesController.ConfigChip("Blank Page Removal", "chip-amber")
                        ),
                        "Technical Drawings Metadata",
                        "{profileCode}_{boxId}",
                        2,
                        "Updated yesterday",
                        false,
                        true,
                        "Start new document",
                        "Remove barcode page from final document",
                        "0Â°",
                        "Normal",
                        "Normal",
                        true,
                        "Multi-page TIFF",
                        true
                ),
                new ProfilesController.ProfileCardModel(
                        "Court Records",
                        "CourtRecords",
                        "Used for legal archive workflows.",
                        "Draft",
                        List.of(
                                new ProfilesController.ConfigChip("Barcode Split Off", "chip-neutral"),
                                new ProfilesController.ConfigChip("OCR Enabled", "chip-purple"),
                                new ProfilesController.ConfigChip("Brightness Correction", "chip-orange")
                        ),
                        "Court Records Metadata",
                        "{profileCode}_{boxId}",
                        1,
                        "Updated 3 days ago",
                        false,
                        false,
                        "Start new document",
                        "Keep barcode page in final document",
                        "0Â°",
                        "Lighter",
                        "Normal",
                        true,
                        "PDF/A",
                        true
                )
        );
    }

    static ProfilesController.ProfileCardModel newProfile() {
        return new ProfilesController.ProfileCardModel(
                "New Profile",
                "NewProfile",
                "Describe this scanning workflow profile.",
                "Draft",
                List.of(
                        new ProfilesController.ConfigChip("Barcode Split Off", "chip-neutral"),
                        new ProfilesController.ConfigChip("Deskew", "chip-indigo")
                ),
                "Building Archive Metadata",
                "{profileCode}_{boxId}",
                0,
                "Created just now",
                false,
                false,
                "Start new document",
                "Remove barcode page from final document",
                "0Â°",
                "Normal",
                "Normal",
                true,
                "Multi-page TIFF",
                true
        );
    }

    static List<ProfilesController.AccessUser> profileAccessUsers() {
        return List.of(
                new ProfilesController.AccessUser("John Doe", "Admin", "Active"),
                new ProfilesController.AccessUser("Sarah Smith", "User", "Active"),
                new ProfilesController.AccessUser("Michael Johnson", "User", "Active"),
                new ProfilesController.AccessUser("Emily Davis", "User", "Active"),
                new ProfilesController.AccessUser("Sofia Nielsen", "User", "Active")
        );
    }

    static List<MetadataController.MetadataTemplateRow> metadataTemplates() {
        return List.of(
                new MetadataController.MetadataTemplateRow(
                        "Building Archive Metadata",
                        "Used for municipal building archive scans.",
                        List.of("Building Archive", "Standard Building Scan", "Aalborg Building Archive"),
                        7,
                        "Active",
                        "Today"
                ),
                new MetadataController.MetadataTemplateRow(
                        "Technical Drawings Metadata",
                        "Used for engineering drawings and technical documentation.",
                        List.of("Technical Drawings", "Engineering Archive"),
                        5,
                        "Active",
                        "Yesterday"
                ),
                new MetadataController.MetadataTemplateRow(
                        "Court Records Metadata",
                        "Registration fields for legal archive documents.",
                        List.of("Court Records"),
                        9,
                        "Draft",
                        "3 days ago"
                ),
                new MetadataController.MetadataTemplateRow(
                        "Client Intake Metadata",
                        "Basic metadata used when creating new archive cases.",
                        List.of("Building Archive"),
                        6,
                        "Active",
                        "1 week ago"
                ),
                new MetadataController.MetadataTemplateRow(
                        "Box Registration Metadata",
                        "Fields used when preparing boxes before scanning.",
                        List.of("Standard Building Scan"),
                        4,
                        "Active",
                        "2 weeks ago"
                ),
                new MetadataController.MetadataTemplateRow(
                        "Legacy Archive Metadata",
                        "Archived template kept for older scanning workflows.",
                        List.of("Court Records"),
                        8,
                        "Archived",
                        "Last month"
                )
        );
    }

    static MetadataController.MetadataTemplateRow newMetadataTemplate() {
        return new MetadataController.MetadataTemplateRow(
                "Create Metadata Template",
                "Create a reusable metadata form for scanning profiles.",
                List.of(),
                0,
                "Draft",
                "New"
        );
    }

    static List<MetadataController.TemplateFieldRow> metadataTemplateFields() {
        return List.of(
                new MetadataController.TemplateFieldRow("Box ID", "Text", true, "Enter box ID"),
                new MetadataController.TemplateFieldRow("Case Number", "Text", true, "Enter case number"),
                new MetadataController.TemplateFieldRow("Building Address", "Text", true, "Enter building address"),
                new MetadataController.TemplateFieldRow("Date Range", "Date", false, "Select date range"),
                new MetadataController.TemplateFieldRow("Department", "Dropdown", false, "Select department"),
                new MetadataController.TemplateFieldRow("Notes", "Long Text", false, "Optional notes")
        );
    }

    static List<MetadataController.AssignedProfileRow> metadataAssignedProfiles() {
        return List.of(
                new MetadataController.AssignedProfileRow("Aalborg Building Archive", "AalborgBuilding_{boxId}", "Active", true, false),
                new MetadataController.AssignedProfileRow("Standard Building Scan", "StandardBuilding_{boxId}", "Active", true, false),
                new MetadataController.AssignedProfileRow("Municipal Archive Scan", "MunicipalArchive_{boxId}", "Active", true, false),
                new MetadataController.AssignedProfileRow("Technical Archive", "TechnicalArchive_{boxId}", "Active", false, false),
                new MetadataController.AssignedProfileRow("Court Records", "CourtRecords_{boxId}", "Draft", false, false),
                new MetadataController.AssignedProfileRow("Old Court Records", "OldCourtRecords_{boxId}", "Archived", false, true)
        );
    }

    static List<MetadataReviewController.MetadataReviewRow> metadataReviewRecords() {
        return List.of(
                new MetadataReviewController.MetadataReviewRow("record-1", "BOX-2026-004 / Case 2026-042 / Document 3", "Aalborg Municipality", "Building Archive", "Building Archive", "Building Archive Metadata", "Missing Required Fields", "Waiting for QA", 12, "Today 10:42", "Sarah Smith", "Sarah Smith", "Today", true),
                new MetadataReviewController.MetadataReviewRow("record-2", "BOX-2026-011 / Case 2026-088 / Document 1", "Maersk Archive", "Technical Archive", "Technical Drawings", "Technical Drawings Metadata", "Invalid", "QA Rejected", 4, "Yesterday 15:20", "John Doe", "John Doe", "Last 7 Days", true),
                new MetadataReviewController.MetadataReviewRow("record-3", "BOX-2026-018 / Case 2026-104 / Document 8", "Copenhagen Airport", "Airport Archive", "Standard Scan", "Standard Box Registration", "Complete", "Ready for QA", 7, "Today 08:15", "Unassigned", "System Import", "Today", false),
                new MetadataReviewController.MetadataReviewRow("record-4", "BOX-2026-022 / Case 2026-120 / Document 2", "Aalborg Municipality", "Building Archive", "Building Archive", "Building Archive Metadata", "Not Started", "Not Started", 16, "Today 11:03", "Sofia Nielsen", "Sofia Nielsen", "Today", false),
                new MetadataReviewController.MetadataReviewRow("record-5", "BOX-2026-025 / Case 2026-131 / Document 5", "Maersk Archive", "Technical Archive", "Technical Drawings", "Technical Drawings Metadata", "Incomplete", "QA In Progress", 9, "Today 12:12", "John Doe", "John Doe", "Today", true),
                new MetadataReviewController.MetadataReviewRow("record-6", "BOX-2026-029 / Case 2026-144 / Document 2", "Copenhagen Airport", "Airport Archive", "Standard Scan", "Standard Box Registration", "Approved", "QA Approved", 6, "Yesterday 09:40", "Sarah Smith", "System Import", "Last 7 Days", false),
                new MetadataReviewController.MetadataReviewRow("record-7", "BOX-2026-033 / Case 2026-155 / Document 9", "Aalborg Municipality", "Building Archive", "Building Archive", "Building Archive Metadata", "Complete", "Ready for QA", 11, "Today 13:05", "Sofia Nielsen", "Sofia Nielsen", "Today", false),
                new MetadataReviewController.MetadataReviewRow("record-8", "BOX-2026-041 / Case 2026-170 / Document 4", "Maersk Archive", "Technical Archive", "Technical Drawings", "Technical Drawings Metadata", "Missing Required Fields", "Waiting for QA", 14, "Today 14:22", "Sarah Smith", "Sarah Smith", "Today", true),
                new MetadataReviewController.MetadataReviewRow("record-9", "BOX-2026-047 / Case 2026-184 / Document 6", "Copenhagen Airport", "Airport Archive", "Standard Scan", "Standard Box Registration", "Not Started", "Not Started", 3, "Yesterday 11:10", "Unassigned", "System Import", "Last 7 Days", false),
                new MetadataReviewController.MetadataReviewRow("record-10", "BOX-2026-052 / Case 2026-199 / Document 1", "Aalborg Municipality", "Building Archive", "Building Archive", "Building Archive Metadata", "Invalid", "QA Rejected", 8, "Today 15:37", "John Doe", "John Doe", "Today", true),
                new MetadataReviewController.MetadataReviewRow("record-11", "BOX-2026-058 / Case 2026-208 / Document 7", "Maersk Archive", "Technical Archive", "Technical Drawings", "Technical Drawings Metadata", "Complete", "QA In Progress", 10, "Yesterday 16:02", "Sarah Smith", "Sarah Smith", "Last 7 Days", false),
                new MetadataReviewController.MetadataReviewRow("record-12", "BOX-2026-063 / Case 2026-216 / Document 3", "Copenhagen Airport", "Airport Archive", "Standard Scan", "Standard Box Registration", "Approved", "QA Approved", 5, "Today 16:55", "Sofia Nielsen", "System Import", "Today", false)
        );
    }

    static List<AssignmentsController.ProfileAccessModel> assignmentProfiles() {
        return List.of(
                new AssignmentsController.ProfileAccessModel(
                        1,
                        "Building Archive",
                        "Used for municipal building archive scans.",
                        "BuildingArchive_{boxId}",
                        "Active"
                ),
                new AssignmentsController.ProfileAccessModel(
                        2,
                        "Technical Drawings",
                        "Used for engineering and drawing archives.",
                        "TechnicalDrawings_{boxId}",
                        "Active"
                ),
                new AssignmentsController.ProfileAccessModel(
                        3,
                        "Court Records",
                        "Used for legal archive workflows.",
                        "CourtRecords_{boxId}",
                        "Draft"
                ),
                new AssignmentsController.ProfileAccessModel(
                        4,
                        "Legacy Archive",
                        "Archived scanning workflow for older cases.",
                        "LegacyArchive_{boxId}",
                        "Archived"
                )
        );
    }

    static List<AssignmentsController.UserAccessModel> assignmentUsers() {
        return List.of(
                new AssignmentsController.UserAccessModel(1, "John Doe", "john@example.com", "Admin", "Active"),
                new AssignmentsController.UserAccessModel(2, "Sarah Smith", "sarah@example.com", "User", "Active"),
                new AssignmentsController.UserAccessModel(3, "Michael Johnson", "michael@example.com", "User", "Active"),
                new AssignmentsController.UserAccessModel(4, "Emily Davis", "emily@example.com", "User", "Active"),
                new AssignmentsController.UserAccessModel(5, "David Wilson", "david@example.com", "User", "Active"),
                new AssignmentsController.UserAccessModel(6, "Olivia Brown", "olivia@example.com", "User", "Active")
        );
    }

    static Map<Integer, Set<Integer>> profileAssignments() {
        return Map.of(
                1, Set.of(1, 2, 3),
                2, Set.of(4, 5),
                3, Set.of(6),
                4, Set.of()
        );
    }

    static List<ActivityController.ActivityLogEntry> activityLogEntries() {
        return List.of(
                activityLogEntry(
                        "a-001",
                        "Today",
                        "10:42",
                        "2026-04-25 10:42",
                        "Scans",
                        "Sarah Smith",
                        "Sarah Smith completed scan",
                        "BOX-2026-004",
                        "Success",
                        "BOX-2026-004 - Building Archive - 42 pages",
                        activityDetail("Profile", "Building Archive"),
                        activityDetail("Pages scanned", "42"),
                        activityDetail("Documents created", "6"),
                        activityDetail("Barcode splits", "5"),
                        activityDetail("Current status", "Waiting for QA")
                ),
                activityLogEntry(
                        "a-002",
                        "Today",
                        "10:31",
                        "2026-04-25 10:31",
                        "Scans",
                        "System",
                        "System detected barcode split",
                        "Document 3",
                        "Success",
                        "Barcode separator detected and document split was applied automatically.",
                        activityDetail("Box", "BOX-2026-004"),
                        activityDetail("Document", "Document 3"),
                        activityDetail("Split rule", "Barcode Split On"),
                        activityDetail("Result", "New document boundary created")
                ),
                activityLogEntry(
                        "a-003",
                        "Today",
                        "09:58",
                        "2026-04-25 09:58",
                        "Access",
                        "Admin",
                        "Admin assigned profile access",
                        "Building Archive",
                        "Success",
                        "Building Archive profile was assigned to Sarah Smith.",
                        activityDetail("Profile", "Building Archive"),
                        activityDetail("Assigned user", "Sarah Smith"),
                        activityDetail("Before", "Sarah Smith did not have access"),
                        activityDetail("After", "Sarah Smith has access")
                ),
                activityLogEntry(
                        "a-004",
                        "Today",
                        "09:44",
                        "2026-04-25 09:44",
                        "Metadata",
                        "Admin",
                        "Admin updated metadata template",
                        "Court Records Metadata",
                        "Success",
                        "Field \"Case Number\" was added to Court Records Metadata.",
                        activityDetail("Template", "Court Records Metadata"),
                        activityDetail("Field added", "Case Number"),
                        activityDetail("Required", "Yes"),
                        activityDetail("Field type", "Text")
                ),
                activityLogEntry(
                        "a-005",
                        "Today",
                        "09:12",
                        "2026-04-25 09:12",
                        "Users",
                        "Admin",
                        "Admin created user",
                        "Sarah Smith",
                        "Success",
                        "A new user account was created for Sarah Smith.",
                        activityDetail("User", "Sarah Smith"),
                        activityDetail("Role", "User"),
                        activityDetail("Status", "Active")
                ),
                activityLogEntry(
                        "a-006",
                        "Today",
                        "08:51",
                        "2026-04-25 08:51",
                        "Security",
                        "System",
                        "Failed login attempt",
                        "david",
                        "Failed",
                        "A failed login attempt was recorded for username \"david\".",
                        activityDetail("Username", "david"),
                        activityDetail("Reason", "Invalid password"),
                        activityDetail("Source", "Login screen")
                ),
                activityLogEntry(
                        "a-007",
                        "Yesterday",
                        "15:12",
                        "2026-04-24 15:12",
                        "Profiles",
                        "Admin",
                        "Admin created profile",
                        "Technical Drawings",
                        "Success",
                        "Technical Drawings scan profile was created.",
                        activityDetail("Profile", "Technical Drawings"),
                        activityDetail("Barcode split", "On"),
                        activityDetail("OCR", "Enabled"),
                        activityDetail("Export naming", "TechnicalDrawings_{boxId}")
                ),
                activityLogEntry(
                        "a-008",
                        "Yesterday",
                        "14:30",
                        "2026-04-24 14:30",
                        "QA",
                        "John Doe",
                        "John Doe completed QA",
                        "BOX-2026-003",
                        "Success",
                        "QA was completed for BOX-2026-003.",
                        activityDetail("Box", "BOX-2026-003"),
                        activityDetail("QA result", "Approved"),
                        activityDetail("Documents reviewed", "4")
                ),
                activityLogEntry(
                        "a-009",
                        "Yesterday",
                        "13:44",
                        "2026-04-24 13:44",
                        "Documents",
                        "John Doe",
                        "Document status changed",
                        "DOC-2026-042",
                        "Info",
                        "Document changed from Waiting for QA to QA Completed.",
                        activityDetail("Document", "DOC-2026-042"),
                        activityDetail("Before", "Waiting for QA"),
                        activityDetail("After", "QA Completed")
                ),
                activityLogEntry(
                        "a-010",
                        "Yesterday",
                        "11:25",
                        "2026-04-24 11:25",
                        "Exports",
                        "System",
                        "System created export",
                        "BuildingArchive_BOX-2026-004",
                        "Success",
                        "Export package was generated successfully.",
                        activityDetail("Export name", "BuildingArchive_BOX-2026-004"),
                        activityDetail("Format", "PDF/A + metadata CSV"),
                        activityDetail("Documents", "6"),
                        activityDetail("Status", "Completed")
                ),
                activityLogEntry(
                        "a-011",
                        "Yesterday",
                        "10:05",
                        "2026-04-24 10:05",
                        "Exports",
                        "System",
                        "Export failed",
                        "CourtRecords_BOX-2026-011",
                        "Failed",
                        "Export failed because required metadata fields were missing.",
                        activityDetail("Export name", "CourtRecords_BOX-2026-011"),
                        activityDetail("Reason", "Missing required metadata"),
                        activityDetail("Missing fields", "Document Type, Registration Date")
                ),
                activityLogEntry(
                        "a-012",
                        "Earlier This Week",
                        "16:20",
                        "2026-04-22 16:20",
                        "Metadata",
                        "Admin",
                        "Admin marked metadata field as required",
                        "Building Archive Metadata",
                        "Warning",
                        "Box ID was marked as required for Building Archive Metadata.",
                        activityDetail("Template", "Building Archive Metadata"),
                        activityDetail("Field", "Box ID"),
                        activityDetail("Before", "Optional"),
                        activityDetail("After", "Required")
                )
        );
    }

    private static ActivityController.ActivityLogEntry activityLogEntry(
            String id,
            String dateGroup,
            String timestamp,
            String fullTimestamp,
            String type,
            String actor,
            String action,
            String target,
            String status,
            String description,
            ActivityController.ActivityDetail... details
    ) {
        return new ActivityController.ActivityLogEntry(
                id,
                dateGroup,
                timestamp,
                fullTimestamp,
                type,
                actor,
                action,
                target,
                status,
                description,
                List.of(details)
        );
    }

    private static ActivityController.ActivityDetail activityDetail(String label, String value) {
        return new ActivityController.ActivityDetail(label, value);
    }

    record DashboardSummary(
            int totalUsers,
            int activeProfiles,
            int scansToday,
            int waitingForQa
    ) {
    }

    record NeedsAttention(
            int usersWithNoProfiles,
            int failedExports,
            int draftProfiles
    ) {
        int total() {
            return usersWithNoProfiles + failedExports + draftProfiles;
        }
    }

    record WorkflowStatus(
            int inProgress,
            int waitingForQa,
            int exported
    ) {
        int total() {
            return inProgress + waitingForQa + exported;
        }
    }

    record ProfileOption(
            String name,
            String status
    ) {
    }
}
