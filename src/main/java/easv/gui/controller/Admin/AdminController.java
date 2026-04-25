package easv.gui.controller.Admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class AdminController {

    private static final String DASHBOARD_VIEW = "/view/AdminViews/dashboard-view.fxml";
    private static final String USERS_VIEW = "/view/AdminViews/manage-users-view.fxml";
    private static final String PROFILES_VIEW = "/view/AdminViews/manage-profiles-view.fxml";
    private static final String ASSIGNMENTS_VIEW = "/view/AdminViews/assignments-view.fxml";
    private static final String METADATA_VIEW = "/view/AdminViews/metadata-view.fxml";
    private static final String ACTIVITY_VIEW = "/view/AdminViews/activity-view.fxml";

    private static final String LIGHT_MODE_LOGO =
            "/images/weblager/styleguide/Main Blue/LogoBlueH.png";

    private static final String DARK_MODE_LOGO =
            "/images/weblager/styleguide/DarkmodeBlue/LogoBlue2H.png";

    private static final String ACTIVE_NAV_CLASS = "sidebar-nav-item-active";
    private static final String INACTIVE_NAV_CLASS = "sidebar-nav-item";
    private static final String DARK_MODE_CLASS = "dark";

    private static final String MOON_ICON_PATH =
            "M12 3.25a8.75 8.75 0 1 0 8.75 8.75c0-.45-.04-.89-.1-1.32A6.75 6.75 0 0 1 12.32 3.4c-.1-.05-.21-.1-.32-.15zM5.25 12A6.74 6.74 0 0 1 9.83 5.6a8.75 8.75 0 0 0 8.57 8.57A6.75 6.75 0 0 1 5.25 12z";

    private static final String SUN_ICON_PATH =
            "M12 5.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9zM11 1h2v3h-2V1zm0 19h2v3h-2v-3zM1 11h3v2H1v-2zm19 0h3v2h-3v-2zM4.22 2.81l2.12 2.12-1.41 1.41L2.81 4.22l1.41-1.41zm14.85 14.85 2.12 2.12-1.41 1.41-2.12-2.12 1.41-1.41zM19.78 2.81l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12zM4.93 17.66l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12z";

    @FXML private BorderPane appRoot;
    @FXML private StackPane contentHost;

    @FXML private Label brandLogoFallbackLabel;
    @FXML private ImageView brandLogoImageView;

    @FXML private HBox dashboardNavItem;
    @FXML private HBox usersNavItem;
    @FXML private HBox profilesNavItem;
    @FXML private HBox assignmentsNavItem;
    @FXML private HBox metadataNavItem;
    @FXML private HBox activityNavItem;

    @FXML private HBox darkModeRow;
    @FXML private ToggleButton darkModeToggleButton;
    @FXML private Label themeModeLabel;
    @FXML private SVGPath themeModeIcon;
    @FXML private SVGPath darkModeToggleIcon;

    @FXML
    private void initialize() {
        configureBrandLogo();
        configureThemeToggle();
        configureNavigation();
        showDashboard();
    }

    private void configureBrandLogo() {
        updateBrandLogo(false);
    }

    private void updateBrandLogo(boolean isDark) {
        URL logoUrl = getClass().getResource(isDark ? DARK_MODE_LOGO : LIGHT_MODE_LOGO);
        boolean logoExists = logoUrl != null;

        brandLogoImageView.setVisible(logoExists);
        brandLogoImageView.setManaged(logoExists);

        brandLogoFallbackLabel.setVisible(!logoExists);
        brandLogoFallbackLabel.setManaged(!logoExists);

        if (logoExists) {
            brandLogoImageView.setImage(new Image(logoUrl.toExternalForm(), true));
        }
    }

    private void configureThemeToggle() {
        updateTheme(false);

        darkModeToggleButton.selectedProperty().addListener((observable, oldValue, isDark) ->
                updateTheme(isDark)
        );

        darkModeRow.setOnMouseClicked(event -> {
            if (!isInsideNode(event.getTarget(), darkModeToggleButton)) {
                darkModeToggleButton.setSelected(!darkModeToggleButton.isSelected());
            }
        });
    }

    private void updateTheme(boolean isDark) {
        setDarkModeStyleClass(isDark);
        updateBrandLogo(isDark);
        updateThemeTextAndIcons(isDark);
    }

    private void setDarkModeStyleClass(boolean isDark) {
        if (isDark) {
            if (!appRoot.getStyleClass().contains(DARK_MODE_CLASS)) {
                appRoot.getStyleClass().add(DARK_MODE_CLASS);
            }
            return;
        }

        appRoot.getStyleClass().remove(DARK_MODE_CLASS);
    }

    private void updateThemeTextAndIcons(boolean isDark) {
        themeModeLabel.setText(isDark ? "Light Mode" : "Dark Mode");

        if (themeModeIcon != null) {
            themeModeIcon.setContent(isDark ? SUN_ICON_PATH : MOON_ICON_PATH);
        }

        if (darkModeToggleIcon != null) {
            darkModeToggleIcon.setContent(isDark ? MOON_ICON_PATH : SUN_ICON_PATH);
        }
    }

    private boolean isInsideNode(Object target, Node node) {
        if (!(target instanceof Node targetNode)) {
            return false;
        }

        Node current = targetNode;

        while (current != null) {
            if (current == node) {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }

    private void configureNavigation() {
        dashboardNavItem.setOnMouseClicked(event -> showDashboard());
        usersNavItem.setOnMouseClicked(event -> showUsers());
        profilesNavItem.setOnMouseClicked(event -> showProfiles());
        assignmentsNavItem.setOnMouseClicked(event -> showAssignments());
        metadataNavItem.setOnMouseClicked(event -> showMetadata());
        activityNavItem.setOnMouseClicked(event -> showActivity());
    }

    private void showDashboard() {
        showPage(DASHBOARD_VIEW, "Dashboard", dashboardNavItem);
    }

    private void showUsers() {
        showPage(USERS_VIEW, "Users", usersNavItem);
    }

    private void showProfiles() {
        showPage(PROFILES_VIEW, "Profiles", profilesNavItem);
    }

    private void showAssignments() {
        showPage(ASSIGNMENTS_VIEW, "Assignments", assignmentsNavItem);
    }

    private void showMetadata() {
        showPage(METADATA_VIEW, "Metadata", metadataNavItem);
    }

    private void showActivity() {
        showPage(ACTIVITY_VIEW, "Activity", activityNavItem);
    }

    private void showPage(String fxmlPath, String pageTitle, HBox activeNavItem) {
        loadPage(fxmlPath, pageTitle);
        setActiveNavItem(activeNavItem);
    }

    private void loadPage(String fxmlPath, String pageTitle) {
        URL pageUrl = getClass().getResource(fxmlPath);

        if (pageUrl == null) {
            contentHost.getChildren().setAll(createMissingPagePlaceholder(pageTitle));
            return;
        }

        try {
            Parent page = FXMLLoader.load(pageUrl);
            configureLoadedPageSize(page);
            contentHost.getChildren().setAll(page);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load page: " + fxmlPath, exception);
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

    private void setActiveNavItem(HBox activeNavItem) {
        for (HBox navItem : getNavigationItems()) {
            navItem.getStyleClass().setAll(
                    navItem == activeNavItem ? ACTIVE_NAV_CLASS : INACTIVE_NAV_CLASS
            );
        }
    }

    private List<HBox> getNavigationItems() {
        return List.of(
                dashboardNavItem,
                usersNavItem,
                profilesNavItem,
                assignmentsNavItem,
                metadataNavItem,
                activityNavItem
        );
    }
}