package easv.gui.controller.admin;

import easv.be.ScanProfile;
import easv.be.User;
import easv.bll.AdminManager;
import easv.gui.controller.util.PrimeIcons;
import easv.gui.controller.util.Strings;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    private AdminManager adminManager;

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
    @FXML private Label selectedStatusBadge;

    @FXML private Label changesLabel;
    @FXML private Button addEntityButton;

    void setAdminManager(AdminManager adminManager) {
        this.adminManager = adminManager;
        if (this.adminManager == null) {
            return;
        }
        loadDataFromManager();
        configureFiltersForMode();
        renderPage();
    }

    @FXML
    private void initialize() {
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
        if (adminManager == null) {
            return;
        }

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
        if (adminManager == null) {
            profiles.clear();
            users.clear();
            savedAssignments.clear();
            workingAssignments.clear();
            selectedProfile = null;
            selectedUser = null;
            return;
        }

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
            leftPanelTitleLabel.setText("SCANNING PROFILES");
            rightPanelTitleLabel.setText("ASSIGNED USERS");
            leftSearchField.setPromptText("Search profiles...");
            rightSearchField.setPromptText("Search users...");
            addEntityButton.setText("+ Add User");
            return;
        }

        leftPanelTitleLabel.setText("USERS");
        rightPanelTitleLabel.setText("ASSIGNED PROFILES");
        leftSearchField.setPromptText("Search users...");
        rightSearchField.setPromptText("Search profiles...");
        addEntityButton.setText("+ Add Profile");
    }

    private void renderLeftList() {
        if (mode == AssignmentMode.BY_PROFILE) {
            renderProfileList();
        } else {
            renderUserList();
        }
    }

    private void renderProfileList() {
        String searchText = Strings.normalize(leftSearchField.getText());
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
        String searchText = Strings.normalize(leftSearchField.getText());
        String selectedRole = leftFilterComboBox.getValue();

        leftListContainer.getChildren().setAll(
                users.stream()
                        .filter(user -> matchesUserSearch(user, searchText))
                        .filter(user -> matchesRole(user.getRole(), selectedRole))
                        .map(this::buildUserListItem)
                        .toList()
        );
    }

    private Button buildProfileListItem(ScanProfile profile) {
        Button item = createListItem(isSelectedProfile(profile));
        HBox content = createListItemContent();

        Label iconLabel = PrimeIcons.create(profileIconFor(profile), "assignment-profile-icon", profileIconClassFor(profile));

        HBox countRow = new HBox(5);
        countRow.setAlignment(Pos.CENTER_LEFT);
        countRow.getChildren().addAll(
                PrimeIcons.create("", "assignment-list-user-icon"),
                createLabel(String.valueOf(getAssignedUserIds(profile.getId()).size()), "assignment-list-count")
        );

        VBox textBox = new VBox(5);
        textBox.setMinWidth(0);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        textBox.getChildren().addAll(
                createLabel(profile.getName(), "assignment-list-title"),
                countRow
        );

        content.getChildren().addAll(
                iconLabel,
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

        item.setGraphic(content);
        item.setOnAction(event -> selectProfile.run());

        return item;
    }

    private Button buildUserListItem(User user) {
        Button item = createListItem(isSelectedUser(user));
        HBox content = createListItemContent();

        Label iconLabel = PrimeIcons.create("", "assignment-profile-icon", "assignment-profile-icon-user");

        VBox textBox = new VBox(5);
        textBox.setMinWidth(0);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        textBox.getChildren().addAll(
                createLabel(user.getName(), "assignment-list-title"),
                createLabel(Strings.displayText(user.getEmail(), "No email"), "assignment-list-subtitle"),
                createLabel(formatAssignedProfiles(getAssignedProfileIds(user.getId()).size()), "assignment-list-subtitle")
        );

        content.getChildren().addAll(
                iconLabel,
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

        item.setGraphic(content);
        item.setOnAction(event -> selectUser.run());

        return item;
    }

    private Button createListItem(boolean active) {
        Button item = new Button();
        item.setMaxWidth(Double.MAX_VALUE);
        item.setFocusTraversable(true);
        item.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        item.getStyleClass().add("assignment-list-item");

        if (active) {
            item.getStyleClass().add(ACTIVE_LIST_ITEM_CLASS);
        }

        return item;
    }

    private HBox createListItemContent() {
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);
        return content;
    }

    private void renderSelectedDetails() {
        if (mode == AssignmentMode.BY_PROFILE) {
            renderSelectedProfileDetails();
            addEntityButton.setDisable(selectedProfile == null);
        } else {
            renderSelectedUserDetails();
            addEntityButton.setDisable(selectedUser == null);
        }
    }

    private void renderSelectedProfileDetails() {
        if (selectedProfile == null) {
            clearSelectedDetails("No profile selected");
            return;
        }

        selectedTitleLabel.setText(selectedProfile.getName());
        updateSelectedStatus(displayProfileStatus(selectedProfile));
    }

    private void renderSelectedUserDetails() {
        if (selectedUser == null) {
            clearSelectedDetails("No user selected");
            return;
        }

        selectedTitleLabel.setText(selectedUser.getName());
        updateSelectedStatus(selectedUser.getStatus());
    }

    private void clearSelectedDetails(String message) {
        selectedTitleLabel.setText(message);
        selectedStatusBadge.setText("");
        selectedStatusBadge.getStyleClass().setAll("assignment-status-badge", "assignment-status-archived");
    }

    private void renderAssignmentRows() {
        if (mode == AssignmentMode.BY_PROFILE) {
            renderAssignedUserRows();
        } else {
            renderAssignedProfileRows();
        }
    }

    private void renderAssignedUserRows() {
        if (selectedProfile == null) {
            assignmentRowsContainer.getChildren().clear();
            return;
        }

        String searchText = Strings.normalize(rightSearchField.getText());
        String selectedRole = rightFilterComboBox.getValue();
        Set<Integer> assignedIds = getAssignedUserIds(selectedProfile.getId());

        assignmentRowsContainer.getChildren().setAll(
                users.stream()
                        .filter(user -> assignedIds.contains(user.getId()))
                        .filter(user -> matchesUserSearch(user, searchText))
                        .filter(user -> matchesRole(user.getRole(), selectedRole))
                        .map(this::buildAssignedUserRow)
                        .toList()
        );
    }

    private void renderAssignedProfileRows() {
        if (selectedUser == null) {
            assignmentRowsContainer.getChildren().clear();
            return;
        }

        String searchText = Strings.normalize(rightSearchField.getText());
        String selectedStatus = rightFilterComboBox.getValue();
        List<Integer> assignedIds = getAssignedProfileIds(selectedUser.getId());

        assignmentRowsContainer.getChildren().setAll(
                profiles.stream()
                        .filter(profile -> assignedIds.contains(profile.getId()))
                        .filter(profile -> matchesProfileSearch(profile, searchText))
                        .filter(profile -> matchesStatus(displayProfileStatus(profile), selectedStatus))
                        .map(this::buildAssignedProfileRow)
                        .toList()
        );
    }

    private HBox buildAssignedUserRow(User user) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("assignment-assigned-row");

        VBox textBox = createAssignmentTextBox(
                user.getName(),
                Strings.displayText(user.getEmail(), "No email")
        );

        Button removeBtn = createRemoveButton(() -> {
            updateAssignment(selectedProfile.getId(), user.getId(), false);
            renderAssignedUserRows();
            renderLeftList();
            updateChangesLabel();
        });

        row.getChildren().addAll(
                createAvatar(Strings.initials(user.getName(), "")),
                textBox,
                createRoleBadge(user.getRole()),
                removeBtn
        );
        return row;
    }

    private HBox buildAssignedProfileRow(ScanProfile profile) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("assignment-assigned-row");

        Label iconLabel = PrimeIcons.create(profileIconFor(profile), "assignment-profile-icon", profileIconClassFor(profile));

        VBox textBox = createAssignmentTextBox(
                profile.getName(),
                Strings.displayText(profile.getExportNaming(), ScanProfile.DEFAULT_EXPORT_NAMING)
        );

        Button removeBtn = createRemoveButton(() -> {
            updateAssignment(profile.getId(), selectedUser.getId(), false);
            renderAssignedProfileRows();
            renderLeftList();
            updateChangesLabel();
        });

        row.getChildren().addAll(iconLabel, textBox, removeBtn);
        return row;
    }

    private VBox createAssignmentTextBox(String title, String subtitle) {
        Label titleLabel = createLabel(title, "assignment-row-title");
        Label subtitleLabel = createLabel(subtitle, "assignment-row-subtitle");

        VBox textBox = new VBox(3, titleLabel, subtitleLabel);
        textBox.setMinWidth(0);
        textBox.setMaxWidth(Double.MAX_VALUE);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        return textBox;
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
        selectedStatusBadge.getStyleClass().setAll("assignment-status-badge", statusClassFor(status));
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().addAll("assignment-status-badge", statusClassFor(status));
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

    private Button createRemoveButton(Runnable onRemove) {
        Button removeBtn = new Button();
        removeBtn.setGraphic(PrimeIcons.create("\uE90B", "assignment-remove-icon"));
        removeBtn.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        removeBtn.getStyleClass().add("assignment-remove-button");
        removeBtn.setFocusTraversable(false);
        removeBtn.setOnAction(e -> onRemove.run());
        return removeBtn;
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

        return Strings.normalize(profile.getName()).contains(searchText)
                || Strings.normalize(profile.getDescription()).contains(searchText)
                || Strings.normalize(profile.getExportNaming()).contains(searchText)
                || Strings.normalize(displayProfileStatus(profile)).contains(searchText);
    }

    private boolean matchesUserSearch(User user, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return Strings.normalize(user.getName()).contains(searchText)
                || Strings.normalize(user.getEmail()).contains(searchText)
                || Strings.normalize(user.getRole()).contains(searchText)
                || Strings.normalize(user.getStatus()).contains(searchText);
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
        return switch (Strings.normalize(status)) {
            case "active" -> "assignment-status-active";
            case "draft" -> "assignment-status-draft";
            case "archived", "inactive" -> "assignment-status-archived";
            default -> "assignment-status-archived";
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

    private String profileIconFor(ScanProfile profile) {
        if (profile.isArchived()) {
            return "";
        }
        return switch (Strings.normalize(profile.getStatus())) {
            case "draft" -> "";
            default -> "";
        };
    }

    private String profileIconClassFor(ScanProfile profile) {
        if (profile.isArchived()) {
            return "assignment-profile-icon-archived";
        }
        return switch (Strings.normalize(profile.getStatus())) {
            case "draft" -> "assignment-profile-icon-draft";
            default -> "assignment-profile-icon-active";
        };
    }

    @FXML
    private void addEntity() {
        if (mode == AssignmentMode.BY_PROFILE && selectedProfile != null) {
            openUserPicker();
        } else if (mode == AssignmentMode.BY_USER && selectedUser != null) {
            openProfilePicker();
        }
    }

    private void openUserPicker() {
        Set<Integer> alreadyAssigned = new HashSet<>(getAssignedUserIds(selectedProfile.getId()));
        Set<Integer> pending = new HashSet<>();

        TextField searchField = buildPickerSearch("Search users...");
        ComboBox<String> roleFilter = new ComboBox<>();
        roleFilter.getItems().setAll(ALL_ROLES, "Admin", "User");
        roleFilter.setValue(ALL_ROLES);
        roleFilter.getStyleClass().add("role-filter");

        Label countLabel = new Label("Select users to add");
        countLabel.getStyleClass().add("assignment-picker-count");

        Button assignBtn = new Button("Assign");
        assignBtn.getStyleClass().add("assignment-picker-assign-button");
        assignBtn.setFocusTraversable(false);
        assignBtn.setDisable(true);

        VBox listBox = new VBox(0);
        listBox.setFillWidth(true);

        Runnable refresh = () -> listBox.getChildren().setAll(
                users.stream()
                        .filter(u -> matchesUserSearch(u, Strings.normalize(searchField.getText())))
                        .filter(u -> matchesRole(u.getRole(), roleFilter.getValue()))
                        .map(u -> buildPickerUserRow(u, alreadyAssigned, pending, countLabel, assignBtn))
                        .toList()
        );

        searchField.textProperty().addListener((obs, o, n) -> refresh.run());
        roleFilter.valueProperty().addListener((obs, o, n) -> refresh.run());
        refresh.run();

        Runnable close = showPicker("Add Users", searchField, roleFilter, listBox, countLabel, assignBtn);

        assignBtn.setOnAction(e -> {
            pending.forEach(uid -> updateAssignment(selectedProfile.getId(), uid, true));
            renderAssignedUserRows();
            renderLeftList();
            updateChangesLabel();
            close.run();
        });

        Platform.runLater(searchField::requestFocus);
    }

    private void openProfilePicker() {
        Set<Integer> alreadyAssigned = new HashSet<>(getAssignedProfileIds(selectedUser.getId()));
        Set<Integer> pending = new HashSet<>();

        TextField searchField = buildPickerSearch("Search profiles...");
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().setAll(ALL_STATUSES, "Active", "Draft", "Archived");
        statusFilter.setValue(ALL_STATUSES);
        statusFilter.getStyleClass().add("role-filter");

        Label countLabel = new Label("Select profiles to add");
        countLabel.getStyleClass().add("assignment-picker-count");

        Button assignBtn = new Button("Assign");
        assignBtn.getStyleClass().add("assignment-picker-assign-button");
        assignBtn.setFocusTraversable(false);
        assignBtn.setDisable(true);

        VBox listBox = new VBox(0);
        listBox.setFillWidth(true);

        Runnable refresh = () -> listBox.getChildren().setAll(
                profiles.stream()
                        .filter(p -> matchesProfileSearch(p, Strings.normalize(searchField.getText())))
                        .filter(p -> matchesStatus(displayProfileStatus(p), statusFilter.getValue()))
                        .map(p -> buildPickerProfileRow(p, alreadyAssigned, pending, countLabel, assignBtn))
                        .toList()
        );

        searchField.textProperty().addListener((obs, o, n) -> refresh.run());
        statusFilter.valueProperty().addListener((obs, o, n) -> refresh.run());
        refresh.run();

        Runnable close = showPicker("Add Profiles", searchField, statusFilter, listBox, countLabel, assignBtn);

        assignBtn.setOnAction(e -> {
            pending.forEach(pid -> updateAssignment(pid, selectedUser.getId(), true));
            renderAssignedProfileRows();
            renderLeftList();
            updateChangesLabel();
            close.run();
        });

        Platform.runLater(searchField::requestFocus);
    }

    private Button buildPickerUserRow(User user, Set<Integer> alreadyAssigned, Set<Integer> pending, Label countLabel, Button assignBtn) {
        boolean isAssigned = alreadyAssigned.contains(user.getId());

        Button row = new Button();
        row.setMaxWidth(Double.MAX_VALUE);
        row.setFocusTraversable(false);
        row.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        row.getStyleClass().add("assignment-picker-row");

        if (isAssigned) {
            row.getStyleClass().add("assignment-picker-row-assigned");
        } else if (pending.contains(user.getId())) {
            row.getStyleClass().add("assignment-picker-row-selected");
        }

        VBox textBox = new VBox(3,
                createLabel(user.getName(), "assignment-picker-name"),
                createLabel(Strings.displayText(user.getEmail(), "No email"), "assignment-picker-subtitle")
        );
        textBox.setMinWidth(0);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox content = new HBox(10, createAvatar(Strings.initials(user.getName(), "")), textBox, createRoleBadge(user.getRole()));
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setFillHeight(false);
        content.getStyleClass().add("assignment-picker-row-content");

        if (isAssigned) {
            Label inProfileLabel = new Label("In profile");
            inProfileLabel.getStyleClass().add("assignment-picker-in-label");
            content.getChildren().add(inProfileLabel);
        }

        row.setGraphic(content);

        if (!isAssigned) {
            row.setOnAction(e -> {
                if (pending.remove(user.getId())) {
                    row.getStyleClass().remove("assignment-picker-row-selected");
                } else {
                    pending.add(user.getId());
                    row.getStyleClass().add("assignment-picker-row-selected");
                }
                updatePickerFooter(countLabel, assignBtn, pending.size());
            });
        }

        return row;
    }

    private Button buildPickerProfileRow(ScanProfile profile, Set<Integer> alreadyAssigned, Set<Integer> pending, Label countLabel, Button assignBtn) {
        boolean isAssigned = alreadyAssigned.contains(profile.getId());

        Button row = new Button();
        row.setMaxWidth(Double.MAX_VALUE);
        row.setFocusTraversable(false);
        row.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        row.getStyleClass().add("assignment-picker-row");

        if (isAssigned) {
            row.getStyleClass().add("assignment-picker-row-assigned");
        } else if (pending.contains(profile.getId())) {
            row.getStyleClass().add("assignment-picker-row-selected");
        }

        Label iconLabel = PrimeIcons.create(profileIconFor(profile), "assignment-profile-icon", profileIconClassFor(profile));

        VBox textBox = new VBox(2,
                createLabel(profile.getName(), "assignment-picker-name"),
                createLabel(Strings.displayText(profile.getExportNaming(), ScanProfile.DEFAULT_EXPORT_NAMING), "assignment-picker-subtitle")
        );
        textBox.setMinWidth(0);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox content = new HBox(10, iconLabel, textBox);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);
        content.getStyleClass().add("assignment-picker-row-content");

        if (isAssigned) {
            Label assignedLabel = new Label("Assigned");
            assignedLabel.getStyleClass().add("assignment-picker-in-label");
            content.getChildren().add(assignedLabel);
        }

        row.setGraphic(content);

        if (!isAssigned) {
            row.setOnAction(e -> {
                if (pending.remove(profile.getId())) {
                    row.getStyleClass().remove("assignment-picker-row-selected");
                } else {
                    pending.add(profile.getId());
                    row.getStyleClass().add("assignment-picker-row-selected");
                }
                updatePickerFooter(countLabel, assignBtn, pending.size());
            });
        }

        return row;
    }

    private TextField buildPickerSearch(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("search-field-input");
        HBox.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private Runnable showPicker(String title, TextField searchField, ComboBox<String> filter, VBox listBox, Label countLabel, Button assignBtn) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("assignment-picker-title");

        Button closeButton = new Button("X");
        closeButton.getStyleClass().add("assignment-picker-close-button");
        closeButton.setFocusTraversable(false);

        Region dragFill = createSpacer();
        HBox dragHandle = new HBox(8, titleLabel, dragFill, closeButton);
        dragHandle.setAlignment(Pos.CENTER_LEFT);
        dragHandle.getStyleClass().add("assignment-picker-drag-handle");

        HBox searchRow = new HBox(8, searchField, filter);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getStyleClass().add("assignment-picker-header");

        ScrollPane listScroll = new ScrollPane(listBox);
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        listScroll.getStyleClass().add("assignment-picker-list-scroll");
        listScroll.setMaxHeight(264);

        HBox footer = new HBox(8, countLabel, createSpacer(), assignBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("assignment-picker-footer");

        VBox pickerBox = new VBox(0, dragHandle, searchRow, listScroll, footer);
        pickerBox.getStyleClass().add("assignment-picker-popup");
        pickerBox.setPrefWidth(370);

        // Full-scene transparent overlay — managed child of StackPane root so
        // JavaFX's layout pass reaches pickerBox and lays out its children.
        Pane overlay = new Pane();
        overlay.setMaxWidth(Double.MAX_VALUE);
        overlay.setMaxHeight(Double.MAX_VALUE);

        // Click-catcher fills the overlay; closes the picker on outside clicks.
        Region clickCatcher = new Region();
        clickCatcher.setPickOnBounds(true);
        clickCatcher.prefWidthProperty().bind(overlay.widthProperty());
        clickCatcher.prefHeightProperty().bind(overlay.heightProperty());

        Pane sceneRoot = (Pane) addEntityButton.getScene().getRoot();
        Runnable close = () -> {
            clickCatcher.prefWidthProperty().unbind();
            clickCatcher.prefHeightProperty().unbind();
            sceneRoot.getChildren().remove(overlay);
        };
        closeButton.setOnAction(event -> close.run());
        clickCatcher.setOnMouseClicked(e -> close.run());

        // Position picker BESIDE the Add button — to its left when there's room,
        // otherwise to its right. Vertically anchored at the button's top.
        Bounds btnBounds = addEntityButton.localToScene(addEntityButton.getBoundsInLocal());
        double pickerWidth = 370;
        double leftX = btnBounds.getMinX() - pickerWidth - 8;
        double rightX = btnBounds.getMaxX() + 8;
        double x = (leftX >= 0)
                ? leftX
                : Math.min(rightX, sceneRoot.getWidth() - pickerWidth);

        pickerBox.setLayoutX(x);
        pickerBox.setLayoutY(btnBounds.getMinY());

        overlay.getChildren().addAll(clickCatcher, pickerBox);
        sceneRoot.getChildren().add(overlay);
        makePickerMovable(pickerBox, titleLabel, sceneRoot);
        makePickerMovable(pickerBox, dragFill, sceneRoot);

        // After layout, slide up only enough to keep the picker fully on screen.
        Platform.runLater(() -> {
            double pHeight = pickerBox.getHeight();
            if (pHeight <= 0) return;
            double maxY = sceneRoot.getHeight() - pHeight;
            pickerBox.setLayoutY(Math.max(0, Math.min(btnBounds.getMinY(), maxY)));
        });

        return close;
    }

    private void makePickerMovable(VBox pickerBox, Node dragTarget, Pane sceneRoot) {
        double[] dragOffset = new double[2];
        boolean[] dragging = new boolean[1];

        dragTarget.setOnMousePressed(event -> {
            dragging[0] = true;
            dragOffset[0] = event.getSceneX() - pickerBox.getLayoutX();
            dragOffset[1] = event.getSceneY() - pickerBox.getLayoutY();
            event.consume();
        });

        dragTarget.setOnMouseDragged(event -> {
            if (!dragging[0]) {
                return;
            }

            double width = Math.max(pickerBox.getWidth(), pickerBox.getPrefWidth());
            double height = Math.max(pickerBox.getHeight(), pickerBox.prefHeight(-1));
            double maxX = Math.max(0, sceneRoot.getWidth() - width);
            double maxY = Math.max(0, sceneRoot.getHeight() - height);

            pickerBox.setLayoutX(clamp(event.getSceneX() - dragOffset[0], 0, maxX));
            pickerBox.setLayoutY(clamp(event.getSceneY() - dragOffset[1], 0, maxY));
            event.consume();
        });

        dragTarget.setOnMouseReleased(event -> dragging[0] = false);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void updatePickerFooter(Label countLabel, Button assignBtn, int count) {
        if (count == 0) {
            countLabel.setText("Select to add");
            assignBtn.setDisable(true);
        } else {
            countLabel.setText(count == 1 ? "1 selected" : count + " selected");
            assignBtn.setDisable(false);
        }
    }

    private enum AssignmentMode {
        BY_PROFILE,
        BY_USER
    }
}
