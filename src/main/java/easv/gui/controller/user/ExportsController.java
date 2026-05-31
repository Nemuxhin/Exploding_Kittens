package easv.gui.controller.user;

import easv.be.Document;
import easv.be.PageImage;
import easv.be.ScanProfile;
import easv.be.TiffExportPlan;
import easv.bll.ExportService;
import easv.bll.TiffExportManager;
import easv.bll.UserPortalModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExportsController {
    private static final String ALL_STATUSES = "All Statuses";
    private static final DateTimeFormatter ITEM_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserPortalModel portalModel;
    private final VBox table = new VBox();
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final DatePicker fromDateField = new DatePicker();
    private final DatePicker toDateField = new DatePicker();
    private final Label paginationSummaryLabel = new Label();
    private final HBox paginationButtonsBox = new HBox();
    private final ComboBox<Integer> rowsPerPageFilter = new ComboBox<>();
    private final TiffExportManager tiffExportManager = new TiffExportManager();
    private final ExportService exportService = new ExportService();
    private int currentPage = 1;

    public ExportsController(UserPortalModel portalModel) {
        this.portalModel = portalModel;
    }

    public Node create() {
        VBox page = new VBox(24);
        page.getStyleClass().addAll("portal-page", "exports-page");

        Label title = new Label("Exports");
        title.getStyleClass().add("exports-title");

        Label subtitle = new Label("Review previous exports and export approved scans again.");
        subtitle.getStyleClass().add("exports-subtitle");

        configureFilters();

        VBox tablePanel = new VBox();
        tablePanel.getStyleClass().add("exports-table-panel");
        table.getStyleClass().add("exports-table");
        tablePanel.getChildren().addAll(table, buildPaginationBar());

        refreshTable();

        page.getChildren().addAll(
                new VBox(6, title, subtitle),
                buildSummaryRow(),
                buildFilterPanel(),
                tablePanel
        );
        return page;
    }

    private void configureFilters() {
        searchField.setPromptText("Search by file, box, or profile");
        searchField.getStyleClass().add("exports-filter-input");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshTable());

        statusFilter.getItems().setAll(ALL_STATUSES, "Success", "Failed");
        statusFilter.setValue(ALL_STATUSES);
        statusFilter.getStyleClass().add("exports-status-filter");
        statusFilter.setMinWidth(0);
        statusFilter.setPrefWidth(240);
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshTable());

        UserPortalUi.configureDateFilterPicker(fromDateField);
        fromDateField.valueProperty().addListener((observable, oldValue, newValue) -> refreshTable());

        UserPortalUi.configureDateFilterPicker(toDateField);
        toDateField.valueProperty().addListener((observable, oldValue, newValue) -> refreshTable());

        rowsPerPageFilter.getItems().setAll(10, 25, 50);
        rowsPerPageFilter.setValue(10);
        rowsPerPageFilter.getStyleClass().add("exports-status-filter");
        rowsPerPageFilter.getStyleClass().add("pagination-rows-filter");
        rowsPerPageFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentPage = 1;
            refreshTable();
        });
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(18,
                metricCard("Total Files", String.valueOf(portalModel.fetchExports().size())),
                metricCard("Successful Exports", String.valueOf(countSuccessfulExports())),
                metricCard("Total Size", totalExportSizeText())
        );
        row.getStyleClass().add("exports-summary-row");
        return row;
    }

    private VBox metricCard(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("exports-metric-label");

        Label value = new Label(valueText);
        value.getStyleClass().add("exports-metric-value");

        VBox card = new VBox(6, label, value);
        card.getStyleClass().add("exports-metric-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox buildFilterPanel() {
        HBox filterRow = new HBox(18,
                buildFilter("Search", searchField),
                buildFilter("Status", statusFilter),
                buildFilter("From Date", fromDateField),
                buildFilter("To Date", toDateField)
        );
        filterRow.getStyleClass().add("exports-filter-panel");
        filterRow.setAlignment(Pos.CENTER_LEFT);
        return filterRow;
    }

    private VBox buildFilter(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("exports-filter-label");

        VBox wrap = new VBox(6, label, control);
        wrap.getStyleClass().add("exports-filter-wrap");
        HBox.setHgrow(wrap, Priority.ALWAYS);

        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }

        return wrap;
    }

    private void refreshTable() {
        table.getChildren().setAll(createHeaderRow("BOX ID", "PROFILE", "CREATED", "SIZE", "STATUS", "ACTION"));

        List<UserPortalModel.ExportItem> visibleItems = portalModel.fetchExports().stream()
                .filter(this::matchesFilters)
                .toList();

        int rowsPerPage = rowsPerPageFilter.getValue() == null ? 10 : rowsPerPageFilter.getValue();
        int totalItems = visibleItems.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) rowsPerPage));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        if (visibleItems.isEmpty()) {
            table.getChildren().add(emptyRow("No matching exports"));
            updatePagination(totalItems, totalPages);
            return;
        }

        int fromIndex = Math.min((currentPage - 1) * rowsPerPage, totalItems);
        int toIndex = Math.min(fromIndex + rowsPerPage, totalItems);

        for (UserPortalModel.ExportItem item : visibleItems.subList(fromIndex, toIndex)) {
            table.getChildren().add(createDataRow(item));
        }

        updatePagination(totalItems, totalPages);
    }

    private boolean matchesFilters(UserPortalModel.ExportItem item) {
        String selectedStatus = statusFilter.getValue();
        boolean statusMatches = selectedStatus == null
                || ALL_STATUSES.equals(selectedStatus)
                || selectedStatus.equalsIgnoreCase(item.status());

        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        boolean searchMatches = query.isBlank()
                || item.fileName().toLowerCase().contains(query)
                || item.boxId().toLowerCase().contains(query)
                || item.profileName().toLowerCase().contains(query)
                || item.status().toLowerCase().contains(query);

        LocalDate itemDate = parseItemDate(item.createdAt());
        LocalDate fromDate = fromDateField.getValue();
        LocalDate toDate = toDateField.getValue();

        boolean fromMatches = fromDate == null || (itemDate != null && !itemDate.isBefore(fromDate));
        boolean toMatches = toDate == null || (itemDate != null && !itemDate.isAfter(toDate));

        return statusMatches && searchMatches && fromMatches && toMatches;
    }

    private GridPane createHeaderRow(String... values) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-header-row");

        for (int index = 0; index < values.length; index++) {
            row.add(headerCell(values[index]), index, 0);
        }

        return row;
    }

    private GridPane createDataRow(UserPortalModel.ExportItem item) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-row");

        Button action = new Button("Export Again");
        action.getStyleClass().addAll("portal-row-button", "exports-export-button");
        action.setGraphic(UserPortalUi.buildIcon("download", "portal-button-icon-inverse"));
        action.setOnAction(event -> openExportDialog(item));

        HBox actionBox = new HBox(action);
        actionBox.getStyleClass().add("exports-action-box");

        row.add(primaryCell(item.boxId()), 0, 0);
        row.add(dataCell(item.profileName()), 1, 0);
        row.add(dataCell(item.createdAt()), 2, 0);
        row.add(dataCell(item.size()), 3, 0);
        row.add(statusCell(item.status()), 4, 0);
        row.add(actionBox, 5, 0);

        return row;
    }

    private void openExportDialog(UserPortalModel.ExportItem item) {
        Stage stage = new Stage();
        stage.setTitle("TIFF Export");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);

        if (table.getScene() != null) {
            stage.initOwner(table.getScene().getWindow());
        }

        VBox content = buildExportDialogContent(stage, item);
        StackPane root = new StackPane(content);
        root.getStyleClass().addAll("app-shell", "exports-dialog-stage");
        if (isDarkModeEnabled()) {
            root.getStyleClass().add("dark");
        }

        // Separate Stage/Scene, so it does NOT inherit the app's stylesheets.
        // Reuse the owning scene's full list (app.css, tokens, export.css, etc.)
        // so the export dialog is fully styled rather than only app.css.
        Scene scene = new Scene(root);
        if (table.getScene() != null) {
            scene.getStylesheets().setAll(table.getScene().getStylesheets());
        } else {
            URL stylesheetUrl = getClass().getResource("/css/app.css");
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            }
        }

        stage.setScene(scene);
        stage.sizeToScene();
        stage.showAndWait();
    }

    private VBox buildExportDialogContent(Stage stage, UserPortalModel.ExportItem item) {
        List<Document> documents = portalModel.fetchExportDocuments(item);
        List<String> boxFiles = buildExportFiles(documents);
        ObjectProperty<TiffExportType> selectedType = new SimpleObjectProperty<>(TiffExportType.MULTI_PAGE);

        Label title = new Label("TIFF Export");
        title.getStyleClass().add("exports-dialog-title");

        VBox header = new VBox(9, title);
        header.getStyleClass().add("exports-dialog-header");

        Label boxValue = new Label(item.boxId());
        boxValue.getStyleClass().add("exports-dialog-box-value");

        Label boxDetail = new Label("Only documents from this box can be exported in this dialog.");
        boxDetail.getStyleClass().add("exports-dialog-box-detail");

        VBox boxCard = new VBox(6, boxValue, boxDetail);
        boxCard.getStyleClass().add("exports-dialog-box-card");

        Button singlePageCard = buildExportTypeCard(
                "Single-page TIFF",
                "Separate TIFF files",
                TiffExportType.SINGLE_PAGE,
                selectedType
        );
        Button multiPageCard = buildExportTypeCard(
                "Multi-page TIFF",
                "One TIFF per document",
                TiffExportType.MULTI_PAGE,
                selectedType
        );
        HBox.setHgrow(singlePageCard, Priority.ALWAYS);
        HBox.setHgrow(multiPageCard, Priority.ALWAYS);

        HBox typeRow = new HBox(18, singlePageCard, multiPageCard);
        typeRow.getStyleClass().add("exports-dialog-type-row");

        Label selectedFilesTitle = new Label("Documents in box");
        selectedFilesTitle.getStyleClass().add("exports-dialog-files-title");

        Label selectedFilesCount = new Label(formatSelectedDocumentCount(boxFiles.size()));
        selectedFilesCount.getStyleClass().add("exports-dialog-files-count");

        Region filesSpacer = new Region();
        HBox.setHgrow(filesSpacer, Priority.ALWAYS);

        HBox filesHeader = new HBox(18, selectedFilesTitle, filesSpacer, selectedFilesCount);
        filesHeader.setAlignment(Pos.CENTER_LEFT);

        GridPane fileGrid = new GridPane();
        fileGrid.getStyleClass().add("exports-dialog-file-grid");

        ScrollPane fileListScroll = new ScrollPane(fileGrid);
        fileListScroll.getStyleClass().add("exports-dialog-file-scroll");
        fileListScroll.setFitToWidth(true);
        fileListScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fileListScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fileListScroll.setPrefViewportHeight(156);
        renderSelectedFiles(fileGrid, boxFiles);

        VBox filesCard = new VBox(18, filesHeader, fileListScroll);
        filesCard.getStyleClass().add("exports-dialog-files-card");

        Region divider = new Region();
        divider.getStyleClass().add("portal-divider");
        divider.setMaxWidth(Double.MAX_VALUE);

        Label outputLabel = new Label("Output:");
        outputLabel.getStyleClass().add("exports-dialog-output-label");

        Label outputValue = new Label(buildOutputText(selectedType.get(), documents));
        outputValue.getStyleClass().add("exports-dialog-output-value");
        outputValue.setWrapText(false);
        outputValue.setMinHeight(Region.USE_PREF_SIZE);
        outputValue.setPrefWidth(420);
        outputValue.setMaxWidth(420);
        selectedType.addListener((observable, oldValue, newValue) ->
                outputValue.setText(buildOutputText(newValue, documents))
        );

        HBox outputBox = new HBox(9, outputLabel, outputValue);
        outputBox.getStyleClass().add("exports-dialog-output-box");
        outputBox.setAlignment(Pos.CENTER_LEFT);
        outputBox.setMinHeight(36);
        outputBox.setPrefHeight(36);
        outputBox.setMaxHeight(36);

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("portal-secondary-button", "exports-dialog-cancel-button");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> stage.close());

        Button exportButton = new Button("Export");
        exportButton.getStyleClass().addAll("portal-primary-button", "exports-dialog-export-button");
        exportButton.setDefaultButton(true);
        exportButton.setOnAction(event -> handleExport(stage, item, selectedType.get(), documents));

        HBox footerActions = new HBox(9, cancelButton, exportButton);
        footerActions.getStyleClass().add("exports-dialog-footer-actions");
        footerActions.setAlignment(Pos.CENTER_RIGHT);

        VBox footer = new VBox(9, outputBox, footerActions);
        footer.getStyleClass().add("exports-dialog-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setFillWidth(true);

        VBox content = new VBox(18, header, boxCard, typeRow, filesCard, divider, footer);
        content.getStyleClass().add("exports-dialog-content");
        content.setFillWidth(true);
        return content;
    }

    private Button buildExportTypeCard(
            String titleText,
            String subtitleText,
            TiffExportType type,
            ObjectProperty<TiffExportType> selectedType
    ) {
        Label title = new Label(titleText);
        title.getStyleClass().add("exports-dialog-option-title");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("exports-dialog-option-subtitle");

        VBox copy = new VBox(9, title, subtitle);
        copy.getStyleClass().add("exports-dialog-option-copy");

        StackPane checkBadge = new StackPane(UserPortalUi.buildIcon("selected-check", "exports-dialog-option-check-icon"));
        checkBadge.getStyleClass().add("exports-dialog-option-check-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox graphic = new HBox(12, copy, spacer, checkBadge);
        graphic.getStyleClass().add("exports-dialog-option-content");
        graphic.setAlignment(Pos.TOP_LEFT);

        Button button = new Button();
        button.getStyleClass().add("exports-dialog-option-button");
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setGraphic(graphic);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFocusTraversable(false);
        button.setOnAction(event -> selectedType.set(type));

        Runnable refreshSelection = () -> updateExportTypeCard(button, checkBadge, selectedType.get() == type);
        selectedType.addListener((observable, oldValue, newValue) -> refreshSelection.run());
        refreshSelection.run();

        return button;
    }

    private void updateExportTypeCard(Button button, StackPane checkBadge, boolean selected) {
        button.getStyleClass().removeAll(
                "exports-dialog-option-button-selected",
                "exports-dialog-option-button-unselected"
        );
        button.getStyleClass().add(selected
                ? "exports-dialog-option-button-selected"
                : "exports-dialog-option-button-unselected");
        checkBadge.setVisible(selected);
        checkBadge.setManaged(true);
    }

    private void renderSelectedFiles(GridPane fileGrid, List<String> selectedFiles) {
        fileGrid.getChildren().clear();
        fileGrid.getColumnConstraints().setAll(
                percentColumn(33.333),
                percentColumn(33.333),
                percentColumn(33.333)
        );

        if (selectedFiles.isEmpty()) {
            Label emptyState = new Label("No documents available for this export.");
            emptyState.getStyleClass().add("exports-dialog-empty-state");
            fileGrid.add(emptyState, 0, 0, 3, 1);
            return;
        }

        for (int index = 0; index < selectedFiles.size(); index++) {
            int column = index % 3;
            int row = index / 3;
            fileGrid.add(createSelectedFileCell(selectedFiles.get(index), column < 2), column, row);
        }
    }

    private HBox createSelectedFileCell(String fileName, boolean withRightBorder) {
        Label fileLabel = new Label(fileName);
        fileLabel.getStyleClass().add("exports-dialog-file-name");

        HBox cell = new HBox(6, fileLabel);
        cell.getStyleClass().add("exports-dialog-file-cell-box");
        if (withRightBorder) {
            cell.getStyleClass().add("exports-dialog-file-cell-box-bordered");
        }
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private List<String> buildExportFiles(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<String> files = new ArrayList<>(documents.size());
        for (Document document : documents) {
            int pageCount = document.getPages().size();
            files.add(document.getSourceItemId() + " (" + pageCount + " " + pluralize(pageCount, "page") + ")");
        }
        return files;
    }

    private String buildOutputText(TiffExportType type, List<Document> documents) {
        int documentCount = documents == null ? 0 : documents.size();
        if (documentCount == 0) {
            return "No documents are available for export";
        }

        return switch (type) {
            case SINGLE_PAGE -> countExportPages(documents) + " separate .tiff "
                    + pluralize(countExportPages(documents), "file") + " will be generated";
            case MULTI_PAGE -> documentCount + " multi-page .tiff " + pluralize(documentCount, "file")
                    + " will be generated, one per document";
        };
    }

    private String formatSelectedDocumentCount(int count) {
        return count + " " + pluralize(count, "document");
    }

    private int countExportPages(List<Document> documents) {
        if (documents == null) {
            return 0;
        }

        int pageCount = 0;
        for (Document document : documents) {
            pageCount += document.getPages().size();
        }
        return pageCount;
    }

    private void handleExport(
            Stage stage,
            UserPortalModel.ExportItem item,
            TiffExportType exportType,
            List<Document> documents
    ) {
        if (documents == null || documents.isEmpty()) {
            showExportAlert(stage, Alert.AlertType.ERROR, "No documents to export",
                    "No stored document pages were found for this completed scan.");
            return;
        }

        ScanProfile profile = portalModel.fetchExportProfile(item);
        String profileName = item == null ? "" : item.profileName();
        String boxId = item == null ? "" : item.boxId();
        String profileCode = firstNonBlank(profile == null ? null : profile.getCode(), profileName);
        String exportNaming = firstNonBlank(profile == null ? null : profile.getExportNaming(), ScanProfile.DEFAULT_EXPORT_NAMING);

        try {
            TiffExportPlan plan = createExportPlan(exportType, profileName, profileCode, exportNaming, boxId, documents);
            Path outputDirectory = Path.of(
                    System.getProperty("user.home"),
                    "Downloads",
                    "WebLager Exports",
                    safeFolderName(profileName, boxId)
            );
            TiffExportManager.ExportResult result = exportService.exportPlan(
                    item.sessionId(),
                    profileName,
                    boxId,
                    plan,
                    outputDirectory,
                    documents
            );
            showExportAlert(stage, Alert.AlertType.INFORMATION, "Export completed",
                    result.writtenFiles().size() + " TIFF " + pluralize(result.writtenFiles().size(), "file")
                            + " written to " + result.outputDirectory());
            stage.close();
        } catch (IOException | RuntimeException exception) {
            showExportAlert(stage, Alert.AlertType.ERROR, "Export failed", exception.getMessage());
        }
    }

    private TiffExportPlan createExportPlan(
            TiffExportType exportType,
            String profileName,
            String profileCode,
            String exportNaming,
            String boxId,
            List<Document> documents
    ) {
        return exportType == TiffExportType.SINGLE_PAGE
                ? tiffExportManager.createSinglePagePlan(profileName, profileCode, exportNaming, boxId, flattenExportPages(documents))
                : tiffExportManager.createMultiPagePlan(profileName, profileCode, exportNaming, boxId, documents);
    }

    private List<PageImage> flattenExportPages(List<Document> documents) {
        List<PageImage> pages = new ArrayList<>();
        if (documents == null) {
            return pages;
        }

        for (Document document : documents) {
            pages.addAll(document.getPages());
        }
        return pages;
    }

    private void showExportAlert(Stage owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "The export could not be completed." : message);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private String safeFolderName(String profileName, String boxId) {
        return firstNonBlank(profileName, "profile").replaceAll("[^a-zA-Z0-9._-]", "_")
                + "_" + firstNonBlank(boxId, "box").replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred.trim();
    }

    private String pluralize(int count, String singular) {
        return count == 1 ? singular : singular + "s";
    }

    private boolean isDarkModeEnabled() {
        return table.getScene() != null
                && table.getScene().getRoot() != null
                && table.getScene().getRoot().getStyleClass().contains("dark");
    }

    private HBox statusCell(String status) {
        HBox wrap = new HBox(UserPortalUi.buildStatusChip(status));
        wrap.getStyleClass().add("exports-table-cell-wrap");
        return wrap;
    }

    private Label headerCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-table-header");
        return label;
    }

    private Label primaryCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-table-cell-primary");
        return label;
    }

    private Label dataCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-table-cell");
        return label;
    }

    private HBox emptyRow(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-footer-text");

        HBox row = new HBox(label);
        row.getStyleClass().add("exports-empty-row");
        return row;
    }

    private int countSuccessfulExports() {
        return (int) portalModel.fetchExports().stream()
                .filter(item -> "Success".equalsIgnoreCase(item.status()))
                .count();
    }

    private String totalExportSizeText() {
        double totalMb = 0;

        for (UserPortalModel.ExportItem item : portalModel.fetchExports()) {
            String size = item.size();
            if (size == null || !size.endsWith("MB")) {
                continue;
            }

            try {
                totalMb += Double.parseDouble(size.replace("MB", "").trim());
            } catch (NumberFormatException ignored) {
                // Demo data can keep the UI running even if one display value is not numeric.
            }
        }

        return String.format(Locale.US, "%.1f MB", totalMb);
    }

    private GridPane createRowSkeleton() {
        GridPane row = new GridPane();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setHgap(12);
        row.getColumnConstraints().setAll(
                percentColumn(17),
                percentColumn(18),
                percentColumn(21),
                percentColumn(12),
                percentColumn(14),
                percentColumn(18)
        );
        return row;
    }

    private HBox buildPaginationBar() {
        paginationSummaryLabel.getStyleClass().add("pagination-summary-label");
        paginationButtonsBox.getStyleClass().add("pagination-buttons-box");

        Label rowsPerPageLabel = new Label("Results per page");
        rowsPerPageLabel.getStyleClass().add("rows-per-page-label");

        HBox rowsPerPageBox = new HBox(9, rowsPerPageLabel, rowsPerPageFilter);
        rowsPerPageBox.getStyleClass().add("rows-per-page-box");
        HBox.setHgrow(rowsPerPageBox, Priority.ALWAYS);

        HBox summaryBox = new HBox(paginationSummaryLabel);
        summaryBox.getStyleClass().add("pagination-summary-box");
        HBox.setHgrow(summaryBox, Priority.ALWAYS);

        HBox centerBox = new HBox(paginationButtonsBox);
        centerBox.getStyleClass().add("pagination-center-box");
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        HBox bar = new HBox(18, summaryBox, centerBox, rowsPerPageBox);
        bar.getStyleClass().add("pagination-bar");
        return bar;
    }

    private void updatePagination(int totalItems, int totalPages) {
        int rowsPerPage = rowsPerPageFilter.getValue() == null ? 10 : rowsPerPageFilter.getValue();
        int start = totalItems == 0 ? 0 : ((currentPage - 1) * rowsPerPage) + 1;
        int end = totalItems == 0 ? 0 : Math.min(currentPage * rowsPerPage, totalItems);
        paginationSummaryLabel.setText("Showing " + start + "-" + end + " of " + totalItems + " exports");
        paginationButtonsBox.getChildren().setAll(buildPaginationButtons(totalPages));
    }

    private List<Node> buildPaginationButtons(int totalPages) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(paginationButton("«", 1, currentPage == 1, false));
        nodes.add(paginationButton("‹", currentPage - 1, currentPage == 1, false));

        for (int page : visiblePages(totalPages)) {
            if (page < 0) {
                Label ellipsis = new Label("...");
                ellipsis.getStyleClass().add("pagination-ellipsis");
                nodes.add(ellipsis);
            } else {
                nodes.add(paginationButton(String.valueOf(page), page, false, page == currentPage));
            }
        }

        nodes.add(paginationButton("›", currentPage + 1, currentPage == totalPages, false));
        nodes.add(paginationButton("»", totalPages, currentPage == totalPages, false));
        return nodes;
    }

    private List<Integer> visiblePages(int totalPages) {
        List<Integer> pages = new ArrayList<>();
        if (totalPages <= 5) {
            for (int page = 1; page <= totalPages; page++) {
                pages.add(page);
            }
            return pages;
        }

        pages.add(1);
        if (currentPage > 3) {
            pages.add(-1);
        }

        int start = Math.max(2, currentPage - 1);
        int end = Math.min(totalPages - 1, currentPage + 1);
        for (int page = start; page <= end; page++) {
            pages.add(page);
        }

        if (currentPage < totalPages - 2) {
            pages.add(-1);
        }
        pages.add(totalPages);
        return pages;
    }

    private Button paginationButton(String text, int targetPage, boolean disabled, boolean active) {
        Button button = new Button(text);
        button.getStyleClass().add("pagination-button");
        if (active) {
            button.getStyleClass().add("pagination-button-active");
        }
        button.setDisable(disabled);
        button.setOnAction(event -> {
            currentPage = targetPage;
            refreshTable();
        });
        return button;
    }

    private ColumnConstraints percentColumn(double width) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(width);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
    }

    private LocalDate parseItemDate(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return null;
        }

        try {
            return LocalDateTime.parse(value.trim(), ITEM_DATE_TIME).toLocalDate();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private enum TiffExportType {
        SINGLE_PAGE,
        MULTI_PAGE
    }
}
