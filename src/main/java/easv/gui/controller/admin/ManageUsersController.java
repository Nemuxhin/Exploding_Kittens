package easv.gui.controller.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;

import java.net.URL;
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

    private static final int DEFAULT_ROWS_PER_PAGE = 10;
    private static final List<Integer> ROWS_PER_PAGE_OPTIONS = List.of(10, 25, 50);

    private static final double NAME_COLUMN_WIDTH = 20;
    private static final double USERNAME_COLUMN_WIDTH = 15;
    private static final double EMAIL_COLUMN_WIDTH = 25;
    private static final double ROLE_COLUMN_WIDTH = 10;
    private static final double PROFILES_COLUMN_WIDTH = 15;
    private static final double ACTIONS_COLUMN_WIDTH = 15;

    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private static final String EDIT_ICON_PATH =
            "M4 17.25V20h2.75l8.12-8.12-2.75-2.75L4 17.25zm11.71-9.04a.996.996 0 0 0 0-1.41l-1.5-1.5a.996.996 0 1 0-1.41 1.41l1.5 1.5a.996.996 0 0 0 1.41 0z";

    private static final String DELETE_ICON_PATH =
            "M6 7h12l-1 13H7L6 7zm3-3h6l1 2h4v2H4V6h4l1-2z";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterComboBox;

    @FXML private Label usersCountLabel;
    @FXML private VBox userListContainer;
    @FXML private VBox emptyStateBox;

    @FXML private HBox paginationBar;
    @FXML private Label paginationSummaryLabel;
    @FXML private HBox paginationButtonsBox;
    @FXML private ComboBox<Integer> rowsPerPageComboBox;

    private final ObservableList<UserRow> masterUsers = FXCollections.observableArrayList();

    private FilteredList<UserRow> filteredUsers;
    private int currentPage = 1;
    private int rowsPerPage = DEFAULT_ROWS_PER_PAGE;

    @FXML
    private void initialize() {
        configureRoleFilter();
        configureRowsPerPageSelector();
        loadSampleUsers();
        configureFiltering();
        applyFilters();
    }

    @FXML
    private void openCreateUserDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Create User");
        dialog.initModality(Modality.APPLICATION_MODAL);

        if (userListContainer.getScene() != null) {
            dialog.initOwner(userListContainer.getScene().getWindow());
        }

        ButtonType cancelButtonType = ButtonType.CANCEL;
        ButtonType createButtonType = new ButtonType("Create User", ButtonBar.ButtonData.OK_DONE);

        DialogPane dialogPane = dialog.getDialogPane();
        applyDialogStyles(dialogPane);
        dialogPane.setPrefWidth(780);
        dialogPane.setMinWidth(720);
        dialogPane.getButtonTypes().setAll(cancelButtonType, createButtonType);

        TextField fullNameField = createModalTextField("Sarah Smith");
        TextField usernameField = createModalTextField("sarah");
        TextField emailField = createModalTextField("sarah@example.com");
        TextField temporaryPasswordField = createModalTextField("Temporary password");

        ComboBox<String> roleComboBox = createModalComboBox(List.of(ROLE_USER, ROLE_ADMIN), ROLE_USER);
        ComboBox<String> statusComboBox = createModalComboBox(List.of(STATUS_ACTIVE, STATUS_INACTIVE), STATUS_ACTIVE);

        Button generatePasswordButton = new Button("Generate");
        generatePasswordButton.getStyleClass().add("metadata-secondary-header-button");
        generatePasswordButton.setFocusTraversable(false);

        Label generatedPasswordNotice = new Label("Temporary password generated. Copy it before closing.");
        generatedPasswordNotice.getStyleClass().add("create-user-generated-note");
        setVisibleAndManaged(generatedPasswordNotice, false);

        Label validationLabel = new Label();
        validationLabel.getStyleClass().add("create-user-validation-message");
        validationLabel.setWrapText(true);
        setVisibleAndManaged(validationLabel, false);

        Label profileHelpLabel = new Label("Choose which scan profiles this user can access.");
        profileHelpLabel.getStyleClass().add("create-user-section-copy");
        profileHelpLabel.setWrapText(true);

        TextField profileSearchField = createModalTextField("Search profiles...");

        VBox profileListBox = new VBox(0);
        profileListBox.getStyleClass().add("create-user-profile-list");

        List<ProfileAccessControl> profileControls = loadAvailableProfiles().stream()
                .map(this::createProfileAccessControl)
                .toList();

        profileListBox.getChildren().setAll(
                profileControls.stream()
                        .map(ProfileAccessControl::row)
                        .toList()
        );

        Label noProfilesWarningLabel = new Label("This user has no assigned profiles and will not be able to start scans.");
        noProfilesWarningLabel.getStyleClass().add("create-user-warning-message");
        noProfilesWarningLabel.setWrapText(true);

        Button showProfileAccessButton = new Button("Show profile access");
        showProfileAccessButton.getStyleClass().add("metadata-action-button");
        showProfileAccessButton.setFocusTraversable(false);

        VBox profileAccessContent = new VBox(12, profileSearchField, profileListBox);
        profileAccessContent.getStyleClass().add("create-user-profile-content");

        boolean[] adminProfileAccessExpanded = {false};

        Runnable updateNoProfilesWarning = () -> {
            boolean roleIsUser = ROLE_USER.equals(roleComboBox.getValue());
            boolean noProfilesSelected = getSelectedProfileNames(profileControls).isEmpty();
            setVisibleAndManaged(noProfilesWarningLabel, roleIsUser && noProfilesSelected);
        };

        Runnable updateProfileAccessMode = () -> {
            boolean roleIsAdmin = ROLE_ADMIN.equals(roleComboBox.getValue());

            if (roleIsAdmin) {
                profileHelpLabel.setText("Admins can manage users, profiles, metadata, access, and activity logs. Profile access is optional if this admin will also scan.");
                setVisibleAndManaged(showProfileAccessButton, !adminProfileAccessExpanded[0]);
                setVisibleAndManaged(profileAccessContent, adminProfileAccessExpanded[0]);
            } else {
                profileHelpLabel.setText("Choose which scan profiles this user can access.");
                setVisibleAndManaged(showProfileAccessButton, false);
                setVisibleAndManaged(profileAccessContent, true);
            }

            updateNoProfilesWarning.run();
        };

        showProfileAccessButton.setOnAction(event -> {
            adminProfileAccessExpanded[0] = true;
            updateProfileAccessMode.run();
        });

        roleComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (ROLE_ADMIN.equals(newValue)) {
                adminProfileAccessExpanded[0] = false;
            }

            updateProfileAccessMode.run();
        });

        for (ProfileAccessControl control : profileControls) {
            control.checkBox().selectedProperty().addListener((observable, oldValue, newValue) ->
                    updateNoProfilesWarning.run()
            );
        }

        profileSearchField.textProperty().addListener((observable, oldValue, newValue) ->
                filterProfileAccessRows(profileControls, newValue)
        );

        final String[] lastGeneratedUsername = {""};

        fullNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            String generatedUsername = generateUsernameFromName(newValue);

            if (usernameField.getText().isBlank() || usernameField.getText().equals(lastGeneratedUsername[0])) {
                usernameField.setText(generatedUsername);
            }

            lastGeneratedUsername[0] = generatedUsername;
        });

        generatePasswordButton.setOnAction(event -> {
            temporaryPasswordField.setText(generateTemporaryPassword());
            setVisibleAndManaged(generatedPasswordNotice, true);
        });

        HBox passwordRow = new HBox(9, temporaryPasswordField, generatePasswordButton);
        passwordRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(temporaryPasswordField, Priority.ALWAYS);

        GridPane userDetailsGrid = createUserDetailsGrid();
        userDetailsGrid.add(buildModalField("Full Name *", fullNameField), 0, 0);
        userDetailsGrid.add(buildModalField("Username *", usernameField), 1, 0);
        userDetailsGrid.add(buildModalField("Email", emailField), 0, 1);
        userDetailsGrid.add(buildModalField("Role *", roleComboBox), 1, 1);
        userDetailsGrid.add(buildModalField("Temporary Password *", passwordRow), 0, 2);
        userDetailsGrid.add(buildModalField("Status", statusComboBox), 1, 2);

        VBox userDetailsSection = new VBox(
                12,
                createModalSectionTitle("User Details"),
                userDetailsGrid,
                generatedPasswordNotice
        );
        userDetailsSection.getStyleClass().add("create-user-section");

        VBox profileAccessSection = new VBox(
                12,
                createModalSectionTitle("Profile Access"),
                profileHelpLabel,
                showProfileAccessButton,
                profileAccessContent,
                noProfilesWarningLabel
        );
        profileAccessSection.getStyleClass().add("create-user-section");

        VBox content = new VBox(
                18,
                buildModalHeader(),
                userDetailsSection,
                profileAccessSection,
                validationLabel
        );
        content.getStyleClass().add("create-user-modal");

        dialogPane.setContent(content);

        Button createButton = (Button) dialogPane.lookupButton(createButtonType);
        createButton.getStyleClass().add("create-user-button");
        createButton.setFocusTraversable(false);

        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        cancelButton.getStyleClass().add("metadata-secondary-header-button");
        cancelButton.setFocusTraversable(false);

        UserRow[] createdUser = new UserRow[1];

        createButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validateCreateUserForm(
                    fullNameField,
                    usernameField,
                    emailField,
                    temporaryPasswordField,
                    roleComboBox,
                    statusComboBox,
                    validationLabel
            )) {
                event.consume();
                return;
            }

            List<String> selectedProfileNames = getSelectedProfileNames(profileControls);

            if (ROLE_USER.equals(roleComboBox.getValue()) && selectedProfileNames.isEmpty()) {
                boolean shouldCreateAnyway = confirmCreateUserWithoutProfiles();

                if (!shouldCreateAnyway) {
                    event.consume();
                    return;
                }
            }

            createdUser[0] = createUserFromForm(
                    fullNameField,
                    usernameField,
                    emailField,
                    roleComboBox,
                    statusComboBox,
                    selectedProfileNames
            );
        });

        updateProfileAccessMode.run();
        dialog.showAndWait();

        if (createdUser[0] != null) {
            masterUsers.add(createdUser[0]);
            applyFilters();
            showCreateUserSuccess(createdUser[0]);
        }
    }

    private void configureRoleFilter() {
        roleFilterComboBox.getItems().setAll(ALL_ROLES, ROLE_ADMIN, ROLE_USER);
        roleFilterComboBox.setValue(ALL_ROLES);
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

    private void applyFilters() {
        currentPage = 1;

        String searchText = normalize(searchField.getText());
        String selectedRole = roleFilterComboBox.getValue();

        filteredUsers.setPredicate(user ->
                matchesSelectedRole(user, selectedRole)
                        && matchesSearch(user, searchText)
        );

        renderUsers();
    }

    private boolean matchesSelectedRole(UserRow user, String selectedRole) {
        return selectedRole == null
                || ALL_ROLES.equals(selectedRole)
                || user.role().equalsIgnoreCase(selectedRole);
    }

    private boolean matchesSearch(UserRow user, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return normalize(user.name()).contains(searchText)
                || normalize(user.username()).contains(searchText)
                || normalize(user.email()).contains(searchText)
                || normalize(user.role()).contains(searchText)
                || normalize(user.status()).contains(searchText)
                || normalize(user.searchableProfileText()).contains(searchText);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private void renderUsers() {
        List<UserRow> visibleUsers = filteredUsers.stream().toList();

        int totalUsers = visibleUsers.size();
        int totalPages = calculateTotalPages(totalUsers);

        currentPage = clamp(currentPage, 1, totalPages);

        int fromIndex = Math.min((currentPage - 1) * rowsPerPage, totalUsers);
        int toIndex = Math.min(fromIndex + rowsPerPage, totalUsers);

        List<UserRow> pageUsers = visibleUsers.subList(fromIndex, toIndex);

        userListContainer.getChildren().setAll(
                pageUsers.stream()
                        .map(this::buildUserRecord)
                        .toList()
        );

        updateEmptyState(totalUsers);

        usersCountLabel.setText(formatUserCount(totalUsers));
        renderPagination(totalPages, totalUsers, fromIndex, toIndex);
    }

    private int calculateTotalPages(int totalUsers) {
        if (totalUsers == 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalUsers / rowsPerPage);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
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

    private GridPane buildUserRecord(UserRow user) {
        GridPane row = createRecordGrid();
        row.getStyleClass().add("user-record");
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(userListContainer.widthProperty());

        addCell(row, buildNameCell(user), 0, HPos.LEFT);
        addCell(row, buildCenteredTextCell(user.username()), 1, HPos.CENTER);
        addCell(row, buildCenteredTextCell(user.email()), 2, HPos.CENTER);
        addCell(row, buildRoleCell(user), 3, HPos.CENTER);
        addCell(row, buildCenteredTextCell(user.profileAccessSummary()), 4, HPos.CENTER);
        addCell(row, buildActionsCell(user), 5, HPos.CENTER);

        return row;
    }

    private GridPane createRecordGrid() {
        GridPane grid = new GridPane();

        grid.getColumnConstraints().setAll(
                createPercentColumn(NAME_COLUMN_WIDTH),
                createPercentColumn(USERNAME_COLUMN_WIDTH),
                createPercentColumn(EMAIL_COLUMN_WIDTH),
                createPercentColumn(ROLE_COLUMN_WIDTH),
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

    private HBox buildNameCell(UserRow user) {
        Label avatar = new Label(initialsFor(user.name()));
        avatar.getStyleClass().add("user-avatar-initials");

        Label nameLabel = createLeftTableLabel(user.name(), "table-cell-text");

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

    private StackPane buildRoleCell(UserRow user) {
        Label roleBadge = new Label(user.role());
        roleBadge.getStyleClass().addAll(
                "role-badge",
                ROLE_ADMIN.equalsIgnoreCase(user.role())
                        ? "role-badge-admin"
                        : "role-badge-user"
        );

        StackPane wrapper = new StackPane(roleBadge);
        wrapper.getStyleClass().add("role-cell");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        return wrapper;
    }

    private HBox buildActionsCell(UserRow user) {
        HBox actionBox = new HBox(12);
        actionBox.getStyleClass().add("inline-actions");
        actionBox.setAlignment(Pos.CENTER);

        actionBox.getChildren().add(
                createInlineActionButton("Edit", EDIT_ICON_PATH, "edit-link-button", "edit-link-icon")
        );

        if (!user.currentUser()) {
            actionBox.getChildren().add(
                    createInlineActionButton("Delete", DELETE_ICON_PATH, "delete-link-button", "delete-link-icon")
            );
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

    private StackPane createActionIcon(String pathData, String iconStyleClass) {
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.getStyleClass().add(iconStyleClass);
        icon.setScaleX(0.68);
        icon.setScaleY(0.68);

        StackPane shell = new StackPane(icon);
        shell.getStyleClass().add("action-icon-shell");

        return shell;
    }

    private String initialsFor(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }

        String[] nameParts = fullName.trim().split("\\s+");

        if (nameParts.length == 1) {
            return nameParts[0]
                    .substring(0, Math.min(2, nameParts[0].length()))
                    .toUpperCase(Locale.ROOT);
        }

        return (nameParts[0].substring(0, 1) + nameParts[1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }

    private void renderPagination(int totalPages, int totalUsers, int fromIndex, int toIndex) {
        paginationButtonsBox.getChildren().clear();

        if (totalUsers == 0) {
            paginationSummaryLabel.setText("Showing 0 users");
            return;
        }

        paginationSummaryLabel.setText(formatPaginationSummary(fromIndex, toIndex, totalUsers));

        paginationButtonsBox.getChildren().add(createPaginationButton("<<", 1, currentPage == 1));
        paginationButtonsBox.getChildren().add(createPaginationButton("<", currentPage - 1, currentPage == 1));

        for (String pageItem : buildPageItems(totalPages)) {
            Node paginationItem = "...".equals(pageItem)
                    ? createPaginationEllipsis()
                    : createPaginationButton(pageItem, Integer.parseInt(pageItem), false);

            paginationButtonsBox.getChildren().add(paginationItem);
        }

        paginationButtonsBox.getChildren().add(createPaginationButton(">", currentPage + 1, currentPage == totalPages));
        paginationButtonsBox.getChildren().add(createPaginationButton(">>", totalPages, currentPage == totalPages));
    }

    private String formatPaginationSummary(int fromIndex, int toIndex, int totalUsers) {
        return "Showing " + (fromIndex + 1) + "-" + toIndex + " of " + totalUsers + " users";
    }

    private Label createPaginationEllipsis() {
        Label ellipsis = new Label("...");
        ellipsis.getStyleClass().add("pagination-ellipsis");
        return ellipsis;
    }

    private List<String> buildPageItems(int totalPages) {
        List<String> items = new ArrayList<>();

        if (totalPages <= 5) {
            for (int page = 1; page <= totalPages; page++) {
                items.add(String.valueOf(page));
            }

            return items;
        }

        if (currentPage <= 3) {
            items.add("1");
            items.add("2");
            items.add("3");
            items.add("...");
            items.add(String.valueOf(totalPages));
            return items;
        }

        if (currentPage >= totalPages - 2) {
            items.add("1");
            items.add("...");
            items.add(String.valueOf(totalPages - 2));
            items.add(String.valueOf(totalPages - 1));
            items.add(String.valueOf(totalPages));
            return items;
        }

        items.add("1");
        items.add("...");
        items.add(String.valueOf(currentPage - 1));
        items.add(String.valueOf(currentPage));
        items.add(String.valueOf(currentPage + 1));
        items.add("...");
        items.add(String.valueOf(totalPages));

        return items;
    }

    private Button createPaginationButton(String text, int targetPage, boolean disabled) {
        Button button = new Button(text);
        button.getStyleClass().add("pagination-button");
        button.setFocusTraversable(false);
        button.setDisable(disabled);

        boolean isCurrentPageButton = text.equals(String.valueOf(currentPage));

        if (isCurrentPageButton) {
            button.getStyleClass().add("pagination-button-active");
            return button;
        }

        if (!disabled) {
            button.setOnAction(event -> {
                currentPage = targetPage;
                renderUsers();
            });
        }

        return button;
    }

    private String formatUserCount(int count) {
        return count == 1 ? "1 user" : count + " users";
    }

    private VBox buildModalHeader() {
        Label title = new Label("Create User");
        title.getStyleClass().add("create-user-modal-title");

        Label subtitle = new Label("Add a new user and choose what they can access.");
        subtitle.getStyleClass().add("create-user-modal-subtitle");
        subtitle.setWrapText(true);

        VBox header = new VBox(3, title, subtitle);
        header.getStyleClass().add("create-user-modal-header");
        return header;
    }

    private Label createModalSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("create-user-section-title");
        return label;
    }

    private GridPane createUserDetailsGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("create-user-details-grid");
        grid.setHgap(12);
        grid.setVgap(12);

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPercentWidth(50);
        firstColumn.setHgrow(Priority.ALWAYS);
        firstColumn.setFillWidth(true);

        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPercentWidth(50);
        secondColumn.setHgrow(Priority.ALWAYS);
        secondColumn.setFillWidth(true);

        grid.getColumnConstraints().setAll(firstColumn, secondColumn);

        return grid;
    }

    private VBox buildModalField(String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("create-user-field-label");

        VBox wrapper = new VBox(6, label, field);
        wrapper.getStyleClass().add("create-user-field");

        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }

        return wrapper;
    }

    private TextField createModalTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().add("create-user-input");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private ComboBox<String> createModalComboBox(List<String> values, String defaultValue) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(values);
        comboBox.setValue(defaultValue);
        comboBox.getStyleClass().add("create-user-input");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return comboBox;
    }

    private ProfileAccessControl createProfileAccessControl(ProfileOption profile) {
        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("assignment-checkbox");
        checkBox.setFocusTraversable(false);

        Label nameLabel = new Label(profile.name());
        nameLabel.getStyleClass().add("create-user-profile-name");

        Label statusBadge = new Label(profile.status());
        statusBadge.getStyleClass().addAll(
                "metadata-profile-status",
                STATUS_ACTIVE.equalsIgnoreCase(profile.status())
                        ? "metadata-profile-status-active"
                        : "metadata-profile-status-draft"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, checkBox, nameLabel, spacer, statusBadge);
        row.getStyleClass().add("create-user-profile-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setOnMouseClicked(event -> checkBox.setSelected(!checkBox.isSelected()));

        checkBox.setOnMouseClicked(event -> event.consume());

        return new ProfileAccessControl(profile, row, checkBox);
    }

    private void filterProfileAccessRows(List<ProfileAccessControl> profileControls, String searchText) {
        String normalizedSearch = normalize(searchText);

        for (ProfileAccessControl control : profileControls) {
            boolean matches = normalizedSearch.isBlank()
                    || normalize(control.profile().name()).contains(normalizedSearch)
                    || normalize(control.profile().status()).contains(normalizedSearch);

            setVisibleAndManaged(control.row(), matches);
        }
    }

    private boolean validateCreateUserForm(
            TextField fullNameField,
            TextField usernameField,
            TextField emailField,
            TextField temporaryPasswordField,
            ComboBox<String> roleComboBox,
            ComboBox<String> statusComboBox,
            Label validationLabel
    ) {
        List<String> errors = new ArrayList<>();

        String fullName = clean(fullNameField.getText());
        String username = clean(usernameField.getText());
        String email = clean(emailField.getText());
        String password = clean(temporaryPasswordField.getText());

        if (fullName.isBlank()) {
            errors.add("Full name is required.");
        }

        if (username.isBlank()) {
            errors.add("Username is required.");
        } else if (usernameAlreadyExists(username)) {
            errors.add("Username must be unique.");
        }

        if (!email.isBlank() && !isValidEmail(email)) {
            errors.add("Email must be valid if entered.");
        }

        if (password.isBlank()) {
            errors.add("Temporary password is required.");
        }

        if (roleComboBox.getValue() == null) {
            errors.add("Role is required.");
        }

        if (statusComboBox.getValue() == null) {
            errors.add("Status is required.");
        }

        boolean hasErrors = !errors.isEmpty();

        validationLabel.setText(String.join("\n", errors));
        setVisibleAndManaged(validationLabel, hasErrors);

        return !hasErrors;
    }

    private boolean usernameAlreadyExists(String username) {
        String normalizedUsername = normalize(username);

        return masterUsers.stream()
                .map(UserRow::username)
                .map(this::normalize)
                .anyMatch(existingUsername -> existingUsername.equals(normalizedUsername));
    }

    private boolean isValidEmail(String email) {
        int atIndex = email.indexOf("@");
        int dotIndex = email.lastIndexOf(".");

        return atIndex > 0
                && dotIndex > atIndex + 1
                && dotIndex < email.length() - 1;
    }

    private boolean confirmCreateUserWithoutProfiles() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Create user without profile access?");
        alert.setHeaderText("Create user without profile access?");
        alert.setContentText("This user will be able to log in, but cannot start scanning until a profile is assigned.");

        if (userListContainer.getScene() != null) {
            alert.initOwner(userListContainer.getScene().getWindow());
        }

        applyDialogStyles(alert.getDialogPane());

        ButtonType goBackButton = new ButtonType("Go Back", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType createAnywayButton = new ButtonType("Create Anyway", ButtonBar.ButtonData.OK_DONE);

        alert.getButtonTypes().setAll(goBackButton, createAnywayButton);

        return alert.showAndWait()
                .filter(createAnywayButton::equals)
                .isPresent();
    }

    private UserRow createUserFromForm(
            TextField fullNameField,
            TextField usernameField,
            TextField emailField,
            ComboBox<String> roleComboBox,
            ComboBox<String> statusComboBox,
            List<String> selectedProfileNames
    ) {
        return new UserRow(
                clean(fullNameField.getText()),
                clean(usernameField.getText()),
                clean(emailField.getText()),
                roleComboBox.getValue(),
                statusComboBox.getValue(),
                selectedProfileNames,
                false
        );
    }

    private void showCreateUserSuccess(UserRow user) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User created");
        alert.setHeaderText("User created successfully.");
        alert.setContentText(
                user.name() + " can now log in with username \"" + user.username() + "\".\n"
                        + "Assigned profiles: " + user.successProfileText() + "."
        );

        if (userListContainer.getScene() != null) {
            alert.initOwner(userListContainer.getScene().getWindow());
        }

        applyDialogStyles(alert.getDialogPane());
        alert.showAndWait();
    }

    private List<String> getSelectedProfileNames(List<ProfileAccessControl> profileControls) {
        return profileControls.stream()
                .filter(control -> control.checkBox().isSelected())
                .map(control -> control.profile().name())
                .toList();
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder();

        for (int index = 0; index < 12; index++) {
            int characterIndex = PASSWORD_RANDOM.nextInt(PASSWORD_CHARACTERS.length());
            password.append(PASSWORD_CHARACTERS.charAt(characterIndex));
        }

        return password.toString();
    }

    private String generateUsernameFromName(String fullName) {
        String cleanedName = clean(fullName);

        if (cleanedName.isBlank()) {
            return "";
        }

        String firstName = cleanedName.split("\\s+")[0];

        return firstName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void setVisibleAndManaged(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void applyDialogStyles(DialogPane dialogPane) {
        URL stylesheetUrl = getClass().getResource("/css/app.css");

        if (stylesheetUrl != null && !dialogPane.getStylesheets().contains(stylesheetUrl.toExternalForm())) {
            dialogPane.getStylesheets().add(stylesheetUrl.toExternalForm());
        }

        dialogPane.getStyleClass().removeAll("app-shell", "dark", "create-user-dialog-pane");
        dialogPane.getStyleClass().addAll("app-shell", "create-user-dialog-pane");

        if (isApplicationInDarkMode()) {
            dialogPane.getStyleClass().add("dark");
        }
    }

    private boolean isApplicationInDarkMode() {
        return userListContainer != null
                && userListContainer.getScene() != null
                && userListContainer.getScene().getRoot() != null
                && userListContainer.getScene().getRoot().getStyleClass().contains("dark");
    }

    private List<ProfileOption> loadAvailableProfiles() {
        return List.of(
                new ProfileOption("Building Archive", STATUS_ACTIVE),
                new ProfileOption("Technical Drawings", STATUS_ACTIVE),
                new ProfileOption("Court Records", "Draft"),
                new ProfileOption("Standard Scan", STATUS_ACTIVE)
        );
    }

    private void loadSampleUsers() {
        masterUsers.setAll(
                new UserRow("John Doe", "john", "john@example.com", ROLE_ADMIN, STATUS_ACTIVE, List.of(), true),
                new UserRow("Sarah Smith", "sarah", "sarah@example.com", ROLE_USER, STATUS_ACTIVE, List.of("Building Archive", "Technical Drawings", "Standard Scan"), false),
                new UserRow("Michael Johnson", "michael", "michael@example.com", ROLE_USER, STATUS_ACTIVE, List.of("Building Archive", "Technical Drawings"), false),
                new UserRow("Emily Davis", "emily", "emily@example.com", ROLE_USER, STATUS_ACTIVE, List.of("Standard Scan"), false),
                new UserRow("David Wilson", "david", "david@example.com", ROLE_USER, STATUS_ACTIVE, List.of(), false),
                new UserRow("Olivia Brown", "olivia", "olivia@example.com", ROLE_USER, STATUS_ACTIVE, List.of("Building Archive", "Technical Drawings", "Court Records", "Standard Scan"), false),
                new UserRow("Lucas Andersen", "lucas", "lucas@example.com", ROLE_ADMIN, STATUS_ACTIVE, List.of(), false),
                new UserRow("Sofia Nielsen", "sofia", "sofia@example.com", ROLE_USER, STATUS_ACTIVE, List.of("Building Archive", "Standard Scan"), false)
        );
    }

    private record ProfileOption(
            String name,
            String status
    ) {
    }

    private record ProfileAccessControl(
            ProfileOption profile,
            HBox row,
            CheckBox checkBox
    ) {
    }

    private record UserRow(
            String name,
            String username,
            String email,
            String role,
            String status,
            List<String> assignedProfiles,
            boolean currentUser
    ) {
        private UserRow {
            assignedProfiles = assignedProfiles == null
                    ? List.of()
                    : List.copyOf(assignedProfiles);
        }

        private String profileAccessSummary() {
            if (ROLE_ADMIN.equalsIgnoreCase(role)) {
                return "All profiles";
            }

            int profileCount = assignedProfiles.size();

            if (profileCount == 0) {
                return "0 profiles assigned";
            }

            if (profileCount == 1) {
                return "1 profile assigned";
            }

            return profileCount + " profiles assigned";
        }

        private String searchableProfileText() {
            return profileAccessSummary() + " " + String.join(" ", assignedProfiles);
        }

        private String successProfileText() {
            if (ROLE_ADMIN.equalsIgnoreCase(role)) {
                return "All profiles";
            }

            if (assignedProfiles.isEmpty()) {
                return "0 profiles assigned";
            }

            return String.join(", ", assignedProfiles);
        }
    }
}