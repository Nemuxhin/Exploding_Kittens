package easv.gui;

import java.util.List;

public class UserPortalModel {
    private static final String BOX_ID_PATTERN = "^BOX-\\d{4}-\\d{3}$";
    private AccountProfile accountProfile = new AccountProfile("John Doe", "john.doe@company.com", "Archives");

    public record DashboardMetric(String label, String value) {
    }

    public record AccountProfile(String fullName, String email, String department) {
    }

    public record ScanProfileInfo(String metadataRequired, String qaRequired, String splittingMethod) {
    }

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

    public record PortalSession(ProfileItem profile, BoxItem box) {
        public String exportName() {
            return profile.name() + "_" + box.id();
        }
    }

    public List<DashboardMetric> fetchDashboardMetrics() {
        return List.of(
                new DashboardMetric("Total Scans", "247"),
                new DashboardMetric("This Month", "23"),
                new DashboardMetric("Pages Scanned", "18.4K"),
                new DashboardMetric("Completed", "94.7%")
        );
    }

    public List<ProfileItem> fetchProfilesForUser() {
        return List.of(
                new ProfileItem(1, "Standard Scan", "Balanced settings for everyday document intake and general office work.", true, List.of(
                        new BoxItem("BOX-2026-042", "Primary intake"),
                        new BoxItem("BOX-2026-041", "Overflow intake")
                )),
                new ProfileItem(2, "High Quality", "Higher resolution profile for detailed pages and image-heavy packets.", false, List.of(
                        new BoxItem("BOX-2026-040", "Detailed review queue")
                )),
                new ProfileItem(3, "Archive", "Optimized for long-term archival exports and retention workflows.", false, List.of(
                        new BoxItem("BOX-2026-039", "Archive staging")
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
                new RecentScanItem("BOX-2026-042", "Standard Scan", "Completed", "2026-04-24 14:45", 125),
                new RecentScanItem("BOX-2026-041", "High Quality", "Processing", "2026-04-24 13:15", 89),
                new RecentScanItem("BOX-2026-040", "Standard Scan", "Completed", "2026-04-23 16:38", 203),
                new RecentScanItem("BOX-2026-039", "Archive", "Failed", "2026-04-23 10:05", 0)
        );
    }

    public List<HistoryItem> fetchScanHistory() {
        return List.of(
                new HistoryItem("BOX-2026-042", "Standard Scan", "Completed", "2026-04-24 14:45", "2026-04-24 15:02", 125),
                new HistoryItem("BOX-2026-041", "High Quality", "Processing", "2026-04-24 13:15", "-", 89),
                new HistoryItem("BOX-2026-040", "Standard Scan", "Completed", "2026-04-23 16:38", "2026-04-23 17:11", 203),
                new HistoryItem("BOX-2026-039", "Archive", "Failed", "2026-04-23 10:05", "2026-04-23 10:22", 0),
                new HistoryItem("BOX-2026-038", "High Quality", "Completed", "2026-04-22 09:40", "2026-04-22 10:18", 176)
        );
    }

    public List<ExportItem> fetchExports() {
        return List.of(
                new ExportItem("standard_scan_BOX-2026-042.pdf", "BOX-2026-042", "Standard Scan", "2026-04-24 15:02", "102.7 MB", "Ready"),
                new ExportItem("high_quality_BOX-2026-041.pdf", "BOX-2026-041", "High Quality", "2026-04-24 13:15", "96.3 MB", "Ready"),
                new ExportItem("archive_BOX-2026-039.pdf", "BOX-2026-039", "Archive", "2026-04-23 10:22", "-", "Failed"),
                new ExportItem("standard_scan_BOX-2026-040.pdf", "BOX-2026-040", "Standard Scan", "2026-04-23 17:11", "145.0 MB", "Processing")
        );
    }

    public List<ProfileSetting> fetchProfileSettings(ProfileItem profile) {
        if (profile == null) {
            return List.of();
        }
        return switch (profile.name()) {
            case "High Quality" -> List.of(
                    new ProfileSetting("Resolution", "600 DPI"),
                    new ProfileSetting("Denoise", "On"),
                    new ProfileSetting("Deskew", "On"),
                    new ProfileSetting("Output format", "PDF/A")
            );
            case "Archive" -> List.of(
                    new ProfileSetting("Resolution", "400 DPI"),
                    new ProfileSetting("Blank page removal", "On"),
                    new ProfileSetting("Retention tag", "Archive"),
                    new ProfileSetting("Output format", "TIFF + PDF")
            );
            default -> List.of(
                    new ProfileSetting("Resolution", "300 DPI"),
                    new ProfileSetting("Auto crop", "On"),
                    new ProfileSetting("Blank page removal", "On"),
                    new ProfileSetting("Output format", "PDF")
            );
        };
    }

    public ScanProfileInfo fetchScanProfileInfo(ProfileItem profile) {
        if (profile == null) {
            return new ScanProfileInfo("—", "—", "—");
        }
        return switch (profile.name()) {
            case "High Quality" -> new ScanProfileInfo("Yes", "No", "Single document");
            case "Archive" -> new ScanProfileInfo("Yes", "Yes", "Manual or barcode");
            default -> new ScanProfileInfo("No", "No", "Manual");
        };
    }

    public AccountProfile fetchAccountProfile() {
        return accountProfile;
    }

    public void updateAccountProfile(String fullName, String email, String department) {
        accountProfile = new AccountProfile(
                normalizedValue(fullName, accountProfile.fullName()),
                normalizedValue(email, accountProfile.email()),
                normalizedValue(department, accountProfile.department())
        );
    }

    public String formatExportName(String profileName, String boxId) {
        return profileName.toLowerCase().replace(' ', '_') + "_" + boxId;
    }

    public boolean isValidBoxId(String boxId) {
        return boxId != null && boxId.trim().matches(BOX_ID_PATTERN);
    }

    public PortalSession startSession(ProfileItem profile, String boxId) {
        String normalizedBoxId = boxId == null || boxId.isBlank() ? "BOX-2026-042" : boxId.trim();
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

    private String normalizedValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
