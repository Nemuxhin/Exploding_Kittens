package easv.bll;

import easv.be.AuditLog;
import easv.be.MetadataField;
import easv.be.MetadataTemplate;
import easv.be.ScanProfile;
import easv.be.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminManager {
    private final List<User> users = new ArrayList<>();
    private final List<ScanProfile> profiles = new ArrayList<>();
    private final List<MetadataTemplate> metadataTemplates = new ArrayList<>();
    private final List<AuditLog> auditLogs = new ArrayList<>();

    private final Map<Integer, Set<Integer>> profileAssignments = new HashMap<>();

    private int nextUserId = 1;
    private int nextProfileId = 1;
    private int nextMetadataTemplateId = 1;
    private int nextMetadataFieldId = 1;
    private int nextAuditLogId = 1;

    public List<User> getUsers() {
        return users.stream()
                .sorted(Comparator.comparingInt(User::getId))
                .toList();
    }

    public User createUser(UserInput input) {
        validateUserInput(input, null);

        User user = new User(
                nextUserId++,
                input.getName(),
                input.getUsername(),
                input.getEmail(),
                input.getRole(),
                input.getStatus(),
                input.getAssignedProfiles(),
                false
        );

        users.add(user);
        addAuditLog("Users", "Admin", "Created user", user.getName(), "Success",
                "A new user account was created.");

        return user;
    }

    public User updateUser(int userId, UserInput input) {
        User user = findRequiredUser(userId);
        validateUserInput(input, userId);

        user.setName(input.getName());
        user.setUsername(input.getUsername());
        user.setEmail(input.getEmail());
        user.setRole(input.getRole());
        user.setStatus(input.getStatus());
        user.setAssignedProfiles(input.getAssignedProfiles());

        addAuditLog("Users", "Admin", "Updated user", user.getName(), "Success",
                "User details were updated.");

        return user;
    }

    public void deleteUser(int userId) {
        User user = findRequiredUser(userId);

        if (user.isCurrentUser()) {
            throw new IllegalArgumentException("The current user cannot be deleted.");
        }

        users.remove(user);
        removeUserFromAssignments(userId);

        addAuditLog("Users", "Admin", "Deleted user", user.getName(), "Success",
                "A user account was deleted.");
    }

    public boolean usernameExists(String username, Integer excludedUserId) {
        String normalizedUsername = normalize(username);

        if (normalizedUsername.isBlank()) {
            return false;
        }

        return users.stream()
                .filter(user -> excludedUserId == null || user.getId() != excludedUserId)
                .map(User::getUsername)
                .map(this::normalize)
                .anyMatch(existingUsername -> existingUsername.equals(normalizedUsername));
    }

    public List<ScanProfile> getProfiles() {
        return profiles.stream()
                .sorted(Comparator.comparingInt(ScanProfile::getId))
                .toList();
    }

    public List<ScanProfile> getAssignableProfiles() {
        return profiles.stream()
                .filter(profile -> !profile.isArchived())
                .sorted(Comparator.comparing(ScanProfile::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public ScanProfile createProfile(ProfileInput input) {
        validateProfileInput(input, null);

        ScanProfile profile = new ScanProfile(
                nextProfileId++,
                input.getName(),
                input.getCode(),
                input.getDescription(),
                input.getStatus(),
                input.getMetadataTemplateName(),
                input.getExportNaming(),
                "Created just now",
                isArchivedStatus(input.getStatus()),
                input.isBarcodeSplitting(),
                input.getBarcodeDetectedBehavior(),
                input.getBarcodePageBehavior(),
                input.getDefaultRotation(),
                input.getBrightness(),
                input.getContrast(),
                input.isDeskew(),
                input.getExportFormat(),
                input.isMetadataRequiredBeforeExport()
        );

        profiles.add(profile);
        addAuditLog("Profiles", "Admin", "Created profile", profile.getName(), "Success",
                "A scan profile was created.");

        return profile;
    }

    public ScanProfile updateProfile(int profileId, ProfileInput input) {
        ScanProfile profile = findRequiredProfile(profileId);
        validateProfileInput(input, profileId);

        profile.setName(input.getName());
        profile.setCode(input.getCode());
        profile.setDescription(input.getDescription());
        profile.setStatus(input.getStatus());
        profile.setMetadataTemplateName(input.getMetadataTemplateName());
        profile.setExportNaming(input.getExportNaming());
        profile.setLastUpdated("Updated just now");
        profile.setArchived(isArchivedStatus(input.getStatus()));
        profile.setBarcodeSplitting(input.isBarcodeSplitting());
        profile.setBarcodeDetectedBehavior(input.getBarcodeDetectedBehavior());
        profile.setBarcodePageBehavior(input.getBarcodePageBehavior());
        profile.setDefaultRotation(input.getDefaultRotation());
        profile.setBrightness(input.getBrightness());
        profile.setContrast(input.getContrast());
        profile.setDeskew(input.isDeskew());
        profile.setExportFormat(input.getExportFormat());
        profile.setMetadataRequiredBeforeExport(input.isMetadataRequiredBeforeExport());

        addAuditLog("Profiles", "Admin", "Updated profile", profile.getName(), "Success",
                "A scan profile was updated.");

        return profile;
    }

    public void archiveProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        profile.setArchived(true);
        profile.setStatus("Archived");
        profile.setLastUpdated("Archived just now");

        addAuditLog("Profiles", "Admin", "Archived profile", profile.getName(), "Success",
                "A scan profile was archived.");
    }

    public void restoreProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        profile.setArchived(false);
        profile.setStatus("Active");
        profile.setLastUpdated("Restored just now");

        addAuditLog("Profiles", "Admin", "Restored profile", profile.getName(), "Success",
                "A scan profile was restored.");
    }

    public boolean profileCodeExists(String code, Integer excludedProfileId) {
        String normalizedCode = normalize(code);

        if (normalizedCode.isBlank()) {
            return false;
        }

        return profiles.stream()
                .filter(profile -> excludedProfileId == null || profile.getId() != excludedProfileId)
                .map(ScanProfile::getCode)
                .map(this::normalize)
                .anyMatch(existingCode -> existingCode.equals(normalizedCode));
    }

    public List<MetadataTemplate> getMetadataTemplates() {
        return metadataTemplates.stream()
                .sorted(Comparator.comparingInt(MetadataTemplate::getId))
                .toList();
    }

    public MetadataTemplate createMetadataTemplate(MetadataTemplateInput input) {
        validateMetadataTemplateInput(input, null);

        MetadataTemplate template = new MetadataTemplate(
                nextMetadataTemplateId++,
                input.getName(),
                input.getDescription(),
                input.getAssignedProfileNames(),
                withFieldIds(input.getFields()),
                input.getStatus(),
                "Created just now"
        );

        metadataTemplates.add(template);
        addAuditLog("Metadata", "Admin", "Created metadata template", template.getName(), "Success",
                "A metadata template was created.");

        return template;
    }

    public MetadataTemplate updateMetadataTemplate(int templateId, MetadataTemplateInput input) {
        MetadataTemplate template = findRequiredMetadataTemplate(templateId);
        validateMetadataTemplateInput(input, templateId);

        template.setName(input.getName());
        template.setDescription(input.getDescription());
        template.setAssignedProfileNames(input.getAssignedProfileNames());
        template.setFields(withFieldIds(input.getFields()));
        template.setStatus(input.getStatus());
        template.setLastUpdated("Updated just now");

        addAuditLog("Metadata", "Admin", "Updated metadata template", template.getName(), "Success",
                "A metadata template was updated.");

        return template;
    }

    public void archiveMetadataTemplate(int templateId) {
        MetadataTemplate template = findRequiredMetadataTemplate(templateId);
        template.setStatus("Archived");
        template.setLastUpdated("Archived just now");

        addAuditLog("Metadata", "Admin", "Archived metadata template", template.getName(), "Success",
                "A metadata template was archived.");
    }

    public void restoreMetadataTemplate(int templateId) {
        MetadataTemplate template = findRequiredMetadataTemplate(templateId);
        template.setStatus("Active");
        template.setLastUpdated("Restored just now");

        addAuditLog("Metadata", "Admin", "Restored metadata template", template.getName(), "Success",
                "A metadata template was restored.");
    }

    public boolean metadataTemplateNameExists(String name, Integer excludedTemplateId) {
        String normalizedName = normalize(name);

        if (normalizedName.isBlank()) {
            return false;
        }

        return metadataTemplates.stream()
                .filter(template -> excludedTemplateId == null || template.getId() != excludedTemplateId)
                .map(MetadataTemplate::getName)
                .map(this::normalize)
                .anyMatch(existingName -> existingName.equals(normalizedName));
    }

    public Map<Integer, Set<Integer>> getProfileAssignments() {
        return copyAssignments(profileAssignments);
    }

    public Set<Integer> getAssignedUserIds(int profileId) {
        return new HashSet<>(profileAssignments.getOrDefault(profileId, Set.of()));
    }

    public List<Integer> getAssignedProfileIds(int userId) {
        return profileAssignments.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    public void saveProfileAssignments(Map<Integer, Set<Integer>> assignments) {
        profileAssignments.clear();
        profileAssignments.putAll(copyAssignments(assignments));

        addAuditLog("Access", "Admin", "Updated profile access", "Assignments", "Success",
                "Profile access assignments were saved.");
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs.stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .toList();
    }

    public AuditLog addAuditLog(String type, String actor, String action, String target,
                                String status, String description) {
        AuditLog log = new AuditLog(
                nextAuditLogId++,
                LocalDateTime.now(),
                type,
                actor,
                action,
                target,
                status,
                description,
                List.of()
        );

        auditLogs.add(log);
        return log;
    }

    public DashboardSummary getDashboardSummary() {
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

    private User findRequiredUser(int userId) {
        return users.stream()
                .filter(user -> user.getId() == userId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User could not be found."));
    }

    private ScanProfile findRequiredProfile(int profileId) {
        return profiles.stream()
                .filter(profile -> profile.getId() == profileId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile could not be found."));
    }

    private MetadataTemplate findRequiredMetadataTemplate(int templateId) {
        return metadataTemplates.stream()
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

        for (MetadataField field : fields) {
            int fieldId = field.getId() > 0 ? field.getId() : nextMetadataFieldId++;

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

    private void removeUserFromAssignments(int userId) {
        for (Set<Integer> assignedUserIds : profileAssignments.values()) {
            assignedUserIds.remove(userId);
        }
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

    private boolean isArchivedStatus(String status) {
        return "Archived".equalsIgnoreCase(status);
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
        private final String role;
        private final String status;
        private final List<String> assignedProfiles;

        public UserInput(String name, String username, String email,
                         String role, String status, List<String> assignedProfiles) {
            this.name = name;
            this.username = username;
            this.email = email;
            this.role = role;
            this.status = status;
            this.assignedProfiles = assignedProfiles == null ? List.of() : List.copyOf(assignedProfiles);
        }

        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
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
