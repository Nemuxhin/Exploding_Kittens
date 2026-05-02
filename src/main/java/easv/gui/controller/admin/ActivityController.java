package easv.gui.controller.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActivityController {

    private static final String ALL_TYPES = "All Types";
    private static final String ALL_USERS = "All Users";
    private static final String ALL_STATUSES = "All Statuses";
    private static final String RANGE_LAST_7_DAYS = "Last 7 Days";
    private static final String RANGE_TODAY = "Today";
    private static final String RANGE_YESTERDAY = "Yesterday";
    private static final String RANGE_ALL_DATES = "All Dates";
    private static final String RANGE_CUSTOM = "Custom Range";
    private static final DateTimeFormatter ACTIVITY_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_RANGE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObservableList<ActivityLogEntry> activityEntries = FXCollections.observableArrayList();

    private String expandedEntryId;
    private boolean updatingDateControls;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate pendingRangeStart;

    @FXML private TextField searchField;

    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> userFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> dateRangeFilterComboBox;
    @FXML private DatePicker dateRangePicker;
    @FXML private Label exportStatusLabel;

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
        configureDateRangePicker();

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
                RANGE_LAST_7_DAYS,
                RANGE_TODAY,
                RANGE_YESTERDAY,
                RANGE_ALL_DATES,
                RANGE_CUSTOM
        );

        setDateRange(RANGE_LAST_7_DAYS, LocalDate.now().minusDays(7), LocalDate.now());
    }

    private void configureListeners() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshFilteredTimeline());

        typeFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshFilteredTimeline());
        userFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshFilteredTimeline());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshFilteredTimeline());
        dateRangeFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingDateControls) {
                return;
            }

            applyPresetDateRange(newValue);
            refreshFilteredTimeline();
        });

        dateRangePicker.setOnAction(event -> handleDateRangeSelection(dateRangePicker.getValue()));
    }

    private void renderTimeline() {
        List<ActivityLogEntry> filteredEntries = filteredActivityEntries();

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

        Label metaLabel = new Label(entry.actor() + " - " + entry.target());
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

        Runnable toggleDetails = () -> {
            expandedEntryId = entry.id().equals(expandedEntryId) ? null : entry.id();
            renderTimeline();
        };

        row.setOnMouseClicked(event -> toggleDetails.run());
        AdminKeyboard.makeActivatable(row, "Toggle details for " + entry.action(), toggleDetails);

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
                && matchesDateRange(entry);
    }

    private boolean matchesCombo(String value, String selectedValue, String allValue) {
        return selectedValue == null
                || allValue.equals(selectedValue)
                || value.equalsIgnoreCase(selectedValue);
    }

    private boolean matchesDateRange(ActivityLogEntry entry) {
        if (RANGE_ALL_DATES.equals(dateRangeFilterComboBox.getValue())) {
            return true;
        }

        LocalDate activityDate = parseActivityDate(entry);

        if (activityDate == null) {
            return false;
        }

        LocalDate rangeStart = fromDate;
        LocalDate rangeEnd = toDate;

        if (rangeStart == null && rangeEnd == null) {
            return true;
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            LocalDate swappedDate = rangeStart;
            rangeStart = rangeEnd;
            rangeEnd = swappedDate;
        }

        boolean afterStart = rangeStart == null || !activityDate.isBefore(rangeStart);
        boolean beforeEnd = rangeEnd == null || !activityDate.isAfter(rangeEnd);

        return afterStart && beforeEnd;
    }

    private LocalDate parseActivityDate(ActivityLogEntry entry) {
        try {
            return LocalDateTime.parse(entry.fullTimestamp(), ACTIVITY_TIMESTAMP_FORMATTER).toLocalDate();
        } catch (DateTimeParseException exception) {
            return null;
        }
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
        LocalDate today = LocalDate.now();
        setDateRange(RANGE_TODAY, today, today);
        refreshFilteredTimeline();
    }

    @FXML
    private void showTodayActivityFromKeyboard(KeyEvent event) {
        AdminKeyboard.runOnActivationKey(event, this::showTodayActivity);
    }

    @FXML
    private void showScansOnly() {
        typeFilterComboBox.setValue("Scans");
        statusFilterComboBox.setValue(ALL_STATUSES);
    }

    @FXML
    private void showScansOnlyFromKeyboard(KeyEvent event) {
        AdminKeyboard.runOnActivationKey(event, this::showScansOnly);
    }

    @FXML
    private void showQaCompleted() {
        typeFilterComboBox.setValue("QA");
        statusFilterComboBox.setValue("Success");
    }

    @FXML
    private void showQaCompletedFromKeyboard(KeyEvent event) {
        AdminKeyboard.runOnActivationKey(event, this::showQaCompleted);
    }

    @FXML
    private void showFailedEvents() {
        statusFilterComboBox.setValue("Failed");
    }

    @FXML
    private void showFailedEventsFromKeyboard(KeyEvent event) {
        AdminKeyboard.runOnActivationKey(event, this::showFailedEvents);
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        typeFilterComboBox.setValue(ALL_TYPES);
        userFilterComboBox.setValue(ALL_USERS);
        statusFilterComboBox.setValue(ALL_STATUSES);
        setDateRange(RANGE_LAST_7_DAYS, LocalDate.now().minusDays(7), LocalDate.now());
        expandedEntryId = null;
        hideExportStatus();
        renderTimeline();
    }

    @FXML
    private void exportActivityLog() {
        List<ActivityLogEntry> visibleEntries = filteredActivityEntries();
        String eventLabel = visibleEntries.size() == 1 ? "event" : "events";
        showExportStatus("Prepared " + visibleEntries.size() + " visible " + eventLabel + " for export.");
    }

    private void refreshFilteredTimeline() {
        hideExportStatus();
        renderTimeline();
    }

    private void applyPresetDateRange(String selectedRange) {
        LocalDate today = LocalDate.now();

        switch (selectedRange) {
            case RANGE_TODAY -> setDateRange(RANGE_TODAY, today, today);
            case RANGE_YESTERDAY -> {
                LocalDate yesterday = today.minusDays(1);
                setDateRange(RANGE_YESTERDAY, yesterday, yesterday);
            }
            case RANGE_ALL_DATES -> setDateRange(RANGE_ALL_DATES, null, null);
            case RANGE_CUSTOM -> {
            }
            default -> setDateRange(RANGE_LAST_7_DAYS, today.minusDays(7), today);
        }
    }

    private void setDateRange(String selectedRange, LocalDate fromDate, LocalDate toDate) {
        updatingDateControls = true;
        this.fromDate = fromDate;
        this.toDate = toDate;
        pendingRangeStart = null;
        dateRangeFilterComboBox.setValue(selectedRange);
        dateRangePicker.setValue(toDate != null ? toDate : fromDate);
        dateRangePicker.getEditor().setText(formatDateRange());
        dateRangePicker.setDayCellFactory(dateRangePicker.getDayCellFactory());
        updatingDateControls = false;
    }

    private void handleDateRangeSelection(LocalDate selectedDate) {
        if (updatingDateControls) {
            return;
        }

        if (selectedDate == null) {
            return;
        }

        updatingDateControls = true;
        if (pendingRangeStart == null) {
            pendingRangeStart = selectedDate;
            fromDate = selectedDate;
            toDate = null;
        } else {
            if (selectedDate.isBefore(pendingRangeStart)) {
                fromDate = selectedDate;
                toDate = pendingRangeStart;
            } else {
                fromDate = pendingRangeStart;
                toDate = selectedDate;
            }

            pendingRangeStart = null;
        }

        dateRangeFilterComboBox.setValue(RANGE_CUSTOM);
        dateRangePicker.setValue(toDate != null ? toDate : fromDate);
        dateRangePicker.getEditor().setText(formatDateRange());
        dateRangePicker.setDayCellFactory(dateRangePicker.getDayCellFactory());
        updatingDateControls = false;
        refreshFilteredTimeline();
    }

    private void configureDateRangePicker() {
        dateRangePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return formatDateRange();
            }

            @Override
            public LocalDate fromString(String value) {
                return null;
            }
        });

        dateRangePicker.setDayCellFactory(picker -> new DateCell() {
            {
                addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    LocalDate selectedDate = getItem();

                    if (isSelectableDate(selectedDate)) {
                        selectDateFromCalendar(selectedDate, event);
                    }
                });
                addEventFilter(MouseEvent.MOUSE_RELEASED, MouseEvent::consume);
                addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
            }

            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                getStyleClass().removeAll(
                        "activity-log-date-range-start",
                        "activity-log-date-range-end",
                        "activity-log-date-range-between",
                        "activity-log-date-disabled"
                );

                if (empty || date == null) {
                    return;
                }

                if (date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    getStyleClass().add("activity-log-date-disabled");
                    return;
                }

                if (isRangeStart(date)) {
                    getStyleClass().add("activity-log-date-range-start");
                }

                if (isRangeEnd(date)) {
                    getStyleClass().add("activity-log-date-range-end");
                }

                if (isBetweenRange(date)) {
                    getStyleClass().add("activity-log-date-range-between");
                }
            }
        });
    }

    private void selectDateFromCalendar(LocalDate selectedDate, MouseEvent event) {
        handleDateRangeSelection(selectedDate);

        if (pendingRangeStart == null) {
            dateRangePicker.hide();
        } else {
            dateRangePicker.show();
        }

        event.consume();
    }

    private boolean isSelectableDate(LocalDate date) {
        return date != null && !date.isAfter(LocalDate.now());
    }

    private boolean isRangeStart(LocalDate date) {
        return fromDate != null && date.equals(fromDate);
    }

    private boolean isRangeEnd(LocalDate date) {
        return toDate != null && date.equals(toDate);
    }

    private boolean isBetweenRange(LocalDate date) {
        return fromDate != null
                && toDate != null
                && date.isAfter(fromDate)
                && date.isBefore(toDate);
    }

    private String formatDateRange() {
        if (fromDate == null && toDate == null) {
            return RANGE_ALL_DATES;
        }

        if (fromDate != null && toDate == null) {
            return DATE_RANGE_FORMATTER.format(fromDate);
        }

        return DATE_RANGE_FORMATTER.format(fromDate) + " - " + DATE_RANGE_FORMATTER.format(toDate);
    }

    private List<ActivityLogEntry> filteredActivityEntries() {
        return activityEntries.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesFilters)
                .toList();
    }

    private void showExportStatus(String message) {
        exportStatusLabel.setText(message);
        exportStatusLabel.setVisible(true);
        exportStatusLabel.setManaged(true);
    }

    private void hideExportStatus() {
        exportStatusLabel.setText("");
        exportStatusLabel.setVisible(false);
        exportStatusLabel.setManaged(false);
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
        activityEntries.setAll(AdminDemoData.activityLogEntries());
    }

    record ActivityLogEntry(
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

    record ActivityDetail(String label, String value) {
    }
}
