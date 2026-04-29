package easv.gui.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

public class DashboardController {

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

    private AdminNavigator navigator = AdminNavigator.none();

    void setNavigator(AdminNavigator navigator) {
        this.navigator = navigator == null ? AdminNavigator.none() : navigator;
    }

    @FXML
    private void initialize() {
        populateSummaryCards();
        populateNeedsAttention();
        populateWorkflowStatus();
    }

    private void populateSummaryCards() {
        AdminDemoData.DashboardSummary summary = AdminDemoData.dashboardSummary();

        totalUsersValueLabel.setText(String.valueOf(summary.totalUsers()));
        activeProfilesValueLabel.setText(String.valueOf(summary.activeProfiles()));
        scansTodayValueLabel.setText(String.valueOf(summary.scansToday()));
        waitingForQaValueLabel.setText(String.valueOf(summary.waitingForQa()));
    }

    private void populateNeedsAttention() {
        AdminDemoData.NeedsAttention needsAttention = AdminDemoData.needsAttention();

        usersNoProfilesCountLabel.setText(needsAttention.usersWithNoProfiles() + " users have no profiles");
        failedExportsCountLabel.setText(needsAttention.failedExports() + " failed exports");
        draftProfilesCountLabel.setText(needsAttention.draftProfiles() + " draft profile");
        needsAttentionValueLabel.setText(String.valueOf(needsAttention.total()));
    }

    private void populateWorkflowStatus() {
        AdminDemoData.WorkflowStatus workflowStatus = AdminDemoData.workflowStatus();

        inProgressValueLabel.setText(String.valueOf(workflowStatus.inProgress()));
        workflowWaitingQaValueLabel.setText(String.valueOf(workflowStatus.waitingForQa()));
        exportedValueLabel.setText(String.valueOf(workflowStatus.exported()));

        setProgressWidth(inProgressBar, workflowStatus.inProgress(), workflowStatus.total());
        setProgressWidth(waitingQaBar, workflowStatus.waitingForQa(), workflowStatus.total());
        setProgressWidth(exportedBar, workflowStatus.exported(), workflowStatus.total());
    }

    private void setProgressWidth(Region bar, int value, int total) {
        double width = total == 0 ? 0 : Math.max(36, (value / (double) total) * 360);
        bar.setPrefWidth(width);
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
}
