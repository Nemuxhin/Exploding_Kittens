package easv.gui.controller.admin;

import easv.be.AuditLog;
import easv.bll.AdminManager;
import easv.gui.controller.utilities.AppDates;
import easv.gui.PrimeIcons;
import easv.util.Strings;
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
import javafx.scene.layout.FlowPane;
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
import java.util.Arrays;
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

    private static final String UPLOAD_ICON_GLYPH = "\ue934";
    private static final String CHECK_ICON_GLYPH = "\ue90a";
    private static final String WARNING_ICON_GLYPH = "\ue922";
    private static final String REFRESH_ICON_GLYPH = "\ue938";
    private static final String USER_ICON_GLYPH = "\ue939";
    private static final String GEAR_ICON_GLYPH = "\ue94a";
    private static final String DOWNLOAD_ICON_GLYPH = "\ue956";
    private static final String DOCUMENT_ICON_GLYPH = "\ue958";
    private static final String PAGES_ICON_GLYPH = "\ue95c";
    private static final String CLOCK_ICON_GLYPH = "\ue940";
    private static final String BOX_ICON_GLYPH = "\ue941";
    private static final String CHART_ICON_GLYPH = "\ue9e4";

    private static final String AREA_FILTER_ICON_GLYPH = "\ue941";
    private static final String RESULT_FILTER_ICON_GLYPH = "\ue90a";
    private static final String DATE_FILTER_ICON_GLYPH = "\ue927";
    private static final String SORT_FILTER_ICON_GLYPH = "\ue915";

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
        configureFilterComboBox(typeFilterComboBox, "Area", AREA_FILTER_ICON_GLYPH);
        configureFilterComboBox(userFilterComboBox, "User", USER_ICON_GLYPH);
        configureFilterComboBox(statusFilterComboBox, "Result", RESULT_FILTER_ICON_GLYPH);
        configureFilterComboBox(sortFilterComboBox, "Sort", SORT_FILTER_ICON_GLYPH);

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

        Label valueLabel = new Label(Strings.displayText(value, ""));
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

        VBox card = createPayloadCard(entry);
        tray.getChildren().setAll(card);
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
        return List.of(createPayloadCard(entry));
    }

    // â”€â”€ Structured payload card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private VBox createPayloadCard(ActivityLogEntry entry) {
        VBox card = new VBox(0);
        card.getStyleClass().add("logs-payload-card");
        if (isError(entry)) {
            card.getStyleClass().add("logs-payload-card-failed");
        }
        card.setFillWidth(true);
        card.setMaxWidth(Double.MAX_VALUE);

        // Error banner spans full width at the top
        if (isError(entry)) {
            card.getChildren().add(createPayloadErrorBanner(entry));
        }

        // ── Left column ─────────────────────────────────────────
        VBox leftColumn = new VBox(0);
        leftColumn.setFillWidth(true);
        leftColumn.setMinWidth(0);
        leftColumn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        boolean any = false;
        String area = Strings.normalize(displayArea(entry));

        // Hero section — subject + actor + status (non-error events)
        if (!isError(entry)) {
            leftColumn.getChildren().add(createPayloadHeroSection(entry));
            any = true;
        }

        // TIFF metrics — scan/import success events
        if (!isError(entry) && (isTiffActivity(entry) || area.equals("import"))) {
            List<ActivityDetailRow> tiffRows = tiffMetricRows(entry);
            if (!tiffRows.isEmpty()) {
                if (any) leftColumn.getChildren().add(createPayloadSectionDivider());
                leftColumn.getChildren().add(createPayloadTileSection("TIFF details", tiffRows));
                any = true;
            }
        }

        // Field changes — update events (not create/delete/password)
        List<ActivityChange> visChanges = visibleChanges(entry);
        if (!visChanges.isEmpty() && !isCreateEvent(entry) && !isDeleteEvent(entry) && !isPasswordEvent(entry)) {
            if (any) leftColumn.getChildren().add(createPayloadSectionDivider());
            leftColumn.getChildren().add(createPayloadChangesSection(changeSectionTitle(entry), visChanges));
            any = true;
        }

        // Password security note
        if (isPasswordEvent(entry) && !isError(entry)) {
            if (any) leftColumn.getChildren().add(createPayloadSectionDivider());
            leftColumn.getChildren().add(createPayloadSection("Security change",
                    List.of(new ActivityDetailRow("Field changed", "Password"),
                            new ActivityDetailRow("Value", "Hidden for security")), false));
            any = true;
        }

        // Create / delete snapshot
        if (isCreateEvent(entry) || isDeleteEvent(entry)) {
            List<ActivityDetailRow> snap = compactVisibleRows(snapshotRowsForChangeEvent(entry), 8);
            if (!snap.isEmpty()) {
                if (any) leftColumn.getChildren().add(createPayloadSectionDivider());
                leftColumn.getChildren().add(createPayloadSection(changeSectionTitle(entry), snap, false));
                any = true;
            }
        }

        // QA review details
        if (area.equals("qa")) {
            List<ActivityDetailRow> qaRows = buildQaDetailRows(entry);
            if (!qaRows.isEmpty()) {
                if (any) leftColumn.getChildren().add(createPayloadSectionDivider());
                leftColumn.getChildren().add(createPayloadSection("Review details", qaRows, false));
                any = true;
            }
        }

        // Export details
        if (area.equals("exports") || Strings.normalize(entry.action()).contains("export")) {
            List<ActivityDetailRow> exportRows = buildExportDetailRows(entry);
            if (!exportRows.isEmpty()) {
                if (any) leftColumn.getChildren().add(createPayloadSectionDivider());
                leftColumn.getChildren().add(createPayloadSection("Export details", exportRows, false));
                any = true;
            }
        }

        // Failure summary
        if (isError(entry)) {
            List<ActivityDetailRow> failRows = buildFailureSummaryRows(entry);
            if (!failRows.isEmpty()) {
                if (any) leftColumn.getChildren().add(createPayloadSectionDivider());
                leftColumn.getChildren().add(createPayloadSection("Failure summary", failRows, true));
                any = true;
            }
        }

        // General context catch-all — documents, system, and any event with no dedicated section
        List<ActivityDetailRow> contextRows = buildGeneralContextRows(entry, area, any);
        if (!contextRows.isEmpty()) {
            if (any) leftColumn.getChildren().add(createPayloadSectionDivider());
            leftColumn.getChildren().add(createPayloadSection("Event details", contextRows, false));
        }

        // ── Right sidebar — Storage & trace ─────────────────────
        List<ActivityDetailRow> storeRows = storageTraceRows(entry);
        if (!storeRows.isEmpty()) {
            VBox sidebar = new VBox(0);
            sidebar.getStyleClass().add("logs-payload-sidebar");
            sidebar.setFillWidth(true);
            sidebar.getChildren().add(createPayloadSidebarSection(entry, "Storage & trace", storeRows));

            HBox body = new HBox(0, leftColumn, sidebar);
            body.setFillHeight(true);
            body.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().add(body);
        } else {
            card.getChildren().add(leftColumn);
        }

        return card;
    }

    private HBox createPayloadErrorBanner(ActivityLogEntry entry) {
        Label icon = createPrimeIcon(WARNING_ICON_GLYPH, "logs-payload-error-icon");

        Label titleLabel = new Label(formatAction(entry.action()));
        titleLabel.getStyleClass().add("logs-payload-error-title");
        titleLabel.setWrapText(true);

        VBox copy = new VBox(3, titleLabel);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        String desc = Strings.displayText(entry.description(), "");
        if (!desc.isBlank()) {
            Label descLabel = new Label(desc);
            descLabel.getStyleClass().add("logs-payload-error-copy");
            descLabel.setWrapText(true);
            copy.getChildren().add(descLabel);
        }

        HBox banner = new HBox(12, icon, copy);
        banner.getStyleClass().add("logs-payload-error-banner");
        banner.setAlignment(Pos.CENTER_LEFT);
        return banner;
    }

    private HBox createPayloadHeroSection(ActivityLogEntry entry) {
        // Colored icon shell
        String iconPath = heroIconFor(entry);
        String iconBgClass = heroIconBgClassFor(entry);
        Label iconLabel = createPrimeIcon(iconPath, "logs-payload-hero-icon");
        StackPane iconShell = new StackPane(iconLabel);
        iconShell.getStyleClass().addAll("logs-payload-hero-icon-shell", iconBgClass);

        // Subject — the main thing this action was about
        String subject = heroSubjectFor(entry);
        Label subjectLabel = new Label(subject.isBlank() ? formatAction(entry.action()) : subject);
        subjectLabel.getStyleClass().add("logs-payload-hero-subject");
        subjectLabel.setWrapText(false);
        subjectLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        subjectLabel.setMinWidth(0);
        subjectLabel.setMaxWidth(Double.MAX_VALUE);

        VBox copy = new VBox(4, subjectLabel);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        // Scan profile — for TIFF/import events (between filename and actor)
        if (isTiffActivity(entry) || Strings.normalize(displayArea(entry)).equals("import")) {
            String profile = firstContextValue(entry, "profile");
            if (!profile.isBlank()) {
                Label gearIcon = createPrimeIcon(GEAR_ICON_GLYPH, "logs-payload-hero-meta-icon");
                Label profileLabel = new Label(profile);
                profileLabel.getStyleClass().add("logs-payload-hero-meta");
                HBox profileLine = new HBox(5, gearIcon, profileLabel);
                profileLine.setAlignment(Pos.CENTER_LEFT);
                copy.getChildren().add(profileLine);
            }
        }

        // Actor line — "Uploaded by Alex Johnson" (skip if actor IS the subject)
        if (!isSystemActor(entry.actor())) {
            String actorName = displayActor(entry.actor());
            boolean actorIsSubject = Strings.normalize(actorName).equals(Strings.normalize(subject));
            if (!actorIsSubject) {
                String prefix = heroActorPrefix(entry);
                Label actorLabel = new Label(prefix + " " + actorName);
                actorLabel.getStyleClass().add("logs-payload-hero-actor");
                copy.getChildren().add(actorLabel);
            }
        }

        // Status + description line
        String desc = Strings.displayText(entry.description(), "");
        if (!desc.isBlank()) {
            Label checkIcon = createPrimeIcon(CHECK_ICON_GLYPH, "logs-payload-hero-status-icon");
            Label descLabel = new Label(desc);
            descLabel.getStyleClass().add("logs-payload-hero-status-text");
            descLabel.setWrapText(false);
            descLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            HBox statusLine = new HBox(6, checkIcon, descLabel);
            statusLine.setAlignment(Pos.CENTER_LEFT);
            copy.getChildren().add(statusLine);
        }

        HBox hero = new HBox(15, iconShell, copy);
        hero.getStyleClass().add("logs-payload-hero");
        hero.setAlignment(Pos.CENTER_LEFT);
        return hero;
    }

    private String heroSubjectFor(ActivityLogEntry entry) {
        // TIFF / import events — use filename
        if (isTiffActivity(entry) || Strings.normalize(displayArea(entry)).equals("import")) {
            String file = displayTiffItem(entry);
            if (!file.isBlank()) return file;
        }
        // Access / security — actor IS the subject (the person who logged in)
        String area = Strings.normalize(displayArea(entry));
        if (area.equals("access") || area.equals("security")) {
            if (!isSystemActor(entry.actor())) return displayActor(entry.actor());
        }
        // Use target for everything else
        String target = Strings.displayText(entry.target(), "");
        if (!target.isBlank() && !Strings.normalize(target).equals("system")) return target;
        return "";
    }

    private String heroActorPrefix(ActivityLogEntry entry) {
        if (isCreateEvent(entry)) return "Created by";
        if (isDeleteEvent(entry)) return "Deleted by";
        if (isPasswordEvent(entry)) return "Changed by";
        String area = Strings.normalize(displayArea(entry));
        if (area.equals("import") || isTiffActivity(entry)) return "Imported by";
        return "Performed by";
    }

    private String heroIconFor(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        return switch (area) {
            case "users", "access", "security" -> USER_ICON_GLYPH;
            case "profiles" -> GEAR_ICON_GLYPH;
            case "qa" -> CHECK_ICON_GLYPH;
            case "files", "import", "documents" -> DOCUMENT_ICON_GLYPH;
            default -> CLOCK_ICON_GLYPH;
        };
    }

    private String heroIconBgClassFor(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        return switch (area) {
            case "users", "access", "security", "profiles" -> "logs-payload-hero-icon-purple";
            case "qa" -> "logs-payload-hero-icon-teal";
            case "files", "import", "documents" -> "logs-payload-hero-icon-blue";
            default -> "logs-payload-hero-icon-neutral";
        };
    }

    private VBox createPayloadSection(String title, List<ActivityDetailRow> rows, boolean failureSection) {
        VBox section = new VBox(0);
        section.getStyleClass().add("logs-payload-section");
        section.setFillWidth(true);
        section.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("logs-payload-section-title");
        section.getChildren().add(titleLabel);

        List<ActivityDetailRow> visible = rows.stream()
                .filter(r -> !isMissingAuditValue(r.value()))
                .toList();

        for (int i = 0; i < visible.size(); i++) {
            if (i > 0) section.getChildren().add(createPayloadRowDivider());
            ActivityDetailRow row = visible.get(i);
            String valueStyle = failureSection && isPayloadDangerLabel(row.label())
                    ? "logs-payload-kv-value-danger"
                    : isPayloadSuccessLabel(row.label()) ? "logs-payload-kv-value-success"
                    : isPayloadLinkLabel(row.label()) ? "logs-payload-kv-value-link"
                    : null;
            section.getChildren().add(
                    createPayloadKvRow(iconForPayloadLabel(row.label()), row.label(),
                            displayAuditValue(row.value()), valueStyle));
        }

        return section;
    }

    private VBox createPayloadTileSection(String title, List<ActivityDetailRow> rows) {
        VBox section = new VBox(0);
        section.getStyleClass().add("logs-payload-section");
        section.setFillWidth(true);
        section.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("logs-payload-section-title");
        section.getChildren().add(titleLabel);

        List<ActivityDetailRow> visible = rows.stream()
                .filter(r -> !isMissingAuditValue(r.value()))
                .toList();

        if (!visible.isEmpty()) {
            FlowPane grid = new FlowPane();
            grid.getStyleClass().add("logs-payload-tile-grid");
            grid.setHgap(8);
            grid.setVgap(8);
            visible.forEach(row -> grid.getChildren().add(
                    createMetricTile(iconForPayloadLabel(row.label()), row.label(), displayAuditValue(row.value()))
            ));
            section.getChildren().add(grid);
        }

        return section;
    }

    private VBox createMetricTile(String iconPath, String label, String value) {
        StackPane iconShell = new StackPane(createPrimeIcon(iconPath, "logs-payload-tile-icon"));
        iconShell.getStyleClass().add("logs-payload-tile-icon-shell");

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("logs-payload-tile-label");
        labelNode.setWrapText(false);

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("logs-payload-tile-value");
        valueNode.setWrapText(false);
        valueNode.setTextOverrun(OverrunStyle.ELLIPSIS);
        valueNode.setTooltip(new Tooltip(value));

        VBox tile = new VBox(4, iconShell, labelNode, valueNode);
        tile.getStyleClass().add("logs-payload-tile");
        return tile;
    }

    private VBox createPayloadChangesSection(String title, List<ActivityChange> changes) {
        VBox section = new VBox(0);
        section.getStyleClass().add("logs-payload-section");
        section.setFillWidth(true);
        section.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("logs-payload-section-title");
        section.getChildren().add(titleLabel);

        List<ActivityChange> visible = changes.stream().limit(6).toList();
        for (int i = 0; i < visible.size(); i++) {
            if (i > 0) section.getChildren().add(createPayloadRowDivider());
            section.getChildren().add(createPayloadChangeRow(visible.get(i)));
        }

        return section;
    }

    private VBox createPayloadChangeRow(ActivityChange change) {
        // Field name — small muted label at top
        Label fieldLabel = new Label(change.field());
        fieldLabel.getStyleClass().add("logs-payload-change-field-label");

        // Before block — red-tinted
        Label beforeSubtitle = new Label("Before");
        beforeSubtitle.getStyleClass().add("logs-payload-change-subtitle");
        Label beforeValue = new Label(displayAuditValue(change.oldValue()));
        beforeValue.getStyleClass().add("logs-payload-change-before-value");
        beforeValue.setTextOverrun(OverrunStyle.ELLIPSIS);
        beforeValue.setMinWidth(0);
        beforeValue.setMaxWidth(Double.MAX_VALUE);
        VBox beforeBox = new VBox(2, beforeSubtitle, beforeValue);
        beforeBox.getStyleClass().add("logs-payload-change-before");
        beforeBox.setMinWidth(0);
        beforeBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(beforeBox, Priority.ALWAYS);

        // Arrow between blocks
        Label arrowLabel = new Label("→");
        arrowLabel.getStyleClass().add("logs-payload-change-center-arrow");

        // After / Now block — green-tinted, bold, dominant
        Label afterSubtitle = new Label("Now");
        afterSubtitle.getStyleClass().add("logs-payload-change-subtitle");
        Label afterValue = new Label(displayAuditValue(change.newValue()));
        afterValue.getStyleClass().add("logs-payload-change-after-value");
        afterValue.setTextOverrun(OverrunStyle.ELLIPSIS);
        afterValue.setMinWidth(0);
        afterValue.setMaxWidth(Double.MAX_VALUE);
        VBox afterBox = new VBox(2, afterSubtitle, afterValue);
        afterBox.getStyleClass().add("logs-payload-change-after");
        afterBox.setMinWidth(0);
        afterBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(afterBox, Priority.ALWAYS);

        // Row of colored blocks with arrow between
        HBox boxesRow = new HBox(10, beforeBox, arrowLabel, afterBox);
        boxesRow.getStyleClass().add("logs-payload-change-boxes");
        boxesRow.setAlignment(Pos.CENTER_LEFT);
        boxesRow.setMinWidth(0);
        boxesRow.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(6, fieldLabel, boxesRow);
        card.getStyleClass().add("logs-payload-change-card");
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private HBox createPayloadKvRow(String iconPath, String label, String value, String valueStyleClass) {
        StackPane iconShell = createPayloadIconShell(iconPath);

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("logs-payload-kv-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("logs-payload-kv-value");
        if (valueStyleClass != null && !valueStyleClass.isBlank()) {
            valueNode.getStyleClass().add(valueStyleClass);
        }
        valueNode.setTextOverrun(OverrunStyle.ELLIPSIS);
        valueNode.setTooltip(new Tooltip(value));
        valueNode.setMinWidth(0);
        valueNode.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(valueNode, Priority.ALWAYS);

        HBox row = new HBox(12, iconShell, labelNode, valueNode);
        row.getStyleClass().add("logs-payload-kv-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private StackPane createPayloadIconShell(String iconPath) {
        StackPane shell = new StackPane(createPrimeIcon(iconPath, "logs-payload-kv-icon"));
        shell.getStyleClass().add("logs-payload-kv-icon-shell");
        return shell;
    }

    private Region createPayloadSectionDivider() {
        Region div = new Region();
        div.getStyleClass().add("logs-payload-section-divider");
        return div;
    }

    private Region createPayloadRowDivider() {
        Region div = new Region();
        div.getStyleClass().add("logs-payload-row-divider");
        return div;
    }

    private List<ActivityDetailRow> buildEventOverviewRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        String desc = Strings.displayText(entry.description(), "");
        if (!desc.isBlank()) {
            rows.add(new ActivityDetailRow("Note", desc));
        }
        String target = Strings.displayText(entry.target(), "");
        if (!target.isBlank() && !Strings.normalize(target).equals("system")) {
            rows.add(new ActivityDetailRow("Affected", target));
        }
        if (!isSystemActor(entry.actor())) {
            rows.add(new ActivityDetailRow("Performed by", Strings.displayText(entry.actor(), "")));
        }
        return rows;
    }

    private List<ActivityDetailRow> buildFailureSummaryRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();

        String file = displayTiffItem(entry);
        if (!file.isBlank()) {
            rows.add(new ActivityDetailRow("Batch file", file));
        }

        String desc = Strings.displayText(entry.description(), "");
        if (!desc.isBlank()) {
            rows.add(new ActivityDetailRow("Reason", desc));
        }

        rows.addAll(normalizedContextRows(entry));

        String action = actionNeeded(entry);
        if (!action.isBlank() && rows.stream().noneMatch(r -> Strings.normalize(r.label()).contains("action"))) {
            rows.add(new ActivityDetailRow("Action needed", action));
        }

        return compactVisibleRows(rows, 10);
    }

    private String iconForPayloadLabel(String label) {
        String norm = Strings.normalize(label);
        if (norm.contains("user") || norm.contains("actor") || norm.contains("importer") || norm.equals("username")) return USER_ICON_GLYPH;
        if (norm.equals("result") || norm.contains("status") || norm.contains("accepted") || norm.contains("action needed")) return CHECK_ICON_GLYPH;
        if (norm.contains("checklist")) return CHECK_ICON_GLYPH;
        if (norm.contains("issue") || norm.contains("reason") || norm.contains("failed") || norm.contains("warning") || norm.contains("error")) return WARNING_ICON_GLYPH;
        if (norm.contains("trace") || norm.equals("log id") || norm.contains("recorded")) return CLOCK_ICON_GLYPH;
        if (norm.contains("export") || norm.contains("download") || norm.contains("records")) return DOWNLOAD_ICON_GLYPH;
        if (norm.contains("box")) return BOX_ICON_GLYPH;
        if (norm.equals("pages") || norm.equals("page count") || norm.equals("page")) return PAGES_ICON_GLYPH;
        if (norm.contains("file size") || norm.equals("size")) return CHART_ICON_GLYPH;
        if (norm.contains("file") || norm.contains("tiff") || norm.contains("batch")) return UPLOAD_ICON_GLYPH;
        if (norm.contains("role") || norm.contains("profile") || norm.contains("split rule") || norm.contains("setting") || norm.contains("config") || norm.equals("field changed")) return GEAR_ICON_GLYPH;
        if (norm.equals("case") || norm.equals("case id")) return PAGES_ICON_GLYPH;
        if (norm.contains("date") || norm.contains("time") || norm.contains("modified") || norm.contains("created at")) return CLOCK_ICON_GLYPH;
        return DOCUMENT_ICON_GLYPH;
    }

    private boolean isPayloadDangerLabel(String label) {
        String norm = Strings.normalize(label);
        return norm.contains("failed") || norm.equals("reason") || norm.contains("records failed")
                || norm.equals("issues found") || norm.equals("issues");
    }

    private boolean isPayloadSuccessLabel(String label) {
        String norm = Strings.normalize(label);
        return norm.contains("accepted") || norm.equals("result") || norm.equals("value");
    }

    private boolean isPayloadLinkLabel(String label) {
        String norm = Strings.normalize(label);
        return norm.equals("box") || norm.equals("case") || norm.equals("user") || norm.equals("document");
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
        String area = Strings.normalize(displayArea(entry));

        if (isError(entry)) {
            addRailCell(cells, createRailDetailCell(WARNING_ICON_GLYPH, primaryRailLabel(entry), primaryRailValue(entry), "logs-inline-rail-danger-cell"));
            addRailCell(cells, createRailDetailCell(WARNING_ICON_GLYPH, "Reason", failureReason(entry), "logs-inline-rail-wide-cell", "logs-inline-rail-danger-cell"));
            addRailCell(cells, createRailDetailCell(CHECK_ICON_GLYPH, "Action needed", actionNeeded(entry), "logs-inline-rail-wide-cell"));
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
                addRailCell(cells, createRailDetailCell(USER_ICON_GLYPH, "Full name", firstCreatedValue(entry, "full name", "name")));
                addRailCell(cells, createRailDetailCell(USER_ICON_GLYPH, "Username", firstCreatedValue(entry, "username")));
                addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_GLYPH, "Email", firstCreatedValue(entry, "email", "email address"), "logs-inline-rail-wide-cell"));
                addRailCell(cells, createRailDetailCell(GEAR_ICON_GLYPH, "Role", firstCreatedValue(entry, "role")));
                addRailCell(cells, createRailDetailCell(CHECK_ICON_GLYPH, "Status", firstCreatedValue(entry, "status"), "logs-inline-rail-success-cell"));
                addLogRailCell(cells, entry);
                return cells;
            }

            if (area.equals("profiles")) {
                addRailCell(cells, createRailDetailCell(GEAR_ICON_GLYPH, "Profile", primaryRailValue(entry), "logs-inline-rail-primary-cell"));
                addRailCell(cells, createRailDetailCell(USER_ICON_GLYPH, "Client", firstCreatedValue(entry, "client", "customer")));
                addRailCell(cells, createRailDetailCell(REFRESH_ICON_GLYPH, "Split rule", firstCreatedValue(entry, "split rule", "barcode splitting", "barcode")));
                addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_GLYPH, "Export label", firstCreatedValue(entry, "export label", "export naming", "export format"), "logs-inline-rail-wide-cell"));
                addLogRailCell(cells, entry);
                return cells;
            }

            if (area.equals("documents")) {
                addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_GLYPH, "Note", Strings.displayText(entry.description(), "A document detail item was created."), "logs-inline-rail-note-cell"));
                addRailCell(cells, createRailDetailCell(GEAR_ICON_GLYPH, "Category", documentCategory(entry)));
                addLogRailCell(cells, entry);
                return cells;
            }
        }

        if (isDeleteEvent(entry)) {
            if (area.equals("users")) {
                addRailCell(cells, createRailDetailCell(USER_ICON_GLYPH, "User", firstDeletedValue(entry, "full name", "name", "user"), "logs-inline-rail-primary-cell"));
                addRailCell(cells, createRailDetailCell(USER_ICON_GLYPH, "Username", firstDeletedValue(entry, "username")));
                addRailCell(cells, createRailDetailCell(WARNING_ICON_GLYPH, "Action", deleteActionText(entry), "logs-inline-rail-danger-cell"));
                addLogRailCell(cells, entry);
                return cells;
            }

            if (area.equals("profiles")) {
                addRailCell(cells, createRailDetailCell(GEAR_ICON_GLYPH, "Profile", firstDeletedValue(entry, "profile", "profile name", "name"), "logs-inline-rail-primary-cell"));
                addRailCell(cells, createRailDetailCell(REFRESH_ICON_GLYPH, "Split rule", firstDeletedValue(entry, "split rule", "barcode splitting")));
                addRailCell(cells, createRailDetailCell(WARNING_ICON_GLYPH, "Action", deleteActionText(entry), "logs-inline-rail-danger-cell"));
                addLogRailCell(cells, entry);
                return cells;
            }

            addRailCell(cells, createRailDetailCell(WARNING_ICON_GLYPH, primaryRailLabel(entry), primaryRailValue(entry), "logs-inline-rail-danger-cell"));
            compactVisibleRows(snapshotRowsForChangeEvent(entry), 3)
                    .forEach(row -> addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_GLYPH, row.label(), row.value())));
            addLogRailCell(cells, entry);
            return cells;
        }

        if (area.equals("exports")) {
            addRailCell(cells, createRailDetailCell(DOWNLOAD_ICON_GLYPH, "Note", Strings.displayText(entry.description(), "A report was exported."), "logs-inline-rail-note-cell"));
            addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_GLYPH, "Export mode", firstContextValue(entry, "export mode", "mode", "format")));
            addLogRailCell(cells, entry);
            return cells;
        }

        if (area.equals("documents")) {
            addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_GLYPH, "Note", Strings.displayText(entry.description(), "Document detail activity was recorded."), "logs-inline-rail-note-cell"));
            addRailCell(cells, createRailDetailCell(GEAR_ICON_GLYPH, "Category", documentCategory(entry)));
            addLogRailCell(cells, entry);
            return cells;
        }

        if (area.equals("qa")) {
            addRailCell(cells, createRailDetailCell(CHECK_ICON_GLYPH, "Document", firstContextOrTarget(entry, "document", "document id", "box", "box id"), "logs-inline-rail-primary-cell"));
            addRailCell(cells, createRailDetailCell(CHECK_ICON_GLYPH, "Checklist", firstContextValue(entry, "checklist", "checklist count")));
            addRailCell(cells, createRailDetailCell(WARNING_ICON_GLYPH, "Issues", firstContextValue(entry, "issues", "issue count")));
            addRailCell(cells, createRailDetailCell(CHECK_ICON_GLYPH, "Result", displayStatus(entry.status()), "logs-inline-rail-success-cell"));
            addLogRailCell(cells, entry);
            return cells;
        }

        if (isTiffActivity(entry)) {
            List<ActivityDetailRow> tiffRows = compactVisibleRows(tiffMetricRows(entry), 5);
            if (tiffRows.isEmpty()) {
                tiffRows = compactVisibleRows(compactDetailRows(entry), 5);
            }
            tiffRows.forEach(row -> addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_GLYPH, row.label(), row.value())));
            addLogRailCell(cells, entry);
            return cells;
        }

        compactVisibleRows(compactDetailRows(entry), 5)
                .forEach(row -> addRailCell(cells, createRailDetailCell(GEAR_ICON_GLYPH, row.label(), row.value())));

        if (cells.isEmpty()) {
            addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_GLYPH, "Note", Strings.displayText(entry.description(), "Activity was recorded."), "logs-inline-rail-note-cell"));
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
        addRailCell(cells, createRailDetailCell(DOCUMENT_ICON_GLYPH, "Log ID", "LOG-" + entry.id(), "logs-inline-rail-log-cell"));
    }

    private HBox createRailDetailCell(String iconPath, String label, String value, String... styleClasses) {
        String displayValue = displayAuditValue(value);
        if (isMissingAuditValue(displayValue) || "\u2014".equals(displayValue)) {
            return null;
        }

        StackPane icon = createRailIcon(iconPath);

        Label labelNode = new Label(Strings.displayText(label, "Detail"));
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

        StackPane icon = createRailIcon(sensitive ? USER_ICON_GLYPH : REFRESH_ICON_GLYPH);

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

        Label fieldNode = new Label(Strings.displayText(change.field(), "Field"));
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

        StackPane icon = createRailIcon(sensitive ? USER_ICON_GLYPH : REFRESH_ICON_GLYPH);

        Label fieldNode = new Label(Strings.displayText(field, "Field"));
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
        return Strings.normalize(formatAction(entry.action())).contains("password")
                || entry.changes().stream().anyMatch(change -> Strings.normalize(change.field()).contains("password"));
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
        List<String> normalizedLabels = Arrays.stream(labels)
                .map(Strings::normalize)
                .toList();

        String exactMatch = entry.changes().stream()
                .filter(change -> normalizedLabels.contains(Strings.normalize(change.field())))
                .map(change -> newValue ? change.newValue() : change.oldValue())
                .filter(value -> !isMissingAuditValue(value))
                .findFirst()
                .orElse("");

        if (!exactMatch.isBlank()) {
            return exactMatch;
        }

        return entry.changes().stream()
                .filter(change -> normalizedLabels.stream().anyMatch(label -> Strings.normalize(change.field()).contains(label)))
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
        return "\u2014".equals(displayAuditValue(target)) ? Strings.displayText(entry.target(), "") : target;
    }

    private String failureReason(ActivityLogEntry entry) {
        String reason = Strings.displayText(entry.description(), "");
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

        String area = Strings.normalize(displayArea(entry));
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
        String action = Strings.normalize(formatAction(entry.action()));
        return action.contains("deactivated") || action.contains("inactive") ? "Deactivated" : "Deleted";
    }

    private String documentCategory(ActivityLogEntry entry) {
        String action = Strings.normalize(formatAction(entry.action()));
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
        String area = Strings.normalize(displayArea(entry));

        if (!entry.changes().isEmpty() && (isCreateEvent(entry) || isDeleteEvent(entry))) {
            rows.addAll(snapshotRowsForChangeEvent(entry));
        } else if (isError(entry) && (area.equals("import") || isTiffActivity(entry))) {
            rows.add(new ActivityDetailRow("File", displayTiffItem(entry)));

            String reason = Strings.displayText(entry.description(), "");
            if (!reason.isBlank()) {
                rows.add(new ActivityDetailRow("Reason", reason));
            }

            rows.addAll(normalizedContextRows(entry));

            if (rows.stream().noneMatch(row -> Strings.normalize(row.label()).contains("accepted"))) {
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

            String normalizedLabel = Strings.normalize(row.label());
            if (normalizedLabel.equals("trace") || normalizedLabel.equals("log id") || normalizedLabel.equals("recorded")) {
                continue;
            }

            String key = normalizedLabel + "|" + Strings.normalize(displayAuditValue(row.value()));
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
        String area = Strings.normalize(displayArea(entry));

        switch (area) {
            case "users", "access", "security" -> {
                // Who was affected
                String target = Strings.displayText(entry.target(), "");
                if (!target.isBlank() && !Strings.normalize(target).equals("system")) {
                    rows.add(new ActivityDetailRow("User", target));
                }
                // Key identifiers pulled from change snapshots (works for create/delete/update)
                String username = firstCreatedValue(entry, "username");
                if (username.isBlank()) username = firstDeletedValue(entry, "username");
                if (!username.isBlank()) rows.add(new ActivityDetailRow("Username", username));

                String email = firstCreatedValue(entry, "email", "email address");
                if (email.isBlank()) email = firstDeletedValue(entry, "email", "email address");
                if (!email.isBlank()) rows.add(new ActivityDetailRow("Email", email));

                String role = firstCreatedValue(entry, "role");
                if (role.isBlank()) role = firstDeletedValue(entry, "role");
                if (!role.isBlank()) rows.add(new ActivityDetailRow("Role", role));
            }
            case "profiles" -> {
                String client = firstCreatedValue(entry, "client", "customer");
                if (client.isBlank()) client = firstDeletedValue(entry, "client", "customer");
                if (!client.isBlank()) rows.add(new ActivityDetailRow("Client", client));

                String splitRule = firstCreatedValue(entry, "split rule", "barcode splitting", "barcode");
                if (splitRule.isBlank()) splitRule = firstDeletedValue(entry, "split rule", "barcode splitting", "barcode");
                if (!splitRule.isBlank()) rows.add(new ActivityDetailRow("Split rule", splitRule));

                String exportLabel = firstCreatedValue(entry, "export label", "export naming", "export format");
                if (exportLabel.isBlank()) exportLabel = firstDeletedValue(entry, "export label", "export naming", "export format");
                if (!exportLabel.isBlank()) rows.add(new ActivityDetailRow("Export label", exportLabel));
            }
            case "qa" -> {
                String document = firstContextValue(entry, "document", "document id");
                if (!document.isBlank()) rows.add(new ActivityDetailRow("Document", document));
                String checklist = firstContextValue(entry, "checklist", "checklist count");
                if (!checklist.isBlank()) rows.add(new ActivityDetailRow("Checklist", checklist));
                String issues = firstContextValue(entry, "issues", "issue count");
                if (!issues.isBlank()) rows.add(new ActivityDetailRow("Issues", issues));
                rows.add(new ActivityDetailRow("Result", displayStatus(entry.status())));
            }
            case "exports" -> {
                String mode = firstContextValue(entry, "export mode", "mode", "format");
                if (!mode.isBlank()) rows.add(new ActivityDetailRow("Export mode", mode));
                String records = firstContextValue(entry, "records", "record count", "total records");
                if (!records.isBlank()) rows.add(new ActivityDetailRow("Records", records));
                addFirstDetail(rows, entry, "Box", "box", "box id");
            }
            default -> {
                // TIFF / scan / import / documents / system
                addFirstDetail(rows, entry, "Box", "box", "box id");
                addFirstDetail(rows, entry, "Profile", "profile");
                addFirstDetail(rows, entry, "Case", "case", "case id");
                addFirstDetail(rows, entry, "File ID", "file id", "file");
                addFirstDetail(rows, entry, "Path", "path", "storage path");
            }
        }

        rows.add(new ActivityDetailRow("Trace", "LOG-" + entry.id() + " · " + entry.fullTimestamp()));
        return rows;
    }

    // ── Sidebar section (stacked rows + coloured icons) ──────────────────────

    private VBox createPayloadSidebarSection(ActivityLogEntry entry, String title, List<ActivityDetailRow> rows) {
        VBox section = new VBox(0);
        section.getStyleClass().add("logs-payload-section");
        section.setFillWidth(true);
        section.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("logs-payload-section-title");
        section.getChildren().add(titleLabel);

        String iconColorClass = sidebarIconColorClass(entry);

        List<ActivityDetailRow> visible = rows.stream()
                .filter(r -> !isMissingAuditValue(r.value()))
                .toList();

        for (int i = 0; i < visible.size(); i++) {
            if (i > 0) section.getChildren().add(createPayloadRowDivider());
            ActivityDetailRow row = visible.get(i);
            String valueStyle = isPayloadDangerLabel(row.label()) ? "logs-payload-kv-value-danger"
                    : isPayloadSuccessLabel(row.label()) ? "logs-payload-kv-value-success"
                    : isPayloadLinkLabel(row.label()) ? "logs-payload-kv-value-link"
                    : null;
            section.getChildren().add(createPayloadSidebarRow(
                    iconForPayloadLabel(row.label()), iconColorClass,
                    row.label(), displayAuditValue(row.value()), valueStyle));
        }
        return section;
    }

    private HBox createPayloadSidebarRow(String iconPath, String iconColorClass,
                                         String label, String value, String valueStyle) {
        Label iconLabel = createPrimeIcon(iconPath, "logs-payload-sidebar-icon");
        iconLabel.getStyleClass().add(iconColorClass);
        StackPane iconShell = new StackPane(iconLabel);
        iconShell.getStyleClass().add("logs-payload-sidebar-icon-shell");

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("logs-payload-sidebar-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("logs-payload-sidebar-value");
        if (valueStyle != null && !valueStyle.isBlank()) {
            valueNode.getStyleClass().add(valueStyle);
        }
        valueNode.setWrapText(true);
        valueNode.setMinWidth(0);
        valueNode.setMaxWidth(Double.MAX_VALUE);

        VBox stack = new VBox(2, labelNode, valueNode);
        stack.setMinWidth(0);
        stack.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(stack, Priority.ALWAYS);

        HBox row = new HBox(10, iconShell, stack);
        row.getStyleClass().add("logs-payload-sidebar-row");
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private String sidebarIconColorClass(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        return switch (area) {
            case "users", "access", "security", "profiles" -> "logs-payload-sidebar-icon-purple";
            case "qa" -> "logs-payload-sidebar-icon-teal";
            case "files", "import", "documents" -> "logs-payload-sidebar-icon-blue";
            default -> "logs-payload-sidebar-icon-neutral";
        };
    }

    private List<ActivityDetailRow> buildQaDetailRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        String doc = firstContextOrTarget(entry, "document", "document id", "box", "box id");
        if (!doc.isBlank()) rows.add(new ActivityDetailRow("Document", doc));
        addFirstDetail(rows, entry, "Checklist items", "checklist", "checklist count");
        addFirstDetail(rows, entry, "Issues found", "issues", "issue count");
        String result = displayStatus(entry.status());
        if (!result.isBlank()) rows.add(new ActivityDetailRow("Result", result));
        String desc = Strings.displayText(entry.description(), "");
        if (!desc.isBlank()) rows.add(new ActivityDetailRow("Note", desc));
        return rows;
    }

    private List<ActivityDetailRow> buildExportDetailRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        addFirstDetail(rows, entry, "Export mode", "export mode", "mode", "format");
        addFirstDetail(rows, entry, "Records exported", "records", "record count", "total records");
        addFirstDetail(rows, entry, "Box", "box", "box id");
        String desc = Strings.displayText(entry.description(), "");
        if (!desc.isBlank()) rows.add(new ActivityDetailRow("Note", desc));
        return rows;
    }

    private List<ActivityDetailRow> buildGeneralContextRows(ActivityLogEntry entry, String area, boolean hasMainContent) {
        // Skip areas that already have dedicated sections
        if (isTiffActivity(entry) || area.equals("import") || area.equals("qa") || area.equals("exports")) {
            return List.of();
        }
        // For user/profile events, only show context if there's something novel not already in hero or snapshot
        if ((area.equals("users") || area.equals("profiles") || area.equals("access") || area.equals("security"))
                && hasMainContent) {
            return List.of();
        }
        // For events that got no main content: show description + any context details
        if (!hasMainContent) {
            List<ActivityDetailRow> rows = new ArrayList<>();
            String desc = Strings.displayText(entry.description(), "");
            if (!desc.isBlank()) rows.add(new ActivityDetailRow("Note", desc));
            rows.addAll(normalizedContextRows(entry).stream()
                    .filter(r -> !Strings.normalize(r.label()).contains("box")
                            && !Strings.normalize(r.label()).contains("profile")
                            && !Strings.normalize(r.label()).contains("file"))
                    .limit(6)
                    .toList());
            return rows;
        }
        // For document/metadata/other events that have main content but still have context to surface
        return normalizedContextRows(entry).stream()
                .filter(r -> !isMissingAuditValue(r.value()))
                .filter(r -> {
                    String norm = Strings.normalize(r.label());
                    return !norm.contains("box") && !norm.contains("profile")
                            && !norm.contains("file") && !norm.contains("case");
                })
                .limit(4)
                .toList();
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
            rows.add(new ActivityDetailRow("Failure reason", Strings.displayText(entry.description(), "No failure reason recorded.")));
            rows.addAll(contextRows);
            return rows;
        }

        if (entry.changes().isEmpty()) {
            if (!contextRows.isEmpty()) {
                return contextRows;
            }

            String description = Strings.displayText(entry.description(), "");
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

    private String normalizeDetailLabel(String label) {
        String normalizedLabel = Strings.normalize(label);

        return switch (normalizedLabel) {
            case "file", "file id", "filename", "file name" -> "TIFF file";
            case "box", "box id" -> "Box";
            case "case", "case id" -> "Case";
            case "document", "document id" -> "Document";
            case "page", "page number" -> "Page";
            case "profile", "profile name" -> "Scan profile";
            default -> Strings.displayText(label, "Detail");
        };
    }

    private boolean hasVisibleChange(ActivityChange change) {
        return !isMissingAuditValue(change.oldValue()) || !isMissingAuditValue(change.newValue());
    }

    private boolean isMissingAuditValue(String value) {
        return value == null || value.trim().isBlank() || "null".equalsIgnoreCase(value.trim());
    }

    private boolean isStateSnapshotField(String field) {
        String normalizedField = Strings.normalize(field);
        return "account state".equals(normalizedField)
                || "profile state".equals(normalizedField)
                || "template state".equals(normalizedField);
    }

    private boolean isCreateEvent(ActivityLogEntry entry) {
        String action = Strings.normalize(formatAction(entry.action()));
        return action.contains("created") || action.startsWith("create ");
    }

    private boolean isDeleteEvent(ActivityLogEntry entry) {
        String action = Strings.normalize(formatAction(entry.action()));
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
        return matcher.find() ? matcher.group() : "â€”";
    }

    private boolean hasRealTargetId(String targetId) {
        if (isMissingAuditValue(targetId) || "â€”".equals(targetId.trim())) {
            return false;
        }

        String cleanedValue = targetId.trim();

        return TARGET_ID_PATTERN.matcher(cleanedValue).matches()
                || cleanedValue.matches("\\d+")
                || (!cleanedValue.contains(".") && cleanedValue.matches("[A-Za-z0-9_-]{3,}"));
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

        if (item.isBlank()) {
            return "[" + area + "] " + actor + " " + action;
        }

        return "[" + area + "] " + actor + " " + action + " " + item;
    }

    private String shortActor(String actor) {
        String displayActor = displayActor(actor);

        if (Strings.normalize(displayActor).contains("admin")) {
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
        // Always surface box context on the collapsed meta line when available
        String box = firstContextValue(entry, "box", "box id");

        if (isError(entry) && !Strings.displayText(entry.description(), "").isBlank()) {
            String desc = entry.description();
            return box.isBlank() ? desc : "Box " + box + " · " + desc;
        }

        if (isTiffActivity(entry)) {
            String fileSize = firstContextValue(entry, "file size", "size");
            String pages = firstContextValue(entry, "pages", "page count", "page");
            String resolution = firstContextValue(entry, "resolution", "dpi");
            List<String> parts = new ArrayList<>();

            if (!box.isBlank()) {
                parts.add("Box " + box);
            }

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
            String changeText = entry.changes().size() == 1
                    ? entry.changes().get(0).field() + " changed"
                    : entry.changes().size() + " fields changed";
            return box.isBlank() ? changeText : "Box " + box + " · " + changeText;
        }

        String fallback = Strings.displayText(entry.description(), Strings.displayText(entry.target(), "Audit event"));
        return box.isBlank() ? fallback : "Box " + box + " · " + fallback;
    }

    private String rowAreaChipText(ActivityLogEntry entry) {
        return isTiffActivity(entry) ? "TIFF" : displayArea(entry);
    }

    private String eventIconPath(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        String action = Strings.normalize(entry.action());

        if (isError(entry)) {
            return WARNING_ICON_GLYPH;
        }

        if (area.equals("users") || area.equals("access") || area.equals("security")) {
            return USER_ICON_GLYPH;
        }

        if (area.equals("qa")) {
            return CHECK_ICON_GLYPH;
        }

        if (area.equals("exports")) {
            return DOWNLOAD_ICON_GLYPH;
        }

        if (action.contains("replace") || action.contains("retry")) {
            return REFRESH_ICON_GLYPH;
        }

        if (isTiffActivity(entry) || area.equals("import")) {
            return UPLOAD_ICON_GLYPH;
        }

        return GEAR_ICON_GLYPH;
    }

    private String eventIconClass(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));

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

        String rawTarget = Strings.displayText(entry.target(), "");
        if (rawTarget.isBlank()) {
            return "";
        }

        String target = displayAuditValue(rawTarget);

        for (String part : target.split("/")) {
            String cleanedPart = part.trim();
            String normalizedPart = Strings.normalize(cleanedPart);

            if (normalizedPart.startsWith("file ")) {
                return Strings.displayText(cleanedPart.substring(5), target);
            }

            if (normalizedPart.endsWith(".tif") || normalizedPart.endsWith(".tiff")) {
                return cleanedPart;
            }
        }

        return isMissingAuditValue(rawTarget) ? "" : target;
    }

    private String firstContextValue(ActivityLogEntry entry, String... labels) {
        List<String> normalizedLabels = Arrays.stream(labels)
                .map(Strings::normalize)
                .toList();

        return entry.contextDetails().stream()
                .filter(row -> normalizedLabels.contains(Strings.normalize(row.label())))
                .map(ActivityDetailRow::value)
                .filter(value -> !isMissingAuditValue(value))
                .findFirst()
                .orElse("");
    }

    private String displayArea(ActivityLogEntry entry) {
        String normalizedType = Strings.normalize(entry.type());
        String normalizedAction = Strings.normalize(entry.action());

        if (normalizedAction.contains("import")) {
            return "Import";
        }

        if ("review".equals(normalizedType)) {
            return "QA";
        }

        if (isTiffActivity(entry)) {
            return "Files";
        }

        return Strings.displayText(entry.type(), "System");
    }

    private String areaBadgeClass(ActivityLogEntry entry) {
        String normalizedType = Strings.normalize(displayArea(entry));

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
        String type = Strings.normalize(entry.type());
        String action = Strings.normalize(entry.action());
        String target = Strings.normalize(entry.target());

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
        String searchText = Strings.normalize(searchField == null ? "" : searchField.getText());

        if (searchText.isBlank()) {
            return true;
        }

        return Strings.normalize(displayArea(entry)).contains(searchText)
                || Strings.normalize(entry.type()).contains(searchText)
                || Strings.normalize(entry.actor()).contains(searchText)
                || Strings.normalize(entry.action()).contains(searchText)
                || Strings.normalize(entry.target()).contains(searchText)
                || Strings.normalize(entry.status()).contains(searchText)
                || Strings.normalize(entry.description()).contains(searchText)
                || Strings.normalize(entry.fullTimestamp()).contains(searchText)
                || entry.changes().stream().anyMatch(change ->
                Strings.normalize(change.field()).contains(searchText)
                        || Strings.normalize(change.oldValue()).contains(searchText)
                        || Strings.normalize(change.newValue()).contains(searchText)
        ) || entry.contextDetails().stream().anyMatch(row ->
                Strings.normalize(row.label()).contains(searchText)
                        || Strings.normalize(row.searchText()).contains(searchText)
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
                || Strings.displayText(entry.type(), "").equalsIgnoreCase(selectedArea);
    }

    private boolean matchesUser(ActivityLogEntry entry) {
        String selectedUser = comboValue(userFilterComboBox);

        return selectedUser.isBlank()
                || ALL_USERS.equals(selectedUser)
                || Strings.displayText(entry.actor(), "").equalsIgnoreCase(selectedUser);
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
        dateFilterMenuButton.setGraphic(createFilterGraphic(DATE_FILTER_ICON_GLYPH, "Date", value));
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

    private String statusBadgeClass(String status) {
        return switch (Strings.normalize(status)) {
            case "success" -> "logs-result-success";
            case "failed", "error" -> "logs-result-failed";
            default -> "logs-result-info";
        };
    }

    private boolean isError(ActivityLogEntry entry) {
        String status = Strings.normalize(entry.status());

        return "failed".equals(status)
                || "error".equals(status)
                || Strings.normalize(entry.action()).contains("failed")
                || Strings.normalize(entry.action()).contains("rejected");
    }

    private String displayStatus(String status) {
        String normalizedStatus = Strings.normalize(status);

        return "failed".equals(normalizedStatus) || "error".equals(normalizedStatus)
                ? "Failed"
                : Strings.displayText(status, "Info");
    }

    private String displayActor(String actor) {
        return isSystemActor(actor) ? "System" : Strings.displayText(actor, "System");
    }

    private boolean isSystemActor(String actor) {
        String normalizedActor = Strings.normalize(actor);
        return normalizedActor.isBlank() || "system".equals(normalizedActor);
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

        int count = (int) Math.min(visibleChanges(entry).size(), 6);
        return count + " field" + (count == 1 ? "" : "s") + " changed";
    }

    private String formatAction(String action) {
        String cleanedAction = Strings.displayText(action, "Activity");
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
                Strings.displayText(log.getType(), "System"),
                Strings.displayText(log.getActor(), "System"),
                Strings.displayText(log.getAction(), "Activity"),
                Strings.displayText(log.getTarget(), ""),
                Strings.displayText(log.getStatus(), "Info"),
                Strings.displayText(log.getDescription(), ""),
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
