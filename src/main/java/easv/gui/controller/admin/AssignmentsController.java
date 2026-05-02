package easv.gui.controller.admin;

import easv.be.ScanProfile;
import easv.be.User;
import easv.bll.AdminManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AssignmentsController {

    private static final String ALL_STATUSES = "All Statuses";
    private static final String ALL_ROLES = "All Roles";

    private static final String ACTIVE_MODE_CLASS = "assignment-mode-button-active";
    private static final String ACTIVE_LIST_ITEM_CLASS = "assignment-list-item-active";

    private AssignmentMode mode = AssignmentMode.BY_PROFILE;

    private ScanProfile selectedProfile;
    private User selectedUser;

    private final ObservableList<ScanProfile> profiles = FXCollections.observableArrayList();
    private final ObservableList<User> users = FXCollections.observableArrayList();

    private final Map<Integer, Set<Integer>> savedAssignments = new HashMap<>();
    private final Map<Integer, Set<Integer>> workingAssignments = new HashMap<>();

    private AdminManager adminManager = new AdminManager();

    @FXML private Button byProfileButton;
    @FXML private Button byUserButton;

    @FXML private Label leftPanelTitleLabel;
    @FXML private Label rightPanelTitleLabel;

    @FXML private TextField leftSearchField;
    @FXML private TextField rightSearchField;

    @FXML private ComboBox<String> leftFilterComboBox;
    @FXML private ComboBox<String> rightFilterComboBox;

    @FXML private ScrollPane leftListScrollPane;
    @FXML private ScrollPane assignmentRowsScrollPane;

    @FXML private VBox leftListContainer;
    @FXML private VBox assignmentRowsContainer;

    @FXML private Label selectedTitleLabel;
    @FXML private Label selectedSubtitleLabel;
    @FXML private Label selectedCodeLabel;
    @FXML private Label selectedStatusBadge;

    @FXML private Label changesLabel;

    void setAdminManager(AdminManager adminManager) {
        this.adminManager = adminManager == null ? new AdminManager() : adminManager;
        loadDataFromManager();
        configureFiltersForMode();
        renderPage();
    }

    @FXML
    private void initialize() {
        loadDataFromManager();
        configureListeners();
        configureFiltersForMode();
        renderPage();
    }

    @FXML
    private void selectByProfileMode() {
        switchToMode(AssignmentMode.BY_PROFILE);
    }

    @FXML
    private void selectByUserMode() {
        switchToMode(AssignmentMode.BY_USER);
    }

    @FXML
    private void saveChanges() {
        adminManager.saveProfileAssignments(workingAssignments);
        loadDataFromManager();
        renderPage();
    }

    @FXML
    private void cancelChanges() {
        resetWorkingAssignments();
        renderPage();
    }

    private void switchToMode(AssignmentMode newMode) {
        mode = newMode;

        leftSearchField.clear();
        rightSearchField.clear();

        configureFiltersForMode();
        ensureSelectionExists();
        renderPage();
    }

    private void configureListeners() {
        leftSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            renderLeftList();
            resetLeftScroll();
        });

        rightSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            renderAssignmentRows();
            resetRightScroll();
        });

        leftFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            renderLeftList();
            resetLeftScroll();
        });

        rightFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            renderAssignmentRows();
            resetRightScroll();
        });
    }

    private void loadDataFromManager() {
        int selectedProfileId = selectedProfile == null ? -1 : selectedProfile.getId();
        int selectedUserId = selectedUser == null ? -1 : selectedUser.getId();

        profiles.setAll(adminManager.getProfiles());
        users.setAll(adminManager.getUsers());

        copyAssignments(adminManager.getProfileAssignments(), savedAssignments);
        resetWorkingAssignments();

        selectedProfile = findProfileById(selectedProfileId)
                .orElseGet(() -> profiles.isEmpty() ? null : profiles.get(0));

        selectedUser = findUserById(selectedUserId)
                .orElseGet(() -> users.isEmpty() ? null : users.get(0));
    }

    private void ensureSelectionExists() {
        if (selectedProfile == null && !profiles.isEmpty()) {
            selectedProfile = profiles.get(0);
        }

        if (selectedUser == null && !users.isEmpty()) {
            selectedUser = users.get(0);
        }
    }

    private void configureFiltersForMode() {
        if (mode == AssignmentMode.BY_PROFILE) {
            leftFilterComboBox.getItems().setAll(ALL_STATUSES, "Active", "Draft", "Archived");
            leftFilterComboBox.setValue(ALL_STATUSES);

            rightFilterComboBox.getItems().setAll(ALL_ROLES, "Admin", "User");
            rightFilterComboBox.setValue(ALL_ROLES);
            return;
        }

        leftFilterComboBox.getItems().setAll(ALL_ROLES, "Admin", "User");
        leftFilterComboBox.setValue(ALL_ROLES);

        rightFilterComboBox.getItems().setAll(ALL_STATUSES, "Active", "Draft", "Archived");
        rightFilterComboBox.setValue(ALL_STATUSES);
    }

    private void renderPage() {
        updateModeButtons();
        updatePageText();
        renderLeftList();
        renderSelectedDetails();
        renderAssignmentRows();
        updateChangesLabel();
        resetScrollPositions();
    }

    private void updateModeButtons() {
        setActiveMode(byProfileButton, mode == AssignmentMode.BY_PROFILE);
        setActiveMode(byUserButton, mode == AssignmentMode.BY_USER);
    }

    private void setActiveMode(Button button, boolean active) {
        button.getStyleClass().remove(ACTIVE_MODE_CLASS);

        if (active) {
            button.getStyleClass().add(ACTIVE_MODE_CLASS);
        }
    }

    private void updatePageText() {
        if (mode == AssignmentMode.BY_PROFILE) {
            leftPanelTitleLabel.setText("Profiles");
            rightPanelTitleLabel.setText("Assigned Users");
            leftSearchField.setPromptText("Search profiles...");
            rightSearchField.setPromptText("Search users...");
            return;
        }

        leftPanelTitleLabel.setText("Users");
        rightPanelTitleLabel.setText("Assigned Profiles");
        leftSearchField.setPromptText("Search users...");
        rightSearchField.setPromptText("Search profiles...");
    }

    private void renderLeftList() {
        if (mode == AssignmentMode.BY_PROFILE) {
            renderProfileList();
        } else {
            renderUserList();
        }
    }

    private void renderProfileList() {
        String searchText = normalize(leftSearchField.getText());
        String selectedStatus = leftFilterComboBox.getValue();

        leftListContainer.getChildren().setAll(
                profiles.stream()
                        .filter(profile -> matchesProfileSearch(profile, searchText))
                        .filter(profile -> matchesStatus(displayProfileStatus(profile), selectedStatus))
                        .map(this::buildProfileListItem)
                        .toList()
        );
    }

    private void renderUserList() {
        String searchText = normalize(leftSearchField.getText());
        String selectedRole = leftFilterComboBox.getValue();

        leftListContainer.getChildren().setAll(
                users.stream()
                        .filter(user -> matchesUserSearch(user, searchText))
                        .filter(user -> matchesRole(user.getRole(), selectedRole))
                        .map(this::buildUserListItem)
                        .toList()
        );
    }

    private HBox buildProfileListItem(ScanProfile profile) {
        HBox item = createListItem(isSelectedProfile(profile));

        VBox textBox = new VBox(6);
        textBox.getChildren().addAll(
                createLabel(profile.getName(), "assignment-list-title"),
                createLabel(formatAssignedUsers(getAssignedUserIds(profile.getId()).size()), "assignment-list-subtitle"),
                createLabel("Export: " + displayText(profile.getExportNaming(), "{profileCode}_{boxId}"), "assignment-list-subtitle")
        );

        item.getChildren().addAll(
                textBox,
                createSpacer(),
                createStatusBadge(displayProfileStatus(profile))
        );

        Runnable selectProfile = () -> {
            selectedProfile = profile;
            renderSelectedDetails();
            renderAssignmentRows();
            renderLeftList();
            updateChangesLabel();
            resetRightScroll();
        };

        item.setOnMouseClicked(event -> selectProfile.run());
        AdminKeyboard.makeActivatable(item, "Select profile " + profile.getName(), selectProfile);

        return item;
    }

    private HBox buildUserListItem(User user) {
        HBox item = createListItem(isSelectedUser(user));

        VBox textBox = new VBox(6);
        textBox.getChildren().addAll(
                createLabel(user.getName(), "assignment-list-title"),
                createLabel(displayText(user.getEmail(), "No email"), "assignment-list-subtitle"),
                createLabel(formatAssignedProfiles(getAssignedProfileIds(user.getId()).size()), "assignment-list-subtitle")
        );

        item.getChildren().addAll(
                textBox,
                createSpacer(),
                createRoleBadge(user.getRole())
        );

        Runnable selectUser = () -> {
            selectedUser = user;
            renderSelectedDetails();
            renderAssignmentRows();
            renderLeftList();
            updateChangesLabel();
            resetRightScroll();
        };

        item.setOnMouseClicked(event -> selectUser.run());
        AdminKeyboard.makeActivatable(item, "Select user " + user.getName(), selectUser);

        return item;
    }

    private HBox createListItem(boolean active) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("assignment-list-item");

        if (active) {
            item.getStyleClass().add(ACTIVE_LIST_ITEM_CLASS);
        }

        return item;
    }

    private void renderSelectedDetails() {
        if (mode == AssignmentMode.BY_PROFILE) {
            renderSelectedProfileDetails();
        } else {
            renderSelectedUserDetails();
        }
    }

    private void renderSelectedProfileDetails() {
        if (selectedProfile == null) {
            clearSelectedDetails("No profile selected");
            return;
        }

        selectedTitleLabel.setText(selectedProfile.getName());
        selectedSubtitleLabel.setText(displayText(selectedProfile.getDescription(), "No description"));
        selectedCodeLabel.setText(displayText(selectedProfile.getExportNaming(), "{profileCode}_{boxId}"));
        updateSelectedStatus(displayProfileStatus(selectedProfile));
    }

    private void renderSelectedUserDetails() {
        if (selectedUser == null) {
            clearSelectedDetails("No user selected");
            return;
        }

        selectedTitleLabel.setText(selectedUser.getName());
        selectedSubtitleLabel.setText(displayText(selectedUser.getEmail(), "No email"));
        selectedCodeLabel.setText(selectedUser.getRole() + " account");
        updateSelectedStatus(selectedUser.getStatus());
    }

    private void clearSelectedDetails(String message) {
        selectedTitleLabel.setText(message);
        selectedSubtitleLabel.setText("");
        selectedCodeLabel.setText("");
        selectedStatusBadge.setText("");
        selectedStatusBadge.getStyleClass().setAll("metadata-status-badge", "metadata-status-archived");
    }

    private void renderAssignmentRows() {
        if (mode == AssignmentMode.BY_PROFILE) {
            renderUserAssignmentRows();
        } else {
            renderProfileAssignmentRows();
        }
    }

    private void renderUserAssignmentRows() {
        if (selectedProfile == null) {
            assignmentRowsContainer.getChildren().clear();
            return;
        }

        String searchText = normalize(rightSearchField.getText());
        String selectedRole = rightFilterComboBox.getValue();

        assignmentRowsContainer.getChildren().setAll(
                users.stream()
                        .filter(user -> matchesUserSearch(user, searchText))
                        .filter(user -> matchesRole(user.getRole(), selectedRole))
                        .map(this::buildUserAssignmentRow)
                        .toList()
        );
    }

    private void renderProfileAssignmentRows() {
        if (selectedUser == null) {
            assignmentRowsContainer.getChildren().clear();
            return;
        }

        String searchText = normalize(rightSearchField.getText());
        String selectedStatus = rightFilterComboBox.getValue();

        assignmentRowsContainer.getChildren().setAll(
                profiles.stream()
                        .filter(profile -> matchesProfileSearch(profile, searchText))
                        .filter(profile -> matchesStatus(displayProfileStatus(profile), selectedStatus))
                        .map(this::buildProfileAssignmentRow)
                        .toList()
        );
    }

    private HBox buildUserAssignmentRow(User user) {
        HBox row = createAssignmentRow();

        CheckBox checkBox = createCheckBox(getAssignedUserIds(selectedProfile.getId()).contains(user.getId()));

        checkBox.setOnAction(event -> {
            setUserAssignment(user, checkBox.isSelected());
            event.consume();
        });

        Runnable toggleUserAssignment = () -> {
            checkBox.setSelected(!checkBox.isSelected());
            setUserAssignment(user, checkBox.isSelected());
        };

        row.setOnMouseClicked(event -> {
            if (isInsideCheckBox(event.getPickResult().getIntersectedNode())) {
                return;
            }

            toggleUserAssignment.run();
        });

        AdminKeyboard.makeActivatable(row, "Toggle assignment for " + user.getName(), toggleUserAssignment);

        VBox textBox = new VBox(3);
        textBox.getChildren().addAll(
                createLabel(user.getName(), "assignment-row-title"),
                createLabel(displayText(user.getEmail(), "No email"), "assignment-row-subtitle")
        );

        row.getChildren().addAll(
                checkBox,
                createAvatar(initialsFor(user.getName())),
                textBox,
                createSpacer(),
                createRoleBadge(user.getRole()),
                createStatusBadge(user.getStatus())
        );

        return row;
    }

    private HBox buildProfileAssignmentRow(ScanProfile profile) {
        HBox row = createAssignmentRow();

        CheckBox checkBox = createCheckBox(getAssignedUserIds(profile.getId()).contains(selectedUser.getId()));

        checkBox.setOnAction(event -> {
            setProfileAssignment(profile, checkBox.isSelected());
            event.consume();
        });

        Runnable toggleProfileAssignment = () -> {
            checkBox.setSelected(!checkBox.isSelected());
            setProfileAssignment(profile, checkBox.isSelected());
        };

        row.setOnMouseClicked(event -> {
            if (isInsideCheckBox(event.getPickResult().getIntersectedNode())) {
                return;
            }

            toggleProfileAssignment.run();
        });

        AdminKeyboard.makeActivatable(row, "Toggle assignment for " + profile.getName(), toggleProfileAssignment);

        VBox textBox = new VBox(3);
        textBox.getChildren().addAll(
                createLabel(profile.getName(), "assignment-row-title"),
                createLabel(displayText(profile.getExportNaming(), "{profileCode}_{boxId}"), "assignment-row-subtitle")
        );

        row.getChildren().addAll(
                checkBox,
                createAvatar(initialsFor(profile.getName())),
                textBox,
                createSpacer(),
                createStatusBadge(displayProfileStatus(profile))
        );

        return row;
    }

    private HBox createAssignmentRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("assignment-checkbox-row");
        row.setPickOnBounds(true);
        return row;
    }

    private CheckBox createCheckBox(boolean selected) {
        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("assignment-checkbox");
        checkBox.setSelected(selected);
        checkBox.setFocusTraversable(false);
        checkBox.setPickOnBounds(true);

        checkBox.setMinSize(24, 24);
        checkBox.setPrefSize(24, 24);
        checkBox.setMaxSize(24, 24);

        return checkBox;
    }

    private void setUserAssignment(User user, boolean assigned) {
        updateAssignment(selectedProfile.getId(), user.getId(), assigned);
        renderLeftList();
        updateChangesLabel();
    }

    private void setProfileAssignment(ScanProfile profile, boolean assigned) {
        updateAssignment(profile.getId(), selectedUser.getId(), assigned);
        renderLeftList();
        updateChangesLabel();
    }

    private boolean isInsideCheckBox(Node node) {
        Node current = node;

        while (current != null) {
            if (current instanceof CheckBox) {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }

    private void updateAssignment(int profileId, int userId, boolean assigned) {
        Set<Integer> assignedUsers = workingAssignments.computeIfAbsent(profileId, ignored -> new HashSet<>());

        if (assigned) {
            assignedUsers.add(userId);
        } else {
            assignedUsers.remove(userId);
        }

        if (assignedUsers.isEmpty()) {
            workingAssignments.remove(profileId);
        }
    }

    private Set<Integer> getAssignedUserIds(int profileId) {
        return workingAssignments.getOrDefault(profileId, Set.of());
    }

    private List<Integer> getAssignedProfileIds(int userId) {
        return workingAssignments.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .toList();
    }

    private void updateSelectedStatus(String status) {
        selectedStatusBadge.setText(status);
        selectedStatusBadge.getStyleClass().setAll("metadata-status-badge", statusClassFor(status));
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().addAll("metadata-status-badge", statusClassFor(status));
        return badge;
    }

    private Label createRoleBadge(String role) {
        Label badge = new Label(role);
        badge.getStyleClass().addAll(
                "role-badge",
                "Admin".equalsIgnoreCase(role) ? "role-badge-admin" : "role-badge-user"
        );
        return badge;
    }

    private Label createAvatar(String initials) {
        Label avatar = new Label(initials);
        avatar.getStyleClass().add("assignment-avatar");
        return avatar;
    }

    private Label createLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void updateChangesLabel() {
        changesLabel.setText(hasUnsavedChanges() ? "Unsaved changes" : "No unsaved changes");
    }

    private boolean hasUnsavedChanges() {
        return !savedAssignments.equals(workingAssignments);
    }

    private void resetWorkingAssignments() {
        copyAssignments(savedAssignments, workingAssignments);
    }

    private void copyAssignments(Map<Integer, Set<Integer>> source, Map<Integer, Set<Integer>> destination) {
        destination.clear();

        if (source == null) {
            return;
        }

        source.forEach((profileId, userIds) -> {
            if (userIds != null && !userIds.isEmpty()) {
                destination.put(profileId, new HashSet<>(userIds));
            }
        });
    }

    private void resetScrollPositions() {
        Platform.runLater(() -> {
            resetLeftScroll();
            resetRightScroll();
        });
    }

    private void resetLeftScroll() {
        if (leftListScrollPane != null) {
            leftListScrollPane.setVvalue(0);
        }
    }

    private void resetRightScroll() {
        if (assignmentRowsScrollPane != null) {
            assignmentRowsScrollPane.setVvalue(0);
        }
    }

    private boolean matchesProfileSearch(ScanProfile profile, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return normalize(profile.getName()).contains(searchText)
                || normalize(profile.getDescription()).contains(searchText)
                || normalize(profile.getExportNaming()).contains(searchText)
                || normalize(displayProfileStatus(profile)).contains(searchText);
    }

    private boolean matchesUserSearch(User user, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return normalize(user.getName()).contains(searchText)
                || normalize(user.getEmail()).contains(searchText)
                || normalize(user.getRole()).contains(searchText)
                || normalize(user.getStatus()).contains(searchText);
    }

    private boolean matchesStatus(String status, String selectedStatus) {
        return selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || status.equalsIgnoreCase(selectedStatus);
    }

    private boolean matchesRole(String role, String selectedRole) {
        return selectedRole == null
                || ALL_ROLES.equals(selectedRole)
                || role.equalsIgnoreCase(selectedRole);
    }

    private String displayProfileStatus(ScanProfile profile) {
        return profile.isArchived() ? "Archived" : profile.getStatus();
    }

    private String statusClassFor(String status) {
        return switch (normalize(status)) {
            case "active" -> "metadata-status-active";
            case "draft" -> "metadata-status-draft";
            case "archived", "inactive" -> "metadata-status-archived";
            default -> "metadata-status-archived";
        };
    }

    private String formatAssignedUsers(int count) {
        return count == 1 ? "1 assigned user" : count + " assigned users";
    }

    private String formatAssignedProfiles(int count) {
        return count == 1 ? "1 assigned profile" : count + " assigned profiles";
    }

    private boolean isSelectedProfile(ScanProfile profile) {
        return selectedProfile != null && selectedProfile.getId() == profile.getId();
    }

    private boolean isSelectedUser(User user) {
        return selectedUser != null && selectedUser.getId() == user.getId();
    }

    private Optional<ScanProfile> findProfileById(int profileId) {
        return profiles.stream()
                .filter(profile -> profile.getId() == profileId)
                .findFirst();
    }

    private Optional<User> findUserById(int userId) {
        return users.stream()
                .filter(user -> user.getId() == userId)
                .findFirst();
    }

    private String displayText(String value, String fallback) {
        String cleanedValue = value == null ? "" : value.trim();
        return cleanedValue.isBlank() ? fallback : cleanedValue;
    }

    private String initialsFor(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String[] parts = value.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0]
                    .substring(0, Math.min(2, parts[0].length()))
                    .toUpperCase(Locale.ROOT);
        }

        return (parts[0].substring(0, 1) + parts[1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private enum AssignmentMode {
        BY_PROFILE,
        BY_USER
    }

    static final class ProfileAccessModel {
        private final int id;
        private final String name;
        private final String description;
        private final String exportNaming;
        private final String status;

        ProfileAccessModel(int id, String name, String description, String exportNaming, String status) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.exportNaming = exportNaming;
            this.status = status;
        }

        int id() {
            return id;
        }

        String name() {
            return name;
        }

        String description() {
            return description;
        }

        String exportNaming() {
            return exportNaming;
        }

        String status() {
            return status;
        }
    }

    static final class UserAccessModel {
        private final int id;
        private final String name;
        private final String email;
        private final String role;
        private final String status;

        UserAccessModel(int id, String name, String email, String role, String status) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.status = status;
        }

        int id() {
            return id;
        }

        String name() {
            return name;
        }

        String email() {
            return email;
        }

        String role() {
            return role;
        }

        String status() {
            return status;
        }
    }
}
