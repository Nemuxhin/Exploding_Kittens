package easv.gui.controller.admin;

import easv.be.AuditLog;
import easv.bll.AdminManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class DashboardController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label totalUsersValueLabel;
    @FXML private Label activeProfilesValueLabel;
    @FXML private Label scansTodayValueLabel;
    @FXML private Label waitingForQaValueLabel;

    @FXML private Label needsAttentionValueLabel;
    @FXML private Label usersNoProfilesCountLabel;
    @FXML private Label failedExportsCountLabel;
    @FXML private Label draftProfilesCountLabel;

    @FXML private Label inProgressValueLabel;
    @FXML private Label workflowWaitingQaValueLabel;
    @FXML private Label exportedValueLabel;

    @FXML private Region inProgressBar;
    @FXML private Region waitingQaBar;
    @FXML private Region exportedBar;
    @FXML private VBox recentActivityList;

    private AdminNavigator navigator = AdminNavigator.none();
    private AdminManager adminManager = new AdminManager();

    void setNavigator(AdminNavigator navigator) {
        this.navigator = navigator == null ? AdminNavigator.none() : navigator;
    }

    void setAdminManager(AdminManager adminManager) {
        this.adminManager = adminManager == null ? new AdminManager() : adminManager;
        refreshDashboard();
    }

    @FXML
    private void initialize() {
        refreshDashboard();
    }

    private void refreshDashboard() {
        populateSummaryCards();
        populateNeedsAttention();
        populateWorkflowStatus();
        populateRecentActivity();
    }

    private void populateSummaryCards() {
        AdminManager.DashboardSummary summary = adminManager.getDashboardSummary();
        LocalDate today = LocalDate.now();

        int scansToday = countLogs(log ->
                "Scans".equalsIgnoreCase(log.getType())
                        && log.getTimestamp() != null
                        && today.equals(log.getTimestamp().toLocalDate())
        );

        int waitingForQa = countLogs(log ->
                contains(log.getAction(), "waiting")
                        || contains(log.getDescription(), "waiting")
                        || contains(log.getStatus(), "qa")
        );

        totalUsersValueLabel.setText(String.valueOf(summary.getTotalUsers()));
        activeProfilesValueLabel.setText(String.valueOf(summary.getActiveProfiles()));
        scansTodayValueLabel.setText(String.valueOf(scansToday));
        waitingForQaValueLabel.setText(String.valueOf(waitingForQa));
    }

    private void populateNeedsAttention() {
        AdminManager.DashboardSummary summary = adminManager.getDashboardSummary();
        int totalNeedsAttention = summary.getUsersWithoutProfiles()
                + summary.getFailedEvents()
                + summary.getDraftProfiles();

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

        int waitingForQa = countLogs(log ->
                contains(log.getAction(), "qa")
                        && (contains(log.getStatus(), "waiting") || contains(log.getDescription(), "waiting"))
        );

        int exported = countLogs(log ->
                "Exports".equalsIgnoreCase(log.getType())
                        && "Success".equalsIgnoreCase(log.getStatus())
        );

        int total = inProgress + waitingForQa + exported;

        inProgressValueLabel.setText(String.valueOf(inProgress));
        workflowWaitingQaValueLabel.setText(String.valueOf(waitingForQa));
        exportedValueLabel.setText(String.valueOf(exported));

        setProgressWidth(inProgressBar, inProgress, total);
        setProgressWidth(waitingQaBar, waitingForQa, total);
        setProgressWidth(exportedBar, exported, total);
    }

    private void populateRecentActivity() {
        if (recentActivityList == null) {
            return;
        }

        List<AuditLog> recentLogs = adminManager.getAuditLogs().stream()
                .limit(5)
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

        Label titleLabel = new Label(log.getAction());
        titleLabel.getStyleClass().add("dashboard-activity-title");

        Label detailLabel = new Label(log.getDescription());
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

    private void setProgressWidth(Region bar, int value, int total) {
        double width = total == 0 || value == 0 ? 0 : Math.max(36, (value / (double) total) * 360);
        bar.setPrefWidth(width);
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

    private boolean contains(String value, String searchText) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(searchText.toLowerCase(Locale.ROOT));
    }

    private String pluralize(int count, String singularText) {
        if (count == 1) {
            return "1 " + singularText;
        }

        return count + " " + singularText + "s";
    }

    private String iconBoxClassFor(AuditLog log) {
        if ("Failed".equalsIgnoreCase(log.getStatus()) || "Warning".equalsIgnoreCase(log.getStatus())) {
            return "dashboard-activity-icon-warning-box";
        }

        return switch (log.getType()) {
            case "Scans" -> "dashboard-activity-icon-scan-box";
            case "Profiles" -> "dashboard-activity-icon-profile-box";
            case "Access" -> "dashboard-activity-icon-access-box";
            case "QA" -> "dashboard-activity-icon-qa-box";
            default -> "dashboard-activity-icon-profile-box";
        };
    }

    private String iconPathClassFor(AuditLog log) {
        if ("Failed".equalsIgnoreCase(log.getStatus()) || "Warning".equalsIgnoreCase(log.getStatus())) {
            return "dashboard-activity-icon-warning-path";
        }

        return switch (log.getType()) {
            case "Scans" -> "dashboard-activity-icon-scan-path";
            case "Profiles" -> "dashboard-activity-icon-profile-path";
            case "Access" -> "dashboard-activity-icon-access-path";
            case "QA" -> "dashboard-activity-icon-qa-path";
            default -> "dashboard-activity-icon-profile-path";
        };
    }

    private String iconPathFor(AuditLog log) {
        if ("Failed".equalsIgnoreCase(log.getStatus()) || "Warning".equalsIgnoreCase(log.getStatus())) {
            return "M12 2 1 21h22L12 2zm0 6 1 7h-2l1-7zm-1 9h2v2h-2v-2z";
        }

        return switch (log.getType()) {
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
        navigator.showMetadataReview();
    }

    private interface LogPredicate {
        boolean matches(AuditLog log);
    }
}
