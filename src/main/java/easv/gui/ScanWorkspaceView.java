package easv.gui;

import easv.be.AuditLog;
import easv.be.CaseMetadata;
import easv.be.PageImage;
import easv.be.TiffExportItem;
import easv.be.TiffExportPlan;
import easv.bll.AuditLogManager;
import easv.bll.MetadataManager;
import easv.bll.ScanManager;
import easv.bll.TiffExportManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small scan workspace used to demonstrate the sprint tasks.
 * The business logic is still in BLL classes, so this view can be replaced later.
 */
public class ScanWorkspaceView {

    private final AuditLogManager auditLogManager = new AuditLogManager();
    private final MetadataManager metadataManager = new MetadataManager(new easv.dal.MetadataDAO(), auditLogManager);
    private final ScanManager scanManager = new ScanManager(auditLogManager);
    private final TiffExportManager tiffExportManager = new TiffExportManager();

    private int nextPageNumber = 1;

    public Parent createView(MainApp mainApp) {
        TextField caseIdField = new TextField("CASE-001");
        TextField profileNameField = new TextField("Profile-A");
        TextField boxIdField = new TextField("BOX-001");
        TextField documentIdField = new TextField("DOC-001");
        TextField fileIdField = new TextField("FILE-001");
        TextArea notesArea = new TextArea();
        TextArea outputArea = new TextArea();

        notesArea.setPromptText("Enter metadata notes after scanning.");
        notesArea.setPrefRowCount(4);
        outputArea.setEditable(false);
        outputArea.setPrefRowCount(12);

        Button startScanButton = new Button("Start Scan");
        Button addPageButton = new Button("Add TIFF Page");
        Button deletePageButton = new Button("Delete Page");
        Button completeScanButton = new Button("Complete Scan");
        Button saveMetadataButton = new Button("Save Metadata");
        Button loadMetadataButton = new Button("Load Metadata");
        Button singleExportButton = new Button("Preview Single-Page TIFFs");
        Button multiExportButton = new Button("Preview Multi-Page TIFF");
        Button backButton = new Button("Back");

        startScanButton.setOnAction(event -> {
            scanManager.startScan(caseIdField.getText(), profileNameField.getText(), boxIdField.getText());
            showOutput(outputArea, "Scan started.");
        });

        addPageButton.setOnAction(event -> {
            PageImage page = scanManager.addPage(caseIdField.getText(), documentIdField.getText(),
                    fileIdField.getText(), nextPageNumber, fileIdField.getText() + ".tiff",
                    profileNameField.getText(), boxIdField.getText());
            nextPageNumber++;
            showOutput(outputArea, "Added page " + page.getPageNumber() + " for document " + page.getDocumentId() + ".");
        });

        deletePageButton.setOnAction(event -> {
            boolean deleted = scanManager.deletePage(caseIdField.getText(), fileIdField.getText(),
                    nextPageNumber - 1, profileNameField.getText(), boxIdField.getText());
            showOutput(outputArea, deleted ? "Latest page deleted." : "No active page found to delete.");
        });

        completeScanButton.setOnAction(event -> {
            scanManager.completeScan(caseIdField.getText(), profileNameField.getText(), boxIdField.getText());
            showOutput(outputArea, "Scan completed.");
        });

        saveMetadataButton.setOnAction(event -> {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("Notes", notesArea.getText());

            boolean saved = metadataManager.saveMetadata(caseIdField.getText(), profileNameField.getText(),
                    boxIdField.getText(), values);
            showOutput(outputArea, saved ? "Metadata saved." : "Metadata is locked for this case.");
        });

        loadMetadataButton.setOnAction(event -> {
            CaseMetadata metadata = metadataManager.loadMetadataForm(caseIdField.getText());

            if (metadata == null) {
                showOutput(outputArea, "No metadata saved for this case yet.");
                return;
            }

            notesArea.setText(metadata.getValues().getOrDefault("Notes", ""));
            showOutput(outputArea, "Saved metadata loaded for case " + metadata.getCaseId() + ".");
        });

        singleExportButton.setOnAction(event -> showExportPreview(outputArea,
                tiffExportManager.createSinglePagePlan(profileNameField.getText(), boxIdField.getText(),
                        scanManager.getActivePages(caseIdField.getText()))));

        multiExportButton.setOnAction(event -> showExportPreview(outputArea,
                tiffExportManager.createMultiPagePlan(profileNameField.getText(), boxIdField.getText(),
                        scanManager.getActivePages(caseIdField.getText()))));

        backButton.setOnAction(event -> mainApp.showMainView());

        VBox layout = new VBox(16,
                new Label("Scan Workspace"),
                createForm(caseIdField, profileNameField, boxIdField, documentIdField, fileIdField),
                new HBox(10, startScanButton, addPageButton, deletePageButton, completeScanButton),
                new Label("Metadata after scanning"),
                notesArea,
                new HBox(10, saveMetadataButton, loadMetadataButton),
                new HBox(10, singleExportButton, multiExportButton),
                outputArea,
                backButton
        );

        layout.setAlignment(Pos.TOP_LEFT);
        layout.setPadding(new Insets(30));
        layout.getStyleClass().add("main-panel");

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private GridPane createForm(TextField caseIdField, TextField profileNameField, TextField boxIdField,
                                TextField documentIdField, TextField fileIdField) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        gridPane.addRow(0, new Label("Case ID"), caseIdField);
        gridPane.addRow(1, new Label("Profile name"), profileNameField);
        gridPane.addRow(2, new Label("Box ID"), boxIdField);
        gridPane.addRow(3, new Label("Document ID"), documentIdField);
        gridPane.addRow(4, new Label("File ID"), fileIdField);

        return gridPane;
    }

    private void showExportPreview(TextArea outputArea, TiffExportPlan exportPlan) {
        StringBuilder text = new StringBuilder();
        text.append("Export type: ").append(exportPlan.getExportType()).append(System.lineSeparator());
        text.append("Files to export: ").append(exportPlan.getFileCount()).append(System.lineSeparator());
        text.append("Pages to export: ").append(exportPlan.getPageCount()).append(System.lineSeparator());

        for (String warning : exportPlan.getWarnings()) {
            text.append("Warning: ").append(warning).append(System.lineSeparator());
        }

        text.append(System.lineSeparator()).append("Filename preview:").append(System.lineSeparator());

        for (TiffExportItem item : exportPlan.getItems()) {
            text.append("- Document ").append(item.getDocumentId()).append(": ")
                    .append(item.getFileName()).append(System.lineSeparator());
        }

        outputArea.setText(text.toString());
        auditLogManager.logUserAction(AuditLogManager.EXPORT_PREVIEW_CREATED, null, null, null,
                null, null, null, "TIFF export preview was created.");
    }

    private void showOutput(TextArea outputArea, String message) {
        StringBuilder text = new StringBuilder(message).append(System.lineSeparator()).append(System.lineSeparator());
        text.append("Audit log:").append(System.lineSeparator());

        for (AuditLog auditLog : auditLogManager.getLogs()) {
            text.append(auditLog.getTimestamp())
                    .append(" | ")
                    .append(auditLog.isSystemAction() ? "SYSTEM" : auditLog.getUsername())
                    .append(" | ")
                    .append(auditLog.getAction())
                    .append(" | case=")
                    .append(auditLog.getCaseId())
                    .append(" | document=")
                    .append(auditLog.getDocumentId())
                    .append(" | file=")
                    .append(auditLog.getFileId())
                    .append(" | page=")
                    .append(auditLog.getPageNumber())
                    .append(System.lineSeparator());
        }

        outputArea.setText(text.toString());
    }
}
