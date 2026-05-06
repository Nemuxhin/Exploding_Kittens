package easv.bll;

import easv.be.AuditLog;
import easv.be.MetadataField;
import easv.be.MetadataReviewRecord;
import easv.be.MetadataTemplate;
import easv.be.ScanProfile;
import easv.be.User;
import easv.dal.AuditLogDAO;
import easv.dal.DatabaseConnection;
import easv.dal.MetadataDAO;
import easv.dal.ScanProfileDAO;
import easv.dal.UserDAO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminManager {
    private final UserDAO userDAO;
    private final ScanProfileDAO scanProfileDAO;
    private final MetadataDAO metadataDAO;
    private final AuditLogDAO auditLogDAO;

    public AdminManager() {
        this(new DatabaseConnection());
    }

    public AdminManager(DatabaseConnection databaseConnection) {
        this(
                new UserDAO(databaseConnection),
                new ScanProfileDAO(databaseConnection),
                new MetadataDAO(databaseConnection),
                new AuditLogDAO(databaseConnection)
        );
    }

    public AdminManager(UserDAO userDAO, ScanProfileDAO scanProfileDAO, MetadataDAO metadataDAO, AuditLogDAO auditLogDAO) {
        this.userDAO = userDAO;
        this.scanProfileDAO = scanProfileDAO;
        this.metadataDAO = metadataDAO;
        this.auditLogDAO = auditLogDAO;
    }

    public List<User> getUsers() {
        return userDAO.findAll().stream()
                .sorted(Comparator.comparingInt(User::getId))
                .toList();
    }

    public User createUser(UserInput input) {
        validateUserInput(input, null);
        User user = userDAO.create(input);

        addAuditLog("Users", "Admin", "Created user", user.getName(), "Success",
                "A new user account was created.");

        return user;
    }

    public User updateUser(int userId, UserInput input) {
        validateUserInput(input, userId);
        User user = userDAO.update(userId, input);

        addAuditLog("Users", "Admin", "Updated user", user.getName(), "Success",
                "User details were updated.");

        return user;
    }

    public void deleteUser(int userId) {
        User user = findRequiredUser(userId);

        if (user.isCurrentUser()) {
            throw new IllegalArgumentException("The current user cannot be deleted.");
        }

        userDAO.delete(userId);

        addAuditLog("Users", "Admin", "Deleted user", user.getName(), "Success",
                "A user account was deleted.");
    }

    public boolean usernameExists(String username, Integer excludedUserId) {
        String normalizedUsername = normalize(username);
        if (normalizedUsername.isBlank()) {
            return false;
        }
        return userDAO.usernameExists(username, excludedUserId);
    }

    public List<ScanProfile> getProfiles() {
        return scanProfileDAO.findAll().stream()
                .sorted(Comparator.comparingInt(ScanProfile::getId))
                .toList();
    }

    public List<ScanProfile> getAssignableProfiles() {
        return getProfiles().stream()
                .filter(profile -> !profile.isArchived())
                .sorted(Comparator.comparing(ScanProfile::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public ScanProfile createProfile(ProfileInput input) {
        validateProfileInput(input, null);
        ScanProfile profile = scanProfileDAO.create(input);

        addAuditLog("Profiles", "Admin", "Created profile", profile.getName(), "Success",
                "A scan profile was created.");

        return profile;
    }

    public ScanProfile updateProfile(int profileId, ProfileInput input) {
        validateProfileInput(input, profileId);
        ScanProfile profile = scanProfileDAO.update(profileId, input);

        addAuditLog("Profiles", "Admin", "Updated profile", profile.getName(), "Success",
                "A scan profile was updated.");

        return profile;
    }

    public void archiveProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        scanProfileDAO.updateArchivedState(profileId, true, "Archived", "Archived just now");

        addAuditLog("Profiles", "Admin", "Archived profile", profile.getName(), "Success",
                "A scan profile was archived.");
    }

    public void restoreProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        scanProfileDAO.updateArchivedState(profileId, false, "Active", "Restored just now");

        addAuditLog("Profiles", "Admin", "Restored profile", profile.getName(), "Success",
                "A scan profile was restored.");
    }

    public boolean profileCodeExists(String code, Integer excludedProfileId) {
        String normalizedCode = normalize(code);
        if (normalizedCode.isBlank()) {
            return false;
        }
        return scanProfileDAO.codeExists(code, excludedProfileId);
    }

    public List<MetadataTemplate> getMetadataTemplates() {
        return metadataDAO.findAllTemplates().stream()
                .sorted(Comparator.comparingInt(MetadataTemplate::getId))
                .toList();
    }

    public MetadataTemplate createMetadataTemplate(MetadataTemplateInput input) {
        validateMetadataTemplateInput(input, null);
        MetadataTemplate template = metadataDAO.createTemplate(new MetadataTemplateInput(
                input.getName(),
                input.getDescription(),
                input.getAssignedProfileNames(),
                withFieldIds(input.getFields()),
                input.getStatus()
        ));

        addAuditLog("Metadata", "Admin", "Created metadata template", template.getName(), "Success",
                "A metadata template was created.");

        return template;
    }

    public MetadataTemplate updateMetadataTemplate(int templateId, MetadataTemplateInput input) {
        validateMetadataTemplateInput(input, templateId);
        MetadataTemplate template = metadataDAO.updateTemplate(templateId, new MetadataTemplateInput(
                input.getName(),
                input.getDescription(),
                input.getAssignedProfileNames(),
                withFieldIds(input.getFields()),
                input.getStatus()
        ));

        addAuditLog("Metadata", "Admin", "Updated metadata template", template.getName(), "Success",
                "A metadata template was updated.");

        return template;
    }

    public void archiveMetadataTemplate(int templateId) {
        MetadataTemplate template = findRequiredMetadataTemplate(templateId);
        metadataDAO.updateTemplateStatus(templateId, "Archived", "Archived just now");

        addAuditLog("Metadata", "Admin", "Archived metadata template", template.getName(), "Success",
                "A metadata template was archived.");
    }

    public void restoreMetadataTemplate(int templateId) {
        MetadataTemplate template = findRequiredMetadataTemplate(templateId);
        metadataDAO.updateTemplateStatus(templateId, "Active", "Restored just now");

        addAuditLog("Metadata", "Admin", "Restored metadata template", template.getName(), "Success",
                "A metadata template was restored.");
    }

    public boolean metadataTemplateNameExists(String name, Integer excludedTemplateId) {
        String normalizedName = normalize(name);
        if (normalizedName.isBlank()) {
            return false;
        }
        return metadataDAO.templateNameExists(name, excludedTemplateId);
    }

    public List<MetadataReviewRecord> getMetadataReviewRecords() {
        return metadataDAO.findAllReviewRecords().stream()
                .map(this::copyMetadataReviewRecord)
                .toList();
    }

    public MetadataReviewRecord saveMetadataReviewRecord(MetadataReviewRecord updatedRecord) {
        if (updatedRecord == null || clean(updatedRecord.getId()).isBlank()) {
            throw new IllegalArgumentException("Metadata review record is required.");
        }

        boolean existed = getMetadataReviewRecords().stream()
                .anyMatch(record -> record.getId().equals(updatedRecord.getId()));

        MetadataReviewRecord savedRecord = metadataDAO.saveReviewRecord(copyMetadataReviewRecord(updatedRecord));

        addAuditLog("Metadata", "Admin",
                existed ? "Updated metadata review" : "Created metadata review",
                updatedRecord.getIdentity(),
                "Success",
                existed ? "A metadata review record was updated." : "A metadata review record was created.");

        return copyMetadataReviewRecord(savedRecord);
    }

    public Map<Integer, Set<Integer>> getProfileAssignments() {
        return copyAssignments(userDAO.getProfileAssignments());
    }

    public Set<Integer> getAssignedUserIds(int profileId) {
        return new HashSet<>(getProfileAssignments().getOrDefault(profileId, Set.of()));
    }

    public List<Integer> getAssignedProfileIds(int userId) {
        return getProfileAssignments().entrySet().stream()
                .filter(entry -> entry.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    public void saveProfileAssignments(Map<Integer, Set<Integer>> assignments) {
        userDAO.replaceProfileAssignments(assignments == null ? Map.of() : assignments);

        addAuditLog("Access", "Admin", "Updated profile access", "Assignments", "Success",
                "Profile access assignments were saved.");
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogDAO.findAll().stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public AuditLog addAuditLog(String type, String actor, String action, String target,
                                String status, String description) {
        return auditLogDAO.create(type, actor, action, target, status, description);
    }

    public DashboardSummary getDashboardSummary() {
        List<User> users = getUsers();
        List<ScanProfile> profiles = getProfiles();
        List<AuditLog> auditLogs = getAuditLogs();

        int totalUsers = users.size();

        int activeProfiles = (int) profiles.stream()
                .filter(profile -> !profile.isArchived())
                .filter(profile -> "Active".equalsIgnoreCase(profile.getStatus()))
                .count();

        int draftProfiles = (int) profiles.stream()
                .filter(profile -> "Draft".equalsIgnoreCase(profile.getStatus()))
                .count();

        int usersWithoutProfiles = (int) users.stream()
                .filter(user -> user.getAssignedProfiles().isEmpty())
                .filter(user -> !"Admin".equalsIgnoreCase(user.getRole()))
                .count();

        int failedEvents = (int) auditLogs.stream()
                .filter(log -> "Failed".equalsIgnoreCase(log.getStatus()))
                .count();

        return new DashboardSummary(totalUsers, activeProfiles, draftProfiles, usersWithoutProfiles, failedEvents);
    }

    private MetadataReviewRecord copyMetadataReviewRecord(MetadataReviewRecord record) {
        return new MetadataReviewRecord(
                record.getId(),
                record.getIdentity(),
                record.getClient(),
                record.getArchive(),
                record.getProfile(),
                record.getMetadataTemplate(),
                record.getMetadataStatus(),
                record.getQaStatus(),
                record.getPages(),
                record.getLastUpdated(),
                record.getAssignedTo(),
                record.getScannedBy(),
                record.getDateGroup(),
                record.hasWarning()
        );
    }

    private User findRequiredUser(int userId) {
        return getUsers().stream()
                .filter(user -> user.getId() == userId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User could not be found."));
    }

    private ScanProfile findRequiredProfile(int profileId) {
        return getProfiles().stream()
                .filter(profile -> profile.getId() == profileId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile could not be found."));
    }

    private MetadataTemplate findRequiredMetadataTemplate(int templateId) {
        return getMetadataTemplates().stream()
                .filter(template -> template.getId() == templateId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Metadata template could not be found."));
    }

    private void validateUserInput(UserInput input, Integer excludedUserId) {
        if (input == null) {
            throw new IllegalArgumentException("User details are required.");
        }

        if (clean(input.getName()).isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }

        if (clean(input.getUsername()).isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (usernameExists(input.getUsername(), excludedUserId)) {
            throw new IllegalArgumentException("Username must be unique.");
        }

        if (!clean(input.getEmail()).isBlank() && !isValidEmail(input.getEmail())) {
            throw new IllegalArgumentException("Email must be valid if entered.");
        }

        if (clean(input.getRole()).isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }

        if (clean(input.getStatus()).isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
    }

    private void validateProfileInput(ProfileInput input, Integer excludedProfileId) {
        if (input == null) {
            throw new IllegalArgumentException("Profile details are required.");
        }

        if (clean(input.getName()).isBlank()) {
            throw new IllegalArgumentException("Profile name is required.");
        }

        if (clean(input.getCode()).isBlank()) {
            throw new IllegalArgumentException("Profile code is required.");
        }

        if (profileCodeExists(input.getCode(), excludedProfileId)) {
            throw new IllegalArgumentException("Profile code must be unique.");
        }

        if (clean(input.getStatus()).isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
    }

    private void validateMetadataTemplateInput(MetadataTemplateInput input, Integer excludedTemplateId) {
        if (input == null) {
            throw new IllegalArgumentException("Metadata template details are required.");
        }

        if (clean(input.getName()).isBlank()) {
            throw new IllegalArgumentException("Template name is required.");
        }

        if (metadataTemplateNameExists(input.getName(), excludedTemplateId)) {
            throw new IllegalArgumentException("Template name must be unique.");
        }

        if (clean(input.getStatus()).isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
    }

    private List<MetadataField> withFieldIds(List<MetadataField> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }

        List<MetadataField> fieldsWithIds = new ArrayList<>();
        int fallbackId = 1;

        for (MetadataField field : fields) {
            int fieldId = field.getId() > 0 ? field.getId() : fallbackId++;
            fieldsWithIds.add(new MetadataField(
                    fieldId,
                    field.getName(),
                    field.getType(),
                    field.isRequired(),
                    field.getPlaceholder()
            ));
        }

        return fieldsWithIds;
    }

    private Map<Integer, Set<Integer>> copyAssignments(Map<Integer, Set<Integer>> source) {
        Map<Integer, Set<Integer>> copy = new HashMap<>();

        if (source == null) {
            return copy;
        }

        source.forEach((profileId, userIds) ->
                copy.put(profileId, userIds == null ? new HashSet<>() : new HashSet<>(userIds))
        );

        return copy;
    }

    private boolean isValidEmail(String email) {
        String cleanedEmail = clean(email);
        int atIndex = cleanedEmail.indexOf("@");
        int dotIndex = cleanedEmail.lastIndexOf(".");

        return atIndex > 0
                && dotIndex > atIndex + 1
                && dotIndex < cleanedEmail.length() - 1;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    public static class UserInput {
        private final String name;
        private final String username;
        private final String email;
        private final String passwordHash;
        private final String role;
        private final String status;
        private final List<String> assignedProfiles;

        public UserInput(String name, String username, String email,
                         String role, String status, List<String> assignedProfiles) {
            this(name, username, email, "", role, status, assignedProfiles);
        }

        public UserInput(String name, String username, String email, String passwordHash,
                         String role, String status, List<String> assignedProfiles) {
            this.name = name;
            this.username = username;
            this.email = email;
            this.passwordHash = passwordHash;
            this.role = role;
            this.status = status;
            this.assignedProfiles = assignedProfiles == null ? List.of() : List.copyOf(assignedProfiles);
        }

        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getPasswordHash() { return passwordHash; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
        public List<String> getAssignedProfiles() { return assignedProfiles; }
    }

    public static class ProfileInput {
        private final String name;
        private final String code;
        private final String description;
        private final String status;
        private final String metadataTemplateName;
        private final String exportNaming;
        private final boolean barcodeSplitting;
        private final String barcodeDetectedBehavior;
        private final String barcodePageBehavior;
        private final String defaultRotation;
        private final String brightness;
        private final String contrast;
        private final boolean deskew;
        private final String exportFormat;
        private final boolean metadataRequiredBeforeExport;

        public ProfileInput(
                String name,
                String code,
                String description,
                String status,
                String metadataTemplateName,
                String exportNaming,
                boolean barcodeSplitting,
                String barcodeDetectedBehavior,
                String barcodePageBehavior,
                String defaultRotation,
                String brightness,
                String contrast,
                boolean deskew,
                String exportFormat,
                boolean metadataRequiredBeforeExport
        ) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.status = status;
            this.metadataTemplateName = metadataTemplateName;
            this.exportNaming = exportNaming;
            this.barcodeSplitting = barcodeSplitting;
            this.barcodeDetectedBehavior = barcodeDetectedBehavior;
            this.barcodePageBehavior = barcodePageBehavior;
            this.defaultRotation = defaultRotation;
            this.brightness = brightness;
            this.contrast = contrast;
            this.deskew = deskew;
            this.exportFormat = exportFormat;
            this.metadataRequiredBeforeExport = metadataRequiredBeforeExport;
        }

        public String getName() { return name; }
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
        public String getMetadataTemplateName() { return metadataTemplateName; }
        public String getExportNaming() { return exportNaming; }
        public boolean isBarcodeSplitting() { return barcodeSplitting; }
        public String getBarcodeDetectedBehavior() { return barcodeDetectedBehavior; }
        public String getBarcodePageBehavior() { return barcodePageBehavior; }
        public String getDefaultRotation() { return defaultRotation; }
        public String getBrightness() { return brightness; }
        public String getContrast() { return contrast; }
        public boolean isDeskew() { return deskew; }
        public String getExportFormat() { return exportFormat; }
        public boolean isMetadataRequiredBeforeExport() { return metadataRequiredBeforeExport; }
    }

    public static class MetadataTemplateInput {
        private final String name;
        private final String description;
        private final List<String> assignedProfileNames;
        private final List<MetadataField> fields;
        private final String status;

        public MetadataTemplateInput(
                String name,
                String description,
                List<String> assignedProfileNames,
                List<MetadataField> fields,
                String status
        ) {
            this.name = name;
            this.description = description;
            this.assignedProfileNames = assignedProfileNames == null ? List.of() : List.copyOf(assignedProfileNames);
            this.fields = fields == null ? List.of() : List.copyOf(fields);
            this.status = status;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<String> getAssignedProfileNames() { return assignedProfileNames; }
        public List<MetadataField> getFields() { return fields; }
        public String getStatus() { return status; }
    }

    public static class DashboardSummary {
        private final int totalUsers;
        private final int activeProfiles;
        private final int draftProfiles;
        private final int usersWithoutProfiles;
        private final int failedEvents;

        public DashboardSummary(int totalUsers, int activeProfiles, int draftProfiles,
                                int usersWithoutProfiles, int failedEvents) {
            this.totalUsers = totalUsers;
            this.activeProfiles = activeProfiles;
            this.draftProfiles = draftProfiles;
            this.usersWithoutProfiles = usersWithoutProfiles;
            this.failedEvents = failedEvents;
        }

        public int getTotalUsers() { return totalUsers; }
        public int getActiveProfiles() { return activeProfiles; }
        public int getDraftProfiles() { return draftProfiles; }
        public int getUsersWithoutProfiles() { return usersWithoutProfiles; }
        public int getFailedEvents() { return failedEvents; }
    }
}
