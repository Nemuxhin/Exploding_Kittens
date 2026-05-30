package easv.gui.controller.admin;

import easv.be.User;
import easv.bll.AdminManager;
import easv.dal.DataAccessException;
import easv.gui.controller.util.PrimeIcons;
import easv.gui.controller.util.PaginationHelper;
import easv.gui.controller.util.Strings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageUsersController {

    private static final String ALL_ROLES = "All Roles";
    private static final String ROLE_ADMIN = "Admin";
    private static final String ROLE_USER = "User";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_INACTIVE = "Inactive";

    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private static final int DEFAULT_ROWS_PER_PAGE = 10;
    private static final List<Integer> ROWS_PER_PAGE_OPTIONS = List.of(10, 25, 50);

    private static final double NAME_COLUMN_WIDTH = 18;
    private static final double USERNAME_COLUMN_WIDTH = 13;
    private static final double EMAIL_COLUMN_WIDTH = 22;
    private static final double ROLE_COLUMN_WIDTH = 9;
    private static final double STATUS_COLUMN_WIDTH = 10;
    private static final double PROFILES_COLUMN_WIDTH = 13;
    private static final double ACTIONS_COLUMN_WIDTH = 15;

    private static final String EDIT_ICON = "\ue942";
    private static final String DEACTIVATE_ICON = "\ue90b";
    private static final String REACTIVATE_ICON = "\ue938";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterComboBox;

    @FXML private VBox overviewPane;
    @FXML private ScrollPane userEditorPane;

    @FXML private Label usersCountLabel;
    @FXML private Label userActionMessageLabel;
    @FXML private VBox userListContainer;
    @FXML private VBox emptyStateBox;

    @FXML private HBox paginationBar;
    @FXML private Label paginationSummaryLabel;
    @FXML private HBox paginationButtonsBox;
    @FXML private ComboBox<Integer> rowsPerPageComboBox;

    @FXML private Label userEditorTitleLabel;
    @FXML private Label userEditorSubtitleLabel;
    @FXML private Button saveUserButton;
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private Label temporaryPasswordLabel;
    @FXML private TextField temporaryPasswordField;
    @FXML private ComboBox<String> userRoleComboBox;
    @FXML private ComboBox<String> userStatusComboBox;
    @FXML private Label generatedPasswordNoticeLabel;
    @FXML private Label profileHelpLabel;
    @FXML private Button showProfileAccessButton;
    @FXML private VBox profileAccessContent;
    @FXML private TextField profileSearchField;
    @FXML private VBox profileListBox;
    @FXML private Label noProfilesWarningLabel;
    @FXML private Label validationLabel;

    private final ObservableList<User> masterUsers = FXCollections.observableArrayList();
    private final List<ProfileAccessControl> profileControls = new ArrayList<>();

    private AdminManager adminManager;
    private FilteredList<User> filteredUsers;
    private int currentPage = 1;
    private int rowsPerPage = DEFAULT_ROWS_PER_PAGE;
    private User editingUser;
    private boolean adminProfileAccessExpanded;

    void setAdminManager(AdminManager adminManager) {
        this.adminManager = adminManager;
        if (this.adminManager == null) {
            return;
        }
        refreshProfileAccessControls();
        loadUsers();
        applyFilters();
    }

    @FXML
    private void initialize() {
        configureRoleFilter();
        configureUserEditor();
        configureRowsPerPageSelector();
        loadUsers();
        configureFiltering();
        applyFilters();
    }

    @FXML
    private void showCreateUserEditor() {
        editingUser = null;
        adminProfileAccessExpanded = false;
        resetUserEditor();
        userEditorTitleLabel.setText("Create User");
        userEditorSubtitleLabel.setText("Add a new user and choose what they can access.");
        saveUserButton.setText("Create User");
        temporaryPasswordLabel.setText("Temporary Password *");
        temporaryPasswordField.setPromptText("Temporary password");
        showEditor();
    }

    @FXML
    private void showOverview() {
        editingUser = null;
        setVisibleAndManaged(userEditorPane, false);
        setVisibleAndManaged(overviewPane, true);
    }

    @FXML
    private void saveUser() {
        if (adminManager == null) {
            showValidationMessage("User storage is not available.");
            return;
        }

        if (!validateUserEditor()) {
            return;
        }

        AdminManager.UserInput userInput = createUserInputFromEditor();
        User savedUser;

        try {
            if (editingUser == null) {
                savedUser = adminManager.createUser(userInput);
                showUserActionMessage(savedUser.getName() + " was created.");
            } else {
                savedUser = adminManager.updateUser(editingUser.getId(), userInput);
                showUserActionMessage(savedUser.getName() + " was updated.");
            }
        } catch (IllegalArgumentException exception) {
            showValidationMessage(exception.getMessage());
            return;
        } catch (DataAccessException exception) {
            showValidationMessage("User could not be saved. Check the database connection and role setup.");
            return;
        }

        loadUsers();
        applyFilters();
        showOverview();
    }

    @FXML
    private void generateTemporaryPassword() {
        temporaryPasswordField.setText(generatePassword());
        setVisibleAndManaged(generatedPasswordNoticeLabel, true);
    }

    @FXML
    private void showProfileAccessForAdmin() {
        adminProfileAccessExpanded = true;
        updateProfileAccessMode();
    }

    private void configureRoleFilter() {
        roleFilterComboBox.getItems().setAll(ALL_ROLES, ROLE_ADMIN, ROLE_USER);
        roleFilterComboBox.setValue(ALL_ROLES);
    }

    private void configureUserEditor() {
        userRoleComboBox.getItems().setAll(ROLE_USER, ROLE_ADMIN);
        userStatusComboBox.getItems().setAll(STATUS_ACTIVE, STATUS_INACTIVE);

        if (adminManager != null) {
            refreshProfileAccessControls();
        } else if (profileListBox != null) {
            profileListBox.getChildren().clear();
        }

        userRoleComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (ROLE_ADMIN.equals(newValue) && !ROLE_ADMIN.equals(oldValue)) {
                adminProfileAccessExpanded = false;
            }

            updateProfileAccessMode();
        });

        profileSearchField.textProperty().addListener((observable, oldValue, newValue) ->
                filterProfileAccessRows(newValue)
        );

        final String[] lastGeneratedUsername = {""};

        fullNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (editingUser != null) {
                return;
            }

            String generatedUsername = generateUsernameFromName(newValue);

            if (usernameField.getText().isBlank() || usernameField.getText().equals(lastGeneratedUsername[0])) {
                usernameField.setText(generatedUsername);
            }

            lastGeneratedUsername[0] = generatedUsername;
        });
    }

    private void refreshProfileAccessControls() {
        if (adminManager == null) {
            profileControls.clear();
            if (profileListBox != null) {
                profileListBox.getChildren().clear();
            }
            return;
        }

        profileControls.clear();

        profileControls.addAll(loadProfileOptions().stream()
                .map(this::createProfileAccessControl)
                .toList());

        profileListBox.getChildren().setAll(
                profileControls.stream()
                        .map(ProfileAccessControl::checkBox)
                        .toList()
        );

        for (ProfileAccessControl control : profileControls) {
            control.checkBox().selectedProperty().addListener((observable, oldValue, newValue) ->
                    updateNoProfilesWarning()
            );
        }

        filterProfileAccessRows(profileSearchField == null ? "" : profileSearchField.getText());
        updateNoProfilesWarning();
    }

    private void configureRowsPerPageSelector() {
        rowsPerPageComboBox.getItems().setAll(ROWS_PER_PAGE_OPTIONS);
        rowsPerPageComboBox.setValue(DEFAULT_ROWS_PER_PAGE);

        rowsPerPageComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue <= 0) {
                return;
            }

            rowsPerPage = newValue;
            currentPage = 1;
            renderUsers();
        });
    }

    private void configureFiltering() {
        filteredUsers = new FilteredList<>(masterUsers, user -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        roleFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void showEditUserEditor(User user) {
        editingUser = user;
        resetUserEditor();

        userEditorTitleLabel.setText("Edit User");
        userEditorSubtitleLabel.setText("Update the user's details and profile access.");
        saveUserButton.setText("Save Changes");
        temporaryPasswordLabel.setText("Temporary Password");
        temporaryPasswordField.setPromptText("Leave blank to keep current password");

        fullNameField.setText(user.getName());
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        userRoleComboBox.setValue(user.getRole());
        userStatusComboBox.setValue(user.getStatus());
        selectAssignedProfiles(user.getAssignedProfiles());
        adminProfileAccessExpanded = ROLE_ADMIN.equalsIgnoreCase(user.getRole())
                && !user.getAssignedProfiles().isEmpty();

        updateProfileAccessMode();
        showEditor();
    }

    private void showEditor() {
        hideUserActionMessage();
        setVisibleAndManaged(overviewPane, false);
        setVisibleAndManaged(userEditorPane, true);
        userEditorPane.setVvalue(0);
    }

    private void resetUserEditor() {
        fullNameField.clear();
        usernameField.clear();
        emailField.clear();
        temporaryPasswordField.clear();
        profileSearchField.clear();
        userRoleComboBox.setValue(ROLE_USER);
        userStatusComboBox.setValue(STATUS_ACTIVE);
        selectAssignedProfiles(List.of());

        setVisibleAndManaged(generatedPasswordNoticeLabel, false);
        setVisibleAndManaged(validationLabel, false);
        validationLabel.setText("");
        filterProfileAccessRows("");
        updateProfileAccessMode();
    }

    private void updateProfileAccessMode() {
        boolean roleIsAdmin = ROLE_ADMIN.equals(userRoleComboBox.getValue());

        if (roleIsAdmin) {
            profileHelpLabel.setText("Admins can manage users, profiles, review details, access, and activity logs. Profile access is optional if this admin will also scan.");
            setVisibleAndManaged(showProfileAccessButton, !adminProfileAccessExpanded);
            setVisibleAndManaged(profileAccessContent, adminProfileAccessExpanded);
        } else {
            profileHelpLabel.setText("Choose which scan profiles this user can access.");
            setVisibleAndManaged(showProfileAccessButton, false);
            setVisibleAndManaged(profileAccessContent, true);
        }

        updateNoProfilesWarning();
    }

    private void updateNoProfilesWarning() {
        boolean roleIsUser = ROLE_USER.equals(userRoleComboBox.getValue());
        boolean noProfilesSelected = getSelectedProfileNames().isEmpty();
        setVisibleAndManaged(noProfilesWarningLabel, roleIsUser && noProfilesSelected);
    }

    private void filterProfileAccessRows(String searchText) {
        String normalizedSearch = Strings.normalize(searchText);

        for (ProfileAccessControl control : profileControls) {
            boolean matches = normalizedSearch.isBlank()
                    || Strings.normalize(control.profile().name()).contains(normalizedSearch)
                    || Strings.normalize(control.profile().status()).contains(normalizedSearch);

            setVisibleAndManaged(control.checkBox(), matches);
        }
    }

    private ProfileAccessControl createProfileAccessControl(ProfileOption profile) {
        Label nameLabel = new Label(profile.name());
        nameLabel.getStyleClass().add("create-user-profile-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox row = new CheckBox();

        HBox content = new HBox(12, nameLabel, spacer);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMinWidth(0);
        content.setMaxWidth(Double.MAX_VALUE);
        content.getStyleClass().add("create-user-profile-content");
        content.prefWidthProperty().bind(row.widthProperty().subtract(66));

        row.setGraphic(content);
        row.getStyleClass().add("create-user-profile-row");
        row.setFocusTraversable(true);
        row.setMaxWidth(Double.MAX_VALUE);

        return new ProfileAccessControl(profile, row);
    }

    private boolean validateUserEditor() {
        List<String> errors = new ArrayList<>();

        String fullName = Strings.clean(fullNameField.getText());
        String username = Strings.clean(usernameField.getText());
        String email = Strings.clean(emailField.getText());
        String password = Strings.clean(temporaryPasswordField.getText());

        if (fullName.isBlank()) {
            errors.add("Full name is required.");
        }

        if (username.isBlank()) {
            errors.add("Username is required.");
        } else if (usernameAlreadyExists(username, editingUser)) {
            errors.add("Username must be unique.");
        }

        if (!email.isBlank() && !isValidEmail(email)) {
            errors.add("Email must be valid if entered.");
        }

        if (editingUser == null && password.isBlank()) {
            errors.add("Temporary password is required.");
        }

        if (userRoleComboBox.getValue() == null) {
            errors.add("Role is required.");
        }

        if (userStatusComboBox.getValue() == null) {
            errors.add("Status is required.");
        }

        boolean hasErrors = !errors.isEmpty();

        validationLabel.setText(String.join("\n", errors));
        setVisibleAndManaged(validationLabel, hasErrors);

        return !hasErrors;
    }

    private AdminManager.UserInput createUserInputFromEditor() {
        String password = Strings.clean(temporaryPasswordField.getText());
        Boolean mustChangePassword = password.isBlank() ? null : Boolean.TRUE;
        return new AdminManager.UserInput(
                Strings.clean(fullNameField.getText()),
                Strings.clean(usernameField.getText()),
                Strings.clean(emailField.getText()),
                userRoleComboBox.getValue(),
                userStatusComboBox.getValue(),
                getSelectedProfileNames(),
                password,
                mustChangePassword
        );
    }

    private List<String> getSelectedProfileNames() {
        return profileControls.stream()
                .filter(control -> control.checkBox().isSelected())
                .map(control -> control.profile().name())
                .toList();
    }

    private void selectAssignedProfiles(List<String> assignedProfiles) {
        for (ProfileAccessControl control : profileControls) {
            control.checkBox().setSelected(assignedProfiles.contains(control.profile().name()));
        }
    }

    private void deactivateUser(User user) {
        if (adminManager == null) {
            showUserActionMessage("User storage is not available.");
            return;
        }

        try {
            adminManager.deactivateUser(user.getId());
        } catch (IllegalArgumentException exception) {
            showUserActionMessage(exception.getMessage());
            return;
        } catch (DataAccessException exception) {
            showUserActionMessage("User could not be deactivated. Check the database connection.");
            return;
        }

        loadUsers();
        applyFilters();
        showUserActionMessage(user.getName() + " was deactivated.");
    }

    private void reactivateUser(User user) {
        if (adminManager == null) {
            showUserActionMessage("User storage is not available.");
            return;
        }

        try {
            adminManager.reactivateUser(user.getId());
        } catch (DataAccessException exception) {
            showUserActionMessage("User could not be reactivated. Check the database connection.");
            return;
        }

        loadUsers();
        applyFilters();
        showUserActionMessage(user.getName() + " was reactivated.");
    }

    private void applyFilters() {
        currentPage = 1;

        String searchText = Strings.normalize(searchField.getText());
        String selectedRole = roleFilterComboBox.getValue();

        filteredUsers.setPredicate(user ->
                matchesSelectedRole(user, selectedRole)
                        && matchesSearch(user, searchText)
        );

        renderUsers();
    }

    private boolean matchesSelectedRole(User user, String selectedRole) {
        return selectedRole == null
                || ALL_ROLES.equals(selectedRole)
                || user.getRole().equalsIgnoreCase(selectedRole);
    }

    private boolean matchesSearch(User user, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return Strings.normalize(user.getName()).contains(searchText)
                || Strings.normalize(user.getUsername()).contains(searchText)
                || Strings.normalize(user.getEmail()).contains(searchText)
                || Strings.normalize(user.getRole()).contains(searchText)
                || Strings.normalize(user.getStatus()).contains(searchText)
                || Strings.normalize(searchableProfileText(user)).contains(searchText);
    }

    private void renderUsers() {
        List<User> visibleUsers = filteredUsers.stream().toList();

        int totalUsers = visibleUsers.size();
        PaginationHelper.PageSlice pageSlice = PaginationHelper.slice(currentPage, rowsPerPage, totalUsers);

        currentPage = pageSlice.currentPage();

        List<User> pageUsers = visibleUsers.subList(pageSlice.fromIndex(), pageSlice.toIndex());

        userListContainer.getChildren().setAll(
                pageUsers.stream()
                        .map(this::buildUserRecord)
                        .toList()
        );

        updateEmptyState(totalUsers);

        usersCountLabel.setText(formatUserCount(totalUsers));
        PaginationHelper.renderInto(paginationButtonsBox, paginationSummaryLabel, pageSlice,
                totalUsers, "users", page -> {
                    currentPage = page;
                    renderUsers();
                });
    }

    private void updateEmptyState(int totalUsers) {
        boolean hasUsers = totalUsers > 0;

        userListContainer.setVisible(hasUsers);
        userListContainer.setManaged(hasUsers);

        emptyStateBox.setVisible(!hasUsers);
        emptyStateBox.setManaged(!hasUsers);

        paginationBar.setVisible(hasUsers);
        paginationBar.setManaged(hasUsers);
    }

    private GridPane buildUserRecord(User user) {
        GridPane row = createRecordGrid();
        row.getStyleClass().add("user-record");
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(userListContainer.widthProperty());

        addCell(row, buildNameCell(user), 0, HPos.LEFT);
        addCell(row, buildCenteredTextCell(user.getUsername()), 1, HPos.CENTER);
        addCell(row, buildCenteredTextCell(user.getEmail()), 2, HPos.CENTER);
        addCell(row, buildRoleCell(user), 3, HPos.CENTER);
        addCell(row, buildStatusCell(user), 4, HPos.CENTER);
        addCell(row, buildCenteredTextCell(profileAccessSummary(user)), 5, HPos.CENTER);
        addCell(row, buildActionsCell(user), 6, HPos.CENTER);

        return row;
    }

    private GridPane createRecordGrid() {
        GridPane grid = new GridPane();

        grid.getColumnConstraints().setAll(
                createPercentColumn(NAME_COLUMN_WIDTH),
                createPercentColumn(USERNAME_COLUMN_WIDTH),
                createPercentColumn(EMAIL_COLUMN_WIDTH),
                createPercentColumn(ROLE_COLUMN_WIDTH),
                createPercentColumn(STATUS_COLUMN_WIDTH),
                createPercentColumn(PROFILES_COLUMN_WIDTH),
                createPercentColumn(ACTIONS_COLUMN_WIDTH)
        );

        return grid;
    }

    private ColumnConstraints createPercentColumn(double percentWidth) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percentWidth);
        column.setHgrow(Priority.ALWAYS);
        column.setFillWidth(true);
        return column;
    }

    private void addCell(GridPane row, Node content, int columnIndex, HPos horizontalAlignment) {
        GridPane.setHalignment(content, horizontalAlignment);
        GridPane.setValignment(content, VPos.CENTER);
        row.add(content, columnIndex, 0);
    }

    private HBox buildNameCell(User user) {
        Label avatar = new Label(Strings.initials(user.getName(), ""));
        avatar.getStyleClass().add("user-avatar-initials");

        Label nameLabel = createLeftTableLabel(user.getName(), "table-cell-text");

        HBox nameCell = new HBox(9, avatar, nameLabel);
        nameCell.getStyleClass().add("name-cell");
        nameCell.setAlignment(Pos.CENTER_LEFT);

        return nameCell;
    }

    private Label createLeftTableLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setAlignment(Pos.CENTER_LEFT);
        return label;
    }

    private StackPane buildCenteredTextCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-cell-text-muted");
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setAlignment(Pos.CENTER);

        StackPane wrapper = new StackPane(label);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        return wrapper;
    }

    private StackPane buildRoleCell(User user) {
        Label roleBadge = new Label(user.getRole());
        roleBadge.getStyleClass().addAll(
                "role-badge",
                ROLE_ADMIN.equalsIgnoreCase(user.getRole())
                        ? "role-badge-admin"
                        : "role-badge-user"
        );

        StackPane wrapper = new StackPane(roleBadge);
        wrapper.getStyleClass().add("role-cell");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        return wrapper;
    }

    private StackPane buildStatusCell(User user) {
        Label statusBadge = new Label(user.getStatus());
        statusBadge.getStyleClass().addAll(
                "user-status-badge",
                user.isActive() ? "user-status-active" : "user-status-inactive"
        );

        StackPane wrapper = new StackPane(statusBadge);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        return wrapper;
    }

    private HBox buildActionsCell(User user) {
        HBox actionBox = new HBox(12);
        actionBox.getStyleClass().add("inline-actions");
        actionBox.setAlignment(Pos.CENTER);

        Button editButton = createInlineActionButton("Edit", EDIT_ICON, "edit-link-button", "edit-link-icon");
        editButton.setOnAction(event -> showEditUserEditor(user));
        actionBox.getChildren().add(editButton);

        if (!user.isCurrentUser() && user.isActive()) {
            Button deactivateButton = createInlineActionButton("Deactivate", DEACTIVATE_ICON, "deactivate-link-button", "deactivate-link-icon");
            deactivateButton.setOnAction(event -> deactivateUser(user));
            actionBox.getChildren().add(deactivateButton);
        } else if (!user.isCurrentUser()) {
            Button reactivateButton = createInlineActionButton("Reactivate", REACTIVATE_ICON, "reactivate-link-button", "reactivate-link-icon");
            reactivateButton.setOnAction(event -> reactivateUser(user));
            actionBox.getChildren().add(reactivateButton);
        }

        return actionBox;
    }

    private Button createInlineActionButton(String text, String iconPath, String buttonClass, String iconClass) {
        Button button = new Button(text);
        button.getStyleClass().add(buttonClass);
        button.setFocusTraversable(false);
        button.setGraphic(createActionIcon(iconPath, iconClass));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(6);
        return button;
    }

    private StackPane createActionIcon(String glyph, String iconStyleClass) {
        Label icon = PrimeIcons.create(glyph, iconStyleClass);

        StackPane shell = new StackPane(icon);
        shell.getStyleClass().add("action-icon-shell");

        return shell;
    }

    private boolean usernameAlreadyExists(String username, User userBeingEdited) {
        Integer excludedUserId = userBeingEdited == null ? null : userBeingEdited.getId();
        return adminManager.usernameExists(username, excludedUserId);
    }

    private void loadUsers() {
        if (adminManager == null) {
            masterUsers.clear();
            return;
        }

        masterUsers.setAll(adminManager.getUsers());
    }

    private List<ProfileOption> loadProfileOptions() {
        if (adminManager == null) {
            return List.of();
        }

        return adminManager.getProfiles().stream()
                .map(profile -> new ProfileOption(profile.getName(), profile.getStatus()))
                .toList();
    }

    private String profileAccessSummary(User user) {
        if (ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            return "All profiles";
        }

        int profileCount = user.getAssignedProfiles().size();

        if (profileCount == 0) {
            return "0 profiles assigned";
        }

        if (profileCount == 1) {
            return "1 profile assigned";
        }

        return profileCount + " profiles assigned";
    }

    private String searchableProfileText(User user) {
        return profileAccessSummary(user) + " " + String.join(" ", user.getAssignedProfiles());
    }

    private String formatUserCount(int count) {
        return count == 1 ? "1 user" : count + " users";
    }

    private void showUserActionMessage(String message) {
        userActionMessageLabel.setText(message);
        setVisibleAndManaged(userActionMessageLabel, true);
    }

    private void hideUserActionMessage() {
        userActionMessageLabel.setText("");
        setVisibleAndManaged(userActionMessageLabel, false);
    }

    private void showValidationMessage(String message) {
        validationLabel.setText(message);
        setVisibleAndManaged(validationLabel, true);
    }

    private boolean isValidEmail(String email) {
        int atIndex = email.indexOf("@");
        int dotIndex = email.lastIndexOf(".");

        return atIndex > 0
                && dotIndex > atIndex + 1
                && dotIndex < email.length() - 1;
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder();

        for (int index = 0; index < 12; index++) {
            int characterIndex = PASSWORD_RANDOM.nextInt(PASSWORD_CHARACTERS.length());
            password.append(PASSWORD_CHARACTERS.charAt(characterIndex));
        }

        return password.toString();
    }

    private String generateUsernameFromName(String fullName) {
        String cleanedName = Strings.clean(fullName);

        if (cleanedName.isBlank()) {
            return "";
        }

        String firstName = cleanedName.split("\\s+")[0];

        return firstName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private void setVisibleAndManaged(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private record ProfileOption(String name, String status) {
    }

    private record ProfileAccessControl(ProfileOption profile, CheckBox checkBox) {
    }
}
