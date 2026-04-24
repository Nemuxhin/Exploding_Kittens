package easv.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ActionCard extends VBox {
    private final Button actionButton;

    public ActionCard(String iconText, String titleText, String bodyText, String buttonText, boolean primaryButton, Runnable action) {
        getStyleClass().addAll("dashboard-card", "portal-action-card");
        setSpacing(12);

        Region iconGlyph = new Region();
        iconGlyph.getStyleClass().addAll("dashboard-action-glyph", "dashboard-action-glyph-" + iconText.toLowerCase());
        StackPane icon = new StackPane(iconGlyph);
        icon.getStyleClass().add("dashboard-action-icon");
        HBox iconRow = new HBox(icon);

        Label title = new Label(titleText);
        title.getStyleClass().add("dashboard-card-title-lg");
        Label body = new Label(bodyText);
        body.getStyleClass().add("dashboard-card-body");
        body.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        actionButton = new Button(buttonText);
        actionButton.getStyleClass().add(primaryButton ? "dashboard-primary-button" : "dashboard-outline-button");
        actionButton.setOnAction(event -> action.run());

        HBox buttonRow = new HBox(actionButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(iconRow, title, body, spacer, buttonRow);
    }

    public Button getActionButton() {
        return actionButton;
    }
}
