package easv.gui.controller.admin;

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
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

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

    private static final String OPEN_ICON =
            "M4 4h7l2 2h7v3H4V4zm0 5h17l-2 11H6L4 9zm3.4 9h10.2l1.3-7H6.1l1.3 7z";

    private static final String PREVIEW_ICON =
            "M12 5c-6.5 0-10 7-10 7s3.5 7 10 7 10-7 10-7-3.5-7-10-7zm0 11a4 4 0 1 1 0-8 4 4 0 0 1 0 8zm0-2a2 2 0 1 0 0-4 2 2 0 0 0 0 4z";

    private static final String ARCHIVE_ICON =
            "M3 4h18v5H3V4zm2 2v1h14V6H5zm1 5h12v9H6v-9zm4 3v2h4v-2h-4z";

    private static final String RESTORE_ICON =
            "M12 5a7 7 0 0 1 6.32 4H16l3.5 3.5L23 9h-2.55A9 9 0 1 0 12 21v-2a7 7 0 1 1 0-14z";

    private static final String EDIT_ICON =
            "M3 14.25V17h2.75L14.81 7.94l-2.75-2.75L3 14.25zm14.71-8.04a1.003 1.003 0 0 0 0-1.42l-1.5-1.5a1.003 1.003 0 0 0-1.42 0l-1.17 1.17 2.75 2.75 1.34-1z";

    private static final String DELETE_ICON =
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";

    @FXML private ScrollPane overviewPage;
    @FXML private ScrollPane editorPage;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> profileFilterComboBox;
    @FXML private ComboBox<String> templateStatusComboBox;

    @FXML private VBox metadataListContainer;
    @FXML private VBox metadataFieldsList;
    @FXML private VBox metadataProfileList;

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

    @FXML private VBox generalTab;
    @FXML private VBox fieldsTab;
    @FXML private VBox assignedProfilesTab;
    @FXML private VBox previewTab;

    private final ObservableList<MetadataTemplateRow> masterTemplates = FXCollections.observableArrayList();
    private final ObservableList<TemplateFieldRow> templateFields = FXCollections.observableArrayList();
    private final ObservableList<AssignedProfileRow> assignedProfiles = FXCollections.observableArrayList();

    private FilteredList<MetadataTemplateRow> filteredTemplates;
    private int currentPage = 1;
    private int rowsPerPage = DEFAULT_ROWS_PER_PAGE;

    @FXML
    private void initialize() {
        configureFilters();
        configureRowsPerPageSelector();
        loadSampleTemplates();
        loadTemplateFields();
        loadAssignedProfiles();
        configureFiltering();

        renderTemplateFields();
        renderAssignedProfiles();

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
                "Aalborg Building Archive",
                "Municipal Archive Scan"
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
        List<MetadataTemplateRow> visibleTemplates = new ArrayList<>(filteredTemplates);

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

        boolean archived = isStatus(template, "Archived");

        if (archived) {
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
        HBox actions = new HBox(15);
        actions.getStyleClass().add("metadata-template-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setOnMouseClicked(MouseEvent::consume);

        Button previewButton = createTemplateActionButton(
                "Preview",
                PREVIEW_ICON,
                "metadata-template-action-preview",
                "metadata-template-action-preview-icon"
        );
        previewButton.setOnAction(event -> showPreviewEditor(template));

        if (isStatus(template, "Archived")) {
            Button restoreButton = createTemplateActionButton(
                    "Restore",
                    RESTORE_ICON,
                    "metadata-template-action-restore",
                    "metadata-template-action-restore-icon"
            );
            restoreButton.setOnAction(event -> restoreTemplate(template));

            actions.getChildren().addAll(previewButton, restoreButton);
            return actions;
        }

        Button openButton = createTemplateActionButton(
                "Open",
                OPEN_ICON,
                "metadata-template-action-open",
                "metadata-template-action-open-icon"
        );
        openButton.setOnAction(event -> showEditEditor(template));

        Button archiveButton = createTemplateActionButton(
                "Archive",
                ARCHIVE_ICON,
                "metadata-template-action-archive",
                "metadata-template-action-archive-icon"
        );
        archiveButton.setOnAction(event -> archiveTemplate(template));

        actions.getChildren().addAll(openButton, previewButton, archiveButton);
        return actions;
    }

    private Button createTemplateActionButton(String text, String iconPath, String buttonClass, String iconClass) {
        Button button = new Button(text);
        button.getStyleClass().setAll("metadata-template-action-button", buttonClass);
        button.setGraphic(createActionIcon(iconPath, iconClass));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(6);
        button.setFocusTraversable(false);
        return button;
    }

    private StackPane createActionIcon(String iconPath, String iconClass) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add(iconClass);

        StackPane iconShell = new StackPane(icon);
        iconShell.getStyleClass().add("metadata-template-action-icon-shell");

        return iconShell;
    }

    @FXML
    private void addField() {
        int nextFieldNumber = templateFields.size() + 1;

        templateFields.add(new TemplateFieldRow(
                "New Field " + nextFieldNumber,
                "Text",
                false,
                "Enter value"
        ));

        renderTemplateFields();
    }

    private void loadTemplateFields() {
        templateFields.setAll(
                new TemplateFieldRow("Box ID", "Text", true, "Enter box ID"),
                new TemplateFieldRow("Case Number", "Text", true, "Enter case number"),
                new TemplateFieldRow("Building Address", "Text", true, "Enter building address"),
                new TemplateFieldRow("Date Range", "Date", false, "Select date range"),
                new TemplateFieldRow("Department", "Dropdown", false, "Select department"),
                new TemplateFieldRow("Notes", "Long Text", false, "Optional notes")
        );
    }

    private void renderTemplateFields() {
        metadataFieldsList.getChildren().setAll(
                templateFields.stream()
                        .map(this::buildFieldRow)
                        .toList()
        );
    }

    private GridPane buildFieldRow(TemplateFieldRow field) {
        GridPane row = new GridPane();
        row.getStyleClass().add("metadata-field-row");
        row.setMaxWidth(Double.MAX_VALUE);

        row.getColumnConstraints().setAll(
                createPercentColumn(6),
                createPercentColumn(23),
                createPercentColumn(15),
                createPercentColumn(14),
                createPercentColumn(28),
                createPercentColumn(14)
        );

        Label dragHandle = new Label("⋮⋮");
        dragHandle.getStyleClass().add("metadata-drag-handle");

        Label nameLabel = new Label(field.name());
        nameLabel.getStyleClass().add("metadata-field-name");

        Label typeLabel = new Label(field.type());
        typeLabel.getStyleClass().add("metadata-table-text");

        Label requiredLabel = new Label(field.required() ? "Yes" : "No");
        requiredLabel.getStyleClass().addAll(
                "metadata-required-pill",
                field.required() ? "metadata-required-yes" : "metadata-required-no"
        );

        Label placeholderLabel = new Label(field.placeholder());
        placeholderLabel.getStyleClass().add("metadata-table-text");

        Button editButton = createFieldIconButton(EDIT_ICON, "metadata-edit-icon", false);
        Button deleteButton = createFieldIconButton(DELETE_ICON, "metadata-delete-icon", true);

        deleteButton.setOnAction(event -> {
            templateFields.remove(field);
            renderTemplateFields();
        });

        HBox actions = new HBox(12, editButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        addCell(row, dragHandle, 0, HPos.LEFT);
        addCell(row, nameLabel, 1, HPos.LEFT);
        addCell(row, typeLabel, 2, HPos.LEFT);
        addCell(row, requiredLabel, 3, HPos.LEFT);
        addCell(row, placeholderLabel, 4, HPos.LEFT);
        addCell(row, actions, 5, HPos.LEFT);

        return row;
    }

    private Button createFieldIconButton(String iconPath, String iconClass, boolean destructive) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add(iconClass);

        StackPane iconShell = new StackPane(icon);
        iconShell.getStyleClass().add("action-icon-shell");

        Button button = new Button();
        button.setGraphic(iconShell);
        button.setFocusTraversable(false);

        if (destructive) {
            button.getStyleClass().setAll("metadata-icon-action-button", "metadata-icon-action-button-destructive");
        } else {
            button.getStyleClass().setAll("metadata-icon-action-button");
        }

        return button;
    }

    private void loadAssignedProfiles() {
        assignedProfiles.setAll(
                new AssignedProfileRow("Aalborg Building Archive", "AalborgBuilding_{boxId}", "Active", true, false),
                new AssignedProfileRow("Standard Building Scan", "StandardBuilding_{boxId}", "Active", true, false),
                new AssignedProfileRow("Municipal Archive Scan", "MunicipalArchive_{boxId}", "Active", true, false),
                new AssignedProfileRow("Technical Archive", "TechnicalArchive_{boxId}", "Active", false, false),
                new AssignedProfileRow("Court Records", "CourtRecords_{boxId}", "Draft", false, false),
                new AssignedProfileRow("Old Court Records", "OldCourtRecords_{boxId}", "Archived", false, true)
        );
    }

    private void renderAssignedProfiles() {
        List<Node> rows = new ArrayList<>();

        for (int index = 0; index < assignedProfiles.size(); index++) {
            rows.add(buildAssignedProfileRow(
                    assignedProfiles.get(index),
                    index == assignedProfiles.size() - 1
            ));
        }

        metadataProfileList.getChildren().setAll(rows);
    }

    private HBox buildAssignedProfileRow(AssignedProfileRow profile, boolean lastRow) {
        HBox row = new HBox(18);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("metadata-profile-row");

        if (lastRow) {
            row.getStyleClass().add("metadata-profile-row-last");
        }

        if (profile.archived()) {
            row.getStyleClass().add("metadata-profile-row-archived");
        }

        Label checkLabel = new Label(profile.selected() ? "✓" : "");
        checkLabel.getStyleClass().addAll(
                "metadata-profile-check",
                profile.selected() ? "metadata-profile-check-selected" : "metadata-profile-check-empty"
        );

        Label titleLabel = new Label(profile.name());
        titleLabel.getStyleClass().add("metadata-profile-title");

        Label statusLabel = new Label(profile.status());
        statusLabel.getStyleClass().addAll("metadata-profile-status", profileStatusClassFor(profile.status()));

        HBox titleRow = new HBox(12, titleLabel, statusLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label codeLabel = new Label(profile.code());
        codeLabel.getStyleClass().add("metadata-profile-code");

        VBox textBox = new VBox(4, titleRow, codeLabel);

        row.getChildren().addAll(checkLabel, textBox);

        if (!profile.archived()) {
            row.setOnMouseClicked(event -> toggleAssignedProfile(profile));
        }

        return row;
    }

    private void toggleAssignedProfile(AssignedProfileRow profile) {
        int index = assignedProfiles.indexOf(profile);

        if (index < 0) {
            return;
        }

        assignedProfiles.set(index, new AssignedProfileRow(
                profile.name(),
                profile.code(),
                profile.status(),
                !profile.selected(),
                profile.archived()
        ));

        renderAssignedProfiles();
    }

    private String profileStatusClassFor(String status) {
        return switch (normalize(status)) {
            case "active" -> "metadata-profile-status-active";
            case "draft" -> "metadata-profile-status-draft";
            case "archived" -> "metadata-profile-status-archived";
            default -> "metadata-profile-status-archived";
        };
    }

    private void archiveTemplate(MetadataTemplateRow template) {
        updateTemplateStatus(template, "Archived");
    }

    private void restoreTemplate(MetadataTemplateRow template) {
        updateTemplateStatus(template, "Active");
    }

    private void updateTemplateStatus(MetadataTemplateRow template, String newStatus) {
        int index = masterTemplates.indexOf(template);

        if (index < 0) {
            return;
        }

        MetadataTemplateRow updatedTemplate = new MetadataTemplateRow(
                template.name(),
                template.description(),
                template.assignedProfiles(),
                template.fieldCount(),
                newStatus,
                "Today"
        );

        masterTemplates.set(index, updatedTemplate);
        applyFilters();
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

        boolean currentPageButton = text.equals(String.valueOf(currentPage));

        if (currentPageButton) {
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

        previewHeaderButton.setText("Preview Form");
        saveHeaderButton.setText("Save Changes");

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

        if (cancelEditorButton != null) {
            cancelEditorButton.setVisible(true);
            cancelEditorButton.setManaged(true);
        }
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
        showTab(previewTab, null);
    }

    private void showTab(VBox selectedTab, Button selectedButton) {
        for (VBox tab : getTabs()) {
            boolean selected = tab == selectedTab;
            tab.setVisible(selected);
            tab.setManaged(selected);
        }

        for (Button button : getTabButtons()) {
            button.getStyleClass().setAll("metadata-tab-button");
        }

        if (selectedButton != null) {
            selectedButton.getStyleClass().setAll("metadata-tab-button", ACTIVE_TAB_CLASS);
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
                assignedProfilesTabButton
        );
    }

    private boolean isStatus(MetadataTemplateRow template, String status) {
        return template.status().equalsIgnoreCase(status);
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

    private record TemplateFieldRow(
            String name,
            String type,
            boolean required,
            String placeholder
    ) {
    }

    private record AssignedProfileRow(
            String name,
            String code,
            String status,
            boolean selected,
            boolean archived
    ) {
    }
}