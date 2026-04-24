package easv.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class DashboardHeader extends HBox {
    public DashboardHeader() {
        getStyleClass().add("dashboard-header");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);

        StackPane menuButton = buildCircle("=", "header-menu-icon");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane notification = buildCircle("o", "header-notification-icon");
        StackPane avatar = buildCircle("JD", "header-avatar");

        getChildren().addAll(menuButton, spacer, notification, avatar);
    }

    private StackPane buildCircle(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass + "-text");
        StackPane pane = new StackPane(label);
        pane.getStyleClass().add(styleClass);
        return pane;
    }
}
