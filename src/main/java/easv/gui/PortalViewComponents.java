package easv.gui;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

public class PortalViewComponents {

    public ImageView buildSidebarLogo(boolean darkMode) {
        String resourcePath = darkMode
                ? "/images/weblager/styleguide/White/LogoWhiteH.png"
                : "/images/weblager/styleguide/Main Blue/LogoBlueH.png";
        return buildLogo(resourcePath, 138, "sidebar-logo");
    }

    public ImageView buildQaHeaderLogo() {
        return buildLogo("/images/weblager/styleguide/Main Blue/LogoBlue_Logoicon.png", 28, "qa-brand-logo");
    }

    public StackPane buildProgressRing(double percent, String labelText) {
        double radius = 30;
        double circumference = 2 * Math.PI * radius;
        Circle track = new Circle(30);
        track.getStyleClass().add("scan-progress-ring-track");

        Circle progress = new Circle(radius);
        progress.getStyleClass().add("scan-progress-ring-fill");
        progress.getStrokeDashArray().setAll(circumference);
        progress.setStrokeDashOffset(circumference * (1 - Math.max(0, Math.min(100, percent)) / 100.0));
        progress.setRotate(-90);

        Label label = new Label(labelText);
        label.getStyleClass().add("scan-progress-percent");

        StackPane ring = new StackPane(track, progress, label);
        ring.getStyleClass().add("scan-progress-ring");
        return ring;
    }

    public StackPane buildSuccessBadge() {
        StackPane badge = new StackPane();
        badge.getStyleClass().add("scan-success-badge");
        SVGPath check = new SVGPath();
        check.setContent("M7 12L10.5 15.5L17 8");
        check.getStyleClass().add("scan-success-check");
        badge.getChildren().add(check);
        return badge;
    }

    public HBox scanProgressStep(String text, String state) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(scanStepIcon(state), scanStepLabel(text, state));
        return row;
    }

    public Label scanInfoLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("scan-flow-info-label");
        return label;
    }

    public Label scanInfoValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("scan-flow-info-value");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_RIGHT);
        return label;
    }

    public VBox buildPaperPreview() {
        Label corner = new Label("");
        corner.getStyleClass().add("paper-corner");
        Label lineOne = new Label("");
        lineOne.getStyleClass().add("paper-line");
        Label lineTwo = new Label("");
        lineTwo.getStyleClass().add("paper-line");
        Label lineThree = new Label("");
        lineThree.getStyleClass().add("paper-line-short");

        VBox preview = new VBox(7, corner, lineOne, lineTwo, lineThree);
        preview.getStyleClass().add("paper-preview");
        return preview;
    }

    public VBox buildLargePaperPreview() {
        Label corner = new Label("");
        corner.getStyleClass().add("paper-corner");
        Label lineOne = new Label("");
        lineOne.getStyleClass().add("paper-line");
        Label lineTwo = new Label("");
        lineTwo.getStyleClass().add("paper-line");
        Label lineThree = new Label("");
        lineThree.getStyleClass().add("paper-line");
        Label lineFour = new Label("");
        lineFour.getStyleClass().add("paper-line-short");
        Label lineFive = new Label("");
        lineFive.getStyleClass().add("paper-line");

        VBox preview = new VBox(6, corner, lineOne, lineTwo, lineThree, lineFour, lineFive);
        preview.getStyleClass().add("scan-large-paper-preview");
        return preview;
    }

    public VBox infoBlock(String labelText, String valueText) {
        VBox block = new VBox(4);
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("field-value");
        block.getChildren().addAll(label, value);
        return block;
    }

    public ColumnConstraints column(double width) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPrefWidth(width);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
    }

    public Label headerCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-header-cell");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    public Label bodyCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-body-cell");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    public HBox statusCell(String status) {
        Label pill = new Label(status);
        pill.getStyleClass().add("status-pill");
        if ("Completed".equals(status)) {
            pill.getStyleClass().add("status-completed");
        } else if ("Processing".equals(status)) {
            pill.getStyleClass().add("status-processing");
        } else {
            pill.getStyleClass().add("status-failed");
        }

        HBox wrap = new HBox(pill);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.getStyleClass().add("table-row-wrap");
        return wrap;
    }

    public HBox exportStatusCell(String status) {
        Label pill = new Label(status);
        pill.getStyleClass().add("status-pill");
        if ("Ready".equals(status)) {
            pill.getStyleClass().add("status-ready");
        } else {
            pill.getStyleClass().add("status-processing");
        }

        HBox wrap = new HBox(pill);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.getStyleClass().add("table-row-wrap");
        return wrap;
    }

    public HBox exportActionCell(UserPortalModel.ExportRow export) {
        HBox wrap = new HBox(6);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.getStyleClass().add("table-row-wrap");
        if (!"Ready".equals(export.status())) {
            Label pending = new Label("Pending");
            pending.getStyleClass().add("export-pending-text");
            wrap.getChildren().add(pending);
            return wrap;
        }

        SVGPath downloadIcon = new SVGPath();
        downloadIcon.setContent("M12 3V15 M7 10L12 15L17 10 M5 21H19");
        downloadIcon.getStyleClass().add("export-download-icon");
        Label download = new Label("Download");
        download.getStyleClass().add("export-download-text");
        wrap.getStyleClass().add("export-download-action");
        wrap.getChildren().addAll(downloadIcon, download);
        return wrap;
    }

    public Label qaStatusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("document-status-pill");
        if ("QA Completed".equals(status)) {
            badge.getStyleClass().add("document-status-completed");
        } else if ("In Progress".equals(status)) {
            badge.getStyleClass().add("document-status-progress");
        } else {
            badge.getStyleClass().add("document-status-waiting");
        }
        return badge;
    }

    public StackPane buildActionIconBadge(UserPortalModel.ActionCard card) {
        StackPane badge = new StackPane();
        badge.getStyleClass().add(card.accent() ? "dashboard-card-badge-accent" : "dashboard-card-badge");

        SVGPath icon = new SVGPath();
        icon.getStyleClass().add(card.accent() ? "dashboard-card-icon-accent" : "dashboard-card-icon");
        icon.setContent(actionIconPath(card.title()));

        badge.getChildren().add(icon);
        return badge;
    }

    private StackPane scanStepIcon(String state) {
        StackPane wrap = new StackPane();
        wrap.getStyleClass().add("scan-step-icon-" + state);
        if ("complete".equals(state)) {
            SVGPath check = new SVGPath();
            check.setContent("M6 12L10 16L18 8");
            check.getStyleClass().add("scan-step-check");
            wrap.getChildren().add(check);
        } else if ("active".equals(state)) {
            SVGPath spinner = new SVGPath();
            spinner.setContent("M19 12A7 7 0 1 1 12 5");
            spinner.getStyleClass().add("scan-step-spinner");
            wrap.getChildren().add(spinner);
        } else {
            Circle dot = new Circle(4);
            dot.getStyleClass().add("scan-step-dot");
            wrap.getChildren().add(dot);
        }
        return wrap;
    }

    private Label scanStepLabel(String text, String state) {
        Label label = new Label(text);
        label.getStyleClass().add("pending".equals(state) ? "scan-step-text-muted" : "scan-step-text");
        return label;
    }

    private String actionIconPath(String title) {
        return switch (title) {
            case "Start Scan" -> "M7 3H5A2 2 0 0 0 3 5V7 M17 3H19A2 2 0 0 1 21 5V7 M21 17V19A2 2 0 0 1 19 21H17 M7 21H5A2 2 0 0 1 3 19V17 M8 12H16";
            case "My Scans" -> "M14 2H6A2 2 0 0 0 4 4V20A2 2 0 0 0 6 22H18A2 2 0 0 0 20 20V8L14 2Z M14 2V8H20 M8 13H16 M8 17H14";
            case "Exports" -> "M21 8V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V8 M1 3H23V8H1Z M10 12H14";
            default -> "M12 5V19 M5 12H19";
        };
    }

    private ImageView buildLogo(String resourcePath, double fitWidth, String styleClass) {
        var resource = getClass().getResource(resourcePath);
        ImageView logo = resource == null ? new ImageView() : new ImageView(new Image(resource.toExternalForm()));
        logo.setPreserveRatio(true);
        logo.setFitWidth(fitWidth);
        logo.getStyleClass().add(styleClass);
        return logo;
    }
}
