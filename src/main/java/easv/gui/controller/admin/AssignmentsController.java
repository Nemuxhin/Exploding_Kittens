package easv.gui.controller.admin;

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
import java.util.Set;

public class AssignmentsController {

    private static final String ALL_STATUSES = "All Statuses";
    private static final String ALL_ROLES = "All Roles";

    private static final String ACTIVE_MODE_CLASS = "assignment-mode-button-active";
    private static final String ACTIVE_LIST_ITEM_CLASS = "assignment-list-item-active";

    private AssignmentMode mode = AssignmentMode.BY_PROFILE;

    private ProfileAccessModel selectedProfile;
    private UserAccessModel selectedUser;

    private final ObservableList<ProfileAccessModel> profiles = FXCollections.observableArrayList();
    private final ObservableList<UserAccessModel> users = FXCollections.observableArrayList();

    private final Map<Integer, Set<Integer>> savedAssignments = new HashMap<>();
    private final Map<Integer, Set<Integer>> workingAssignments = new HashMap<>();

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

    @FXML
    private void initialize() {
        loadSampleData();
        resetWorkingAssignments();

        if (!profiles.isEmpty()) {
            selectedProfile = profiles.get(0);
        }

        if (!users.isEmpty()) {
            selectedUser = users.get(0);
        }

        configureListeners();
        switchToMode(AssignmentMode.BY_PROFILE);
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
        copyAssignments(workingAssignments, savedAssignments);
        updateChangesLabel();
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

        if (mode == AssignmentMode.BY_PROFILE && selectedProfile == null && !profiles.isEmpty()) {
            selectedProfile = profiles.get(0);
        }

        if (mode == AssignmentMode.BY_USER && selectedUser == null && !users.isEmpty()) {
            selectedUser = users.get(0);
        }

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
                        .filter(profile -> matchesStatus(profile.status(), selectedStatus))
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
                        .filter(user -> matchesRole(user.role(), selectedRole))
                        .map(this::buildUserListItem)
                        .toList()
        );
    }

    private HBox buildProfileListItem(ProfileAccessModel profile) {
        HBox item = createListItem(profile.equals(selectedProfile));

        VBox textBox = new VBox(6);
        textBox.getChildren().addAll(
                createLabel(profile.name(), "assignment-list-title"),
                createLabel(formatAssignedUsers(getAssignedUserIds(profile.id()).size()), "assignment-list-subtitle"),
                createLabel("Export: " + profile.exportNaming(), "assignment-list-subtitle")
        );

        item.getChildren().addAll(
                textBox,
                createSpacer(),
                createStatusBadge(profile.status())
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
        AdminKeyboard.makeActivatable(item, "Select profile " + profile.name(), selectProfile);

        return item;
    }

    private HBox buildUserListItem(UserAccessModel user) {
        HBox item = createListItem(user.equals(selectedUser));

        VBox textBox = new VBox(6);
        textBox.getChildren().addAll(
                createLabel(user.name(), "assignment-list-title"),
                createLabel(user.email(), "assignment-list-subtitle"),
                createLabel(formatAssignedProfiles(getAssignedProfileIds(user.id()).size()), "assignment-list-subtitle")
        );

        item.getChildren().addAll(
                textBox,
                createSpacer(),
                createRoleBadge(user.role())
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
        AdminKeyboard.makeActivatable(item, "Select user " + user.name(), selectUser);

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
            if (selectedProfile == null) {
                clearSelectedDetails();
                return;
            }

            selectedTitleLabel.setText(selectedProfile.name());
            selectedSubtitleLabel.setText(selectedProfile.description());
            selectedCodeLabel.setText(selectedProfile.exportNaming());
            updateSelectedStatus(selectedProfile.status());
            return;
        }

        if (selectedUser == null) {
            clearSelectedDetails();
            return;
        }

        selectedTitleLabel.setText(selectedUser.name());
        selectedSubtitleLabel.setText(selectedUser.email());
        selectedCodeLabel.setText(selectedUser.role() + " account");
        updateSelectedStatus(selectedUser.status());
    }

    private void clearSelectedDetails() {
        selectedTitleLabel.setText("");
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
                        .filter(user -> matchesRole(user.role(), selectedRole))
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
                        .filter(profile -> matchesStatus(profile.status(), selectedStatus))
                        .map(this::buildProfileAssignmentRow)
                        .toList()
        );
    }

    private HBox buildUserAssignmentRow(UserAccessModel user) {
        HBox row = createAssignmentRow();

        CheckBox checkBox = createCheckBox(getAssignedUserIds(selectedProfile.id()).contains(user.id()));

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
        AdminKeyboard.makeActivatable(row, "Toggle assignment for " + user.name(), toggleUserAssignment);

        VBox textBox = new VBox(3);
        textBox.getChildren().addAll(
                createLabel(user.name(), "assignment-row-title"),
                createLabel(user.email(), "assignment-row-subtitle")
        );

        row.getChildren().addAll(
                checkBox,
                createAvatar(initialsFor(user.name())),
                textBox,
                createSpacer(),
                createRoleBadge(user.role()),
                createStatusBadge(user.status())
        );

        return row;
    }

    private HBox buildProfileAssignmentRow(ProfileAccessModel profile) {
        HBox row = createAssignmentRow();

        CheckBox checkBox = createCheckBox(getAssignedUserIds(profile.id()).contains(selectedUser.id()));

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
        AdminKeyboard.makeActivatable(row, "Toggle assignment for " + profile.name(), toggleProfileAssignment);

        VBox textBox = new VBox(3);
        textBox.getChildren().addAll(
                createLabel(profile.name(), "assignment-row-title"),
                createLabel(profile.exportNaming(), "assignment-row-subtitle")
        );

        row.getChildren().addAll(
                checkBox,
                createAvatar(initialsFor(profile.name())),
                textBox,
                createSpacer(),
                createStatusBadge(profile.status())
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

    private void setUserAssignment(UserAccessModel user, boolean assigned) {
        updateAssignment(selectedProfile.id(), user.id(), assigned);
        renderLeftList();
        updateChangesLabel();
    }

    private void setProfileAssignment(ProfileAccessModel profile, boolean assigned) {
        updateAssignment(profile.id(), selectedUser.id(), assigned);
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
        Set<Integer> assignedUsers = getAssignedUserIds(profileId);

        if (assigned) {
            assignedUsers.add(userId);
        } else {
            assignedUsers.remove(userId);
        }
    }

    private Set<Integer> getAssignedUserIds(int profileId) {
        return workingAssignments.computeIfAbsent(profileId, ignored -> new HashSet<>());
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

        source.forEach((profileId, userIds) ->
                destination.put(profileId, new HashSet<>(userIds))
        );
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

    private boolean matchesProfileSearch(ProfileAccessModel profile, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return normalize(profile.name()).contains(searchText)
                || normalize(profile.description()).contains(searchText)
                || normalize(profile.exportNaming()).contains(searchText)
                || normalize(profile.status()).contains(searchText);
    }

    private boolean matchesUserSearch(UserAccessModel user, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return normalize(user.name()).contains(searchText)
                || normalize(user.email()).contains(searchText)
                || normalize(user.role()).contains(searchText)
                || normalize(user.status()).contains(searchText);
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

    private String statusClassFor(String status) {
        return switch (normalize(status)) {
            case "active" -> "metadata-status-active";
            case "draft" -> "metadata-status-draft";
            case "archived" -> "metadata-status-archived";
            default -> "metadata-status-archived";
        };
    }

    private String formatAssignedUsers(int count) {
        return count == 1 ? "1 assigned user" : count + " assigned users";
    }

    private String formatAssignedProfiles(int count) {
        return count == 1 ? "1 assigned profile" : count + " assigned profiles";
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

    private void loadSampleData() {
        profiles.setAll(AdminDemoData.assignmentProfiles());
        users.setAll(AdminDemoData.assignmentUsers());
        copyAssignments(AdminDemoData.profileAssignments(), savedAssignments);
    }

    private enum AssignmentMode {
        BY_PROFILE,
        BY_USER
    }

    record ProfileAccessModel(
            int id,
            String name,
            String description,
            String exportNaming,
            String status
    ) {
    }

    record UserAccessModel(
            int id,
            String name,
            String email,
            String role,
            String status
    ) {
    }
}
