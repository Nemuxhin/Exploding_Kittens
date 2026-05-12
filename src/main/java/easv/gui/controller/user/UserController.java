package easv.gui.controller.user;

import easv.be.User;
import easv.bll.UserManager;
import easv.bll.UserSession;
import easv.gui.MainApp;
import easv.gui.UserPortalModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

public class UserController implements UserNavigator {

    private static final String ACTIVE_NAV_CLASS = "active";
    private static final String DARK_MODE_CLASS = "dark";

    private static final String PREFERENCES_NODE = "easv.gui.portal";
    private static final String DARK_MODE_PREFERENCE_KEY = "userPortal.darkMode";

    private static final String ACCOUNT_SECTION = "Edit Profile";

    private static final String LIGHT_MODE_LOGO =
            "/images/weblager/styleguide/Main Blue/LogoBlueH.png";

    private static final String DARK_MODE_LOGO =
            "/images/weblager/styleguide/DarkmodeBlue/LogoBlue2H.png";

    private static final String MOON_ICON_PATH =
            "M12 3.25a8.75 8.75 0 1 0 8.75 8.75c0-.45-.04-.89-.1-1.32A6.75 6.75 0 0 1 12.32 3.4c-.1-.05-.21-.1-.32-.15zM5.25 12A6.74 6.74 0 0 1 9.83 5.6a8.75 8.75 0 0 0 8.57 8.57A6.75 6.75 0 0 1 5.25 12z";

    private static final String SUN_ICON_PATH =
            "M12 5.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9zM11 1h2v3h-2V1zm0 19h2v3h-2v-3zM1 11h3v2H1v-2zm19 0h3v2h-3v-2zM4.22 2.81l2.12 2.12-1.41 1.41L2.81 4.22l1.41-1.41zm14.85 14.85 2.12 2.12-1.41 1.41-2.12-2.12 1.41-1.41zM19.78 2.81l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12zM4.93 17.66l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12z";

    private static final String HELP_SCAN_ICON_PATH =
            "M4 4h5v2H6v3H4V4zm11 0h5v5h-2V6h-3V4zM4 15h2v3h3v2H4v-5zm14 0h2v5h-5v-2h3v-3zM11 8h2v3h3v2h-3v3h-2v-3H8v-2h3V8z";

    private static final String HELP_QA_ICON_PATH =
            "M5 3h10l4 4v14H5V3zm2 2v14h10V8h-3V5H7zm2 5h6v1.5H9V10zm0 3h6v1.5H9V13zm0 3h4v1.5H9V16z";

    private static final String HELP_EXPORT_ICON_PATH =
            "M11 3h2v8h3l-4 4-4-4h3V3zM5 14h2v4h10v-4h2v6H5v-6z";

    private static final String HELP_SETTINGS_ICON_PATH =
            "M9 2h2l.5 2.1 1.9.8 1.8-1 1.4 1.4-1 1.8.8 1.9L18 9v2l-2.1.5-.8 1.9 1 1.8-1.4 1.4-1.8-1-.9 1.9L11 18H9l-.5-2.1-1.9-.8-1.8 1L3.4 14.7l1-1.8L3.6 11 2 10V8l2.1-.5.8-1.9-1-1.8L5.3 2.4l1.8 1 .9-1.9z M10 7a3 3 0 100 6 3 3 0 000-6z";

    @FXML private StackPane appShell;
    @FXML private BorderPane appRoot;
    @FXML private StackPane contentHost;

    @FXML private StackPane brandMark;
    @FXML private Label brandLogoFallbackLabel;
    @FXML private ImageView brandLogoImageView;

    @FXML private ToggleButton dashboardNavItem;
    @FXML private ToggleButton scanNavItem;
    @FXML private ToggleButton myScansNavItem;
    @FXML private ToggleButton assignedQANavItem;
    @FXML private ToggleButton exportsNavItem;

    @FXML private Button keyboardShortcutsButton;
    @FXML private Button helpButton;

    @FXML private Button accountMenuButton;
    @FXML private Label accountNameLabel;
    @FXML private Label accountInitialsLabel;
    @FXML private VBox accountDropdownPane;
    @FXML private Label accountDropdownNameLabel;
    @FXML private Label accountDropdownDetailLabel;
    @FXML private Button editProfileMenuButton;
    @FXML private Button settingsPrivacyMenuButton;
    @FXML private Button logoutMenuButton;
    @FXML private ToggleButton darkModeToggleButton;
    @FXML private SVGPath darkModeToggleIcon;

    private final UserPortalModel portalModel = new UserPortalModel();
    private final UserManager userManager = new UserManager();
    private final Preferences preferences = Preferences.userRoot().node(PREFERENCES_NODE);

    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        configureAccount();
        configureAccountMenu();
        configureKeyboardShortcutsButton();
        configureHelpButton();
        configureThemeToggle();
        configureNavigation();
        showPage(UserPage.DASHBOARD);
    }

    private void configureAccount() {
        UserPortalModel.AccountProfile fallbackProfile = portalModel.fetchAccountProfile();
        User currentUser = UserSession.getCurrentUser();

        String displayName = displayNameFor(currentUser, fallbackProfile);
        String accountDetail = accountDetailFor(currentUser);

        if (accountNameLabel != null) {
            accountNameLabel.setText(displayName);
        }

        if (accountInitialsLabel != null) {
            accountInitialsLabel.setText(initialsFor(displayName));
        }

        if (accountDropdownNameLabel != null) {
            accountDropdownNameLabel.setText(displayName);
        }

        if (accountDropdownDetailLabel != null) {
            accountDropdownDetailLabel.setText(accountDetail);
        }
    }

    private void configureAccountMenu() {
        if (accountDropdownPane != null) {
            accountDropdownPane.setMaxHeight(Region.USE_PREF_SIZE);
            accountDropdownPane.setVisible(false);
        }

        if (accountMenuButton != null) {
            accountMenuButton.setOnAction(event -> toggleAccountDropdown());
        }

        if (editProfileMenuButton != null) {
            editProfileMenuButton.setOnAction(event -> showAccountSettingsPage());
        }

        if (settingsPrivacyMenuButton != null) {
            settingsPrivacyMenuButton.setOnAction(event -> {
                hideAccountDropdown();
                showPage(UserPage.SETTINGS);
            });
        }

        if (logoutMenuButton != null) {
            logoutMenuButton.setOnAction(event -> logout());
        }
    }

    private void configureKeyboardShortcutsButton() {
        if (keyboardShortcutsButton != null) {
            keyboardShortcutsButton.setOnAction(event -> showKeyboardShortcutsDialog());
        }
    }

    private void configureHelpButton() {
        if (helpButton != null) {
            helpButton.setOnAction(event -> showHelpDialog());
        }
    }

    private void configureThemeToggle() {
        updateTheme(isDarkModeEnabled());

        if (darkModeToggleButton != null) {
            darkModeToggleButton.selectedProperty().addListener((observable, oldValue, isDark) ->
                    updateTheme(isDark)
            );
        }
    }

    private boolean isDarkModeEnabled() {
        return preferences.getBoolean(DARK_MODE_PREFERENCE_KEY, false);
    }

    private void updateTheme(boolean isDark) {
        updateDarkModeClass(isDark);
        updateBrandLogo(isDark);
        preferences.putBoolean(DARK_MODE_PREFERENCE_KEY, isDark);
        updateThemeControls(isDark);
    }

    private void updateDarkModeClass(boolean isDark) {
        if (appShell == null) {
            return;
        }

        appShell.getStyleClass().remove(DARK_MODE_CLASS);

        if (isDark) {
            appShell.getStyleClass().add(DARK_MODE_CLASS);
        }
    }

    private void updateBrandLogo(boolean isDark) {
        if (brandLogoImageView == null || brandLogoFallbackLabel == null) {
            return;
        }

        String logoPath = isDark ? DARK_MODE_LOGO : LIGHT_MODE_LOGO;
        URL logoUrl = getClass().getResource(logoPath);

        boolean logoExists = logoUrl != null;

        brandLogoImageView.setVisible(logoExists);
        brandLogoImageView.setManaged(logoExists);

        brandLogoFallbackLabel.setVisible(!logoExists);
        brandLogoFallbackLabel.setManaged(!logoExists);

        if (brandMark != null) {
            brandMark.setVisible(!logoExists);
            brandMark.setManaged(!logoExists);
        }

        if (logoExists) {
            brandLogoImageView.setImage(new Image(logoUrl.toExternalForm(), true));
        }
    }

    private void updateThemeControls(boolean isDark) {
        if (darkModeToggleButton != null) {
            if (darkModeToggleButton.isSelected() != isDark) {
                darkModeToggleButton.setSelected(isDark);
            }

            darkModeToggleButton.setText(isDark ? "Dark Mode" : "Light Mode");
        }

        if (darkModeToggleIcon != null) {
            darkModeToggleIcon.setContent(isDark ? MOON_ICON_PATH : SUN_ICON_PATH);
        }
    }

    private void configureNavigation() {
        for (UserPage page : UserPage.values()) {
            ToggleButton navItem = getNavItem(page);

            if (navItem != null) {
                navItem.setOnAction(event -> showPage(page));
            }
        }
    }

    @Override
    public void showPage(UserPage page) {
        hideAccountDropdown();
        loadPage(page);
        setActiveNavItem(getNavItem(page));
    }

    @Override
    public void resumeRecentScan(UserPortalModel.RecentScanItem item) {
        showPage(UserPage.SCAN);
    }

    @Override
    public void resumeHistoryScan(UserPortalModel.HistoryItem item) {
        showPage(UserPage.SCAN);
    }

    private void loadPage(UserPage page) {
        if (!page.hasFxml()) {
            contentHost.getChildren().setAll(wrapScrollable(createProgrammaticPage(page)));
            return;
        }

        URL pageUrl = getClass().getResource(page.fxmlPath());

        if (pageUrl == null) {
            contentHost.getChildren().setAll(createMissingPagePlaceholder(page.title()));
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(pageUrl);
            Parent loadedPage = loader.load();

            configureLoadedController(loader.getController());
            configureLoadedPageSize(loadedPage);

            contentHost.getChildren().setAll(loadedPage);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load page: " + page.fxmlPath(), exception);
        }
    }

    private Node createProgrammaticPage(UserPage page) {
        return switch (page) {
            case DASHBOARD -> new DashboardController(portalModel, this).create();
            case MY_SCANS -> new MyScansController(portalModel, this).create();
            case EXPORTS -> new ExportsController(portalModel).create();
            case SETTINGS -> new SettingsController(portalModel).create();
            default -> createMissingPagePlaceholder(page.title());
        };
    }

    private ScrollPane wrapScrollable(Node page) {
        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.getStyleClass().add("portal-page-scroll");
        scrollPane.setMaxWidth(Double.MAX_VALUE);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        return scrollPane;
    }

    private void configureLoadedController(Object controller) {
        if (controller instanceof ScanController scanController) {
            scanController.setNavigator(this);
        }
    }

    private void configureLoadedPageSize(Parent page) {
        if (!(page instanceof Region region)) {
            return;
        }

        region.setMinWidth(0);
        region.setMinHeight(0);
        region.setMaxWidth(Double.MAX_VALUE);
        region.setMaxHeight(Double.MAX_VALUE);
    }

    private VBox createMissingPagePlaceholder(String pageTitle) {
        Label titleLabel = new Label(pageTitle);
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("No content is available for this section.");
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox placeholder = new VBox(6, titleLabel, subtitleLabel);
        placeholder.getStyleClass().add("page-content");
        placeholder.setAlignment(Pos.TOP_LEFT);
        placeholder.setMaxWidth(Double.MAX_VALUE);
        placeholder.setMaxHeight(Double.MAX_VALUE);

        return placeholder;
    }

    private void setActiveNavItem(ToggleButton activeNavItem) {
        for (ToggleButton navItem : getNavigationItems()) {
            if (navItem != null) {
                setNavItemActive(navItem, navItem == activeNavItem);
            }
        }
    }

    private void setNavItemActive(ToggleButton navItem, boolean active) {
        navItem.setSelected(active);
        navItem.getStyleClass().remove(ACTIVE_NAV_CLASS);

        if (active) {
            navItem.getStyleClass().add(ACTIVE_NAV_CLASS);
        }
    }

    private List<ToggleButton> getNavigationItems() {
        return List.of(
                dashboardNavItem,
                scanNavItem,
                myScansNavItem,
                assignedQANavItem,
                exportsNavItem
        );
    }

    private ToggleButton getNavItem(UserPage page) {
        return switch (page) {
            case DASHBOARD -> dashboardNavItem;
            case SCAN -> scanNavItem;
            case MY_SCANS -> myScansNavItem;
            case ASSIGNED_QA -> assignedQANavItem;
            case EXPORTS -> exportsNavItem;
            case EDIT_PROFILE -> null;
            case SETTINGS -> null;
        };
    }

    private void toggleAccountDropdown() {
        if (accountDropdownPane == null) {
            return;
        }

        boolean shouldShow = !accountDropdownPane.isVisible();
        accountDropdownPane.setVisible(shouldShow);

        if (shouldShow) {
            accountDropdownPane.toFront();
        }
    }

    private void hideAccountDropdown() {
        if (accountDropdownPane != null) {
            accountDropdownPane.setVisible(false);
        }
    }

    private void showAccountSettingsPage() {
        hideAccountDropdown();
        setActiveNavItem(null);
        contentHost.getChildren().setAll(wrapScrollable(createAccountSettingsPage()));
    }

    private VBox createAccountSettingsPage() {
        Label titleLabel = new Label(ACCOUNT_SECTION);
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("Manage your account information and password.");
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox intro = new VBox(3, titleLabel, subtitleLabel);
        intro.getStyleClass().add("page-heading-copy");

        VBox detailsPanel = new VBox(20);
        detailsPanel.getStyleClass().addAll("portal-card", "settings-panel");
        detailsPanel.setMaxWidth(Double.MAX_VALUE);
        detailsPanel.getChildren().setAll(buildAccountProfileSection());

        VBox page = new VBox(18, intro, detailsPanel);
        page.getStyleClass().addAll("portal-page", "main-content");
        page.setMaxWidth(Double.MAX_VALUE);
        page.setMaxHeight(Double.MAX_VALUE);

        return page;
    }

    private Node buildAccountProfileSection() {
        User account = currentAccountUser();
        UserPortalModel.AccountProfile fallbackProfile = portalModel.fetchAccountProfile();

        Label heading = new Label("Account Information");
        heading.getStyleClass().add("settings-section-heading");

        TextField nameField = createAccountTextField(displayNameFor(account, fallbackProfile));
        TextField usernameField = createAccountTextField(account == null ? "" : clean(account.getUsername()));
        TextField emailField = createAccountTextField(account == null ? "" : clean(account.getEmail()));
        TextField roleField = createAccountTextField(account == null ? "User" : clean(account.getRole()));
        TextField statusField = createAccountTextField(account == null ? "Active" : clean(account.getStatus()));

        roleField.setEditable(false);
        statusField.setEditable(false);
        roleField.getStyleClass().add("admin-account-readonly-field");
        statusField.getStyleClass().add("admin-account-readonly-field");

        PasswordField newPasswordField = createAccountPasswordField("New password");
        PasswordField confirmPasswordField = createAccountPasswordField("Confirm new password");

        Label saveMessage = new Label();
        saveMessage.getStyleClass().add("portal-inline-message");
        saveMessage.setVisible(false);
        saveMessage.setManaged(false);

        Button saveButton = new Button("Save Changes");
        saveButton.getStyleClass().add("portal-primary-button");
        saveButton.setOnAction(event -> saveAccountProfile(
                account,
                nameField,
                usernameField,
                emailField,
                newPasswordField,
                confirmPasswordField,
                saveMessage
        ));

        VBox accountForm = new VBox(14,
                formField("Full Name", nameField),
                formField("Username", usernameField),
                formField("Email Address", emailField),
                formField("Role", roleField),
                formField("Status", statusField)
        );
        accountForm.getStyleClass().add("settings-form");

        VBox passwordForm = new VBox(14,
                sectionLabel("Password"),
                formField("New Password", newPasswordField),
                formField("Confirm Password", confirmPasswordField)
        );
        passwordForm.getStyleClass().add("settings-form");

        HBox actions = new HBox(12, saveButton, saveMessage);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(20,
                heading,
                accountForm,
                divider(),
                passwordForm,
                divider(),
                actions
        );
        content.getStyleClass().add("settings-form");

        return content;
    }

    private void saveAccountProfile(User account,
                                    TextField nameField,
                                    TextField usernameField,
                                    TextField emailField,
                                    PasswordField newPasswordField,
                                    PasswordField confirmPasswordField,
                                    Label saveMessage) {
        if (account == null) {
            showInlineMessage(saveMessage, "Could not find the current account.", false);
            return;
        }

        String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (!newPassword.isBlank() || !confirmPassword.isBlank()) {
            if (!newPassword.equals(confirmPassword)) {
                showInlineMessage(saveMessage, "Passwords do not match.", false);
                return;
            }
        }

        try {
            User updatedUser = userManager.updateCurrentAccount(
                    new UserManager.AccountInput(
                            nameField.getText(),
                            usernameField.getText(),
                            emailField.getText(),
                            newPassword
                    )
            );

            UserSession.setCurrentUser(updatedUser);
            configureAccount();

            newPasswordField.clear();
            confirmPasswordField.clear();

            showInlineMessage(saveMessage, "Changes saved.", true);
        } catch (RuntimeException exception) {
            showInlineMessage(saveMessage, exception.getMessage(), false);
        }
    }

    private User currentAccountUser() {
        return userManager.getCurrentAccount();
    }

    private VBox formField(String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        return new VBox(8, label, field);
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("portal-section-title");
        return label;
    }

    private TextField createAccountTextField(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("portal-text-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private PasswordField createAccountPasswordField(String promptText) {
        PasswordField field = new PasswordField();
        field.setPromptText(promptText);
        field.getStyleClass().add("portal-text-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private Region divider() {
        Region divider = new Region();
        divider.getStyleClass().add("portal-divider");
        return divider;
    }

    private void showInlineMessage(Label messageLabel, String message, boolean success) {
        messageLabel.getStyleClass().removeAll("success", "error");
        messageLabel.getStyleClass().add(success ? "success" : "error");
        messageLabel.setText(clean(message).isBlank() ? "Something went wrong." : message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showKeyboardShortcutsDialog() {
        hideAccountDropdown();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setHeaderText(null);
        dialog.setTitle("Keyboard Shortcuts");

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Node defaultCloseButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (defaultCloseButton != null) {
            defaultCloseButton.setVisible(false);
            defaultCloseButton.setManaged(false);
        }

        dialog.getDialogPane().getStyleClass().addAll(
                "app-shell",
                "weblager-shortcuts-dialog-pane"
        );

        if (isDarkModeEnabled()) {
            dialog.getDialogPane().getStyleClass().add(DARK_MODE_CLASS);
        }

        if (appShell != null && appShell.getScene() != null) {
            dialog.initOwner(appShell.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().setAll(appShell.getScene().getStylesheets());
        }

        dialog.getDialogPane().setContent(createKeyboardShortcutsContent(dialog));
        dialog.showAndWait();
    }

    private VBox createKeyboardShortcutsContent(Dialog<ButtonType> dialog) {
        VBox root = new VBox();
        root.getStyleClass().add("weblager-shortcuts-root");

        root.getChildren().addAll(
                createKeyboardShortcutsHeader(dialog),
                createKeyboardShortcutsBody(dialog)
        );

        return root;
    }

    private HBox createKeyboardShortcutsHeader(Dialog<ButtonType> dialog) {
        Label title = new Label("Keyboard Shortcuts");
        title.getStyleClass().add("weblager-shortcuts-title");

        Label subtitle = new Label("Common actions for scanning and QA.");
        subtitle.getStyleClass().add("weblager-shortcuts-subtitle");

        VBox copy = new VBox(3, title, subtitle);
        copy.getStyleClass().add("weblager-shortcuts-header-copy");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("weblager-shortcuts-x-button");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        HBox header = new HBox(12, copy, spacer, closeButton);
        header.getStyleClass().add("weblager-shortcuts-header");
        header.setAlignment(Pos.CENTER_LEFT);

        return header;
    }

    private VBox createKeyboardShortcutsBody(Dialog<ButtonType> dialog) {
        HBox cards = new HBox(24,
                createShortcutCard(
                        "General",
                        List.of(
                                shortcutData("Open shortcuts", "?"),
                                shortcutData("Close dialog", "Esc"),
                                shortcutData("Save progress", "Ctrl + S")
                        )
                ),
                createShortcutCard(
                        "Scanning",
                        List.of(
                                shortcutData("Scan next page", "Space"),
                                shortcutData("Previous page", "←"),
                                shortcutData("Next page", "→"),
                                shortcutData("Rotate right", "R"),
                                shortcutData("Rotate left", "L"),
                                shortcutData("Delete page", "Delete"),
                                shortcutData("Open metadata", "M"),
                                shortcutData("Export", "E")
                        )
                ),
                createShortcutCard(
                        "QA Review",
                        List.of(
                                shortcutData("Approve page", "A"),
                                shortcutData("Mark needs fix", "F"),
                                shortcutData("Add comment", "N"),
                                shortcutData("Previous page", "←"),
                                shortcutData("Next page", "→"),
                                shortcutData("Complete QA", "C")
                        )
                )
        );
        cards.getStyleClass().add("weblager-shortcuts-card-row");
        cards.setAlignment(Pos.TOP_CENTER);

        Label footerText = new Label("You can open this anytime from the help button or Settings.");
        footerText.getStyleClass().add("weblager-shortcuts-footer-text");

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("weblager-shortcuts-close-button");
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        VBox body = new VBox(24, cards, footerText, closeButton);
        body.getStyleClass().add("weblager-shortcuts-body");
        body.setAlignment(Pos.TOP_CENTER);

        return body;
    }

    private VBox createShortcutCard(String titleText, List<ShortcutData> shortcuts) {
        Label title = new Label(titleText);
        title.getStyleClass().add("weblager-shortcuts-card-title");

        VBox rows = new VBox(12);
        rows.getStyleClass().add("weblager-shortcuts-list");

        for (ShortcutData shortcut : shortcuts) {
            rows.getChildren().add(createKeyboardShortcutRow(shortcut.label(), shortcut.key()));
        }

        VBox card = new VBox(18, title, rows);
        card.getStyleClass().add("weblager-shortcuts-card");

        return card;
    }

    private HBox createKeyboardShortcutRow(String labelText, String keyText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("weblager-shortcuts-action-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label key = new Label(keyText);
        key.getStyleClass().add("weblager-shortcuts-key");

        HBox row = new HBox(12, label, spacer, key);
        row.getStyleClass().add("weblager-shortcuts-row");
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    private ShortcutData shortcutData(String label, String key) {
        return new ShortcutData(label, key);
    }

    private record ShortcutData(String label, String key) {
    }

    private HBox shortcutRow(String shortcut, String description) {
        Label shortcutLabel = new Label(shortcut);
        shortcutLabel.getStyleClass().add("portal-section-title");
        shortcutLabel.setMinWidth(72);

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("portal-muted");
        descriptionLabel.setWrapText(true);

        HBox row = new HBox(12, shortcutLabel, descriptionLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(descriptionLabel, Priority.ALWAYS);

        return row;
    }

    private void showHelpDialog() {
        hideAccountDropdown();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setHeaderText(null);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Node defaultCloseButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (defaultCloseButton != null) {
            defaultCloseButton.setVisible(false);
            defaultCloseButton.setManaged(false);
        }

        dialog.getDialogPane().getStyleClass().addAll(
                "app-shell",
                "weblager-help-dialog-pane"
        );

        if (isDarkModeEnabled()) {
            dialog.getDialogPane().getStyleClass().add(DARK_MODE_CLASS);
        }

        if (appShell != null && appShell.getScene() != null) {
            dialog.initOwner(appShell.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().setAll(appShell.getScene().getStylesheets());
        }

        dialog.getDialogPane().setContent(createHelpDialogContent(dialog));
        dialog.showAndWait();
    }

    private VBox createHelpDialogContent(Dialog<ButtonType> dialog) {
        VBox root = new VBox();
        root.getStyleClass().add("weblager-help-root");

        root.getChildren().addAll(
                createHelpDialogHeader(dialog),
                createHelpDialogBody()
        );

        return root;
    }

    private HBox createHelpDialogHeader(Dialog<ButtonType> dialog) {
        Label title = new Label("Help & Documentation");
        title.getStyleClass().add("weblager-help-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("weblager-help-close-button");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(event -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });

        HBox header = new HBox(12, title, spacer, closeButton);
        header.getStyleClass().add("weblager-help-header");
        header.setAlignment(Pos.CENTER_LEFT);

        return header;
    }

    private ScrollPane createHelpDialogBody() {
        VBox content = new VBox(18);
        content.getStyleClass().add("weblager-help-content");

        VBox gettingStartedRows = new VBox(15,
                createHelpRow(
                        HELP_SCAN_ICON_PATH,
                        "Starting a Scan",
                        "Navigate to New Scan, select your scan type, choose a profile, and enter the Box ID. The system will guide you through each page for quality approval."
                ),
                createHelpRow(
                        HELP_QA_ICON_PATH,
                        "Quality Assurance",
                        "During scanning, review each page as it appears. Use Space to approve, F to flag for rescan, or Delete to remove. Flagged pages can be rescanned later."
                ),
                createHelpRow(
                        HELP_EXPORT_ICON_PATH,
                        "Exporting Files",
                        "After scanning, configure your export settings including format, quality level, and OCR options. Files are available in the Exports page."
                ),
                createHelpRow(
                        HELP_SETTINGS_ICON_PATH,
                        "Customizing Settings",
                        "Open your account menu to access Settings and Privacy, dark mode, account details, and logout."
                )
        );

        VBox commonQuestions = new VBox(6,
                createHelpSectionTitle("Common Questions"),
                createQuestion(
                        "What's the difference between single and multi scan?",
                        "Single scan is for one document with multiple pages. Multi scan allows you to scan multiple separate documents in one session."
                ),
                createQuestion(
                        "Can I edit a scan after completion?",
                        "Once completed, you can view and export scans but cannot modify them. You can flag pages for rescan or delete unwanted pages during the scanning process."
                ),
                createQuestion(
                        "How long are exports stored?",
                        "Exports are stored for 30 days. Download important files promptly to avoid data loss."
                )
        );
        commonQuestions.getStyleClass().add("weblager-help-questions");

        content.getChildren().addAll(
                createHelpSectionTitle("Getting Started"),
                gettingStartedRows,
                createHelpDivider(),
                commonQuestions,
                createHelpDivider(),
                createHelpSupportSection()
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPrefViewportWidth(690);
        scrollPane.setPrefViewportHeight(510);
        scrollPane.getStyleClass().add("weblager-help-scroll");

        return scrollPane;
    }

    private VBox createHelpSupportSection() {
        Label title = new Label("Need More Help?");
        title.getStyleClass().add("weblager-help-section-title");

        Label copy = new Label("Contact your system administrator or IT support team for additional assistance.");
        copy.setWrapText(true);
        copy.getStyleClass().add("weblager-help-support-copy");

        Hyperlink emailLink = new Hyperlink("support@company.com");
        emailLink.getStyleClass().add("weblager-help-support-link");
        emailLink.setFocusTraversable(false);

        VBox section = new VBox(12, title, copy, emailLink);
        section.getStyleClass().add("weblager-help-support-section");

        return section;
    }

    private HBox createHelpRow(String iconPath, String titleText, String bodyText) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add("weblager-help-icon");

        StackPane iconShell = new StackPane(icon);
        iconShell.getStyleClass().add("weblager-help-icon-shell");

        Label title = new Label(titleText);
        title.getStyleClass().add("weblager-help-row-title");

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.getStyleClass().add("weblager-help-row-copy");

        VBox textBox = new VBox(4, title, body);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox row = new HBox(15, iconShell, textBox);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("weblager-help-row");

        return row;
    }

    private TitledPane createQuestion(String questionText, String answerText) {
        Label answer = new Label(answerText);
        answer.setWrapText(true);
        answer.getStyleClass().add("weblager-help-answer");

        TitledPane question = new TitledPane(questionText, answer);
        question.setExpanded(false);
        question.setAnimated(false);
        question.setFocusTraversable(false);
        question.getStyleClass().add("weblager-help-question");

        return question;
    }

    private Label createHelpSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("weblager-help-section-title");
        return label;
    }

    private Region createHelpDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("weblager-help-divider");
        return divider;
    }

    private void logout() {
        UserSession.clearCurrentUser();
        hideAccountDropdown();

        if (mainApp == null) {
            return;
        }

        try {
            mainApp.showLoginView();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not return to login.", exception);
        }
    }

    private String displayNameFor(User user, UserPortalModel.AccountProfile fallbackProfile) {
        if (user != null) {
            if (!clean(user.getName()).isBlank()) {
                return clean(user.getName());
            }

            if (!clean(user.getUsername()).isBlank()) {
                return clean(user.getUsername());
            }
        }

        if (fallbackProfile != null && !clean(fallbackProfile.fullName()).isBlank()) {
            return clean(fallbackProfile.fullName());
        }

        return "User";
    }

    private String accountDetailFor(User user) {
        if (user == null) {
            return "User account";
        }

        if (!clean(user.getEmail()).isBlank()) {
            return clean(user.getEmail());
        }

        if (!clean(user.getUsername()).isBlank()) {
            return clean(user.getUsername());
        }

        return clean(user.getRole()).isBlank() ? "User account" : clean(user.getRole()) + " account";
    }

    private String initialsFor(String name) {
        String cleanedName = clean(name);

        if (cleanedName.isBlank()) {
            return "U";
        }

        String[] parts = cleanedName.split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }

        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
