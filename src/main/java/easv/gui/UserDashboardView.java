package easv.gui;

import easv.gui.controller.DashboardController;
import easv.gui.controller.DashboardController.PageState;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class UserDashboardView {
    private final UserPortalModel portalModel;
    private final DashboardController controller;
    private final PortalViewComponents components;
    private final MainView mainView;
    private final ScanWorkspaceView scanWorkspaceView;
    private final QaReviewView qaReviewView;

    public UserDashboardView(UserPortalModel portalModel) {
        this.portalModel = portalModel;
        this.controller = new DashboardController(portalModel);
        this.components = new PortalViewComponents();
        this.mainView = new MainView(portalModel, controller, components);
        this.scanWorkspaceView = new ScanWorkspaceView(controller, components);
        this.qaReviewView = new QaReviewView(controller, components);
    }

    public Parent create() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");

        Runnable render = () -> {
            root.getStyleClass().remove("dark-theme");
            if (controller.darkMode()) {
                root.getStyleClass().add("dark-theme");
            }

            if (controller.currentPage() == PageState.SCAN_PROGRESS || controller.currentPage() == PageState.SCAN_COMPLETE) {
                root.setLeft(null);
            } else {
                root.setLeft(buildSidebar(controller.currentPage(), controller.darkMode()));
            }
            root.setCenter(buildContent(controller.currentPage()));
        };

        controller.setOnStateChanged(render);
        render.run();
        return root;
    }

    private VBox buildSidebar(PageState currentPage, boolean darkMode) {
        VBox sidebar = new VBox(16);
        sidebar.getStyleClass().add("portal-sidebar");
        sidebar.setPrefWidth(205);
        sidebar.setMinWidth(200);

        VBox brand = new VBox(14);
        brand.getStyleClass().add("sidebar-brand");

        HBox brandRow = new HBox();
        brandRow.setAlignment(Pos.CENTER_LEFT);
        ImageView logo = components.buildSidebarLogo(darkMode);
        brandRow.getChildren().add(logo);

        VBox navigation = new VBox(8);
        for (UserPortalModel.NavigationItem item : portalModel.sidebarItems()) {
            navigation.getChildren().add(buildNavItem(item, currentPage));
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox footer = new VBox(14);
        footer.getStyleClass().add("sidebar-footer");

        HBox languageRow = new HBox(10);
        languageRow.setAlignment(Pos.CENTER_LEFT);
        Label languageBadge = new Label("DK");
        languageBadge.getStyleClass().add("language-badge");
        Label languageText = new Label("Dansk");
        languageText.getStyleClass().add("footer-text");
        languageRow.getChildren().addAll(languageBadge, languageText);

        HBox logoutRow = new HBox(10);
        logoutRow.setAlignment(Pos.CENTER_LEFT);
        logoutRow.getStyleClass().add("logout-row");
        Label logoutIcon = new Label("<-");
        logoutIcon.getStyleClass().add("nav-icon");
        Label logoutText = new Label("Log Out");
        logoutText.getStyleClass().add("logout-text");
        logoutRow.getChildren().addAll(logoutIcon, logoutText);

        HBox darkModeRow = new HBox(10);
        darkModeRow.setAlignment(Pos.CENTER_LEFT);
        Label darkModeIcon = new Label("\u263E");
        darkModeIcon.getStyleClass().add("nav-icon");
        Label darkModeText = new Label("Dark Mode");
        darkModeText.getStyleClass().add("footer-text");
        Region darkSpacer = new Region();
        HBox.setHgrow(darkSpacer, Priority.ALWAYS);
        HBox darkSwitch = new HBox();
        darkSwitch.getStyleClass().add("dark-switch");
        if (darkMode) {
            darkSwitch.getStyleClass().add("dark-switch-active");
        }
        Label switchKnob = new Label("\u263C");
        switchKnob.getStyleClass().add("dark-switch-knob");
        if (darkMode) {
            switchKnob.getStyleClass().add("dark-switch-knob-active");
        }
        darkSwitch.getChildren().add(switchKnob);
        darkSwitch.setOnMouseClicked(event -> controller.toggleDarkMode());
        darkModeRow.getChildren().addAll(darkModeIcon, darkModeText, darkSpacer, darkSwitch);

        footer.getChildren().addAll(languageRow, logoutRow, darkModeRow);

        brand.getChildren().add(brandRow);
        sidebar.getChildren().addAll(brand, navigation, spacer, footer);
        return sidebar;
    }

    private HBox buildNavItem(UserPortalModel.NavigationItem item, PageState currentPage) {
        PageState page = controller.mapPage(item.label());
        boolean active = currentPage == page
                || ((currentPage == PageState.SCANNING || currentPage == PageState.QA_REVIEW) && page == PageState.PROFILES);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(active ? "sidebar-nav-item-active" : "sidebar-nav-item");

        Label icon = new Label(active ? "[]" : "-");
        icon.getStyleClass().add("nav-icon");
        Label text = new Label(item.label());
        text.getStyleClass().add(active ? "sidebar-nav-text-active" : "sidebar-nav-text");

        row.getChildren().addAll(icon, text);
        row.setOnMouseClicked(event -> controller.navigateTo(page));
        return row;
    }

    private Parent buildContent(PageState pageState) {
        BorderPane content = new BorderPane();
        content.getStyleClass().add("dashboard-shell");
        if (pageState != PageState.QA_REVIEW && pageState != PageState.SCAN_PROGRESS && pageState != PageState.SCAN_COMPLETE) {
            content.setTop(buildTopBar());
        }

        Parent pageBody = switch (pageState) {
            case DASHBOARD -> mainView.buildDashboardBody(controller::navigateTo);
            case PROFILES -> mainView.buildProfilesBody(
                    controller.selectedProfile(),
                    controller.currentBoxId(),
                    controller::selectProfile,
                    controller::startScanning
            );
            case LOGS -> mainView.buildLogsBody();
            case MY_SCANS -> mainView.buildMyScansBody();
            case SCAN_PROGRESS -> mainView.buildScanProgressBody(
                    controller.selectedProfile(),
                    controller.currentBoxId(),
                    controller.scanProgress(),
                    controller::showProfiles
            );
            case SCAN_COMPLETE -> mainView.buildScanCompleteBody(
                    controller.selectedProfile(),
                    controller.currentBoxId(),
                    controller::openScanningWorkspace,
                    controller::showProfiles
            );
            case SCANNING -> scanWorkspaceView.buildScanningBody(
                    controller.selectedProfile(),
                    controller.currentBoxId(),
                    controller.scanningDocuments(),
                    controller.scannedWorkspaceDocuments(),
                    controller.activeScanningDocument(),
                    controller::selectScanningDocument,
                    controller::openScanningFile,
                    controller::openQaReview,
                    controller::refresh,
                    controller::showProfiles,
                    controller::scanAllDocuments
            );
            case FILE_PAGES -> scanWorkspaceView.buildFilePagesBody(
                    controller.selectedProfile(),
                    controller.currentBoxId(),
                    controller.activeScanningDocument(),
                    controller.activeScanningFile(),
                    controller::openScanningWorkspace
            );
            case QA_REVIEW -> qaReviewView.buildQaReviewBody(
                    controller.selectedProfile(),
                    controller.currentBoxId(),
                    controller.scannedWorkspaceDocuments(),
                    controller.activeScanningDocument(),
                    controller.activeScanningFile(),
                    controller.qaCurrentPage(),
                    controller.qaRotation(),
                    controller.qaZoom(),
                    controller.qaPanX(),
                    controller.qaPanY(),
                    controller.qaHighlight(),
                    controller.qaApproved(),
                    controller.qaHighlightedText(),
                    controller.selectedTiffAction(),
                    controller::selectQaFile,
                    controller::openScanningWorkspace,
                    controller::showDashboard,
                    controller::exportQa,
                    controller::stepQaPage,
                    controller::rotateQaPage,
                    controller::zoomQaPage,
                    delta -> controller.panQaPage(delta[0], delta[1]),
                    controller::toggleQaHighlight,
                    controller::toggleQaTextHighlight,
                    controller::approveQa,
                    controller::rejectQa,
                    controller::selectTiffAction
            );
        };
        content.setCenter(pageBody);
        return content;
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("dashboard-topbar");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label notification = new Label("o");
        notification.getStyleClass().add("topbar-icon");
        Label userBadge = new Label("JD");
        userBadge.getStyleClass().add("topbar-user");

        topBar.getChildren().addAll(spacer, notification, userBadge);
        return topBar;
    }
}
