package easv.gui.controller.admin;

import easv.be.Document;
import easv.be.PageImage;
import easv.be.ScanProfile;
import easv.be.TiffExportPlan;
import easv.bll.AdminManager;
import easv.bll.QAService;
import easv.bll.TiffExportManager;
import easv.util.Strings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminExportsController {
    private static final String ALL_PROFILES = "All Profiles";
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label totalExportsLabel;
    @FXML private Label readyFilesLabel;
    @FXML private Label totalDocumentsLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> profileFilterComboBox;
    @FXML private VBox exportsRowsContainer;

    private final TiffExportManager tiffExportManager = new TiffExportManager();
    private AdminManager adminManager;
    private List<AdminExportItem> exportItems = List.of();

    void setAdminManager(AdminManager adminManager) {
        this.adminManager = adminManager;
        refreshData();
    }

    @FXML
    private void initialize() {
        configureFilters();
        refreshTable();
    }

    private void configureFilters() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshTable());
        }

        if (profileFilterComboBox != null) {
            profileFilterComboBox.getItems().setAll(ALL_PROFILES);
            profileFilterComboBox.setValue(ALL_PROFILES);
            profileFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshTable());
        }
    }

    private void refreshData() {
        if (adminManager == null) {
            exportItems = List.of();
        } else {
            exportItems = adminManager.getApprovedExportsForAdmin().stream()
                    .map(this::toExportItem)
                    .toList();
        }

        refreshProfileFilter();
        refreshSummary();
        refreshTable();
    }

    private void refreshProfileFilter() {
        if (profileFilterComboBox == null) {
            return;
        }

        String selectedProfile = profileFilterComboBox.getValue();
        List<String> profiles = new ArrayList<>();
        profiles.add(ALL_PROFILES);
        exportItems.stream()
                .map(AdminExportItem::profileName)
                .filter(value -> !Strings.clean(value).isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(profiles::add);

        profileFilterComboBox.getItems().setAll(profiles);
        profileFilterComboBox.setValue(profiles.contains(selectedProfile) ? selectedProfile : ALL_PROFILES);
    }

    private void refreshSummary() {
        if (totalExportsLabel != null) {
            totalExportsLabel.setText(String.valueOf(exportItems.size()));
        }

        if (readyFilesLabel != null) {
            readyFilesLabel.setText(String.valueOf(exportItems.stream().mapToInt(AdminExportItem::pageCount).sum()));
        }

        if (totalDocumentsLabel != null) {
            totalDocumentsLabel.setText(String.valueOf(exportItems.stream().mapToInt(AdminExportItem::documentCount).sum()));
        }
    }

    private void refreshTable() {
        if (exportsRowsContainer == null) {
            return;
        }

        exportsRowsContainer.getChildren().setAll(createHeaderRow());
        List<AdminExportItem> visibleItems = exportItems.stream()
                .filter(this::matchesFilters)
                .toList();

        if (visibleItems.isEmpty()) {
            exportsRowsContainer.getChildren().add(emptyRow("No exports are ready."));
            return;
        }

        visibleItems.forEach(item -> exportsRowsContainer.getChildren().add(createDataRow(item)));
    }

    private boolean matchesFilters(AdminExportItem item) {
        String query = Strings.normalize(searchField == null ? "" : searchField.getText());
        boolean searchMatches = query.isBlank()
                || Strings.normalize(item.boxId()).contains(query)
                || Strings.normalize(item.profileName()).contains(query)
                || Strings.normalize(item.scannedBy()).contains(query);

        String selectedProfile = profileFilterComboBox == null ? ALL_PROFILES : profileFilterComboBox.getValue();
        boolean profileMatches = selectedProfile == null
                || ALL_PROFILES.equals(selectedProfile)
                || selectedProfile.equalsIgnoreCase(item.profileName());

        return searchMatches && profileMatches;
    }

    private GridPane createHeaderRow() {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-header-row");

        row.add(headerCell("BOX ID"), 0, 0);
        row.add(headerCell("PROFILE"), 1, 0);
        row.add(headerCell("SCANNED BY"), 2, 0);
        row.add(headerCell("COMPLETED"), 3, 0);
        row.add(headerCell("DOCUMENTS"), 4, 0);
        row.add(headerCell("ACTION"), 5, 0);
        return row;
    }

    private GridPane createDataRow(AdminExportItem item) {
        GridPane row = createRowSkeleton();
        row.getStyleClass().add("exports-table-row");

        Button exportButton = new Button("Export");
        exportButton.getStyleClass().addAll("portal-row-button", "exports-export-button");
        exportButton.setOnAction(event -> openExportDialog(item));

        HBox actionBox = new HBox(exportButton);
        actionBox.getStyleClass().add("exports-action-box");

        row.add(primaryCell(item.boxId()), 0, 0);
        row.add(dataCell(item.profileName()), 1, 0);
        row.add(dataCell(item.scannedBy()), 2, 0);
        row.add(dataCell(item.completedAt()), 3, 0);
        row.add(dataCell(item.documentCount() + " documents"), 4, 0);
        row.add(actionBox, 5, 0);
        return row;
    }

    private void openExportDialog(AdminExportItem item) {
        Stage stage = new Stage();
        stage.setTitle("Admin TIFF Export");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);

        if (exportsRowsContainer.getScene() != null) {
            stage.initOwner(exportsRowsContainer.getScene().getWindow());
        }

        VBox content = buildExportDialogContent(stage, item);
        StackPane root = new StackPane(content);
        root.getStyleClass().addAll("app-shell", "exports-dialog-stage");
        if (isDarkModeEnabled()) {
            root.getStyleClass().add("dark");
        }

        Scene scene = new Scene(root);
        URL stylesheetUrl = getClass().getResource("/css/app.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        }

        stage.setScene(scene);
        stage.sizeToScene();
        stage.showAndWait();
    }

    private VBox buildExportDialogContent(Stage stage, AdminExportItem item) {
        List<Document> documents = toExportDocuments(item.assignment());
        ExportType defaultType = defaultExportType(item.profile());
        ObjectProperty<ExportType> selectedType = new SimpleObjectProperty<>(defaultType);

        Label title = new Label("Admin TIFF Export");
        title.getStyleClass().add("exports-dialog-title");

        Label boxValue = new Label(item.boxId());
        boxValue.getStyleClass().add("exports-dialog-box-value");

        Label boxDetail = new Label(item.profileName() + " scanned by " + item.scannedBy());
        boxDetail.getStyleClass().add("exports-dialog-box-detail");

        VBox boxCard = new VBox(6, boxValue, boxDetail);
        boxCard.getStyleClass().add("exports-dialog-box-card");

        Button singlePageCard = buildExportTypeCard(
                ScanProfile.EXPORT_FORMAT_SINGLE_PAGE_TIFF,
                countExportPages(documents) + " separate TIFF files",
                ExportType.SINGLE_PAGE,
                selectedType
        );
        Button multiPageCard = buildExportTypeCard(
                ScanProfile.EXPORT_FORMAT_MULTI_PAGE_TIFF,
                documents.size() + " TIFF files, one per document",
                ExportType.MULTI_PAGE,
                selectedType
        );
        HBox.setHgrow(singlePageCard, Priority.ALWAYS);
        HBox.setHgrow(multiPageCard, Priority.ALWAYS);

        HBox typeRow = new HBox(18, singlePageCard, multiPageCard);
        typeRow.getStyleClass().add("exports-dialog-type-row");

        GridPane fileGrid = new GridPane();
        fileGrid.getStyleClass().add("exports-dialog-file-grid");
        renderSelectedFiles(fileGrid, buildExportFiles(documents));

        ScrollPane fileListScroll = new ScrollPane(fileGrid);
        fileListScroll.getStyleClass().add("exports-dialog-file-scroll");
        fileListScroll.setFitToWidth(true);
        fileListScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fileListScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fileListScroll.setPrefViewportHeight(156);

        Label selectedFilesTitle = new Label("Documents in export");
        selectedFilesTitle.getStyleClass().add("exports-dialog-files-title");

        Label selectedFilesCount = new Label(documents.size() + " " + pluralize(documents.size(), "document"));
        selectedFilesCount.getStyleClass().add("exports-dialog-files-count");

        Region filesSpacer = new Region();
        HBox.setHgrow(filesSpacer, Priority.ALWAYS);

        HBox filesHeader = new HBox(18, selectedFilesTitle, filesSpacer, selectedFilesCount);
        filesHeader.setAlignment(Pos.CENTER_LEFT);

        VBox filesCard = new VBox(18, filesHeader, fileListScroll);
        filesCard.getStyleClass().add("exports-dialog-files-card");

        Region divider = new Region();
        divider.getStyleClass().add("portal-divider");
        divider.setMaxWidth(Double.MAX_VALUE);

        Label outputLabel = new Label("Output:");
        outputLabel.getStyleClass().add("exports-dialog-output-label");

        Label outputValue = new Label(buildOutputText(selectedType.get(), documents));
        outputValue.getStyleClass().add("exports-dialog-output-value");
        selectedType.addListener((observable, oldValue, newValue) ->
                outputValue.setText(buildOutputText(newValue, documents)));

        HBox outputBox = new HBox(9, outputLabel, outputValue);
        outputBox.getStyleClass().add("exports-dialog-output-box");
        outputBox.setAlignment(Pos.CENTER_LEFT);

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

        VBox content = new VBox(18, new VBox(9, title), boxCard, typeRow, filesCard, divider, footer);
        content.getStyleClass().add("exports-dialog-content");
        return content;
    }

    private Button buildExportTypeCard(
            String titleText,
            String subtitleText,
            ExportType type,
            ObjectProperty<ExportType> selectedType
    ) {
        Label title = new Label(titleText);
        title.getStyleClass().add("exports-dialog-option-title");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("exports-dialog-option-subtitle");

        VBox copy = new VBox(9, title, subtitle);
        copy.getStyleClass().add("exports-dialog-option-copy");

        StackPane checkBadge = new StackPane();
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

    private void handleExport(Stage stage, AdminExportItem item, ExportType exportType, List<Document> documents) {
        if (documents.isEmpty()) {
            showExportAlert(stage, Alert.AlertType.ERROR, "No documents to export",
                    "No approved document pages were found for this export.");
            return;
        }

        try {
            String profileCode = firstNonBlank(item.profile() == null ? "" : item.profile().getCode(), item.profileName());
            String exportNaming = firstNonBlank(
                    item.profile() == null ? "" : item.profile().getExportNaming(),
                    ScanProfile.DEFAULT_EXPORT_NAMING
            );
            TiffExportPlan plan = exportType == ExportType.SINGLE_PAGE
                    ? tiffExportManager.createSinglePagePlan(
                            item.profileName(),
                            profileCode,
                            exportNaming,
                            item.boxId(),
                            flattenExportPages(documents)
                    )
                    : tiffExportManager.createMultiPagePlan(
                            item.profileName(),
                            profileCode,
                            exportNaming,
                            item.boxId(),
                            documents
                    );
            Path outputDirectory = Path.of(
                    System.getProperty("user.home"),
                    "Downloads",
                    "WebLager Admin Exports",
                    safeFolderName(item.profileName(), item.boxId())
            );
            TiffExportManager.ExportResult result = tiffExportManager.exportPlan(plan, outputDirectory);
            showExportAlert(stage, Alert.AlertType.INFORMATION, "Export completed",
                    result.writtenFiles().size() + " TIFF " + pluralize(result.writtenFiles().size(), "file")
                            + " written to " + result.outputDirectory());
            stage.close();
        } catch (IOException | RuntimeException exception) {
            showExportAlert(stage, Alert.AlertType.ERROR, "Export failed", exception.getMessage());
        }
    }

    private AdminExportItem toExportItem(QAService.QaAssignmentSnapshot assignment) {
        ScanProfile profile = findProfileByName(assignment.profileName());
        int documentCount = assignment.documents().size();
        int pageCount = assignment.documents().stream()
                .mapToInt(document -> document.pages().size())
                .sum();

        return new AdminExportItem(
                assignment,
                profile,
                assignment.boxId(),
                assignment.profileName(),
                Strings.displayText(assignment.scannedByName(), "Unknown"),
                formatInstant(assignment.completedAt() == null ? assignment.submittedAt() : assignment.completedAt()),
                documentCount,
                pageCount
        );
    }

    private List<Document> toExportDocuments(QAService.QaAssignmentSnapshot assignment) {
        List<Document> documents = new ArrayList<>();
        if (assignment == null) {
            return documents;
        }

        for (QAService.QaDocumentSnapshot qaDocument : assignment.documents()) {
            List<PageImage> pages = new ArrayList<>();
            for (QAService.QaPageSnapshot qaPage : qaDocument.pages()) {
                if (qaPage.reviewStatus() == QAService.QaPageReviewStatus.NEEDS_FIX) {
                    continue;
                }
                PageImage pageImage = new PageImage(
                        qaPage.pageNumber(),
                        PageImage.PageType.TIFF,
                        firstNonBlank(qaPage.sourceReference(), "Document " + qaDocument.number())
                );
                pageImage.setRotationDegrees(qaPage.rotationDegrees());
                pageImage.setDisplayContent(qaPage.displayContent());
                pages.add(pageImage);
            }
            if (!pages.isEmpty()) {
                documents.add(new Document("document_" + String.format(Locale.US, "%03d", qaDocument.number()), pages));
            }
        }

        return documents;
    }

    private ScanProfile findProfileByName(String profileName) {
        if (adminManager == null || Strings.clean(profileName).isBlank()) {
            return null;
        }

        return adminManager.getProfiles().stream()
                .filter(profile -> profile.getName().equalsIgnoreCase(profileName))
                .findFirst()
                .orElse(null);
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

    private String buildOutputText(ExportType type, List<Document> documents) {
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

    private List<PageImage> flattenExportPages(List<Document> documents) {
        List<PageImage> pages = new ArrayList<>();
        for (Document document : documents) {
            pages.addAll(document.getPages());
        }
        return pages;
    }

    private ExportType defaultExportType(ScanProfile profile) {
        if (profile == null) {
            return ExportType.MULTI_PAGE;
        }

        return ScanProfile.EXPORT_FORMAT_SINGLE_PAGE_TIFF.equalsIgnoreCase(ScanProfile.normalizeExportFormat(profile.getExportFormat()))
                ? ExportType.SINGLE_PAGE
                : ExportType.MULTI_PAGE;
    }

    private GridPane createRowSkeleton() {
        GridPane row = new GridPane();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setHgap(12);
        row.getColumnConstraints().setAll(
                percentColumn(16),
                percentColumn(18),
                percentColumn(18),
                percentColumn(18),
                percentColumn(13),
                percentColumn(17)
        );
        return row;
    }

    private ColumnConstraints percentColumn(double width) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(width);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
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

    private int countExportPages(List<Document> documents) {
        if (documents == null) {
            return 0;
        }

        return documents.stream()
                .mapToInt(document -> document.getPages().size())
                .sum();
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

    private boolean isDarkModeEnabled() {
        return exportsRowsContainer.getScene() != null
                && exportsRowsContainer.getScene().getRoot() != null
                && exportsRowsContainer.getScene().getRoot().getStyleClass().contains("dark");
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "-" : EXPORT_TIME_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
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

    private enum ExportType {
        SINGLE_PAGE,
        MULTI_PAGE
    }

    private record AdminExportItem(
            QAService.QaAssignmentSnapshot assignment,
            ScanProfile profile,
            String boxId,
            String profileName,
            String scannedBy,
            String completedAt,
            int documentCount,
            int pageCount
    ) {
    }
}
