package easv.gui.controller.utilities;

import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class AppDates {
    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);

    private AppDates() {
    }

    public static String format(LocalDate date) {
        return date == null ? "" : FORMATTER.format(date);
    }

    public static LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim(), FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public static void configure(DatePicker picker) {
        if (picker == null) {
            return;
        }

        picker.setPromptText("MM/DD/YYYY");
        picker.setEditable(true);
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return format(value);
            }

            @Override
            public LocalDate fromString(String value) {
                return parse(value);
            }
        });
    }

    public static void preventFutureDates(DatePicker picker) {
        if (picker == null) {
            return;
        }

        picker.setDayCellFactory(datePicker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                getStyleClass().remove("activity-log-date-disabled");

                if (!empty && date != null && date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    getStyleClass().add("activity-log-date-disabled");
                }
            }
        });
    }
}
