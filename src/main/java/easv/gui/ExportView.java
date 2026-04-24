package easv.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ExportView {
    private final UserPortalModel portalModel;
    private final UserPortalModel.PortalSession session;
    private final Runnable onDashboard;
    private final Runnable onBackToQa;
    private UserPortalModel.ExportMode selectedMode = UserPortalModel.ExportMode.MULTI_PAGE;

    public ExportView(UserPortalModel portalModel, UserPortalModel.PortalSession session, Runnable onDashboard, Runnable onBackToQa) {
        this.portalModel = portalModel;
        this.session = session;
        this.onDashboard = onDashboard;
        this.onBackToQa = onBackToQa;
    }

    public Parent create() {
        Label title = new Label("Export Documents");
        title.getStyleClass().add("export-title");
        Label subtitle = new Label("Choose export format for your documents");
        subtitle.getStyleClass().add("export-subtitle");
        Button backToQaButton = new Button("Back To QA");
        backToQaButton.getStyleClass().add("back-button");
        backToQaButton.setOnAction(event -> onBackToQa.run());
        Button dashboardButton = new Button("Back To Dashboard");
        dashboardButton.getStyleClass().add("back-button");
        dashboardButton.setOnAction(event -> onDashboard.run());
        Button closeButton = new Button("X");
        closeButton.getStyleClass().add("export-close-button");
        closeButton.setOnAction(event -> onDashboard.run());

        HBox header = new HBox(16, new VBox(8, title, subtitle), backToQaButton, dashboardButton, closeButton);
        header.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);
        header.getStyleClass().add("export-header");

        Label intro = new Label("All exports will be in TIFF format only. Select your preferred export type:");
        intro.getStyleClass().add("export-intro");

        ToggleGroup exportModeGroup = new ToggleGroup();
        RadioButton multiRadio = new RadioButton();
        RadioButton singleRadio = new RadioButton();
        multiRadio.setToggleGroup(exportModeGroup);
        singleRadio.setToggleGroup(exportModeGroup);
        multiRadio.setSelected(true);

        VBox multiOption = buildOptionCard(
                multiRadio,
                "Multi-page TIFF",
                "One document is saved as one TIFF file containing all its pages. This is ideal for keeping documents together as a single file.",
                "Example: \"" + session.getExportLabel() + "_Document.tiff\" (contains all pages of the document)",
                true
        );
        VBox singleOption = buildOptionCard(
                singleRadio,
                "Single-page TIFF",
                "Each page/file is saved as a separate TIFF file. This is useful when you need individual access to each page.",
                "Example: \"" + session.getExportLabel() + "_Page1.tiff\", \"" + session.getExportLabel() + "_Page2.tiff\", \"" + session.getExportLabel() + "_Page3.tiff\"",
                false
        );

        exportModeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            selectedMode = newToggle == singleRadio ? UserPortalModel.ExportMode.SINGLE_PAGE : UserPortalModel.ExportMode.MULTI_PAGE;
            multiOption.getStyleClass().setAll("export-option-card", selectedMode == UserPortalModel.ExportMode.MULTI_PAGE ? "export-option-selected" : "export-option-plain");
            singleOption.getStyleClass().setAll("export-option-card", selectedMode == UserPortalModel.ExportMode.SINGLE_PAGE ? "export-option-selected" : "export-option-plain");
        });

        Label ready = new Label("Ready to export: " + session.getAllFiles().size()
                + " assigned file(s) across " + session.getDocuments().size() + " document(s)");
        ready.getStyleClass().add("export-ready");

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("export-cancel-button");
        cancelButton.setOnAction(event -> onBackToQa.run());

        Button dashboardFooterButton = new Button("Dashboard");
        dashboardFooterButton.getStyleClass().add("export-cancel-button");
        dashboardFooterButton.setOnAction(event -> onDashboard.run());

        Button exportButton = new Button("Export as TIFF");
        exportButton.getStyleClass().add("export-submit-button");
        exportButton.setOnAction(event -> {
            portalModel.previewExportFiles(session, selectedMode);
            onDashboard.run();
        });

        HBox footer = new HBox(20, cancelButton, dashboardFooterButton, exportButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("export-footer");

        VBox body = new VBox(18, intro, multiOption, singleOption, ready);
        body.getStyleClass().add("export-body");

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.getStyleClass().add("transparent-scroll");

        BorderPane modal = new BorderPane();
        modal.getStyleClass().add("export-modal");
        modal.setTop(header);
        modal.setCenter(bodyScroll);
        modal.setBottom(footer);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("export-page");
        root.setCenter(modal);
        BorderPane.setMargin(modal, new Insets(20));

        return root;
    }

    private VBox buildOptionCard(RadioButton radioButton, String title, String description, String example, boolean selected) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("export-option-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("export-option-description");
        descriptionLabel.setWrapText(true);
        Label exampleLabel = new Label(example);
        exampleLabel.getStyleClass().add("export-example");
        exampleLabel.setWrapText(true);

        HBox titleRow = new HBox(24, radioButton, titleLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(16, titleRow, descriptionLabel, exampleLabel);
        VBox card = new VBox(text);
        card.getStyleClass().addAll("export-option-card", selected ? "export-option-selected" : "export-option-plain");
        card.setOnMouseClicked(event -> radioButton.setSelected(true));
        return card;
    }
}
