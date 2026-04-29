package easv.gui.controller.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public class ProfilesController {

    private static final String ALL_STATUSES = "All Statuses";
    private static final String ALL_METADATA_TEMPLATES = "All Metadata Templates";

    private static final double PAGE_HORIZONTAL_PADDING = 48;
    private static final double CARD_GAP = 18;
    private static final double MIN_CARD_WIDTH = 330;

    private static final String PREVIEW_BOX_ID = "BOX-2026-004";

    @FXML private ScrollPane pageScrollPane;

    @FXML private VBox overviewPane;
    @FXML private VBox editorPane;

    @FXML private Label profilesCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> metadataFilterComboBox;
    @FXML private GridPane profilesCardsGrid;
    @FXML private VBox emptyStateBox;

    @FXML private Label editorTitleLabel;
    @FXML private Label editorSubtitleLabel;
    @FXML private Label editorStatusBadge;

    @FXML private Button generalTabButton;
    @FXML private Button scanRulesTabButton;
    @FXML private Button metadataTabButton;
    @FXML private Button accessTabButton;
    @FXML private Button previewTabButton;

    @FXML private VBox generalTabPane;
    @FXML private VBox scanRulesTabPane;
    @FXML private VBox metadataTabPane;
    @FXML private VBox accessTabPane;
    @FXML private VBox previewTabPane;

    @FXML private TextField profileNameField;
    @FXML private TextField profileCodeField;
    @FXML private TextArea profileDescriptionArea;
    @FXML private ComboBox<String> profileStatusComboBox;

    @FXML private ToggleButton barcodeSplitToggle;
    @FXML private ComboBox<String> barcodeDetectedComboBox;
    @FXML private ComboBox<String> barcodePageBehaviorComboBox;

    @FXML private ComboBox<String> defaultRotationComboBox;
    @FXML private ComboBox<String> brightnessComboBox;
    @FXML private ComboBox<String> contrastComboBox;
    @FXML private ToggleButton deskewToggle;

    @FXML private ComboBox<String> exportFormatComboBox;
    @FXML private TextField exportNamingField;
    @FXML private Label exportNamingPreviewLabel;

    @FXML private ComboBox<String> metadataTemplateComboBox;
    @FXML private ToggleButton metadataRequiredToggle;

    @FXML private Label accessCountLabel;
    @FXML private VBox accessRowsContainer;

    @FXML private Label previewProfileNameLabel;
    @FXML private Label previewBoxIdLabel;
    @FXML private Label previewExportFolderLabel;
    @FXML private Label previewBarcodeLabel;
    @FXML private Label previewMetadataTemplateLabel;
    @FXML private Label previewPageCorrectionLabel;
    @FXML private Label previewExportFormatLabel;

    private final ObservableList<ProfileCardModel> masterProfiles = FXCollections.observableArrayList();
    private FilteredList<ProfileCardModel> filteredProfiles;

    private ProfileCardModel currentProfile;
    private AdminNavigator navigator = AdminNavigator.none();

    void setNavigator(AdminNavigator navigator) {
        this.navigator = navigator == null ? AdminNavigator.none() : navigator;
    }

    @FXML
    private void initialize() {
        configureOverviewFilters();
        configureEditorControls();
        loadSampleProfiles();
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

        metadataFilterComboBox.getItems().setAll(
                ALL_METADATA_TEMPLATES,
                "Building Archive Metadata",
                "Technical Drawings Metadata",
                "Court Records Metadata"
        );
        metadataFilterComboBox.setValue(ALL_METADATA_TEMPLATES);
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

        defaultRotationComboBox.getItems().setAll("0°", "90°", "180°", "270°");
        brightnessComboBox.getItems().setAll("Normal", "Lighter", "Darker");
        contrastComboBox.getItems().setAll("Normal", "Higher", "Lower");

        exportFormatComboBox.getItems().setAll(
                "Multi-page TIFF",
                "PDF",
                "PDF/A",
                "Single-page TIFF"
        );

        metadataTemplateComboBox.getItems().setAll(
                "Building Archive Metadata",
                "Technical Drawings Metadata",
                "Court Records Metadata"
        );

        profileNameField.textProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        profileCodeField.textProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        exportNamingField.textProperty().addListener((observable, oldValue, newValue) -> syncPreview());

        barcodeSplitToggle.selectedProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        deskewToggle.selectedProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        metadataRequiredToggle.selectedProperty().addListener((observable, oldValue, newValue) -> syncPreview());

        barcodeDetectedComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        barcodePageBehaviorComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        defaultRotationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        brightnessComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        contrastComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        exportFormatComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
        metadataTemplateComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncPreview());
    }

    private void configureFiltering() {
        filteredProfiles = new FilteredList<>(masterProfiles, profile -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        metadataFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void configureResponsiveGrid() {
        pageScrollPane.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) -> layoutProfileGrid());
        profilesCardsGrid.widthProperty().addListener((observable, oldWidth, newWidth) -> layoutProfileGrid());

        Platform.runLater(this::layoutProfileGrid);
    }

    private void applyFilters() {
        String searchText = normalize(searchField.getText());
        String selectedStatus = statusFilterComboBox.getValue();
        String selectedMetadata = metadataFilterComboBox.getValue();

        filteredProfiles.setPredicate(profile ->
                matchesSearch(profile, searchText)
                        && matchesStatus(profile, selectedStatus)
                        && matchesMetadata(profile, selectedMetadata)
        );

        renderProfileCards();
    }

    private boolean matchesSearch(ProfileCardModel profile, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return normalize(profile.name).contains(searchText)
                || normalize(profile.code).contains(searchText)
                || normalize(profile.description).contains(searchText)
                || normalize(profile.metadataTemplate).contains(searchText)
                || normalize(profile.exportNaming).contains(searchText)
                || normalize(profile.displayStatus()).contains(searchText)
                || profile.configChips.stream().anyMatch(chip -> normalize(chip.label()).contains(searchText));
    }

    private boolean matchesStatus(ProfileCardModel profile, String selectedStatus) {
        return selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || profile.displayStatus().equalsIgnoreCase(selectedStatus);
    }

    private boolean matchesMetadata(ProfileCardModel profile, String selectedMetadata) {
        return selectedMetadata == null
                || ALL_METADATA_TEMPLATES.equals(selectedMetadata)
                || profile.metadataTemplate.equalsIgnoreCase(selectedMetadata);
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

    private VBox buildProfileCard(ProfileCardModel profile) {
        VBox card = new VBox(14);
        card.getStyleClass().add("profile-card");

        if (profile.archived) {
            card.getStyleClass().add("profile-card-archived");
        }

        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);

        HBox headerRow = buildHeaderRow(profile);
        FlowPane chipsPane = buildChipsPane(profile);
        Region topDivider = createDivider();
        VBox infoBox = buildInfoBox(profile);

        Region footerSpacer = new Region();
        footerSpacer.getStyleClass().add("profile-card-footer-spacer");
        VBox.setVgrow(footerSpacer, Priority.ALWAYS);

        Region bottomDivider = createDivider();
        HBox footerRow = buildFooterRow(profile);

        card.getChildren().addAll(
                headerRow,
                chipsPane,
                topDivider,
                infoBox,
                footerSpacer,
                bottomDivider,
                footerRow
        );

        return card;
    }

    private HBox buildHeaderRow(ProfileCardModel profile) {
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.TOP_LEFT);

        VBox titleBox = new VBox(4);

        Label nameLabel = new Label(profile.name);
        nameLabel.getStyleClass().add("profile-card-title");

        Label descriptionLabel = new Label(profile.description);
        descriptionLabel.getStyleClass().add("profile-card-description");
        descriptionLabel.setWrapText(true);

        titleBox.getChildren().addAll(nameLabel, descriptionLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(profile.displayStatus());
        statusBadge.getStyleClass().addAll("profile-status-badge", statusClassFor(profile.displayStatus()));

        headerRow.getChildren().addAll(titleBox, spacer, statusBadge);
        return headerRow;
    }

    private FlowPane buildChipsPane(ProfileCardModel profile) {
        FlowPane chipsPane = new FlowPane();
        chipsPane.getStyleClass().add("profile-chip-group");
        chipsPane.setHgap(8);
        chipsPane.setVgap(8);

        for (ConfigChip chip : profile.configChips) {
            Label chipLabel = new Label(chip.label());
            chipLabel.getStyleClass().addAll("profile-config-chip", chip.styleClass());
            chipsPane.getChildren().add(chipLabel);
        }

        return chipsPane;
    }

    private VBox buildInfoBox(ProfileCardModel profile) {
        VBox infoBox = new VBox(10);
        infoBox.getChildren().addAll(
                buildInfoBlock("Metadata Template", profile.metadataTemplate, false),
                buildInfoBlock("Export Naming", profile.exportNaming, true),
                buildInfoBlock("Assigned Users", formatAssignedUsers(profile.assignedUsersCount), false),
                buildInfoBlock("Updated", profile.lastUpdated, false)
        );

        return infoBox;
    }

    private HBox buildFooterRow(ProfileCardModel profile) {
        HBox footerRow = new HBox(10);
        footerRow.getStyleClass().add("profile-card-footer");
        footerRow.setAlignment(Pos.CENTER_LEFT);

        Button openButton = createActionButton("Open", "profile-open-button");
        openButton.setOnAction(event -> openProfile(profile));

        Button archiveButton = createActionButton(profile.archived ? "Restore" : "Archive", "profile-secondary-button");
        archiveButton.setOnAction(event -> toggleArchive(profile));

        footerRow.getChildren().addAll(openButton, archiveButton);
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

    private void openProfile(ProfileCardModel profile) {
        currentProfile = profile;

        populateEditor(profile);
        showEditorPane();
        selectTab(EditorTab.GENERAL);

        Platform.runLater(() -> pageScrollPane.setVvalue(0));
    }

    private void populateEditor(ProfileCardModel profile) {
        editorTitleLabel.setText(profile.name + " Profile");
        editorSubtitleLabel.setText(profile.description);

        editorStatusBadge.setText(profile.displayStatus());
        editorStatusBadge.getStyleClass().setAll("profile-status-badge", statusClassFor(profile.displayStatus()));

        profileNameField.setText(profile.name);
        profileCodeField.setText(profile.code);
        profileDescriptionArea.setText(profile.description);
        profileStatusComboBox.setValue(profile.displayStatus());

        barcodeSplitToggle.setSelected(profile.barcodeSplitting);
        barcodeDetectedComboBox.setValue(profile.barcodeDetectedBehavior);
        barcodePageBehaviorComboBox.setValue(profile.barcodePageBehavior);

        defaultRotationComboBox.setValue(profile.defaultRotation);
        brightnessComboBox.setValue(profile.brightness);
        contrastComboBox.setValue(profile.contrast);
        deskewToggle.setSelected(profile.deskew);

        exportFormatComboBox.setValue(profile.exportFormat);
        exportNamingField.setText(profile.exportNaming);

        metadataTemplateComboBox.setValue(profile.metadataTemplate);
        metadataRequiredToggle.setSelected(profile.metadataRequiredBeforeExport);

        populateAccessRows(profile);
        syncPreview();
    }

    private void populateAccessRows(ProfileCardModel profile) {
        accessCountLabel.setText(formatAssignedUsers(profile.assignedUsersCount));

        List<AccessUser> users = AdminDemoData.profileAccessUsers();

        accessRowsContainer.getChildren().clear();

        for (int index = 0; index < users.size(); index++) {
            AccessUser user = users.get(index);

            HBox row = new HBox(9);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("profile-editor-access-row");

            if (index == users.size() - 1) {
                row.getStyleClass().add("profile-editor-access-row-last");
            }

            Label nameLabel = new Label(user.name());
            nameLabel.getStyleClass().add("profile-editor-access-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label roleBadge = new Label(user.role());
            roleBadge.getStyleClass().addAll(
                    "profile-editor-small-badge",
                    user.role().equalsIgnoreCase("Admin")
                            ? "profile-editor-admin-badge"
                            : "profile-editor-user-badge"
            );

            Label statusBadge = new Label(user.status());
            statusBadge.getStyleClass().addAll("profile-editor-small-badge", "profile-editor-active-badge");

            row.getChildren().addAll(nameLabel, spacer, roleBadge, statusBadge);
            accessRowsContainer.getChildren().add(row);
        }
    }

    private void syncPreview() {
        String profileName = safe(profileNameField.getText());
        String profileCode = safe(profileCodeField.getText());
        String namingPattern = safe(exportNamingField.getText());

        if (profileName.isBlank()) {
            profileName = "Untitled Profile";
        }

        if (profileCode.isBlank()) {
            profileCode = "ProfileCode";
        }

        if (namingPattern.isBlank()) {
            namingPattern = "{profileCode}_{boxId}";
        }

        String exportFolder = namingPattern
                .replace("{profileCode}", profileCode)
                .replace("{boxId}", PREVIEW_BOX_ID);

        exportNamingPreviewLabel.setText(exportFolder);

        previewProfileNameLabel.setText(profileName);
        previewBoxIdLabel.setText(PREVIEW_BOX_ID);
        previewExportFolderLabel.setText(exportFolder);

        String barcodeStatus = barcodeSplitToggle.isSelected()
                ? "Enabled · " + safeValue(barcodeDetectedComboBox) + " · " + shortBarcodePageBehavior(safeValue(barcodePageBehaviorComboBox))
                : "Disabled";

        previewBarcodeLabel.setText(barcodeStatus);

        previewMetadataTemplateLabel.setText(safeValue(metadataTemplateComboBox));

        previewPageCorrectionLabel.setText(
                "Rotation " + safeValue(defaultRotationComboBox)
                        + " · Brightness " + safeValue(brightnessComboBox)
                        + " · Contrast " + safeValue(contrastComboBox)
                        + " · Deskew " + (deskewToggle.isSelected() ? "Enabled" : "Disabled")
        );

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
        showOverviewPane();
        Platform.runLater(() -> pageScrollPane.setVvalue(0));
    }

    @FXML
    private void createProfile() {
        ProfileCardModel newProfile = AdminDemoData.newProfile();

        masterProfiles.add(0, newProfile);

        searchField.clear();
        statusFilterComboBox.setValue(ALL_STATUSES);
        metadataFilterComboBox.setValue(ALL_METADATA_TEMPLATES);

        applyFilters();
        openProfile(newProfile);
    }

    @FXML
    private void saveChanges() {
        if (currentProfile == null) {
            return;
        }

        currentProfile.name = safe(profileNameField.getText());
        currentProfile.code = safe(profileCodeField.getText());
        currentProfile.description = safe(profileDescriptionArea.getText());
        currentProfile.status = safeValue(profileStatusComboBox);
        currentProfile.archived = currentProfile.status.equalsIgnoreCase("Archived");

        currentProfile.barcodeSplitting = barcodeSplitToggle.isSelected();
        currentProfile.barcodeDetectedBehavior = safeValue(barcodeDetectedComboBox);
        currentProfile.barcodePageBehavior = safeValue(barcodePageBehaviorComboBox);

        currentProfile.defaultRotation = safeValue(defaultRotationComboBox);
        currentProfile.brightness = safeValue(brightnessComboBox);
        currentProfile.contrast = safeValue(contrastComboBox);
        currentProfile.deskew = deskewToggle.isSelected();

        currentProfile.exportFormat = safeValue(exportFormatComboBox);
        currentProfile.exportNaming = safe(exportNamingField.getText());

        currentProfile.metadataTemplate = safeValue(metadataTemplateComboBox);
        currentProfile.metadataRequiredBeforeExport = metadataRequiredToggle.isSelected();

        currentProfile.lastUpdated = "Updated just now";
        currentProfile.configChips = buildChipsForProfile(currentProfile);

        populateEditor(currentProfile);
        renderProfileCards();
    }

    @FXML
    private void previewExport() {
        selectTab(EditorTab.PREVIEW);
        Platform.runLater(() -> pageScrollPane.setVvalue(0));
    }

    @FXML
    private void previewMetadataForm() {
        selectTab(EditorTab.METADATA);
    }

    @FXML
    private void editTemplate() {
        navigator.showMetadataTemplates();
    }

    @FXML
    private void manageAccess() {
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
    private void showMetadataTab() {
        selectTab(EditorTab.METADATA);
    }

    @FXML
    private void showAccessTab() {
        selectTab(EditorTab.ACCESS);
    }

    @FXML
    private void showPreviewTab() {
        selectTab(EditorTab.PREVIEW);
    }

    private void selectTab(EditorTab tab) {
        syncPreview();

        setTabButtonActive(generalTabButton, tab == EditorTab.GENERAL);
        setTabButtonActive(scanRulesTabButton, tab == EditorTab.SCAN_RULES);
        setTabButtonActive(metadataTabButton, tab == EditorTab.METADATA);
        setTabButtonActive(accessTabButton, tab == EditorTab.ACCESS);
        setTabButtonActive(previewTabButton, tab == EditorTab.PREVIEW);

        setPaneVisible(generalTabPane, tab == EditorTab.GENERAL);
        setPaneVisible(scanRulesTabPane, tab == EditorTab.SCAN_RULES);
        setPaneVisible(metadataTabPane, tab == EditorTab.METADATA);
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

    private void toggleArchive(ProfileCardModel profile) {
        profile.archived = !profile.archived;
        profile.status = profile.archived ? "Archived" : "Active";
        profile.lastUpdated = profile.archived ? "Archived just now" : "Restored just now";

        renderProfileCards();

        if (currentProfile == profile) {
            populateEditor(profile);
        }
    }

    private List<ConfigChip> buildChipsForProfile(ProfileCardModel profile) {
        List<ConfigChip> chips = new ArrayList<>();

        chips.add(profile.barcodeSplitting
                ? new ConfigChip("Barcode Split On", "chip-teal")
                : new ConfigChip("Barcode Split Off", "chip-neutral"));

        chips.add(profile.deskew
                ? new ConfigChip("Deskew", "chip-indigo")
                : new ConfigChip("Deskew Off", "chip-neutral"));

        if (profile.name.toLowerCase(Locale.ROOT).contains("drawing")) {
            chips.add(new ConfigChip("OCR Enabled", "chip-purple"));
        }

        if (profile.brightness != null && !profile.brightness.equalsIgnoreCase("Normal")) {
            chips.add(new ConfigChip("Brightness Correction", "chip-orange"));
        }

        return chips;
    }

    private String statusClassFor(String status) {
        return switch (normalize(status)) {
            case "active" -> "profile-status-active";
            case "draft" -> "profile-status-draft";
            case "archived" -> "profile-status-archived";
            default -> "profile-status-default";
        };
    }

    private String formatAssignedUsers(int assignedUsersCount) {
        return assignedUsersCount + (assignedUsersCount == 1 ? " user assigned" : " users assigned");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void loadSampleProfiles() {
        masterProfiles.setAll(AdminDemoData.profiles());
    }

    private enum EditorTab {
        GENERAL,
        SCAN_RULES,
        METADATA,
        ACCESS,
        PREVIEW
    }

    static final class ProfileCardModel {
        private String name;
        private String code;
        private String description;
        private String status;
        private List<ConfigChip> configChips;
        private String metadataTemplate;
        private String exportNaming;
        private int assignedUsersCount;
        private String lastUpdated;
        private boolean archived;

        private boolean barcodeSplitting;
        private String barcodeDetectedBehavior;
        private String barcodePageBehavior;

        private String defaultRotation;
        private String brightness;
        private String contrast;
        private boolean deskew;

        private String exportFormat;
        private boolean metadataRequiredBeforeExport;

        ProfileCardModel(
                String name,
                String code,
                String description,
                String status,
                List<ConfigChip> configChips,
                String metadataTemplate,
                String exportNaming,
                int assignedUsersCount,
                String lastUpdated,
                boolean archived,
                boolean barcodeSplitting,
                String barcodeDetectedBehavior,
                String barcodePageBehavior,
                String defaultRotation,
                String brightness,
                String contrast,
                boolean deskew,
                String exportFormat,
                boolean metadataRequiredBeforeExport
        ) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.status = status;
            this.configChips = new ArrayList<>(configChips);
            this.metadataTemplate = metadataTemplate;
            this.exportNaming = exportNaming;
            this.assignedUsersCount = assignedUsersCount;
            this.lastUpdated = lastUpdated;
            this.archived = archived;
            this.barcodeSplitting = barcodeSplitting;
            this.barcodeDetectedBehavior = barcodeDetectedBehavior;
            this.barcodePageBehavior = barcodePageBehavior;
            this.defaultRotation = defaultRotation;
            this.brightness = brightness;
            this.contrast = contrast;
            this.deskew = deskew;
            this.exportFormat = exportFormat;
            this.metadataRequiredBeforeExport = metadataRequiredBeforeExport;
        }

        private String displayStatus() {
            return archived ? "Archived" : status;
        }
    }

    record ConfigChip(String label, String styleClass) {
    }

    record AccessUser(String name, String role, String status) {
    }
}
