package easv.gui.controller.admin;

import easv.be.ScanProfile;
import easv.be.User;
import easv.bll.AdminManager;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    private final ObservableList<ScanProfile> masterProfiles = FXCollections.observableArrayList();

    private FilteredList<ScanProfile> filteredProfiles;
    private ScanProfile currentProfile;

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

        metadataFilterComboBox.getItems().setAll(ALL_METADATA_TEMPLATES);
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

        defaultRotationComboBox.getItems().setAll("0 deg", "90 deg", "180 deg", "270 deg");
        brightnessComboBox.getItems().setAll("Normal", "Lighter", "Darker");
        contrastComboBox.getItems().setAll("Normal", "Higher", "Lower");

        exportFormatComboBox.getItems().setAll(
                "Multi-page TIFF",
                "PDF",
                "PDF/A",
                "Single-page TIFF"
        );

        metadataTemplateComboBox.getItems().setAll("");

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

    private void loadProfiles() {
        if (adminManager == null) {
            masterProfiles.clear();
            refreshMetadataOptions();
            return;
        }

        masterProfiles.setAll(adminManager.getProfiles());
        refreshMetadataOptions();
    }

    private void refreshMetadataOptions() {
        LinkedHashSet<String> metadataNames = new LinkedHashSet<>();

        if (adminManager != null) {
            adminManager.getMetadataTemplates().forEach(template -> {
                if (!clean(template.getName()).isBlank()) {
                    metadataNames.add(template.getName());
                }
            });
        }

        masterProfiles.forEach(profile -> {
            if (!clean(profile.getMetadataTemplateName()).isBlank()) {
                metadataNames.add(profile.getMetadataTemplateName());
            }
        });

        String selectedFilter = metadataFilterComboBox.getValue();

        List<String> filterOptions = new ArrayList<>();
        filterOptions.add(ALL_METADATA_TEMPLATES);
        filterOptions.addAll(metadataNames);

        metadataFilterComboBox.getItems().setAll(filterOptions);
        metadataFilterComboBox.setValue(filterOptions.contains(selectedFilter)
                ? selectedFilter
                : ALL_METADATA_TEMPLATES);

        String selectedEditorTemplate = metadataTemplateComboBox.getValue();

        List<String> editorOptions = new ArrayList<>(metadataNames);

        if (editorOptions.isEmpty()) {
            editorOptions.add("");
        }

        metadataTemplateComboBox.getItems().setAll(editorOptions);
        metadataTemplateComboBox.setValue(editorOptions.contains(selectedEditorTemplate)
                ? selectedEditorTemplate
                : editorOptions.get(0));
    }

    private void applyFilters() {
        if (filteredProfiles == null) {
            return;
        }

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

    private boolean matchesSearch(ScanProfile profile, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return normalize(profile.getName()).contains(searchText)
                || normalize(profile.getCode()).contains(searchText)
                || normalize(profile.getDescription()).contains(searchText)
                || normalize(profile.getMetadataTemplateName()).contains(searchText)
                || normalize(profile.getExportNaming()).contains(searchText)
                || normalize(displayStatus(profile)).contains(searchText)
                || configChipsFor(profile).stream()
                .anyMatch(chip -> normalize(chip.getLabel()).contains(searchText));
    }

    private boolean matchesStatus(ScanProfile profile, String selectedStatus) {
        return selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || displayStatus(profile).equalsIgnoreCase(selectedStatus);
    }

    private boolean matchesMetadata(ScanProfile profile, String selectedMetadata) {
        return selectedMetadata == null
                || ALL_METADATA_TEMPLATES.equals(selectedMetadata)
                || profile.getMetadataTemplateName().equalsIgnoreCase(selectedMetadata);
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

        headerRow.getChildren().addAll(titleBox, spacer, statusBadge);
        return headerRow;
    }

    private FlowPane buildChipsPane(ScanProfile profile) {
        FlowPane chipsPane = new FlowPane();
        chipsPane.getStyleClass().add("profile-chip-group");
        chipsPane.setHgap(8);
        chipsPane.setVgap(8);

        for (ConfigChip chip : configChipsFor(profile)) {
            Label chipLabel = new Label(chip.getLabel());
            chipLabel.getStyleClass().addAll("profile-config-chip", chip.getStyleClass());
            chipsPane.getChildren().add(chipLabel);
        }

        return chipsPane;
    }

    private VBox buildInfoBox(ScanProfile profile) {
        VBox infoBox = new VBox(10);
        infoBox.getChildren().addAll(
                buildInfoBlock("Metadata Template", displayText(profile.getMetadataTemplateName(), "No metadata template"), false),
                buildInfoBlock("Export Naming", displayText(profile.getExportNaming(), "{profileCode}_{boxId}"), true),
                buildInfoBlock("Assigned Users", formatAssignedUsers(assignedUserCountFor(profile)), false),
                buildInfoBlock("Updated", displayText(profile.getLastUpdated(), "Not updated yet"), false)
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

    private void openProfile(ScanProfile profile) {
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
        profileCodeField.setText(profile.getCode());
        profileDescriptionArea.setText(profile.getDescription());
        profileStatusComboBox.setValue(displayStatus(profile));

        barcodeSplitToggle.setSelected(profile.isBarcodeSplitting());
        barcodeDetectedComboBox.setValue(profile.getBarcodeDetectedBehavior());
        barcodePageBehaviorComboBox.setValue(profile.getBarcodePageBehavior());

        defaultRotationComboBox.setValue(profile.getDefaultRotation());
        brightnessComboBox.setValue(profile.getBrightness());
        contrastComboBox.setValue(profile.getContrast());
        deskewToggle.setSelected(profile.isDeskew());

        exportFormatComboBox.setValue(profile.getExportFormat());
        exportNamingField.setText(profile.getExportNaming());

        metadataTemplateComboBox.setValue(profile.getMetadataTemplateName());
        metadataRequiredToggle.setSelected(profile.isMetadataRequiredBeforeExport());

        populateAccessRows(profile);
        syncPreview();
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
        String profileName = clean(profileNameField.getText());
        String profileCode = clean(profileCodeField.getText());
        String namingPattern = clean(exportNamingField.getText());

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
                ? "Enabled - " + safeValue(barcodeDetectedComboBox) + " - " + shortBarcodePageBehavior(safeValue(barcodePageBehaviorComboBox))
                : "Disabled";

        previewBarcodeLabel.setText(barcodeStatus);
        previewMetadataTemplateLabel.setText(displayText(safeValue(metadataTemplateComboBox), "No metadata template"));

        previewPageCorrectionLabel.setText(
                "Rotation " + safeValue(defaultRotationComboBox)
                        + " - Brightness " + safeValue(brightnessComboBox)
                        + " - Contrast " + safeValue(contrastComboBox)
                        + " - Deskew " + (deskewToggle.isSelected() ? "Enabled" : "Disabled")
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
        ScanProfile newProfile = adminManager.createProfile(createDefaultProfileInput());

        searchField.clear();
        statusFilterComboBox.setValue(ALL_STATUSES);
        metadataFilterComboBox.setValue(ALL_METADATA_TEMPLATES);

        loadProfiles();
        applyFilters();
        openProfile(newProfile);
    }

    @FXML
    private void saveChanges() {
        if (currentProfile == null) {
            return;
        }

        try {
            ScanProfile savedProfile = adminManager.updateProfile(
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

    private AdminManager.ProfileInput createDefaultProfileInput() {
        String name = createUniqueProfileName();
        String code = createProfileCode(name);

        return new AdminManager.ProfileInput(
                name,
                code,
                "Describe this scanning workflow profile.",
                "Draft",
                defaultMetadataTemplateName(),
                "{profileCode}_{boxId}",
                false,
                "Start new document",
                "Remove barcode page from final document",
                "0 deg",
                "Normal",
                "Normal",
                true,
                "Multi-page TIFF",
                true
        );
    }

    private AdminManager.ProfileInput createProfileInputFromEditor() {
        return new AdminManager.ProfileInput(
                clean(profileNameField.getText()),
                clean(profileCodeField.getText()),
                clean(profileDescriptionArea.getText()),
                safeValue(profileStatusComboBox),
                safeValue(metadataTemplateComboBox),
                clean(exportNamingField.getText()),
                barcodeSplitToggle.isSelected(),
                safeValue(barcodeDetectedComboBox),
                safeValue(barcodePageBehaviorComboBox),
                safeValue(defaultRotationComboBox),
                safeValue(brightnessComboBox),
                safeValue(contrastComboBox),
                deskewToggle.isSelected(),
                safeValue(exportFormatComboBox),
                metadataRequiredToggle.isSelected()
        );
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

        if (profile.getName().toLowerCase(Locale.ROOT).contains("drawing")) {
            chips.add(new ConfigChip("OCR Enabled", "chip-purple"));
        }

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
        String normalizedName = normalize(name);

        return adminManager.getProfiles().stream()
                .map(ScanProfile::getName)
                .map(this::normalize)
                .anyMatch(existingName -> existingName.equals(normalizedName));
    }

    private String createProfileCode(String name) {
        String baseCode = clean(name).replaceAll("[^A-Za-z0-9]", "");

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

    private String defaultMetadataTemplateName() {
        return adminManager.getMetadataTemplates().stream()
                .map(template -> template.getName())
                .filter(name -> !clean(name).isBlank())
                .findFirst()
                .orElse("");
    }

    private String displayStatus(ScanProfile profile) {
        return profile.isArchived() ? "Archived" : profile.getStatus();
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

    private String displayText(String value, String fallback) {
        String cleanedValue = clean(value);
        return cleanedValue.isBlank() ? fallback : cleanedValue;
    }

    private String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private enum EditorTab {
        GENERAL,
        SCAN_RULES,
        METADATA,
        ACCESS,
        PREVIEW
    }

    static class ConfigChip {
        private final String label;
        private final String styleClass;

        ConfigChip(String label, String styleClass) {
            this.label = label;
            this.styleClass = styleClass;
        }

        String getLabel() {
            return label;
        }

        String getStyleClass() {
            return styleClass;
        }
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

        String displayStatus() {
            return archived ? "Archived" : status;
        }
    }

    static final class AccessUser {
        private final String name;
        private final String role;
        private final String status;

        AccessUser(String name, String role, String status) {
            this.name = name;
            this.role = role;
            this.status = status;
        }

        String name() {
            return name;
        }

        String role() {
            return role;
        }

        String status() {
            return status;
        }
    }
}
