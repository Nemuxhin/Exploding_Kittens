package easv.gui;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class ProfileSummaryCard extends VBox {
    public ProfileSummaryCard(String titleText, UserPortalModel.ProfileItem profile, List<UserPortalModel.ProfileSetting> settings, Runnable onViewDetails) {
        getStyleClass().addAll("dashboard-card", "profile-summary-card");
        setSpacing(12);

        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        Label profileName = new Label(profile.name());
        profileName.getStyleClass().add("profile-summary-name");
        Label badge = new Label("Default");
        badge.getStyleClass().add("default-badge");
        HBox profileHeader = new HBox(8, profileName, badge);
        Label description = new Label(profile.description());
        description.getStyleClass().add("dashboard-card-body");
        description.setWrapText(true);

        VBox settingsList = new VBox(10);
        for (UserPortalModel.ProfileSetting setting : settings) {
            Region check = new Region();
            check.getStyleClass().add("profile-setting-check");
            Label label = new Label(setting.label() + ": " + setting.value());
            label.getStyleClass().add("profile-setting-bullet");
            HBox row = new HBox(10, new StackPane(check), label);
            row.getStyleClass().add("profile-setting-bullet-row");
            settingsList.getChildren().add(row);
        }

        Label detailsLink = new Label("View full details ->");
        detailsLink.getStyleClass().add("dashboard-link");
        detailsLink.setOnMouseClicked(event -> onViewDetails.run());

        getChildren().addAll(title, profileHeader, description, settingsList, detailsLink);
    }
}
