package easv.gui.controller.user;

import easv.be.User;
import easv.bll.AuthManager;
import easv.bll.UserSession;
import easv.gui.MainApp;
import easv.gui.UserPortalModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.prefs.Preferences;

public class UserController implements UserNavigator {

    private static final String ACTIVE_NAV_CLASS = "active";
    private static final String DARK_MODE_CLASS = "dark";
    private static final String PREFERENCES_NODE = "easv.gui.portal";
    private static final String DARK_MODE_PREFERENCE_KEY = "userPortal.darkMode";

    private static final String LIGHT_MODE_LOGO =
            "/images/weblager/styleguide/Main Blue/LogoBlueH.png";

    private static final String DARK_MODE_LOGO =
            "/images/weblager/styleguide/DarkmodeBlue/LogoBlue2H.png";

    private static final String MOON_ICON_PATH =
            "M12 3.25a8.75 8.75 0 1 0 8.75 8.75c0-.45-.04-.89-.1-1.32A6.75 6.75 0 0 1 12.32 3.4c-.1-.05-.21-.1-.32-.15zM5.25 12A6.74 6.74 0 0 1 9.83 5.6a8.75 8.75 0 0 0 8.57 8.57A6.75 6.75 0 0 1 5.25 12z";

    private static final String SUN_ICON_PATH =
            "M12 5.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9zM11 1h2v3h-2V1zm0 19h2v3h-2v-3zM1 11h3v2H1v-2zm19 0h3v2h-3v-2zM4.22 2.81l2.12 2.12-1.41 1.41L2.81 4.22l1.41-1.41zm14.85 14.85 2.12 2.12-1.41 1.41-2.12-2.12 1.41-1.41zM19.78 2.81l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12zM4.93 17.66l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12z";

    @FXML private BorderPane appRoot;
    @FXML private StackPane contentHost;

    @FXML private StackPane brandMark;
    @FXML private Label brandLogoFallbackLabel;
    @FXML private ImageView brandLogoImageView;
    @FXML private Label accountNameLabel;
    @FXML private Label accountRoleLabel;
    @FXML private Label avatarInitialsLabel;
    @FXML private Button accountMenuButton;

    @FXML private ToggleButton dashboardNavItem;
    @FXML private ToggleButton scanNavItem;
    @FXML private ToggleButton myScansNavItem;
    @FXML private ToggleButton assignedQANavItem;
    @FXML private ToggleButton exportsNavItem;

    @FXML private ToggleButton darkModeToggleButton;
    @FXML private SVGPath darkModeToggleIcon;
    @FXML private Label darkModeToggleLabel;

    private final UserPortalModel portalModel = new UserPortalModel();
    private final AuthManager authManager = new AuthManager();
    private final Preferences preferences = Preferences.userRoot().node(PREFERENCES_NODE);
    private MainApp mainApp;
    private final ContextMenu accountMenu = new ContextMenu();

    @FXML
    private void initialize() {
        configureShell();
        configureThemeToggle();
        configureNavigation();
        showPage(UserPage.DASHBOARD);
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    private void configureShell() {
        UserPortalModel.AccountProfile fallbackProfile = portalModel.fetchAccountProfile();
        User currentUser = UserSession.getCurrentUser();

        String accountName = fallbackProfile.fullName() == null || fallbackProfile.fullName().isBlank()
                ? (currentUser == null || currentUser.getName().isBlank() ? "User" : currentUser.getName())
                : fallbackProfile.fullName();

        String accountRole = currentUser == null || currentUser.getRole().isBlank()
                ? "User Portal"
                : currentUser.getRole();

        if (accountNameLabel != null) {
            accountNameLabel.setText(accountName);
        }

        if (accountRoleLabel != null) {
            accountRoleLabel.setText(accountRole);
        }

        if (avatarInitialsLabel != null) {
            avatarInitialsLabel.setText(initialsFor(accountName));
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
        appRoot.getStyleClass().remove(DARK_MODE_CLASS);

        if (isDark) {
            appRoot.getStyleClass().add(DARK_MODE_CLASS);
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
        if (darkModeToggleButton != null && darkModeToggleButton.isSelected() != isDark) {
            darkModeToggleButton.setSelected(isDark);
        }

        if (darkModeToggleIcon != null) {
            darkModeToggleIcon.setContent(isDark ? MOON_ICON_PATH : SUN_ICON_PATH);
        }

        if (darkModeToggleLabel != null) {
            darkModeToggleLabel.setText(isDark ? "Dark Mode" : "Light Mode");
        }
    }

    private void configureNavigation() {
        for (UserPage page : UserPage.values()) {
            ToggleButton navItem = getNavItem(page);

            if (navItem != null) {
                navItem.setOnAction(event -> showPage(page));
            }
        }

        if (accountMenuButton != null) {
            accountMenuButton.setOnAction(event -> toggleAccountMenu());
        }

        configureAccountMenu();
    }

    @Override
    public void showPage(UserPage page) {
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
            case EDIT_PROFILE -> createEditProfilePage();
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

        Label subtitleLabel = new Label("This page has not been created yet.");
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox placeholder = new VBox(6, titleLabel, subtitleLabel);
        placeholder.getStyleClass().add("page-content");
        placeholder.setAlignment(Pos.TOP_LEFT);
        placeholder.setMaxWidth(Double.MAX_VALUE);
        placeholder.setMaxHeight(Double.MAX_VALUE);

        return placeholder;
    }

    private VBox createEditProfilePage() {
        UserPortalModel.AccountProfile profile = portalModel.fetchAccountProfile();

        Label titleLabel = new Label("Edit User Profile");
        titleLabel.getStyleClass().add("exports-title");

        Label subtitleLabel = new Label("Update the account details shown in the portal header and personal menu.");
        subtitleLabel.getStyleClass().add("exports-subtitle");

        Label nameLabel = new Label("Full Name");
        nameLabel.getStyleClass().add("field-label");
        TextField nameField = new TextField(profile.fullName());
        nameField.getStyleClass().add("weblager-text-field");

        Label emailLabel = new Label("Email");
        emailLabel.getStyleClass().add("field-label");
        TextField emailField = new TextField(profile.email());
        emailField.getStyleClass().add("weblager-text-field");

        Label departmentLabel = new Label("Department");
        departmentLabel.getStyleClass().add("field-label");
        TextField departmentField = new TextField(profile.department());
        departmentField.getStyleClass().add("weblager-text-field");

        VBox form = new VBox(12,
                nameLabel, nameField,
                emailLabel, emailField,
                departmentLabel, departmentField
        );
        form.getStyleClass().add("profile-info-panel");

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("portal-secondary-button");
        cancelButton.setOnAction(event -> showPage(UserPage.DASHBOARD));

        Button saveButton = new Button("Save Profile");
        saveButton.getStyleClass().add("portal-primary-button");
        saveButton.setOnAction(event -> {
            portalModel.updateAccountProfile(nameField.getText(), emailField.getText(), departmentField.getText());
            configureShell();
            showPage(UserPage.DASHBOARD);
        });

        HBox actions = new HBox(9, cancelButton, saveButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(24, new VBox(6, titleLabel, subtitleLabel), form, actions);
        page.getStyleClass().addAll("portal-page", "exports-page");
        page.setMaxWidth(Double.MAX_VALUE);
        return page;
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

    private String initialsFor(String name) {
        if (name == null || name.isBlank()) {
            return "U";
        }

        String[] parts = name.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
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
            case EDIT_PROFILE, SETTINGS -> null;
        };
    }

    private void configureAccountMenu() {
        UserPortalModel.AccountProfile profile = portalModel.fetchAccountProfile();

        HBox headerRow = new HBox(12,
                buildMenuAvatar(initialsFor(profile.fullName())),
                buildMenuHeaderText(profile.fullName(), profile.email())
        );
        headerRow.getStyleClass().add("account-dropdown-header");

        CustomMenuItem headerItem = new CustomMenuItem(headerRow, false);
        headerItem.getStyleClass().add("account-dropdown-header-item");
        headerItem.setHideOnClick(false);

        MenuItem editProfileItem = createAccountMenuItem("Edit User Profile", "user", () -> showPage(UserPage.EDIT_PROFILE));
        MenuItem settingsItem = createAccountMenuItem("Settings", "settings", () -> showPage(UserPage.SETTINGS));
        MenuItem logoutItem = createAccountMenuItem("Log Out", "download", this::handleLogout);

        accountMenu.getItems().setAll(
                headerItem,
                new SeparatorMenuItem(),
                editProfileItem,
                settingsItem,
                logoutItem
        );
        if (!accountMenu.getStyleClass().contains("account-dropdown-menu")) {
            accountMenu.getStyleClass().add("account-dropdown-menu");
        }
    }

    private void toggleAccountMenu() {
        if (accountMenuButton == null) {
            return;
        }

        if (accountMenu.isShowing()) {
            accountMenu.hide();
            return;
        }

        configureAccountMenu();
        accountMenu.show(accountMenuButton, Side.BOTTOM, 0, 9);
        keepAccountMenuInsideWindow();
    }

    private void keepAccountMenuInsideWindow() {
        if (accountMenuButton == null || accountMenu.getScene() == null || accountMenu.getSkin() == null) {
            return;
        }

        Bounds buttonBounds = accountMenuButton.localToScreen(accountMenuButton.getBoundsInLocal());
        Window window = accountMenuButton.getScene().getWindow();

        if (buttonBounds == null || window == null) {
            return;
        }

        double menuWidth = accountMenu.getSkin().getNode().prefWidth(-1);
        double inset = 12;
        double minX = window.getX() + inset;
        double maxX = window.getX() + window.getWidth() - menuWidth - inset;
        double anchorX = Math.min(Math.max(buttonBounds.getMaxX() - menuWidth, minX), maxX);

        accountMenu.setAnchorX(anchorX);
        accountMenu.setAnchorY(buttonBounds.getMaxY() + 9);
    }

    private MenuItem createAccountMenuItem(String text, String iconKey, Runnable action) {
        HBox row = new HBox(12,
                wrapMenuIcon(iconKey),
                buildMenuItemLabel(text),
                buildMenuItemArrow()
        );
        row.getStyleClass().add("account-dropdown-item-row");
        HBox.setMargin(row.getChildren().get(2), new Insets(0, 0, 0, 6));

        CustomMenuItem item = new CustomMenuItem(row, true);
        item.getStyleClass().add("account-dropdown-item");
        item.setOnAction(event -> action.run());
        return item;
    }

    private StackPane buildMenuAvatar(String initials) {
        Label label = new Label(initials);
        label.getStyleClass().add("account-dropdown-avatar-label");

        StackPane avatar = new StackPane(label);
        avatar.getStyleClass().add("account-dropdown-avatar");
        return avatar;
    }

    private VBox buildMenuHeaderText(String name, String detail) {
        Label nameLabel = new Label(name == null || name.isBlank() ? "User" : name);
        nameLabel.getStyleClass().add("account-dropdown-name");

        Label detailLabel = new Label(detail == null || detail.isBlank() ? "User Portal" : detail);
        detailLabel.getStyleClass().add("account-dropdown-detail");

        VBox textWrap = new VBox(3, nameLabel, detailLabel);
        textWrap.setAlignment(Pos.CENTER_LEFT);
        return textWrap;
    }

    private StackPane wrapMenuIcon(String iconKey) {
        StackPane shell = new StackPane(UserPortalUi.buildIcon(iconKey, "account-dropdown-item-icon"));
        shell.getStyleClass().add("account-dropdown-item-icon-shell");
        return shell;
    }

    private Label buildMenuItemLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("account-dropdown-item-label");
        HBox.setHgrow(label, Priority.ALWAYS);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label buildMenuItemArrow() {
        Label arrow = new Label("›");
        arrow.getStyleClass().add("account-dropdown-item-arrow");
        return arrow;
    }

    private void handleLogout() {
        authManager.logout();

        if (mainApp == null) {
            throw new IllegalStateException("MainApp is not available for logout navigation.");
        }

        try {
            mainApp.showLoginView();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not return to the login view.", exception);
        }
    }
}
