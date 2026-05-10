package easv.gui.controller.user;

import easv.be.PageImage;
import easv.be.TiffExportItem;
import easv.be.TiffExportPlan;
import easv.bll.TiffExportManager;
import easv.gui.UserPortalModel;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExportsController {
    private static final String ALL_STATUSES = "All Statuses";
    private static final String SINGLE_PAGE_EXPORT = "Single-page TIFF";
    private static final String MULTI_PAGE_EXPORT = "Multi-page TIFF";
    private static final String INCLUDE_ONE_FILE = "One file";
    private static final String INCLUDE_SELECTED_FILES = "Selected files";
    private static final String INCLUDE_ENTIRE_BOX = "Entire box";

    private final UserPortalModel portalModel;
    private final TiffExportManager tiffExportManager = new TiffExportManager();
    private final VBox page = new VBox(24);
    private final VBox table = new VBox();
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private boolean filtersConfigured;

    private record SelectableExportFile(UserPortalModel.ExportItem exportItem, String documentId, String displayName) {
    }

    public ExportsController(UserPortalModel portalModel) {
        this.portalModel = portalModel;
    }

    public Node create() {
        page.getStyleClass().addAll("portal-page", "exports-page");
        showExportListPage();
        return page;
    }

    private void showExportListPage() {
        page.getChildren().clear();
        Label title = new Label("Exports");
        title.getStyleClass().add("exports-title");

        Label subtitle = new Label("Choose completed files and prepare single-page or multi-page TIFF exports.");
        subtitle.getStyleClass().add("exports-subtitle");

        configureFilters();

        VBox tablePanel = new VBox();
        tablePanel.getStyleClass().add("exports-table-panel");
        table.getStyleClass().add("exports-table");
        tablePanel.getChildren().add(table);

        refreshTable();

        page.getChildren().addAll(
                new VBox(6, title, subtitle),
                buildSummaryRow(),
                buildFilterPanel(),
                tablePanel
        );
    }

    private void configureFilters() {
        if (filtersConfigured) {
            return;
        }

        searchField.setPromptText("Search by file, box, or profile");
        searchField.getStyleClass().add("exports-filter-input");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshTable());

        statusFilter.getItems().setAll(ALL_STATUSES, "Ready", "Processing", "Failed");
        statusFilter.setValue(ALL_STATUSES);
        statusFilter.getStyleClass().add("exports-status-filter");
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshTable());
        filtersConfigured = true;
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(18,
                metricCard("Total Files", String.valueOf(portalModel.fetchExports().size())),
                metricCard("Ready to Download", String.valueOf(countReadyExports())),
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
                buildFilter("Status", statusFilter)
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
        table.getChildren().setAll(createHeaderRow("FILE NAME", "BOX ID", "PROFILE", "CREATED", "SIZE", "STATUS", "ACTION"));

        List<UserPortalModel.ExportItem> visibleItems = portalModel.fetchExports().stream()
                .filter(this::matchesFilters)
                .toList();

        if (visibleItems.isEmpty()) {
            table.getChildren().add(emptyRow("No matching exports"));
            return;
        }

        for (UserPortalModel.ExportItem item : visibleItems) {
            table.getChildren().add(createDataRow(item));
        }
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

        return statusMatches && searchMatches;
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

        Button action = new Button("Export");
        action.getStyleClass().add("portal-row-button");
        action.setGraphic(UserPortalUi.buildIcon("download", "portal-button-icon"));
        action.setDisable(!"Ready".equalsIgnoreCase(item.status()));
        action.setOnAction(event -> showExportDialog(item));

        row.add(primaryCell(item.fileName()), 0, 0);
        row.add(dataCell(item.boxId()), 1, 0);
        row.add(dataCell(item.profileName()), 2, 0);
        row.add(dataCell(item.createdAt()), 3, 0);
        row.add(dataCell(item.size()), 4, 0);
        row.add(statusCell(item.status()), 5, 0);
        row.add(action, 6, 0);

        return row;
    }

    private void showExportDialog(UserPortalModel.ExportItem item) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("TIFF Export");

        ButtonType exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exportButtonType, ButtonType.CANCEL);

        Label title = new Label("TIFF Export");
        title.getStyleClass().add("exports-title");

        Label subtitle = new Label("Export files as separate TIFFs or one combined TIFF.");
        subtitle.getStyleClass().add("exports-subtitle");

        RadioButton singlePageOption = new RadioButton(SINGLE_PAGE_EXPORT);
        RadioButton multiPageOption = new RadioButton(MULTI_PAGE_EXPORT);
        ToggleGroup exportTypeGroup = new ToggleGroup();
        singlePageOption.setToggleGroup(exportTypeGroup);
        multiPageOption.setToggleGroup(exportTypeGroup);
        multiPageOption.setSelected(true);

        HBox exportTypeRow = new HBox(18,
                exportOptionCard(singlePageOption, "Each selected page/file becomes its own .tif file."),
                exportOptionCard(multiPageOption, "All selected pages/files become one combined .tif file.")
        );
        exportTypeRow.setAlignment(Pos.CENTER_LEFT);

        RadioButton oneFileOption = includeOption(INCLUDE_ONE_FILE);
        RadioButton selectedFilesOption = includeOption(INCLUDE_SELECTED_FILES);
        RadioButton entireBoxOption = includeOption(INCLUDE_ENTIRE_BOX);
        ToggleGroup includeGroup = new ToggleGroup();
        oneFileOption.setToggleGroup(includeGroup);
        selectedFilesOption.setToggleGroup(includeGroup);
        entireBoxOption.setToggleGroup(includeGroup);
        selectedFilesOption.setSelected(true);

        HBox includeRow = new HBox(0, oneFileOption, selectedFilesOption, entireBoxOption);
        includeRow.getStyleClass().add("exports-filter-panel");
        includeRow.setAlignment(Pos.CENTER_LEFT);

        Map<SelectableExportFile, CheckBox> fileCheckBoxes = buildFileCheckBoxes(item);

        VBox fileSelectionBox = new VBox(8);
        fileSelectionBox.getStyleClass().add("exports-table-panel");

        VBox previewBox = new VBox(8);
        previewBox.getStyleClass().add("exports-table-panel");

        Label outputLabel = new Label();
        outputLabel.getStyleClass().add("exports-footer-text");

        Label warningLabel = new Label();
        warningLabel.getStyleClass().add("portal-inline-message");
        warningLabel.setWrapText(true);

        VBox content = new VBox(18,
                new VBox(6, title, subtitle),
                sectionTitle("TIFF type"),
                exportTypeRow,
                sectionTitle("Include"),
                includeRow,
                fileSelectionBox,
                previewBox,
                outputLabel,
                warningLabel
        );
        content.setPrefWidth(820);
        dialog.getDialogPane().setContent(content);

        Button exportButton = (Button) dialog.getDialogPane().lookupButton(exportButtonType);
        exportButton.getStyleClass().add("portal-primary-button");

        Runnable refreshPreview = () -> {
            String includeMode = selectedIncludeMode(includeGroup);
            syncFileSelection(item, includeMode, fileCheckBoxes);
            List<SelectableExportFile> selectedFiles = selectedFilesForMode(item, includeMode, fileCheckBoxes);
            List<PageImage> pages = createPagesForFiles(selectedFiles);
            TiffExportPlan plan = createPlan(item, singlePageOption.isSelected(), pages);

            fileSelectionBox.getChildren().setAll(buildFileSelectionContent(includeMode, fileCheckBoxes));
            previewBox.getChildren().setAll(buildSelectedFilesPreview(selectedFiles, item, includeMode, plan));
            outputLabel.setText(outputText(plan, singlePageOption.isSelected()));
            warningLabel.setText(String.join(" ", plan.getWarnings()));
            warningLabel.setVisible(!plan.getWarnings().isEmpty());
            exportButton.setDisable(plan.getPageCount() == 0);
        };

        singlePageOption.setOnAction(event -> refreshPreview.run());
        multiPageOption.setOnAction(event -> refreshPreview.run());
        oneFileOption.setOnAction(event -> refreshPreview.run());
        selectedFilesOption.setOnAction(event -> refreshPreview.run());
        entireBoxOption.setOnAction(event -> refreshPreview.run());
        fileCheckBoxes.values().forEach(checkBox -> checkBox.setOnAction(event -> refreshPreview.run()));

        exportButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String includeMode = selectedIncludeMode(includeGroup);
            List<SelectableExportFile> selectedFiles = selectedFilesForMode(item, includeMode, fileCheckBoxes);
            List<PageImage> pages = createPagesForFiles(selectedFiles);
            TiffExportPlan plan = createPlan(item, singlePageOption.isSelected(), pages);
            outputLabel.setText("Export prepared: " + plan.getFileCount() + " file(s), " + plan.getPageCount() + " page(s).");
            event.consume();
        });

        refreshPreview.run();
        dialog.showAndWait();
    }

    private VBox exportOptionCard(RadioButton option, String descriptionText) {
        Label description = new Label(descriptionText);
        description.getStyleClass().add("exports-table-cell");
        description.setWrapText(true);

        VBox card = new VBox(6, option, description);
        card.getStyleClass().add("exports-table-panel");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private RadioButton includeOption(String text) {
        RadioButton option = new RadioButton(text);
        option.setUserData(text);
        option.getStyleClass().add("exports-table-cell-primary");
        return option;
    }

    private Map<SelectableExportFile, CheckBox> buildFileCheckBoxes(UserPortalModel.ExportItem selectedItem) {
        Map<SelectableExportFile, CheckBox> checkBoxes = new LinkedHashMap<>();

        for (SelectableExportFile file : selectableFilesInSameBox(selectedItem)) {
            CheckBox checkBox = new CheckBox(file.displayName());
            checkBox.getStyleClass().add("exports-table-cell-primary");
            checkBox.setSelected(file.exportItem().equals(selectedItem));
            checkBoxes.put(file, checkBox);
        }

        return checkBoxes;
    }

    private VBox buildFileSelectionContent(String includeMode, Map<SelectableExportFile, CheckBox> fileCheckBoxes) {
        VBox content = new VBox(8);
        content.getChildren().addAll(
                sectionTitle("Choose files"),
                infoLine("Mode", includeMode)
        );

        for (CheckBox checkBox : fileCheckBoxes.values()) {
            content.getChildren().add(checkBox);
        }

        if (fileCheckBoxes.isEmpty()) {
            content.getChildren().add(infoLine("Files", "No ready files available for this box."));
        }

        return content;
    }

    private VBox buildSelectedFilesPreview(List<SelectableExportFile> selectedFiles,
                                           UserPortalModel.ExportItem item,
                                           String includeMode,
                                           TiffExportPlan plan) {
        VBox content = new VBox(8);

        content.getChildren().addAll(
                sectionTitle("Export preview"),
                infoLine("Selection", includeMode),
                infoLine("Box ID", item.boxId()),
                infoLine("Profile", item.profileName()),
                infoLine("Files selected", String.valueOf(selectedFiles.size())),
                infoLine("Pages selected", String.valueOf(plan.getPageCount()))
        );

        for (SelectableExportFile selectedFile : selectedFiles.stream().limit(4).toList()) {
            content.getChildren().add(infoLine("File", selectedFile.displayName()));
        }

        if (selectedFiles.size() > 4) {
            content.getChildren().add(infoLine("More", (selectedFiles.size() - 4) + " more file(s)"));
        }

        if (selectedFiles.isEmpty()) {
            content.getChildren().add(infoLine("File", "Select at least one file to export."));
        }

        content.getChildren().add(sectionTitle("Filename preview"));
        for (TiffExportItem exportItem : plan.getItems()) {
            content.getChildren().add(infoLine("Output", exportItem.getFileName()));
        }

        return content;
    }

    private String outputText(TiffExportPlan plan, boolean singlePage) {
        if (singlePage) {
            return "Output: Creates " + plan.getFileCount() + " separate TIFF file(s).";
        }

        return "Output: Creates " + plan.getFileCount() + " multi-page TIFF file.";
    }

    private TiffExportPlan createPlan(UserPortalModel.ExportItem item, boolean singlePage, List<PageImage> pages) {
        if (singlePage) {
            return tiffExportManager.createSinglePagePlan(item.profileName(), item.boxId(), pages);
        }

        return tiffExportManager.createMultiPagePlan(item.profileName(), item.boxId(), pages);
    }

    private void syncFileSelection(UserPortalModel.ExportItem selectedItem,
                                   String includeMode,
                                   Map<SelectableExportFile, CheckBox> fileCheckBoxes) {
        boolean customSelection = INCLUDE_SELECTED_FILES.equals(includeMode);

        for (Map.Entry<SelectableExportFile, CheckBox> entry : fileCheckBoxes.entrySet()) {
            SelectableExportFile file = entry.getKey();
            CheckBox checkBox = entry.getValue();
            checkBox.setDisable(!customSelection);

            if (INCLUDE_ONE_FILE.equals(includeMode)) {
                checkBox.setSelected(file.equals(firstFileForItem(selectedItem)));
            }

            if (INCLUDE_ENTIRE_BOX.equals(includeMode)) {
                checkBox.setSelected(true);
            }
        }
    }

    private List<SelectableExportFile> selectedFilesForMode(UserPortalModel.ExportItem selectedItem,
                                                            String includeMode,
                                                            Map<SelectableExportFile, CheckBox> fileCheckBoxes) {
        if (INCLUDE_ONE_FILE.equals(includeMode)) {
            return List.of(firstFileForItem(selectedItem));
        }

        if (INCLUDE_ENTIRE_BOX.equals(includeMode)) {
            return selectableFilesInSameBox(selectedItem);
        }

        return fileCheckBoxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<SelectableExportFile> selectableFilesInSameBox(UserPortalModel.ExportItem selectedItem) {
        List<SelectableExportFile> files = new ArrayList<>();

        for (UserPortalModel.ExportItem item : readyItemsInSameBox(selectedItem)) {
            for (String documentId : documentIdsFor(item)) {
                String displayName = documentId + ".pdf";
                files.add(new SelectableExportFile(item, documentId, displayName));
            }
        }

        return files;
    }

    private List<UserPortalModel.ExportItem> readyItemsInSameBox(UserPortalModel.ExportItem selectedItem) {
        return portalModel.fetchExports().stream()
                .filter(item -> item.boxId().equalsIgnoreCase(selectedItem.boxId()))
                .filter(item -> "Ready".equalsIgnoreCase(item.status()))
                .toList();
    }

    private SelectableExportFile firstFileForItem(UserPortalModel.ExportItem item) {
        String documentId = documentIdsFor(item).get(0);
        return new SelectableExportFile(item, documentId, documentId + ".pdf");
    }

    private List<PageImage> createPagesForFiles(List<SelectableExportFile> selectedFiles) {
        List<PageImage> pages = new ArrayList<>();
        for (SelectableExportFile selectedFile : selectedFiles) {
            pages.addAll(createPagesForFile(selectedFile));
        }
        return pages;
    }

    private List<PageImage> createPagesForFile(SelectableExportFile file) {
        List<PageImage> pages = new ArrayList<>();
        UserPortalModel.ExportItem item = file.exportItem();
        List<String> documentIds = documentIdsFor(item);
        int documentIndex = Math.max(0, documentIds.indexOf(file.documentId()));
        int totalPages = Math.max(1, item.pageCount());
        int basePages = totalPages / documentIds.size();
        int extraPages = totalPages % documentIds.size();
        int pagesForDocument = Math.max(1, basePages + (documentIndex < extraPages ? 1 : 0));
        int firstPageNumber = documentIndex * basePages + Math.min(documentIndex, extraPages) + 1;

        for (int offset = 0; offset < pagesForDocument; offset++) {
            pages.add(new PageImage(firstPageNumber + offset, PageImage.PageType.TIFF, file.documentId()));
        }

        return pages;
    }

    private List<String> documentIdsFor(UserPortalModel.ExportItem item) {
        if (!item.documentIds().isEmpty()) {
            return item.documentIds();
        }

        return List.of(item.fileName().replace(".pdf", ""));
    }

    private String selectedIncludeMode(ToggleGroup includeGroup) {
        if (includeGroup.getSelectedToggle() == null) {
            return INCLUDE_SELECTED_FILES;
        }

        return String.valueOf(includeGroup.getSelectedToggle().getUserData());
    }

    private List<PageImage> createPagesForExport(UserPortalModel.ExportItem item) {
        List<PageImage> pages = new ArrayList<>();
        List<String> documentIds = item.documentIds().isEmpty()
                ? List.of(item.fileName().replace(".pdf", ""))
                : item.documentIds();

        int pageCount = Math.max(1, item.pageCount());

        for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
            String documentId = documentIds.get((pageNumber - 1) % documentIds.size());
            pages.add(new PageImage(pageNumber, PageImage.PageType.TIFF, documentId));
        }

        return pages;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("portal-section-title");
        return label;
    }

    private HBox infoLine(String labelText, String valueText) {
        Label label = new Label(labelText + ":");
        label.getStyleClass().add("exports-table-cell-primary");

        Label value = new Label(valueText == null || valueText.isBlank() ? "-" : valueText);
        value.getStyleClass().add("exports-table-cell");
        value.setWrapText(true);
        HBox.setHgrow(value, Priority.ALWAYS);

        HBox row = new HBox(9, label, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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

    private int countReadyExports() {
        return (int) portalModel.fetchExports().stream()
                .filter(item -> "Ready".equalsIgnoreCase(item.status()))
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
        row.getColumnConstraints().setAll(
                percentColumn(24),
                percentColumn(12),
                percentColumn(15),
                percentColumn(18),
                percentColumn(9),
                percentColumn(12),
                percentColumn(10)
        );
        return row;
    }

    private ColumnConstraints percentColumn(double width) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(width);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
    }
}
