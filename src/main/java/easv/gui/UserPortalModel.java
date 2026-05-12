package easv.gui;

import easv.be.CaseFile;
import easv.be.Document;
import easv.be.PageImage;
import easv.be.ScanProfile;
import easv.be.User;
import easv.bll.UserSession;
import easv.dal.CaseFileDAO;
import easv.dal.DataAccessException;
import easv.dal.ScanProfileDAO;
import easv.dal.ScanSessionDAO;

import java.util.ArrayList;
import java.util.Comparator;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserPortalModel {
    private static final String BOX_ID_PATTERN = "^BOX-\\d{4}-\\d{3}$";
    private static final AccountProfile DEFAULT_ACCOUNT =
            new AccountProfile("User", "", "Scanning");
    private static final DateTimeFormatter HISTORY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ScanProfileDAO scanProfileDAO;
    private final CaseFileDAO caseFileDAO;
    private final ScanSessionDAO scanSessionDAO;
    private AccountProfile accountProfile = DEFAULT_ACCOUNT;

    public UserPortalModel() {
        this(new ScanProfileDAO(), new CaseFileDAO(), new ScanSessionDAO());
    }

    UserPortalModel(ScanProfileDAO scanProfileDAO, CaseFileDAO caseFileDAO, ScanSessionDAO scanSessionDAO) {
        this.scanProfileDAO = scanProfileDAO;
        this.caseFileDAO = caseFileDAO;
        this.scanSessionDAO = scanSessionDAO;
        syncAccountFromSession();
    }

    public record DashboardMetric(String label, String value) {}
    public record AccountProfile(String fullName, String email, String department) {}
    public record ScanProfileInfo(String metadataRequired, String qaRequired, String splittingMethod) {}
    public record BoxItem(String id, String description) {
        @Override
        public String toString() {
            return id;
        }
    }
    public record ProfileSetting(String label, String value) {}
    public record ProfileItem(int id, String name, String description, boolean defaultProfile, List<BoxItem> assignedBoxes) {
        @Override
        public String toString() {
            return name;
        }
    }
    public record RecentScanItem(String boxId, String profileName, String status, String startedAt, int pages) {}
    public record HistoryItem(String boxId, String profileName, int documents, String status, String startedAt, String completedAt, int pages, String size) {}
    public record ExportItem(String fileName, String boxId, String profileName, int documents, String createdAt, String size, String status) {}
    public record PortalSession(ProfileItem profile, BoxItem box) {
        public String exportName() {
            return profile.name() + "_" + box.id();
        }
    }

    public List<DashboardMetric> fetchDashboardMetrics() {
        syncAccountFromSession();

        List<ProfileItem> profiles = fetchProfilesForUser();
        List<HistoryItem> history = fetchScanHistory();
        int documents = fetchAllCaseFiles().stream()
                .mapToInt(caseFile -> caseFile.getDocuments().size())
                .sum();
        int pages = fetchAllCaseFiles().stream()
                .flatMap(caseFile -> caseFile.getDocuments().stream())
                .mapToInt(document -> document.getPages().size())
                .sum();

        return List.of(
                new DashboardMetric("Assigned Profiles", String.valueOf(profiles.size())),
                new DashboardMetric("Batches", String.valueOf(history.size())),
                new DashboardMetric("Documents", String.valueOf(documents)),
                new DashboardMetric("Pages", String.valueOf(pages))
        );
    }

    public List<ProfileItem> fetchProfilesForUser() {
        syncAccountFromSession();

        User currentUser = UserSession.getCurrentUser();
        List<ScanProfile> availableProfiles = safeProfiles();
        if (availableProfiles.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> allowedNames = new LinkedHashSet<>();
        if (currentUser != null && currentUser.getAssignedProfiles() != null && !currentUser.getAssignedProfiles().isEmpty()) {
            allowedNames.addAll(currentUser.getAssignedProfiles());
        }

        List<ScanProfile> filteredProfiles = allowedNames.isEmpty()
                ? availableProfiles.stream().filter(profile -> !profile.isArchived()).toList()
                : availableProfiles.stream()
                .filter(profile -> allowedNames.stream().anyMatch(assigned -> assigned.equalsIgnoreCase(profile.getName())))
                .toList();

        if (filteredProfiles.isEmpty()) {
            filteredProfiles = availableProfiles.stream().filter(profile -> !profile.isArchived()).toList();
        }

        List<BoxItem> boxes = fetchDistinctBoxes();
        List<ProfileItem> items = new ArrayList<>();
        for (int index = 0; index < filteredProfiles.size(); index++) {
            ScanProfile profile = filteredProfiles.get(index);
            items.add(new ProfileItem(
                    profile.getId(),
                    profile.getName(),
                    profile.getDescription(),
                    index == 0,
                    boxes
            ));
        }
        return items;
    }

    public ProfileItem getDefaultProfileForUser() {
        return fetchProfilesForUser().stream()
                .findFirst()
                .orElse(null);
    }

    public List<RecentScanItem> fetchRecentScans() {
        return fetchScanHistory().stream()
                .limit(4)
                .map(item -> new RecentScanItem(
                        item.boxId(),
                        item.profileName(),
                        item.status(),
                        item.startedAt(),
                        item.pages()
                ))
                .toList();
    }

    public List<HistoryItem> fetchScanHistory() {
        try {
            return scanSessionDAO.findHistorySummaries().stream()
                    .map(summary -> new HistoryItem(
                            summary.boxId(),
                            summary.profileName(),
                            summary.documentCount(),
                            summary.status(),
                            formatHistoryTime(summary.startedAt()),
                            isCompletedStatus(summary.status()) ? formatHistoryTime(summary.startedAt()) : "-",
                            summary.pageCount(),
                            "-"
                    ))
                    .toList();
        } catch (DataAccessException exception) {
            return List.of();
        }
    }

    public List<ExportItem> fetchExports() {
        return fetchScanHistory().stream()
                .filter(item -> isCompletedStatus(item.status()))
                .map(item -> new ExportItem(
                        formatExportName(item.profileName(), item.boxId()) + ".pdf",
                        item.boxId(),
                        item.profileName(),
                        item.documents(),
                        item.completedAt(),
                        item.size(),
                        item.status()
                ))
                .toList();
    }

    public List<ProfileSetting> fetchProfileSettings(ProfileItem profile) {
        ScanProfile scanProfile = findProfile(profile);
        if (scanProfile == null) {
            return List.of();
        }

        return List.of(
                new ProfileSetting("Rotation", emptyAsDash(scanProfile.getDefaultRotation())),
                new ProfileSetting("Brightness", emptyAsDash(scanProfile.getBrightness())),
                new ProfileSetting("Contrast", emptyAsDash(scanProfile.getContrast())),
                new ProfileSetting("Export format", emptyAsDash(scanProfile.getExportFormat())),
                new ProfileSetting("Barcode behavior", emptyAsDash(scanProfile.getBarcodeDetectedBehavior()))
        );
    }

    public ScanProfileInfo fetchScanProfileInfo(ProfileItem profile) {
        ScanProfile scanProfile = findProfile(profile);
        if (scanProfile == null) {
            return new ScanProfileInfo("-", "-", "-");
        }

        String metadataRequired = scanProfile.isMetadataRequiredBeforeExport() ? "Yes" : "No";
        String qaRequired = scanProfile.isMetadataRequiredBeforeExport() ? "Yes" : "No";
        String splittingMethod = scanProfile.isBarcodeSplitting() ? "Barcode" : "Manual";
        return new ScanProfileInfo(metadataRequired, qaRequired, splittingMethod);
    }

    public AccountProfile fetchAccountProfile() {
        syncAccountFromSession();
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
        return profileName.toLowerCase(Locale.ROOT).replace(' ', '_') + "_" + boxId;
    }

    public boolean isValidBoxId(String boxId) {
        return boxId != null && boxId.trim().matches(BOX_ID_PATTERN);
    }

    public PortalSession startSession(ProfileItem profile, String boxId) {
        String normalizedBoxId = boxId == null || boxId.isBlank() ? "BOX-UNSPECIFIED" : boxId.trim();
        String description = "Scanning session";
        return new PortalSession(profile, new BoxItem(normalizedBoxId, description));
    }

    public PortalSession resumeSession(RecentScanItem item) {
        return resumeSession(item.boxId(), item.profileName());
    }

    public PortalSession resumeSession(HistoryItem item) {
        return resumeSession(item.boxId(), item.profileName());
    }

    private PortalSession resumeSession(String boxId, String profileName) {
        ProfileItem profile = fetchProfilesForUser().stream()
                .filter(candidate -> candidate.name().equalsIgnoreCase(profileName))
                .findFirst()
                .orElse(getDefaultProfileForUser());
        return new PortalSession(profile, new BoxItem(boxId, "Resumed from stored history"));
    }

    private void syncAccountFromSession() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String department = currentUser.getAssignedProfiles().isEmpty()
                ? "Scanning"
                : currentUser.getAssignedProfiles().get(0);

        accountProfile = new AccountProfile(
                normalizedValue(currentUser.getName(), DEFAULT_ACCOUNT.fullName()),
                normalizedValue(currentUser.getEmail(), DEFAULT_ACCOUNT.email()),
                normalizedValue(department, DEFAULT_ACCOUNT.department())
        );
    }

    private List<ScanProfile> safeProfiles() {
        try {
            return scanProfileDAO.findAll();
        } catch (DataAccessException exception) {
            return List.of();
        }
    }

    private List<CaseFile> fetchAllCaseFiles() {
        try {
            return new ArrayList<>(caseFileDAO.findAll());
        } catch (DataAccessException exception) {
            return List.of();
        }
    }

    private List<BoxItem> fetchDistinctBoxes() {
        Map<String, BoxItem> boxes = new LinkedHashMap<>();
        for (CaseFile caseFile : fetchAllCaseFiles()) {
            boxes.putIfAbsent(
                    caseFile.getBox().getBoxId(),
                    new BoxItem(caseFile.getBox().getBoxId(), caseFile.getBox().getDescription())
            );
        }
        return new ArrayList<>(boxes.values());
    }

    private ScanProfile findProfile(ProfileItem profile) {
        if (profile == null) {
            return null;
        }
        return safeProfiles().stream()
                .filter(candidate -> candidate.getId() == profile.id() || candidate.getName().equalsIgnoreCase(profile.name()))
                .findFirst()
                .orElse(null);
    }

    private String normalizedValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String emptyAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String formatHistoryTime(java.time.Instant instant) {
        return HISTORY_TIME_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
    }

    private boolean isCompletedStatus(String status) {
        return "Completed".equalsIgnoreCase(status);
    }
}
