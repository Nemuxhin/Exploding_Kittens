package easv.gui.controller.Admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActivityController {

    private static final String ALL_TYPES = "All Types";
    private static final String ALL_USERS = "All Users";
    private static final String ALL_STATUSES = "All Statuses";

    private final ObservableList<ActivityLogEntry> activityEntries = FXCollections.observableArrayList();

    private String expandedEntryId;

    @FXML private TextField searchField;

    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> userFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> dateRangeFilterComboBox;

    @FXML private Label todayEventsValueLabel;
    @FXML private Label scansCompletedValueLabel;
    @FXML private Label qaCompletedValueLabel;
    @FXML private Label failedEventsValueLabel;

    @FXML private VBox timelineContainer;
    @FXML private VBox emptyStateBox;

    @FXML
    private void initialize() {
        configureFilters();
        loadSampleActivity();
        configureListeners();
        updateSummaryCards();
        renderTimeline();
    }

    private void configureFilters() {
        typeFilterComboBox.getItems().setAll(
                ALL_TYPES,
                "Users",
                "Profiles",
                "Access",
                "Metadata",
                "Scans",
                "Documents",
                "QA",
                "Exports",
                "Security",
                "System"
        );
        typeFilterComboBox.setValue(ALL_TYPES);

        userFilterComboBox.getItems().setAll(
                ALL_USERS,
                "Admin",
                "Sarah Smith",
                "John Doe",
                "System",
                "Michael Johnson",
                "Sofia Nielsen"
        );
        userFilterComboBox.setValue(ALL_USERS);

        statusFilterComboBox.getItems().setAll(
                ALL_STATUSES,
                "Success",
                "Warning",
                "Failed",
                "Info"
        );
        statusFilterComboBox.setValue(ALL_STATUSES);

        dateRangeFilterComboBox.getItems().setAll(
                "Last 7 Days",
                "Today",
                "Yesterday",
                "All Dates"
        );
        dateRangeFilterComboBox.setValue("Last 7 Days");
    }

    private void configureListeners() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> renderTimeline());

        typeFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderTimeline());
        userFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderTimeline());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderTimeline());
        dateRangeFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderTimeline());
    }

    private void renderTimeline() {
        List<ActivityLogEntry> filteredEntries = activityEntries.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesFilters)
                .toList();

        timelineContainer.getChildren().clear();

        boolean hasEntries = !filteredEntries.isEmpty();

        timelineContainer.setVisible(hasEntries);
        timelineContainer.setManaged(hasEntries);

        emptyStateBox.setVisible(!hasEntries);
        emptyStateBox.setManaged(!hasEntries);

        if (!hasEntries) {
            return;
        }

        Map<String, List<ActivityLogEntry>> groups = groupByDate(filteredEntries);

        groups.forEach((dateGroup, entries) -> {
            timelineContainer.getChildren().add(createDateHeader(dateGroup));

            for (ActivityLogEntry entry : entries) {
                timelineContainer.getChildren().add(createActivityRow(entry));
            }
        });
    }

    private Map<String, List<ActivityLogEntry>> groupByDate(List<ActivityLogEntry> entries) {
        Map<String, List<ActivityLogEntry>> groups = new LinkedHashMap<>();

        for (ActivityLogEntry entry : entries) {
            groups.computeIfAbsent(entry.dateGroup(), key -> new ArrayList<>()).add(entry);
        }

        return groups;
    }

    private HBox createDateHeader(String dateGroup) {
        Label dateLabel = new Label(dateGroup);
        dateLabel.getStyleClass().add("activity-log-date-title");

        Region line = new Region();
        line.getStyleClass().add("activity-log-date-line");
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox header = new HBox(12, dateLabel, line);
        header.getStyleClass().add("activity-log-date-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);

        return header;
    }

    private VBox createActivityRow(ActivityLogEntry entry) {
        VBox row = new VBox();
        row.getStyleClass().add("activity-log-event-row");
        row.setMaxWidth(Double.MAX_VALUE);

        if (entry.id().equals(expandedEntryId)) {
            row.getStyleClass().add("activity-log-event-row-expanded");
        }

        HBox summary = new HBox(12);
        summary.setAlignment(Pos.CENTER_LEFT);
        summary.setMaxWidth(Double.MAX_VALUE);
        summary.getStyleClass().add("activity-log-event-summary");

        StackPane iconShell = createEventIcon(entry.type());

        VBox copyBox = new VBox(3);
        copyBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copyBox, Priority.ALWAYS);

        Label actionLabel = new Label(entry.action());
        actionLabel.getStyleClass().add("activity-log-event-title");
        actionLabel.setWrapText(true);

        Label descriptionLabel = new Label(entry.description());
        descriptionLabel.getStyleClass().add("activity-log-event-description");
        descriptionLabel.setWrapText(true);

        Label metaLabel = new Label(entry.actor() + " · " + entry.target());
        metaLabel.getStyleClass().add("activity-log-event-meta");
        metaLabel.setWrapText(true);

        copyBox.getChildren().setAll(actionLabel, descriptionLabel, metaLabel);

        VBox rightBox = new VBox(6);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.getStyleClass().add("activity-log-event-right");

        Label typeBadge = createTypeBadge(entry.type());
        Label statusBadge = createStatusBadge(entry.status());

        Label timeLabel = new Label(entry.timestamp());
        timeLabel.getStyleClass().add("activity-log-event-time");

        rightBox.getChildren().setAll(timeLabel, typeBadge, statusBadge);

        summary.getChildren().setAll(iconShell, copyBox, rightBox);

        row.getChildren().add(summary);

        if (entry.id().equals(expandedEntryId)) {
            row.getChildren().add(createDetailsPanel(entry));
        }

        row.setOnMouseClicked(event -> {
            expandedEntryId = entry.id().equals(expandedEntryId) ? null : entry.id();
            renderTimeline();
        });

        return row;
    }

    private StackPane createEventIcon(String type) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPathForType(type));
        icon.getStyleClass().add("activity-log-event-icon");

        StackPane shell = new StackPane(icon);
        shell.getStyleClass().add("activity-log-event-icon-shell");
        shell.getStyleClass().add(iconStyleClassForType(type));

        return shell;
    }

    private Label createTypeBadge(String type) {
        Label badge = new Label(type);
        badge.getStyleClass().add("activity-log-type-badge");
        badge.getStyleClass().add(typeBadgeClass(type));
        return badge;
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("activity-log-status-badge");
        badge.getStyleClass().add(statusBadgeClass(status));
        return badge;
    }

    private VBox createDetailsPanel(ActivityLogEntry entry) {
        VBox panel = new VBox(9);
        panel.getStyleClass().add("activity-log-details-panel");
        panel.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("Activity Details");
        title.getStyleClass().add("activity-log-details-title");

        GridPane detailGrid = new GridPane();
        detailGrid.getStyleClass().add("activity-log-details-grid");
        detailGrid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints keyColumn = new ColumnConstraints();
        keyColumn.setPercentWidth(22);
        keyColumn.setHgrow(Priority.NEVER);

        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setPercentWidth(78);
        valueColumn.setHgrow(Priority.ALWAYS);

        detailGrid.getColumnConstraints().setAll(keyColumn, valueColumn);

        List<ActivityDetail> details = new ArrayList<>();
        details.add(new ActivityDetail("Action", entry.action()));
        details.add(new ActivityDetail("Performed by", entry.actor()));
        details.add(new ActivityDetail("Target", entry.target()));
        details.add(new ActivityDetail("Timestamp", entry.fullTimestamp()));
        details.add(new ActivityDetail("Status", entry.status()));
        details.addAll(entry.details());

        for (int rowIndex = 0; rowIndex < details.size(); rowIndex++) {
            ActivityDetail detail = details.get(rowIndex);

            Label keyLabel = new Label(detail.label() + ":");
            keyLabel.getStyleClass().add("activity-log-detail-key");

            Label valueLabel = new Label(detail.value());
            valueLabel.getStyleClass().add("activity-log-detail-value");
            valueLabel.setWrapText(true);
            valueLabel.setMaxWidth(Double.MAX_VALUE);

            GridPane.setHalignment(keyLabel, HPos.LEFT);
            GridPane.setValignment(keyLabel, VPos.TOP);

            GridPane.setHalignment(valueLabel, HPos.LEFT);
            GridPane.setValignment(valueLabel, VPos.TOP);
            GridPane.setHgrow(valueLabel, Priority.ALWAYS);

            detailGrid.add(keyLabel, 0, rowIndex);
            detailGrid.add(valueLabel, 1, rowIndex);
        }

        panel.getChildren().setAll(title, detailGrid);

        return panel;
    }

    private boolean matchesSearch(ActivityLogEntry entry) {
        String searchText = normalize(searchField.getText());

        if (searchText.isBlank()) {
            return true;
        }

        return normalize(entry.type()).contains(searchText)
                || normalize(entry.actor()).contains(searchText)
                || normalize(entry.action()).contains(searchText)
                || normalize(entry.target()).contains(searchText)
                || normalize(entry.status()).contains(searchText)
                || normalize(entry.description()).contains(searchText)
                || normalize(entry.fullTimestamp()).contains(searchText)
                || entry.details().stream().anyMatch(detail ->
                normalize(detail.label()).contains(searchText)
                        || normalize(detail.value()).contains(searchText)
        );
    }

    private boolean matchesFilters(ActivityLogEntry entry) {
        return matchesCombo(entry.type(), typeFilterComboBox.getValue(), ALL_TYPES)
                && matchesCombo(entry.actor(), userFilterComboBox.getValue(), ALL_USERS)
                && matchesCombo(entry.status(), statusFilterComboBox.getValue(), ALL_STATUSES)
                && matchesDateRange(entry.dateGroup(), dateRangeFilterComboBox.getValue());
    }

    private boolean matchesCombo(String value, String selectedValue, String allValue) {
        return selectedValue == null
                || allValue.equals(selectedValue)
                || value.equalsIgnoreCase(selectedValue);
    }

    private boolean matchesDateRange(String dateGroup, String selectedRange) {
        if (selectedRange == null
                || "Last 7 Days".equals(selectedRange)
                || "All Dates".equals(selectedRange)) {
            return true;
        }

        return dateGroup.equalsIgnoreCase(selectedRange);
    }

    private void updateSummaryCards() {
        long todayEvents = activityEntries.stream()
                .filter(entry -> "Today".equals(entry.dateGroup()))
                .count();

        long completedScans = activityEntries.stream()
                .filter(entry -> "Scans".equals(entry.type()))
                .filter(entry -> normalize(entry.action()).contains("completed"))
                .count();

        long completedQa = activityEntries.stream()
                .filter(entry -> "QA".equals(entry.type()))
                .filter(entry -> normalize(entry.action()).contains("completed")
                        || normalize(entry.action()).contains("approved"))
                .count();

        long failedEvents = activityEntries.stream()
                .filter(entry -> "Failed".equals(entry.status()))
                .count();

        todayEventsValueLabel.setText(todayEvents == 1 ? "1 event" : todayEvents + " events");
        scansCompletedValueLabel.setText(String.valueOf(completedScans));
        qaCompletedValueLabel.setText(String.valueOf(completedQa));
        failedEventsValueLabel.setText(String.valueOf(failedEvents));
    }

    @FXML
    private void showTodayActivity() {
        dateRangeFilterComboBox.setValue("Today");
    }

    @FXML
    private void showScansOnly() {
        typeFilterComboBox.setValue("Scans");
        statusFilterComboBox.setValue(ALL_STATUSES);
    }

    @FXML
    private void showQaCompleted() {
        typeFilterComboBox.setValue("QA");
        statusFilterComboBox.setValue("Success");
    }

    @FXML
    private void showFailedEvents() {
        statusFilterComboBox.setValue("Failed");
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        typeFilterComboBox.setValue(ALL_TYPES);
        userFilterComboBox.setValue(ALL_USERS);
        statusFilterComboBox.setValue(ALL_STATUSES);
        dateRangeFilterComboBox.setValue("Last 7 Days");
        expandedEntryId = null;
        renderTimeline();
    }

    @FXML
    private void exportActivityLog() {
        // Hook this up to CSV/report export later.
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String iconStyleClassForType(String type) {
        return switch (type) {
            case "Users" -> "activity-log-icon-user";
            case "Profiles" -> "activity-log-icon-profile";
            case "Access" -> "activity-log-icon-access";
            case "Metadata" -> "activity-log-icon-metadata";
            case "Scans" -> "activity-log-icon-scan";
            case "Documents" -> "activity-log-icon-document";
            case "QA" -> "activity-log-icon-qa";
            case "Exports" -> "activity-log-icon-export";
            case "Security" -> "activity-log-icon-security";
            default -> "activity-log-icon-system";
        };
    }

    private String typeBadgeClass(String type) {
        return switch (type) {
            case "Scans" -> "activity-log-type-scan";
            case "QA" -> "activity-log-type-qa";
            case "Exports" -> "activity-log-type-export";
            case "Security" -> "activity-log-type-security";
            case "Metadata" -> "activity-log-type-metadata";
            case "Access" -> "activity-log-type-access";
            case "Profiles" -> "activity-log-type-profile";
            case "Documents" -> "activity-log-type-document";
            case "Users" -> "activity-log-type-user";
            default -> "activity-log-type-system";
        };
    }

    private String statusBadgeClass(String status) {
        return switch (status) {
            case "Success" -> "activity-log-status-success";
            case "Warning" -> "activity-log-status-warning";
            case "Failed" -> "activity-log-status-failed";
            default -> "activity-log-status-info";
        };
    }

    private String iconPathForType(String type) {
        return switch (type) {
            case "Users" ->
                    "M8 7.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zm8 0a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zM8 9.5c-3.04 0-5.5 1.79-5.5 4V15h11v-1.5c0-2.21-2.46-4-5.5-4zm8 0c-.52 0-1.03.05-1.5.15 1.23.89 2 2.11 2 3.55V15h5v-1.5c0-2.21-2.46-4-5.5-4z";
            case "Profiles" ->
                    "M5 3.5h14a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2zm0 2v13h14v-13H5zm2.5 3h4.2a2.4 2.4 0 0 1 4.6 0h1.2v2h-1.2a2.4 2.4 0 0 1-4.6 0H7.5v-2z";
            case "Access" ->
                    "M8 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-3.31 0-6 1.79-6 4v2h8.1a6.48 6.48 0 0 1-.1-1.1c0-1.9.82-3.61 2.13-4.8A10.6 10.6 0 0 0 8 13zm8.5 0a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9zm-.5 6.7-2.1-2.1 1.06-1.06L16 17.59l2.54-2.55 1.06 1.06-3.6 3.6z";
            case "Metadata" ->
                    "M4 4h8.8L20 11.2V20H4V4zm2 2v12h12v-5.8L11.8 6H6zm3 3h3v2H9V9zm0 4h6v2H9v-2z";
            case "Scans" ->
                    "M5 3h14v18H5V3zm2 2v14h10V5H7zm2 3h6v2H9V8zm0 4h6v2H9v-2z";
            case "Documents" ->
                    "M6 3h9l5 5v13H6V3zm8 2v4h4l-4-4zM8 12h10v2H8v-2zm0 4h10v2H8v-2z";
            case "QA" ->
                    "M12 2 4 5v6c0 5 3.4 9.4 8 11 4.6-1.6 8-6 8-11V5l-8-3zm-1 14-4-4 1.4-1.4L11 13.2l5.6-5.6L18 9l-7 7z";
            case "Exports" ->
                    "M5 20h14v-2H5v2zm7-17-5 5h3v6h4V8h3l-5-5z";
            case "Security" ->
                    "M12 2 4 5v6c0 5 3.4 9.4 8 11 4.6-1.6 8-6 8-11V5l-8-3zm0 5a3 3 0 0 1 3 3v1h1v6H8v-6h1v-1a3 3 0 0 1 3-3zm-1 4h2v-1a1 1 0 0 0-2 0v1z";
            default ->
                    "M4 4h16v16H4V4zm2 2v12h12V6H6zm2 2h8v2H8V8zm0 4h5v2H8v-2z";
        };
    }

    private void loadSampleActivity() {
        activityEntries.setAll(
                new ActivityLogEntry(
                        "a-001",
                        "Today",
                        "10:42",
                        "2026-04-25 10:42",
                        "Scans",
                        "Sarah Smith",
                        "Sarah Smith completed scan",
                        "BOX-2026-004",
                        "Success",
                        "BOX-2026-004 · Building Archive · 42 pages",
                        List.of(
                                new ActivityDetail("Profile", "Building Archive"),
                                new ActivityDetail("Pages scanned", "42"),
                                new ActivityDetail("Documents created", "6"),
                                new ActivityDetail("Barcode splits", "5"),
                                new ActivityDetail("Current status", "Waiting for QA")
                        )
                ),
                new ActivityLogEntry(
                        "a-002",
                        "Today",
                        "10:31",
                        "2026-04-25 10:31",
                        "Scans",
                        "System",
                        "System detected barcode split",
                        "Document 3",
                        "Success",
                        "Barcode separator detected and document split was applied automatically.",
                        List.of(
                                new ActivityDetail("Box", "BOX-2026-004"),
                                new ActivityDetail("Document", "Document 3"),
                                new ActivityDetail("Split rule", "Barcode Split On"),
                                new ActivityDetail("Result", "New document boundary created")
                        )
                ),
                new ActivityLogEntry(
                        "a-003",
                        "Today",
                        "09:58",
                        "2026-04-25 09:58",
                        "Access",
                        "Admin",
                        "Admin assigned profile access",
                        "Building Archive",
                        "Success",
                        "Building Archive profile was assigned to Sarah Smith.",
                        List.of(
                                new ActivityDetail("Profile", "Building Archive"),
                                new ActivityDetail("Assigned user", "Sarah Smith"),
                                new ActivityDetail("Before", "Sarah Smith did not have access"),
                                new ActivityDetail("After", "Sarah Smith has access")
                        )
                ),
                new ActivityLogEntry(
                        "a-004",
                        "Today",
                        "09:44",
                        "2026-04-25 09:44",
                        "Metadata",
                        "Admin",
                        "Admin updated metadata template",
                        "Court Records Metadata",
                        "Success",
                        "Field “Case Number” was added to Court Records Metadata.",
                        List.of(
                                new ActivityDetail("Template", "Court Records Metadata"),
                                new ActivityDetail("Field added", "Case Number"),
                                new ActivityDetail("Required", "Yes"),
                                new ActivityDetail("Field type", "Text")
                        )
                ),
                new ActivityLogEntry(
                        "a-005",
                        "Today",
                        "09:12",
                        "2026-04-25 09:12",
                        "Users",
                        "Admin",
                        "Admin created user",
                        "Sarah Smith",
                        "Success",
                        "A new user account was created for Sarah Smith.",
                        List.of(
                                new ActivityDetail("User", "Sarah Smith"),
                                new ActivityDetail("Role", "User"),
                                new ActivityDetail("Status", "Active")
                        )
                ),
                new ActivityLogEntry(
                        "a-006",
                        "Today",
                        "08:51",
                        "2026-04-25 08:51",
                        "Security",
                        "System",
                        "Failed login attempt",
                        "david",
                        "Failed",
                        "A failed login attempt was recorded for username “david”.",
                        List.of(
                                new ActivityDetail("Username", "david"),
                                new ActivityDetail("Reason", "Invalid password"),
                                new ActivityDetail("Source", "Login screen")
                        )
                ),
                new ActivityLogEntry(
                        "a-007",
                        "Yesterday",
                        "15:12",
                        "2026-04-24 15:12",
                        "Profiles",
                        "Admin",
                        "Admin created profile",
                        "Technical Drawings",
                        "Success",
                        "Technical Drawings scan profile was created.",
                        List.of(
                                new ActivityDetail("Profile", "Technical Drawings"),
                                new ActivityDetail("Barcode split", "On"),
                                new ActivityDetail("OCR", "Enabled"),
                                new ActivityDetail("Export naming", "TechnicalDrawings_{boxId}")
                        )
                ),
                new ActivityLogEntry(
                        "a-008",
                        "Yesterday",
                        "14:30",
                        "2026-04-24 14:30",
                        "QA",
                        "John Doe",
                        "John Doe completed QA",
                        "BOX-2026-003",
                        "Success",
                        "QA was completed for BOX-2026-003.",
                        List.of(
                                new ActivityDetail("Box", "BOX-2026-003"),
                                new ActivityDetail("QA result", "Approved"),
                                new ActivityDetail("Documents reviewed", "4")
                        )
                ),
                new ActivityLogEntry(
                        "a-009",
                        "Yesterday",
                        "13:44",
                        "2026-04-24 13:44",
                        "Documents",
                        "John Doe",
                        "Document status changed",
                        "DOC-2026-042",
                        "Info",
                        "Document changed from Waiting for QA to QA Completed.",
                        List.of(
                                new ActivityDetail("Document", "DOC-2026-042"),
                                new ActivityDetail("Before", "Waiting for QA"),
                                new ActivityDetail("After", "QA Completed")
                        )
                ),
                new ActivityLogEntry(
                        "a-010",
                        "Yesterday",
                        "11:25",
                        "2026-04-24 11:25",
                        "Exports",
                        "System",
                        "System created export",
                        "BuildingArchive_BOX-2026-004",
                        "Success",
                        "Export package was generated successfully.",
                        List.of(
                                new ActivityDetail("Export name", "BuildingArchive_BOX-2026-004"),
                                new ActivityDetail("Format", "PDF/A + metadata CSV"),
                                new ActivityDetail("Documents", "6"),
                                new ActivityDetail("Status", "Completed")
                        )
                ),
                new ActivityLogEntry(
                        "a-011",
                        "Yesterday",
                        "10:05",
                        "2026-04-24 10:05",
                        "Exports",
                        "System",
                        "Export failed",
                        "CourtRecords_BOX-2026-011",
                        "Failed",
                        "Export failed because required metadata fields were missing.",
                        List.of(
                                new ActivityDetail("Export name", "CourtRecords_BOX-2026-011"),
                                new ActivityDetail("Reason", "Missing required metadata"),
                                new ActivityDetail("Missing fields", "Document Type, Registration Date")
                        )
                ),
                new ActivityLogEntry(
                        "a-012",
                        "Earlier This Week",
                        "16:20",
                        "2026-04-22 16:20",
                        "Metadata",
                        "Admin",
                        "Admin marked metadata field as required",
                        "Building Archive Metadata",
                        "Warning",
                        "Box ID was marked as required for Building Archive Metadata.",
                        List.of(
                                new ActivityDetail("Template", "Building Archive Metadata"),
                                new ActivityDetail("Field", "Box ID"),
                                new ActivityDetail("Before", "Optional"),
                                new ActivityDetail("After", "Required")
                        )
                )
        );
    }

    private record ActivityLogEntry(
            String id,
            String dateGroup,
            String timestamp,
            String fullTimestamp,
            String type,
            String actor,
            String action,
            String target,
            String status,
            String description,
            List<ActivityDetail> details
    ) {
    }

    private record ActivityDetail(String label, String value) {
    }
}