package easv.gui.controller.admin;

import easv.bll.AdminManager;
import easv.bll.AuthManager;
import easv.gui.MainApp;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
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
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class AdminController implements AdminNavigator {

    private static final String LIGHT_MODE_LOGO =
            "/images/weblager/styleguide/Main Blue/LogoBlueH.png";

    private static final String DARK_MODE_LOGO =
            "/images/weblager/styleguide/DarkmodeBlue/LogoBlue2H.png";

    private static final String ACTIVE_NAV_CLASS = "active";
    private static final String DARK_MODE_CLASS = "dark";

    private static final String MOON_ICON_PATH =
            "M12 3.25a8.75 8.75 0 1 0 8.75 8.75c0-.45-.04-.89-.1-1.32A6.75 6.75 0 0 1 12.32 3.4c-.1-.05-.21-.1-.32-.15zM5.25 12A6.74 6.74 0 0 1 9.83 5.6a8.75 8.75 0 0 0 8.57 8.57A6.75 6.75 0 0 1 5.25 12z";

    private static final String SUN_ICON_PATH =
            "M12 5.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9zM11 1h2v3h-2V1zm0 19h2v3h-2v-3zM1 11h3v2H1v-2zm19 0h3v2h-3v-2zM4.22 2.81l2.12 2.12-1.41 1.41L2.81 4.22l1.41-1.41zm14.85 14.85 2.12 2.12-1.41 1.41-2.12-2.12 1.41-1.41zM19.78 2.81l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12zM4.93 17.66l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12z";

    @FXML private BorderPane appRoot;
    @FXML private StackPane contentHost;

    @FXML private StackPane brandMark;
    @FXML private Label brandLogoFallbackLabel;
    @FXML private ImageView brandLogoImageView;

    @FXML private ToggleButton dashboardNavItem;
    @FXML private ToggleButton usersNavItem;
    @FXML private ToggleButton profilesNavItem;
    @FXML private ToggleButton assignmentsNavItem;
    @FXML private ToggleButton metadataNavItem;
    @FXML private ToggleButton metadataReviewNavItem;
    @FXML private ToggleButton activityNavItem;
    @FXML private Button logoutNavButton;

    @FXML private ToggleButton darkModeToggleButton;
    @FXML private Label accountNameLabel;
    @FXML private Label accountRoleLabel;
    @FXML private Label avatarInitialsLabel;
    @FXML private Button accountMenuButton;
    @FXML private Label themeModeLabel;
    @FXML private SVGPath themeModeIcon;
    @FXML private SVGPath darkModeToggleIcon;

    private final AdminManager adminManager = new AdminManager();
    private final AuthManager authManager = new AuthManager();
    private final ContextMenu accountMenu = new ContextMenu();
    private MainApp mainApp;

    @FXML
    private void initialize() {
        configureBrandLogo();
        configureThemeToggle();
        configureAccountShell();
        configureNavigation();
        showPage(AdminPage.DASHBOARD);
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    private void configureBrandLogo() {
        updateBrandLogo(isDarkModeEnabled());
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
        return darkModeToggleButton != null && darkModeToggleButton.isSelected();
    }

    private void updateTheme(boolean isDark) {
        updateDarkModeClass(isDark);
        updateBrandLogo(isDark);
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
        if (themeModeLabel != null) {
            themeModeLabel.setText(isDark ? "Dark Mode" : "Light Mode");
        }

        if (themeModeIcon != null) {
            themeModeIcon.setContent(isDark ? MOON_ICON_PATH : SUN_ICON_PATH);
        }

        if (darkModeToggleIcon != null) {
            darkModeToggleIcon.setContent(isDark ? MOON_ICON_PATH : SUN_ICON_PATH);
        }
    }

    private void configureNavigation() {
        for (AdminPage page : AdminPage.values()) {
            setNavigationAction(getNavItem(page), () -> showPage(page));
        }

        if (logoutNavButton != null) {
            logoutNavButton.setOnAction(event -> handleLogout());
        }
    }

    private void configureAccountShell() {
        if (accountNameLabel != null && (accountNameLabel.getText() == null || accountNameLabel.getText().isBlank())) {
            accountNameLabel.setText("Admin System");
        }

        if (accountRoleLabel != null && (accountRoleLabel.getText() == null || accountRoleLabel.getText().isBlank())) {
            accountRoleLabel.setText("Admin Portal");
        }

        if (avatarInitialsLabel != null) {
            avatarInitialsLabel.setText(initialsFor(accountNameLabel == null ? "Admin System" : accountNameLabel.getText()));
        }

        if (accountMenuButton != null) {
            accountMenuButton.setOnAction(event -> toggleAccountMenu());
        }

        configureAccountMenu();
    }

    private void configureAccountMenu() {
        String name = accountNameLabel == null ? "Admin System" : accountNameLabel.getText();
        String detail = accountRoleLabel == null ? "Admin Portal" : accountRoleLabel.getText();

        HBox headerRow = new HBox(12,
                buildMenuAvatar(initialsFor(name)),
                buildMenuHeaderText(name, detail)
        );
        headerRow.getStyleClass().add("account-dropdown-header");

        CustomMenuItem headerItem = new CustomMenuItem(headerRow, false);
        headerItem.getStyleClass().add("account-dropdown-header-item");
        headerItem.setHideOnClick(false);

        MenuItem editProfileItem = createAccountMenuItem("Edit Admin Profile", "user", () -> showPage(AdminPage.EDIT_PROFILE));
        MenuItem logoutItem = createAccountMenuItem("Log Out", "download", this::handleLogout);

        accountMenu.getItems().setAll(
                headerItem,
                new SeparatorMenuItem(),
                editProfileItem,
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
        double inset = 24;
        double minX = window.getX() + inset;
        double maxX = window.getX() + window.getWidth() - menuWidth - inset;
        double preferredX = buttonBounds.getMaxX() - menuWidth - 12;
        double anchorX = Math.min(Math.max(preferredX, minX), maxX);

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
        Label nameLabel = new Label(name == null || name.isBlank() ? "Admin System" : name);
        nameLabel.getStyleClass().add("account-dropdown-name");

        Label detailLabel = new Label(detail == null || detail.isBlank() ? "Admin Portal" : detail);
        detailLabel.getStyleClass().add("account-dropdown-detail");

        VBox textWrap = new VBox(3, nameLabel, detailLabel);
        textWrap.setAlignment(Pos.CENTER_LEFT);
        return textWrap;
    }

    private StackPane wrapMenuIcon(String iconKey) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath(iconKey));
        icon.getStyleClass().add("account-dropdown-item-icon");

        StackPane shell = new StackPane(icon);
        shell.getStyleClass().add("account-dropdown-item-icon-shell");
        return shell;
    }

    private String iconPath(String iconKey) {
        return switch (iconKey) {
            case "user" -> "M10 2a3 3 0 110 6 3 3 0 010-6zm0 8c3 0 5 1.5 5 4v2H5v-2c0-2.5 2-4 5-4z";
            case "download" -> "M9 2h2v7h3l-4 4-4-4h3z M4 14h12v2H4z";
            default -> "M4 4h12v12H4z";
        };
    }

    private Label buildMenuItemLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("account-dropdown-item-label");
        HBox.setHgrow(label, Priority.ALWAYS);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label buildMenuItemArrow() {
        Label arrow = new Label("\u203A");
        arrow.getStyleClass().add("account-dropdown-item-arrow");
        return arrow;
    }

    private Parent createProgrammaticPage(AdminPage page) {
        if (page == AdminPage.EDIT_PROFILE) {
            return createEditProfilePage();
        }

        return createMissingPagePlaceholder(page.title());
    }

    private VBox createEditProfilePage() {
        String currentName = accountNameLabel == null || accountNameLabel.getText() == null || accountNameLabel.getText().isBlank()
                ? "Admin System"
                : accountNameLabel.getText();
        String currentRole = accountRoleLabel == null || accountRoleLabel.getText() == null || accountRoleLabel.getText().isBlank()
                ? "Admin Portal"
                : accountRoleLabel.getText();

        Label titleLabel = new Label("Edit Admin Profile");
        titleLabel.getStyleClass().add("exports-title");

        Label subtitleLabel = new Label("Update the account details shown in the admin portal header and personal menu.");
        subtitleLabel.getStyleClass().add("exports-subtitle");

        Label nameLabel = new Label("Full Name");
        nameLabel.getStyleClass().add("field-label");
        TextField nameField = new TextField(currentName);
        nameField.getStyleClass().add("weblager-text-field");

        Label roleLabel = new Label("Role");
        roleLabel.getStyleClass().add("field-label");
        TextField roleField = new TextField(currentRole);
        roleField.getStyleClass().add("weblager-text-field");

        VBox form = new VBox(12,
                nameLabel, nameField,
                roleLabel, roleField
        );
        form.getStyleClass().add("profile-info-panel");

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("portal-secondary-button");
        cancelButton.setOnAction(event -> showPage(AdminPage.DASHBOARD));

        Button saveButton = new Button("Save Profile");
        saveButton.getStyleClass().add("portal-primary-button");
        saveButton.setOnAction(event -> {
            if (accountNameLabel != null) {
                accountNameLabel.setText(nameField.getText());
            }
            if (accountRoleLabel != null) {
                accountRoleLabel.setText(roleField.getText());
            }
            if (avatarInitialsLabel != null) {
                avatarInitialsLabel.setText(initialsFor(nameField.getText()));
            }
            configureAccountMenu();
            showPage(AdminPage.DASHBOARD);
        });

        HBox actions = new HBox(9, cancelButton, saveButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(24, new VBox(6, titleLabel, subtitleLabel), form, actions);
        page.getStyleClass().addAll("portal-page", "exports-page");
        page.setMaxWidth(Double.MAX_VALUE);
        return page;
    }

    private String initialsFor(String name) {
        if (name == null || name.isBlank()) {
            return "A";
        }

        String[] parts = name.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private void setNavigationAction(ToggleButton navItem, Runnable action) {
        if (navItem == null) {
            return;
        }

        navItem.setOnAction(event -> action.run());
    }

    @Override
    public void showPage(AdminPage page) {
        loadPage(page);
        setActiveNavItem(getNavItem(page));
    }

    private void loadPage(AdminPage page) {
        if (page.fxmlPath() == null) {
            Parent programmaticPage = createProgrammaticPage(page);
            configureLoadedPageSize(programmaticPage);
            contentHost.getChildren().setAll(programmaticPage);
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

    private void configureLoadedController(Object controller) {
        if (controller instanceof DashboardController dashboardController) {
            dashboardController.setNavigator(this);
            dashboardController.setAdminManager(adminManager);
        } else if (controller instanceof ManageUsersController manageUsersController) {
            manageUsersController.setAdminManager(adminManager);
        } else if (controller instanceof ProfilesController profilesController) {
            profilesController.setNavigator(this);
            profilesController.setAdminManager(adminManager);
        } else if (controller instanceof AssignmentsController assignmentsController) {
            assignmentsController.setAdminManager(adminManager);
        } else if (controller instanceof MetadataController metadataController) {
            metadataController.setAdminManager(adminManager);
        } else if (controller instanceof MetadataReviewController metadataReviewController) {
            metadataReviewController.setAdminManager(adminManager);
        } else if (controller instanceof ActivityController activityController) {
            activityController.setAdminManager(adminManager);
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
        placeholder.getStyleClass().addAll("admin-page", "main-content");
        placeholder.setAlignment(Pos.TOP_LEFT);
        placeholder.setMaxWidth(Double.MAX_VALUE);
        placeholder.setMaxHeight(Double.MAX_VALUE);
        placeholder.setStyle("-fx-padding: 24;");

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
                usersNavItem,
                profilesNavItem,
                assignmentsNavItem,
                metadataNavItem,
                metadataReviewNavItem,
                activityNavItem
        );
    }

    private ToggleButton getNavItem(AdminPage page) {
        return switch (page) {
            case DASHBOARD -> dashboardNavItem;
            case USERS -> usersNavItem;
            case PROFILES -> profilesNavItem;
            case ASSIGNMENTS -> assignmentsNavItem;
            case METADATA_TEMPLATES -> metadataNavItem;
            case METADATA_REVIEW -> metadataReviewNavItem;
            case ACTIVITY -> activityNavItem;
            case EDIT_PROFILE -> null;
        };
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
