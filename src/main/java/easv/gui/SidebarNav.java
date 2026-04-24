package easv.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarNav extends VBox {
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    public SidebarNav(String activeItem, Consumer<String> onNavigate) {
        getStyleClass().add("dashboard-sidebar");
        setSpacing(14);
        setPadding(new Insets(18, 14, 18, 14));
        setPrefWidth(190);
        setMinWidth(190);

        Region logoMark = new Region();
        logoMark.getStyleClass().add("sidebar-logo-mark");
        Label logoTitle = new Label("Scanning Portal");
        logoTitle.getStyleClass().add("sidebar-logo-title");
        HBox logoBlock = new HBox(10, logoMark, logoTitle);
        logoBlock.setAlignment(Pos.CENTER_LEFT);
        logoBlock.getStyleClass().add("sidebar-logo-row");

        VBox navList = new VBox(4);
        navList.getStyleClass().add("sidebar-nav-list");
        for (String item : new String[]{"Dashboard", "Start Scan", "My Scans", "Exports", "Settings", "Help"}) {
            Button button = new Button(item);
            button.getStyleClass().add("sidebar-nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            button.setGraphic(buildNavRow(item));
            button.setOnAction(event -> onNavigate.accept(item));
            navButtons.put(item, button);
            navList.getChildren().add(button);
        }
        setActiveItem(activeItem);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label initials = new Label("JD");
        initials.getStyleClass().add("sidebar-user-avatar");
        Label name = new Label("Jane Doe");
        name.getStyleClass().add("sidebar-user-name");
        Label role = new Label("User");
        role.getStyleClass().add("sidebar-user-role");
        VBox userText = new VBox(2, name, role);
        Region userSpacer = new Region();
        HBox.setHgrow(userSpacer, Priority.ALWAYS);
        Label chevron = new Label("v");
        chevron.getStyleClass().add("sidebar-user-role");
        HBox userSection = new HBox(10, initials, userText, userSpacer, chevron);
        userSection.setAlignment(Pos.CENTER_LEFT);
        userSection.getStyleClass().add("sidebar-user-section");

        getChildren().addAll(logoBlock, navList, spacer, userSection);
    }

    private HBox buildNavRow(String item) {
        Region icon = new Region();
        icon.getStyleClass().addAll("sidebar-nav-icon", "sidebar-nav-icon-" + iconKey(item));

        Label text = new Label(item);
        text.getStyleClass().add("sidebar-nav-label");

        HBox row = new HBox(10, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String iconKey(String item) {
        return switch (item) {
            case "Start Scan" -> "start";
            case "My Scans" -> "scans";
            case "Exports" -> "exports";
            case "Settings" -> "settings";
            case "Help" -> "help";
            default -> "dashboard";
        };
    }

    public void setActiveItem(String activeItem) {
        navButtons.forEach((item, button) -> {
            button.getStyleClass().remove("active");
            if (item.equals(activeItem)) {
                button.getStyleClass().add("active");
            }
        });
    }
}
