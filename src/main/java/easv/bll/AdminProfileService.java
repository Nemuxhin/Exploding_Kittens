package easv.bll;

import easv.be.ScanProfile;
import easv.be.User;
import easv.dal.MetadataDAO;
import easv.util.Strings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AdminProfileService {
    private final MetadataDAO metadataDAO;
    private final List<ScanProfile> profiles;
    private final List<User> users;
    private final Map<Integer, Set<Integer>> profileAssignments;

    AdminProfileService(
            MetadataDAO metadataDAO,
            List<ScanProfile> profiles,
            List<User> users,
            Map<Integer, Set<Integer>> profileAssignments
    ) {
        this.metadataDAO = metadataDAO;
        this.profiles = profiles;
        this.users = users;
        this.profileAssignments = profileAssignments;
    }

    List<ScanProfile> getProfiles() {
        return profiles.stream()
                .sorted(Comparator.comparingInt(ScanProfile::getId))
                .toList();
    }

    List<ScanProfile> getAssignableProfiles() {
        return profiles.stream()
                .filter(profile -> !profile.isArchived())
                .sorted(Comparator.comparing(ScanProfile::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    ScanProfile createProfile(AdminManager.ProfileInput input) {
        ScanProfile profile = new ScanProfile(
                0,
                input.getName(),
                input.getClient(),
                input.getCode(),
                input.getDescription(),
                input.getStatus(),
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
        return savedProfile;
    }

    ProfileUpdateResult updateProfile(int profileId, AdminManager.ProfileInput input) {
        ScanProfile profile = findRequiredProfile(profileId);
        ScanProfile previousProfile = copyProfile(profile);
        String previousName = profile.getName();

        profile.setName(input.getName());
        profile.setClient(input.getClient());
        profile.setCode(input.getCode());
        profile.setDescription(input.getDescription());
        profile.setStatus(input.getStatus());
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
        return new ProfileUpdateResult(profile, previousProfile);
    }

    ProfileStatusResult archiveProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        String previousStatus = profile.getStatus();
        profile.setArchived(true);
        profile.setStatus("Archived");
        profile.setLastUpdated("Archived just now");
        metadataDAO.updateProfile(profile);
        return new ProfileStatusResult(profile, previousStatus);
    }

    ProfileStatusResult restoreProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        String previousStatus = profile.getStatus();
        profile.setArchived(false);
        profile.setStatus("Active");
        profile.setLastUpdated("Restored just now");
        metadataDAO.updateProfile(profile);
        return new ProfileStatusResult(profile, previousStatus);
    }

    ScanProfile deleteProfile(int profileId) {
        ScanProfile profile = findRequiredProfile(profileId);
        String normalizedDeletedProfileName = Strings.normalize(profile.getName());

        metadataDAO.deleteProfile(profileId);

        profiles.removeIf(storedProfile -> storedProfile.getId() == profileId);
        profileAssignments.remove(profileId);

        users.forEach(user -> user.setAssignedProfiles(
                user.getAssignedProfiles().stream()
                        .filter(profileName -> !Strings.normalize(profileName).equals(normalizedDeletedProfileName))
                        .toList()
        ));

        return profile;
    }

    boolean profileCodeExists(String code, Integer excludedProfileId) {
        String normalizedCode = Strings.normalize(code);

        if (normalizedCode.isBlank()) {
            return false;
        }

        return profiles.stream()
                .filter(profile -> excludedProfileId == null || profile.getId() != excludedProfileId)
                .map(ScanProfile::getCode)
                .map(Strings::normalize)
                .anyMatch(existingCode -> existingCode.equals(normalizedCode));
    }

    ScanProfile findProfileByName(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return null;
        }
        String normalized = profileName.trim();
        return profiles.stream()
                .filter(profile -> profile.getName().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    Set<Integer> getAssignedUserIds(int profileId) {
        return new HashSet<>(profileAssignments.getOrDefault(profileId, Set.of()));
    }

    List<Integer> getAssignedProfileIds(int userId) {
        return profileAssignments.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    List<Integer> profileIdsForNames(List<String> profileNames) {
        if (profileNames == null || profileNames.isEmpty()) {
            return List.of();
        }

        List<Integer> profileIds = new ArrayList<>();

        for (String profileName : profileNames) {
            if (Strings.clean(profileName).isBlank()) {
                continue;
            }

            ScanProfile profile = findProfileOptionalByName(profileName)
                    .orElseThrow(() -> new IllegalArgumentException("Profile could not be found: " + profileName));
            profileIds.add(profile.getId());
        }

        return profileIds;
    }

    void syncProfileAssignmentsForUser(User user) {
        removeUserFromAssignments(user.getId());

        for (String assignedProfileName : user.getAssignedProfiles()) {
            findProfileOptionalByName(assignedProfileName).ifPresent(profile ->
                    profileAssignments
                            .computeIfAbsent(profile.getId(), profileId -> new HashSet<>())
                            .add(user.getId())
            );
        }
    }

    void refreshUsersFromProfileAssignments() {
        for (User user : users) {
            user.setAssignedProfiles(getAssignedProfileNames(user.getId()));
        }
    }

    void removeUserFromAssignments(int userId) {
        for (Set<Integer> assignedUserIds : profileAssignments.values()) {
            assignedUserIds.remove(userId);
        }
    }

    void loadProfiles() {
        profiles.clear();
        profiles.addAll(metadataDAO.getProfiles());
    }

    Map<Integer, Set<Integer>> copyAssignments(Map<Integer, Set<Integer>> source) {
        Map<Integer, Set<Integer>> copy = new HashMap<>();

        if (source == null) {
            return copy;
        }

        source.forEach((profileId, userIds) ->
                copy.put(profileId, userIds == null ? new HashSet<>() : new HashSet<>(userIds))
        );

        return copy;
    }

    private ScanProfile findRequiredProfile(int profileId) {
        return profiles.stream()
                .filter(profile -> profile.getId() == profileId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile could not be found."));
    }

    private ScanProfile copyProfile(ScanProfile profile) {
        return new ScanProfile(
                profile.getId(),
                profile.getName(),
                profile.getClient(),
                profile.getCode(),
                profile.getDescription(),
                profile.getStatus(),
                profile.getExportNaming(),
                profile.getLastUpdated(),
                profile.isArchived(),
                profile.isBarcodeSplitting(),
                profile.getBarcodeDetectedBehavior(),
                profile.getBarcodePageBehavior(),
                profile.getDefaultRotation(),
                profile.getBrightness(),
                profile.getContrast(),
                profile.isDeskew(),
                profile.getExportFormat(),
                profile.isMetadataRequiredBeforeExport()
        );
    }

    private java.util.Optional<ScanProfile> findProfileOptionalByName(String profileName) {
        String normalizedProfileName = Strings.normalize(profileName);

        return profiles.stream()
                .filter(profile -> Strings.normalize(profile.getName()).equals(normalizedProfileName))
                .findFirst();
    }

    private void renameAssignedProfile(String previousName, String newName) {
        if (Strings.normalize(previousName).equals(Strings.normalize(newName))) {
            return;
        }

        for (User user : users) {
            LinkedHashSet<String> updatedProfiles = new LinkedHashSet<>();

            for (String assignedProfile : user.getAssignedProfiles()) {
                if (Strings.normalize(assignedProfile).equals(Strings.normalize(previousName))) {
                    updatedProfiles.add(newName);
                } else {
                    updatedProfiles.add(assignedProfile);
                }
            }

            user.setAssignedProfiles(new ArrayList<>(updatedProfiles));
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

    private boolean isArchivedStatus(String status) {
        return "Archived".equalsIgnoreCase(status);
    }

    record ProfileUpdateResult(ScanProfile profile, ScanProfile previousProfile) {
    }

    record ProfileStatusResult(ScanProfile profile, String previousStatus) {
    }
}
