package easv.gui.controller.Admin;

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
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public class ProfilesController {

    private static final String ALL_STATUSES = "All Statuses";
    private static final String ALL_METADATA_TEMPLATES = "All Metadata Templates";

    private static final double PAGE_HORIZONTAL_PADDING = 48;
    private static final double CARD_GAP = 18;
    private static final double MIN_CARD_WIDTH = 330;

    @FXML private ScrollPane pageScrollPane;
    @FXML private VBox pageRoot;

    @FXML private Label profilesCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> metadataFilterComboBox;
    @FXML private GridPane profilesCardsGrid;
    @FXML private VBox emptyStateBox;

    private final ObservableList<ProfileCardModel> masterProfiles = FXCollections.observableArrayList();
    private FilteredList<ProfileCardModel> filteredProfiles;

    @FXML
    private void initialize() {
        configureFilters();
        loadSampleProfiles();
        configureFiltering();
        configureResponsiveGrid();
        applyFilters();
    }

    private void configureFilters() {
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

    private void configureFiltering() {
        filteredProfiles = new FilteredList<>(masterProfiles, profile -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        metadataFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void configureResponsiveGrid() {
        pageScrollPane.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) -> {
            pageRoot.setPrefWidth(newBounds.getWidth());
            layoutProfileGrid();
        });

        pageRoot.widthProperty().addListener((observable, oldWidth, newWidth) -> layoutProfileGrid());

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

        return normalize(profile.name()).contains(searchText)
                || normalize(profile.description()).contains(searchText)
                || normalize(profile.metadataTemplate()).contains(searchText)
                || normalize(profile.exportNaming()).contains(searchText)
                || normalize(profile.status()).contains(searchText)
                || profile.configChips().stream()
                .anyMatch(chip -> normalize(chip.label()).contains(searchText));
    }

    private boolean matchesStatus(ProfileCardModel profile, String selectedStatus) {
        return selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || profile.status().equalsIgnoreCase(selectedStatus);
    }

    private boolean matchesMetadata(ProfileCardModel profile, String selectedMetadata) {
        return selectedMetadata == null
                || ALL_METADATA_TEMPLATES.equals(selectedMetadata)
                || profile.metadataTemplate().equalsIgnoreCase(selectedMetadata);
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

        if (viewportWidth <= 0) {
            viewportWidth = pageRoot.getWidth();
        }

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
        HBox footerRow = buildFooterRow();

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

        Label nameLabel = new Label(profile.name());
        nameLabel.getStyleClass().add("profile-card-title");

        Label descriptionLabel = new Label(profile.description());
        descriptionLabel.getStyleClass().add("profile-card-description");
        descriptionLabel.setWrapText(true);

        titleBox.getChildren().addAll(nameLabel, descriptionLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(profile.status());
        statusBadge.getStyleClass().addAll("profile-status-badge", statusClassFor(profile.status()));

        headerRow.getChildren().addAll(titleBox, spacer, statusBadge);
        return headerRow;
    }

    private FlowPane buildChipsPane(ProfileCardModel profile) {
        FlowPane chipsPane = new FlowPane();
        chipsPane.getStyleClass().add("profile-chip-group");
        chipsPane.setHgap(8);
        chipsPane.setVgap(8);

        for (ConfigChip chip : profile.configChips()) {
            Label chipLabel = new Label(chip.label());
            chipLabel.getStyleClass().addAll("profile-config-chip", chip.styleClass());
            chipsPane.getChildren().add(chipLabel);
        }

        return chipsPane;
    }

    private VBox buildInfoBox(ProfileCardModel profile) {
        VBox infoBox = new VBox(10);
        infoBox.getChildren().addAll(
                buildInfoBlock("Metadata Template", profile.metadataTemplate(), false),
                buildInfoBlock("Export Naming", profile.exportNaming(), true),
                buildInfoBlock("Assigned Users", formatAssignedUsers(profile.assignedUsersCount()), false),
                buildInfoBlock("Updated", profile.lastUpdated(), false)
        );

        return infoBox;
    }

    private HBox buildFooterRow() {
        HBox footerRow = new HBox(10);
        footerRow.getStyleClass().add("profile-card-footer");
        footerRow.setAlignment(Pos.CENTER_LEFT);

        Button openButton = createActionButton("Open", "profile-open-button");
        Button duplicateButton = createActionButton("Duplicate", "profile-secondary-button");
        Button archiveButton = createIconActionButton("Archive", "profile-icon-button");

        footerRow.getChildren().addAll(openButton, duplicateButton, archiveButton);
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
        button.setFocusTraversable(false);
        return button;
    }

    private Button createIconActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setContentDisplay(ContentDisplay.TEXT_ONLY);
        button.setFocusTraversable(false);
        return button;
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

    private void loadSampleProfiles() {
        masterProfiles.setAll(
                new ProfileCardModel(
                        "Building Archive",
                        "Used for municipal archive scanning",
                        "Active",
                        List.of(
                                new ConfigChip("Barcode Split On", "chip-teal"),
                                new ConfigChip("Auto Rotate On", "chip-blue"),
                                new ConfigChip("Deskew", "chip-indigo")
                        ),
                        "Building Archive Metadata",
                        "BuildingArchive_{boxId}",
                        5,
                        "Updated today"
                ),
                new ProfileCardModel(
                        "Technical Drawings",
                        "Used for engineering and drawing archives",
                        "Active",
                        List.of(
                                new ConfigChip("Barcode Split On", "chip-teal"),
                                new ConfigChip("OCR Enabled", "chip-purple"),
                                new ConfigChip("Deskew", "chip-indigo"),
                                new ConfigChip("Blank Page Removal", "chip-amber")
                        ),
                        "Technical Drawings Metadata",
                        "TechnicalDrawings_{boxId}",
                        2,
                        "Updated yesterday"
                ),
                new ProfileCardModel(
                        "Court Records",
                        "Used for legal archive workflows",
                        "Draft",
                        List.of(
                                new ConfigChip("Barcode Split Off", "chip-neutral"),
                                new ConfigChip("OCR Enabled", "chip-purple"),
                                new ConfigChip("Brightness Correction", "chip-orange")
                        ),
                        "Court Records Metadata",
                        "CourtRecords_{boxId}",
                        1,
                        "Updated 3 days ago"
                )
        );
    }

    private record ProfileCardModel(
            String name,
            String description,
            String status,
            List<ConfigChip> configChips,
            String metadataTemplate,
            String exportNaming,
            int assignedUsersCount,
            String lastUpdated
    ) {
    }

    private record ConfigChip(
            String label,
            String styleClass
    ) {
    }
}