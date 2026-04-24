package easv.gui;

import java.util.List;

public class UserPortalModel {
    private static final String BOX_ID_PATTERN = "^BOX-\\d{4}-\\d{3}$";

    public record BoxItem(String id, String description) {
        @Override
        public String toString() {
            return id;
        }
    }

    public record ProfileSetting(String label, String value) {
    }

    public record ProfileItem(int id, String name, String description, boolean defaultProfile, List<BoxItem> assignedBoxes) {
        @Override
        public String toString() {
            return name;
        }
    }

    public record RecentScanItem(String boxId, String profileName, String status, String startedAt, int pages) {
    }

    public record HistoryItem(String boxId, String profileName, String status, String startedAt, String completedAt, int pages) {
    }

    public record ExportItem(String fileName, String boxId, String profileName, String createdAt, String size, String status) {
    }

    public record ProgressStep(String label, String marker, String stateStyle) {
    }

    public record ScanProgress(String profileName, String boxId, String startedAt, String startedBy, int progressPercent,
                               int pagesScanned, int totalPages, List<ProgressStep> steps) {
    }

    public record ScanSummary(String profileName, String boxId, String startedAt, String completedAt, int totalPages, String status) {
    }

    public record PortalSession(ProfileItem profile, BoxItem box) {
        public String exportName() {
            return profile.name() + "_" + box.id();
        }
    }

    public List<ProfileItem> fetchProfilesForUser() {
        return List.of(
                new ProfileItem(1, "Finance Standard", "Optimized for financial documents and reports.", true, List.of(
                        new BoxItem("BOX-2026-001", "Primary finance intake"),
                        new BoxItem("BOX-2026-014", "Monthly reconciliation batch")
                )),
                new ProfileItem(2, "Legal Standard", "Configured for legal matter packets and supporting paperwork.", false, List.of(
                        new BoxItem("BOX-2026-002", "Assigned legal queue")
                )),
                new ProfileItem(3, "HR Standard", "Balanced settings for onboarding files and employee records.", false, List.of(
                        new BoxItem("BOX-2026-003", "HR intake box")
                ))
        );
    }

    public ProfileItem getDefaultProfileForUser() {
        return fetchProfilesForUser().stream()
                .filter(ProfileItem::defaultProfile)
                .findFirst()
                .orElse(fetchProfilesForUser().get(0));
    }

    public List<RecentScanItem> fetchRecentScans() {
        return List.of(
                new RecentScanItem("BOX-2026-001", "Finance Standard", "Completed", "Apr 24, 2024 10:30 AM", 245),
                new RecentScanItem("BOX-2026-002", "Legal Standard", "In Progress", "Apr 24, 2024 9:45 AM", 125),
                new RecentScanItem("BOX-2026-003", "HR Standard", "Failed", "Apr 23, 2024 4:05 PM", 45)
        );
    }

    public List<HistoryItem> fetchScanHistory() {
        return List.of(
                new HistoryItem("BOX-2026-001", "Finance Standard", "Completed", "Apr 24, 2024 10:30 AM", "Apr 24, 2024 11:05 AM", 245),
                new HistoryItem("BOX-2026-002", "Legal Standard", "In Progress", "Apr 24, 2024 9:45 AM", "-", 125),
                new HistoryItem("BOX-2026-003", "HR Standard", "Failed", "Apr 23, 2024 4:05 PM", "Apr 23, 2024 4:50 PM", 45),
                new HistoryItem("BOX-2026-004", "Finance Standard", "Completed", "Apr 23, 2024 12:15 PM", "Apr 23, 2024 2:45 PM", 310),
                new HistoryItem("BOX-2026-005", "Legal Standard", "Completed", "Apr 22, 2024 11:30 AM", "Apr 22, 2024 11:50 AM", 198)
        );
    }

    public List<ExportItem> fetchExports() {
        return List.of(
                new ExportItem("Finance Standard_BOX-2026-001.pdf", "BOX-2026-001", "Finance Standard", "Apr 24, 2024 11:05 AM", "102.7 MB", "Ready"),
                new ExportItem("Legal Standard_BOX-2026-002.pdf", "BOX-2026-002", "Legal Standard", "Apr 24, 2024 9:45 AM", "96.3 MB", "Ready"),
                new ExportItem("HR Standard_BOX-2026-003.pdf", "BOX-2026-003", "HR Standard", "Apr 23, 2024 5:00 PM", "22.1 MB", "Failed"),
                new ExportItem("Finance Standard_BOX-2026-004.pdf", "BOX-2026-004", "Finance Standard", "Apr 23, 2024 2:15 PM", "-", "Processing")
        );
    }

    public List<ProfileSetting> fetchProfileSettings(ProfileItem profile) {
        if (profile == null) {
            return List.of();
        }
        return switch (profile.name()) {
            case "Legal Standard" -> List.of(
                    new ProfileSetting("Auto-rotate", "3 deg"),
                    new ProfileSetting("Brightness", "+8%"),
                    new ProfileSetting("Remove blank pages", "On"),
                    new ProfileSetting("Output format", "PDF")
            );
            case "HR Standard" -> List.of(
                    new ProfileSetting("Auto-rotate", "2 deg"),
                    new ProfileSetting("Brightness", "+6%"),
                    new ProfileSetting("Remove blank pages", "On"),
                    new ProfileSetting("Output format", "PDF")
            );
            default -> List.of(
                    new ProfileSetting("Auto-rotate", "5 deg"),
                    new ProfileSetting("Brightness", "+10%"),
                    new ProfileSetting("Remove blank pages", "On"),
                    new ProfileSetting("Output format", "PDF")
            );
        };
    }

    public List<String> fetchHelpTopics() {
        return List.of(
                "How to start a new scan",
                "Understanding profiles",
                "Review and export scans",
                "Where are my exports?"
        );
    }

    public String formatExportName(String profileName, String boxId) {
        return profileName + "_" + boxId;
    }

    public boolean isValidBoxId(String boxId) {
        return boxId != null && boxId.trim().matches(BOX_ID_PATTERN);
    }

    public PortalSession startSession(ProfileItem profile, String boxId) {
        String normalizedBoxId = boxId == null || boxId.isBlank() ? "BOX-2026-001" : boxId.trim();
        return new PortalSession(profile, new BoxItem(normalizedBoxId, "Manual scan started from dashboard"));
    }

    public PortalSession resumeSession(RecentScanItem item) {
        return resumeSession(item.boxId(), item.profileName());
    }

    public PortalSession resumeSession(HistoryItem item) {
        return resumeSession(item.boxId(), item.profileName());
    }

    private PortalSession resumeSession(String boxId, String profileName) {
        ProfileItem profile = fetchProfilesForUser().stream()
                .filter(candidate -> candidate.name().equals(profileName))
                .findFirst()
                .orElse(getDefaultProfileForUser());
        return new PortalSession(profile, new BoxItem(boxId, "Resumed from history"));
    }

    public ScanProgress fetchScanProgress(PortalSession session) {
        String boxId = session == null ? "BOX-2026-001" : session.box().id();
        String profileName = session == null ? getDefaultProfileForUser().name() : session.profile().name();
        return new ScanProgress(
                profileName,
                boxId,
                "Apr 24, 2024 10:30 AM",
                "Jane Doe",
                65,
                163,
                250,
                List.of(
                        new ProgressStep("Capturing pages", "OK", "progress-marker-complete"),
                        new ProgressStep("Applying profile settings", "OK", "progress-marker-complete"),
                        new ProgressStep("Analyzing pages", "...", "progress-marker-active"),
                        new ProgressStep("Finalizing", "O", "progress-marker-pending")
                )
        );
    }

    public ScanSummary fetchCompletedSummary(PortalSession session) {
        String boxId = session == null ? "BOX-2026-001" : session.box().id();
        String profileName = session == null ? getDefaultProfileForUser().name() : session.profile().name();
        return new ScanSummary(profileName, boxId, "Apr 24, 2024 10:30 AM", "Apr 24, 2024 11:05 AM", 245, "Completed");
    }
}
