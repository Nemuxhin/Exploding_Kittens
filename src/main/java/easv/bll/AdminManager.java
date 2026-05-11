package easv.bll;

import easv.be.AuditLog;
import easv.be.MetadataField;
import easv.be.ReviewRecord;
import easv.be.MetadataTemplate;
import easv.be.ScanProfile;
import easv.be.User;
import easv.dal.AuditLogDAO;
import easv.dal.MetadataDAO;
import easv.dal.UserDAO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminManager {
    private final UserDAO userDAO;
    private final MetadataDAO metadataDAO;
    private final AuditLogDAO auditLogDAO;

    private final List<User> users = new ArrayList<>();
    private final List<ScanProfile> profiles = new ArrayList<>();
    private final List<MetadataTemplate> metadataTemplates = new ArrayList<>();
    private final List<ReviewRecord> reviewRecords = new ArrayList<>();
    private final List<AuditLog> auditLogs = new ArrayList<>();

    private final Map<Integer, Set<Integer>> profileAssignments = new HashMap<>();

    private int nextUserId = 1;
    private int nextProfileId = 1;
    private int nextMetadataTemplateId = 1;
    private int nextMetadataFieldId = 1;
    private int nextAuditLogId = 1;

    public AdminManager() {
        this(new UserDAO(), new MetadataDAO(), new AuditLogDAO());
    }

    public AdminManager(UserDAO userDAO) {
        this(userDAO, new MetadataDAO(), new AuditLogDAO());
    }

    public AdminManager(UserDAO userDAO, MetadataDAO metadataDAO, AuditLogDAO auditLogDAO) {
        this.userDAO = userDAO == null ? new UserDAO() : userDAO;
        this.metadataDAO = metadataDAO == null ? new MetadataDAO() : metadataDAO;
        this.auditLogDAO = auditLogDAO == null ? new AuditLogDAO() : auditLogDAO;
        loadAdminData();
    }

    public List<User> getUsers() {
        return users.stream()
                .sorted(Comparator.comparingInt(User::getId))
                .toList();
    }

    public User createUser(UserInput input) {
        validateUserInput(input, null);

        User user = new User(
                0,
                input.getName(),
                input.getUsername(),
                input.getEmail(),
                PasswordHasher.hash(clean(input.getPlainPassword())),
                input.getRole(),
                input.getStatus(),
                input.getAssignedProfiles(),
                false
        );

        User savedUser = userDAO.saveUser(user, profileIdsForNames(input.getAssignedProfiles()));
        users.add(savedUser);
        syncProfileAssignmentsForUser(savedUser);
        nextUserId = Math.max(nextUserId, savedUser.getId() + 1);

        addAuditLog("Users", "Created user", savedUser.getName(), "Success",
                "A new user account was created.");

        return savedUser;
    }

    public User updateUser(int userId, UserInput input) {
        User user = findRequiredUser(userId);
        validateUserInput(input, userId);

        String updatedPasswordHash = user.getPasswordHash();

        if (!clean(input.getPlainPassword()).isBlank()) {
            updatedPasswordHash = PasswordHasher.hash(clean(input.getPlainPassword()));
        }

        User updatedUser = new User(
                user.getId(),
                input.getName(),
                input.getUsername(),
                input.getEmail(),
                updatedPasswordHash,
                input.getRole(),
                input.getStatus(),
                input.getAssignedProfiles(),
                user.isCurrentUser()
        );

        User savedUser = userDAO.updateUser(updatedUser, profileIdsForNames(input.getAssignedProfiles()));

        user.setName(savedUser.getName());
        user.setUsername(savedUser.getUsername());
        user.setEmail(savedUser.getEmail());
        user.setPasswordHash(savedUser.getPasswordHash());
        user.setRole(savedUser.getRole());
        user.setStatus(savedUser.getStatus());
        user.setAssignedProfiles(savedUser.getAssignedProfiles());

        syncProfileAssignmentsForUser(user);

        addAuditLog("Users", "Updated user", user.getName(), "Success",
                "User details were updated.");

        return user;
    }

    public void deleteUser(int userId) {
        User user = findRequiredUser(userId);

        if (user.isCurrentUser()) {
            throw new IllegalArgumentException("The current user cannot be deleted.");
        }

        userDAO.deleteUser(userId);
        removeUserFromAssignments(userId);
        users.remove(user);

        addAuditLog("Users", "Deleted user", user.getName(), "Success",
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
                0,
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

        ScanProfile savedProfile = metadataDAO.saveProfile(profile);
        profiles.add(savedProfile);
        nextProfileId = Math.max(nextProfileId, savedProfile.getId() + 1);

        addAuditLog("Profiles", "Created profile", savedProfile.getName(), "Success",
                "A scan profile was created.");

        return savedProfile;
    }

    public ScanProfile updateProfile(int profileId, ProfileInput input) {
        ScanProfile profile = findRequiredProfile(profileId);
        validateProfileInput(input, profileId);

        String previousName = profile.getName();

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

        metadataDAO.updateProfile(profile);
        renameAssignedProfile(previousName, profile.getName());

        addAuditLog("Profiles", "Updated profile", profile.getName(), "Success",
                "A scan profile was updated.");

        return profile;
    }

    public void archiveProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        profile.setArchived(true);
        profile.setStatus("Archived");
        profile.setLastUpdated("Archived just now");

        metadataDAO.updateProfile(profile);

        addAuditLog("Profiles", "Archived profile", profile.getName(), "Success",
                "A scan profile was archived.");
    }

    public void restoreProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        profile.setArchived(false);
        profile.setStatus("Active");
        profile.setLastUpdated("Restored just now");

        metadataDAO.updateProfile(profile);

        addAuditLog("Profiles", "Restored profile", profile.getName(), "Success",
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
                0,
                input.getName(),
                input.getDescription(),
                input.getAssignedProfileNames(),
                withFieldIds(input.getFields()),
                input.getStatus(),
                "Created just now"
        );

        MetadataTemplate savedTemplate = metadataDAO.saveMetadataTemplate(template);
        metadataTemplates.add(savedTemplate);
        nextMetadataTemplateId = Math.max(nextMetadataTemplateId, savedTemplate.getId() + 1);

        addAuditLog("Metadata", "Created metadata template", savedTemplate.getName(), "Success",
                "A metadata template was created.");

        return savedTemplate;
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

        MetadataTemplate savedTemplate = metadataDAO.updateMetadataTemplate(template);
        template.setFields(savedTemplate.getFields());

        addAuditLog("Metadata", "Updated metadata template", template.getName(), "Success",
                "A metadata template was updated.");

        return template;
    }

    public void archiveMetadataTemplate(int templateId) {
        MetadataTemplate template = findRequiredMetadataTemplate(templateId);
        template.setStatus("Archived");
        template.setLastUpdated("Archived just now");

        metadataDAO.updateMetadataTemplate(template);

        addAuditLog("Metadata", "Archived metadata template", template.getName(), "Success",
                "A metadata template was archived.");
    }

    public void restoreMetadataTemplate(int templateId) {
        MetadataTemplate template = findRequiredMetadataTemplate(templateId);
        template.setStatus("Active");
        template.setLastUpdated("Restored just now");

        metadataDAO.updateMetadataTemplate(template);

        addAuditLog("Metadata", "Restored metadata template", template.getName(), "Success",
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

    public List<ReviewRecord> getReviewRecords() {
        return reviewRecords.stream()
                .map(this::copyReviewRecord)
                .toList();
    }

    public ReviewRecord saveReviewRecord(ReviewRecord updatedRecord) {
        if (updatedRecord == null || clean(updatedRecord.getId()).isBlank()) {
            throw new IllegalArgumentException("Review record is required.");
        }

        for (int index = 0; index < reviewRecords.size(); index++) {
            ReviewRecord existingRecord = reviewRecords.get(index);

            if (existingRecord.getId().equals(updatedRecord.getId())) {
                ReviewRecord savedRecord = copyReviewRecord(updatedRecord);
                metadataDAO.saveReviewRecord(savedRecord);
                reviewRecords.set(index, savedRecord);

                addAuditLog("Review", "Updated review", updatedRecord.getIdentity(), "Success",
                        "A review record was updated.");

                return copyReviewRecord(updatedRecord);
            }
        }

        ReviewRecord savedRecord = copyReviewRecord(updatedRecord);
        metadataDAO.saveReviewRecord(savedRecord);
        reviewRecords.add(savedRecord);

        addAuditLog("Review", "Created review", updatedRecord.getIdentity(), "Success",
                "A review record was created.");

        return copyReviewRecord(updatedRecord);
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
        userDAO.replaceProfileAssignments(profileAssignments);
        refreshUsersFromProfileAssignments();

        addAuditLog("Access", "Updated profile access", "Assignments", "Success",
                "Profile access assignments were saved.");
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs.stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .toList();
    }

    public AuditLog addAuditLog(String type, String action, String target,
                                String status, String description) {
        return addAuditLog(type, currentActorName(), action, target, status, description);
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

        AuditLog savedLog = auditLogDAO.saveAuditLog(log);
        auditLogs.add(savedLog);
        nextAuditLogId = Math.max(nextAuditLogId, savedLog.getId() + 1);
        return savedLog;
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

    private ReviewRecord copyReviewRecord(ReviewRecord record) {
        return new ReviewRecord(
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

    private void loadAdminData() {
        loadUsers();
        loadProfiles();
        loadMetadataTemplates();
        loadReviewRecords();
        loadProfileAssignments();
        loadAuditLogs();
        refreshUsersFromProfileAssignments();
    }

    private void loadUsers() {
        users.clear();
        users.addAll(userDAO.getAllUsers());
        nextUserId = userDAO.nextUserId();
    }

    private void loadProfiles() {
        profiles.clear();
        profiles.addAll(metadataDAO.getProfiles());
        nextProfileId = metadataDAO.nextProfileId();
    }

    private void loadMetadataTemplates() {
        metadataTemplates.clear();
        metadataTemplates.addAll(metadataDAO.getMetadataTemplates());
        nextMetadataTemplateId = metadataDAO.nextMetadataTemplateId();
        nextMetadataFieldId = metadataDAO.nextMetadataFieldId();
    }

    private void loadReviewRecords() {
        reviewRecords.clear();
        reviewRecords.addAll(metadataDAO.getReviewRecords());
    }

    private void loadProfileAssignments() {
        profileAssignments.clear();
        profileAssignments.putAll(copyAssignments(userDAO.getProfileAssignments()));
    }

    private void loadAuditLogs() {
        auditLogs.clear();
        auditLogs.addAll(auditLogDAO.getAllAuditLogs());
        nextAuditLogId = auditLogDAO.nextAuditLogId();
    }

    private String currentActorName() {
        User currentUser = UserSession.getCurrentUser();

        if (currentUser == null || clean(currentUser.getName()).isBlank()) {
            return "Admin";
        }

        return currentUser.getName();
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

        if (excludedUserId == null && clean(input.getPlainPassword()).isBlank()) {
            throw new IllegalArgumentException("Temporary password is required.");
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

    private List<Integer> profileIdsForNames(List<String> profileNames) {
        if (profileNames == null || profileNames.isEmpty()) {
            return List.of();
        }

        List<Integer> profileIds = new ArrayList<>();

        for (String profileName : profileNames) {
            if (clean(profileName).isBlank()) {
                continue;
            }

            ScanProfile profile = findProfileByName(profileName)
                    .orElseThrow(() -> new IllegalArgumentException("Profile could not be found: " + profileName));
            profileIds.add(profile.getId());
        }

        return profileIds;
    }

    private void syncProfileAssignmentsForUser(User user) {
        removeUserFromAssignments(user.getId());

        for (String assignedProfileName : user.getAssignedProfiles()) {
            findProfileByName(assignedProfileName).ifPresent(profile ->
                    profileAssignments
                            .computeIfAbsent(profile.getId(), profileId -> new HashSet<>())
                            .add(user.getId())
            );
        }
    }

    private java.util.Optional<ScanProfile> findProfileByName(String profileName) {
        String normalizedProfileName = normalize(profileName);

        return profiles.stream()
                .filter(profile -> normalize(profile.getName()).equals(normalizedProfileName))
                .findFirst();
    }

    private void refreshUsersFromProfileAssignments() {
        for (User user : users) {
            user.setAssignedProfiles(getAssignedProfileNames(user.getId()));
        }
    }

    private List<String> getAssignedProfileNames(int userId) {
        List<String> assignedProfileNames = new ArrayList<>();

        for (ScanProfile profile : getProfiles()) {
            Set<Integer> assignedUserIds = profileAssignments.getOrDefault(profile.getId(), Set.of());

            if (assignedUserIds.contains(userId)) {
                assignedProfileNames.add(profile.getName());
            }
        }

        return assignedProfileNames;
    }

    private void renameAssignedProfile(String previousName, String newName) {
        if (normalize(previousName).equals(normalize(newName))) {
            return;
        }

        for (User user : users) {
            LinkedHashSet<String> updatedProfiles = new LinkedHashSet<>();

            for (String assignedProfile : user.getAssignedProfiles()) {
                if (normalize(assignedProfile).equals(normalize(previousName))) {
                    updatedProfiles.add(newName);
                } else {
                    updatedProfiles.add(assignedProfile);
                }
            }

            user.setAssignedProfiles(new ArrayList<>(updatedProfiles));
        }
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
        private final String plainPassword;
        private final String role;
        private final String status;
        private final List<String> assignedProfiles;

        public UserInput(String name, String username, String email,
                         String role, String status, List<String> assignedProfiles) {
            this(name, username, email, role, status, assignedProfiles, "");
        }

        public UserInput(String name, String username, String email,
                         String role, String status, List<String> assignedProfiles, String plainPassword) {
            this.name = name;
            this.username = username;
            this.email = email;
            this.plainPassword = plainPassword;
            this.role = role;
            this.status = status;
            this.assignedProfiles = assignedProfiles == null ? List.of() : List.copyOf(assignedProfiles);
        }

        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getPlainPassword() { return plainPassword; }
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
