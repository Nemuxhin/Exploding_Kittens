package easv.gui.controller.admin;

import easv.be.ScanProfile;
import easv.be.User;
import easv.bll.AdminManager;
import easv.util.Strings;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class ProfilesController {

    private static final String ALL_STATUSES = "All Statuses";

    private static final double PAGE_HORIZONTAL_PADDING = 48;
    private static final double CARD_GAP = 18;
    private static final double MIN_CARD_WIDTH = 330;

    private static final String PREVIEW_BOX_ID = "BOX-2026-004";
    private static final int MAX_VISIBLE_ASSIGNED_USERS = 5;

    @FXML private ScrollPane pageScrollPane;

    @FXML private VBox overviewPane;
    @FXML private VBox editorPane;

    @FXML private Label profilesCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private GridPane profilesCardsGrid;
    @FXML private VBox emptyStateBox;

    @FXML private Label editorTitleLabel;
    @FXML private Label editorSubtitleLabel;
    @FXML private Label editorStatusBadge;
    @FXML private Button deleteProfileButton;
    @FXML private Button previewExportButton;
    @FXML private Button saveProfileButton;

    @FXML private Button generalTabButton;
    @FXML private Button scanRulesTabButton;
    @FXML private Button accessTabButton;
    @FXML private Button previewTabButton;

    @FXML private VBox generalTabPane;
    @FXML private VBox scanRulesTabPane;
    @FXML private VBox accessTabPane;
    @FXML private VBox previewTabPane;

    @FXML private TextField profileNameField;
    @FXML private TextField profileClientField;
    @FXML private TextArea profileDescriptionArea;
    @FXML private ComboBox<String> profileStatusComboBox;

    @FXML private ToggleButton barcodeSplitToggle;
    @FXML private ComboBox<String> barcodeDetectedComboBox;
    @FXML private ComboBox<String> barcodePageBehaviorComboBox;

    @FXML private ComboBox<String> defaultRotationComboBox;
    @FXML private ComboBox<String> brightnessComboBox;
    @FXML private ComboBox<String> contrastComboBox;
    @FXML private ToggleButton deskewToggle;
    @FXML private ToggleButton qaRequiredToggle;

    @FXML private ComboBox<String> exportFormatComboBox;
    @FXML private TextField exportNamingField;
    @FXML private Label exportNamingPreviewLabel;

    @FXML private Label accessCountLabel;
    @FXML private VBox accessRowsContainer;

    @FXML private Label previewProfileNameLabel;
    @FXML private Label previewBoxIdLabel;
    @FXML private Label previewExportFolderLabel;
    @FXML private Label previewBarcodeLabel;
    @FXML private Label previewPageCorrectionLabel;
    @FXML private Label previewQaRequiredLabel;
    @FXML private Label previewExportFormatLabel;

    private final ObservableList<ScanProfile> masterProfiles = FXCollections.observableArrayList();

    private FilteredList<ScanProfile> filteredProfiles;
    private ScanProfile currentProfile;
    private boolean creatingProfile;

    private AdminManager adminManager;
    private AdminNavigator navigator = AdminNavigator.none();

    void setNavigator(AdminNavigator navigator) {
        this.navigator = navigator == null ? AdminNavigator.none() : navigator;
    }

    void setAdminManager(AdminManager adminManager) {
        this.adminManager = adminManager;
        if (this.adminManager == null) {
            return;
        }
        loadProfiles();
        applyFilters();
    }

    @FXML
    private void initialize() {
        configureOverviewFilters();
        configureEditorControls();
        configureFiltering();
        configureResponsiveGrid();

        applyFilters();
        showOverviewPane();
    }

    private void configureOverviewFilters() {
        statusFilterComboBox.getItems().setAll(
                ALL_STATUSES,
                "Active",
                "Draft",
                "Archived"
        );
        statusFilterComboBox.setValue(ALL_STATUSES);

    }

    private void configureEditorControls() {
        profileStatusComboBox.getItems().setAll("Active", "Draft", "Archived");

        barcodeDetectedComboBox.getItems().setAll(
                "Start new document",
                "End current document",
                "Stop scanning and ask user",
                "Continue scanning and split automatically"
        );

        barcodePageBehaviorComboBox.getItems().setAll(
                "Remove barcode page from final document",
                "Keep barcode page in final document",
                "Move barcode page to separate document"
        );

        defaultRotationComboBox.getItems().setAll("0 deg", "90 deg", "180 deg", "270 deg");
        brightnessComboBox.getItems().setAll("Normal", "Lighter", "Darker");
        contrastComboBox.getItems().setAll("Normal", "Higher", "Lower");

        exportFormatComboBox.getItems().setAll(
                "Multi-page TIFF",
                "Single-page TIFF"
        );

        profileNameField.textProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        exportNamingField.textProperty().addListener((observable, oldValue, newValue) -> syncPreview());

        barcodeSplitToggle.selectedProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        deskewToggle.selectedProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        qaRequiredToggle.selectedProperty().addListener((observable, oldValue, newValue) -> syncPreview());

        barcodeDetectedComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        barcodePageBehaviorComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        defaultRotationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        brightnessComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        contrastComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        exportFormatComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());

        profileNameField.textProperty().addListener((observable, oldValue, newValue) -> updateEditorActionState());
        profileClientField.textProperty().addListener((observable, oldValue, newValue) -> updateEditorActionState());
        profileStatusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateEditorActionState());
    }

    private void configureFiltering() {
        filteredProfiles = new FilteredList<>(masterProfiles, profile -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void configureResponsiveGrid() {
        pageScrollPane.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) -> layoutProfileGrid());
        profilesCardsGrid.widthProperty().addListener((observable, oldWidth, newWidth) -> layoutProfileGrid());

        Platform.runLater(this::layoutProfileGrid);
    }

    private void loadProfiles() {
        if (adminManager == null) {
            masterProfiles.clear();
            return;
        }

        masterProfiles.setAll(adminManager.getProfiles());
    }

    private void applyFilters() {
        if (filteredProfiles == null) {
            return;
        }

        String searchText = Strings.normalize(searchField.getText());
        String selectedStatus = statusFilterComboBox.getValue();

        filteredProfiles.setPredicate(profile ->
                matchesSearch(profile, searchText)
                        && matchesStatus(profile, selectedStatus)
        );

        renderProfileCards();
    }

    private boolean matchesSearch(ScanProfile profile, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return Strings.normalize(profile.getName()).contains(searchText)
                || Strings.normalize(profile.getDescription()).contains(searchText)
                || Strings.normalize(profile.getExportNaming()).contains(searchText)
                || Strings.normalize(displayStatus(profile)).contains(searchText)
                || configChipsFor(profile).stream()
                .anyMatch(chip -> Strings.normalize(chip.label()).contains(searchText));
    }

    private boolean matchesStatus(ScanProfile profile, String selectedStatus) {
        return selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || displayStatus(profile).equalsIgnoreCase(selectedStatus);
    }

    private void renderProfileCards() {
        List<VBox> cards = filteredProfiles.stream()
                .map(this::buildProfileCard)
                .toList();

        profilesCardsGrid.getChildren().setAll(cards);

        int count = filteredProfiles.size();
        profilesCountLabel.setText(count == 1 ? "1 profile" : count + " profiles");

        boolean hasProfiles = count > 0;

        profilesCardsGrid.setVisible(hasProfiles);
        profilesCardsGrid.setManaged(hasProfiles);

        emptyStateBox.setVisible(!hasProfiles);
        emptyStateBox.setManaged(!hasProfiles);

        Platform.runLater(this::layoutProfileGrid);
    }

    private void layoutProfileGrid() {
        int cardCount = profilesCardsGrid.getChildren().size();

        if (cardCount == 0) {
            profilesCardsGrid.getColumnConstraints().clear();
            profilesCardsGrid.getRowConstraints().clear();
            return;
        }

        double viewportWidth = pageScrollPane.getViewportBounds().getWidth();
        double availableWidth = viewportWidth - PAGE_HORIZONTAL_PADDING;

        if (availableWidth <= 0) {
            return;
        }

        int columns = calculateColumns(availableWidth, cardCount);
        double cardWidth = calculateCardWidth(availableWidth, columns);

        profilesCardsGrid.setMinWidth(0);
        profilesCardsGrid.setPrefWidth(availableWidth);
        profilesCardsGrid.setMaxWidth(Double.MAX_VALUE);

        profilesCardsGrid.getColumnConstraints().setAll(createColumnConstraints(columns, cardWidth));
        profilesCardsGrid.getRowConstraints().clear();

        for (int index = 0; index < cardCount; index++) {
            Node card = profilesCardsGrid.getChildren().get(index);

            int column = index % columns;
            int row = index / columns;

            GridPane.setColumnIndex(card, column);
            GridPane.setRowIndex(card, row);
            GridPane.setFillWidth(card, true);
            GridPane.setHgrow(card, Priority.ALWAYS);

            if (card instanceof Region region) {
                region.setMinWidth(0);
                region.setPrefWidth(cardWidth);
                region.setMaxWidth(Double.MAX_VALUE);
            }
        }
    }

    private int calculateColumns(double availableWidth, int cardCount) {
        int columns = (int) Math.floor((availableWidth + CARD_GAP) / (MIN_CARD_WIDTH + CARD_GAP));

        columns = Math.max(1, columns);
        columns = Math.min(columns, cardCount);

        return columns;
    }

    private double calculateCardWidth(double availableWidth, int columns) {
        double totalGapWidth = CARD_GAP * (columns - 1);
        return Math.floor((availableWidth - totalGapWidth) / columns);
    }

    private List<ColumnConstraints> createColumnConstraints(int columns, double cardWidth) {
        return IntStream.range(0, columns)
                .mapToObj(index -> {
                    ColumnConstraints constraints = new ColumnConstraints();
                    constraints.setMinWidth(0);
                    constraints.setPrefWidth(cardWidth);
                    constraints.setMaxWidth(cardWidth);
                    constraints.setHgrow(Priority.ALWAYS);
                    constraints.setFillWidth(true);
                    return constraints;
                })
                .toList();
    }

    private VBox buildProfileCard(ScanProfile profile) {
        VBox card = new VBox(14);
        card.getStyleClass().add("profile-card");

        if (profile.isArchived()) {
            card.getStyleClass().add("profile-card-archived");
        }

        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);

        Region footerSpacer = new Region();
        footerSpacer.getStyleClass().add("profile-card-footer-spacer");
        VBox.setVgrow(footerSpacer, Priority.ALWAYS);

        card.getChildren().addAll(
                buildHeaderRow(profile),
                buildChipsPane(profile),
                createDivider(),
                buildInfoBox(profile),
                footerSpacer,
                createDivider(),
                buildFooterRow(profile)
        );

        return card;
    }

    private HBox buildHeaderRow(ScanProfile profile) {
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.TOP_LEFT);

        VBox titleBox = new VBox(4);
        titleBox.setMinWidth(0);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label nameLabel = new Label(profile.getName());
        nameLabel.getStyleClass().add("profile-card-title");

        Label descriptionLabel = new Label(profile.getDescription());
        descriptionLabel.getStyleClass().add("profile-card-description");
        descriptionLabel.setWrapText(true);

        titleBox.getChildren().addAll(nameLabel, descriptionLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(displayStatus(profile));
        statusBadge.getStyleClass().addAll("profile-status-badge", statusClassFor(displayStatus(profile)));
        statusBadge.setMinWidth(Region.USE_PREF_SIZE);
        statusBadge.setMaxWidth(Region.USE_PREF_SIZE);

        headerRow.getChildren().addAll(titleBox, spacer, statusBadge);
        return headerRow;
    }

    private FlowPane buildChipsPane(ScanProfile profile) {
        FlowPane chipsPane = new FlowPane();
        chipsPane.getStyleClass().add("profile-chip-group");
        chipsPane.setHgap(8);
        chipsPane.setVgap(8);

        for (ConfigChip chip : configChipsFor(profile)) {
            Label chipLabel = new Label(chip.label());
            chipLabel.getStyleClass().addAll("profile-config-chip", chip.styleClass());
            chipsPane.getChildren().add(chipLabel);
        }

        return chipsPane;
    }

    private VBox buildInfoBox(ScanProfile profile) {
        VBox infoBox = new VBox(10);
        infoBox.getChildren().addAll(
                buildInfoBlock("Export Naming", Strings.displayText(profile.getExportNaming(), ScanProfile.DEFAULT_EXPORT_NAMING), true),
                buildAssignedUsersBlock(profile),
                buildInfoBlock("Updated", Strings.displayText(profile.getLastUpdated(), "Not updated yet"), false)
        );

        return infoBox;
    }

    private HBox buildFooterRow(ScanProfile profile) {
        HBox footerRow = new HBox(10);
        footerRow.getStyleClass().add("profile-card-footer");
        footerRow.setAlignment(Pos.CENTER_LEFT);

        Button openButton = createActionButton("Open", "profile-open-button");
        openButton.setOnAction(event -> openProfile(profile));

        Button archiveButton = createActionButton(profile.isArchived() ? "Restore" : "Archive", "profile-secondary-button");
        archiveButton.setOnAction(event -> toggleArchive(profile));

        Button deleteButton = createActionButton("Delete", "profile-danger-button");
        deleteButton.setOnAction(event -> deleteProfile(profile));

        footerRow.getChildren().addAll(openButton, archiveButton, deleteButton);
        return footerRow;
    }

    private VBox buildInfoBlock(String labelText, String valueText, boolean codeStyle) {
        VBox box = new VBox(4);

        Label label = new Label(labelText);
        label.getStyleClass().add("profile-info-label");

        Label value = new Label(valueText);
        value.getStyleClass().add(codeStyle ? "profile-info-code" : "profile-info-value");
        value.setWrapText(true);

        box.getChildren().addAll(label, value);
        return box;
    }

    private VBox buildAssignedUsersBlock(ScanProfile profile) {
        VBox box = new VBox(7);

        Label label = new Label("Assigned Users");
        label.getStyleClass().add("profile-info-label");

        List<User> assignedUsers = usersAssignedTo(profile);

        Label value = new Label(formatAssignedUsers(assignedUsers.size()));
        value.getStyleClass().add("profile-info-value");
        value.setWrapText(true);

        HBox avatars = buildAssignedUsersStrip(assignedUsers);

        box.getChildren().addAll(label, value);

        if (!assignedUsers.isEmpty()) {
            box.getChildren().add(avatars);
        }

        return box;
    }

    private HBox buildAssignedUsersStrip(List<User> assignedUsers) {
        HBox strip = new HBox(-6);
        strip.getStyleClass().add("profile-assigned-users-strip");
        strip.setAlignment(Pos.CENTER_LEFT);

        int visibleUserCount = Math.min(assignedUsers.size(), MAX_VISIBLE_ASSIGNED_USERS);

        for (int index = 0; index < visibleUserCount; index++) {
            strip.getChildren().add(buildAssignedUserAvatar(assignedUsers.get(index), index));
        }

        int hiddenUserCount = assignedUsers.size() - visibleUserCount;

        if (hiddenUserCount > 0) {
            strip.getChildren().add(buildOverflowAvatar(hiddenUserCount));
        }

        return strip;
    }

    private StackPane buildAssignedUserAvatar(User user, int index) {
        Label initialsLabel = new Label(Strings.initials(user.getName(), "?"));
        initialsLabel.getStyleClass().add("profile-assigned-avatar-initials");

        StackPane avatar = new StackPane(initialsLabel);
        avatar.getStyleClass().addAll(
                "profile-assigned-avatar",
                "profile-assigned-avatar-" + ((index % 4) + 1)
        );
        Tooltip.install(avatar, new Tooltip(user.getName()));

        return avatar;
    }

    private StackPane buildOverflowAvatar(int hiddenUserCount) {
        Label countLabel = new Label("+" + hiddenUserCount);
        countLabel.getStyleClass().add("profile-assigned-avatar-initials");

        StackPane avatar = new StackPane(countLabel);
        avatar.getStyleClass().addAll("profile-assigned-avatar", "profile-assigned-avatar-overflow");

        return avatar;
    }

    private Region createDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("profile-card-divider");
        divider.setPrefHeight(1);
        VBox.setMargin(divider, new Insets(2, 0, 2, 0));
        return divider;
    }

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setContentDisplay(ContentDisplay.TEXT_ONLY);
        button.setFocusTraversable(false);
        return button;
    }

    private void openProfile(ScanProfile profile) {
        creatingProfile = false;
        currentProfile = profile;

        populateEditor(profile);
        showEditorPane();
        selectTab(EditorTab.GENERAL);

        Platform.runLater(() -> pageScrollPane.setVvalue(0));
    }

    private void populateEditor(ScanProfile profile) {
        editorTitleLabel.setText(profile.getName() + " Profile");
        editorSubtitleLabel.setText(profile.getDescription());

        editorStatusBadge.setText(displayStatus(profile));
        editorStatusBadge.getStyleClass().setAll("profile-status-badge", statusClassFor(displayStatus(profile)));

        profileNameField.setText(profile.getName());
        profileClientField.setText(profile.getClient());
        profileDescriptionArea.setText(profile.getDescription());
        profileStatusComboBox.setValue(displayStatus(profile));

        barcodeSplitToggle.setSelected(profile.isBarcodeSplitting());
        barcodeDetectedComboBox.setValue(profile.getBarcodeDetectedBehavior());
        barcodePageBehaviorComboBox.setValue(profile.getBarcodePageBehavior());

        defaultRotationComboBox.setValue(profile.getDefaultRotation());
        brightnessComboBox.setValue(profile.getBrightness());
        contrastComboBox.setValue(profile.getContrast());
        deskewToggle.setSelected(profile.isDeskew());
        qaRequiredToggle.setSelected(profile.isMetadataRequiredBeforeExport());

        exportFormatComboBox.setValue(profile.getExportFormat());
        exportNamingField.setText(profile.getExportNaming());

        populateAccessRows(profile);
        updateEditorActionState();
        syncPreview();
    }

    private void populateCreateProfileEditor() {
        creatingProfile = true;
        currentProfile = null;

        editorTitleLabel.setText("Create Profile");
        editorSubtitleLabel.setText("Add the required details, then save to create the profile.");
        editorStatusBadge.setText("Draft");
        editorStatusBadge.getStyleClass().setAll("profile-status-badge", statusClassFor("Draft"));

        profileNameField.clear();
        profileClientField.clear();
        profileDescriptionArea.clear();
        profileStatusComboBox.setValue("Draft");

        barcodeSplitToggle.setSelected(false);
        barcodeDetectedComboBox.setValue("Start new document");
        barcodePageBehaviorComboBox.setValue("Remove barcode page from final document");

        defaultRotationComboBox.setValue("0 deg");
        brightnessComboBox.setValue("Normal");
        contrastComboBox.setValue("Normal");
        deskewToggle.setSelected(true);
        qaRequiredToggle.setSelected(true);

        exportFormatComboBox.setValue("Multi-page TIFF");
        exportNamingField.setText(ScanProfile.DEFAULT_EXPORT_NAMING);

        populateCreateAccessRows();
        updateEditorActionState();
        syncPreview();
    }

    private void populateCreateAccessRows() {
        accessCountLabel.setText("0 users assigned");
        accessRowsContainer.getChildren().clear();

        Label emptyLabel = new Label("Create the profile before assigning users.");
        emptyLabel.getStyleClass().add("profile-editor-helper-text");
        accessRowsContainer.getChildren().add(emptyLabel);
    }

    private void updateEditorActionState() {
        if (saveProfileButton != null) {
            saveProfileButton.setText(creatingProfile ? "Create Profile" : "Save Changes");
            saveProfileButton.setDisable(creatingProfile && !isEditorProfileReady());
        }

        setVisibleAndManaged(deleteProfileButton, !creatingProfile);
        setVisibleAndManaged(previewExportButton, true);
        setVisibleAndManaged(accessTabButton, !creatingProfile);
    }

    private boolean isEditorProfileReady() {
        return !Strings.clean(profileNameField.getText()).isBlank()
                && !Strings.clean(profileClientField.getText()).isBlank()
                && !Strings.clean(profileStatusComboBox.getValue()).isBlank();
    }

    private void populateAccessRows(ScanProfile profile) {
        int assignedUsersCount = assignedUserCountFor(profile);
        accessCountLabel.setText(formatAssignedUsers(assignedUsersCount));

        accessRowsContainer.getChildren().clear();

        List<User> assignedUsers = usersAssignedTo(profile);

        if (assignedUsers.isEmpty()) {
            Label emptyLabel = new Label("No users assigned to this profile yet.");
            emptyLabel.getStyleClass().add("profile-editor-helper-text");
            accessRowsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (int index = 0; index < assignedUsers.size(); index++) {
            User user = assignedUsers.get(index);
            accessRowsContainer.getChildren().add(buildAccessRow(user, index == assignedUsers.size() - 1));
        }
    }

    private HBox buildAccessRow(User user, boolean lastRow) {
        HBox row = new HBox(9);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("profile-editor-access-row");

        if (lastRow) {
            row.getStyleClass().add("profile-editor-access-row-last");
        }

        Label nameLabel = new Label(user.getName());
        nameLabel.getStyleClass().add("profile-editor-access-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label roleBadge = new Label(user.getRole());
        roleBadge.getStyleClass().addAll(
                "profile-editor-small-badge",
                user.getRole().equalsIgnoreCase("Admin")
                        ? "profile-editor-admin-badge"
                        : "profile-editor-user-badge"
        );

        Label statusBadge = new Label(user.getStatus());
        statusBadge.getStyleClass().addAll("profile-editor-small-badge", "profile-editor-active-badge");

        row.getChildren().addAll(nameLabel, spacer, roleBadge, statusBadge);
        return row;
    }

    private void syncPreview() {
        String profileName = Strings.clean(profileNameField.getText());
        String namingPattern = ScanProfile.normalizeExportNaming(exportNamingField.getText());

        if (profileName.isBlank()) {
            profileName = "Untitled Profile";
        }

        if (namingPattern.isBlank()) {
            namingPattern = ScanProfile.DEFAULT_EXPORT_NAMING;
        }

        String exportFolder = namingPattern
                .replace("{profileName}", profileName)
                .replace("{boxId}", PREVIEW_BOX_ID);

        exportNamingPreviewLabel.setText(exportFolder);

        previewProfileNameLabel.setText(profileName);
        previewBoxIdLabel.setText(PREVIEW_BOX_ID);
        previewExportFolderLabel.setText(exportFolder);

        String barcodeStatus = barcodeSplitToggle.isSelected()
                ? "Enabled - " + safeValue(barcodeDetectedComboBox) + " - " + shortBarcodePageBehavior(safeValue(barcodePageBehaviorComboBox))
                : "Disabled";

        previewBarcodeLabel.setText(barcodeStatus);

        previewPageCorrectionLabel.setText(
                "Rotation " + safeValue(defaultRotationComboBox)
                        + " - Brightness " + safeValue(brightnessComboBox)
                        + " - Contrast " + safeValue(contrastComboBox)
                        + " - Deskew " + (deskewToggle.isSelected() ? "Enabled" : "Disabled")
        );

        previewQaRequiredLabel.setText(qaRequiredToggle.isSelected() ? "Yes" : "No");
        previewExportFormatLabel.setText(safeValue(exportFormatComboBox));
    }

    private String shortBarcodePageBehavior(String value) {
        if (value.equalsIgnoreCase("Remove barcode page from final document")) {
            return "Remove barcode page";
        }

        if (value.equalsIgnoreCase("Keep barcode page in final document")) {
            return "Keep barcode page";
        }

        if (value.equalsIgnoreCase("Move barcode page to separate document")) {
            return "Move barcode page";
        }

        return value;
    }

    private String safeValue(ComboBox<String> comboBox) {
        return comboBox.getValue() == null ? "" : comboBox.getValue();
    }

    private void showOverviewPane() {
        overviewPane.setVisible(true);
        overviewPane.setManaged(true);

        editorPane.setVisible(false);
        editorPane.setManaged(false);

        overviewPane.toFront();
    }

    private void showEditorPane() {
        overviewPane.setVisible(false);
        overviewPane.setManaged(false);

        editorPane.setVisible(true);
        editorPane.setManaged(true);

        editorPane.toFront();
    }

    @FXML
    private void showOverview() {
        currentProfile = null;
        creatingProfile = false;
        showOverviewPane();
        Platform.runLater(() -> pageScrollPane.setVvalue(0));
    }

    @FXML
    private void createProfile() {
        if (adminManager == null) {
            return;
        }

        populateCreateProfileEditor();
        showEditorPane();
        selectTab(EditorTab.GENERAL);

        Platform.runLater(() -> {
            pageScrollPane.setVvalue(0);
            profileNameField.requestFocus();
        });
    }

    private void showCreateProfileDialog() {
        ButtonType createButtonType = new ButtonType("Create Profile", ButtonBar.ButtonData.OK_DONE);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Create Profile");
        dialog.setHeaderText(null);
        dialog.setResizable(false);
        dialog.getDialogPane().getButtonTypes().setAll(createButtonType, ButtonType.CANCEL);
        configureCreateProfileDialogShell(dialog);

        ProfileDialogFields fields = createProfileDialogFields();
        dialog.getDialogPane().setContent(buildCreateProfileDialogContent(fields));

        Node createButton = dialog.getDialogPane().lookupButton(createButtonType);

        if (createButton != null) {
            configureCreateProfileButton(createButton, fields);
        }

        dialog.setOnShown(event -> fields.nameField.requestFocus());
        dialog.showAndWait();
    }

    private void configureCreateProfileButton(Node createButton, ProfileDialogFields fields) {
        createButton.getStyleClass().add("create-user-button");
        createButton.setDisable(!isCreateProfileDialogReady(fields));
        createButton.addEventFilter(ActionEvent.ACTION, event ->
                submitCreateProfileDialog(event, fields)
        );

        fields.nameField.textProperty().addListener((observable, oldValue, newValue) ->
                updateCreateProfileButtonState(createButton, fields)
        );
        fields.clientField.textProperty().addListener((observable, oldValue, newValue) ->
                updateCreateProfileButtonState(createButton, fields)
        );
        fields.statusComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateCreateProfileButtonState(createButton, fields)
        );
    }

    private void updateCreateProfileButtonState(Node createButton, ProfileDialogFields fields) {
        createButton.setDisable(!isCreateProfileDialogReady(fields));

        if (isCreateProfileDialogReady(fields)) {
            setVisibleAndManaged(fields.validationLabel, false);
        }
    }

    private boolean isCreateProfileDialogReady(ProfileDialogFields fields) {
        return !Strings.clean(fields.nameField.getText()).isBlank()
                && !Strings.clean(fields.clientField.getText()).isBlank()
                && !Strings.clean(fields.statusComboBox.getValue()).isBlank();
    }

    private void configureCreateProfileDialogShell(Dialog<?> dialog) {
        dialog.getDialogPane().getStyleClass().addAll("app-shell", "admin-dialog-pane", "profile-create-dialog-pane");

        if (pageScrollPane.getScene() == null) {
            return;
        }

        dialog.initOwner(pageScrollPane.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().setAll(pageScrollPane.getScene().getStylesheets());

        if (pageScrollPane.getScene().getRoot().getStyleClass().contains("dark")) {
            dialog.getDialogPane().getStyleClass().add("dark");
        }
    }

    private ProfileDialogFields createProfileDialogFields() {
        ProfileDialogFields fields = new ProfileDialogFields();

        fields.nameField = createProfileDialogTextField("Profile name");
        fields.clientField = createProfileDialogTextField("Client");
        fields.descriptionArea = createProfileDialogTextArea("Description", 4);
        fields.statusComboBox = createProfileDialogComboBox("Draft", "Active", "Draft", "Archived");

        fields.barcodeSplitToggle = createProfileDialogToggle(false);
        fields.barcodeDetectedComboBox = createProfileDialogComboBox(
                "Start new document",
                "Start new document",
                "End current document",
                "Stop scanning and ask user",
                "Continue scanning and split automatically"
        );
        fields.barcodePageBehaviorComboBox = createProfileDialogComboBox(
                "Remove barcode page from final document",
                "Remove barcode page from final document",
                "Keep barcode page in final document",
                "Move barcode page to separate document"
        );

        fields.defaultRotationComboBox = createProfileDialogComboBox("0 deg", "0 deg", "90 deg", "180 deg", "270 deg");
        fields.brightnessComboBox = createProfileDialogComboBox("Normal", "Normal", "Lighter", "Darker");
        fields.contrastComboBox = createProfileDialogComboBox("Normal", "Normal", "Higher", "Lower");
        fields.deskewToggle = createProfileDialogToggle(true);
        fields.qaRequiredToggle = createProfileDialogToggle(true);

        fields.exportFormatComboBox = createProfileDialogComboBox("Multi-page TIFF", "Multi-page TIFF", "Single-page TIFF");
        fields.exportNamingField = createProfileDialogTextField(ScanProfile.DEFAULT_EXPORT_NAMING);
        fields.exportNamingField.setText(ScanProfile.DEFAULT_EXPORT_NAMING);

        fields.exportNamingPreviewLabel = createProfileDialogValueLabel("profile-editor-code-preview");
        fields.previewProfileNameLabel = createProfileDialogValueLabel("profile-editor-preview-value");
        fields.previewBoxIdLabel = createProfileDialogValueLabel("profile-editor-preview-value");
        fields.previewExportFolderLabel = createProfileDialogValueLabel("profile-info-code");
        fields.previewBarcodeLabel = createProfileDialogValueLabel("profile-editor-preview-value");
        fields.previewPageCorrectionLabel = createProfileDialogValueLabel("profile-editor-preview-value");
        fields.previewQaRequiredLabel = createProfileDialogValueLabel("profile-editor-preview-value");
        fields.previewExportFormatLabel = createProfileDialogValueLabel("profile-editor-preview-value");

        fields.generalTabButton = createProfileDialogTabButton("General");
        fields.scanRulesTabButton = createProfileDialogTabButton("Scan Rules");
        fields.accessTabButton = createProfileDialogTabButton("Access");
        fields.previewTabButton = createProfileDialogTabButton("Preview");

        fields.validationLabel = new Label();
        fields.validationLabel.getStyleClass().add("create-user-validation-message");
        setVisibleAndManaged(fields.validationLabel, false);

        configureCreateProfileDialogPreview(fields);
        return fields;
    }

    private Node buildCreateProfileDialogContent(ProfileDialogFields fields) {
        fields.generalTabPane = buildCreateGeneralTab(fields);
        fields.scanRulesTabPane = buildCreateScanRulesTab(fields);
        fields.accessTabPane = buildCreateAccessTab();
        fields.previewTabPane = buildCreatePreviewTab(fields);

        StackPane tabContent = new StackPane(
                fields.generalTabPane,
                fields.scanRulesTabPane,
                fields.accessTabPane,
                fields.previewTabPane
        );
        tabContent.getStyleClass().add("profile-editor-content");

        HBox tabs = new HBox(
                fields.generalTabButton,
                fields.scanRulesTabButton,
                fields.accessTabButton,
                fields.previewTabButton
        );
        tabs.getStyleClass().add("profile-editor-tabs");

        fields.generalTabButton.setOnAction(event -> selectCreateDialogTab(fields, EditorTab.GENERAL));
        fields.scanRulesTabButton.setOnAction(event -> selectCreateDialogTab(fields, EditorTab.SCAN_RULES));
        fields.accessTabButton.setOnAction(event -> selectCreateDialogTab(fields, EditorTab.ACCESS));
        fields.previewTabButton.setOnAction(event -> selectCreateDialogTab(fields, EditorTab.PREVIEW));

        VBox header = new VBox(3);
        header.getStyleClass().add("page-heading-copy");
        header.getChildren().addAll(
                createProfileDialogLabel("Create Profile", "page-title"),
                createProfileDialogLabel("Configure the profile before it appears in the profile list.", "page-subtitle")
        );

        VBox editor = new VBox(18, header, tabs, tabContent, fields.validationLabel);
        editor.getStyleClass().addAll("admin-page", "profile-editor-page", "profile-create-dialog-editor");
        editor.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scrollPane = new ScrollPane(editor);
        scrollPane.getStyleClass().add("profile-create-dialog-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(930);
        scrollPane.setPrefViewportHeight(610);

        selectCreateDialogTab(fields, EditorTab.GENERAL);
        return scrollPane;
    }

    private VBox buildCreateGeneralTab(ProfileDialogFields fields) {
        VBox card = new VBox(27);
        card.getStyleClass().add("profile-editor-card");
        card.getChildren().addAll(
                createProfileDialogField("Profile Name *", fields.nameField),
                createProfileDialogField("Client *", fields.clientField),
                createProfileDialogField("Description", fields.descriptionArea),
                createProfileDialogField("Status", fields.statusComboBox)
        );
        return card;
    }

    private VBox buildCreateScanRulesTab(ProfileDialogFields fields) {
        VBox barcodeSection = new VBox(27);
        barcodeSection.getStyleClass().add("profile-editor-section");

        HBox barcodeHeader = new HBox();
        barcodeHeader.setAlignment(Pos.CENTER_LEFT);
        Label barcodeTitle = createProfileDialogLabel("Barcode Splitting", "profile-editor-section-title");
        Region barcodeSpacer = new Region();
        HBox.setHgrow(barcodeSpacer, Priority.ALWAYS);
        barcodeHeader.getChildren().addAll(barcodeTitle, barcodeSpacer, fields.barcodeSplitToggle);

        GridPane barcodeGrid = new GridPane();
        barcodeGrid.setHgap(21);
        barcodeGrid.setVgap(6);
        barcodeGrid.setMaxWidth(Double.MAX_VALUE);
        barcodeGrid.getColumnConstraints().setAll(createPercentColumns(2));
        barcodeGrid.add(createProfileDialogLabel("When barcode is detected", "profile-editor-field-label"), 0, 0);
        barcodeGrid.add(createProfileDialogLabel("Barcode page behavior", "profile-editor-field-label"), 1, 0);
        barcodeGrid.add(fields.barcodeDetectedComboBox, 0, 1);
        barcodeGrid.add(fields.barcodePageBehaviorComboBox, 1, 1);

        barcodeSection.getChildren().addAll(
                barcodeHeader,
                createProfileDialogLabel("Enable barcode document splitting", "profile-editor-strong-text"),
                barcodeGrid
        );

        VBox correctionSection = new VBox(27);
        correctionSection.getStyleClass().add("profile-editor-section");

        GridPane correctionGrid = new GridPane();
        correctionGrid.setHgap(21);
        correctionGrid.setVgap(6);
        correctionGrid.setMaxWidth(Double.MAX_VALUE);
        correctionGrid.getColumnConstraints().setAll(createPercentColumns(3));
        correctionGrid.add(createProfileDialogLabel("Default rotation", "profile-editor-field-label"), 0, 0);
        correctionGrid.add(createProfileDialogLabel("Brightness", "profile-editor-field-label"), 1, 0);
        correctionGrid.add(createProfileDialogLabel("Contrast", "profile-editor-field-label"), 2, 0);
        correctionGrid.add(fields.defaultRotationComboBox, 0, 1);
        correctionGrid.add(fields.brightnessComboBox, 1, 1);
        correctionGrid.add(fields.contrastComboBox, 2, 1);

        HBox deskewRow = new HBox();
        deskewRow.setAlignment(Pos.CENTER_LEFT);
        Region deskewSpacer = new Region();
        HBox.setHgrow(deskewSpacer, Priority.ALWAYS);
        deskewRow.getChildren().addAll(
                createProfileDialogLabel("Deskew / straighten pages", "profile-editor-strong-text"),
                deskewSpacer,
                fields.deskewToggle
        );

        HBox qaRequiredRow = new HBox();
        qaRequiredRow.setAlignment(Pos.CENTER_LEFT);
        Region qaRequiredSpacer = new Region();
        HBox.setHgrow(qaRequiredSpacer, Priority.ALWAYS);
        qaRequiredRow.getChildren().addAll(
                createProfileDialogLabel("Require QA for scanned boxes", "profile-editor-strong-text"),
                qaRequiredSpacer,
                fields.qaRequiredToggle
        );

        correctionSection.getChildren().addAll(
                createProfileDialogLabel("Page Correction Defaults", "profile-editor-section-title"),
                correctionGrid,
                deskewRow,
                qaRequiredRow
        );

        VBox exportSection = new VBox(24);
        exportSection.getStyleClass().add("profile-editor-section");

        VBox formatField = createProfileDialogField("Export format", fields.exportFormatComboBox);
        formatField.setMaxWidth(420);

        HBox namingRow = new HBox(21);
        namingRow.setAlignment(Pos.CENTER_LEFT);
        namingRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fields.exportNamingField, Priority.ALWAYS);

        VBox previewBox = new VBox(6);
        previewBox.setMinWidth(276);
        previewBox.setPrefWidth(276);
        previewBox.setMaxWidth(276);
        previewBox.getStyleClass().add("profile-editor-preview-box");
        previewBox.getChildren().addAll(
                createProfileDialogLabel("Preview:", "profile-editor-helper-text"),
                fields.exportNamingPreviewLabel
        );

        namingRow.getChildren().addAll(fields.exportNamingField, previewBox);
        exportSection.getChildren().addAll(
                createProfileDialogLabel("Export Settings", "profile-editor-section-title"),
                formatField,
                createProfileDialogField("Export folder naming", namingRow)
        );

        VBox sections = new VBox(0);
        sections.getStyleClass().addAll("profile-editor-card", "profile-editor-sectioned-card");
        sections.getChildren().addAll(
                barcodeSection,
                createDivider(),
                correctionSection,
                createDivider(),
                exportSection
        );

        return sections;
    }

    private VBox buildCreateAccessTab() {
        VBox card = new VBox(24);
        card.getStyleClass().add("profile-editor-card");

        VBox header = new VBox(6);
        header.getChildren().addAll(
                createProfileDialogLabel("Profile Access", "profile-editor-section-title"),
                createProfileDialogLabel("0 users assigned", "profile-editor-subtitle")
        );

        VBox list = new VBox(0);
        list.getStyleClass().add("profile-editor-access-list");

        Label emptyLabel = createProfileDialogLabel("Create the profile before assigning users.", "profile-editor-helper-text");
        VBox.setMargin(emptyLabel, new Insets(21));
        list.getChildren().add(emptyLabel);

        card.getChildren().addAll(header, list);
        return card;
    }

    private VBox buildCreatePreviewTab(ProfileDialogFields fields) {
        VBox previewBox = new VBox(18);
        previewBox.getStyleClass().add("profile-editor-scanner-preview-box");
        previewBox.getChildren().addAll(
                createPreviewBlock("Selected Profile:", fields.previewProfileNameLabel),
                createPreviewBlock("Box ID:", fields.previewBoxIdLabel),
                createPreviewBlock("Export folder:", fields.previewExportFolderLabel),
                createPreviewBlock("Barcode splitting:", fields.previewBarcodeLabel),
                createPreviewBlock("Page correction:", fields.previewPageCorrectionLabel),
                createPreviewBlock("QA required:", fields.previewQaRequiredLabel),
                createPreviewBlock("Export format:", fields.previewExportFormatLabel)
        );

        VBox card = new VBox(24);
        card.getStyleClass().add("profile-editor-card");
        card.getChildren().addAll(
                createProfileDialogLabel("Scanner Preview", "profile-editor-section-title"),
                previewBox
        );
        return card;
    }

    private VBox createPreviewBlock(String labelText, Label valueLabel) {
        VBox block = new VBox(6);
        block.getChildren().addAll(
                createProfileDialogLabel(labelText, "profile-editor-preview-key"),
                valueLabel
        );
        return block;
    }

    private VBox createProfileDialogField(String labelText, Node control) {
        VBox field = new VBox(6);
        field.setMaxWidth(Double.MAX_VALUE);
        field.getChildren().addAll(createProfileDialogLabel(labelText, "profile-editor-field-label"), control);
        return field;
    }

    private TextField createProfileDialogTextField(String promptText) {
        TextField textField = new TextField();
        textField.setPromptText(promptText);
        textField.setMaxWidth(Double.MAX_VALUE);
        textField.getStyleClass().add("profile-editor-input");
        return textField;
    }

    private TextArea createProfileDialogTextArea(String promptText, int prefRowCount) {
        TextArea textArea = new TextArea();
        textArea.setPromptText(promptText);
        textArea.setPrefRowCount(prefRowCount);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.getStyleClass().add("profile-editor-textarea");
        return textArea;
    }

    private ComboBox<String> createProfileDialogComboBox(String defaultValue, String... values) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(values);
        comboBox.setValue(defaultValue);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.getStyleClass().add("profile-editor-input");
        return comboBox;
    }

    private ToggleButton createProfileDialogToggle(boolean selected) {
        ToggleButton toggle = new ToggleButton();
        toggle.setSelected(selected);
        toggle.setFocusTraversable(false);
        toggle.getStyleClass().add("profile-editor-switch");
        return toggle;
    }

    private Button createProfileDialogTabButton(String text) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        button.getStyleClass().add("profile-editor-tab-button");
        return button;
    }

    private Label createProfileDialogValueLabel(String styleClass) {
        Label label = new Label();
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private Label createProfileDialogLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private List<ColumnConstraints> createPercentColumns(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> {
                    ColumnConstraints constraints = new ColumnConstraints();
                    constraints.setPercentWidth(100.0 / count);
                    constraints.setHgrow(Priority.ALWAYS);
                    return constraints;
                })
                .toList();
    }

    private void configureCreateProfileDialogPreview(ProfileDialogFields fields) {
        fields.nameField.textProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        fields.exportNamingField.textProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        fields.barcodeSplitToggle.selectedProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        fields.deskewToggle.selectedProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        fields.qaRequiredToggle.selectedProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        fields.barcodeDetectedComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        fields.barcodePageBehaviorComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        fields.defaultRotationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        fields.brightnessComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        fields.contrastComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        fields.exportFormatComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncCreateProfilePreview(fields));
        syncCreateProfilePreview(fields);
    }

    private void syncCreateProfilePreview(ProfileDialogFields fields) {
        String profileName = Strings.displayText(fields.nameField.getText(), "Untitled Profile");
        String namingPattern = ScanProfile.normalizeExportNaming(fields.exportNamingField.getText());
        String exportFolder = namingPattern
                .replace("{profileName}", profileName)
                .replace("{boxId}", PREVIEW_BOX_ID);

        fields.exportNamingPreviewLabel.setText(exportFolder);
        fields.previewProfileNameLabel.setText(profileName);
        fields.previewBoxIdLabel.setText(PREVIEW_BOX_ID);
        fields.previewExportFolderLabel.setText(exportFolder);
        fields.previewBarcodeLabel.setText(fields.barcodeSplitToggle.isSelected()
                ? "Enabled - " + safeValue(fields.barcodeDetectedComboBox) + " - " + shortBarcodePageBehavior(safeValue(fields.barcodePageBehaviorComboBox))
                : "Disabled");
        fields.previewPageCorrectionLabel.setText(
                "Rotation " + safeValue(fields.defaultRotationComboBox)
                        + " - Brightness " + safeValue(fields.brightnessComboBox)
                        + " - Contrast " + safeValue(fields.contrastComboBox)
                        + " - Deskew " + (fields.deskewToggle.isSelected() ? "Enabled" : "Disabled")
        );
        fields.previewQaRequiredLabel.setText(fields.qaRequiredToggle.isSelected() ? "Yes" : "No");
        fields.previewExportFormatLabel.setText(safeValue(fields.exportFormatComboBox));
    }

    private void selectCreateDialogTab(ProfileDialogFields fields, EditorTab tab) {
        syncCreateProfilePreview(fields);

        setTabButtonActive(fields.generalTabButton, tab == EditorTab.GENERAL);
        setTabButtonActive(fields.scanRulesTabButton, tab == EditorTab.SCAN_RULES);
        setTabButtonActive(fields.accessTabButton, tab == EditorTab.ACCESS);
        setTabButtonActive(fields.previewTabButton, tab == EditorTab.PREVIEW);

        setPaneVisible(fields.generalTabPane, tab == EditorTab.GENERAL);
        setPaneVisible(fields.scanRulesTabPane, tab == EditorTab.SCAN_RULES);
        setPaneVisible(fields.accessTabPane, tab == EditorTab.ACCESS);
        setPaneVisible(fields.previewTabPane, tab == EditorTab.PREVIEW);
    }

    private void submitCreateProfileDialog(ActionEvent event, ProfileDialogFields fields) {
        String validationMessage = validateCreateProfileDialog(fields);

        if (!validationMessage.isBlank()) {
            showCreateProfileValidation(fields.validationLabel, validationMessage);
            event.consume();
            return;
        }

        try {
            adminManager.createProfile(createProfileInputFromDialog(fields));

            refreshAfterProfileCreated();
        } catch (RuntimeException exception) {
            showCreateProfileValidation(fields.validationLabel, createProfileErrorMessage(exception));
            event.consume();
        }
    }

    private String validateCreateProfileDialog(ProfileDialogFields fields) {
        if (Strings.clean(fields.nameField.getText()).isBlank()) {
            return "Profile name is required.";
        }

        if (Strings.clean(fields.clientField.getText()).isBlank()) {
            return "Client is required.";
        }

        if (Strings.clean(fields.statusComboBox.getValue()).isBlank()) {
            return "Status is required.";
        }

        return "";
    }

    private void showCreateProfileValidation(Label validationLabel, String message) {
        validationLabel.setText(message);
        setVisibleAndManaged(validationLabel, true);
    }

    private String createProfileErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "Profile could not be created.";
        }

        return message;
    }

    private void refreshAfterProfileCreated() {
        searchField.clear();
        statusFilterComboBox.setValue(ALL_STATUSES);

        loadProfiles();
        applyFilters();
        currentProfile = null;
        showOverviewPane();

        Platform.runLater(() -> pageScrollPane.setVvalue(1));
    }

    private AdminManager.ProfileInput createProfileInputFromDialog(ProfileDialogFields fields) {
        String profileName = Strings.clean(fields.nameField.getText());

        return new AdminManager.ProfileInput(
                profileName,
                Strings.clean(fields.clientField.getText()),
                createProfileCode(profileName),
                Strings.clean(fields.descriptionArea.getText()),
                safeValue(fields.statusComboBox),
                "",
                ScanProfile.normalizeExportNaming(fields.exportNamingField.getText()),
                fields.barcodeSplitToggle.isSelected(),
                safeValue(fields.barcodeDetectedComboBox),
                safeValue(fields.barcodePageBehaviorComboBox),
                safeValue(fields.defaultRotationComboBox),
                safeValue(fields.brightnessComboBox),
                safeValue(fields.contrastComboBox),
                fields.deskewToggle.isSelected(),
                safeValue(fields.exportFormatComboBox),
                fields.qaRequiredToggle.isSelected()
        );
    }

    private void setVisibleAndManaged(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    @FXML
    private void saveChanges() {
        if (adminManager == null || (!creatingProfile && currentProfile == null)) {
            return;
        }

        try {
            ScanProfile savedProfile = creatingProfile
                    ? adminManager.createProfile(createProfileInputFromEditor())
                    : adminManager.updateProfile(
                            currentProfile.getId(),
                            createProfileInputFromEditor()
                    );

            loadProfiles();
            applyFilters();
            openProfile(savedProfile);
        } catch (IllegalArgumentException exception) {
            editorSubtitleLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void deleteCurrentProfile() {
        if (creatingProfile) {
            return;
        }

        deleteProfile(currentProfile);
    }

    private void deleteProfile(ScanProfile profile) {
        if (profile == null || adminManager == null) {
            return;
        }

        try {
            adminManager.deleteProfile(profile.getId());

            if (currentProfile != null && currentProfile.getId() == profile.getId()) {
                currentProfile = null;
            }

            loadProfiles();
            applyFilters();
            showOverviewPane();
        } catch (RuntimeException exception) {
            currentProfile = profile;
            populateEditor(profile);
            showEditorPane();
            editorSubtitleLabel.setText(deleteProfileErrorMessage(exception));
        }
    }

    private String deleteProfileErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "Profile could not be deleted.";
        }

        return message;
    }

    private AdminManager.ProfileInput createProfileInputFromEditor() {
        String profileName = Strings.clean(profileNameField.getText());
        String profileCode = currentProfile == null || Strings.clean(currentProfile.getCode()).isBlank()
                ? createProfileCode(profileName)
                : currentProfile.getCode();

        return new AdminManager.ProfileInput(
                profileName,
                Strings.clean(profileClientField.getText()),
                profileCode,
                Strings.clean(profileDescriptionArea.getText()),
                safeValue(profileStatusComboBox),
                "",
                ScanProfile.normalizeExportNaming(exportNamingField.getText()),
                barcodeSplitToggle.isSelected(),
                safeValue(barcodeDetectedComboBox),
                safeValue(barcodePageBehaviorComboBox),
                safeValue(defaultRotationComboBox),
                safeValue(brightnessComboBox),
                safeValue(contrastComboBox),
                deskewToggle.isSelected(),
                safeValue(exportFormatComboBox),
                qaRequiredToggle.isSelected()
        );
    }

    @FXML
    private void previewExport() {
        selectTab(EditorTab.PREVIEW);
        Platform.runLater(() -> pageScrollPane.setVvalue(0));
    }

    @FXML
    private void manageAccess() {
        if (creatingProfile || currentProfile == null) {
            return;
        }

        navigator.showAssignments();
    }

    @FXML
    private void showGeneralTab() {
        selectTab(EditorTab.GENERAL);
    }

    @FXML
    private void showScanRulesTab() {
        selectTab(EditorTab.SCAN_RULES);
    }

    @FXML
    private void showAccessTab() {
        if (creatingProfile) {
            selectTab(EditorTab.GENERAL);
            return;
        }

        selectTab(EditorTab.ACCESS);
    }

    @FXML
    private void showPreviewTab() {
        selectTab(EditorTab.PREVIEW);
    }

    private void selectTab(EditorTab tab) {
        if (creatingProfile && tab == EditorTab.ACCESS) {
            tab = EditorTab.GENERAL;
        }

        syncPreview();

        setTabButtonActive(generalTabButton, tab == EditorTab.GENERAL);
        setTabButtonActive(scanRulesTabButton, tab == EditorTab.SCAN_RULES);
        setTabButtonActive(accessTabButton, tab == EditorTab.ACCESS);
        setTabButtonActive(previewTabButton, tab == EditorTab.PREVIEW);

        setPaneVisible(generalTabPane, tab == EditorTab.GENERAL);
        setPaneVisible(scanRulesTabPane, tab == EditorTab.SCAN_RULES);
        setPaneVisible(accessTabPane, tab == EditorTab.ACCESS);
        setPaneVisible(previewTabPane, tab == EditorTab.PREVIEW);
    }

    private void setTabButtonActive(Button button, boolean active) {
        button.getStyleClass().setAll("profile-editor-tab-button");

        if (active) {
            button.getStyleClass().add("profile-editor-tab-active");
        }
    }

    private void setPaneVisible(Node pane, boolean visible) {
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    private void toggleArchive(ScanProfile profile) {
        if (profile.isArchived()) {
            adminManager.restoreProfile(profile.getId());
        } else {
            adminManager.archiveProfile(profile.getId());
        }

        loadProfiles();
        applyFilters();

        if (currentProfile != null && currentProfile.getId() == profile.getId()) {
            findProfileById(profile.getId()).ifPresent(updatedProfile -> {
                currentProfile = updatedProfile;
                populateEditor(updatedProfile);
            });
        }
    }

    private List<ConfigChip> configChipsFor(ScanProfile profile) {
        List<ConfigChip> chips = new ArrayList<>();

        chips.add(profile.isBarcodeSplitting()
                ? new ConfigChip("Barcode Split On", "chip-teal")
                : new ConfigChip("Barcode Split Off", "chip-neutral"));

        chips.add(profile.isDeskew()
                ? new ConfigChip("Deskew", "chip-indigo")
                : new ConfigChip("Deskew Off", "chip-neutral"));

        if (!profile.getBrightness().equalsIgnoreCase("Normal")) {
            chips.add(new ConfigChip("Brightness Correction", "chip-orange"));
        }

        return chips;
    }

    private int assignedUserCountFor(ScanProfile profile) {
        return adminManager.getAssignedUserIds(profile.getId()).size();
    }

    private List<User> usersAssignedTo(ScanProfile profile) {
        Set<Integer> assignedUserIds = adminManager.getAssignedUserIds(profile.getId());

        return adminManager.getUsers().stream()
                .filter(user -> assignedUserIds.contains(user.getId()))
                .toList();
    }

    private java.util.Optional<ScanProfile> findProfileById(int profileId) {
        return masterProfiles.stream()
                .filter(profile -> profile.getId() == profileId)
                .findFirst();
    }

    private String createUniqueProfileName() {
        String baseName = "New Profile";
        String name = baseName;
        int number = 2;

        while (profileNameExists(name)) {
            name = baseName + " " + number;
            number++;
        }

        return name;
    }

    private boolean profileNameExists(String name) {
        String normalizedName = Strings.normalize(name);

        return adminManager.getProfiles().stream()
                .map(ScanProfile::getName)
                .map(Strings::normalize)
                .anyMatch(existingName -> existingName.equals(normalizedName));
    }

    private String createProfileCode(String name) {
        String baseCode = Strings.clean(name).replaceAll("[^A-Za-z0-9]", "");

        if (baseCode.isBlank()) {
            baseCode = "NewProfile";
        }

        String code = baseCode;
        int number = 2;

        while (adminManager.profileCodeExists(code, null)) {
            code = baseCode + number;
            number++;
        }

        return code;
    }

    private String displayStatus(ScanProfile profile) {
        return profile.isArchived() ? "Archived" : profile.getStatus();
    }

    private String statusClassFor(String status) {
        return switch (Strings.normalize(status)) {
            case "active" -> "profile-status-active";
            case "draft" -> "profile-status-draft";
            case "archived" -> "profile-status-archived";
            default -> "profile-status-default";
        };
    }

    private String formatAssignedUsers(int assignedUsersCount) {
        return assignedUsersCount + (assignedUsersCount == 1 ? " user assigned" : " users assigned");
    }

    private enum EditorTab {
        GENERAL,
        SCAN_RULES,
        ACCESS,
        PREVIEW
    }

    private static class ProfileDialogFields {
        private TextField nameField;
        private TextField clientField;
        private TextArea descriptionArea;
        private ComboBox<String> statusComboBox;

        private ToggleButton barcodeSplitToggle;
        private ComboBox<String> barcodeDetectedComboBox;
        private ComboBox<String> barcodePageBehaviorComboBox;

        private ComboBox<String> defaultRotationComboBox;
        private ComboBox<String> brightnessComboBox;
        private ComboBox<String> contrastComboBox;
        private ToggleButton deskewToggle;
        private ToggleButton qaRequiredToggle;

        private ComboBox<String> exportFormatComboBox;
        private TextField exportNamingField;
        private Label exportNamingPreviewLabel;

        private Label previewProfileNameLabel;
        private Label previewBoxIdLabel;
        private Label previewExportFolderLabel;
        private Label previewBarcodeLabel;
        private Label previewPageCorrectionLabel;
        private Label previewQaRequiredLabel;
        private Label previewExportFormatLabel;

        private Button generalTabButton;
        private Button scanRulesTabButton;
        private Button accessTabButton;
        private Button previewTabButton;

        private VBox generalTabPane;
        private VBox scanRulesTabPane;
        private VBox accessTabPane;
        private VBox previewTabPane;

        private Label validationLabel;
    }

    private record ConfigChip(String label, String styleClass) {
    }
}
