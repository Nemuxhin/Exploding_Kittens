package easv.gui.controller.admin;

import easv.be.AuditLog;
import easv.bll.AdminManager;
import easv.gui.AppDates;
import easv.gui.PrimeIcons;
import easv.gui.SearchableComboBoxes;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    private static final double EXPANDED_RAIL_HORIZONTAL_OFFSET = 96;

    private static final String UPLOAD_ICON_PATH = "\ue934";
    private static final String CHECK_ICON_PATH = "\ue90a";
    private static final String WARNING_ICON_PATH = "\ue922";
    private static final String REFRESH_ICON_PATH = "\ue938";
    private static final String USER_ICON_PATH = "\ue939";
    private static final String GEAR_ICON_PATH = "\ue94a";
    private static final String DOWNLOAD_ICON_PATH = "\ue956";
    private static final String DOCUMENT_ICON_PATH = "\ue958";

    private static final String AREA_FILTER_ICON_PATH = "\ue941";
    private static final String RESULT_FILTER_ICON_PATH = "\ue90a";
    private static final String DATE_FILTER_ICON_PATH = "\ue927";
    private static final String SORT_FILTER_ICON_PATH = "\ue915";

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
            AppDates.FORMATTER;

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

    @FXML private VBox logsPageRoot;
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
                    "Documents",
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
        StackPane iconShell = new StackPane(createPrimeIcon(iconPath, "logs-filter-icon-path"));
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
        content.setMaxWidth(Double.MAX_VALUE);
        List<Node> payloadContent = createInlinePayloadContent(entry);
        content.getChildren().setAll(payloadContent);

        bindExpandedPayloadWidth(content, payloadContent);

        tray.getChildren().setAll(content);
        return tray;
    }

    private void bindExpandedPayloadWidth(Region content, List<Node> payloadContent) {
        if (logsPageRoot == null) {
            return;
        }

        NumberBinding availableWidth = Bindings.max(
                0,
                logsPageRoot.widthProperty().subtract(EXPANDED_RAIL_HORIZONTAL_OFFSET)
        );

        bindRegionWidth(content, availableWidth);

        payloadContent.stream()
                .filter(Region.class::isInstance)
                .map(Region.class::cast)
                .forEach(region -> bindRegionWidth(region, availableWidth));
    }

    private void bindRegionWidth(Region region, NumberBinding width) {
        region.minWidthProperty().bind(width);
        region.prefWidthProperty().bind(width);
        region.maxWidthProperty().bind(width);
    }

    private List<Node> createInlinePayloadContent(ActivityLogEntry entry) {
        HBox rail = createInlineRail(entry);

        if (rail == null) {
            return List.of();
        }

        StackPane railHolder = new StackPane(rail);
        railHolder.getStyleClass().add("logs-inline-rail-holder");
        railHolder.setAlignment(Pos.CENTER_LEFT);
        railHolder.setMinWidth(0);
        railHolder.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(rail, Pos.CENTER_LEFT);

        return List.of(railHolder);
    }

    private Label createPrimeIcon(String glyph, String styleClass) {
        return PrimeIcons.create(glyph, styleClass);
    }

    private Button createLoadMoreButton() {
        Button button = new Button("Load more events");
        button.getStyleClass().add("logs-load-more-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFocusTraversable(false);
        button.setDisable(true);
        return button;
    }

    private StackPane createEventIcon(ActivityLogEntry entry) {
        Label icon = createPrimeIcon(eventIconPath(entry), "logs-event-icon-path");

        StackPane shell = new StackPane(icon);
        shell.getStyleClass().add("logs-event-icon");
        shell.getStyleClass().add(isError(entry) ? "logs-event-icon-failed" : eventIconClass(entry));
        return shell;
    }

    private Label createEventTimeLabel(ActivityLogEntry entry) {
        Label time = new Label(formatEventTime(entry));
        time.getStyleClass().add("logs-event-time");
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
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);
        return copy;
    }

    private HBox createInlineRail(ActivityLogEntry entry) {
        List<Node> cells = createInlineRailCells(entry);

        if (cells.isEmpty()) {
            return null;
        }

        HBox rail = new HBox(0);
        rail.getStyleClass().add("logs-inline-rail");

        if (isError(entry)) {
            rail.getStyleClass().add("logs-inline-rail-failed");
        }

        for (int index = 0; index < cells.size(); index++) {
            if (index > 0) {
                Region divider = new Region();
                divider.getStyleClass().add("logs-inline-rail-divider");
                rail.getChildren().add(divider);
            }

            Node cell = cells.get(index);
            if (index == 0) {
                cell.getStyleClass().add("logs-inline-rail-cell-first");
            }
            rail.getChildren().add(cell);
        }

        rail.setAlignment(Pos.CENTER_LEFT);
        rail.setMinWidth(0);
        rail.setMaxWidth(Double.MAX_VALUE);
        return rail;
    }

    private List<Node> createInlineRailCells(ActivityLogEntry entry) {
        List<Node> cells = new ArrayList<>();
        String area = normalize(displayArea(entry));

        if (isError(entry)) {
            addRailCell(cells, createRailDetailCell(WARNING_ICON_PATH, primaryRailLabel(entry), primaryRailValue(entry), "logs-inline-rail-danger-cell"));
            addRailCell(cells, createRailDetailCell(WARNING_ICON_PATH, "Reason", failureReason(entry), "logs-inline-rail-wide-cell", "logs-inline-rail-danger-cell"));
            addRailCell(cells, createRailDetailCell(CHECK_ICON_PATH, "Action needed", actionNeeded(entry), "logs-inline-rail-wide-cell"));
            addLogRailCell(cells, entry);
            return cells;
        }

        if (isPasswordEvent(entry)) {
            addRailCell(cells, createRailChangeStackCell(
                    List.of(new ActivityChange("Password", "Existing password", "Updated")),
                    true
            ));
            addLogRailCell(cells, entry);
            return cells;
        }

        List<ActivityChange> visibleChanges = visibleChanges(entry);
        if (!visibleChanges.isEmpty() && !isCreateEvent(entry) && !isDeleteEvent(entry)) {
            addRailCell(cells, createRailChangeStackCell(visibleChanges.stream().limit(4).toList(), false));
            addLogRailCell(cells, entry);
            return cells;
        }

        if (isCreateEvent(entry)) {
            if (area.equals("users")) {
                addRailCell(cells, createRailDetailCell(USER_ICON_PATH, "Full name", firstCreatedValue(entry, "full name", "name")));
                addRailCell(cells, createRailDetailCell(USER_ICON_PATH, "Username", firstCreatedValue(entry, "username")));
                addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_PATH, "Email", firstCreatedValue(entry, "email", "email address"), "logs-inline-rail-wide-cell"));
                addRailCell(cells, createRailDetailCell(GEAR_ICON_PATH, "Role", firstCreatedValue(entry, "role")));
                addRailCell(cells, createRailDetailCell(CHECK_ICON_PATH, "Status", firstCreatedValue(entry, "status"), "logs-inline-rail-success-cell"));
                addLogRailCell(cells, entry);
                return cells;
            }

            if (area.equals("profiles")) {
                addRailCell(cells, createRailDetailCell(GEAR_ICON_PATH, "Profile", primaryRailValue(entry), "logs-inline-rail-primary-cell"));
                addRailCell(cells, createRailDetailCell(USER_ICON_PATH, "Client", firstCreatedValue(entry, "client", "customer")));
                addRailCell(cells, createRailDetailCell(REFRESH_ICON_PATH, "Split rule", firstCreatedValue(entry, "split rule", "barcode splitting", "barcode")));
                addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_PATH, "Export label", firstCreatedValue(entry, "export label", "export naming", "export format"), "logs-inline-rail-wide-cell"));
                addLogRailCell(cells, entry);
                return cells;
            }

            if (area.equals("documents")) {
                addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_PATH, "Note", displayText(entry.description(), "A document detail item was created."), "logs-inline-rail-note-cell"));
                addRailCell(cells, createRailDetailCell(GEAR_ICON_PATH, "Category", documentCategory(entry)));
                addLogRailCell(cells, entry);
                return cells;
            }
        }

        if (isDeleteEvent(entry)) {
            if (area.equals("users")) {
                addRailCell(cells, createRailDetailCell(USER_ICON_PATH, "User", firstDeletedValue(entry, "full name", "name", "user"), "logs-inline-rail-primary-cell"));
                addRailCell(cells, createRailDetailCell(USER_ICON_PATH, "Username", firstDeletedValue(entry, "username")));
                addRailCell(cells, createRailDetailCell(WARNING_ICON_PATH, "Action", deleteActionText(entry), "logs-inline-rail-danger-cell"));
                addLogRailCell(cells, entry);
                return cells;
            }

            if (area.equals("profiles")) {
                addRailCell(cells, createRailDetailCell(GEAR_ICON_PATH, "Profile", firstDeletedValue(entry, "profile", "profile name", "name"), "logs-inline-rail-primary-cell"));
                addRailCell(cells, createRailDetailCell(REFRESH_ICON_PATH, "Split rule", firstDeletedValue(entry, "split rule", "barcode splitting")));
                addRailCell(cells, createRailDetailCell(WARNING_ICON_PATH, "Action", deleteActionText(entry), "logs-inline-rail-danger-cell"));
                addLogRailCell(cells, entry);
                return cells;
            }

            addRailCell(cells, createRailDetailCell(WARNING_ICON_PATH, primaryRailLabel(entry), primaryRailValue(entry), "logs-inline-rail-danger-cell"));
            compactVisibleRows(snapshotRowsForChangeEvent(entry), 3)
                    .forEach(row -> addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_PATH, row.label(), row.value())));
            addLogRailCell(cells, entry);
            return cells;
        }

        if (area.equals("exports")) {
            addRailCell(cells, createRailDetailCell(DOWNLOAD_ICON_PATH, "Note", displayText(entry.description(), "A report was exported."), "logs-inline-rail-note-cell"));
            addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_PATH, "Export mode", firstContextValue(entry, "export mode", "mode", "format")));
            addLogRailCell(cells, entry);
            return cells;
        }

        if (area.equals("documents")) {
            addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_PATH, "Note", displayText(entry.description(), "Document detail activity was recorded."), "logs-inline-rail-note-cell"));
            addRailCell(cells, createRailDetailCell(GEAR_ICON_PATH, "Category", documentCategory(entry)));
            addLogRailCell(cells, entry);
            return cells;
        }

        if (area.equals("qa")) {
            addRailCell(cells, createRailDetailCell(CHECK_ICON_PATH, "Document", firstContextOrTarget(entry, "document", "document id", "box", "box id"), "logs-inline-rail-primary-cell"));
            addRailCell(cells, createRailDetailCell(CHECK_ICON_PATH, "Checklist", firstContextValue(entry, "checklist", "checklist count")));
            addRailCell(cells, createRailDetailCell(WARNING_ICON_PATH, "Issues", firstContextValue(entry, "issues", "issue count")));
            addRailCell(cells, createRailDetailCell(CHECK_ICON_PATH, "Result", displayStatus(entry.status()), "logs-inline-rail-success-cell"));
            addLogRailCell(cells, entry);
            return cells;
        }

        if (isTiffActivity(entry)) {
            List<ActivityDetailRow> tiffRows = compactVisibleRows(tiffMetricRows(entry), 5);
            if (tiffRows.isEmpty()) {
                tiffRows = compactVisibleRows(compactDetailRows(entry), 5);
            }
            tiffRows.forEach(row -> addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_PATH, row.label(), row.value())));
            addLogRailCell(cells, entry);
            return cells;
        }

        compactVisibleRows(compactDetailRows(entry), 5)
                .forEach(row -> addRailCell(cells, createRailDetailCell(GEAR_ICON_PATH, row.label(), row.value())));

        if (cells.isEmpty()) {
            addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_PATH, "Note", displayText(entry.description(), "Activity was recorded."), "logs-inline-rail-note-cell"));
        }

        addLogRailCell(cells, entry);
        return cells;
    }

    private List<ActivityChange> visibleChanges(ActivityLogEntry entry) {
        return entry.changes().stream()
                .filter(this::hasVisibleChange)
                .toList();
    }

    private void addRailCell(List<Node> cells, Node cell) {
        if (cell != null) {
            cells.add(cell);
        }
    }

    private void addLogRailCell(List<Node> cells, ActivityLogEntry entry) {
        addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_PATH, "Log ID", "LOG-" + entry.id(), "logs-inline-rail-log-cell"));
    }

    private HBox createRailDetailCell(String iconPath, String label, String value, String... styleClasses) {
        String displayValue = displayAuditValue(value);
        if (isMissingAuditValue(displayValue) || "\u2014".equals(displayValue)) {
            return null;
        }

        StackPane icon = createRailIcon(iconPath);

        Label labelNode = new Label(displayText(label, "Detail"));
        labelNode.getStyleClass().add("logs-inline-rail-label");
        labelNode.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label valueNode = new Label(displayValue);
        valueNode.getStyleClass().add("logs-inline-rail-value");
        valueNode.setTextOverrun(OverrunStyle.ELLIPSIS);
        valueNode.setTooltip(new Tooltip(displayValue));
        valueNode.setMinWidth(0);
        valueNode.setMaxWidth(Double.MAX_VALUE);

        VBox copy = new VBox(3, labelNode, valueNode);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox cell = new HBox(9, icon, copy);
        cell.getStyleClass().add("logs-inline-rail-cell");
        cell.getStyleClass().addAll(styleClasses);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMinWidth(0);
        cell.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private HBox createRailChangeStackCell(List<ActivityChange> changes, boolean sensitive) {
        List<ActivityChange> visibleChanges = changes.stream()
                .filter(change -> change != null)
                .toList();

        if (visibleChanges.isEmpty()) {
            return null;
        }

        StackPane icon = createRailIcon(sensitive ? USER_ICON_PATH : REFRESH_ICON_PATH);

        VBox rows = new VBox(6);
        rows.getStyleClass().add("logs-inline-rail-change-stack");
        rows.setMinWidth(0);
        rows.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rows, Priority.ALWAYS);

        for (ActivityChange change : visibleChanges) {
            rows.getChildren().add(createRailChangeRow(change, sensitive));
        }

        HBox cell = new HBox(15, icon, rows);
        cell.getStyleClass().add("logs-inline-rail-cell");
        cell.getStyleClass().add("logs-inline-rail-change-stack-cell");
        cell.getStyleClass().add(sensitive ? "logs-inline-rail-sensitive-cell" : "logs-inline-rail-change-cell");
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMinWidth(0);
        cell.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private HBox createRailChangeRow(ActivityChange change, boolean sensitive) {
        String beforeText = sensitive ? "Existing password" : displayAuditValue(change.oldValue());
        String afterText = sensitive ? "Updated" : displayAuditValue(change.newValue());

        Label fieldNode = new Label(displayText(change.field(), "Field"));
        fieldNode.getStyleClass().add("logs-inline-rail-change-field");
        fieldNode.setTextOverrun(OverrunStyle.ELLIPSIS);
        fieldNode.setMinWidth(105);
        fieldNode.setPrefWidth(126);
        fieldNode.setMaxWidth(150);

        Label before = createRailPill(beforeText, "logs-inline-rail-before-pill");
        Label arrow = new Label("\u2192");
        arrow.getStyleClass().add("logs-inline-rail-arrow");
        Label after = createRailPill(afterText, "logs-inline-rail-after-pill");

        HBox.setHgrow(before, Priority.ALWAYS);
        HBox.setHgrow(after, Priority.ALWAYS);

        HBox row = new HBox(12, fieldNode, before, arrow, after);
        row.getStyleClass().add("logs-inline-rail-change-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private HBox createRailChangeCell(String field, String beforeValue, String afterValue, boolean sensitive) {
        String beforeText = displayAuditValue(beforeValue);
        String afterText = displayAuditValue(afterValue);

        if (sensitive) {
            beforeText = "Existing password";
            afterText = "Updated";
        }

        StackPane icon = createRailIcon(sensitive ? USER_ICON_PATH : REFRESH_ICON_PATH);

        Label fieldNode = new Label(displayText(field, "Field"));
        fieldNode.getStyleClass().add("logs-inline-rail-label");
        fieldNode.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label before = createRailPill(beforeText, "logs-inline-rail-before-pill");
        Label arrow = new Label("\u2192");
        arrow.getStyleClass().add("logs-inline-rail-arrow");
        Label after = createRailPill(afterText, "logs-inline-rail-after-pill");

        HBox changeLine = new HBox(9, before, arrow, after);
        changeLine.setAlignment(Pos.CENTER_LEFT);
        changeLine.setMinWidth(0);
        changeLine.setMaxWidth(Double.MAX_VALUE);

        VBox copy = new VBox(4, fieldNode, changeLine);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox cell = new HBox(9, icon, copy);
        cell.getStyleClass().add("logs-inline-rail-cell");
        cell.getStyleClass().add(sensitive ? "logs-inline-rail-sensitive-cell" : "logs-inline-rail-change-cell");
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMinWidth(0);
        cell.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private Label createRailPill(String value, String styleClass) {
        Label pill = new Label(displayAuditValue(value));
        pill.getStyleClass().add(styleClass);
        pill.setTextOverrun(OverrunStyle.ELLIPSIS);
        pill.setTooltip(new Tooltip(pill.getText()));
        pill.setMinWidth(0);
        pill.setMaxWidth(Double.MAX_VALUE);
        return pill;
    }

    private StackPane createRailIcon(String iconPath) {
        StackPane shell = new StackPane(createPrimeIcon(iconPath, "logs-inline-rail-icon"));
        shell.getStyleClass().add("logs-inline-rail-icon-shell");
        return shell;
    }

    private boolean isPasswordEvent(ActivityLogEntry entry) {
        return normalize(formatAction(entry.action())).contains("password")
                || entry.changes().stream().anyMatch(change -> normalize(change.field()).contains("password"));
    }

    private String firstCreatedValue(ActivityLogEntry entry, String... labels) {
        String value = firstChangeValue(entry, true, labels);
        return value.isBlank() ? firstContextOrTarget(entry, labels) : value;
    }

    private String firstDeletedValue(ActivityLogEntry entry, String... labels) {
        String value = firstChangeValue(entry, false, labels);
        return value.isBlank() ? firstContextOrTarget(entry, labels) : value;
    }

    private String firstChangeValue(ActivityLogEntry entry, boolean newValue, String... labels) {
        List<String> normalizedLabels = List.of(labels).stream()
                .map(this::normalize)
                .toList();

        String exactMatch = entry.changes().stream()
                .filter(change -> normalizedLabels.contains(normalize(change.field())))
                .map(change -> newValue ? change.newValue() : change.oldValue())
                .filter(value -> !isMissingAuditValue(value))
                .findFirst()
                .orElse("");

        if (!exactMatch.isBlank()) {
            return exactMatch;
        }

        return entry.changes().stream()
                .filter(change -> normalizedLabels.stream().anyMatch(label -> normalize(change.field()).contains(label)))
                .map(change -> newValue ? change.newValue() : change.oldValue())
                .filter(value -> !isMissingAuditValue(value))
                .findFirst()
                .orElse("");
    }

    private String firstContextOrTarget(ActivityLogEntry entry, String... labels) {
        String value = firstContextValue(entry, labels);
        return value.isBlank() ? primaryRailValue(entry) : value;
    }

    private String primaryRailLabel(ActivityLogEntry entry) {
        String area = displayArea(entry);
        return area.equals("Files") || area.equals("Import") ? "TIFF item" : area;
    }

    private String primaryRailValue(ActivityLogEntry entry) {
        String target = displayTiffItem(entry);
        return "\u2014".equals(displayAuditValue(target)) ? displayText(entry.target(), "") : target;
    }

    private String failureReason(ActivityLogEntry entry) {
        String reason = displayText(entry.description(), "");
        if (!reason.isBlank()) {
            return reason;
        }

        String contextReason = firstContextValue(entry, "reason", "failure reason", "problem", "error");
        return contextReason.isBlank() ? "Action failed." : contextReason;
    }

    private String actionNeeded(ActivityLogEntry entry) {
        String explicitAction = firstContextValue(entry, "action needed", "next step");
        if (!explicitAction.isBlank()) {
            return explicitAction;
        }

        String area = normalize(displayArea(entry));
        if (area.equals("import") || isTiffActivity(entry)) {
            return "Review TIFF and retry";
        }

        if (area.equals("exports")) {
            return "Resolve blocking issue";
        }

        if (area.equals("access") || area.equals("security")) {
            return "Review account access";
        }

        return "";
    }

    private String deleteActionText(ActivityLogEntry entry) {
        String action = normalize(formatAction(entry.action()));
        return action.contains("deactivated") || action.contains("inactive") ? "Deactivated" : "Deleted";
    }

    private String documentCategory(ActivityLogEntry entry) {
        String action = normalize(formatAction(entry.action()));
        if (action.contains("field")) {
            return "Field";
        }
        if (action.contains("template")) {
            return "Template";
        }
        return displayArea(entry);
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
            return "\u2014";
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
            case "documents" -> "logs-event-icon-documents";
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
            case "documents" -> "logs-area-documents";
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
            displayValue = DATE_RANGE_FORMATTER.format(rangeStartDate) + " - " + DATE_RANGE_FORMATTER.format(rangeEndDate);
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

        picker.setPromptText("MM/DD/YYYY");
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
                    try {
                        return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } catch (DateTimeParseException ignored) {
                        return null;
                    }
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
            case "DOCUMENT_DETAILS_SAVED" -> {
                return "Saved document details";
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
