package easv.gui.controller.admin;

import easv.be.AuditLog;
import easv.bll.AdminManager;
import easv.gui.controller.utilities.AppDates;
import easv.gui.PrimeIcons;
import easv.util.Strings;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final String ALL_RESULTS = "All statuses";
    private static final String SORT_NEWEST_FIRST = "Newest first";
    private static final String SORT_OLDEST_FIRST = "Oldest first";
    private static final String SORT_ACTION_ASC = "A to Z (Action)";
    private static final String SORT_ACTION_DESC = "Z to A (Action)";
    private static final String STATUS_SELECT_ALL = "Select all";
    private static final List<String> STATUS_OPTIONS = List.of("Success", "Failed", "Warning", "Info");
    private static final List<String> AREA_OPTIONS = List.of(
            ALL_AREAS,
            "Files",
            "QA",
            "Users",
            "Profiles",
            "Access",
            "Exports",
            "Security",
            "System"
    );
    private static final List<String> SORT_OPTIONS = List.of(
            SORT_NEWEST_FIRST,
            SORT_OLDEST_FIRST,
            SORT_ACTION_ASC,
            SORT_ACTION_DESC
    );

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
    private static final String COPY_ICON_GLYPH = "\ue92c";
    private static final String TRASH_ICON_GLYPH = "\ue93d";

    private static final String AREA_FILTER_ICON_GLYPH = "\ue941";
    private static final String RESULT_FILTER_ICON_GLYPH = "\ue90a";
    private static final String DATE_FILTER_ICON_GLYPH = "\ue927";
    private static final String SORT_FILTER_ICON_GLYPH = "\ue915";
    private static final String FILTER_ICON_GLYPH = "\ue94c";

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
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<ActivityLogEntry> activityEntries = FXCollections.observableArrayList();
    private final List<String> recentSearches = new ArrayList<>(List.of(
            "User updated",
            "Inventory",
            "Order cancelled",
            "Login failed",
            "User deleted"
    ));
    private final Set<String> selectedStatuses = new LinkedHashSet<>();
    private final Set<String> pendingStatuses = new LinkedHashSet<>();
    private final List<String> filterUserOptions = new ArrayList<>(List.of(ALL_USERS));

    private AdminManager adminManager;
    private String selectedEntryId;
    private VBox selectedLogRow;
    private ScrollPane logListScroll;
    private boolean logKeyNavAttached;
    private boolean detailClosed;
    private ContextMenu searchHistoryMenu;

    private boolean updatingDateControls;
    private DateFilterMode dateFilterMode = DateFilterMode.ALL;
    private LocalDate specificDate;
    private LocalDate rangeStartDate;
    private LocalDate rangeEndDate;
    private LocalTime rangeStartTime;
    private LocalTime rangeEndTime;
    private YearMonth displayedCalendarMonth = YearMonth.now();
    private boolean awaitingRangeEnd;
    private String selectedArea = ALL_AREAS;
    private String pendingArea = ALL_AREAS;
    private String selectedUser = ALL_USERS;
    private String pendingUser = ALL_USERS;
    private String selectedSort = SORT_NEWEST_FIRST;

    @FXML private TextField searchField;
    @FXML private HBox searchBox;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> userFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> sortFilterComboBox;

    @FXML private MenuButton dateFilterMenuButton;
    @FXML private MenuButton statusMenuButton;
    @FXML private MenuButton filtersMenuButton;
    @FXML private MenuButton sortMenuButton;
    @FXML private Button specificDateModeButton;
    @FXML private Button rangeDateModeButton;
    @FXML private VBox specificDateBox;
    @FXML private VBox dateRangeBox;
    @FXML private DatePicker specificDatePicker;
    @FXML private DatePicker rangeStartDatePicker;
    @FXML private DatePicker rangeEndDatePicker;
    @FXML private TextField rangeStartTimeField;
    @FXML private TextField rangeEndTimeField;
    @FXML private Label dateCalendarMonthLabel;
    @FXML private GridPane dateCalendarGrid;

    @FXML private VBox logsPageRoot;
    @FXML private VBox timelineContainer;
    @FXML private VBox emptyStateBox;
    @FXML private Label logsShowingLabel;

    @FXML
    private void initialize() {
        configureFilters();
        configureToolbarGraphics();
        configureListeners();
        installLogKeyNav();
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
        selectedStatuses.clear();
        selectedStatuses.addAll(STATUS_OPTIONS);
        pendingStatuses.clear();
        pendingStatuses.addAll(STATUS_OPTIONS);
        selectedArea = ALL_AREAS;
        pendingArea = ALL_AREAS;
        selectedUser = ALL_USERS;
        pendingUser = ALL_USERS;
        selectedSort = SORT_NEWEST_FIRST;

        if (typeFilterComboBox != null) {
            typeFilterComboBox.getItems().setAll(AREA_OPTIONS);
            typeFilterComboBox.setValue(ALL_AREAS);
        }

        if (userFilterComboBox != null) {
            userFilterComboBox.getItems().setAll(ALL_USERS);
            userFilterComboBox.setValue(ALL_USERS);
        }

        if (statusFilterComboBox != null) {
            statusFilterComboBox.getItems().setAll(ALL_RESULTS);
            statusFilterComboBox.getItems().addAll(STATUS_OPTIONS);
            statusFilterComboBox.setValue(ALL_RESULTS);
        }

        if (sortFilterComboBox != null) {
            sortFilterComboBox.getItems().setAll(SORT_OPTIONS);
            sortFilterComboBox.setValue(SORT_NEWEST_FIRST);
        }

        dateFilterMode = DateFilterMode.ALL;
        updateDateFilterState();
        updateCalendarDisplay();
    }

    private void configureToolbarGraphics() {
        configureFilterComboBox(typeFilterComboBox, "Filters", AREA_FILTER_ICON_GLYPH);
        configureFilterComboBox(userFilterComboBox, "User", USER_ICON_GLYPH);
        configureFilterComboBox(statusFilterComboBox, "Status", RESULT_FILTER_ICON_GLYPH);
        configureFilterComboBox(sortFilterComboBox, "Sort", SORT_FILTER_ICON_GLYPH);
        configureSearchHistoryMenu();
        configureStatusMenu();
        configureFiltersMenu();
        configureSortMenu();

        if (dateFilterMenuButton != null) {
            dateFilterMenuButton.setText(null);
            dateFilterMenuButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            dateFilterMenuButton.setOnShowing(event -> prepareDatePopover());
            setDateFilterButtonDisplay("Date & Time");
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

        Label valueLabel = new Label(displayFilterButtonText(heading, value));
        valueLabel.getStyleClass().add("logs-filter-button-text");
        valueLabel.setMinWidth(0);
        valueLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        valueLabel.setWrapText(false);

        HBox graphic = new HBox(9, iconShell, valueLabel);
        graphic.getStyleClass().add("logs-filter-graphic");
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.setMinWidth(0);
        graphic.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        return graphic;
    }

    private String displayFilterButtonText(String heading, String value) {
        String cleanHeading = Strings.displayText(heading, "");
        String cleanValue = Strings.displayText(value, "");

        if ("Date".equals(cleanHeading)) {
            return cleanValue.isBlank() || "All dates".equals(cleanValue) ? "Date & Time" : cleanValue;
        }

        if ("Sort".equals(cleanHeading)) {
            return cleanValue.isBlank() ? SORT_NEWEST_FIRST : cleanValue;
        }

        if ("Filters".equals(cleanHeading)) {
            return cleanValue.isBlank() || ALL_AREAS.equals(cleanValue) ? "Filters" : cleanValue;
        }

        if ("Status".equals(cleanHeading)) {
            return cleanValue.isBlank() || ALL_RESULTS.equals(cleanValue) ? "Status" : cleanValue;
        }

        if ("User".equals(cleanHeading)) {
            return cleanValue.isBlank() || ALL_USERS.equals(cleanValue) ? "User" : cleanValue;
        }

        return cleanValue.isBlank() ? cleanHeading : cleanValue;
    }

    private void setMenuButtonDisplay(MenuButton menuButton, String iconGlyph, String heading, String value) {
        if (menuButton == null) {
            return;
        }

        menuButton.setText(null);
        menuButton.setGraphic(createFilterGraphic(iconGlyph, heading, value));
        menuButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        menuButton.setAccessibleText(displayFilterButtonText(heading, value));
    }

    private void configureSearchHistoryMenu() {
        if (searchField == null) {
            return;
        }

        searchHistoryMenu = new ContextMenu();
        searchHistoryMenu.getStyleClass().add("logs-search-popover-menu");
    }

    private void showSearchHistoryMenu() {
        if (searchField == null || searchHistoryMenu == null || searchField.getScene() == null) {
            return;
        }

        List<String> visibleSearches = visibleRecentSearches();

        if (visibleSearches.isEmpty()) {
            searchHistoryMenu.hide();
            return;
        }

        searchHistoryMenu.getItems().setAll(new CustomMenuItem(createSearchHistoryContent(visibleSearches), false));

        if (!searchHistoryMenu.isShowing()) {
            Node anchor = searchBox == null ? searchField : searchBox;
            searchHistoryMenu.show(anchor, Side.BOTTOM, 0, -1);
        }
    }

    private List<String> visibleRecentSearches() {
        String searchText = Strings.normalize(searchField == null ? "" : searchField.getText());

        if (searchText.isBlank()) {
            return List.of();
        }

        return recentSearches.stream()
                .filter(recentSearch -> Strings.normalize(recentSearch).contains(searchText))
                .toList();
    }

    private VBox createSearchHistoryContent(List<String> visibleSearches) {
        VBox content = new VBox(9);
        content.getStyleClass().add("logs-search-popover");

        HBox header = new HBox(9);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Recent searches");
        title.getStyleClass().add("logs-popover-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("logs-popover-link-button");
        clearButton.setFocusTraversable(false);
        clearButton.setOnAction(event -> {
            recentSearches.clear();
            showSearchHistoryMenu();
        });

        header.getChildren().addAll(title, spacer, clearButton);
        content.getChildren().add(header);

        for (String recentSearch : visibleSearches) {
            Button row = new Button();
            row.getStyleClass().add("logs-search-recent-row");
            row.setFocusTraversable(false);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setGraphic(new HBox(9,
                    createPrimeIcon(CLOCK_ICON_GLYPH, "logs-search-recent-icon"),
                    createSearchRecentLabel(recentSearch)
            ));
            row.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            row.setOnAction(event -> {
                searchField.setText(recentSearch);
                rememberSearch(recentSearch);
                searchHistoryMenu.hide();
            });
            content.getChildren().add(row);
        }

        return content;
    }

    private Label createSearchRecentLabel(String recentSearch) {
        Label label = new Label(recentSearch);
        label.getStyleClass().add("logs-search-recent-text");
        label.setMinWidth(0);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        return label;
    }

    private void rememberSearch(String value) {
        String search = Strings.clean(value);

        if (search.isBlank()) {
            return;
        }

        recentSearches.removeIf(existing -> existing.equalsIgnoreCase(search));
        recentSearches.add(0, search);

        while (recentSearches.size() > 5) {
            recentSearches.remove(recentSearches.size() - 1);
        }
    }

    private void configureStatusMenu() {
        if (statusMenuButton == null) {
            return;
        }

        setMenuButtonDisplay(statusMenuButton, RESULT_FILTER_ICON_GLYPH, "Status", statusButtonText());
        statusMenuButton.setOnShowing(event -> {
            pendingStatuses.clear();
            pendingStatuses.addAll(selectedStatuses);
            statusMenuButton.getItems().setAll(new CustomMenuItem(createStatusPopover(), false));
        });
        statusMenuButton.getItems().setAll(new CustomMenuItem(createStatusPopover(), false));
    }

    private VBox createStatusPopover() {
        VBox content = new VBox(0);
        content.getStyleClass().add("logs-status-popover");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("logs-status-popover-header");

        Label title = new Label("Status");
        title.getStyleClass().add("logs-status-popover-title");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label selectedCountLabel = new Label();
        selectedCountLabel.getStyleClass().add("logs-status-selected-count");
        updateStatusSelectedCountLabel(selectedCountLabel);

        header.getChildren().addAll(title, headerSpacer, selectedCountLabel);

        CheckBox allStatusesCheckBox = createStatusCheckBox(STATUS_SELECT_ALL, "neutral");
        allStatusesCheckBox.setSelected(pendingStatuses.size() == STATUS_OPTIONS.size());
        List<CheckBox> optionCheckBoxes = new ArrayList<>();
        boolean[] updatingStatusChecks = {false};

        VBox optionsBox = new VBox(0);
        optionsBox.getStyleClass().add("logs-status-options");

        allStatusesCheckBox.setOnAction(event -> {
            if (updatingStatusChecks[0]) {
                return;
            }

            pendingStatuses.clear();

            if (allStatusesCheckBox.isSelected()) {
                pendingStatuses.addAll(STATUS_OPTIONS);
            }

            updatingStatusChecks[0] = true;
            for (CheckBox optionCheckBox : optionCheckBoxes) {
                optionCheckBox.setSelected(allStatusesCheckBox.isSelected());
            }
            updatingStatusChecks[0] = false;
            updateStatusSelectedCountLabel(selectedCountLabel);
        });

        optionsBox.getChildren().add(allStatusesCheckBox);
        optionsBox.getChildren().add(createStatusDivider());

        for (String status : STATUS_OPTIONS) {
            CheckBox checkBox = createStatusCheckBox(status, Strings.normalize(status));
            checkBox.setSelected(pendingStatuses.contains(status));
            checkBox.setOnAction(event -> {
                if (updatingStatusChecks[0]) {
                    return;
                }

                if (checkBox.isSelected()) {
                    pendingStatuses.add(status);
                } else {
                    pendingStatuses.remove(status);
                }

                updatingStatusChecks[0] = true;
                allStatusesCheckBox.setSelected(pendingStatuses.size() == STATUS_OPTIONS.size());
                updatingStatusChecks[0] = false;
                updateStatusSelectedCountLabel(selectedCountLabel);
            });
            optionCheckBoxes.add(checkBox);
            optionsBox.getChildren().add(checkBox);
        }

        Label helperText = new Label("Filter audit log entries by status.");
        helperText.getStyleClass().add("logs-status-helper-text");

        HBox footer = createPopoverFooter(
                () -> {
                    pendingStatuses.clear();
                    pendingStatuses.addAll(STATUS_OPTIONS);
                    updatingStatusChecks[0] = true;
                    allStatusesCheckBox.setSelected(true);
                    for (CheckBox optionCheckBox : optionCheckBoxes) {
                        optionCheckBox.setSelected(true);
                    }
                    updatingStatusChecks[0] = false;
                    updateStatusSelectedCountLabel(selectedCountLabel);
                },
                () -> {
                    selectedStatuses.clear();
                    selectedStatuses.addAll(pendingStatuses);
                    if (selectedStatuses.isEmpty()) {
                        selectedStatuses.addAll(STATUS_OPTIONS);
                    }
                    setMenuButtonDisplay(statusMenuButton, RESULT_FILTER_ICON_GLYPH, "Status", statusButtonText());
                    statusMenuButton.hide();
                    refreshFilteredTimeline();
                }
        );

        content.getChildren().addAll(header, optionsBox, createStatusDivider(), helperText, footer);
        return content;
    }

    private CheckBox createStatusCheckBox(String text, String colorClass) {
        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().addAll("logs-status-checkbox", "logs-status-checkbox-" + colorClass);
        checkBox.setFocusTraversable(false);

        Label label = new Label(text);
        label.getStyleClass().add("logs-status-checkbox-label");

        if (STATUS_SELECT_ALL.equals(text)) {
            checkBox.setText(text);
            return checkBox;
        }

        Label pill = new Label(text);
        pill.getStyleClass().addAll("logs-status-pill", "logs-status-pill-" + colorClass);

        HBox graphic = new HBox(pill);
        graphic.setAlignment(Pos.CENTER_LEFT);
        checkBox.setGraphic(graphic);

        if ("failed".equals(colorClass)) {
            checkBox.getStyleClass().add("logs-status-checkbox-highlighted");
        }

        return checkBox;
    }

    private Region createStatusDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("logs-status-divider");
        divider.setPrefHeight(1);
        return divider;
    }

    private void updateStatusSelectedCountLabel(Label label) {
        if (label == null) {
            return;
        }

        int selectedCount = pendingStatuses.size();
        label.setText(selectedCount + " selected");
    }

    private String statusButtonText() {
        if (selectedStatuses.size() == STATUS_OPTIONS.size()) {
            return "Status";
        }

        if (selectedStatuses.size() == 1) {
            return selectedStatuses.iterator().next();
        }

        return selectedStatuses.size() + " statuses";
    }

    private void configureFiltersMenu() {
        if (filtersMenuButton == null) {
            return;
        }

        setMenuButtonDisplay(filtersMenuButton, FILTER_ICON_GLYPH, "Filters", filtersButtonText());
        filtersMenuButton.setOnShowing(event -> {
            pendingArea = selectedArea;
            pendingUser = selectedUser;
            filtersMenuButton.getItems().setAll(new CustomMenuItem(createFiltersPopover(), false));
        });
        filtersMenuButton.getItems().setAll(new CustomMenuItem(createFiltersPopover(), false));
    }

    private VBox createFiltersPopover() {
        VBox content = new VBox(12);
        content.getStyleClass().add("logs-filters-popover");

        ComboBox<String> areaComboBox = new ComboBox<>();
        areaComboBox.getItems().setAll(AREA_OPTIONS);
        areaComboBox.setValue(pendingArea);
        areaComboBox.getStyleClass().add("logs-popover-combo");
        areaComboBox.setMaxWidth(Double.MAX_VALUE);
        areaComboBox.valueProperty().addListener((observable, oldValue, newValue) -> pendingArea = newValue);

        ComboBox<String> userComboBox = new ComboBox<>();
        userComboBox.getItems().setAll(filterUserOptions);
        userComboBox.setValue(filterUserOptions.contains(pendingUser) ? pendingUser : ALL_USERS);
        userComboBox.getStyleClass().add("logs-popover-combo");
        userComboBox.setMaxWidth(Double.MAX_VALUE);
        userComboBox.valueProperty().addListener((observable, oldValue, newValue) -> pendingUser = newValue);

        content.getChildren().addAll(
                createPopoverField("Area", areaComboBox),
                createPopoverField("User", userComboBox),
                createPopoverFooter(
                        () -> {
                            pendingArea = ALL_AREAS;
                            pendingUser = ALL_USERS;
                            areaComboBox.setValue(ALL_AREAS);
                            userComboBox.setValue(ALL_USERS);
                        },
                        () -> {
                            selectedArea = Strings.displayText(pendingArea, ALL_AREAS);
                            selectedUser = Strings.displayText(pendingUser, ALL_USERS);
                            setMenuButtonDisplay(filtersMenuButton, FILTER_ICON_GLYPH, "Filters", filtersButtonText());
                            filtersMenuButton.hide();
                            refreshFilteredTimeline();
                        }
                )
        );

        return content;
    }

    private VBox createPopoverField(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("logs-date-label");

        VBox field = new VBox(6, label, control);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private String filtersButtonText() {
        int activeCount = 0;

        if (!ALL_AREAS.equals(selectedArea)) {
            activeCount++;
        }

        if (!ALL_USERS.equals(selectedUser)) {
            activeCount++;
        }

        return activeCount == 0 ? "Filters" : activeCount + " filters";
    }

    private void configureSortMenu() {
        if (sortMenuButton == null) {
            return;
        }

        setMenuButtonDisplay(sortMenuButton, SORT_FILTER_ICON_GLYPH, "Sort", selectedSort);
        sortMenuButton.setOnShowing(event ->
                sortMenuButton.getItems().setAll(new CustomMenuItem(createSortPopover(), false))
        );
        sortMenuButton.getItems().setAll(new CustomMenuItem(createSortPopover(), false));
    }

    private VBox createSortPopover() {
        VBox content = new VBox(0);
        content.getStyleClass().add("logs-sort-popover");

        for (String sortOption : SORT_OPTIONS) {
            Button row = new Button();
            row.getStyleClass().add("logs-sort-option");
            row.setFocusTraversable(false);
            row.setMaxWidth(Double.MAX_VALUE);

            HBox graphic = new HBox(10);
            graphic.setAlignment(Pos.CENTER_LEFT);

            Label icon = createPrimeIcon(SORT_FILTER_ICON_GLYPH, "logs-sort-option-icon");
            Label label = new Label(sortOption);
            label.getStyleClass().add("logs-sort-option-text");
            label.setMinWidth(0);
            label.setTextOverrun(OverrunStyle.ELLIPSIS);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            graphic.getChildren().addAll(icon, label, spacer);

            if (selectedSort.equals(sortOption)) {
                row.getStyleClass().add("logs-sort-option-selected");
                graphic.getChildren().add(createPrimeIcon(CHECK_ICON_GLYPH, "logs-sort-option-check"));
            }

            row.setGraphic(graphic);
            row.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            row.setOnAction(event -> {
                selectedSort = sortOption;
                setMenuButtonDisplay(sortMenuButton, SORT_FILTER_ICON_GLYPH, "Sort", selectedSort);
                sortMenuButton.hide();
                refreshFilteredTimeline();
            });

            content.getChildren().add(row);
        }

        return content;
    }

    private HBox createPopoverFooter(Runnable clearAction, Runnable applyAction) {
        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("logs-date-clear-button");
        clearButton.setFocusTraversable(false);
        clearButton.setOnAction(event -> clearAction.run());

        Button applyButton = new Button("Apply");
        applyButton.getStyleClass().add("logs-date-apply-button");
        applyButton.setFocusTraversable(false);
        applyButton.setOnAction(event -> applyAction.run());

        HBox footer = new HBox(9, clearButton, applyButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("logs-popover-footer");
        return footer;
    }

    private void configureListeners() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                refreshFilteredTimeline();
                if (Strings.clean(newValue).isBlank()) {
                    if (searchHistoryMenu != null) {
                        searchHistoryMenu.hide();
                    }
                } else if (searchField.isFocused()) {
                    showSearchHistoryMenu();
                }
            });
            searchField.setOnAction(event -> rememberSearch(searchField.getText()));
            searchField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                if (!isFocused) {
                    rememberSearch(searchField.getText());
                }
            });
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

        if (rangeStartTimeField != null) {
            rangeStartTimeField.textProperty().addListener((observable, oldValue, newValue) -> handleDateInputChange());
        }

        if (rangeEndTimeField != null) {
            rangeEndTimeField.textProperty().addListener((observable, oldValue, newValue) -> handleDateInputChange());
        }
    }

    private void renderTimeline() {
        List<ActivityLogEntry> filteredEntries = filteredActivityEntries();
        boolean hasEntries = !filteredEntries.isEmpty();

        if (logsShowingLabel != null) {
            String noun = filteredEntries.size() == 1 ? "event" : "events";
            logsShowingLabel.setText("Showing " + filteredEntries.size() + " " + noun);
        }

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
            timelineContainer.getChildren().setAll(createLogWorkspace(filteredEntries));
            Platform.runLater(this::scrollSelectedRowIntoView);
        }
    }

    private Node createLogWorkspace(List<ActivityLogEntry> entries) {
        VBox stream = createEventStream(entries);
        stream.setMinWidth(0);

        ScrollPane listScroll = new ScrollPane(stream);
        logListScroll = listScroll;
        listScroll.getStyleClass().add("logs-list-scroll");
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listScroll.setMinWidth(0);

        java.util.Optional<ActivityLogEntry> selected = selectedEntry(entries);
        if (selected.isEmpty()) {
            VBox.setVgrow(listScroll, Priority.ALWAYS);
            return listScroll;
        }

        VBox detailColumn = new VBox(0);
        detailColumn.getStyleClass().add("logs-detail-pane");
        detailColumn.setMinWidth(0);
        detailColumn.setFillWidth(true);
        detailColumn.getChildren().setAll(createPayloadCard(selected.get()));

        ScrollPane detailScroll = new ScrollPane(detailColumn);
        detailScroll.getStyleClass().add("logs-detail-scroll");
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.setMinWidth(0);

        SplitPane workspace = new SplitPane(listScroll, detailScroll);
        workspace.getStyleClass().add("logs-workspace");
        workspace.setMinHeight(400);
        workspace.setMaxWidth(Double.MAX_VALUE);
        workspace.setMaxHeight(Double.MAX_VALUE);
        workspace.setDividerPositions(0.58);
        VBox.setVgrow(workspace, Priority.ALWAYS);
        return workspace;
    }

    private void scrollSelectedRowIntoView() {
        if (logListScroll == null || selectedLogRow == null) {
            return;
        }
        Node content = logListScroll.getContent();
        if (content == null) {
            return;
        }
        double contentHeight = content.getBoundsInLocal().getHeight();
        double viewportHeight = logListScroll.getViewportBounds().getHeight();
        if (contentHeight <= viewportHeight) {
            return;
        }
        double rowTop = selectedLogRow.getBoundsInParent().getMinY();
        double rowHeight = selectedLogRow.getBoundsInParent().getHeight();
        double currentTop = logListScroll.getVvalue() * (contentHeight - viewportHeight);
        if (rowTop >= currentTop && rowTop + rowHeight <= currentTop + viewportHeight) {
            return;
        }
        double target = (rowTop - (viewportHeight - rowHeight) / 2) / (contentHeight - viewportHeight);
        logListScroll.setVvalue(Math.max(0, Math.min(1, target)));
    }

    // j/k keyboard navigation through the log list (Gmail-style)
    private void installLogKeyNav() {
        if (logsPageRoot == null) {
            return;
        }
        attachLogKeyNav(logsPageRoot.getScene());
        logsPageRoot.sceneProperty().addListener((obs, oldScene, newScene) -> attachLogKeyNav(newScene));
    }

    private void attachLogKeyNav(Scene scene) {
        if (scene == null || logKeyNavAttached) {
            return;
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleLogKeyNav);
        logKeyNavAttached = true;
    }

    private void handleLogKeyNav(KeyEvent event) {
        if (logsPageRoot == null || logsPageRoot.getScene() == null || !logsPageRoot.isVisible()) {
            return;
        }
        if (logsPageRoot.getScene().getFocusOwner() instanceof TextInputControl) {
            return;
        }
        KeyCode code = event.getCode();
        boolean down = code == KeyCode.J;
        boolean up = code == KeyCode.K;
        if (!down && !up) {
            return;
        }
        List<ActivityLogEntry> entries = filteredActivityEntries();
        if (entries.isEmpty()) {
            return;
        }
        int index = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(selectedEntryId)) {
                index = i;
                break;
            }
        }
        index = down ? Math.min(index + 1, entries.size() - 1) : Math.max(index - 1, 0);
        selectedEntryId = entries.get(index).id();
        detailClosed = false;
        renderTimeline();
        event.consume();
    }

    private void keepSelectedEntryVisible(List<ActivityLogEntry> entries) {
        if (selectedEntryId == null) {
            return;
        }

        boolean selectedEntryVisible = selectedEntryId != null
                && entries.stream().anyMatch(entry -> entry.id().equals(selectedEntryId));

        if (!selectedEntryVisible) {
            selectedEntryId = null;
            detailClosed = false;
        }
    }

    private java.util.Optional<ActivityLogEntry> selectedEntry(List<ActivityLogEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.id().equals(selectedEntryId))
                .findFirst();
    }

    private VBox createEventStream(List<ActivityLogEntry> entries) {
        selectedLogRow = null;
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
            selectedLogRow = row;
        }

        row.getChildren().add(createEventSummaryButton(entry, selected));
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
            if (selected) {
                selectedEntryId = null;
                selectedLogRow = null;
                detailClosed = true;
            } else {
                selectedEntryId = entry.id();
                detailClosed = false;
            }

            renderTimeline();
        });

        row.getStyleClass().add("logs-event-row");

        if (selected) {
            row.getStyleClass().add(isError(entry) ? "logs-event-row-selected-failed" : "logs-event-row-selected");
        }

        return row;
    }


    private VBox createPayloadCard(ActivityLogEntry entry) {
        VBox card = new VBox(0);
        card.getStyleClass().add("logs-payload-card");
        card.setFillWidth(true);
        card.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().add(createPayloadHeader(entry));

        if (isError(entry)) {
            card.getChildren().add(createPayloadErrorBanner(entry));
        }

        Node typedContent = createPayloadTypedContent(entry);
        if (typedContent != null) {
            if (!isError(entry)) {
                card.getChildren().add(createPayloadSectionDivider());
            }
            card.getChildren().add(typedContent);
        }

        card.getChildren().add(createPayloadSectionDivider());
        card.getChildren().add(createStandaloneTraceSection(entry));
        return card;
    }

    private HBox createSimplePayloadHeader(ActivityLogEntry entry) {
        StackPane iconShell = new StackPane(createPrimeIcon(eventIconPath(entry), "logs-payload-simple-header-icon"));
        iconShell.getStyleClass().add("logs-payload-simple-header-icon-shell");

        Label title = new Label(simplePayloadTitle(entry));
        title.getStyleClass().add("logs-payload-simple-title");
        title.setWrapText(true);
        title.setMinWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);

        Label subtitle = new Label(simplePayloadSubtitle(entry));
        subtitle.getStyleClass().add("logs-payload-simple-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMinWidth(0);
        subtitle.setMaxWidth(Double.MAX_VALUE);

        VBox copy = new VBox(4, title, subtitle);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox header = new HBox(14, iconShell, copy, createSimplePayloadStatusPill(entry), createPayloadCloseButton());
        header.getStyleClass().addAll("logs-payload-simple-header", simplePayloadToneClass(entry));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMinWidth(0);
        header.setMaxWidth(Double.MAX_VALUE);
        return header;
    }

    private String simplePayloadTitle(ActivityLogEntry entry) {
        String action = lowerFirst(formatAction(entry.action()));
        String actor = shortActor(entry.actor());
        String target = Strings.displayText(entry.target(), "").trim();

        if (isTiffActivity(entry) || Strings.normalize(displayArea(entry)).equals("import")) {
            String file = displayTiffItem(entry);
            target = file.isBlank() ? target : file;
        }

        if (target.isBlank() || Strings.normalize(target).equals("system")) {
            return "[" + eventPrefix(entry) + "] " + actor + " " + action;
        }

        return "[" + eventPrefix(entry) + "] " + actor + " " + action + " " + target;
    }

    private String simplePayloadSubtitle(ActivityLogEntry entry) {
        String date = formatHeaderDate(entry);
        if (isSystemActor(entry.actor())) {
            return "System event \u00B7 " + date;
        }
        return "Performed by " + displayActor(entry.actor()) + " \u00B7 " + date;
    }

    private Label createSimplePayloadStatusPill(ActivityLogEntry entry) {
        Label pill = new Label(simplePayloadStatusText(entry));
        pill.getStyleClass().addAll("logs-payload-status-pill", simplePayloadPillClass(entry));
        return pill;
    }

    private String simplePayloadStatusText(ActivityLogEntry entry) {
        if (isError(entry)) {
            return "Failed";
        }
        String status = displayStatus(entry.status());
        return "Info".equals(status) && isDeleteEvent(entry) ? "Deleted" : status;
    }

    private String simplePayloadToneClass(ActivityLogEntry entry) {
        String status = Strings.normalize(entry.status());
        String action = Strings.normalize(entry.action());

        if (isError(entry)) {
            return "logs-payload-simple-header-failed";
        }
        if (status.contains("warn") || status.contains("pending") || action.contains("warn")) {
            return "logs-payload-simple-header-warning";
        }
        if (status.contains("success") || status.contains("complete") || status.contains("approved")) {
            return "logs-payload-simple-header-success";
        }
        return "logs-payload-simple-header-info";
    }

    private String simplePayloadPillClass(ActivityLogEntry entry) {
        return switch (simplePayloadToneClass(entry)) {
            case "logs-payload-simple-header-failed" -> "logs-payload-status-pill-failed";
            case "logs-payload-simple-header-warning" -> "logs-payload-status-pill-warning";
            case "logs-payload-simple-header-success" -> "logs-payload-status-pill-success";
            default -> "logs-payload-status-pill-info";
        };
    }

    private VBox createSimplePayloadBody(List<ActivityDetailRow> rows) {
        VBox body = new VBox(0);
        body.getStyleClass().add("logs-payload-simple-body");
        body.setFillWidth(true);
        body.setMaxWidth(Double.MAX_VALUE);

        rows.forEach(row -> body.getChildren().add(createSimplePayloadRow(row.label(), row.value())));
        return body;
    }

    private HBox createSimplePayloadRow(String label, String value) {
        Label labelNode = new Label(Strings.displayText(label, "Detail"));
        labelNode.getStyleClass().add("logs-payload-simple-label");
        labelNode.setMinWidth(0);
        labelNode.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label valueNode = new Label(displayAuditValue(value));
        valueNode.getStyleClass().add("logs-payload-simple-value");
        valueNode.setWrapText(true);
        valueNode.setMinWidth(0);
        valueNode.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(valueNode, Priority.ALWAYS);

        HBox row = new HBox(18, labelNode, valueNode);
        row.getStyleClass().add("logs-payload-simple-row");
        row.setAlignment(Pos.TOP_LEFT);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private List<ActivityDetailRow> simplePayloadRows(ActivityLogEntry entry) {
        Map<String, ActivityDetailRow> rows = new LinkedHashMap<>();
        addSimplePayloadRow(rows, "Area", displayArea(entry));
        addSimplePayloadRow(rows, "Action", formatAction(entry.action()));

        String tiffItem = displayTiffItem(entry);
        if ((isTiffActivity(entry) || Strings.normalize(displayArea(entry)).equals("import")) && !tiffItem.isBlank()) {
            addSimplePayloadRow(rows, "File", tiffItem);
        } else {
            addSimplePayloadRow(rows, "Target", entry.target());
        }

        String description = Strings.displayText(entry.description(), "").trim();
        if (!description.isBlank()) {
            addSimplePayloadRow(rows, isError(entry) ? "Reason" : "Note", description);
        }

        for (ActivityChange change : visibleChanges(entry)) {
            if (isStateSnapshotField(change.field())) {
                continue;
            }
            addSimplePayloadRow(rows, change.field(), simpleChangeValue(entry, change));
        }

        for (ActivityDetailRow detail : normalizedContextRows(entry)) {
            addSimplePayloadRow(rows, detail.label(), detail.value());
        }

        if (rows.isEmpty()) {
            addSimplePayloadRow(rows, "Details", "Activity was recorded.");
        }

        return new ArrayList<>(rows.values());
    }

    private void addSimplePayloadRow(Map<String, ActivityDetailRow> rows, String label, String value) {
        String displayValue = displayAuditValue(value);
        if ("\u2014".equals(displayValue)) {
            return;
        }

        String displayLabel = Strings.displayText(label, "Detail");
        String key = Strings.normalize(displayLabel);
        rows.putIfAbsent(key, new ActivityDetailRow(displayLabel, displayValue));
    }

    private String simpleChangeValue(ActivityLogEntry entry, ActivityChange change) {
        if (isPasswordEvent(entry)) {
            return "Updated";
        }
        if (isCreateEvent(entry)) {
            return displayAuditValue(change.newValue());
        }
        if (isDeleteEvent(entry)) {
            return displayAuditValue(change.oldValue());
        }

        String before = displayAuditValue(change.oldValue());
        String after = displayAuditValue(change.newValue());

        if ("\u2014".equals(before)) {
            return after;
        }
        if ("\u2014".equals(after)) {
            return before;
        }
        return before + " to " + after;
    }

    private VBox createSimplePayloadFooter(ActivityLogEntry entry) {
        VBox footer = new VBox(8);
        footer.getStyleClass().add("logs-payload-simple-footer");
        footer.setFillWidth(true);
        footer.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("Trace");
        title.getStyleClass().add("logs-payload-simple-footer-title");

        String traceValue = "LOG-" + entry.id() + " \u00B7 " + entry.fullTimestamp();
        Label traceLabel = new Label(traceValue);
        traceLabel.getStyleClass().add("logs-payload-simple-trace");
        traceLabel.setMinWidth(0);
        traceLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(traceLabel, Priority.ALWAYS);

        Button copyButton = createCopyButton(traceValue);
        HBox traceRow = new HBox(12, traceLabel, copyButton);
        traceRow.setAlignment(Pos.CENTER_LEFT);
        traceRow.setMinWidth(0);
        traceRow.setMaxWidth(Double.MAX_VALUE);

        footer.getChildren().addAll(title, traceRow);
        return footer;
    }

    // === Header — hero icon + title + subtitle + date + status pill + close ===
    private VBox createPayloadHeader(ActivityLogEntry entry) {
        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.TOP_LEFT);
        topRow.setMinWidth(0);
        topRow.setMaxWidth(Double.MAX_VALUE);

        Node heroIcon = createHeroNode(entry);
        topRow.getChildren().add(heroIcon);

        VBox textColumn = new VBox(4);
        textColumn.setMinWidth(0);
        textColumn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textColumn, Priority.ALWAYS);

        // Row 1 — title + close button
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.TOP_LEFT);
        Node titleNode = createPayloadHeaderTitle(entry);
        if (titleNode instanceof Region region) {
            HBox.setHgrow(region, Priority.ALWAYS);
        }
        titleRow.getChildren().setAll(titleNode, createPayloadCloseButton());
        textColumn.getChildren().add(titleRow);

        boolean hasActor = !isSystemActor(entry.actor()) && !isError(entry);
        String actorPrefix = heroActorPrefix(entry);
        String actorName = displayActor(entry.actor());
        String dateText = formatHeaderDate(entry);

        if (hasActor) {
            textColumn.getChildren().add(createActorSubtitleLine(actorPrefix, actorName));
        }
        textColumn.getChildren().add(createDateStatusRow(dateText, createStatusPill(entry)));

        topRow.getChildren().add(textColumn);

        VBox header = new VBox(0, topRow);
        header.getStyleClass().add("logs-payload-card-header");
        header.setFillWidth(true);
        header.setMaxWidth(Double.MAX_VALUE);
        return header;
    }

    private Button createPayloadCloseButton() {
        Button closeButton = new Button("\u00D7");
        closeButton.getStyleClass().add("logs-payload-close-button");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(event -> {
            selectedEntryId = null;
            selectedLogRow = null;
            detailClosed = true;
            renderTimeline();
        });
        return closeButton;
    }

    private HBox createActorSubtitleLine(String prefix, String actorName) {
        Label prefixLabel = new Label(prefix + " ");
        prefixLabel.getStyleClass().add("logs-payload-header-subtitle");

        Label actorLabel = new Label(actorName);
        actorLabel.getStyleClass().addAll("logs-payload-header-subtitle", "logs-payload-header-actor-link");

        HBox row = new HBox(0, prefixLabel, actorLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox createDateStatusRow(String dateText, Label statusPill) {
        Label dateLabel = new Label(dateText);
        dateLabel.getStyleClass().add("logs-payload-header-date");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, dateLabel, spacer, statusPill);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox createCombinedSubtitleRow(String prefix, String actorName, String dateText, Label statusPill) {
        Label prefixLabel = new Label(prefix + " ");
        prefixLabel.getStyleClass().add("logs-payload-header-subtitle");

        Label actorLabel = new Label(actorName);
        actorLabel.getStyleClass().addAll("logs-payload-header-subtitle", "logs-payload-header-actor-link");

        Label sepLabel = new Label(" \u00B7 ");
        sepLabel.getStyleClass().add("logs-payload-header-subtitle");

        Label dateLabel = new Label(dateText);
        dateLabel.getStyleClass().add("logs-payload-header-subtitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(0, prefixLabel, actorLabel, sepLabel, dateLabel, spacer, statusPill);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node createPayloadHeaderTitle(ActivityLogEntry entry) {
        String entityName = headerTitleEntity(entry);
        String prefix = headerTitlePrefix(entry, entityName);

        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("logs-payload-header-title");

        Text prefixText = new Text(prefix);
        prefixText.getStyleClass().add("logs-payload-header-title-text");
        flow.getChildren().add(prefixText);

        if (!entityName.isBlank()) {
            Text entityText = new Text(entityName);
            entityText.getStyleClass().addAll("logs-payload-header-title-text", "logs-payload-header-title-link");
            flow.getChildren().add(entityText);
        }

        return flow;
    }

    private String headerTitleEntity(ActivityLogEntry entry) {
        if (isTiffActivity(entry) || Strings.normalize(displayArea(entry)).equals("import")) {
            return displayTiffItem(entry);
        }
        return "";
    }

    private String headerTitlePrefix(ActivityLogEntry entry, String entityName) {
        String area = "[" + eventPrefix(entry) + "]";
        String actor = shortActor(entry.actor());
        String action = lowerFirst(formatAction(entry.action()));

        if (!entityName.isBlank()) {
            // File-centric: "[TIFF] Alex uploaded "
            return area + " " + actor + " " + action + " ";
        }

        // Non-file events: drop the [area] prefix, use "Actor action target"
        String target = Strings.displayText(entry.target(), "").trim();
        if (target.isBlank() || Strings.normalize(target).equals("system")) {
            return actor + " " + action;
        }
        return actor + " " + action + " " + target;
    }

    private String formatHeaderDate(ActivityLogEntry entry) {
        LocalDateTime timestamp = parseActivityTimestamp(entry);
        if (timestamp == null) {
            return entry.fullTimestamp();
        }
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dateFmt.format(timestamp) + " \u00B7 " + timeFmt.format(timestamp);
    }

    private Label createStatusPill(ActivityLogEntry entry) {
        String text;
        String variantClass;
        if (isDeleteEvent(entry)) {
            text = "Deleted";
            variantClass = "logs-payload-status-pill-deleted";
        } else if (isError(entry)) {
            text = "Failed";
            variantClass = "logs-payload-status-pill-failed";
        } else {
            text = "Success";
            variantClass = "logs-payload-status-pill-success";
        }

        Label pill = new Label(text);
        pill.getStyleClass().addAll("logs-payload-status-pill", variantClass);
        return pill;
    }

    private Node createHeroNode(ActivityLogEntry entry) {
        if (isError(entry)) {
            // FAILURE — just a warning triangle, no background container
            Label icon = createPrimeIcon(WARNING_ICON_GLYPH, "logs-payload-hero-failure-icon");
            StackPane shell = new StackPane(icon);
            shell.getStyleClass().add("logs-payload-hero-failure");
            return shell;
        }

        if (isDeleteEvent(entry)) {
            // SNAPSHOT — gray square tile with trash icon
            Label icon = createPrimeIcon(TRASH_ICON_GLYPH, "logs-payload-hero-snapshot-icon");
            StackPane tile = new StackPane(icon);
            tile.getStyleClass().add("logs-payload-hero-snapshot-tile");
            return tile;
        }

        String area = Strings.normalize(displayArea(entry));
        boolean fileAsset = isTiffActivity(entry) || area.equals("import");

        if (fileAsset) {
            // ASSET (file) — square tile with file icon + TIFF badge at bottom-left
            Label fileIcon = createPrimeIcon(DOCUMENT_ICON_GLYPH, "logs-payload-hero-asset-icon");
            Label badge = new Label("TIFF");
            badge.getStyleClass().add("logs-payload-hero-asset-badge");

            StackPane tile = new StackPane(fileIcon, badge);
            StackPane.setAlignment(fileIcon, Pos.CENTER);
            StackPane.setAlignment(badge, Pos.BOTTOM_LEFT);
            tile.getStyleClass().add("logs-payload-hero-asset-tile");
            return tile;
        }

        // DIFF — colored circle with role-appropriate icon
        String iconPath = heroIconFor(entry);
        String bgClass = heroIconBgClassFor(entry);
        Label icon = createPrimeIcon(iconPath, "logs-payload-hero-diff-icon");
        StackPane circle = new StackPane(icon);
        circle.getStyleClass().addAll("logs-payload-hero-diff-circle", bgClass);
        return circle;
    }

    private VBox createStandaloneTraceSection(ActivityLogEntry entry) {
        VBox section = new VBox(0);
        section.getStyleClass().add("logs-payload-section");
        section.setFillWidth(true);

        Label title = new Label("Trace");
        title.getStyleClass().add("logs-payload-section-title");
        section.getChildren().add(title);

        String traceValue = "LOG-" + entry.id() + " \u00B7 " + entry.fullTimestamp();
        Label traceLabel = new Label(traceValue);
        traceLabel.getStyleClass().add("logs-payload-trace-line");
        HBox.setHgrow(traceLabel, Priority.ALWAYS);
        traceLabel.setMaxWidth(Double.MAX_VALUE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button copyButton = createCopyButton(traceValue);

        HBox row = new HBox(12, traceLabel, spacer, copyButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("logs-payload-trace-row");
        section.getChildren().add(row);

        return section;
    }

    private String payloadKindLabel(ActivityLogEntry entry) {
        if (isError(entry)) {
            return eventPrefix(entry) + " failure";
        }

        String area = eventPrefix(entry);
        String action = formatAction(entry.action());
        String normalizedAction = Strings.normalize(action);

        if (normalizedAction.contains("upload")) {
            return area + " Upload";
        }
        if (normalizedAction.contains("replace")) {
            return area + " Replace";
        }
        if (normalizedAction.contains("delete")) {
            return area + " Delete";
        }
        if (normalizedAction.contains("create")) {
            return area + " Create";
        }
        if (normalizedAction.contains("change") || normalizedAction.contains("update")) {
            return area + " update";
        }
        return area + " event";
    }

    private String payloadKindClass(ActivityLogEntry entry) {
        if (isError(entry)) {
            return "logs-payload-kind-error";
        }

        String area = Strings.normalize(displayArea(entry));
        return switch (area) {
            case "users", "profiles", "access", "security" -> "logs-payload-kind-purple";
            case "qa" -> "logs-payload-kind-teal";
            case "import" -> "logs-payload-kind-red";
            case "files", "documents", "exports" -> "logs-payload-kind-blue";
            default -> "logs-payload-kind-neutral";
        };
    }

    private Node createPayloadTypedContent(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        List<ActivityChange> changes = visibleChanges(entry);

        if (isError(entry)) {
            return buildFailurePattern(entry);
        }

        if (isDeleteEvent(entry)) {
            return buildSnapshotPattern(entry);
        }

        if (isQaEvent(entry)) {
            return buildQaPattern(entry);
        }

        if (isExportEvent(entry)) {
            return buildExportPattern(entry);
        }

        if (isBoxWorkflowEvent(entry)) {
            return buildBoxWorkflowPattern(entry);
        }

        if (isScanEvent(entry)) {
            return buildScanPattern(entry);
        }

        if (!changes.isEmpty()) {
            return buildDiffPattern(entry, changes);
        }

        if (isCreateEvent(entry)) {
            List<ActivityDetailRow> snap = visibleRows(compactVisibleRows(snapshotRowsForChangeEvent(entry), 12));
            return snap.isEmpty() ? buildFallbackPattern(entry) : createPayloadSection(changeSectionTitle(entry), snap, false);
        }

        return buildFallbackPattern(entry);
    }

    private VBox buildDiffPattern(ActivityLogEntry entry, List<ActivityChange> changes) {
        VBox content = new VBox(0);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(createPayloadChangesSection("Changes", changes));

        List<ActivityDetailRow> affected = affectedResourceRows(entry);
        if (!affected.isEmpty()) {
            content.getChildren().add(createPayloadSectionDivider());
            content.getChildren().add(createPayloadSection(affectedSectionTitle(entry), affected, false));
        }
        return content;
    }

    private VBox buildFailurePattern(ActivityLogEntry entry) {
        VBox content = new VBox(0);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        List<ActivityDetailRow> tiles = visibleRows(buildFailureTileRows(entry));
        if (!tiles.isEmpty()) {
            content.getChildren().add(createPayloadTileSection("Failure metrics", tiles));
        }

        List<ActivityDetailRow> details = visibleRows(buildFailureSummaryRows(entry));
        // Don't repeat tile data inside the details section.
        List<String> tileLabels = tiles.stream().map(r -> Strings.normalize(r.label())).toList();
        details = details.stream()
                .filter(r -> !tileLabels.contains(Strings.normalize(r.label())))
                .toList();

        if (!details.isEmpty()) {
            if (!content.getChildren().isEmpty()) {
                content.getChildren().add(createPayloadSectionDivider());
            }
            content.getChildren().add(createPayloadSection("Failure details", details, true, false));
        }
        return content;
    }

    private Node buildSnapshotPattern(ActivityLogEntry entry) {
        VBox content = new VBox(0);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        List<ActivityDetailRow> snap = visibleRows(compactVisibleRows(snapshotRowsForChangeEvent(entry), 12));
        if (!snap.isEmpty()) {
            content.getChildren().add(
                    createPayloadSection("Item snapshot (at time of deletion)", snap, false, false));
        }
        return content.getChildren().isEmpty() ? null : content;
    }

    private Node buildAssetPattern(ActivityLogEntry entry, String area) {
        if (isScanEvent(entry)) {
            return buildScanPattern(entry);
        }
        if (isQaEvent(entry)) {
            return buildQaPattern(entry);
        }
        if (isExportEvent(entry)) {
            return buildExportPattern(entry);
        }
        if (isBoxWorkflowEvent(entry)) {
            return buildBoxWorkflowPattern(entry);
        }
        if (isCreateEvent(entry)) {
            List<ActivityDetailRow> snap = visibleRows(compactVisibleRows(snapshotRowsForChangeEvent(entry), 12));
            return snap.isEmpty() ? null : createPayloadSection(changeSectionTitle(entry), snap, false);
        }
        return buildFallbackPattern(entry);
    }

    private Node buildAuthPattern(ActivityLogEntry entry) {
        VBox content = new VBox(0);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        List<ActivityDetailRow> securityRows = new ArrayList<>();
        securityRows.add(new ActivityDetailRow("Security event", formatAction(entry.action())));
        if (isPasswordEvent(entry)) {
            securityRows.add(new ActivityDetailRow("Credential", "Password"));
            securityRows.add(new ActivityDetailRow("Recorded value", "Hidden for security"));
        }
        addNonEmptyDetail(securityRows, "User", Strings.displayText(entry.target(), displayActor(entry.actor())));
        addFirstDetail(securityRows, entry, "IP address", "ip", "ip address");
        addFirstDetail(securityRows, entry, "Device", "device", "browser", "client");
        addFirstDetail(securityRows, entry, "Session", "session", "session id");
        addFirstDetail(securityRows, entry, "Duration", "duration", "session duration");

        content.getChildren().add(createPayloadSection("Security details", visibleRows(compactVisibleRows(securityRows, 8)), false));

        List<ActivityChange> nonPasswordChanges = visibleChanges(entry).stream()
                .filter(change -> !Strings.normalize(change.field()).contains("password"))
                .toList();
        if (!nonPasswordChanges.isEmpty()) {
            content.getChildren().add(createPayloadSectionDivider());
            content.getChildren().add(createPayloadChangesSection("Changes", nonPasswordChanges));
        }
        return content;
    }

    private Node buildScanPattern(ActivityLogEntry entry) {
        VBox content = new VBox(0);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        List<ActivityDetailRow> tiffRows = visibleRows(tiffMetricRows(entry));
        List<ActivityDetailRow> storageRows = visibleRows(storageTraceRows(entry));

        // If TIFF metrics are sparse (1-2 rows), merge them into Storage as one substantial
        // "File info" section instead of leaving them looking lonely.
        if (tiffRows.size() < 3 && !tiffRows.isEmpty() && !storageRows.isEmpty()) {
            List<ActivityDetailRow> merged = new ArrayList<>();
            merged.addAll(tiffRows);
            merged.addAll(storageRows);
            content.getChildren().add(createPayloadSection("File info", merged, false));
        } else {
            if (!tiffRows.isEmpty()) {
                if (tiffRows.size() >= 3) {
                    content.getChildren().add(createPayloadTileSection("TIFF details", tiffRows));
                } else {
                    content.getChildren().add(createPayloadSection("TIFF details", tiffRows, false));
                }
            }
            if (!storageRows.isEmpty()) {
                if (!content.getChildren().isEmpty()) content.getChildren().add(createPayloadSectionDivider());
                content.getChildren().add(createPayloadSection("Storage", storageRows, false));
            }
        }

        List<ActivityChange> changes = visibleChanges(entry);
        if (!changes.isEmpty() && !isCreateEvent(entry)) {
            if (!content.getChildren().isEmpty()) content.getChildren().add(createPayloadSectionDivider());
            content.getChildren().add(createPayloadChangesSection("Changes", changes));
        }

        return content.getChildren().isEmpty() ? buildFallbackPattern(entry) : content;
    }

    private Node buildBoxWorkflowPattern(ActivityLogEntry entry) {
        VBox content = new VBox(0);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        List<ActivityDetailRow> summary = visibleRows(buildBoxSummaryRows(entry));
        if (!summary.isEmpty()) {
            if (summary.size() >= 3) {
                content.getChildren().add(createPayloadTileSection("Box summary", summary));
            } else {
                content.getChildren().add(createPayloadSection("Box summary", summary, false));
            }
        }

        List<ActivityChange> changes = visibleChanges(entry);
        if (!changes.isEmpty()) {
            if (!content.getChildren().isEmpty()) content.getChildren().add(createPayloadSectionDivider());
            content.getChildren().add(createPayloadChangesSection("Changes", changes));
        }

        List<ActivityDetailRow> workflow = visibleRows(buildBoxWorkflowRows(entry));
        if (!workflow.isEmpty()) {
            if (!content.getChildren().isEmpty()) content.getChildren().add(createPayloadSectionDivider());
            content.getChildren().add(createPayloadSection("Workflow", workflow, false));
        }

        return content.getChildren().isEmpty() ? buildFallbackPattern(entry) : content;
    }

    private Node buildExportPattern(ActivityLogEntry entry) {
        VBox content = new VBox(0);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        List<ActivityDetailRow> summary = visibleRows(buildExportSummaryRows(entry));
        if (!summary.isEmpty()) {
            content.getChildren().add(createPayloadTileSection("Export contents", summary));
        }

        List<ActivityDetailRow> delivery = visibleRows(buildExportDeliveryRows(entry));
        if (!delivery.isEmpty()) {
            if (!content.getChildren().isEmpty()) content.getChildren().add(createPayloadSectionDivider());
            content.getChildren().add(createPayloadSection("Delivery", delivery, false));
        }

        List<ActivityChange> changes = visibleChanges(entry);
        if (!changes.isEmpty()) {
            if (!content.getChildren().isEmpty()) content.getChildren().add(createPayloadSectionDivider());
            content.getChildren().add(createPayloadChangesSection("Changes made", changes));
        }

        return content.getChildren().isEmpty() ? buildFallbackPattern(entry) : content;
    }

    private Node buildQaPattern(ActivityLogEntry entry) {
        VBox content = new VBox(0);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        List<ActivityDetailRow> decisionRows = visibleRows(buildQaDecisionRows(entry));
        if (!decisionRows.isEmpty()) {
            content.getChildren().add(createPayloadTileSection("QA decision", decisionRows));
        }

        List<ActivityDetailRow> boxSummary = visibleRows(buildBoxSummaryRows(entry));
        if (!boxSummary.isEmpty()) {
            if (!content.getChildren().isEmpty()) content.getChildren().add(createPayloadSectionDivider());
            content.getChildren().add(createPayloadTileSection("Box summary", boxSummary));
        }

        String note = qaNote(entry);
        if (!note.isBlank()) {
            if (!content.getChildren().isEmpty()) content.getChildren().add(createPayloadSectionDivider());
            content.getChildren().add(createPayloadNoteSection("Reviewer note", note));
        }

        List<ActivityChange> changes = visibleChanges(entry);
        if (!changes.isEmpty()) {
            if (!content.getChildren().isEmpty()) content.getChildren().add(createPayloadSectionDivider());
            content.getChildren().add(createPayloadChangesSection("Changes made", changes));
        }

        return content.getChildren().isEmpty() ? buildFallbackPattern(entry) : content;
    }

    private Node buildFallbackPattern(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = visibleRows(compactVisibleRows(eventDetailRows(entry), 12));
        if (rows.isEmpty()) {
            rows = visibleRows(simplePayloadRows(entry));
        }
        return rows.isEmpty() ? null : createPayloadSection("Event details", rows, false);
    }

    private boolean isAuthEvent(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        String action = Strings.normalize(entry.action());
        return isPasswordEvent(entry)
                || area.equals("security")
                || area.equals("access") && (action.contains("login") || action.contains("logout") || action.contains("session") || action.contains("2fa"))
                || action.contains("login")
                || action.contains("logout")
                || action.contains("session")
                || action.contains("password")
                || action.contains("two factor")
                || action.contains("2fa");
    }

    private boolean isQaEvent(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        String action = Strings.normalize(entry.action());
        return area.equals("qa")
                || area.equals("review")
                || action.contains("qa")
                || action.contains("review")
                || action.contains("approve")
                || action.contains("reject")
                || action.contains("rework");
    }

    private boolean isExportEvent(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        String action = Strings.normalize(entry.action());
        return area.equals("exports") || area.equals("export") || action.contains("export");
    }

    private boolean isBoxWorkflowEvent(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        String action = Strings.normalize(entry.action());
        String target = Strings.normalize(entry.target());
        return area.equals("boxes")
                || target.startsWith("box")
                || target.contains("box-")
                || action.contains("box")
                || action.contains("sent to qa")
                || action.contains("send to qa")
                || action.contains("saved")
                || action.contains("archive");
    }

    private boolean isScanEvent(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        String action = Strings.normalize(entry.action());
        return isTiffActivity(entry)
                || area.equals("import")
                || area.equals("files")
                || area.equals("documents")
                || area.equals("scans")
                || action.contains("scan")
                || action.contains("tiff")
                || action.contains("barcode")
                || action.contains("split")
                || action.contains("replace");
    }

    private List<ActivityDetailRow> buildBoxSummaryRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        String box = firstContextValue(entry, "box", "box id");
        if (box.isBlank() && Strings.normalize(entry.target()).startsWith("box")) {
            box = entry.target();
        }
        addNonEmptyDetail(rows, "Box", box);
        addFirstKnownDetail(rows, entry, "Documents", "documents", "document count", "docs", "records");
        addFirstKnownDetail(rows, entry, "Pages", "pages", "page count", "total pages");
        addFirstKnownDetail(rows, entry, "Files", "files", "file count", "tiffs", "tiff count");
        addFirstKnownDetail(rows, entry, "Profile", "profile", "scan profile");
        addFirstKnownDetail(rows, entry, "Status", "status", "box status");
        return compactVisibleRows(rows, 6);
    }

    private List<ActivityDetailRow> buildBoxWorkflowRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        rows.add(new ActivityDetailRow("Action", formatAction(entry.action())));
        String desc = Strings.displayText(entry.description(), "");
        if (!desc.isBlank()) rows.add(new ActivityDetailRow("Note", desc));
        rows.addAll(normalizedContextRows(entry));
        return compactVisibleRows(rows, 8);
    }

    private List<ActivityDetailRow> buildExportSummaryRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        addFirstKnownDetail(rows, entry, "Boxes", "boxes", "box count", "box");
        addFirstKnownDetail(rows, entry, "Documents", "documents", "document count", "records", "record count");
        addFirstKnownDetail(rows, entry, "Total pages", "total pages", "pages", "page count");
        addFirstKnownDetail(rows, entry, "Format", "format", "export format", "mode", "export mode");
        return compactVisibleRows(rows, 4);
    }

    private List<ActivityDetailRow> buildExportDeliveryRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        addFirstKnownDetail(rows, entry, "Destination", "destination", "path", "export path", "delivery path");
        addFirstKnownDetail(rows, entry, "File name", "file name", "filename", "archive", "zip");
        addFirstKnownDetail(rows, entry, "File size", "file size", "size");
        addFirstKnownDetail(rows, entry, "Naming pattern", "naming pattern", "export naming");
        String desc = Strings.displayText(entry.description(), "");
        if (!desc.isBlank()) rows.add(new ActivityDetailRow("Note", desc));
        return compactVisibleRows(rows, 8);
    }

    private List<ActivityDetailRow> buildQaDecisionRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        rows.add(new ActivityDetailRow("Decision", qaDecision(entry)));
        addFirstKnownDetail(rows, entry, "Reviewer", "reviewer", "reviewed by", "performed by");
        addFirstKnownDetail(rows, entry, "Checklist", "checklist", "checklist count", "checks", "checks passed");
        addFirstKnownDetail(rows, entry, "Issues", "issues", "issue count", "issues found");
        addFirstKnownDetail(rows, entry, "Document", "document", "document id");
        return compactVisibleRows(rows, 5);
    }

    private String qaDecision(ActivityLogEntry entry) {
        String action = Strings.normalize(entry.action());
        if (action.contains("reject")) return "Rejected";
        if (action.contains("approve")) return "Approved";
        if (action.contains("rework")) return "Needs rework";
        if (action.contains("review")) return "In review";
        return displayStatus(entry.status());
    }

    private String qaNote(ActivityLogEntry entry) {
        String note = firstContextValue(entry, "note", "comment", "reviewer note", "qa note");
        return note.isBlank() ? Strings.displayText(entry.description(), "") : note;
    }

    private void addFirstKnownDetail(List<ActivityDetailRow> rows, ActivityLogEntry entry, String displayLabel, String... labels) {
        String value = firstContextValue(entry, labels);
        if (value.isBlank()) value = firstChangeValue(entry, true, labels);
        if (value.isBlank()) value = firstChangeValue(entry, false, labels);
        addNonEmptyDetail(rows, displayLabel, value);
    }

    private void addNonEmptyDetail(List<ActivityDetailRow> rows, String label, String value) {
        if (!isMissingAuditValue(value)) {
            rows.add(new ActivityDetailRow(label, value));
        }
    }

    private List<ActivityDetailRow> buildFailureTileRows(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        List<ActivityDetailRow> rows = new ArrayList<>();

        switch (area) {
            case "import" -> {
                addFirstDetail(rows, entry, "File size", "file size", "size");
                rows.add(new ActivityDetailRow("File type", "TIFF"));
                addFirstDetail(rows, entry, "Detected compression",
                        "detected compression", "detected", "compression");
                addFirstDetail(rows, entry, "Expected", "expected", "accepted");
            }
            case "exports" -> {
                addFirstDetail(rows, entry, "Export mode", "export mode", "mode");
                addFirstDetail(rows, entry, "Format", "format", "export format");
                addFirstDetail(rows, entry, "Records attempted",
                        "records attempted", "records", "total records");
                addFirstDetail(rows, entry, "Records failed", "records failed", "failed records");
            }
            case "qa" -> {
                addFirstDetail(rows, entry, "Document", "document", "document id");
                addFirstDetail(rows, entry, "Issues found", "issues", "issue count", "issues found");
                addFirstDetail(rows, entry, "Checklist", "checklist", "checklist count");
                addFirstDetail(rows, entry, "Reviewer", "reviewer", "performed by");
            }
            default -> {
                // Fallback — generic failure tiles from whatever metrics exist
                addFirstDetail(rows, entry, "File size", "file size", "size");
                addFirstDetail(rows, entry, "Pages", "pages", "page count");
                addFirstDetail(rows, entry, "Records failed", "records failed", "failed records");
                addFirstDetail(rows, entry, "Reason", "reason", "failure reason");
            }
        }

        return rows.stream().limit(4).toList();
    }

    private List<ActivityDetailRow> visibleRows(List<ActivityDetailRow> rows) {
        return rows.stream().filter(row -> !isMissingAuditValue(row.value())).toList();
    }

    private List<ActivityDetailRow> affectedResourceRows(ActivityLogEntry entry) {
        List<ActivityDetailRow> rows = new ArrayList<>();
        String id = targetId(entry);
        if (hasRealTargetId(id)) {
            rows.add(new ActivityDetailRow(affectedResourceIdLabel(entry), id));
        }
        String target = Strings.displayText(entry.target(), "");
        if (!target.isBlank() && !Strings.normalize(target).equals("system")
                && rows.stream().noneMatch(row -> Strings.normalize(row.value()).equals(Strings.normalize(target)))) {
            rows.add(new ActivityDetailRow(affectedResourceLabel(entry), target));
        }
        String email = firstContextValue(entry, "email", "email address");
        if (!email.isBlank()) {
            rows.add(new ActivityDetailRow("Email", email));
        }
        return rows;
    }

    private String affectedSectionTitle(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        return switch (area) {
            case "users", "access", "security" -> "Affected user";
            case "profiles" -> "Affected profile";
            default -> "Affected resource";
        };
    }

    private String affectedResourceIdLabel(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        return switch (area) {
            case "users", "access", "security" -> "User ID";
            case "profiles" -> "Profile ID";
            case "documents", "files", "import" -> "File ID";
            default -> "Resource ID";
        };
    }

    private String affectedResourceLabel(ActivityLogEntry entry) {
        return switch (Strings.normalize(displayArea(entry))) {
            case "users", "access", "security" -> "User";
            case "profiles" -> "Profile";
            case "documents", "files", "import" -> "Document";
            case "system" -> "Setting";
            default -> "Resource";
        };
    }

    private boolean isNumericValue(String value) {
        String clean = Strings.clean(value);
        return !clean.isBlank() && clean.matches("\\d[\\d,]*");
    }

    private VBox createPayloadNoteSection(String title, String text) {
        VBox section = new VBox(0);
        section.getStyleClass().add("logs-payload-section");
        if (isStorageSectionTitle(title)) {
            section.getStyleClass().add("logs-payload-section-storage");
        }
        section.setFillWidth(true);
        section.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("logs-payload-section-title");

        Label body = new Label(text);
        body.getStyleClass().add("logs-payload-note-text");
        body.setWrapText(true);
        body.setMinWidth(0);
        body.setMaxWidth(Double.MAX_VALUE);

        section.getChildren().addAll(titleLabel, body);
        return section;
    }

    private HBox createPayloadTraceFooter(ActivityLogEntry entry) {
        Label icon = createPrimeIcon(CLOCK_ICON_GLYPH, "logs-payload-trace-icon");

        Label labelNode = new Label("Trace");
        labelNode.getStyleClass().add("logs-payload-trace-label");

        Label idNode = new Label("LOG-" + entry.id());
        idNode.getStyleClass().add("logs-payload-trace-value");

        Label timestampNode = new Label(entry.fullTimestamp());
        timestampNode.getStyleClass().add("logs-payload-trace-value");

        HBox footer = new HBox(16, icon, labelNode, idNode, timestampNode);
        footer.getStyleClass().add("logs-payload-trace-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setMaxWidth(Double.MAX_VALUE);
        return footer;
    }

    private HBox createPayloadErrorBanner(ActivityLogEntry entry) {
        StackPane iconShell = new StackPane(createPrimeIcon(WARNING_ICON_GLYPH, "logs-payload-error-icon"));
        iconShell.getStyleClass().add("logs-payload-error-icon-shell");

        Label titleLabel = new Label("Action needed");
        titleLabel.getStyleClass().add("logs-payload-error-title");
        titleLabel.setWrapText(true);

        VBox copy = new VBox(4, titleLabel);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        String desc = Strings.displayText(entry.description(), "");
        if (desc.isBlank()) {
            desc = failureHeadline(entry);
        }
        if (!desc.isBlank()) {
            Label descLabel = new Label(desc);
            descLabel.getStyleClass().add("logs-payload-error-copy");
            descLabel.setWrapText(true);
            copy.getChildren().add(descLabel);
        }

        String action = actionNeeded(entry);
        if (!action.isBlank()) {
            Label actionLabel = new Label(action);
            actionLabel.getStyleClass().add("logs-payload-error-copy");
            actionLabel.setWrapText(true);
            copy.getChildren().add(actionLabel);
        }

        HBox banner = new HBox(12, iconShell, copy);
        banner.getStyleClass().add("logs-payload-error-banner");
        banner.setAlignment(Pos.TOP_LEFT);
        return banner;
    }

    private String failureHeadline(ActivityLogEntry entry) {
        String reason = firstContextValue(entry, "reason", "failure reason", "error", "problem");
        if (!reason.isBlank()) {
            return reason;
        }

        String description = Strings.displayText(entry.description(), "");
        if (!description.isBlank()) {
            return description;
        }

        return formatAction(entry.action());
    }

    private HBox createPayloadHeroSection(ActivityLogEntry entry) {
        StackPane iconShell = createPayloadHeroIcon(entry);

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
            Label statusLabel = new Label(displayStatus(entry.status()));
            statusLabel.getStyleClass().add("logs-payload-hero-status-label");
            Label descLabel = new Label(desc);
            descLabel.getStyleClass().add("logs-payload-hero-status-text");
            descLabel.setWrapText(false);
            descLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            HBox statusLine = new HBox(6, checkIcon, statusLabel, descLabel);
            statusLine.setAlignment(Pos.CENTER_LEFT);
            copy.getChildren().add(statusLine);
        }

        HBox hero = new HBox(15, iconShell, copy);
        hero.getStyleClass().add("logs-payload-hero");
        hero.setAlignment(Pos.CENTER_LEFT);
        return hero;
    }

    private StackPane createPayloadHeroIcon(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        if (isTiffActivity(entry) || area.equals("import") || area.equals("exports")) {
            StackPane iconShell = new StackPane();
            iconShell.getStyleClass().add("logs-payload-file-icon-shell");

            Label fileIcon = createPrimeIcon(DOCUMENT_ICON_GLYPH, "logs-payload-file-icon");
            Label fileType = new Label(isTiffActivity(entry) || area.equals("import") ? "TIFF" : "EXP");
            fileType.getStyleClass().add("logs-payload-file-type-badge");
            StackPane.setAlignment(fileType, Pos.BOTTOM_CENTER);

            iconShell.getChildren().setAll(fileIcon, fileType);
            return iconShell;
        }

        String iconPath = heroIconFor(entry);
        String iconBgClass = heroIconBgClassFor(entry);
        Label iconLabel = createPrimeIcon(iconPath, "logs-payload-hero-icon");
        StackPane iconShell = new StackPane(iconLabel);
        iconShell.getStyleClass().addAll("logs-payload-hero-icon-shell", iconBgClass);
        return iconShell;
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
        String action = Strings.normalize(formatAction(entry.action()));
        if (action.contains("deactivat")) return "Deactivated by";
        if (action.contains("reactivat") || action.contains("restore")) return "Reactivated by";
        if (action.contains("archive")) return "Archived by";
        if (action.contains("upload")) return "Uploaded by";
        if (action.contains("replace")) return "Replaced by";
        if (action.contains("approve") || action.contains("review")) return "Reviewed by";
        if (action.contains("reject")) return "Rejected by";
        if (action.contains("export")) return "Exported by";
        if (action.contains("change") || action.contains("update")) return "Updated by";
        String area = Strings.normalize(displayArea(entry));
        if (area.equals("import") || isTiffActivity(entry)) return "Imported by";
        return "Performed by";
    }

    private String heroIconFor(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        if (isDeleteEvent(entry)) {
            return TRASH_ICON_GLYPH;
        }
        return switch (area) {
            case "users", "access", "security" -> USER_ICON_GLYPH;
            case "profiles" -> GEAR_ICON_GLYPH;
            case "qa" -> CHECK_ICON_GLYPH;
            case "files", "import", "documents" -> DOCUMENT_ICON_GLYPH;
            case "exports" -> DOWNLOAD_ICON_GLYPH;
            default -> CLOCK_ICON_GLYPH;
        };
    }

    private String heroIconBgClassFor(ActivityLogEntry entry) {
        String area = Strings.normalize(displayArea(entry));
        if (isDeleteEvent(entry)) {
            return "logs-payload-hero-icon-neutral";
        }
        return switch (area) {
            case "users", "access", "security" -> "logs-payload-hero-icon-user";
            case "profiles" -> "logs-payload-hero-icon-purple";
            case "qa" -> "logs-payload-hero-icon-teal";
            case "files", "import", "documents" -> "logs-payload-hero-icon-blue";
            default -> "logs-payload-hero-icon-neutral";
        };
    }

    private VBox createPayloadSection(String title, List<ActivityDetailRow> rows, boolean failureSection) {
        return createPayloadSection(title, rows, failureSection, false);
    }

    private VBox createPayloadSection(String title, List<ActivityDetailRow> rows,
                                      boolean failureSection, boolean showIcons) {
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

        VBox rowHost = new VBox(0);
        if (shouldWrapPayloadRows(title)) {
            rowHost.getStyleClass().add("logs-payload-kv-card");
        }
        rowHost.setFillWidth(true);
        rowHost.setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < visible.size(); i++) {
            if (i > 0) rowHost.getChildren().add(createPayloadRowDivider());
            ActivityDetailRow row = visible.get(i);
            String valueStyle = failureSection && isPayloadDangerLabel(row.label())
                    ? "logs-payload-kv-value-danger"
                    : isPayloadSuccessLabel(row.label()) ? "logs-payload-kv-value-success"
                      : isPayloadLinkLabel(row.label()) ? "logs-payload-kv-value-link"
                        : null;
            String iconPath = showIcons ? iconForPayloadLabel(row.label()) : null;
            rowHost.getChildren().add(
                    createPayloadKvRow(iconPath, row.label(),
                            displayAuditValue(row.value()), valueStyle));
        }

        if (!rowHost.getChildren().isEmpty()) {
            section.getChildren().add(rowHost);
        }

        return section;
    }

    private boolean shouldWrapPayloadRows(String title) {
        // Wrap every KV section in a bordered card for consistent visual hierarchy.
        return true;
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
            for (ActivityDetailRow row : visible) {
                VBox tile = createMetricTile(iconForPayloadLabel(row.label()), row.label(),
                        displayAuditValue(row.value()));
                if (isPayloadDangerLabel(row.label())) {
                    tile.getStyleClass().add("logs-payload-tile-danger");
                }
                grid.getChildren().add(tile);
            }
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
        valueNode.setMinWidth(0);
        valueNode.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(valueNode, Priority.ALWAYS);

        HBox valueLine = new HBox(6, valueNode);
        valueLine.getStyleClass().add("logs-payload-tile-value-line");
        valueLine.setAlignment(Pos.CENTER_LEFT);
        valueLine.setMinWidth(0);
        valueLine.setMaxWidth(Double.MAX_VALUE);
        if (isCopyableLabel(label)) {
            valueLine.getChildren().add(createCopyButton(value));
        }

        VBox tile = new VBox(4, iconShell, labelNode, valueLine);
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

        if (changes.isEmpty()) {
            return section;
        }

        GridPane grid = new GridPane();
        grid.getStyleClass().add("logs-payload-changes-grid");
        grid.setMaxWidth(Double.MAX_VALUE);

        grid.getColumnConstraints().addAll(
                createPayloadChangeColumn(22, HPos.LEFT),
                createPayloadChangeColumn(34, HPos.CENTER),
                createPayloadChangeColumn(8, HPos.CENTER),
                createPayloadChangeColumn(36, HPos.CENTER)
        );

        int rowIndex = 0;

        grid.add(createPayloadChangeHeaderCell("Field", Pos.CENTER_LEFT), 0, rowIndex);
        grid.add(createPayloadChangeHeaderCell("Before", Pos.CENTER, "logs-payload-change-header-before"), 1, rowIndex);
        grid.add(createPayloadChangeHeaderCell("", Pos.CENTER), 2, rowIndex);
        grid.add(createPayloadChangeHeaderCell("After", Pos.CENTER, "logs-payload-change-header-after"), 3, rowIndex);

        rowIndex++;

        List<ActivityChange> visible = changes.stream().toList();
        for (int i = 0; i < visible.size(); i++) {
            ActivityChange change = visible.get(i);
            boolean lastRow = i == visible.size() - 1;

            Label fieldLabel = new Label(change.field());
            fieldLabel.getStyleClass().add("logs-payload-change-field-label");
            fieldLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            grid.add(createPayloadChangeGridCell(fieldLabel, Pos.CENTER_LEFT, lastRow), 0, rowIndex);

            Label beforePill = createPayloadChangeValue(displayAuditValue(change.oldValue()), "logs-payload-change-before");
            grid.add(createPayloadChangeGridCell(beforePill, Pos.CENTER, lastRow), 1, rowIndex);

            Label arrowLabel = new Label("→");
            arrowLabel.getStyleClass().add("logs-payload-change-center-arrow");
            grid.add(createPayloadChangeGridCell(arrowLabel, Pos.CENTER, lastRow), 2, rowIndex);

            Label afterPill = createPayloadChangeValue(displayAuditValue(change.newValue()), "logs-payload-change-after");
            grid.add(createPayloadChangeGridCell(afterPill, Pos.CENTER, lastRow), 3, rowIndex);

            rowIndex++;
        }

        VBox card = new VBox(grid);
        card.getStyleClass().add("logs-payload-changes-card");
        card.setFillWidth(true);
        section.getChildren().add(card);

        return section;
    }

    private boolean isStorageSectionTitle(String title) {
        String norm = Strings.normalize(title);
        return norm.contains("storage") || norm.contains("trace") || norm.contains("affected");
    }

    private ColumnConstraints createPayloadChangeColumn(double percentWidth, HPos alignment) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percentWidth);
        column.setHalignment(alignment);
        column.setFillWidth(true);
        column.setHgrow(Priority.ALWAYS);
        return column;
    }

    private HBox createPayloadChangeHeaderCell(String text, Pos alignment, String... extraClasses) {
        Label label = new Label(text);
        label.getStyleClass().add("logs-payload-change-header-label");
        label.getStyleClass().addAll(extraClasses);
        label.setAlignment(alignment);
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);

        HBox cell = createPayloadChangeGridCell(label, alignment, false);
        cell.getStyleClass().add("logs-payload-changes-header-cell");
        return cell;
    }

    private HBox createPayloadChangeGridCell(Node content, Pos alignment, boolean lastRow) {
        HBox cell = new HBox(content);
        cell.getStyleClass().add("logs-payload-changes-cell");
        if (lastRow) {
            cell.getStyleClass().add("logs-payload-changes-last-cell");
        }
        cell.setAlignment(alignment);
        cell.setMinWidth(0);
        cell.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(cell, true);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        if (content instanceof Region region) {
            region.setMinWidth(0);
            if (shouldGrowPayloadChangeContent(content)) {
                region.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(region, Priority.ALWAYS);
            }
        }
        return cell;
    }

    private boolean shouldGrowPayloadChangeContent(Node content) {
        List<String> styleClasses = content.getStyleClass();
        return !styleClasses.contains("logs-payload-change-center-arrow");
    }

    private Label createPayloadChangeValue(String value, String styleClass) {
        Label label = new Label(value);
        label.getStyleClass().add(styleClass);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setTooltip(new Tooltip(value));
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private HBox createPayloadKvRow(String iconPath, String label, String value, String valueStyleClass) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("logs-payload-kv-label");
        labelNode.setMinWidth(Region.USE_PREF_SIZE);
        labelNode.setWrapText(true);

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("logs-payload-kv-value");
        if (valueStyleClass != null && !valueStyleClass.isBlank()) {
            valueNode.getStyleClass().add(valueStyleClass);
        }
        valueNode.setTextOverrun(OverrunStyle.ELLIPSIS);
        valueNode.setTooltip(new Tooltip(value));
        valueNode.setAlignment(Pos.CENTER_LEFT);
        valueNode.setMinWidth(0);
        valueNode.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(valueNode, Priority.ALWAYS);

        HBox row = new HBox(12);
        row.getStyleClass().add("logs-payload-kv-row");
        row.setAlignment(Pos.CENTER_LEFT);

        if (iconPath != null && !iconPath.isBlank()) {
            row.getChildren().add(createPayloadIconShell(iconPath));
        }
        row.getChildren().addAll(labelNode, valueNode);

        if (isCopyableLabel(label)) {
            row.getChildren().add(createCopyButton(value));
        }

        return row;
    }

    private Button createCopyButton(String value) {
        Label icon = createPrimeIcon(COPY_ICON_GLYPH, "logs-payload-copy-icon");
        Button button = new Button();
        button.setGraphic(icon);
        button.getStyleClass().add("logs-payload-copy-button");
        button.setFocusTraversable(false);
        button.setTooltip(new Tooltip("Copy"));
        button.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(value);
            Clipboard.getSystemClipboard().setContent(content);
        });
        return button;
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
        if (norm.contains("issue") || norm.contains("reason") || norm.contains("failed")
                || norm.contains("warning") || norm.contains("error")) return WARNING_ICON_GLYPH;
        if (norm.equals("result") || norm.contains("status") || norm.contains("accepted")
                || norm.contains("action needed") || norm.contains("checklist")
                || norm.equals("access")) return CHECK_ICON_GLYPH;
        if (norm.contains("trace") || norm.equals("log id") || norm.contains("recorded")
                || norm.contains("date") || norm.contains("time") || norm.contains("modified")
                || norm.contains("created at") || norm.contains("checksum")) return CLOCK_ICON_GLYPH;
        if (norm.contains("box")) return BOX_ICON_GLYPH;
        if (norm.contains("user") || norm.contains("actor") || norm.contains("importer")
                || norm.contains("performed by") || norm.equals("username")
                || norm.contains("email")) return USER_ICON_GLYPH;
        if (norm.equals("pages") || norm.equals("page count") || norm.equals("page")
                || norm.equals("pages found") || norm.contains("rows") || norm.contains("records")
                || norm.contains("dimensions") || norm.equals("case") || norm.equals("case id")) return PAGES_ICON_GLYPH;
        if (norm.contains("role") || norm.contains("profile") || norm.contains("compression")
                || norm.contains("resolution") || norm.contains("mode") || norm.contains("format")
                || norm.contains("setting") || norm.contains("policy")) return GEAR_ICON_GLYPH;
        if (norm.contains("export")) return DOWNLOAD_ICON_GLYPH;
        if (norm.contains("import") || norm.contains("upload")) return UPLOAD_ICON_GLYPH;
        if (norm.contains("replace") || norm.contains("retry")) return REFRESH_ICON_GLYPH;
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
        return norm.equals("box") || norm.equals("case") || norm.equals("user") || norm.equals("document")
                || norm.equals("user id") || norm.equals("file id") || norm.equals("profile id")
                || norm.equals("resource id") || norm.equals("document id") || norm.equals("case id")
                || norm.equals("box id");
    }

    private boolean isCopyableLabel(String label) {
        String norm = Strings.normalize(label);
        return norm.equals("trace") || norm.equals("checksum") || norm.equals("path") || norm.equals("file id");
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
                // TIFF / scan / documents / system
                addFirstDetail(rows, entry, "Box", "box", "box id");
                addFirstDetail(rows, entry, "Profile", "profile");
                addFirstDetail(rows, entry, "File ID", "file id", "file");
                addFirstDetail(rows, entry, "Path", "path", "storage path");
            }
        }

        return rows;
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
                .filter(change -> !isMissingAuditValue(change.newValue()))
                .map(change -> new ActivityDetailRow(change.field(), change.newValue()))
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
        return matcher.find() ? matcher.group() : "\u2014";
    }

    private boolean hasRealTargetId(String targetId) {
        if (isMissingAuditValue(targetId) || "\u2014".equals(targetId.trim())) {
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

        if (isTiffActivity(entry)) {
            return UPLOAD_ICON_GLYPH;
        }

        if (area.equals("import")) {
            return DOWNLOAD_ICON_GLYPH;
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

        // Trace ID match — accept "183", "LOG-183", or "log-183"
        String traceId = Strings.normalize("LOG-" + entry.id());
        String rawId = Strings.normalize(entry.id());
        if (traceId.contains(searchText) || rawId.equals(searchText)) {
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
        return selectedArea.isBlank()
                || ALL_AREAS.equals(selectedArea)
                || displayArea(entry).equalsIgnoreCase(selectedArea)
                || Strings.displayText(entry.type(), "").equalsIgnoreCase(selectedArea);
    }

    private boolean matchesUser(ActivityLogEntry entry) {
        return selectedUser.isBlank()
                || ALL_USERS.equals(selectedUser)
                || Strings.displayText(entry.actor(), "").equalsIgnoreCase(selectedUser);
    }

    private boolean matchesStatus(ActivityLogEntry entry) {
        if (selectedStatuses.isEmpty() || selectedStatuses.size() == STATUS_OPTIONS.size()) {
            return true;
        }

        if (selectedStatuses.contains("Failed") && isError(entry)) {
            return true;
        }

        return selectedStatuses.contains(displayStatus(entry.status()));
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

        if (dateFilterMode == DateFilterMode.SPECIFIC) {
            return specificDate == null || activityTime.toLocalDate().equals(specificDate);
        }

        LocalDateTime startBoundary = startDateTimeBoundary();
        LocalDateTime endBoundary = endDateTimeBoundary();

        boolean afterStart = startBoundary == null || !activityTime.isBefore(startBoundary);
        boolean beforeEnd = endBoundary == null || !activityTime.isAfter(endBoundary);

        return afterStart && beforeEnd;
    }

    private LocalDateTime startDateTimeBoundary() {
        if (rangeStartDate == null) {
            return null;
        }

        return rangeStartDate.atTime(rangeStartTime == null ? LocalTime.MIN : rangeStartTime);
    }

    private LocalDateTime endDateTimeBoundary() {
        if (rangeEndDate == null) {
            return null;
        }

        return rangeEndDate.atTime(rangeEndTime == null ? LocalTime.MAX : rangeEndTime);
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

        selectedArea = ALL_AREAS;
        pendingArea = ALL_AREAS;
        selectedUser = ALL_USERS;
        pendingUser = ALL_USERS;
        selectedSort = SORT_NEWEST_FIRST;
        selectedStatuses.clear();
        selectedStatuses.addAll(STATUS_OPTIONS);
        pendingStatuses.clear();
        pendingStatuses.addAll(STATUS_OPTIONS);
        updateToolbarMenuDisplays();

        updatingDateControls = true;

        dateFilterMode = DateFilterMode.ALL;
        specificDate = null;
        rangeStartDate = null;
        rangeEndDate = null;
        rangeStartTime = null;
        rangeEndTime = null;
        awaitingRangeEnd = false;

        if (specificDatePicker != null) {
            specificDatePicker.setValue(null);
        }

        if (rangeStartDatePicker != null) {
            rangeStartDatePicker.setValue(null);
        }

        if (rangeEndDatePicker != null) {
            rangeEndDatePicker.setValue(null);
        }

        if (rangeStartTimeField != null) {
            rangeStartTimeField.clear();
        }

        if (rangeEndTimeField != null) {
            rangeEndTimeField.clear();
        }

        updatingDateControls = false;

        updateDateFilterState();
        updateCalendarDisplay();

        selectedEntryId = null;
        renderTimeline();
    }

    private void updateToolbarMenuDisplays() {
        setMenuButtonDisplay(statusMenuButton, RESULT_FILTER_ICON_GLYPH, "Status", statusButtonText());
        setMenuButtonDisplay(filtersMenuButton, FILTER_ICON_GLYPH, "Filters", filtersButtonText());
        setMenuButtonDisplay(sortMenuButton, SORT_FILTER_ICON_GLYPH, "Sort", selectedSort);
    }

    @FXML
    private void exportLogs() {
        List<ActivityLogEntry> entries = filteredActivityEntries();

        if (entries.isEmpty()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export activity logs");
        fileChooser.setInitialFileName("activity-logs.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));

        Window owner = logsPageRoot == null || logsPageRoot.getScene() == null
                ? null
                : logsPageRoot.getScene().getWindow();
        File targetFile = fileChooser.showSaveDialog(owner);

        if (targetFile == null) {
            return;
        }

        try {
            Files.writeString(targetFile.toPath(), buildActivityLogCsv(entries), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            if (logsShowingLabel != null) {
                logsShowingLabel.setText("Could not export logs.");
            }
        }
    }

    private String buildActivityLogCsv(List<ActivityLogEntry> entries) {
        StringBuilder csv = new StringBuilder("Timestamp,Area,Actor,Action,Target,Status,Description\n");

        for (ActivityLogEntry entry : entries) {
            appendCsvRow(csv,
                    entry.fullTimestamp(),
                    displayArea(entry),
                    displayActor(entry.actor()),
                    formatAction(entry.action()),
                    entry.target(),
                    displayStatus(entry.status()),
                    entry.description());
        }

        return csv.toString();
    }

    private void appendCsvRow(StringBuilder csv, String... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }

            csv.append(csvValue(values[index]));
        }

        csv.append('\n');
    }

    private String csvValue(String value) {
        String cleanValue = Strings.displayText(value, "");
        return "\"" + cleanValue.replace("\"", "\"\"") + "\"";
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
        normalizeDateRange();
        awaitingRangeEnd = false;
        dateFilterMode = rangeStartDate == null && rangeEndDate == null
                ? DateFilterMode.ALL
                : DateFilterMode.RANGE;
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
        rangeStartTime = null;
        rangeEndTime = null;
        awaitingRangeEnd = false;

        if (specificDatePicker != null) {
            specificDatePicker.setValue(null);
        }

        if (rangeStartDatePicker != null) {
            rangeStartDatePicker.setValue(null);
        }

        if (rangeEndDatePicker != null) {
            rangeEndDatePicker.setValue(null);
        }

        if (rangeStartTimeField != null) {
            rangeStartTimeField.clear();
        }

        if (rangeEndTimeField != null) {
            rangeEndTimeField.clear();
        }

        updatingDateControls = false;

        updateDateFilterState();
        updateCalendarDisplay();
        refreshFilteredTimeline();
    }

    private void normalizeDateRange() {
        if (rangeStartDate == null || rangeEndDate == null || !rangeEndDate.isBefore(rangeStartDate)) {
            return;
        }

        LocalDate previousStartDate = rangeStartDate;
        rangeStartDate = rangeEndDate;
        rangeEndDate = previousStartDate;

        if (rangeStartDatePicker != null) {
            rangeStartDatePicker.setValue(rangeStartDate);
        }

        if (rangeEndDatePicker != null) {
            rangeEndDatePicker.setValue(rangeEndDate);
        }
    }

    private void refreshFilteredTimeline() {
        renderTimeline();
    }

    private void handleDateInputChange() {
        if (updatingDateControls) {
            return;
        }

        syncDateValuesFromControls();
        awaitingRangeEnd = false;
        updateCalendarMonthFromSelectedDates();
        updateCalendarDisplay();
    }

    private void syncDateValuesFromControls() {
        specificDate = specificDatePicker == null ? null : specificDatePicker.getValue();
        rangeStartDate = rangeStartDatePicker == null ? null : rangeStartDatePicker.getValue();
        rangeEndDate = rangeEndDatePicker == null ? null : rangeEndDatePicker.getValue();
        rangeStartTime = parseTime(rangeStartTimeField == null ? "" : rangeStartTimeField.getText());
        rangeEndTime = parseTime(rangeEndTimeField == null ? "" : rangeEndTimeField.getText());
    }

    private LocalTime parseTime(String value) {
        String cleanedValue = Strings.clean(value);

        if (cleanedValue.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(cleanedValue);
        } catch (DateTimeParseException exception) {
            try {
                return LocalTime.parse(cleanedValue, DateTimeFormatter.ofPattern("H:mm"));
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
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

    private void prepareDatePopover() {
        updateCalendarMonthFromSelectedDates();
        updateCalendarDisplay();
    }

    @FXML
    private void showPreviousCalendarMonth() {
        displayedCalendarMonth = displayedCalendarMonth.minusMonths(1);
        updateCalendarDisplay();
    }

    @FXML
    private void showNextCalendarMonth() {
        displayedCalendarMonth = displayedCalendarMonth.plusMonths(1);
        updateCalendarDisplay();
    }

    private void updateCalendarMonthFromSelectedDates() {
        LocalDate calendarDate = rangeStartDate != null
                ? rangeStartDate
                : rangeEndDate;

        if (calendarDate != null) {
            displayedCalendarMonth = YearMonth.from(calendarDate);
        }
    }

    private void updateCalendarDisplay() {
        if (dateCalendarGrid == null || dateCalendarMonthLabel == null || displayedCalendarMonth == null) {
            return;
        }

        dateCalendarGrid.getChildren().clear();
        dateCalendarMonthLabel.setText(displayedCalendarMonth.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL,
                Locale.ENGLISH
        ) + " " + displayedCalendarMonth.getYear());

        String[] dayNames = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};

        for (int column = 0; column < dayNames.length; column++) {
            Label label = new Label(dayNames[column]);
            label.getStyleClass().add("logs-calendar-day-name");
            dateCalendarGrid.add(label, column, 0);
        }

        LocalDate firstOfMonth = displayedCalendarMonth.atDay(1);
        int leadingDays = firstOfMonth.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        LocalDate firstVisibleDate = firstOfMonth.minusDays(leadingDays);

        for (int index = 0; index < 42; index++) {
            LocalDate date = firstVisibleDate.plusDays(index);
            Button dayButton = createCalendarDayButton(date);
            dateCalendarGrid.add(dayButton, index % 7, index / 7 + 1);
        }
    }

    private Button createCalendarDayButton(LocalDate date) {
        Button dayButton = new Button(String.valueOf(date.getDayOfMonth()));
        dayButton.getStyleClass().add("logs-calendar-day-button");
        dayButton.setFocusTraversable(false);
        dayButton.setMinSize(30, 27);
        dayButton.setPrefSize(30, 27);
        dayButton.setMaxSize(30, 27);

        if (!YearMonth.from(date).equals(displayedCalendarMonth)) {
            dayButton.getStyleClass().add("logs-calendar-day-outside");
        }

        if (date.equals(rangeStartDate) || date.equals(rangeEndDate)) {
            dayButton.getStyleClass().add("logs-calendar-day-selected");
        } else if (isDateInsideSelectedRange(date)) {
            dayButton.getStyleClass().add("logs-calendar-day-in-range");
        }

        dayButton.setOnAction(event -> selectCalendarDate(date));
        return dayButton;
    }

    private boolean isDateInsideSelectedRange(LocalDate date) {
        return rangeStartDate != null
                && rangeEndDate != null
                && date.isAfter(rangeStartDate)
                && date.isBefore(rangeEndDate);
    }

    private void selectCalendarDate(LocalDate date) {
        updatingDateControls = true;

        if (rangeStartDate == null || !awaitingRangeEnd) {
            rangeStartDate = date;
            rangeEndDate = date;
            awaitingRangeEnd = true;
        } else if (date.isBefore(rangeStartDate)) {
            rangeEndDate = rangeStartDate;
            rangeStartDate = date;
            awaitingRangeEnd = false;
        } else {
            rangeEndDate = date;
            awaitingRangeEnd = false;
        }

        if (rangeStartDatePicker != null) {
            rangeStartDatePicker.setValue(rangeStartDate);
        }

        if (rangeEndDatePicker != null) {
            rangeEndDatePicker.setValue(rangeEndDate);
        }

        updatingDateControls = false;
        displayedCalendarMonth = YearMonth.from(date);
        updateCalendarDisplay();
    }

    private void updateDateFilterButtonText() {
        String displayValue;

        if (dateFilterMode == DateFilterMode.SPECIFIC && specificDate != null) {
            displayValue = GROUP_DATE_FORMATTER.format(specificDate);
        } else if (dateFilterMode == DateFilterMode.RANGE
                && rangeStartDate != null
                && rangeStartDate.equals(rangeEndDate)) {
            displayValue = DATE_RANGE_FORMATTER.format(rangeStartDate);
        } else if (dateFilterMode == DateFilterMode.RANGE && rangeStartDate != null && rangeEndDate != null) {
            displayValue = DATE_RANGE_FORMATTER.format(rangeStartDate) + " - " + DATE_RANGE_FORMATTER.format(rangeEndDate);
        } else if (dateFilterMode == DateFilterMode.RANGE && rangeStartDate != null) {
            displayValue = "From " + DATE_RANGE_FORMATTER.format(rangeStartDate);
        } else if (dateFilterMode == DateFilterMode.RANGE && rangeEndDate != null) {
            displayValue = "Until " + DATE_RANGE_FORMATTER.format(rangeEndDate);
        } else {
            displayValue = "Date & Time";
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
                .filter(entry -> !isTestEntry(entry))
                .filter(this::matchesSearch)
                .filter(this::matchesFilters)
                .sorted(activitySortComparator())
                .toList();
    }

    private boolean isTestEntry(ActivityLogEntry entry) {
        String type = Strings.normalize(entry.type());
        String action = Strings.normalize(entry.action());
        String target = Strings.normalize(entry.target());
        if (type.equals("test")
                || action.contains("test action")
                || target.equals("testtarget")) {
            return true;
        }
        // Hide useless "Updated user/profile" entries that captured no actual field changes
        if ((action.contains("updated user") || action.contains("updated profile"))
                && entry.changes().isEmpty()) {
            return true;
        }
        return false;
    }

    private Comparator<ActivityLogEntry> activitySortComparator() {
        if (SORT_ACTION_ASC.equals(selectedSort)) {
            return Comparator
                    .comparing((ActivityLogEntry entry) -> formatAction(entry.action()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(this::parseActivityTimestamp, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        if (SORT_ACTION_DESC.equals(selectedSort)) {
            return Comparator
                    .comparing((ActivityLogEntry entry) -> formatAction(entry.action()), String.CASE_INSENSITIVE_ORDER.reversed())
                    .thenComparing(this::parseActivityTimestamp, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        Comparator<LocalDateTime> timestampComparator = SORT_OLDEST_FIRST.equals(selectedSort)
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
        List<String> actors = activityEntries.stream()
                .map(ActivityLogEntry::actor)
                .filter(actor -> !actor.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<String> filterOptions = new ArrayList<>();
        filterOptions.add(ALL_USERS);
        filterOptions.addAll(actors);

        filterUserOptions.clear();
        filterUserOptions.addAll(filterOptions);

        if (!filterUserOptions.contains(selectedUser)) {
            selectedUser = ALL_USERS;
        }

        if (!filterUserOptions.contains(pendingUser)) {
            pendingUser = selectedUser;
        }

        if (filtersMenuButton != null) {
            setMenuButtonDisplay(filtersMenuButton, FILTER_ICON_GLYPH, "Filters", filtersButtonText());
        }

        if (userFilterComboBox == null) {
            return;
        }

        String comboSelectedUser = userFilterComboBox.getValue();

        userFilterComboBox.getItems().setAll(filterUserOptions);

        if (comboSelectedUser != null && filterUserOptions.contains(comboSelectedUser)) {
            userFilterComboBox.setValue(comboSelectedUser);
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
