package easv.gui.controller.Admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MetadataController {

    private static final String ACTIVE_TAB_CLASS = "metadata-tab-active";

    private static final String ALL_STATUSES = "All Statuses";
    private static final String ALL_PROFILES = "All Profiles";

    private static final int DEFAULT_ROWS_PER_PAGE = 10;
    private static final List<Integer> ROWS_PER_PAGE_OPTIONS = List.of(10, 25, 50);

    private static final double TEMPLATE_COLUMN_WIDTH = 24;
    private static final double PROFILES_COLUMN_WIDTH = 12;
    private static final double FIELDS_COLUMN_WIDTH = 9;
    private static final double STATUS_COLUMN_WIDTH = 11;
    private static final double UPDATED_COLUMN_WIDTH = 12;
    private static final double ACTIONS_COLUMN_WIDTH = 32;

    @FXML private ScrollPane overviewPage;
    @FXML private ScrollPane editorPage;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> profileFilterComboBox;
    @FXML private ComboBox<String> templateStatusComboBox;

    @FXML private VBox metadataListContainer;

    @FXML private HBox paginationBar;
    @FXML private Label paginationSummaryLabel;
    @FXML private HBox paginationButtonsBox;
    @FXML private ComboBox<Integer> rowsPerPageComboBox;

    @FXML private Label editorTitleLabel;
    @FXML private Label editorSubtitleLabel;
    @FXML private Label editorStatusBadge;

    @FXML private TextField templateNameField;
    @FXML private TextArea templateDescriptionArea;

    @FXML private Button cancelEditorButton;
    @FXML private Button previewHeaderButton;
    @FXML private Button saveHeaderButton;

    @FXML private Button generalTabButton;
    @FXML private Button fieldsTabButton;
    @FXML private Button assignedProfilesTabButton;
    @FXML private Button previewTabButton;

    @FXML private VBox generalTab;
    @FXML private VBox fieldsTab;
    @FXML private VBox assignedProfilesTab;
    @FXML private VBox previewTab;

    private final ObservableList<MetadataTemplateRow> masterTemplates = FXCollections.observableArrayList();

    private FilteredList<MetadataTemplateRow> filteredTemplates;
    private int currentPage = 1;
    private int rowsPerPage = DEFAULT_ROWS_PER_PAGE;

    @FXML
    private void initialize() {
        configureFilters();
        configureRowsPerPageSelector();
        loadSampleTemplates();
        configureFiltering();

        selectGeneralTab();
        applyFilters();
    }

    private void configureFilters() {
        statusFilterComboBox.getItems().setAll(ALL_STATUSES, "Active", "Draft", "Archived");
        statusFilterComboBox.setValue(ALL_STATUSES);

        profileFilterComboBox.getItems().setAll(
                ALL_PROFILES,
                "Building Archive",
                "Technical Drawings",
                "Court Records",
                "Standard Building Scan",
                "Engineering Archive",
                "Aalborg Building Archive"
        );
        profileFilterComboBox.setValue(ALL_PROFILES);

        templateStatusComboBox.getItems().setAll("Draft", "Active", "Archived");
        templateStatusComboBox.setValue("Active");
    }

    private void configureRowsPerPageSelector() {
        rowsPerPageComboBox.getItems().setAll(ROWS_PER_PAGE_OPTIONS);
        rowsPerPageComboBox.setValue(DEFAULT_ROWS_PER_PAGE);

        rowsPerPageComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue <= 0) {
                return;
            }

            rowsPerPage = newValue;
            currentPage = 1;
            renderTemplates();
        });
    }

    private void configureFiltering() {
        filteredTemplates = new FilteredList<>(masterTemplates, template -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        profileFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        currentPage = 1;

        String searchText = normalize(searchField.getText());
        String selectedStatus = statusFilterComboBox.getValue();
        String selectedProfile = profileFilterComboBox.getValue();

        filteredTemplates.setPredicate(template ->
                matchesSearch(template, searchText)
                        && matchesStatus(template, selectedStatus)
                        && matchesProfile(template, selectedProfile)
        );

        renderTemplates();
    }

    private boolean matchesSearch(MetadataTemplateRow template, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return normalize(template.name()).contains(searchText)
                || normalize(template.description()).contains(searchText)
                || normalize(template.status()).contains(searchText)
                || normalize(template.lastUpdated()).contains(searchText)
                || String.valueOf(template.fieldCount()).contains(searchText)
                || template.assignedProfiles().stream()
                .anyMatch(profile -> normalize(profile).contains(searchText));
    }

    private boolean matchesStatus(MetadataTemplateRow template, String selectedStatus) {
        return selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || template.status().equalsIgnoreCase(selectedStatus);
    }

    private boolean matchesProfile(MetadataTemplateRow template, String selectedProfile) {
        return selectedProfile == null
                || ALL_PROFILES.equals(selectedProfile)
                || template.assignedProfiles().stream()
                .anyMatch(profile -> profile.equalsIgnoreCase(selectedProfile));
    }

    private void renderTemplates() {
        List<MetadataTemplateRow> visibleTemplates = filteredTemplates.stream().toList();

        int totalTemplates = visibleTemplates.size();
        int totalPages = calculateTotalPages(totalTemplates);

        currentPage = clamp(currentPage, 1, totalPages);

        int fromIndex = Math.min((currentPage - 1) * rowsPerPage, totalTemplates);
        int toIndex = Math.min(fromIndex + rowsPerPage, totalTemplates);

        List<MetadataTemplateRow> pageTemplates = visibleTemplates.subList(fromIndex, toIndex);

        metadataListContainer.getChildren().setAll(
                pageTemplates.stream()
                        .map(this::buildTemplateRow)
                        .toList()
        );

        boolean hasTemplates = totalTemplates > 0;

        metadataListContainer.setVisible(hasTemplates);
        metadataListContainer.setManaged(hasTemplates);

        paginationBar.setVisible(hasTemplates);
        paginationBar.setManaged(hasTemplates);

        renderPagination(totalPages, totalTemplates, fromIndex, toIndex);
    }

    private GridPane buildTemplateRow(MetadataTemplateRow template) {
        GridPane row = createTemplateGrid();
        row.getStyleClass().add("metadata-row");
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(metadataListContainer.widthProperty());

        if ("Archived".equalsIgnoreCase(template.status())) {
            row.getStyleClass().add("metadata-row-archived");
        } else {
            row.setOnMouseClicked(event -> showEditEditor(template));
        }

        addCell(row, buildTemplateNameCell(template), 0, HPos.LEFT);
        addCell(row, createTableLabel(formatAssignedProfiles(template.assignedProfiles().size())), 1, HPos.LEFT);
        addCell(row, createTableLabel(template.fieldCount() + " fields"), 2, HPos.LEFT);
        addCell(row, buildStatusBadge(template.status()), 3, HPos.LEFT);
        addCell(row, createTableLabel(template.lastUpdated()), 4, HPos.LEFT);
        addCell(row, buildActionsCell(template), 5, HPos.LEFT);

        return row;
    }

    private GridPane createTemplateGrid() {
        GridPane grid = new GridPane();

        grid.getColumnConstraints().setAll(
                createPercentColumn(TEMPLATE_COLUMN_WIDTH),
                createPercentColumn(PROFILES_COLUMN_WIDTH),
                createPercentColumn(FIELDS_COLUMN_WIDTH),
                createPercentColumn(STATUS_COLUMN_WIDTH),
                createPercentColumn(UPDATED_COLUMN_WIDTH),
                createPercentColumn(ACTIONS_COLUMN_WIDTH)
        );

        return grid;
    }

    private ColumnConstraints createPercentColumn(double percentWidth) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percentWidth);
        column.setHgrow(Priority.ALWAYS);
        column.setFillWidth(true);
        return column;
    }

    private void addCell(GridPane row, Node content, int columnIndex, HPos horizontalAlignment) {
        GridPane.setHalignment(content, horizontalAlignment);
        GridPane.setValignment(content, VPos.CENTER);
        row.add(content, columnIndex, 0);
    }

    private VBox buildTemplateNameCell(MetadataTemplateRow template) {
        Label titleLabel = new Label(template.name());
        titleLabel.getStyleClass().add("metadata-template-title");

        Label descriptionLabel = new Label(template.description());
        descriptionLabel.getStyleClass().add("metadata-template-description");
        descriptionLabel.setWrapText(true);

        return new VBox(4, titleLabel, descriptionLabel);
    }

    private Label createTableLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("metadata-table-text");
        return label;
    }

    private Label buildStatusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().addAll("metadata-status-badge", statusClassFor(status));
        return badge;
    }

    private HBox buildActionsCell(MetadataTemplateRow template) {
        HBox actionBox = new HBox(12);
        actionBox.getStyleClass().add("metadata-actions");
        actionBox.setAlignment(Pos.CENTER_LEFT);
        actionBox.setOnMouseClicked(MouseEvent::consume);

        Button openButton = createActionButton("Open");
        openButton.setOnAction(event -> showEditEditor(template));

        Button previewButton = createActionButton("Preview");
        previewButton.setOnAction(event -> showPreviewEditor(template));

        Button duplicateButton = createActionButton("Duplicate");
        duplicateButton.setOnAction(event -> showDuplicateEditor(template));

        actionBox.getChildren().addAll(openButton, previewButton, duplicateButton);

        if ("Archived".equalsIgnoreCase(template.status())) {
            Button restoreButton = createMutedActionButton("Restore");
            actionBox.getChildren().add(restoreButton);
        } else {
            Button archiveButton = createMutedActionButton("Archive");
            actionBox.getChildren().add(archiveButton);
        }

        return actionBox;
    }

    private Button createActionButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("metadata-action-button");
        button.setFocusTraversable(false);
        return button;
    }

    private Button createMutedActionButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("metadata-action-button-muted");
        button.setFocusTraversable(false);
        return button;
    }

    private String statusClassFor(String status) {
        return switch (normalize(status)) {
            case "active" -> "metadata-status-active";
            case "draft" -> "metadata-status-draft";
            case "archived" -> "metadata-status-archived";
            default -> "metadata-status-archived";
        };
    }

    private String formatAssignedProfiles(int count) {
        return count == 1 ? "1 profile" : count + " profiles";
    }

    private int calculateTotalPages(int totalTemplates) {
        if (totalTemplates == 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalTemplates / rowsPerPage);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private void renderPagination(int totalPages, int totalTemplates, int fromIndex, int toIndex) {
        paginationButtonsBox.getChildren().clear();

        if (totalTemplates == 0) {
            paginationSummaryLabel.setText("Showing 0 templates");
            return;
        }

        paginationSummaryLabel.setText(
                "Showing " + (fromIndex + 1) + "-" + toIndex + " of " + totalTemplates + " templates"
        );

        paginationButtonsBox.getChildren().add(createPaginationButton("<<", 1, currentPage == 1));
        paginationButtonsBox.getChildren().add(createPaginationButton("<", currentPage - 1, currentPage == 1));

        for (String pageItem : buildPageItems(totalPages)) {
            Node paginationItem = "...".equals(pageItem)
                    ? createPaginationEllipsis()
                    : createPaginationButton(pageItem, Integer.parseInt(pageItem), false);

            paginationButtonsBox.getChildren().add(paginationItem);
        }

        paginationButtonsBox.getChildren().add(createPaginationButton(">", currentPage + 1, currentPage == totalPages));
        paginationButtonsBox.getChildren().add(createPaginationButton(">>", totalPages, currentPage == totalPages));
    }

    private Label createPaginationEllipsis() {
        Label ellipsis = new Label("...");
        ellipsis.getStyleClass().add("pagination-ellipsis");
        return ellipsis;
    }

    private List<String> buildPageItems(int totalPages) {
        List<String> items = new ArrayList<>();

        if (totalPages <= 5) {
            for (int page = 1; page <= totalPages; page++) {
                items.add(String.valueOf(page));
            }

            return items;
        }

        if (currentPage <= 3) {
            items.add("1");
            items.add("2");
            items.add("3");
            items.add("...");
            items.add(String.valueOf(totalPages));
            return items;
        }

        if (currentPage >= totalPages - 2) {
            items.add("1");
            items.add("...");
            items.add(String.valueOf(totalPages - 2));
            items.add(String.valueOf(totalPages - 1));
            items.add(String.valueOf(totalPages));
            return items;
        }

        items.add("1");
        items.add("...");
        items.add(String.valueOf(currentPage - 1));
        items.add(String.valueOf(currentPage));
        items.add(String.valueOf(currentPage + 1));
        items.add("...");
        items.add(String.valueOf(totalPages));

        return items;
    }

    private Button createPaginationButton(String text, int targetPage, boolean disabled) {
        Button button = new Button(text);
        button.getStyleClass().add("pagination-button");
        button.setFocusTraversable(false);
        button.setDisable(disabled);

        boolean isCurrentPageButton = text.equals(String.valueOf(currentPage));

        if (isCurrentPageButton) {
            button.getStyleClass().add("pagination-button-active");
            return button;
        }

        if (!disabled) {
            button.setOnAction(event -> {
                currentPage = targetPage;
                renderTemplates();
            });
        }

        return button;
    }

    @FXML
    private void showOverview() {
        editorPage.setVisible(false);
        editorPage.setManaged(false);

        overviewPage.setVisible(true);
        overviewPage.setManaged(true);
    }

    @FXML
    private void showCreateEditor() {
        MetadataTemplateRow template = new MetadataTemplateRow(
                "Create Metadata Template",
                "Create a reusable metadata form for scanning profiles.",
                List.of(),
                0,
                "Draft",
                "New"
        );

        editorTitleLabel.setText(template.name());
        editorSubtitleLabel.setText(template.description());
        setEditorStatus("Draft");

        templateNameField.clear();
        templateDescriptionArea.clear();
        templateStatusComboBox.setValue("Draft");

        cancelEditorButton.setVisible(true);
        cancelEditorButton.setManaged(true);

        previewHeaderButton.setText("Preview Form");
        saveHeaderButton.setText("Create Template");

        showEditor();
        selectGeneralTab();
    }

    @FXML
    private void showEditEditor() {
        if (masterTemplates.isEmpty()) {
            return;
        }

        showEditEditor(masterTemplates.get(0));
    }

    private void showEditEditor(MetadataTemplateRow template) {
        editorTitleLabel.setText(template.name());
        editorSubtitleLabel.setText(template.description());
        setEditorStatus(template.status());

        templateNameField.setText(template.name());
        templateDescriptionArea.setText(template.description());
        templateStatusComboBox.setValue(template.status());

        cancelEditorButton.setVisible(false);
        cancelEditorButton.setManaged(false);

        previewHeaderButton.setText("Preview Form");
        saveHeaderButton.setText("Save Changes");

        showEditor();
        selectGeneralTab();
    }

    @FXML
    private void showDuplicateEditor() {
        if (masterTemplates.isEmpty()) {
            return;
        }

        showDuplicateEditor(masterTemplates.get(0));
    }

    private void showDuplicateEditor(MetadataTemplateRow template) {
        editorTitleLabel.setText("Copy of " + template.name());
        editorSubtitleLabel.setText("Draft copy of an existing metadata template.");
        setEditorStatus("Draft");

        templateNameField.setText("Copy of " + template.name());
        templateDescriptionArea.setText(template.description());
        templateStatusComboBox.setValue("Draft");

        cancelEditorButton.setVisible(true);
        cancelEditorButton.setManaged(true);

        previewHeaderButton.setText("Preview Form");
        saveHeaderButton.setText("Create Template");

        showEditor();
        selectGeneralTab();
    }

    @FXML
    private void showPreviewEditor() {
        if (masterTemplates.isEmpty()) {
            return;
        }

        showPreviewEditor(masterTemplates.get(0));
    }

    private void showPreviewEditor(MetadataTemplateRow template) {
        showEditEditor(template);
        selectPreviewTab();
    }

    private void showEditor() {
        overviewPage.setVisible(false);
        overviewPage.setManaged(false);

        editorPage.setVisible(true);
        editorPage.setManaged(true);
    }

    private void setEditorStatus(String status) {
        editorStatusBadge.setText(status);
        editorStatusBadge.getStyleClass().setAll("metadata-status-badge", statusClassFor(status));
    }

    @FXML
    private void selectGeneralTab() {
        showTab(generalTab, generalTabButton);
    }

    @FXML
    private void selectFieldsTab() {
        showTab(fieldsTab, fieldsTabButton);
    }

    @FXML
    private void selectAssignedProfilesTab() {
        showTab(assignedProfilesTab, assignedProfilesTabButton);
    }

    @FXML
    private void selectPreviewTab() {
        showTab(previewTab, previewTabButton);
    }

    private void showTab(VBox selectedTab, Button selectedButton) {
        for (VBox tab : getTabs()) {
            boolean selected = tab == selectedTab;
            tab.setVisible(selected);
            tab.setManaged(selected);
        }

        for (Button button : getTabButtons()) {
            button.getStyleClass().remove(ACTIVE_TAB_CLASS);
        }

        if (!selectedButton.getStyleClass().contains(ACTIVE_TAB_CLASS)) {
            selectedButton.getStyleClass().add(ACTIVE_TAB_CLASS);
        }
    }

    private List<VBox> getTabs() {
        return List.of(
                generalTab,
                fieldsTab,
                assignedProfilesTab,
                previewTab
        );
    }

    private List<Button> getTabButtons() {
        return List.of(
                generalTabButton,
                fieldsTabButton,
                assignedProfilesTabButton,
                previewTabButton
        );
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private void loadSampleTemplates() {
        masterTemplates.setAll(
                new MetadataTemplateRow(
                        "Building Archive Metadata",
                        "Used for municipal building archive scans.",
                        List.of("Building Archive", "Standard Building Scan", "Aalborg Building Archive"),
                        7,
                        "Active",
                        "Today"
                ),
                new MetadataTemplateRow(
                        "Technical Drawings Metadata",
                        "Used for engineering drawings and technical documentation.",
                        List.of("Technical Drawings", "Engineering Archive"),
                        5,
                        "Active",
                        "Yesterday"
                ),
                new MetadataTemplateRow(
                        "Court Records Metadata",
                        "Registration fields for legal archive documents.",
                        List.of("Court Records"),
                        9,
                        "Draft",
                        "3 days ago"
                ),
                new MetadataTemplateRow(
                        "Client Intake Metadata",
                        "Basic metadata used when creating new archive cases.",
                        List.of("Building Archive"),
                        6,
                        "Active",
                        "1 week ago"
                ),
                new MetadataTemplateRow(
                        "Box Registration Metadata",
                        "Fields used when preparing boxes before scanning.",
                        List.of("Standard Building Scan"),
                        4,
                        "Active",
                        "2 weeks ago"
                ),
                new MetadataTemplateRow(
                        "Legacy Archive Metadata",
                        "Archived template kept for older scanning workflows.",
                        List.of("Court Records"),
                        8,
                        "Archived",
                        "Last month"
                )
        );
    }

    private record MetadataTemplateRow(
            String name,
            String description,
            List<String> assignedProfiles,
            int fieldCount,
            String status,
            String lastUpdated
    ) {
    }
}