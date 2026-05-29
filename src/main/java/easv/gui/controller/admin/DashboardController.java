package easv.gui.controller.admin;

import easv.be.AuditLog;
import easv.be.ReviewRecord;
import easv.bll.AdminManager;
import easv.gui.PrimeIcons;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DashboardController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final int MAX_RECENT_ACTIVITY_ITEMS = 5;
    private static final String ALL_ACTIVITY_TYPES = "All activity";
    private static final List<String> RECENT_ACTIVITY_FILTER_OPTIONS = List.of(
            ALL_ACTIVITY_TYPES,
            "Scans",
            "QA",
            "Profiles",
            "Users",
            "Access",
            "Exports",
            "Files",
            "Security"
    );

    private static final Set<String> ACRONYM_WORDS =
            Set.of("TIFF", "QA", "ID", "IP", "PDF", "PNG", "JPG", "OCR", "API", "URL");

    @FXML private Label totalUsersValueLabel;
    @FXML private Label activeProfilesValueLabel;
    @FXML private Label scansTodayValueLabel;
    @FXML private Label waitingForQaValueLabel;

    @FXML private Label totalUsersTrendLabel;
    @FXML private Label activeProfilesTrendLabel;
    @FXML private Label scansTodayTrendLabel;
    @FXML private Label waitingForQaTrendLabel;

    @FXML private Label needsAttentionValueLabel;
    @FXML private Label usersNoProfilesCountLabel;
    @FXML private Label failedExportsCountLabel;
    @FXML private Label draftProfilesCountLabel;
    @FXML private VBox needsAttentionCard;
    @FXML private HBox usersNoProfilesRow;
    @FXML private HBox failedEventsRow;
    @FXML private HBox draftProfilesRow;

    @FXML private VBox recentActivityList;
    @FXML private ComboBox<String> recentActivityFilterComboBox;

    private AdminNavigator navigator = AdminNavigator.none();
    private AdminManager adminManager;

    void setNavigator(AdminNavigator navigator) {
        this.navigator = navigator == null ? AdminNavigator.none() : navigator;
    }

    void setAdminManager(AdminManager adminManager) {
        this.adminManager = adminManager;

        if (this.adminManager == null) {
            return;
        }

        refreshDashboard();
    }

    @FXML
    private void initialize() {
        configureRecentActivityFilter();
    }

    private void refreshDashboard() {
        if (adminManager == null) {
            return;
        }

        populateSummaryCards();
        populateNeedsAttention();
        populateRecentActivity();
    }

    private void populateSummaryCards() {
        AdminManager.DashboardSummary summary = adminManager.getDashboardSummary();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        int scansToday = countLogs(log ->
                "Scans".equalsIgnoreCase(log.getType())
                        && log.getTimestamp() != null
                        && today.equals(log.getTimestamp().toLocalDate())
        );

        int scansYesterday = countLogs(log ->
                "Scans".equalsIgnoreCase(log.getType())
                        && log.getTimestamp() != null
                        && yesterday.equals(log.getTimestamp().toLocalDate())
        );

        int waitingForQa = countWaitingForQaRecords();

        totalUsersValueLabel.setText(String.valueOf(summary.getTotalUsers()));
        activeProfilesValueLabel.setText(String.valueOf(summary.getActiveProfiles()));
        scansTodayValueLabel.setText(String.valueOf(scansToday));
        waitingForQaValueLabel.setText(String.valueOf(waitingForQa));

        /*
         * Important:
         * These totals do not have real historical baseline data in DashboardSummary.
         * So showing "- 0%" is misleading.
         *
         * If the previous baseline is zero and the current value is above zero,
         * the trend shows "+ X" instead of fake "+ 100%".
         */
        setTrend(totalUsersTrendLabel, summary.getTotalUsers(), 0, "vs last 7 days");
        setTrend(activeProfilesTrendLabel, summary.getActiveProfiles(), 0, "vs last 7 days");
        setTrend(scansTodayTrendLabel, scansToday, scansYesterday, "vs yesterday");
        setTrend(waitingForQaTrendLabel, waitingForQa, 0, "vs yesterday");
    }

    private void populateNeedsAttention() {
        AdminManager.DashboardSummary summary = adminManager.getDashboardSummary();

        int totalNeedsAttention = summary.getUsersWithoutProfiles()
                + summary.getFailedEvents()
                + summary.getDraftProfiles();

        boolean hasNeedsAttention = totalNeedsAttention > 0;

        if (needsAttentionCard != null) {
            needsAttentionCard.setVisible(hasNeedsAttention);
            needsAttentionCard.setManaged(hasNeedsAttention);
        }

        setAttentionRowState(usersNoProfilesRow, summary.getUsersWithoutProfiles() > 0);
        setAttentionRowState(failedEventsRow, summary.getFailedEvents() > 0);
        setAttentionRowState(draftProfilesRow, summary.getDraftProfiles() > 0);

        usersNoProfilesCountLabel.setText(pluralize(summary.getUsersWithoutProfiles(), "user") + " have no profiles");
        failedExportsCountLabel.setText(pluralize(summary.getFailedEvents(), "failed event"));
        draftProfilesCountLabel.setText(pluralize(summary.getDraftProfiles(), "draft profile"));
        needsAttentionValueLabel.setText(String.valueOf(totalNeedsAttention));
    }

    private void populateRecentActivity() {
        if (recentActivityList == null || adminManager == null) {
            return;
        }

        List<AuditLog> recentLogs = adminManager.getAuditLogs().stream()
                .filter(log -> !isLoginLog(log))
                .filter(this::matchesRecentActivityFilter)
                .limit(MAX_RECENT_ACTIVITY_ITEMS)
                .toList();

        if (recentLogs.isEmpty()) {
            Label emptyLabel = new Label("No activity yet");
            emptyLabel.getStyleClass().add("dashboard-activity-detail");
            recentActivityList.getChildren().setAll(emptyLabel);
            return;
        }

        recentActivityList.getChildren().setAll(
                recentLogs.stream()
                        .map(this::createRecentActivityRow)
                        .toList()
        );
    }

    private void configureRecentActivityFilter() {
        if (recentActivityFilterComboBox == null) {
            return;
        }

        recentActivityFilterComboBox.getItems().setAll(RECENT_ACTIVITY_FILTER_OPTIONS);
        recentActivityFilterComboBox.setValue(ALL_ACTIVITY_TYPES);
        recentActivityFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> populateRecentActivity());
    }

    private boolean matchesRecentActivityFilter(AuditLog log) {
        if (log == null || recentActivityFilterComboBox == null) {
            return true;
        }

        String selectedType = recentActivityFilterComboBox.getValue();
        if (selectedType == null || selectedType.isBlank() || ALL_ACTIVITY_TYPES.equalsIgnoreCase(selectedType)) {
            return true;
        }

        return selectedType.equalsIgnoreCase(safeText(log.getType(), ""));
    }

    private HBox createRecentActivityRow(AuditLog log) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-activity-row");

        StackPane iconBox = createRecentActivityIcon(log);

        VBox copyBox = new VBox(4);
        HBox.setHgrow(copyBox, Priority.ALWAYS);

        Label titleLabel = new Label(humanizeAction(log.getAction()));
        titleLabel.getStyleClass().add("dashboard-activity-title");

        Label detailLabel = new Label(safeText(log.getDescription(), "No details available."));
        detailLabel.getStyleClass().add("dashboard-activity-detail");
        detailLabel.setWrapText(true);

        copyBox.getChildren().setAll(titleLabel, detailLabel);

        Label timeLabel = new Label(formatActivityTime(log.getTimestamp()));
        timeLabel.getStyleClass().add("dashboard-activity-time");

        row.getChildren().setAll(iconBox, copyBox, timeLabel);
        return row;
    }

    private StackPane createRecentActivityIcon(AuditLog log) {
        Label icon = PrimeIcons.create(iconGlyphFor(log), iconPathClassFor(log));

        StackPane iconBox = new StackPane(icon);
        iconBox.getStyleClass().add(iconBoxClassFor(log));

        return iconBox;
    }

    private String formatActivityTime(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }

        LocalDate today = LocalDate.now();
        LocalDate activityDate = timestamp.toLocalDate();

        if (activityDate.equals(today)) {
            return TIME_FORMATTER.format(timestamp);
        }

        if (activityDate.equals(today.minusDays(1))) {
            return "Yesterday";
        }

        return activityDate.toString();
    }

    private int countLogs(LogPredicate predicate) {
        List<AuditLog> logs = adminManager.getAuditLogs();
        int matches = 0;

        for (AuditLog log : logs) {
            if (predicate.matches(log)) {
                matches++;
            }
        }

        return matches;
    }

    private int countReviewRecords(ReviewPredicate predicate) {
        List<ReviewRecord> records = adminManager.getReviewRecords();
        int matches = 0;

        for (ReviewRecord record : records) {
            if (predicate.matches(record)) {
                matches++;
            }
        }

        return matches;
    }

    private int countWaitingForQaRecords() {
        return countReviewRecords(record ->
                contains(record.getQaStatus(), "waiting")
                        || contains(record.getQaStatus(), "ready")
                        || contains(record.getDocumentDetailsStatus(), "ready")
        );
    }

    private void setNeutralTrend(Label label, String comparison) {
        label.setText(comparison);
        label.getStyleClass().setAll("dashboard-trend-neutral");
    }

    private void setTrend(Label label, int currentValue, int previousValue, String comparison) {
        if (previousValue == 0) {
            if (currentValue == 0) {
                setNeutralTrend(label, comparison);
                return;
            }

            label.setText("+" + currentValue + " " + comparison);
            label.getStyleClass().setAll("dashboard-trend-up");
            return;
        }

        int changePercent = (int) Math.round(((currentValue - previousValue) / (double) previousValue) * 100);

        if (changePercent == 0) {
            setNeutralTrend(label, comparison);
        } else if (changePercent > 0) {
            label.setText("+" + changePercent + "% " + comparison);
            label.getStyleClass().setAll("dashboard-trend-up");
        } else {
            label.setText("-" + Math.abs(changePercent) + "% " + comparison);
            label.getStyleClass().setAll("dashboard-trend-down");
        }
    }

    private void setAttentionRowState(HBox row, boolean shouldShow) {
        if (row == null) {
            return;
        }

        row.setVisible(shouldShow);
        row.setManaged(shouldShow);
    }

    private boolean contains(String value, String searchText) {
        return value != null
                && searchText != null
                && value.toLowerCase(Locale.ROOT).contains(searchText.toLowerCase(Locale.ROOT));
    }

    private String pluralize(int count, String singularText) {
        if (count == 1) {
            return "1 " + singularText;
        }

        return count + " " + singularText + "s";
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    private boolean isLoginLog(AuditLog log) {
        String action = log == null ? "" : safeText(log.getAction(), "");

        if (action.isBlank()) {
            return false;
        }

        String upper = action.toUpperCase(Locale.ROOT);
        return upper.contains("LOGIN") || upper.equals("LOGOUT");
    }

    private String humanizeAction(String action) {
        if (action == null || action.isBlank()) {
            return "Activity";
        }

        String[] parts = action.split("_");
        StringBuilder result = new StringBuilder();
        boolean firstWord = true;

        for (String rawWord : parts) {
            if (rawWord == null || rawWord.isEmpty()) {
                continue;
            }

            if (!firstWord) {
                result.append(' ');
            }

            String upperWord = rawWord.toUpperCase(Locale.ROOT);

            if (ACRONYM_WORDS.contains(upperWord)) {
                result.append(upperWord);
            } else if (firstWord) {
                result.append(Character.toUpperCase(rawWord.charAt(0)));
                result.append(rawWord.substring(1).toLowerCase(Locale.ROOT));
            } else {
                result.append(rawWord.toLowerCase(Locale.ROOT));
            }

            firstWord = false;
        }

        return result.length() == 0 ? "Activity" : result.toString();
    }

    private String iconBoxClassFor(AuditLog log) {
        if (isWarningOrFailed(log)) {
            return "dashboard-activity-icon-warning-box";
        }

        return switch (safeText(log.getType(), "")) {
            case "Scans" -> "dashboard-activity-icon-scan-box";
            case "Profiles" -> "dashboard-activity-icon-profile-box";
            case "Access" -> "dashboard-activity-icon-access-box";
            case "QA" -> "dashboard-activity-icon-qa-box";
            default -> "dashboard-activity-icon-profile-box";
        };
    }

    private String iconPathClassFor(AuditLog log) {
        if (isWarningOrFailed(log)) {
            return "dashboard-activity-icon-warning-path";
        }

        return switch (safeText(log.getType(), "")) {
            case "Scans" -> "dashboard-activity-icon-scan-path";
            case "Profiles" -> "dashboard-activity-icon-profile-path";
            case "Access" -> "dashboard-activity-icon-access-path";
            case "QA" -> "dashboard-activity-icon-qa-path";
            default -> "dashboard-activity-icon-profile-path";
        };
    }

    private String iconGlyphFor(AuditLog log) {
        if (isWarningOrFailed(log)) {
            return "\ue922";
        }

        return switch (safeText(log.getType(), "")) {
            case "Scans" -> "\ue9e4";
            case "Access" -> "\ue93f";
            case "QA" -> "\ue90a";
            default -> "\ue992";
        };
    }

    private boolean isWarningOrFailed(AuditLog log) {
        return "Failed".equalsIgnoreCase(log.getStatus())
                || "Warning".equalsIgnoreCase(log.getStatus());
    }

    @FXML
    private void createUser() {
        navigator.showUsers();
    }

    @FXML
    private void createProfile() {
        navigator.showProfiles();
    }

    @FXML
    private void manageAccess() {
        navigator.showAssignments();
    }

    @FXML
    private void viewActivity() {
        navigator.showActivity();
    }

    @FXML
    private void reviewFailedExports() {
        navigator.showReview();
    }

    private interface LogPredicate {
        boolean matches(AuditLog log);
    }

    private interface ReviewPredicate {
        boolean matches(ReviewRecord record);
    }
}
