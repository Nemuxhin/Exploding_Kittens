package easv.gui.controller.admin;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

final class CreateUserDialog {

    private static final String ROLE_ADMIN = "Admin";
    private static final String ROLE_USER = "User";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_INACTIVE = "Inactive";

    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private final Node ownerNode;
    private final Predicate<String> usernameExists;

    CreateUserDialog(Node ownerNode, Predicate<String> usernameExists) {
        this.ownerNode = ownerNode;
        this.usernameExists = usernameExists;
    }

    Optional<ManageUsersController.UserRow> showAndWait() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Create User");
        dialog.initModality(Modality.APPLICATION_MODAL);

        if (ownerNode.getScene() != null) {
            dialog.initOwner(ownerNode.getScene().getWindow());
        }

        ButtonType cancelButtonType = ButtonType.CANCEL;
        ButtonType createButtonType = new ButtonType("Create User", ButtonBar.ButtonData.OK_DONE);

        DialogPane dialogPane = dialog.getDialogPane();
        applyDialogStyles(dialogPane);
        dialogPane.setPrefWidth(780);
        dialogPane.setMinWidth(720);
        dialogPane.getButtonTypes().setAll(cancelButtonType, createButtonType);

        TextField fullNameField = createTextField("Sarah Smith");
        TextField usernameField = createTextField("sarah");
        TextField emailField = createTextField("sarah@example.com");
        TextField temporaryPasswordField = createTextField("Temporary password");

        ComboBox<String> roleComboBox = createComboBox(List.of(ROLE_USER, ROLE_ADMIN), ROLE_USER);
        ComboBox<String> statusComboBox = createComboBox(List.of(STATUS_ACTIVE, STATUS_INACTIVE), STATUS_ACTIVE);

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

        TextField profileSearchField = createTextField("Search profiles...");

        VBox profileListBox = new VBox(0);
        profileListBox.getStyleClass().add("create-user-profile-list");

        List<ProfileAccessControl> profileControls = AdminDemoData.createUserProfileOptions().stream()
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
        userDetailsGrid.add(buildField("Full Name *", fullNameField), 0, 0);
        userDetailsGrid.add(buildField("Username *", usernameField), 1, 0);
        userDetailsGrid.add(buildField("Email", emailField), 0, 1);
        userDetailsGrid.add(buildField("Role *", roleComboBox), 1, 1);
        userDetailsGrid.add(buildField("Temporary Password *", passwordRow), 0, 2);
        userDetailsGrid.add(buildField("Status", statusComboBox), 1, 2);

        VBox userDetailsSection = new VBox(
                12,
                createSectionTitle("User Details"),
                userDetailsGrid,
                generatedPasswordNotice
        );
        userDetailsSection.getStyleClass().add("create-user-section");

        VBox profileAccessSection = new VBox(
                12,
                createSectionTitle("Profile Access"),
                profileHelpLabel,
                showProfileAccessButton,
                profileAccessContent,
                noProfilesWarningLabel
        );
        profileAccessSection.getStyleClass().add("create-user-section");

        VBox content = new VBox(
                18,
                buildHeader(),
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

        ManageUsersController.UserRow[] createdUser = new ManageUsersController.UserRow[1];

        createButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validateForm(
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

        return Optional.ofNullable(createdUser[0]);
    }

    private VBox buildHeader() {
        Label title = new Label("Create User");
        title.getStyleClass().add("create-user-modal-title");

        Label subtitle = new Label("Add a new user and choose what they can access.");
        subtitle.getStyleClass().add("create-user-modal-subtitle");
        subtitle.setWrapText(true);

        VBox header = new VBox(3, title, subtitle);
        header.getStyleClass().add("create-user-modal-header");
        return header;
    }

    private Label createSectionTitle(String text) {
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

    private VBox buildField(String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("create-user-field-label");

        VBox wrapper = new VBox(6, label, field);
        wrapper.getStyleClass().add("create-user-field");

        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }

        return wrapper;
    }

    private TextField createTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().add("create-user-input");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private ComboBox<String> createComboBox(List<String> values, String defaultValue) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(values);
        comboBox.setValue(defaultValue);
        comboBox.getStyleClass().add("create-user-input");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return comboBox;
    }

    private ProfileAccessControl createProfileAccessControl(AdminDemoData.ProfileOption profile) {
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
        Runnable toggleProfile = () -> checkBox.setSelected(!checkBox.isSelected());

        row.setOnMouseClicked(event -> toggleProfile.run());
        AdminKeyboard.makeActivatable(row, "Toggle profile access " + profile.name(), toggleProfile);

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

    private boolean validateForm(
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
        } else if (usernameExists.test(username)) {
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

        if (ownerNode.getScene() != null) {
            alert.initOwner(ownerNode.getScene().getWindow());
        }

        applyDialogStyles(alert.getDialogPane());

        ButtonType goBackButton = new ButtonType("Go Back", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType createAnywayButton = new ButtonType("Create Anyway", ButtonBar.ButtonData.OK_DONE);

        alert.getButtonTypes().setAll(goBackButton, createAnywayButton);

        return alert.showAndWait()
                .filter(createAnywayButton::equals)
                .isPresent();
    }

    private ManageUsersController.UserRow createUserFromForm(
            TextField fullNameField,
            TextField usernameField,
            TextField emailField,
            ComboBox<String> roleComboBox,
            ComboBox<String> statusComboBox,
            List<String> selectedProfileNames
    ) {
        return new ManageUsersController.UserRow(
                clean(fullNameField.getText()),
                clean(usernameField.getText()),
                clean(emailField.getText()),
                roleComboBox.getValue(),
                statusComboBox.getValue(),
                selectedProfileNames,
                false
        );
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
        AdminDialogStyler.apply(dialogPane, ownerNode);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ProfileAccessControl(
            AdminDemoData.ProfileOption profile,
            HBox row,
            CheckBox checkBox
    ) {
    }
}
