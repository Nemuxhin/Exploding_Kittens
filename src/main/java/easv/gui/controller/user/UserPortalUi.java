package easv.gui.controller.user;

import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

import easv.gui.StyleGuideUi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

final class UserPortalUi {
    private static final DateTimeFormatter FILTER_DATE = StyleGuideUi.DATE_FORMATTER;

    private UserPortalUi() {
    }

    static Label buildIcon(String key, String styleClass) {
        return StyleGuideUi.createPrimeIcon(iconGlyph(key), styleClass);
    }

    static Label buildStatusChip(String status) {
        String labelText = status == null || status.isBlank() ? "Unknown" : status;
        Label label = new Label(labelText);
        label.getStyleClass().add("status-chip");
        String normalized = labelText.trim().toLowerCase().replace(' ', '-');
        label.getStyleClass().add("status-" + normalized);
        return label;
    }

    static void configureDateFilterPicker(DatePicker picker) {
        picker.setPromptText("MM/DD/YYYY");
        picker.setEditable(true);
        picker.getStyleClass().add("exports-date-picker");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return value == null ? "" : FILTER_DATE.format(value);
            }

            @Override
            public LocalDate fromString(String value) {
                if (value == null || value.isBlank()) {
                    return null;
                }

                try {
                    return LocalDate.parse(value.trim(), FILTER_DATE);
                } catch (DateTimeParseException ignored) {
                    return null;
                }
            }
        });
        picker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                picker.getEditor().clear();
            } else {
                picker.getEditor().setText(FILTER_DATE.format(newValue));
            }
        });
    }

    private static String iconGlyph(String key) {
        return switch (key) {
            case "dashboard" -> "\ue925";
            case "scan", "scanning" -> "\ue934";
            case "scans", "file", "logo" -> "\ue958";
            case "exports", "download" -> "\ue956";
            case "settings" -> "\ue94a";
            case "help" -> "\ue959";
            case "clock" -> "\ue940";
            case "account", "user" -> "\ue939";
            case "shortcuts" -> "\ue981";
            case "notifications" -> "\ue97c";
            case "privacy" -> "\ue981";
            case "save" -> "\ue95c";
            case "theme" -> "\ue9c7";
            case "selected-check" -> "\ue90a";
            case "trash" -> "\ue93d";
            default -> "\ue958";
        };
    }
}
