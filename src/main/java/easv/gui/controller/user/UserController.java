package easv.gui.controller.user;

import easv.be.User;
import easv.bll.KeyboardShortcut;
import easv.bll.ShortcutManager;
import easv.bll.UserSession;
import easv.gui.UserPortalModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.prefs.Preferences;

public class UserController implements UserNavigator {

    private static final String ACTIVE_NAV_CLASS = "active";
    private static final String DARK_MODE_CLASS = "dark";
    private static final double COMPACT_NAV_WIDTH = 1180;
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

    @FXML private ToggleButton dashboardNavItem;
    @FXML private ToggleButton scanNavItem;
    @FXML private ToggleButton myScansNavItem;
    @FXML private ToggleButton assignedQANavItem;
    @FXML private ToggleButton exportsNavItem;
    @FXML private ToggleButton helpNavItem;
    @FXML private ToggleButton settingsNavItem;

    @FXML private ToggleButton darkModeToggleButton;
    @FXML private SVGPath darkModeToggleIcon;

    private final UserPortalModel portalModel = new UserPortalModel();
    private final ShortcutManager shortcutManager = new ShortcutManager();
    private final Preferences preferences = Preferences.userRoot().node(PREFERENCES_NODE);
    private Object activePageController;
    private Scene shortcutScene;
    private boolean rootShortcutFiltersRegistered;
    private UserPage currentPage = UserPage.DASHBOARD;

    @FXML
    private void initialize() {
        configureShell();
        configureThemeToggle();
        configureNavigation();
        configureResponsiveNavigation();
        configureGlobalHelpShortcuts();
        showPage(UserPage.DASHBOARD);
    }

    private void configureShell() {
        UserPortalModel.AccountProfile fallbackProfile = portalModel.fetchAccountProfile();
        User currentUser = UserSession.getCurrentUser();

        String accountName = currentUser == null || currentUser.getName().isBlank()
                ? fallbackProfile.fullName()
                : currentUser.getName();

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
    }

    private void configureNavigation() {
        for (UserPage page : UserPage.values()) {
            ToggleButton navItem = getNavItem(page);

            if (navItem != null) {
                installCloseIcon(navItem);
                navItem.setOnAction(event -> showPage(page));
            }
        }
    }

    private void configureResponsiveNavigation() {
        if (appRoot == null) {
            return;
        }

        appRoot.widthProperty().addListener((observable, oldWidth, newWidth) ->
                updateNavigationLabelVisibility(newWidth.doubleValue())
        );

        // JavaFX knows the real window width only after the scene is visible.
        Platform.runLater(() -> updateNavigationLabelVisibility(appRoot.getWidth()));
    }

    private void updateNavigationLabelVisibility(double width) {
        boolean showLabels = width <= 0 || width >= COMPACT_NAV_WIDTH;

        for (ToggleButton navItem : getNavigationItems()) {
            updateNavLabelVisibility(navItem, showLabels);
        }
    }

    private void updateNavLabelVisibility(ToggleButton navItem, boolean visible) {
        if (navItem == null || !(navItem.getGraphic() instanceof HBox graphicBox)) {
            return;
        }

        for (Node child : graphicBox.getChildren()) {
            if (child.getStyleClass().contains("admin-top-nav-label")) {
                child.setVisible(visible);
                child.setManaged(visible);
            }
        }
    }

    private void installCloseIcon(ToggleButton navItem) {
        if (!(navItem.getGraphic() instanceof HBox graphicBox)) {
            return;
        }

        Label closeIcon = new Label("x");
        closeIcon.getStyleClass().add("admin-nav-close-icon");
        closeIcon.setVisible(false);
        closeIcon.setManaged(false);
        closeIcon.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> event.consume());
        closeIcon.setOnMouseClicked(event -> {
            showPage(UserPage.DASHBOARD);
            event.consume();
        });

        graphicBox.getChildren().add(closeIcon);
    }

    private void configureGlobalHelpShortcuts() {
        registerRootShortcutFilters();

        Platform.runLater(() -> {
            Scene scene = appRoot.getScene();

            if (scene != null) {
                registerHelpShortcuts(scene);
                return;
            }

            // If the scene is not ready yet, install shortcuts as soon as JavaFX attaches it.
            appRoot.sceneProperty().addListener((observable, oldScene, newScene) -> {
                if (newScene != null) {
                    registerHelpShortcuts(newScene);
                }
            });
        });
    }

    private void registerRootShortcutFilters() {
        if (appRoot == null || rootShortcutFiltersRegistered) {
            return;
        }

        // The root filter is a safety net when JavaFX focus is inside a child control.
        appRoot.addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcut);
        appRoot.addEventFilter(KeyEvent.KEY_TYPED, this::handleGlobalTypedShortcut);
        rootShortcutFiltersRegistered = true;
    }

    private void registerHelpShortcuts(Scene scene) {
        if (scene == shortcutScene) {
            return;
        }

        if (shortcutScene != null) {
            shortcutScene.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcut);
            shortcutScene.removeEventFilter(KeyEvent.KEY_TYPED, this::handleGlobalTypedShortcut);
        }

        shortcutScene = scene;

        // The scene receives key presses from every user page, not only the focused panel.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcut);
        scene.addEventFilter(KeyEvent.KEY_TYPED, this::handleGlobalTypedShortcut);
        scene.getAccelerators().put(KeyCombination.valueOf("F1"), this::showKeyboardShortcutsDialog);
        scene.getAccelerators().put(KeyCombination.valueOf("SHIFT+SLASH"), this::showKeyboardShortcutsDialog);
    }

    private void handleGlobalShortcut(KeyEvent event) {
        if (event.isConsumed()) {
            return;
        }

        if (isShortcutHelp(event)) {
            showKeyboardShortcutsDialog();
            event.consume();
            return;
        }

        if (activePageController instanceof ScanController scanController
                && scanController.handleGlobalShortcut(event)) {
            event.consume();
            return;
        }

        // Scan shortcuts are still reachable from other user pages: open scan, then run the same shortcut.
        if (!(event.getTarget() instanceof TextInputControl) && isScanShortcut(event)) {
            KeyCode code = event.getCode();
            boolean shortcutDown = event.isShortcutDown();
            String typedText = event.getText();

            showPage(UserPage.SCAN);
            Platform.runLater(() -> runShortcutOnActiveScanPage(code, shortcutDown, typedText));
            event.consume();
        }
    }

    private void handleGlobalTypedShortcut(KeyEvent event) {
        if (event.isConsumed()) {
            return;
        }

        if (event.getTarget() instanceof TextInputControl) {
            return;
        }

        String character = event.getCharacter();

        if ("?".equals(character)) {
            showKeyboardShortcutsDialog();
            event.consume();
            return;
        }

        if (activePageController instanceof ScanController scanController
                && scanController.runTypedShortcut(character)) {
            event.consume();
            return;
        }

        if ("+".equals(character) || "-".equals(character)) {
            showPage(UserPage.SCAN);
            Platform.runLater(() -> runTypedShortcutOnActiveScanPage(character));
            event.consume();
        }
    }

    private boolean isShortcutHelp(KeyEvent event) {
        return event.getCode() == KeyCode.F1
                || (event.isShiftDown() && event.getCode() == KeyCode.SLASH)
                || "?".equals(event.getText());
    }

    private boolean isScanShortcut(KeyEvent event) {
        return event.getCode() == KeyCode.RIGHT
                || event.getCode() == KeyCode.LEFT
                || event.getCode() == KeyCode.R
                || event.getCode() == KeyCode.DELETE
                || event.getCode() == KeyCode.PLUS
                || event.getCode() == KeyCode.ADD
                || event.getCode() == KeyCode.EQUALS
                || event.getCode() == KeyCode.MINUS
                || event.getCode() == KeyCode.SUBTRACT
                || "+".equals(event.getText())
                || "-".equals(event.getText())
                || event.getCode() == KeyCode.ESCAPE
                || (event.isShortcutDown() && (
                event.getCode() == KeyCode.Z
                        || event.getCode() == KeyCode.S
                        || event.getCode() == KeyCode.F
                        || event.getCode() == KeyCode.E
        ));
    }

    private void runShortcutOnActiveScanPage(KeyCode code, boolean shortcutDown, String typedText) {
        if (activePageController instanceof ScanController scanController) {
            scanController.runShortcut(code, shortcutDown, typedText);
        }
    }

    private void runTypedShortcutOnActiveScanPage(String character) {
        if (activePageController instanceof ScanController scanController) {
            scanController.runTypedShortcut(character);
        }
    }

    private void showHelpPage() {
        showPage(UserPage.HELP);
    }

    private void showKeyboardShortcutsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Keyboard Shortcuts");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().addAll("app-shell", "shortcut-help-dialog-pane");

        if (appRoot != null && appRoot.getStyleClass().contains(DARK_MODE_CLASS)) {
            dialog.getDialogPane().getStyleClass().add(DARK_MODE_CLASS);
        }

        addAppStylesheet(dialog);
        dialog.getDialogPane().setContent(createShortcutHelpDialogContent(
                "Keyboard Shortcuts",
                false
        ));
        dialog.setResizable(true);

        if (appRoot != null && appRoot.getScene() != null) {
            dialog.initOwner(appRoot.getScene().getWindow());
        }

        dialog.showAndWait();
    }

    private void addAppStylesheet(Dialog<?> dialog) {
        URL stylesheetUrl = getClass().getResource("/css/app.css");

        if (stylesheetUrl != null) {
            dialog.getDialogPane().getStylesheets().add(stylesheetUrl.toExternalForm());
        }
    }

    @Override
    public void showPage(UserPage page) {
        currentPage = page;
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
            activePageController = null;
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

            activePageController = loader.getController();
            configureLoadedController(activePageController);
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
            case HELP -> createHelpPage();
            case SETTINGS -> new SettingsController(portalModel).create();
            default -> createMissingPagePlaceholder(page.title());
        };
    }

    private VBox createHelpPage() {
        VBox page = createShortcutHelpContent(
                "Keyboard Shortcuts",
                true
        );
        page.getStyleClass().add("page-content");
        page.setMaxWidth(Double.MAX_VALUE);
        return page;
    }

    private ScrollPane createShortcutHelpDialogContent(String titleText, boolean fullPage) {
        ScrollPane scrollPane = new ScrollPane(createShortcutHelpContent(titleText, fullPage));
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(900);
        scrollPane.setPrefViewportHeight(620);
        scrollPane.getStyleClass().add("shortcut-help-scroll");
        return scrollPane;
    }

    private VBox createShortcutHelpContent(String titleText, boolean fullPage) {
        VBox content = new VBox(0);
        content.getStyleClass().add("shortcut-help-shell");

        HBox header = createShortcutHelpHeader(titleText);

        GridPane shortcutGrid = createShortcutGrid();

        VBox body = new VBox(shortcutGrid);
        body.getStyleClass().add("shortcut-help-body");

        content.getChildren().addAll(header, body);

        if (fullPage) {
            content.setMaxWidth(Double.MAX_VALUE);
        } else {
            content.setPrefSize(900, 620);
        }

        return content;
    }

    private HBox createShortcutHelpHeader(String titleText) {
        Label keyboardIcon = new Label("KBD");
        keyboardIcon.getStyleClass().add("shortcut-help-header-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("shortcut-help-main-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(18, keyboardIcon, title, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("shortcut-help-header");
        return header;
    }

    private GridPane createShortcutGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(22);
        grid.setVgap(14);

        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(50);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(50);
        grid.getColumnConstraints().addAll(leftColumn, rightColumn);

        List<KeyboardShortcut> shortcuts = shortcutManager.getShortcuts();

        for (int index = 0; index < shortcuts.size(); index++) {
            int column = index % 2;
            int row = index / 2;
            grid.add(createShortcutCard(shortcuts.get(index)), column, row);
        }

        return grid;
    }

    private HBox createShortcutCard(KeyboardShortcut shortcut) {
        Label icon = new Label(shortcutIcon(shortcut));
        icon.getStyleClass().add("shortcut-help-icon");

        Label keys = new Label(shortcut.getDisplayKeys());
        keys.getStyleClass().add("settings-shortcut-key");

        Label actionName = new Label(shortcut.getActionName());
        actionName.getStyleClass().add("shortcut-help-title");

        HBox heading = new HBox(8, keys, actionName);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label action = new Label(shortcut.getDescription());
        action.getStyleClass().add("settings-shortcut-copy");
        action.setWrapText(true);

        VBox copy = new VBox(4, heading, action);
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox card = new HBox(12, icon, copy);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("shortcut-help-card");
        return card;
    }

    private String shortcutIcon(KeyboardShortcut shortcut) {
        return switch (shortcut.getActionName()) {
            case "Next page", "Previous page" -> "NAV";
            case "Rotate" -> "ROT";
            case "Delete" -> "DEL";
            case "Undo" -> "UNDO";
            case "Save" -> "SAVE";
            case "Search / jump" -> "FIND";
            case "Export" -> "EXP";
            case "Zoom in", "Zoom out" -> "ZOOM";
            case "Escape" -> "ESC";
            default -> "HELP";
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

        updateNavCloseIcon(navItem, active && currentPage != UserPage.DASHBOARD);
    }

    private void updateNavCloseIcon(ToggleButton navItem, boolean visible) {
        if (!(navItem.getGraphic() instanceof HBox graphicBox)) {
            return;
        }

        for (Node child : graphicBox.getChildren()) {
            if (child.getStyleClass().contains("admin-nav-close-icon")) {
                child.setVisible(visible);
                child.setManaged(visible);
            }
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
                exportsNavItem,
                helpNavItem,
                settingsNavItem
        );
    }

    private ToggleButton getNavItem(UserPage page) {
        return switch (page) {
            case DASHBOARD -> dashboardNavItem;
            case SCAN -> scanNavItem;
            case MY_SCANS -> myScansNavItem;
            case ASSIGNED_QA -> assignedQANavItem;
            case EXPORTS -> exportsNavItem;
            case HELP -> helpNavItem;
            case SETTINGS -> settingsNavItem;
        };
    }
}
