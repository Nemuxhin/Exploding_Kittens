package easv.gui.controller.admin;

import easv.be.AuditLog;
import easv.bll.AdminManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityController {

    private enum DateFilterMode {
        ALL,
        SPECIFIC,
        RANGE
    }

    private static final String ALL_AREAS = "All areas";
    private static final String ALL_USERS = "All users";
    private static final String ALL_RESULTS = "All results";
    private static final String SORT_NEWEST_FIRST = "Newest first";
    private static final String SORT_OLDEST_FIRST = "Oldest first";

    private static final String UPLOAD_ICON_PATH = "M5 20h14v-2H5v2zm7-18-5.5 5.5 1.41 1.41L11 5.83V16h2V5.83l3.09 3.08 1.41-1.41L12 2z";
    private static final String CHECK_ICON_PATH = "M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z";
    private static final String WARNING_ICON_PATH = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
    private static final String REFRESH_ICON_PATH = "M17.65 6.35A7.95 7.95 0 0 0 12 4V1L7 6l5 5V7a5 5 0 1 1-4.9 6H5.05A7 7 0 1 0 17.65 6.35z";
    private static final String USER_ICON_PATH = "M 12 12 C 14.21 12 16 10.21 16 8 C 16 5.79 14.21 4 12 4 C 9.79 4 8 5.79 8 8 C 8 10.21 9.79 12 12 12 Z M 12 14 C 9.33 14 4 15.34 4 18 L 4 20 L 20 20 L 20 18 C 20 15.34 14.67 14 12 14 Z";
    private static final String GEAR_ICON_PATH = "M19.43 12.98c.04-.32.07-.65.07-.98s-.02-.66-.07-.98l2.11-1.65-2-3.46-2.49 1a7.03 7.03 0 0 0-1.69-.98L15 3h-4l-.36 2.93c-.6.23-1.16.56-1.69.98l-2.49-1-2 3.46 2.11 1.65c-.04.32-.07.65-.07.98s.02.66.07.98l-2.11 1.65 2 3.46 2.49-1c.52.4 1.08.73 1.69.98L11 21h4l.36-2.93c.6-.23 1.16-.56 1.69-.98l2.49 1 2-3.46-2.11-1.65zM13 15.5A3.5 3.5 0 1 1 13 8a3.5 3.5 0 0 1 0 7.5z";
    private static final String DOWNLOAD_ICON_PATH = "M5 20h14v-2H5v2zm14-9h-4V3H9v8H5l7 7 7-7z";
    private static final String DOCUMENT_ICON_PATH = "M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zM13 9V3.5L18.5 9H13z";

    private static final String AREA_FILTER_ICON_PATH = "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5C15 14.17 10.33 13 8 13zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5C23 14.17 18.33 13 16 13z";
    private static final String RESULT_FILTER_ICON_PATH = "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-1.2 14.2-4-4 1.4-1.4 2.6 2.6 5.6-5.6 1.4 1.4-7 7z";
    private static final String DATE_FILTER_ICON_PATH = "M7 2v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-2V2h-2v2H9V2H7zm12 18H5V9h14v11z";
    private static final String SORT_FILTER_ICON_PATH = "M3 18h6v-2H3v2zm0-5h12v-2H3v2zm0-7v2h18V6H3z";

    private static final String TARGET_ID_PATTERN_TEXT = "\\b[A-Z]{2,}(?:[-_][A-Z0-9]+)+\\b";
    private static final Pattern TARGET_ID_PATTERN = Pattern.compile(TARGET_ID_PATTERN_TEXT);

    private static final DateTimeFormatter ACTIVITY_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FALLBACK_ACTIVITY_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ROW_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter GROUP_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_RANGE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final double INLINE_PAYLOAD_WIDTH = 1110;
    private static final double COMPARISON_WIDTH = 960;
    private static final double INLINE_TABLE_WIDTH = 1050;

    private final ObservableList<ActivityLogEntry> activityEntries = FXCollections.observableArrayList();

    private AdminManager adminManager;
    private String selectedEntryId;

    private boolean updatingDateControls;
    private DateFilterMode dateFilterMode = DateFilterMode.ALL;
    private LocalDate specificDate;
    private LocalDate rangeStartDate;
    private LocalDate rangeEndDate;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> userFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> sortFilterComboBox;

    @FXML private MenuButton dateFilterMenuButton;
    @FXML private Button specificDateModeButton;
    @FXML private Button rangeDateModeButton;
    @FXML private VBox specificDateBox;
    @FXML private VBox dateRangeBox;
    @FXML private DatePicker specificDatePicker;
    @FXML private DatePicker rangeStartDatePicker;
    @FXML private DatePicker rangeEndDatePicker;

    @FXML private VBox timelineContainer;
    @FXML private VBox emptyStateBox;

    @FXML
    private void initialize() {
        configureFilters();
        configureToolbarGraphics();
        configureListeners();
        renderTimeline();
    }

    void setAdminManager(AdminManager adminManager) {
        this.adminManager = adminManager;

        if (this.adminManager == null) {
            return;
        }

        loadActivity();
        renderTimeline();
    }

    private void configureFilters() {
        configureDatePickers();

        if (typeFilterComboBox != null) {
            typeFilterComboBox.getItems().setAll(
                    ALL_AREAS,
                    "Files",
                    "QA",
                    "Import",
                    "Users",
                    "Profiles",
                    "Access",
                    "Metadata",
                    "Exports",
                    "Security",
                    "System"
            );
            typeFilterComboBox.setValue(ALL_AREAS);
        }

        if (userFilterComboBox != null) {
            userFilterComboBox.getItems().setAll(ALL_USERS);
            userFilterComboBox.setValue(ALL_USERS);
        }

        if (statusFilterComboBox != null) {
            statusFilterComboBox.getItems().setAll(
                    ALL_RESULTS,
                    "Success",
                    "Failed"
            );
            statusFilterComboBox.setValue(ALL_RESULTS);
        }

        if (sortFilterComboBox != null) {
            sortFilterComboBox.getItems().setAll(SORT_NEWEST_FIRST, SORT_OLDEST_FIRST);
            sortFilterComboBox.setValue(SORT_NEWEST_FIRST);
        }

        dateFilterMode = DateFilterMode.ALL;
        updateDateFilterState();
    }

    private void configureToolbarGraphics() {
        configureFilterComboBox(typeFilterComboBox, "Area", AREA_FILTER_ICON_PATH);
        configureFilterComboBox(userFilterComboBox, "User", USER_ICON_PATH);
        configureFilterComboBox(statusFilterComboBox, "Result", RESULT_FILTER_ICON_PATH);
        configureFilterComboBox(sortFilterComboBox, "Sort", SORT_FILTER_ICON_PATH);

        if (dateFilterMenuButton != null) {
            dateFilterMenuButton.setText(null);
            dateFilterMenuButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setDateFilterButtonDisplay("All dates");
        }
    }

    private void configureFilterComboBox(ComboBox<String> comboBox, String heading, String iconPath) {
        if (comboBox == null) {
            return;
        }

        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                setGraphic(createFilterGraphic(iconPath, heading, value));
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });

        comboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value);
                setGraphic(null);
            }
        });
    }

    private HBox createFilterGraphic(String iconPath, String heading, String value) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add("logs-filter-icon-path");

        StackPane iconShell = new StackPane(icon);
        iconShell.getStyleClass().add("logs-filter-icon-shell");

        Label headingLabel = new Label(heading);
        headingLabel.getStyleClass().add("logs-filter-heading");
        headingLabel.setMinWidth(0);
        headingLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        headingLabel.setWrapText(false);

        Label valueLabel = new Label(displayText(value, ""));
        valueLabel.getStyleClass().add("logs-filter-value");
        valueLabel.setMinWidth(0);
        valueLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        valueLabel.setWrapText(false);

        VBox copy = new VBox(0, headingLabel, valueLabel);
        copy.getStyleClass().add("logs-filter-copy");
        copy.setMinWidth(0);

        HBox graphic = new HBox(9, iconShell, copy);
        graphic.getStyleClass().add("logs-filter-graphic");
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.setMinWidth(0);
        graphic.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(copy, Priority.ALWAYS);

        return graphic;
    }

    private void configureListeners() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshFilteredTimeline());
        }

        if (typeFilterComboBox != null) {
            typeFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshFilteredTimeline());
        }

        if (userFilterComboBox != null) {
            userFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshFilteredTimeline());
        }

        if (statusFilterComboBox != null) {
            statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshFilteredTimeline());
        }

        if (sortFilterComboBox != null) {
            sortFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshFilteredTimeline());
        }

        if (specificDatePicker != null) {
            specificDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleDateInputChange());
        }

        if (rangeStartDatePicker != null) {
            rangeStartDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleDateInputChange());
        }

        if (rangeEndDatePicker != null) {
            rangeEndDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleDateInputChange());
        }
    }

    private void renderTimeline() {
        List<ActivityLogEntry> filteredEntries = filteredActivityEntries();
        boolean hasEntries = !filteredEntries.isEmpty();

        if (timelineContainer != null) {
            timelineContainer.getChildren().clear();
            timelineContainer.setVisible(hasEntries);
            timelineContainer.setManaged(hasEntries);
        }

        if (emptyStateBox != null) {
            emptyStateBox.setVisible(!hasEntries);
            emptyStateBox.setManaged(!hasEntries);
        }

        if (!hasEntries) {
            selectedEntryId = null;
            return;
        }

        keepSelectedEntryVisible(filteredEntries);

        if (timelineContainer != null) {
            timelineContainer.getChildren().setAll(createEventStream(filteredEntries));
        }
    }

    private void keepSelectedEntryVisible(List<ActivityLogEntry> entries) {
        if (selectedEntryId == null) {
            return;
        }

        boolean selectedEntryVisible = selectedEntryId != null
                && entries.stream().anyMatch(entry -> entry.id().equals(selectedEntryId));

        if (!selectedEntryVisible) {
            selectedEntryId = null;
        }
    }

    private java.util.Optional<ActivityLogEntry> selectedEntry(List<ActivityLogEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.id().equals(selectedEntryId))
                .findFirst();
    }

    private VBox createEventStream(List<ActivityLogEntry> entries) {
        VBox stream = new VBox(0);
        stream.getStyleClass().add("logs-event-stream-shell");
        stream.setFillWidth(true);
        stream.setMaxWidth(Double.MAX_VALUE);

        for (Map.Entry<String, List<ActivityLogEntry>> group : groupedEntries(entries).entrySet()) {
            stream.getChildren().add(createEventGroupHeader(group.getKey(), group.getValue().size()));
            group.getValue().forEach(entry -> stream.getChildren().add(createExpandableEventRow(entry)));
        }

        stream.getChildren().add(createLoadMoreButton());
        return stream;
    }

    private Map<String, List<ActivityLogEntry>> groupedEntries(List<ActivityLogEntry> entries) {
        Map<String, List<ActivityLogEntry>> groupedEntries = new LinkedHashMap<>();

        for (ActivityLogEntry entry : entries) {
            groupedEntries.computeIfAbsent(groupLabel(entry), key -> new ArrayList<>()).add(entry);
        }

        return groupedEntries;
    }

    private HBox createEventGroupHeader(String label, int count) {
        Label title = new Label(label);
        title.getStyleClass().add("logs-event-group-title");

        Label countBadge = new Label(String.valueOf(count));
        countBadge.getStyleClass().add("logs-event-count-badge");

        HBox header = new HBox(9, title, countBadge);
        header.getStyleClass().add("logs-event-group-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox createExpandableEventRow(ActivityLogEntry entry) {
        VBox row = new VBox(0);
        row.getStyleClass().add("logs-expandable-row");
        row.setFillWidth(true);
        row.setMaxWidth(Double.MAX_VALUE);

        boolean selected = entry.id().equals(selectedEntryId);

        if (selected) {
            row.getStyleClass().add(isError(entry)
                    ? "logs-expandable-row-selected-failed"
                    : "logs-expandable-row-selected");
        }

        row.getChildren().add(createEventSummaryButton(entry, selected));

        if (selected) {
            row.getChildren().add(createInlinePayloadTray(entry));
        }

        return row;
    }

    private Button createEventSummaryButton(ActivityLogEntry entry, boolean selected) {
        HBox rowContent = new HBox(15);
        rowContent.getStyleClass().add("logs-event-row-content");
        rowContent.setAlignment(Pos.CENTER_LEFT);
        rowContent.setMaxWidth(Double.MAX_VALUE);

        StackPane icon = createEventIcon(entry);
        Label time = createEventTimeLabel(entry);
        VBox copy = createEventCopy(entry);
        Label areaBadge = createAreaBadge(entry);
        Label statusBadge = createStatusBadge(entry.status());

        rowContent.getChildren().setAll(
                icon,
                time,
                copy,
                areaBadge,
                statusBadge
        );

        Button row = new Button();
        row.setGraphic(rowContent);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setFocusTraversable(false);
        rowContent.prefWidthProperty().bind(row.widthProperty());

        row.setOnAction(event -> {
            selectedEntryId = entry.id().equals(selectedEntryId) ? null : entry.id();
            renderTimeline();
        });

        row.getStyleClass().add("logs-event-row");

        if (selected) {
            row.getStyleClass().add(isError(entry) ? "logs-event-row-selected-failed" : "logs-event-row-selected");
        }

        return row;
    }

    private VBox createInlinePayloadTray(ActivityLogEntry entry) {
        VBox tray = new VBox(0);
        tray.getStyleClass().add("logs-inline-payload");

        if (isError(entry)) {
            tray.getStyleClass().add("logs-inline-payload-failed");
        }

        tray.setFillWidth(true);
        tray.setMaxWidth(Double.MAX_VALUE);
        tray.setAlignment(Pos.TOP_LEFT);

        VBox content = new VBox(6);
        content.getStyleClass().add("logs-inline-payload-content");
        content.setFillWidth(true);
        content.setMinWidth(0);
        content.setPrefWidth(Region.USE_COMPUTED_SIZE);
        content.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().setAll(createInlinePayloadContent(entry));

        tray.getChildren().setAll(content);
        return tray;
    }

    private List<Node> createInlinePayloadContent(ActivityLogEntry entry) {
        List<Node> nodes = new ArrayList<>();
        FlowPane detailStrip = createInlineDetailStrip(entry);

        if (detailStrip != null && !detailStrip.getChildren().isEmpty()) {
            nodes.add(detailStrip);
        }

        return nodes;
    }

    private FlowPane createInlineDetailStrip(ActivityLogEntry entry) {
        List<Node> chips = createInlineDetailChips(entry);

        if (chips.isEmpty()) {
            return null;
        }

        FlowPane strip = new FlowPane();
        strip.getStyleClass().add("logs-inline-detail-strip");

        if (isError(entry)) {
            strip.getStyleClass().add("logs-inline-detail-strip-failed");
        }

        strip.setHgap(6);
        strip.setVgap(6);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.setPrefWrapLength(INLINE_TABLE_WIDTH);
        strip.setMaxWidth(Double.MAX_VALUE);
        strip.getChildren().setAll(chips);
        return strip;
    }

    private List<Node> createInlineDetailChips(ActivityLogEntry entry) {
        List<Node> chips = new ArrayList<>();

        if (!entry.changes().isEmpty() && !isCreateEvent(entry) && !isDeleteEvent(entry)) {
            entry.changes().stream()
                    .filter(this::hasVisibleChange)
                    .forEach(change -> chips.add(createInlineChangeChip(change)));
        } else {
            compactDetailRows(entry).forEach(row -> chips.add(createInlineDetailChip(row, entry)));
        }

        String description = displayText(entry.description(), "");
        if (chips.isEmpty() && !description.isBlank()) {
            chips.add(createInlineDetailChip(new ActivityDetailRow("Note", description), entry));
        }

        chips.add(createInlineTraceChip(entry));
        return chips;
    }

    private List<ActivityDetailRow> compactDetailRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        String area = normalize(displayArea(entry));

        if (!entry.changes().isEmpty() && (isCreateEvent(entry) || isDeleteEvent(entry))) {
            rows.addAll(snapshotRowsForChangeEvent(entry));
        } else if (isError(entry) && (area.equals("import") || isTiffActivity(entry))) {
            rows.add(new ActivityDetailRow("File", displayTiffItem(entry)));

            String reason = displayText(entry.description(), "");
            if (!reason.isBlank()) {
                rows.add(new ActivityDetailRow("Reason", reason));
            }

            rows.addAll(normalizedContextRows(entry));

            if (rows.stream().noneMatch(row -> normalize(row.label()).contains("accepted"))) {
                rows.add(new ActivityDetailRow("Accepted", "LZW, PackBits, Uncompressed"));
            }

            rows.add(new ActivityDetailRow("Action needed", "Re-export TIFF using supported compression"));
        } else if (isTiffActivity(entry)) {
            rows.addAll(tiffMetricRows(entry));
            addFirstDetail(rows, entry, "Box", "box", "box id");
            addFirstDetail(rows, entry, "File ID", "file id", "file");
            addFirstDetail(rows, entry, "Path", "path", "storage path");
        } else {
            rows.addAll(eventDetailRows(entry));
        }

        return compactVisibleRows(rows, 10);
    }

    private List<ActivityDetailRow> compactVisibleRows(List<ActivityDetailRow> rows, int limit) {
        Map<String, ActivityDetailRow> uniqueRows = new LinkedHashMap<>();

        for (ActivityDetailRow row : rows) {
            if (row == null || isMissingAuditValue(row.value())) {
                continue;
            }

            String normalizedLabel = normalize(row.label());
            if (normalizedLabel.equals("trace") || normalizedLabel.equals("log id") || normalizedLabel.equals("recorded")) {
                continue;
            }

            String key = normalizedLabel + "|" + normalize(displayAuditValue(row.value()));
            uniqueRows.putIfAbsent(key, row);
        }

        return uniqueRows.values().stream()
                .limit(limit)
                .toList();
    }

    private HBox createInlineDetailChip(ActivityDetailRow row, ActivityLogEntry entry) {
        Label label = new Label(displayText(row.label(), "Detail"));
        label.getStyleClass().add("logs-inline-detail-label");
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);

        String value = displayAuditValue(row.value());
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("logs-inline-detail-value");
        valueLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        valueLabel.setTooltip(new Tooltip(value));
        valueLabel.setWrapText(false);
        valueLabel.setMinWidth(0);
        valueLabel.setMaxWidth(detailValueMaxWidth(row.label()));

        HBox chip = new HBox(6, label, valueLabel);
        chip.getStyleClass().add("logs-inline-detail-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setMinWidth(Region.USE_PREF_SIZE);
        chip.setMaxWidth(Region.USE_PREF_SIZE);

        if (isError(entry)) {
            chip.getStyleClass().add("logs-inline-detail-chip-failed");
        }

        String normalizedLabel = normalize(row.label());
        if (normalizedLabel.contains("reason")
                || normalizedLabel.contains("failed")
                || normalizedLabel.contains("detected")
                || normalizedLabel.contains("action")) {
            chip.getStyleClass().add("logs-inline-detail-chip-attention");
        }

        return chip;
    }

    private double detailValueMaxWidth(String label) {
        String normalizedLabel = normalize(label);

        if (normalizedLabel.contains("action") || normalizedLabel.contains("accepted") || normalizedLabel.contains("reason")) {
            return 300;
        }

        if (normalizedLabel.contains("email") || normalizedLabel.contains("path")) {
            return 260;
        }

        return 210;
    }

    private HBox createInlineChangeChip(ActivityChange change) {
        Label field = new Label(displayText(change.field(), "Field"));
        field.getStyleClass().add("logs-inline-change-field");
        field.setTextOverrun(OverrunStyle.ELLIPSIS);
        field.setTooltip(new Tooltip(field.getText()));
        field.setMinWidth(0);
        field.setMaxWidth(135);

        Label before = createInlineChangeValue(displayAuditValue(change.oldValue()), "logs-inline-change-before");
        Label arrow = new Label("→");
        arrow.getStyleClass().add("logs-inline-change-arrow");
        Label after = createInlineChangeValue(displayAuditValue(change.newValue()), "logs-inline-change-after");

        HBox chip = new HBox(6, field, before, arrow, after);
        chip.getStyleClass().add("logs-inline-change-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setMinWidth(Region.USE_PREF_SIZE);
        chip.setMaxWidth(Region.USE_PREF_SIZE);
        return chip;
    }

    private Label createInlineChangeValue(String text, String styleClass) {
        Label value = new Label(text);
        value.getStyleClass().add(styleClass);
        value.setTextOverrun(OverrunStyle.ELLIPSIS);
        value.setTooltip(new Tooltip(text));
        value.setMinWidth(0);
        value.setMaxWidth(175);
        return value;
    }

    private Label createInlineTraceChip(ActivityLogEntry entry) {
        Label trace = new Label("LOG-" + entry.id());
        trace.getStyleClass().add("logs-inline-trace-chip");
        trace.setTooltip(new Tooltip(entry.fullTimestamp()));
        trace.setMinWidth(Region.USE_PREF_SIZE);
        trace.setMaxWidth(Region.USE_PREF_SIZE);
        return trace;
    }

    private String inlinePayloadTag(ActivityLogEntry entry) {
        String area = normalize(displayArea(entry));

        if (isError(entry)) {
            return area.equals("import") ? "Import failure" : "Failed";
        }

        if (area.equals("users")) {
            return userPayloadTag(entry);
        }

        if (area.equals("qa")) {
            return "QA";
        }

        if (area.equals("exports")) {
            return "Export";
        }

        if (area.equals("import")) {
            return "Import";
        }

        if (isTiffActivity(entry)) {
            return tiffPayloadTag(entry);
        }

        return displayArea(entry);
    }

    private String inlinePayloadTagClass(ActivityLogEntry entry) {
        String area = normalize(displayArea(entry));

        if (isError(entry)) {
            return "logs-payload-tag-failed";
        }

        if (area.equals("users") || area.equals("profiles") || area.equals("access") || area.equals("metadata")) {
            return "logs-payload-tag-users";
        }

        if (area.equals("qa")) {
            return "logs-payload-tag-qa";
        }

        if (area.equals("exports") || area.equals("import") || isTiffActivity(entry)) {
            return "logs-payload-tag-files";
        }

        return "logs-payload-tag-generic";
    }

    private List<ActivityDetailRow> inlineDetailRows(ActivityLogEntry entry) {
        String area = normalize(displayArea(entry));

        if (isError(entry) && (area.equals("import") || isTiffActivity(entry))) {
            List<ActivityDetailRow> rows = new ArrayList<>();
            rows.add(new ActivityDetailRow("File", displayTiffItem(entry)));
            rows.add(new ActivityDetailRow("Reason", displayText(entry.description(), "No failure reason recorded.")));
            rows.addAll(normalizedContextRows(entry));

            if (rows.stream().noneMatch(row -> normalize(row.label()).contains("accepted"))) {
                rows.add(new ActivityDetailRow("Accepted", "LZW, PackBits, Uncompressed"));
            }

            rows.add(new ActivityDetailRow("Action needed", "Re-export TIFF using supported compression"));
            return rows;
        }

        if (isTiffActivity(entry)) {
            List<ActivityDetailRow> rows = new ArrayList<>(tiffMetricRows(entry));
            if (rows.isEmpty()) {
                rows.addAll(eventDetailRows(entry));
            }
            return rows;
        }

        return eventDetailRows(entry);
    }

    private String inlineDetailTitle(ActivityLogEntry entry) {
        String area = normalize(displayArea(entry));

        if (isError(entry)) {
            return "Failure details";
        }

        if (isTiffActivity(entry)) {
            return tiffSectionTitle(entry);
        }

        if (area.equals("qa")) {
            return "QA details";
        }

        if (area.equals("exports")) {
            return "Export details";
        }

        if (area.equals("import")) {
            return "Import result";
        }

        return "Details";
    }

    private VBox createInlineChangeTable(ActivityLogEntry entry) {
        VBox table = createInlineTableShell(changeSectionTitle(entry));
        table.getChildren().add(createInlineChangeHeader());

        entry.changes().stream()
                .filter(this::hasVisibleChange)
                .forEach(change -> table.getChildren().add(createInlineChangeRow(change)));

        if (table.getChildren().size() == 1) {
            table.getChildren().add(createInlineEmptyRow("No field-level changes were recorded for this event."));
        }

        return table;
    }

    private VBox createInlineOverviewTable(String title, List<ActivityDetailRow> rows) {
        VBox table = createInlineTableShell(title);

        List<ActivityDetailRow> visibleRows = rows.stream()
                .filter(row -> !isMissingAuditValue(row.value()))
                .limit(8)
                .toList();

        if (visibleRows.isEmpty()) {
            table.getChildren().add(createInlineEmptyRow("No useful detail rows were recorded for this event."));
            return table;
        }

        table.getChildren().add(createInlineOverviewHeaderRow(visibleRows));
        table.getChildren().add(createInlineOverviewValueRow(visibleRows));
        return table;
    }

    private HBox createInlineOverviewHeaderRow(List<ActivityDetailRow> rows) {
        HBox row = new HBox(0);
        row.getStyleClass().add("logs-inline-overview-header-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        double cellWidth = overviewCellWidth(rows);

        for (int index = 0; index < rows.size(); index++) {
            Label cell = createInlineOverviewTextCell(
                    displayText(rows.get(index).label(), "Detail"),
                    "logs-inline-overview-header-cell",
                    cellWidth,
                    index == rows.size() - 1
            );
            row.getChildren().add(cell);
        }

        return row;
    }

    private HBox createInlineOverviewValueRow(List<ActivityDetailRow> rows) {
        HBox row = new HBox(0);
        row.getStyleClass().add("logs-inline-overview-value-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        double cellWidth = overviewCellWidth(rows);

        for (int index = 0; index < rows.size(); index++) {
            String value = displayAuditValue(rows.get(index).value());
            Label cell = createInlineOverviewTextCell(
                    value,
                    "logs-inline-overview-value-cell",
                    cellWidth,
                    index == rows.size() - 1
            );
            row.getChildren().add(cell);
        }

        return row;
    }

    private double overviewCellWidth(List<ActivityDetailRow> rows) {
        return INLINE_TABLE_WIDTH / Math.max(1, rows.size());
    }

    private Label createInlineOverviewTextCell(String text, String styleClass, double width, boolean last) {
        Label cell = new Label(text);
        cell.getStyleClass().add(styleClass);

        if (last) {
            cell.getStyleClass().add("logs-inline-overview-cell-last");
        }

        cell.setMinWidth(0);
        cell.setPrefWidth(width);
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setTextOverrun(OverrunStyle.ELLIPSIS);
        cell.setTooltip(new Tooltip(text));
        cell.setWrapText(false);
        HBox.setHgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private List<ActivityDetailRow> snapshotRowsForChangeEvent(ActivityLogEntry entry) {
        if (isCreateEvent(entry)) {
            return entry.changes().stream()
                    .filter(change -> !isMissingAuditValue(change.newValue()))
                    .map(change -> new ActivityDetailRow(change.field(), change.newValue()))
                    .toList();
        }

        if (isDeleteEvent(entry)) {
            return entry.changes().stream()
                    .filter(change -> !isStateSnapshotField(change.field()))
                    .filter(change -> !isMissingAuditValue(change.oldValue()))
                    .map(change -> new ActivityDetailRow(change.field(), change.oldValue()))
                    .toList();
        }

        return List.of();
    }

    private VBox createInlineTableShell(String title) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("logs-inline-table-title");

        VBox table = new VBox(0);
        table.getStyleClass().add("logs-inline-table");
        table.setFillWidth(true);
        table.setMinWidth(0);
        table.setPrefWidth(INLINE_TABLE_WIDTH);
        table.setMaxWidth(Double.MAX_VALUE);
        table.getChildren().add(titleLabel);
        return table;
    }

    private HBox createInlineChangeHeader() {
        Label field = createInlineHeaderLabel("Field", 210);
        Label before = createInlineHeaderLabel("Before", 300);
        Label spacer = createInlineHeaderLabel("", 24);
        Label after = createInlineHeaderLabel("After", 300);

        HBox row = new HBox(12, field, before, spacer, after);
        row.getStyleClass().add("logs-inline-table-header");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox createInlineChangeRow(ActivityChange change) {
        Label field = createInlineValueLabel(displayText(change.field(), "Field"), "logs-inline-field", 210);
        Label before = createInlineValueLabel(displayAuditValue(change.oldValue()), "logs-inline-before-pill", 300);
        Label arrow = createInlineValueLabel("\u2192", "logs-inline-arrow", 24);
        Label after = createInlineValueLabel(displayAuditValue(change.newValue()), "logs-inline-after-pill", 300);

        HBox row = new HBox(12, field, before, arrow, after);
        row.getStyleClass().add("logs-inline-table-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox createInlineEmptyRow(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("logs-inline-empty");
        label.setWrapText(true);
        HBox.setHgrow(label, Priority.ALWAYS);

        HBox row = new HBox(label);
        row.getStyleClass().add("logs-inline-table-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label createInlineHeaderLabel(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("logs-inline-table-heading");
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        return label;
    }

    private Label createInlineValueLabel(String text, String styleClass, double width) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setTooltip(new Tooltip(text));
        return label;
    }

    private HBox createInlineTrace(ActivityLogEntry entry) {
        Label trace = new Label("LOG-" + entry.id() + " \u00B7 " + entry.fullTimestamp());
        trace.getStyleClass().add("logs-inline-trace");

        HBox row = new HBox(trace);
        row.getStyleClass().add("logs-inline-trace-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefWidth(INLINE_TABLE_WIDTH);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private StackPane createEventIcon(ActivityLogEntry entry) {
        SVGPath icon = new SVGPath();
        icon.setContent(eventIconPath(entry));
        icon.getStyleClass().add("logs-event-icon-path");

        StackPane shell = new StackPane(icon);
        shell.getStyleClass().add("logs-event-icon");
        shell.getStyleClass().add(isError(entry) ? "logs-event-icon-failed" : eventIconClass(entry));
        return shell;
    }

    private Label createEventTimeLabel(ActivityLogEntry entry) {
        Label time = new Label(formatEventTime(entry));
        time.getStyleClass().add("logs-event-time");
        time.setMinWidth(66);
        time.setPrefWidth(66);
        time.setMaxWidth(66);
        return time;
    }

    private VBox createEventCopy(ActivityLogEntry entry) {
        String sentence = eventSentence(entry);
        String preview = eventPreview(entry);

        Label title = new Label(sentence);
        title.getStyleClass().add("logs-event-title");
        title.setWrapText(false);
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        title.setTooltip(new Tooltip(sentence));

        Label meta = new Label(preview);
        meta.getStyleClass().add("logs-event-meta");
        meta.setWrapText(false);
        meta.setTextOverrun(OverrunStyle.ELLIPSIS);
        meta.setTooltip(new Tooltip(preview));

        VBox copy = new VBox(3, title, meta);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);
        return copy;
    }

    private Button createLoadMoreButton() {
        Button button = new Button("Load more events ↓");
        button.getStyleClass().add("logs-load-more-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFocusTraversable(false);
        button.setDisable(true);
        return button;
    }

    private List<Node> createPayloadContent(ActivityLogEntry entry) {
        List<Node> nodes = new ArrayList<>();

        String area = normalize(displayArea(entry));
        boolean error = isError(entry);

        if (error && (area.equals("import") || isTiffActivity(entry))) {
            nodes.addAll(renderImportFailurePayload(entry));
        } else if (area.equals("users")) {
            nodes.addAll(renderUserPayload(entry));
        } else if (area.equals("qa")) {
            nodes.addAll(renderQaPayload(entry));
        } else if (area.equals("exports")) {
            nodes.addAll(renderExportPayload(entry));
        } else if (area.equals("import")) {
            nodes.addAll(renderImportSuccessPayload(entry));
        } else if (area.equals("profiles") || area.equals("metadata") || area.equals("access") || area.equals("security")) {
            nodes.addAll(renderChangePayload(entry));
        } else if (isTiffActivity(entry)) {
            nodes.addAll(renderTiffPayload(entry));
        } else {
            nodes.addAll(renderGenericPayload(entry));
        }

        return nodes;
    }

    private HBox createPayloadHeader() {
        Label title = new Label("Selected payload");
        title.getStyleClass().add("logs-payload-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label close = new Label("×");
        close.getStyleClass().add("logs-payload-close");

        HBox header = new HBox(9, title, spacer, close);
        header.getStyleClass().add("logs-payload-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private List<Node> renderTiffPayload(ActivityLogEntry entry) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(createPayloadTag(tiffPayloadTag(entry), "logs-payload-tag-files"));
        nodes.add(createPayloadHero(
                DOCUMENT_ICON_PATH,
                displayTiffItem(entry),
                tiffActorLine(entry),
                entry,
                tiffPayloadMessage(entry)
        ));

        List<ActivityDetailRow> metrics = tiffMetricRows(entry);
        if (!metrics.isEmpty()) {
            nodes.add(createSectionTitle(tiffSectionTitle(entry)));
            nodes.add(createMetricGrid(metrics));
        }

        List<ActivityDetailRow> traceRows = storageTraceRows(entry);
        if (!traceRows.isEmpty()) {
            nodes.add(createSection("Storage & trace", traceRows));
        }

        return nodes;
    }

    private List<Node> renderImportFailurePayload(ActivityLogEntry entry) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(createPayloadTag("Import failure", "logs-payload-tag-failed"));
        nodes.add(createAlertHero(
                "Unsupported TIFF compression",
                displayText(entry.description(), "The TIFF import could not be completed."),
                "Please re-export the file using a supported TIFF compression."
        ));

        List<ActivityDetailRow> rows = new ArrayList<>();
        rows.add(new ActivityDetailRow("Batch file", displayTiffItem(entry)));
        rows.addAll(normalizedContextRows(entry));

        if (rows.stream().noneMatch(row -> normalize(row.label()).contains("accepted"))) {
            rows.add(new ActivityDetailRow("Accepted", "LZW, PackBits, Uncompressed"));
        }

        rows.add(new ActivityDetailRow("Action needed", "Re-export TIFF using supported compression"));
        nodes.add(createSection("Failure summary", rows));

        List<ActivityDetailRow> traceRows = storageTraceRows(entry);
        if (!traceRows.isEmpty()) {
            nodes.add(createSection("Storage & trace", traceRows));
        }

        return nodes;
    }

    private String tiffPayloadTag(ActivityLogEntry entry) {
        String action = normalize(formatAction(entry.action()));

        if (isDeleteEvent(entry)) {
            return "TIFF deletion";
        }

        if (action.contains("replaced") || action.contains("retry")) {
            return "TIFF replacement";
        }

        if (action.contains("scan") || action.contains("upload") || action.contains("fetched")) {
            return "TIFF Upload";
        }

        return "TIFF activity";
    }

    private String tiffActorLine(ActivityLogEntry entry) {
        String action = normalize(formatAction(entry.action()));
        String actor = displayActor(entry.actor());

        if (isDeleteEvent(entry)) {
            return "Deleted by " + actor;
        }

        if (action.contains("replaced") || action.contains("retry")) {
            return "Replaced by " + actor;
        }

        if (action.contains("scan") || action.contains("upload") || action.contains("fetched")) {
            return "Uploaded by " + actor;
        }

        return "Handled by " + actor;
    }

    private String tiffPayloadMessage(ActivityLogEntry entry) {
        String action = normalize(formatAction(entry.action()));

        if (!displayText(entry.description(), "").isBlank()) {
            return entry.description();
        }

        if (isDeleteEvent(entry)) {
            return "TIFF snapshot preserved for audit.";
        }

        if (action.contains("replaced") || action.contains("retry")) {
            return "TIFF replacement recorded successfully.";
        }

        if (action.contains("scan") || action.contains("upload") || action.contains("fetched")) {
            return "TIFF uploaded successfully.";
        }

        return "TIFF activity recorded successfully.";
    }

    private String tiffSectionTitle(ActivityLogEntry entry) {
        String action = normalize(formatAction(entry.action()));

        if (isDeleteEvent(entry)) {
            return "Deleted TIFF snapshot";
        }

        if (action.contains("replaced") || action.contains("retry")) {
            return "Replacement details";
        }

        return "TIFF details";
    }

    private List<Node> renderUserPayload(ActivityLogEntry entry) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(createPayloadTag(userPayloadTag(entry), "logs-payload-tag-users"));
        nodes.add(createPayloadHero(
                USER_ICON_PATH,
                displayText(entry.target(), "User account"),
                userActorLine(entry),
                entry,
                displayText(entry.description(), "User account updated.")
        ));

        if (!entry.changes().isEmpty()) {
            nodes.add(createSectionTitle("Changes overview"));
            nodes.add(createComparisonCard(entry.changes()));
        } else {
            nodes.add(createEmptyPayloadState("No field-level account changes were recorded for this event."));
        }

        List<ActivityDetailRow> rows = userTraceRows(entry);
        if (!rows.isEmpty()) {
            nodes.add(createSection("Affected account & trace", rows));
        }

        return nodes;
    }

    private String userPayloadTag(ActivityLogEntry entry) {
        if (isCreateEvent(entry)) {
            return "User created";
        }

        if (isDeleteEvent(entry)) {
            return "User removed";
        }

        return "User update";
    }

    private String userActorLine(ActivityLogEntry entry) {
        String actor = displayActor(entry.actor());

        if (isCreateEvent(entry)) {
            return "Account created by " + actor;
        }

        if (isDeleteEvent(entry)) {
            return "Account removed by " + actor;
        }

        return "Account updated by " + actor;
    }

    private List<Node> renderQaPayload(ActivityLogEntry entry) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(createPayloadTag(isError(entry) ? "QA rejection" : "QA approval", "logs-payload-tag-qa"));
        nodes.add(createPayloadHero(
                CHECK_ICON_PATH,
                displayTiffItem(entry),
                "Reviewed by " + displayActor(entry.actor()),
                entry,
                displayText(entry.description(), isError(entry) ? "TIFF needs correction." : "TIFF approved for workflow.")
        ));

        List<ActivityDetailRow> rows = eventDetailRows(entry);
        if (!rows.isEmpty()) {
            nodes.add(createSection(isError(entry) ? "QA decision" : "QA evidence", rows));
        }

        nodes.add(createSection("Trace", evidenceRows(entry)));
        return nodes;
    }

    private List<Node> renderExportPayload(ActivityLogEntry entry) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(createPayloadTag("Export", "logs-payload-tag-files"));
        nodes.add(createPayloadHero(
                DOWNLOAD_ICON_PATH,
                displayTiffItem(entry),
                "Exported by " + displayActor(entry.actor()),
                entry,
                displayText(entry.description(), "Export event completed.")
        ));

        List<ActivityDetailRow> rows = eventDetailRows(entry);
        if (!rows.isEmpty()) {
            nodes.add(createSection("Export details", rows));
        }

        nodes.add(createSection("Trace", evidenceRows(entry)));
        return nodes;
    }

    private List<Node> renderImportSuccessPayload(ActivityLogEntry entry) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(createPayloadTag("Import", "logs-payload-tag-files"));
        nodes.add(createPayloadHero(
                UPLOAD_ICON_PATH,
                displayTiffItem(entry),
                "Imported by " + displayActor(entry.actor()),
                entry,
                displayText(entry.description(), "TIFF import completed.")
        ));

        List<ActivityDetailRow> rows = eventDetailRows(entry);
        if (!rows.isEmpty()) {
            nodes.add(createSection("Import result", rows));
        }

        nodes.add(createSection("Trace", evidenceRows(entry)));
        return nodes;
    }

    private List<Node> renderChangePayload(ActivityLogEntry entry) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(createPayloadTag(displayArea(entry), "logs-payload-tag-generic"));
        nodes.add(createPayloadHero(
                eventIconPath(entry),
                displayText(entry.target(), displayArea(entry)),
                "Changed by " + displayActor(entry.actor()),
                entry,
                displayText(entry.description(), formatAction(entry.action()))
        ));

        if (!entry.changes().isEmpty()) {
            nodes.add(createSectionTitle(changeSectionTitle(entry)));
            nodes.add(createComparisonCard(entry.changes()));
        } else {
            List<ActivityDetailRow> rows = eventDetailRows(entry);
            nodes.add(rows.isEmpty()
                    ? createEmptyPayloadState("No additional audit payload was recorded for this event.")
                    : createSection("Details", rows));
        }

        nodes.add(createSection("Trace", evidenceRows(entry)));
        return nodes;
    }

    private List<Node> renderGenericPayload(ActivityLogEntry entry) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(createPayloadTag(displayArea(entry), "logs-payload-tag-generic"));
        nodes.add(createPayloadHero(
                eventIconPath(entry),
                displayText(entry.target(), displayArea(entry)),
                "Recorded by " + displayActor(entry.actor()),
                entry,
                displayText(entry.description(), formatAction(entry.action()))
        ));

        List<ActivityDetailRow> rows = eventDetailRows(entry);
        nodes.add(rows.isEmpty()
                ? createEmptyPayloadState("No additional audit payload was recorded for this event.")
                : createSection("Details", rows));
        nodes.add(createSection("Trace", evidenceRows(entry)));
        return nodes;
    }

    private Label createPayloadTag(String text, String styleClass) {
        Label tag = new Label(text);
        tag.getStyleClass().add("logs-payload-tag");
        tag.getStyleClass().add(styleClass);
        tag.setMinWidth(Region.USE_PREF_SIZE);
        tag.setMaxWidth(Region.USE_PREF_SIZE);
        return tag;
    }

    private HBox createPayloadHero(String iconPath, String title, String subtitle, ActivityLogEntry entry, String message) {
        StackPane preview = createPayloadPreview(iconPath, isError(entry));

        Label titleLabel = new Label(displayText(title, "Selected event"));
        titleLabel.getStyleClass().add("logs-payload-hero-title");
        titleLabel.setWrapText(true);

        Label subtitleLabel = new Label(displayText(subtitle, ""));
        subtitleLabel.getStyleClass().add("logs-payload-hero-subtitle");
        subtitleLabel.setWrapText(true);

        HBox statusLine = new HBox(6, createStatusBadge(entry.status()), createHeroMessage(message));
        statusLine.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox(6, titleLabel, subtitleLabel, statusLine);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox hero = new HBox(15, preview, copy);
        hero.getStyleClass().add("logs-payload-hero");
        hero.setAlignment(Pos.CENTER_LEFT);
        return hero;
    }

    private Label createHeroMessage(String text) {
        Label message = new Label(displayText(text, ""));
        message.getStyleClass().add("logs-payload-hero-message");
        message.setWrapText(true);
        return message;
    }

    private StackPane createPayloadPreview(String iconPath, boolean failed) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add("logs-file-preview-icon");

        StackPane preview = new StackPane(icon);
        preview.getStyleClass().add("logs-file-preview");

        if (USER_ICON_PATH.equals(iconPath)) {
            preview.getStyleClass().add("logs-file-preview-user");
        } else if (CHECK_ICON_PATH.equals(iconPath)) {
            preview.getStyleClass().add("logs-file-preview-success");
        }

        if (failed) {
            preview.getStyleClass().add("logs-file-preview-failed");
        }

        return preview;
    }

    private VBox createAlertHero(String title, String message, String action) {
        SVGPath icon = new SVGPath();
        icon.setContent(WARNING_ICON_PATH);
        icon.getStyleClass().add("logs-alert-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("logs-alert-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("logs-alert-copy");
        messageLabel.setWrapText(true);

        Label actionLabel = new Label(action);
        actionLabel.getStyleClass().add("logs-alert-action");
        actionLabel.setWrapText(true);

        VBox copy = new VBox(6, titleLabel, messageLabel, actionLabel);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox content = new HBox(15, icon, copy);
        content.setAlignment(Pos.CENTER_LEFT);

        VBox hero = new VBox(content);
        hero.getStyleClass().add("logs-alert-hero");
        return hero;
    }

    private Label createSectionTitle(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("logs-section-title");
        return label;
    }

    private VBox createSection(String title, List<ActivityDetailRow> rows) {
        VBox section = new VBox(9);
        section.getStyleClass().add("logs-summary-list");
        section.setMaxWidth(Double.MAX_VALUE);
        section.getChildren().add(createSectionTitle(title));

        rows.stream()
                .filter(row -> !isMissingAuditValue(row.value()))
                .forEach(row -> section.getChildren().add(createSummaryRow(row.label(), row.value())));

        return section;
    }

    private HBox createSummaryRow(String label, String value) {
        Label labelNode = new Label(displayText(label, "Detail"));
        labelNode.getStyleClass().add("logs-summary-label");
        labelNode.setMinWidth(126);
        labelNode.setPrefWidth(126);

        Label valueNode = new Label(displayAuditValue(value));
        valueNode.getStyleClass().add("logs-summary-value");
        valueNode.setWrapText(true);
        valueNode.setTooltip(new Tooltip(displayAuditValue(value)));
        HBox.setHgrow(valueNode, Priority.ALWAYS);

        HBox row = new HBox(12, labelNode, valueNode);
        row.getStyleClass().add("logs-summary-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private GridPane createMetricGrid(List<ActivityDetailRow> rows) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("logs-metric-grid");
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setMaxWidth(Double.MAX_VALUE);

        List<ActivityDetailRow> visibleRows = rows.stream()
                .filter(row -> !isMissingAuditValue(row.value()))
                .limit(6)
                .toList();

        for (int index = 0; index < visibleRows.size(); index++) {
            ActivityDetailRow row = visibleRows.get(index);
            grid.add(createMetricCard(row.label(), row.value()), index % 3, index / 3);
        }

        return grid;
    }

    private VBox createMetricCard(String label, String value) {
        Label labelNode = new Label(displayText(label, "Detail"));
        labelNode.getStyleClass().add("logs-metric-label");

        Label valueNode = new Label(displayAuditValue(value));
        valueNode.getStyleClass().add("logs-metric-value");
        valueNode.setWrapText(true);
        valueNode.setTooltip(new Tooltip(displayAuditValue(value)));

        VBox card = new VBox(6, labelNode, valueNode);
        card.getStyleClass().add("logs-metric-card");
        card.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox createComparisonCard(List<ActivityChange> changes) {
        VBox card = new VBox(0);
        card.getStyleClass().add("logs-comparison-card");
        card.setFillWidth(true);
        card.setPrefWidth(COMPARISON_WIDTH);
        card.setMaxWidth(COMPARISON_WIDTH);

        List<ActivityChange> visibleChanges = changes.stream()
                .filter(this::hasVisibleChange)
                .toList();

        if (!visibleChanges.isEmpty()) {
            card.getChildren().add(createComparisonHeader());
        }

        visibleChanges.forEach(change -> card.getChildren().add(createComparisonRow(change)));

        if (card.getChildren().isEmpty()) {
            card.getChildren().add(createEmptyPayloadState("No field-level changes were recorded for this event."));
        }

        return card;
    }

    private HBox createComparisonHeader() {
        Label field = new Label("Field");
        field.getStyleClass().add("logs-comparison-heading");
        field.setMinWidth(132);
        field.setPrefWidth(132);

        Label before = new Label("Before");
        before.getStyleClass().addAll("logs-comparison-heading", "logs-comparison-heading-before");
        before.setMinWidth(135);
        before.setPrefWidth(135);

        Label arrow = new Label("");
        arrow.setMinWidth(18);
        arrow.setPrefWidth(18);

        Label after = new Label("After");
        after.getStyleClass().addAll("logs-comparison-heading", "logs-comparison-heading-after");
        after.setMinWidth(135);
        after.setPrefWidth(135);

        HBox row = new HBox(12, field, before, arrow, after);
        row.getStyleClass().add("logs-comparison-header-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox createComparisonRow(ActivityChange change) {
        Label field = new Label(displayText(change.field(), "Field"));
        field.getStyleClass().add("logs-comparison-field");
        field.setMinWidth(132);
        field.setPrefWidth(132);

        Label before = new Label(displayAuditValue(change.oldValue()));
        before.getStyleClass().add("logs-before-pill");
        before.setMinWidth(135);
        before.setPrefWidth(135);

        Label arrow = new Label("→");
        arrow.setText("\u2192");
        arrow.getStyleClass().add("logs-comparison-arrow");
        arrow.setMinWidth(18);
        arrow.setPrefWidth(18);

        Label after = new Label(displayAuditValue(change.newValue()));
        after.getStyleClass().add("logs-after-pill");
        after.setMinWidth(135);
        after.setPrefWidth(135);

        HBox row = new HBox(12, field, before, arrow, after);
        row.getStyleClass().add("logs-comparison-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox createEmptyPayloadState(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("logs-payload-empty");
        label.setWrapText(true);

        VBox state = new VBox(label);
        state.getStyleClass().add("logs-payload-empty-box");
        return state;
    }

    private Label createAreaBadge(ActivityLogEntry entry) {
        Label badge = new Label(rowAreaChipText(entry));
        badge.getStyleClass().add("logs-area-chip");
        badge.getStyleClass().add(areaBadgeClass(entry));
        badge.setMinWidth(Region.USE_PREF_SIZE);
        badge.setPrefWidth(Region.USE_COMPUTED_SIZE);
        badge.setMaxWidth(Region.USE_PREF_SIZE);
        return badge;
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(displayStatus(status));
        badge.getStyleClass().add("logs-result-pill");
        badge.getStyleClass().add(statusBadgeClass(status));
        badge.setMinWidth(Region.USE_PREF_SIZE);
        badge.setPrefWidth(Region.USE_COMPUTED_SIZE);
        badge.setMaxWidth(Region.USE_PREF_SIZE);
        return badge;
    }

    private List<ActivityDetailRow> tiffMetricRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        addFirstDetail(rows, entry, "File size", "file size", "size");
        addFirstDetail(rows, entry, "Pages", "pages", "page count", "pages found", "page");
        addFirstDetail(rows, entry, "Resolution", "resolution", "dpi");
        addFirstDetail(rows, entry, "Dimensions", "dimensions");
        addFirstDetail(rows, entry, "Compression", "compression");
        addFirstDetail(rows, entry, "Checksum", "checksum");

        if (!rows.isEmpty()) {
            return rows;
        }

        return normalizedContextRows(entry).stream()
                .filter(row -> !List.of("TIFF file", "File").contains(row.label()))
                .limit(6)
                .toList();
    }

    private List<ActivityDetailRow> storageTraceRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        addFirstDetail(rows, entry, "Box", "box", "box id");
        addFirstDetail(rows, entry, "File ID", "file id", "file");
        addFirstDetail(rows, entry, "Path", "path", "storage path");
        rows.add(new ActivityDetailRow("Trace", "LOG-" + entry.id() + " · " + entry.fullTimestamp()));
        return rows;
    }

    private List<ActivityDetailRow> userTraceRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        String userId = targetId(entry);

        if (hasRealTargetId(userId)) {
            rows.add(new ActivityDetailRow("User ID", userId));
        }

        addFirstDetail(rows, entry, "Email", "email", "email address");
        rows.add(new ActivityDetailRow("Trace", "LOG-" + entry.id() + " · " + entry.fullTimestamp()));
        return rows;
    }

    private void addFirstDetail(List<ActivityDetailRow> rows, ActivityLogEntry entry, String displayLabel, String... labels) {
        String value = firstContextValue(entry, labels);

        if (!value.isBlank()) {
            rows.add(new ActivityDetailRow(displayLabel, value));
        }
    }

    private List<ActivityDetailRow> eventDetailRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> contextRows = normalizedContextRows(entry);

        if (isError(entry)) {
            List<ActivityDetailRow> rows = new ArrayList<>();
            rows.add(new ActivityDetailRow("Failure reason", displayText(entry.description(), "No failure reason recorded.")));
            rows.addAll(contextRows);
            return rows;
        }

        if (entry.changes().isEmpty()) {
            if (!contextRows.isEmpty()) {
                return contextRows;
            }

            String description = displayText(entry.description(), "");
            return description.isBlank()
                    ? List.of()
                    : List.of(new ActivityDetailRow("Note", description));
        }

        if (isCreateEvent(entry)) {
            return entry.changes().stream()
                    .filter(change -> !isMissingAuditValue(change.newValue()))
                    .map(change -> new ActivityDetailRow(change.field(), change.newValue()))
                    .toList();
        }

        if (isDeleteEvent(entry)) {
            return entry.changes().stream()
                    .filter(change -> !isStateSnapshotField(change.field()))
                    .filter(change -> !isMissingAuditValue(change.oldValue()))
                    .map(change -> new ActivityDetailRow(change.field(), change.oldValue()))
                    .toList();
        }

        return entry.changes().stream()
                .map(change -> new ActivityDetailRow(
                        change.field(),
                        displayAuditValue(change.oldValue()) + " → " + displayAuditValue(change.newValue())
                ))
                .toList();
    }

    private List<ActivityDetailRow> normalizedContextRows(ActivityLogEntry entry) {
        return entry.contextDetails().stream()
                .filter(row -> !isMissingAuditValue(row.value()))
                .map(row -> new ActivityDetailRow(normalizeDetailLabel(row.label()), row.value()))
                .toList();
    }

    private List<ActivityDetailRow> evidenceRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        rows.add(new ActivityDetailRow("Log ID", "LOG-" + entry.id()));

        String targetId = targetId(entry);
        if (hasRealTargetId(targetId)) {
            rows.add(new ActivityDetailRow("Record ID", targetId));
        }

        rows.add(new ActivityDetailRow("Recorded", entry.fullTimestamp()));

        String trigger = meaningfulTrigger(entry);
        if (!trigger.isBlank()) {
            rows.add(new ActivityDetailRow("Source", trigger));
        }

        return rows;
    }

    private String normalizeDetailLabel(String label) {
        String normalizedLabel = normalize(label);

        return switch (normalizedLabel) {
            case "file", "file id", "filename", "file name" -> "TIFF file";
            case "box", "box id" -> "Box";
            case "case", "case id" -> "Case";
            case "document", "document id" -> "Document";
            case "page", "page number" -> "Page";
            case "profile", "profile name" -> "Scan profile";
            default -> displayText(label, "Detail");
        };
    }

    private boolean hasVisibleChange(ActivityChange change) {
        return !isMissingAuditValue(change.oldValue()) || !isMissingAuditValue(change.newValue());
    }

    private boolean isMissingAuditValue(String value) {
        return value == null || value.trim().isBlank() || "null".equalsIgnoreCase(value.trim());
    }

    private boolean isStateSnapshotField(String field) {
        String normalizedField = normalize(field);
        return "account state".equals(normalizedField)
                || "profile state".equals(normalizedField)
                || "template state".equals(normalizedField);
    }

    private boolean isCreateEvent(ActivityLogEntry entry) {
        String action = normalize(formatAction(entry.action()));
        return action.contains("created") || action.startsWith("create ");
    }

    private boolean isDeleteEvent(ActivityLogEntry entry) {
        String action = normalize(formatAction(entry.action()));
        return action.contains("deleted") || action.startsWith("delete ");
    }

    private String targetId(ActivityLogEntry entry) {
        String detailId = firstContextValue(
                entry,
                "file",
                "file id",
                "document",
                "document id",
                "box",
                "box id",
                "case",
                "case id",
                "user id"
        );

        if (!detailId.isBlank()) {
            return detailId;
        }

        String target = displayAuditValue(entry.target());

        Matcher matcher = TARGET_ID_PATTERN.matcher(target);
        return matcher.find() ? matcher.group() : "—";
    }

    private boolean hasRealTargetId(String targetId) {
        if (isMissingAuditValue(targetId) || "—".equals(targetId.trim())) {
            return false;
        }

        String cleanedValue = targetId.trim();

        return TARGET_ID_PATTERN.matcher(cleanedValue).matches()
                || cleanedValue.matches("\\d+")
                || (!cleanedValue.contains(".") && cleanedValue.matches("[A-Za-z0-9_-]{3,}"));
    }

    private String meaningfulTrigger(ActivityLogEntry entry) {
        String action = normalize(entry.action());
        String type = normalize(entry.type());

        if (!isSystemActor(entry.actor())) {
            return "";
        }

        if (action.contains("import")) {
            return "Import";
        }

        if (action.contains("api")) {
            return "API";
        }

        if (action.contains("scheduled")) {
            return "Scheduled task";
        }

        if (type.contains("scan") || action.contains("scan")) {
            return "Background scan";
        }

        return "";
    }

    private String displayAuditValue(String value) {
        if (isMissingAuditValue(value)) {
            return "—";
        }

        String cleanedValue = value.trim();

        if ("true".equalsIgnoreCase(cleanedValue)) {
            return "Yes";
        }

        if ("false".equalsIgnoreCase(cleanedValue)) {
            return "No";
        }

        return cleanedValue;
    }

    private String groupLabel(ActivityLogEntry entry) {
        LocalDateTime timestamp = parseActivityTimestamp(entry);

        if (timestamp == null) {
            return "Earlier";
        }

        LocalDate date = timestamp.toLocalDate();
        LocalDate today = LocalDate.now();

        if (date.equals(today)) {
            return "Today";
        }

        if (date.equals(today.minusDays(1))) {
            return "Yesterday";
        }

        return GROUP_DATE_FORMATTER.format(date);
    }

    private String formatEventTime(ActivityLogEntry entry) {
        LocalDateTime timestamp = parseActivityTimestamp(entry);

        return timestamp == null
                ? displayAuditValue(entry.timestamp())
                : ROW_TIME_FORMATTER.format(timestamp);
    }

    private String eventSentence(ActivityLogEntry entry) {
        String area = eventPrefix(entry);
        String actor = shortActor(entry.actor());
        String action = lowerFirst(formatAction(entry.action()));
        String item = displayTiffItem(entry);

        if (item.isBlank() || "—".equals(item)) {
            return "[" + area + "] " + actor + " " + action;
        }

        return "[" + area + "] " + actor + " " + action + " " + item;
    }

    private String shortActor(String actor) {
        String displayActor = displayActor(actor);

        if (normalize(displayActor).contains("admin")) {
            return "Admin";
        }

        if (displayActor.contains(" ")) {
            return displayActor.split("\\s+")[0];
        }

        if (displayActor.contains("_")) {
            return displayActor.replace("_", " ");
        }

        return displayActor;
    }

    private String eventPrefix(ActivityLogEntry entry) {
        String area = displayArea(entry);
        return "Files".equals(area) ? "TIFF" : area;
    }

    private String eventPreview(ActivityLogEntry entry) {
        if (isError(entry) && !displayText(entry.description(), "").isBlank()) {
            return entry.description();
        }

        if (isTiffActivity(entry)) {
            String fileSize = firstContextValue(entry, "file size", "size");
            String pages = firstContextValue(entry, "pages", "page count", "page");
            String resolution = firstContextValue(entry, "resolution", "dpi");
            List<String> parts = new ArrayList<>();

            if (!fileSize.isBlank()) {
                parts.add(fileSize);
            }

            if (!pages.isBlank()) {
                parts.add(pages.toLowerCase(Locale.ROOT).contains("page") ? pages : pages + " pages");
            }

            if (!resolution.isBlank()) {
                parts.add(resolution.toLowerCase(Locale.ROOT).contains("dpi") ? resolution : resolution + " DPI");
            }

            if (!parts.isEmpty()) {
                return String.join(" · ", parts);
            }
        }

        if (!entry.changes().isEmpty()) {
            return entry.changes().size() == 1
                    ? entry.changes().get(0).field() + " changed"
                    : entry.changes().size() + " fields changed";
        }

        return displayText(entry.description(), displayText(entry.target(), "Audit event"));
    }

    private String rowAreaChipText(ActivityLogEntry entry) {
        return isTiffActivity(entry) ? "TIFF" : displayArea(entry);
    }

    private String eventIconPath(ActivityLogEntry entry) {
        String area = normalize(displayArea(entry));
        String action = normalize(entry.action());

        if (isError(entry)) {
            return WARNING_ICON_PATH;
        }

        if (area.equals("users") || area.equals("access") || area.equals("security")) {
            return USER_ICON_PATH;
        }

        if (area.equals("qa")) {
            return CHECK_ICON_PATH;
        }

        if (area.equals("exports")) {
            return DOWNLOAD_ICON_PATH;
        }

        if (action.contains("replace") || action.contains("retry")) {
            return REFRESH_ICON_PATH;
        }

        if (isTiffActivity(entry) || area.equals("import")) {
            return UPLOAD_ICON_PATH;
        }

        return GEAR_ICON_PATH;
    }

    private String eventIconClass(ActivityLogEntry entry) {
        String area = normalize(displayArea(entry));

        return switch (area) {
            case "users" -> "logs-event-icon-users";
            case "profiles" -> "logs-event-icon-profiles";
            case "access", "security" -> "logs-event-icon-access";
            case "qa" -> "logs-event-icon-qa";
            case "exports", "files", "import" -> "logs-event-icon-files";
            case "metadata" -> "logs-event-icon-metadata";
            default -> "logs-event-icon-system";
        };
    }

    private String displayTiffItem(ActivityLogEntry entry) {
        String fileFromDetails = firstContextValue(entry, "file", "file id", "filename", "file name");

        if (!fileFromDetails.isBlank()) {
            return fileFromDetails;
        }

        String target = displayAuditValue(entry.target());

        for (String part : target.split("/")) {
            String cleanedPart = part.trim();
            String normalizedPart = normalize(cleanedPart);

            if (normalizedPart.startsWith("file ")) {
                return displayText(cleanedPart.substring(5), target);
            }

            if (normalizedPart.endsWith(".tif") || normalizedPart.endsWith(".tiff")) {
                return cleanedPart;
            }
        }

        return target;
    }

    private String firstContextValue(ActivityLogEntry entry, String... labels) {
        List<String> normalizedLabels = List.of(labels).stream()
                .map(this::normalize)
                .toList();

        return entry.contextDetails().stream()
                .filter(row -> normalizedLabels.contains(normalize(row.label())))
                .map(ActivityDetailRow::value)
                .filter(value -> !isMissingAuditValue(value))
                .findFirst()
                .orElse("");
    }

    private String displayArea(ActivityLogEntry entry) {
        String normalizedType = normalize(entry.type());
        String normalizedAction = normalize(entry.action());

        if (normalizedAction.contains("import")) {
            return "Import";
        }

        if ("review".equals(normalizedType)) {
            return "QA";
        }

        if (isTiffActivity(entry)) {
            return "Files";
        }

        return displayText(entry.type(), "System");
    }

    private String areaBadgeClass(ActivityLogEntry entry) {
        String normalizedType = normalize(displayArea(entry));

        return switch (normalizedType) {
            case "users" -> "logs-area-users";
            case "profiles" -> "logs-area-profiles";
            case "access", "security" -> "logs-area-access";
            case "metadata", "documents" -> "logs-area-metadata";
            case "files", "scans", "exports" -> "logs-area-files";
            case "qa" -> "logs-area-qa";
            case "import" -> isError(entry) ? "logs-area-import-failed" : "logs-area-import";
            default -> "logs-area-system";
        };
    }

    private boolean isTiffActivity(ActivityLogEntry entry) {
        String type = normalize(entry.type());
        String action = normalize(entry.action());
        String target = normalize(entry.target());

        return type.contains("scan")
                || type.contains("document")
                || action.contains("scan")
                || action.contains("tiff")
                || action.contains("page")
                || action.contains("barcode")
                || target.contains(".tif")
                || target.contains(".tiff")
                || !firstContextValue(entry, "file", "file id", "filename", "file name").isBlank();
    }

    private boolean matchesSearch(ActivityLogEntry entry) {
        String searchText = normalize(searchField == null ? "" : searchField.getText());

        if (searchText.isBlank()) {
            return true;
        }

        return normalize(displayArea(entry)).contains(searchText)
                || normalize(entry.type()).contains(searchText)
                || normalize(entry.actor()).contains(searchText)
                || normalize(entry.action()).contains(searchText)
                || normalize(entry.target()).contains(searchText)
                || normalize(entry.status()).contains(searchText)
                || normalize(entry.description()).contains(searchText)
                || normalize(entry.fullTimestamp()).contains(searchText)
                || entry.changes().stream().anyMatch(change ->
                normalize(change.field()).contains(searchText)
                        || normalize(change.oldValue()).contains(searchText)
                        || normalize(change.newValue()).contains(searchText)
        ) || entry.contextDetails().stream().anyMatch(row ->
                normalize(row.label()).contains(searchText)
                        || normalize(row.searchText()).contains(searchText)
        );
    }

    private boolean matchesFilters(ActivityLogEntry entry) {
        return matchesArea(entry)
                && matchesUser(entry)
                && matchesStatus(entry)
                && matchesDateFilter(entry);
    }

    private boolean matchesArea(ActivityLogEntry entry) {
        String selectedArea = comboValue(typeFilterComboBox);

        return selectedArea.isBlank()
                || ALL_AREAS.equals(selectedArea)
                || displayArea(entry).equalsIgnoreCase(selectedArea)
                || displayText(entry.type(), "").equalsIgnoreCase(selectedArea);
    }

    private boolean matchesUser(ActivityLogEntry entry) {
        String selectedUser = comboValue(userFilterComboBox);

        return selectedUser.isBlank()
                || ALL_USERS.equals(selectedUser)
                || displayText(entry.actor(), "").equalsIgnoreCase(selectedUser);
    }

    private boolean matchesStatus(ActivityLogEntry entry) {
        String selectedStatus = comboValue(statusFilterComboBox);

        return selectedStatus.isBlank()
                || ALL_RESULTS.equals(selectedStatus)
                || ("Failed".equalsIgnoreCase(selectedStatus) && isError(entry))
                || displayStatus(entry.status()).equalsIgnoreCase(selectedStatus);
    }

    private String comboValue(ComboBox<String> comboBox) {
        return comboBox == null || comboBox.getValue() == null ? "" : comboBox.getValue();
    }

    private boolean matchesDateFilter(ActivityLogEntry entry) {
        LocalDateTime activityTime = parseActivityTimestamp(entry);

        if (activityTime == null) {
            return false;
        }

        if (dateFilterMode == DateFilterMode.ALL) {
            return true;
        }

        LocalDate activityDate = activityTime.toLocalDate();

        if (dateFilterMode == DateFilterMode.SPECIFIC) {
            return specificDate == null || activityDate.equals(specificDate);
        }

        boolean afterStart = rangeStartDate == null || !activityDate.isBefore(rangeStartDate);
        boolean beforeEnd = rangeEndDate == null || !activityDate.isAfter(rangeEndDate);

        return afterStart && beforeEnd;
    }

    private LocalDateTime parseActivityTimestamp(ActivityLogEntry entry) {
        try {
            return LocalDateTime.parse(entry.fullTimestamp(), ACTIVITY_TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException exception) {
            try {
                return LocalDateTime.parse(entry.fullTimestamp(), FALLBACK_ACTIVITY_TIMESTAMP_FORMATTER);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    @FXML
    private void clearFilters() {
        if (searchField != null) {
            searchField.clear();
        }

        if (typeFilterComboBox != null) {
            typeFilterComboBox.setValue(ALL_AREAS);
        }

        if (userFilterComboBox != null) {
            userFilterComboBox.setValue(ALL_USERS);
        }

        if (statusFilterComboBox != null) {
            statusFilterComboBox.setValue(ALL_RESULTS);
        }

        if (sortFilterComboBox != null) {
            sortFilterComboBox.setValue(SORT_NEWEST_FIRST);
        }

        updatingDateControls = true;

        dateFilterMode = DateFilterMode.ALL;
        specificDate = null;
        rangeStartDate = null;
        rangeEndDate = null;

        if (specificDatePicker != null) {
            specificDatePicker.setValue(null);
        }

        if (rangeStartDatePicker != null) {
            rangeStartDatePicker.setValue(null);
        }

        if (rangeEndDatePicker != null) {
            rangeEndDatePicker.setValue(null);
        }

        updatingDateControls = false;

        updateDateFilterState();

        selectedEntryId = null;
        renderTimeline();
    }

    @FXML
    private void showSpecificDateMode() {
        dateFilterMode = DateFilterMode.SPECIFIC;

        if (specificDatePicker != null && specificDatePicker.getValue() == null) {
            specificDatePicker.setValue(LocalDate.now());
        }

        syncDateValuesFromControls();
        updateDateFilterState();
    }

    @FXML
    private void showRangeDateMode() {
        dateFilterMode = DateFilterMode.RANGE;

        if (rangeStartDatePicker != null && rangeStartDatePicker.getValue() == null) {
            rangeStartDatePicker.setValue(LocalDate.now().minusDays(7));
        }

        if (rangeEndDatePicker != null && rangeEndDatePicker.getValue() == null) {
            rangeEndDatePicker.setValue(LocalDate.now());
        }

        syncDateValuesFromControls();
        updateDateFilterState();
    }

    @FXML
    private void applyDateFilter() {
        syncDateValuesFromControls();
        updateDateFilterButtonText();

        if (dateFilterMenuButton != null) {
            dateFilterMenuButton.hide();
        }

        refreshFilteredTimeline();
    }

    @FXML
    private void clearDateFilter() {
        updatingDateControls = true;

        dateFilterMode = DateFilterMode.ALL;
        specificDate = null;
        rangeStartDate = null;
        rangeEndDate = null;

        if (specificDatePicker != null) {
            specificDatePicker.setValue(null);
        }

        if (rangeStartDatePicker != null) {
            rangeStartDatePicker.setValue(null);
        }

        if (rangeEndDatePicker != null) {
            rangeEndDatePicker.setValue(null);
        }

        updatingDateControls = false;

        updateDateFilterState();
        refreshFilteredTimeline();
    }

    private void refreshFilteredTimeline() {
        renderTimeline();
    }

    private void handleDateInputChange() {
        if (updatingDateControls) {
            return;
        }

        syncDateValuesFromControls();
        updateDateFilterButtonText();
    }

    private void syncDateValuesFromControls() {
        specificDate = specificDatePicker == null ? null : specificDatePicker.getValue();
        rangeStartDate = rangeStartDatePicker == null ? null : rangeStartDatePicker.getValue();
        rangeEndDate = rangeEndDatePicker == null ? null : rangeEndDatePicker.getValue();
    }

    private void configureDatePickers() {
        configureDatePicker(specificDatePicker);
        configureDatePicker(rangeStartDatePicker);
        configureDatePicker(rangeEndDatePicker);
    }

    private void updateDateFilterState() {
        boolean specificMode = dateFilterMode == DateFilterMode.SPECIFIC;
        boolean rangeMode = dateFilterMode == DateFilterMode.RANGE;

        if (specificDateBox != null) {
            specificDateBox.setVisible(specificMode);
            specificDateBox.setManaged(specificMode);
        }

        if (dateRangeBox != null) {
            dateRangeBox.setVisible(rangeMode);
            dateRangeBox.setManaged(rangeMode);
        }

        if (specificDateModeButton != null) {
            specificDateModeButton.getStyleClass().remove("logs-date-mode-button-active");

            if (specificMode && !specificDateModeButton.getStyleClass().contains("logs-date-mode-button-active")) {
                specificDateModeButton.getStyleClass().add("logs-date-mode-button-active");
            }
        }

        if (rangeDateModeButton != null) {
            rangeDateModeButton.getStyleClass().remove("logs-date-mode-button-active");

            if (rangeMode && !rangeDateModeButton.getStyleClass().contains("logs-date-mode-button-active")) {
                rangeDateModeButton.getStyleClass().add("logs-date-mode-button-active");
            }
        }

        updateDateFilterButtonText();
    }

    private void updateDateFilterButtonText() {
        String displayValue;

        if (dateFilterMode == DateFilterMode.SPECIFIC && specificDate != null) {
            displayValue = GROUP_DATE_FORMATTER.format(specificDate);
        } else if (dateFilterMode == DateFilterMode.RANGE && rangeStartDate != null && rangeEndDate != null) {
            displayValue = DATE_RANGE_FORMATTER.format(rangeStartDate) + " – " + DATE_RANGE_FORMATTER.format(rangeEndDate);
        } else if (dateFilterMode == DateFilterMode.RANGE && rangeStartDate != null) {
            displayValue = "From " + DATE_RANGE_FORMATTER.format(rangeStartDate);
        } else if (dateFilterMode == DateFilterMode.RANGE && rangeEndDate != null) {
            displayValue = "Until " + DATE_RANGE_FORMATTER.format(rangeEndDate);
        } else {
            displayValue = "All dates";
        }

        setDateFilterButtonDisplay(displayValue);
    }

    private void setDateFilterButtonDisplay(String value) {
        if (dateFilterMenuButton == null) {
            return;
        }

        dateFilterMenuButton.setText(null);
        dateFilterMenuButton.setGraphic(createFilterGraphic(DATE_FILTER_ICON_PATH, "Date", value));
        dateFilterMenuButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        dateFilterMenuButton.setAccessibleText("Date " + value);
    }

    private void configureDatePicker(DatePicker picker) {
        if (picker == null) {
            return;
        }

        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return value == null ? "" : DATE_RANGE_FORMATTER.format(value);
            }

            @Override
            public LocalDate fromString(String value) {
                if (value == null || value.isBlank()) {
                    return null;
                }

                try {
                    return LocalDate.parse(value.trim(), DATE_RANGE_FORMATTER);
                } catch (DateTimeParseException exception) {
                    return null;
                }
            }
        });

        picker.setDayCellFactory(datePicker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                getStyleClass().removeAll("activity-log-date-disabled");

                if (!empty && date != null && date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    getStyleClass().add("activity-log-date-disabled");
                }
            }
        });
    }

    private List<ActivityLogEntry> filteredActivityEntries() {
        return activityEntries.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesFilters)
                .sorted(activitySortComparator())
                .toList();
    }

    private Comparator<ActivityLogEntry> activitySortComparator() {
        Comparator<LocalDateTime> timestampComparator = SORT_OLDEST_FIRST.equals(comboValue(sortFilterComboBox))
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();

        return Comparator.comparing(
                this::parseActivityTimestamp,
                Comparator.nullsLast(timestampComparator)
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String statusBadgeClass(String status) {
        return switch (normalize(status)) {
            case "success" -> "logs-result-success";
            case "failed", "error" -> "logs-result-failed";
            default -> "logs-result-info";
        };
    }

    private boolean isError(ActivityLogEntry entry) {
        String status = normalize(entry.status());

        return "failed".equals(status)
                || "error".equals(status)
                || normalize(entry.action()).contains("failed")
                || normalize(entry.action()).contains("rejected");
    }

    private String displayStatus(String status) {
        String normalizedStatus = normalize(status);

        return "failed".equals(normalizedStatus) || "error".equals(normalizedStatus)
                ? "Failed"
                : displayText(status, "Info");
    }

    private String displayActor(String actor) {
        return isSystemActor(actor) ? "System" : displayText(actor, "System");
    }

    private boolean isSystemActor(String actor) {
        String normalizedActor = normalize(actor);
        return normalizedActor.isBlank() || "system".equals(normalizedActor);
    }

    private String displayText(String value, String fallback) {
        String cleanedValue = value == null ? "" : value.trim();
        return cleanedValue.isBlank() ? fallback : cleanedValue;
    }

    private String lowerFirst(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text.substring(0, 1).toLowerCase(Locale.ROOT) + text.substring(1);
    }

    private String changeSectionTitle(ActivityLogEntry entry) {
        if (isCreateEvent(entry)) {
            return "Created values";
        }

        if (isDeleteEvent(entry)) {
            return "Deleted snapshot";
        }

        return "Changes overview";
    }

    private String formatAction(String action) {
        String cleanedAction = displayText(action, "Activity");
        String actionKey = cleanedAction.trim().replace(' ', '_').toUpperCase(Locale.ROOT);

        switch (actionKey) {
            case "METADATA_SAVED" -> {
                return "Saved metadata";
            }
            case "SCAN_STARTED" -> {
                return "Started TIFF scan";
            }
            case "TIFF_FETCHED" -> {
                return "Fetched TIFF";
            }
            case "SCAN_COMPLETED" -> {
                return "Completed TIFF scan";
            }
            case "SCAN_FAILED" -> {
                return "TIFF scan failed";
            }
            case "RETRY_USED" -> {
                return "Retried TIFF fetch";
            }
            case "PAGE_CREATED" -> {
                return "Created TIFF page";
            }
            case "PAGE_DELETED" -> {
                return "Deleted TIFF page";
            }
            case "EXPORT_PREVIEW_CREATED" -> {
                return "Created export preview";
            }
            case "BARCODE_DETECTED" -> {
                return "Detected barcode";
            }
            default -> {
            }
        }

        if (!cleanedAction.contains("_")) {
            return cleanedAction;
        }

        String[] words = cleanedAction.toLowerCase(Locale.ROOT).split("_+");
        List<String> formattedWords = new ArrayList<>();

        for (String word : words) {
            if (!word.isBlank()) {
                formattedWords.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
            }
        }

        return String.join(" ", formattedWords);
    }

    private void loadActivity() {
        if (adminManager == null) {
            activityEntries.clear();
            refreshUserFilterOptions();
            return;
        }

        activityEntries.setAll(
                adminManager.getAuditLogs().stream()
                        .map(this::toActivityLogEntry)
                        .toList()
        );

        refreshUserFilterOptions();
    }

    private ActivityLogEntry toActivityLogEntry(AuditLog log) {
        LocalDateTime timestamp = log.getTimestamp() == null ? LocalDateTime.now() : log.getTimestamp();

        return new ActivityLogEntry(
                String.valueOf(log.getId()),
                ROW_TIME_FORMATTER.format(timestamp),
                ACTIVITY_TIMESTAMP_FORMATTER.format(timestamp),
                displayText(log.getType(), "System"),
                displayText(log.getActor(), "System"),
                displayText(log.getAction(), "Activity"),
                displayText(log.getTarget(), ""),
                displayText(log.getStatus(), "Info"),
                displayText(log.getDescription(), ""),
                log.getDetails().stream()
                        .filter(detail -> !detail.isFieldChange())
                        .map(detail -> new ActivityDetailRow(detail.getLabel(), detail.getValue()))
                        .toList(),
                log.getDetails().stream()
                        .filter(AuditLog.AuditLogDetail::isFieldChange)
                        .map(detail -> new ActivityChange(detail.getLabel(), detail.getOldValue(), detail.getNewValue()))
                        .toList()
        );
    }

    private void refreshUserFilterOptions() {
        if (userFilterComboBox == null) {
            return;
        }

        String selectedUser = userFilterComboBox.getValue();

        List<String> actors = activityEntries.stream()
                .map(ActivityLogEntry::actor)
                .filter(actor -> !actor.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<String> filterOptions = new ArrayList<>();
        filterOptions.add(ALL_USERS);
        filterOptions.addAll(actors);

        userFilterComboBox.getItems().setAll(filterOptions);

        if (selectedUser != null && filterOptions.contains(selectedUser)) {
            userFilterComboBox.setValue(selectedUser);
        } else {
            userFilterComboBox.setValue(ALL_USERS);
        }
    }

    record ActivityLogEntry(
            String id,
            String timestamp,
            String fullTimestamp,
            String type,
            String actor,
            String action,
            String target,
            String status,
            String description,
            List<ActivityDetailRow> contextDetails,
            List<ActivityChange> changes
    ) {
    }

    record ActivityChange(String field, String oldValue, String newValue) {
    }

    record ActivityDetailRow(
            String label,
            String value,
            String oldValue,
            String newValue,
            boolean fieldChange
    ) {
        ActivityDetailRow(String label, String value) {
            this(label, value, "", "", false);
        }

        String searchText() {
            return String.join(" ", label, value, oldValue, newValue);
        }
    }
}
