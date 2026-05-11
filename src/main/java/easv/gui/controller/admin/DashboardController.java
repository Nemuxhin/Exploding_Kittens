package easv.gui.controller.admin;

import easv.be.AuditLog;
import easv.be.ReviewRecord;
import easv.bll.AdminManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class DashboardController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final double DONUT_RADIUS = 22;
    private static final double DONUT_HOLE_RADIUS = 15;

    private static final int MAX_RECENT_ACTIVITY_ITEMS = 5;

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

    @FXML private Label inProgressValueLabel;
    @FXML private Label workflowWaitingQaValueLabel;
    @FXML private Label exportedValueLabel;

    @FXML private Label inProgressPercentLabel;
    @FXML private Label waitingQaPercentLabel;
    @FXML private Label exportedPercentLabel;

    @FXML private StackPane workflowDonutChart;
    @FXML private VBox recentActivityList;

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
    }

    private void refreshDashboard() {
        if (adminManager == null) {
            return;
        }

        populateSummaryCards();
        populateNeedsAttention();
        populateWorkflowStatus();
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
        setTrend(totalUsersTrendLabel, summary.getTotalUsers(), 0);
        setTrend(activeProfilesTrendLabel, summary.getActiveProfiles(), 0);
        setTrend(scansTodayTrendLabel, scansToday, scansYesterday);
        setTrend(waitingForQaTrendLabel, waitingForQa, 0);
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

    private void populateWorkflowStatus() {
        int inProgress = countLogs(log ->
                "Scans".equalsIgnoreCase(log.getType())
                        && (contains(log.getStatus(), "progress") || contains(log.getAction(), "started"))
        );

        int waitingForQa = countWaitingForQaRecords();

        int exported = countLogs(log ->
                "Exports".equalsIgnoreCase(log.getType())
                        && "Success".equalsIgnoreCase(log.getStatus())
        );

        int total = inProgress + waitingForQa + exported;

        inProgressValueLabel.setText(String.valueOf(inProgress));
        workflowWaitingQaValueLabel.setText(String.valueOf(waitingForQa));
        exportedValueLabel.setText(String.valueOf(exported));

        inProgressPercentLabel.setText(formatPercent(inProgress, total));
        waitingQaPercentLabel.setText(formatPercent(waitingForQa, total));
        exportedPercentLabel.setText(formatPercent(exported, total));

        renderWorkflowDonut(inProgress, waitingForQa, exported);
    }

    private void populateRecentActivity() {
        if (recentActivityList == null) {
            return;
        }

        List<AuditLog> recentLogs = adminManager.getAuditLogs().stream()
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

    private HBox createRecentActivityRow(AuditLog log) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-activity-row");

        StackPane iconBox = createRecentActivityIcon(log);

        VBox copyBox = new VBox(4);
        HBox.setHgrow(copyBox, Priority.ALWAYS);

        Label titleLabel = new Label(safeText(log.getAction(), "Activity"));
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
        SVGPath icon = new SVGPath();
        icon.setContent(iconPathFor(log));
        icon.getStyleClass().add(iconPathClassFor(log));

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
                        || contains(record.getMetadataStatus(), "ready")
        );
    }

    private void setNeutralTrend(Label label) {
        label.setText("0");
        label.getStyleClass().setAll("dashboard-trend-neutral");
    }

    private void setTrend(Label label, int currentValue, int previousValue) {
        if (previousValue == 0) {
            if (currentValue == 0) {
                setNeutralTrend(label);
                return;
            }

            label.setText("+ " + currentValue);
            label.getStyleClass().setAll("dashboard-trend-up");
            return;
        }

        int changePercent = (int) Math.round(((currentValue - previousValue) / (double) previousValue) * 100);

        if (changePercent == 0) {
            setNeutralTrend(label);
        } else if (changePercent > 0) {
            label.setText("+ " + changePercent + "%");
            label.getStyleClass().setAll("dashboard-trend-up");
        } else {
            label.setText("- " + Math.abs(changePercent) + "%");
            label.getStyleClass().setAll("dashboard-trend-down");
        }
    }

    private String formatPercent(int value, int total) {
        if (total == 0 || value == 0) {
            return "0%";
        }

        return Math.round((value / (double) total) * 100) + "%";
    }

    private void renderWorkflowDonut(int inProgress, int waitingForQa, int exported) {
        if (workflowDonutChart == null) {
            return;
        }

        workflowDonutChart.getChildren().clear();

        Circle track = new Circle(DONUT_RADIUS);
        track.getStyleClass().add("dashboard-donut-track");
        workflowDonutChart.getChildren().add(track);

        int total = inProgress + waitingForQa + exported;

        if (total > 0) {
            double startAngle = 90;
            startAngle = addDonutSegment(inProgress, total, startAngle, "dashboard-donut-blue");
            startAngle = addDonutSegment(waitingForQa, total, startAngle, "dashboard-donut-amber");
            addDonutSegment(exported, total, startAngle, "dashboard-donut-green");
        }

        Circle hole = new Circle(DONUT_HOLE_RADIUS);
        hole.getStyleClass().add("dashboard-donut-hole");
        workflowDonutChart.getChildren().add(hole);
    }

    private double addDonutSegment(int value, int total, double startAngle, String styleClass) {
        if (value <= 0) {
            return startAngle;
        }

        double length = -360.0 * value / total;

        Arc segment = new Arc(0, 0, DONUT_RADIUS, DONUT_RADIUS, startAngle, length);
        segment.setType(ArcType.OPEN);
        segment.getStyleClass().add(styleClass);

        workflowDonutChart.getChildren().add(segment);

        return startAngle + length;
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

    private String iconPathFor(AuditLog log) {
        if (isWarningOrFailed(log)) {
            return "M12 2 1 21h22L12 2zm0 6 1 7h-2l1-7zm-1 9h2v2h-2v-2z";
        }

        return switch (safeText(log.getType(), "")) {
            case "Scans" ->
                    "M3 17.5 9.2 11.3l3.3 3.3L20 7.1V12h2V3.5h-8.5v2H18l-5.5 5.5-3.3-3.3L1.5 15.4 3 17.5z";
            case "Access" ->
                    "M8 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-3.31 0-6 1.79-6 4v2h8.1a6.48 6.48 0 0 1-.1-1.1c0-1.9.82-3.61 2.13-4.8A10.6 10.6 0 0 0 8 13zm8.5 0a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9zm-.5 6.7-2.1-2.1 1.06-1.06L16 17.59l2.54-2.55 1.06 1.06-3.6 3.6z";
            case "QA" ->
                    "M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z";
            default ->
                    "M5 3.5h14a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2zm0 2v13h14v-13H5zm2.5 3h4.2a2.4 2.4 0 0 1 4.6 0h1.2v2h-1.2a2.4 2.4 0 0 1-4.6 0H7.5v-2zm6.5 1.3a.8.8 0 1 0 0-1.6.8.8 0 0 0 0 1.6z";
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
    private void createMetadataTemplate() {
        navigator.showMetadataTemplates();
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