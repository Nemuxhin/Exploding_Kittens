package easv.gui.controller.user;

import easv.gui.UserPortalModel;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsController {
    private static final String[] SECTIONS = {
            "Account",
            "Dashboard",
            "Keyboard Shortcuts",
            "Notifications",
            "Scanning",
            "Exports",
            "Data & Privacy"
    };

    private final UserPortalModel portalModel;
    private final Map<String, Button> sectionButtons = new LinkedHashMap<>();
    private VBox detailsPanel;
    private String selectedSection = "Account";

    public SettingsController(UserPortalModel portalModel) {
        this.portalModel = portalModel;
    }

    public Node create() {
        sectionButtons.clear();

        VBox page = new VBox(28);
        page.getStyleClass().add("portal-page");

        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage your account and preferences");
        subtitle.getStyleClass().add("page-subtitle");
        VBox intro = new VBox(10, title, subtitle);
        intro.getStyleClass().add("portal-page-intro");

        VBox nav = buildSectionNav();
        detailsPanel = new VBox(20);
        detailsPanel.getStyleClass().addAll("portal-card", "settings-panel");

        HBox layout = new HBox(32, nav, detailsPanel);
        layout.getStyleClass().add("settings-layout");
        HBox.setHgrow(detailsPanel, Priority.ALWAYS);

        renderSection();

        page.getChildren().addAll(intro, layout);
        return page;
    }

    private VBox buildSectionNav() {
        VBox nav = new VBox();
        nav.getStyleClass().add("settings-nav");

        for (int index = 0; index < SECTIONS.length; index++) {
            String section = SECTIONS[index];
            Button button = new Button();
            button.getStyleClass().add("settings-nav-button");
            if (index == 0) {
                button.getStyleClass().add("settings-nav-button-first");
            }
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> {
                selectedSection = section;
                renderSection();
            });

            HBox row = new HBox(12,
                    UserPortalUi.buildIcon(iconKey(section), "settings-nav-icon"),
                    labelForSection(section)
            );
            row.getStyleClass().add("settings-nav-row");
            row.setAlignment(Pos.CENTER_LEFT);

            button.setGraphic(row);
            sectionButtons.put(section, button);
            nav.getChildren().add(button);
        }

        return nav;
    }

    private Label labelForSection(String section) {
        Label label = new Label(section);
        label.getStyleClass().add("settings-nav-label");
        return label;
    }

    private void renderSection() {
        sectionButtons.forEach((section, button) -> {
            button.getStyleClass().remove("active");
            if (section.equals(selectedSection)) {
                button.getStyleClass().add("active");
            }
        });

        detailsPanel.getChildren().setAll(
                switch (selectedSection) {
                    case "Account" -> buildAccountSection();
                    default -> buildPlaceholderSection(selectedSection);
                }
        );
    }

    private Node buildAccountSection() {
        UserPortalModel.AccountProfile account = portalModel.fetchAccountProfile();

        Label heading = new Label("Account Information");
        heading.getStyleClass().add("settings-section-heading");

        TextField fullNameField = createField(account.fullName());
        TextField emailField = createField(account.email());
        TextField departmentField = createField(account.department());

        Label saveMessage = new Label();
        saveMessage.getStyleClass().add("portal-inline-message");
        saveMessage.setVisible(false);
        saveMessage.setManaged(false);

        Button changePasswordButton = new Button("Change Password");
        changePasswordButton.getStyleClass().add("portal-secondary-button");

        Button saveButton = new Button("Save Changes");
        saveButton.getStyleClass().add("portal-primary-button");
        saveButton.setGraphic(UserPortalUi.buildIcon("save", "portal-button-icon"));
        saveButton.setOnAction(event -> {
            portalModel.updateAccountProfile(fullNameField.getText(), emailField.getText(), departmentField.getText());
            saveMessage.getStyleClass().removeAll("success", "error");
            saveMessage.getStyleClass().add("success");
            saveMessage.setText("Changes saved.");
            saveMessage.setVisible(true);
            saveMessage.setManaged(true);
        });

        VBox form = new VBox(14,
                formField("Full Name", fullNameField),
                formField("Email Address", emailField),
                formField("Department", departmentField)
        );
        form.getStyleClass().add("settings-form");

        VBox passwordBlock = new VBox(14,
                sectionLabel("Password"),
                changePasswordButton
        );

        VBox content = new VBox(20,
                heading,
                form,
                divider(),
                passwordBlock,
                divider(),
                new HBox(12, saveButton, saveMessage)
        );
        content.getStyleClass().add("settings-form");
        return content;
    }

    private Node buildPlaceholderSection(String section) {
        Label heading = new Label(section);
        heading.getStyleClass().add("settings-section-heading");
        Label body = new Label("This section is being rebuilt in the controller-based portal.");
        body.getStyleClass().add("portal-muted");
        body.setWrapText(true);

        VBox content = new VBox(16, heading, body);
        content.getStyleClass().add("settings-form");
        return content;
    }

    private VBox formField(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        return new VBox(8, label, field);
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("portal-section-title");
        return label;
    }

    private TextField createField(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("portal-text-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private Region divider() {
        Region divider = new Region();
        divider.getStyleClass().add("portal-divider");
        return divider;
    }

    private String iconKey(String section) {
        return switch (section) {
            case "Account" -> "account";
            case "Dashboard" -> "dashboard";
            case "Keyboard Shortcuts" -> "shortcuts";
            case "Notifications" -> "notifications";
            case "Scanning" -> "scanning";
            case "Exports" -> "exports";
            default -> "privacy";
        };
    }
}
