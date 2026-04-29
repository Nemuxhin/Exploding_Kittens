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

    @FXML
    private void initialize() {
        populateSummaryCards();
        populateNeedsAttention();
        populateWorkflowStatus();
    }

    private void populateSummaryCards() {
        totalUsersValueLabel.setText("24");
        activeProfilesValueLabel.setText("3");
        scansTodayValueLabel.setText("12");
        waitingForQaValueLabel.setText("8");
    }

    private void populateNeedsAttention() {
        int usersWithNoProfiles = 3;
        int failedExports = 2;
        int draftProfiles = 1;

        usersNoProfilesCountLabel.setText(usersWithNoProfiles + " users have no profiles");
        failedExportsCountLabel.setText(failedExports + " failed exports");
        draftProfilesCountLabel.setText(draftProfiles + " draft profile");

        int totalNeedsAttention = usersWithNoProfiles + failedExports + draftProfiles;
        needsAttentionValueLabel.setText(String.valueOf(totalNeedsAttention));
    }

    private void populateWorkflowStatus() {
        int inProgress = 18;
        int waitingForQa = 8;
        int exported = 31;
        int total = inProgress + waitingForQa + exported;

        inProgressValueLabel.setText(String.valueOf(inProgress));
        workflowWaitingQaValueLabel.setText(String.valueOf(waitingForQa));
        exportedValueLabel.setText(String.valueOf(exported));

        setProgressWidth(inProgressBar, inProgress, total);
        setProgressWidth(waitingQaBar, waitingForQa, total);
        setProgressWidth(exportedBar, exported, total);
    }

    private void setProgressWidth(Region bar, int value, int total) {
        double width = total == 0 ? 0 : Math.max(36, (value / (double) total) * 360);
        bar.setPrefWidth(width);
    }

    @FXML
    private void createUser() {
        // Later: navigate to Users / open create-user flow.
    }

    @FXML
    private void createProfile() {
        // Later: navigate to Profiles / open create-profile flow.
    }

    @FXML
    private void createMetadataTemplate() {
        // Later: navigate to Metadata Templates / open create-template flow.
    }

    @FXML
    private void manageAccess() {
        // Later: navigate to Profile Access.
    }

    @FXML
    private void viewActivity() {
        // Later: navigate to Activity Log.
    }

    @FXML
    private void reviewFailedExports() {
        // Later: navigate to Metadata Review filtered by failed/export-blocked records.
    }
}