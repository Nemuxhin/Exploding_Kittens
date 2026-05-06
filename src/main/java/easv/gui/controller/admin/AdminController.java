package easv.gui.controller.admin;

import easv.bll.AdminManager;
import easv.bll.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class AdminController implements AdminNavigator {

    private static final String ACTIVE_NAV_CLASS = "active";
    private static final String DARK_MODE_CLASS = "dark";

    private static final String MOON_ICON_PATH =
            "M12 3.25a8.75 8.75 0 1 0 8.75 8.75c0-.45-.04-.89-.1-1.32A6.75 6.75 0 0 1 12.32 3.4c-.1-.05-.21-.1-.32-.15zM5.25 12A6.74 6.74 0 0 1 9.83 5.6a8.75 8.75 0 0 0 8.57 8.57A6.75 6.75 0 0 1 5.25 12z";

    private static final String SUN_ICON_PATH =
            "M12 5.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9zM11 1h2v3h-2V1zm0 19h2v3h-2v-3zM1 11h3v2H1v-2zm19 0h3v2h-3v-2zM4.22 2.81l2.12 2.12-1.41 1.41L2.81 4.22l1.41-1.41zm14.85 14.85 2.12 2.12-1.41 1.41-2.12-2.12 1.41-1.41zM19.78 2.81l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12zM4.93 17.66l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12z";

    @FXML private BorderPane appRoot;
    @FXML private StackPane contentHost;
    @FXML private Label breadcrumbLabel;
    @FXML private Label accountNameLabel;
    @FXML private Label accountRoleLabel;

    @FXML private ToggleButton dashboardNavItem;
    @FXML private ToggleButton usersNavItem;
    @FXML private ToggleButton profilesNavItem;
    @FXML private ToggleButton assignmentsNavItem;
    @FXML private ToggleButton metadataNavItem;
    @FXML private ToggleButton metadataReviewNavItem;
    @FXML private ToggleButton activityNavItem;

    @FXML private ToggleButton darkModeToggleButton;
    @FXML private Label themeModeLabel;
    @FXML private SVGPath themeModeIcon;

    private final AdminManager adminManager = new AdminManager();

    @FXML
    private void initialize() {
        configureShell();
        configureThemeToggle();
        configureNavigation();
        showPage(AdminPage.DASHBOARD);
    }

    private void configureShell() {
        if (accountNameLabel != null) {
            String accountName = UserSession.hasCurrentUser()
                    ? UserSession.getCurrentUser().getName()
                    : "System Admin";
            accountNameLabel.setText(accountName);
        }

        if (accountRoleLabel != null) {
            String accountRole = UserSession.hasCurrentUser()
                    ? UserSession.getCurrentUser().getRole()
                    : "Administrative Portal";
            accountRoleLabel.setText(accountRole);
        }
    }

    private void configureThemeToggle() {
        updateTheme(isDarkModeEnabled());

        darkModeToggleButton.selectedProperty().addListener((observable, oldValue, isDark) ->
                updateTheme(isDark)
        );
    }

    private boolean isDarkModeEnabled() {
        return darkModeToggleButton != null && darkModeToggleButton.isSelected();
    }

    private void updateTheme(boolean isDark) {
        updateDarkModeClass(isDark);
        updateThemeControls(isDark);
    }

    private void updateDarkModeClass(boolean isDark) {
        appRoot.getStyleClass().remove(DARK_MODE_CLASS);

        if (isDark) {
            appRoot.getStyleClass().add(DARK_MODE_CLASS);
        }
    }

    private void updateThemeControls(boolean isDark) {
        if (themeModeLabel != null) {
            themeModeLabel.setText(isDark ? "Dark Mode" : "Light Mode");
        }

        if (themeModeIcon != null) {
            themeModeIcon.setContent(isDark ? MOON_ICON_PATH : SUN_ICON_PATH);
        }
    }

    private void configureNavigation() {
        for (AdminPage page : AdminPage.values()) {
            setNavigationAction(getNavItem(page), () -> showPage(page));
        }
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

        if (breadcrumbLabel != null) {
            breadcrumbLabel.setText("Admin / " + page.title());
        }
    }

    private void loadPage(AdminPage page) {
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
        };
    }
}
