package easv.gui.controller.utilities;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import java.util.List;
import java.util.Locale;

public final class SearchableComboBoxes {
    private static final String CONFIGURED_KEY = "weblager.searchableConfigured";
    private static final String UPDATING_KEY = "weblager.searchableUpdating";

    private SearchableComboBoxes() {
    }

    public static void configure(ComboBox<String> comboBox) {
        if (comboBox == null || comboBox.getItems().size() < 5) {
            return;
        }

        if (Boolean.TRUE.equals(comboBox.getProperties().get(CONFIGURED_KEY))) {
            return;
        }

        ObservableList<String> sourceItems = FXCollections.observableArrayList(comboBox.getItems());
        comboBox.getProperties().put(CONFIGURED_KEY, true);
        comboBox.getStyleClass().add("searchable-combo-box");
        comboBox.setVisibleRowCount(Math.min(8, sourceItems.size()));
        comboBox.setEditable(true);
        comboBox.getEditor().setPromptText("Search...");

        comboBox.showingProperty().addListener((observable, wasShowing, isShowing) -> {
            if (isShowing) {
                refreshSourceItems(comboBox, sourceItems);
                return;
            }

            restore(comboBox, sourceItems);
        });

        comboBox.getEditor().textProperty().addListener((observable, oldText, newText) -> {
            if (!comboBox.isShowing()) {
                return;
            }

            if (Boolean.TRUE.equals(comboBox.getProperties().get(UPDATING_KEY))) {
                return;
            }

            filter(comboBox, sourceItems, newText);
        });
    }

    private static void refreshSourceItems(ComboBox<String> comboBox, ObservableList<String> sourceItems) {
        List<String> currentItems = comboBox.getItems();

        if (!currentItems.isEmpty() && !currentItems.equals(sourceItems)) {
            sourceItems.setAll(currentItems);
        }
    }

    private static void restore(ComboBox<String> comboBox, ObservableList<String> sourceItems) {
        comboBox.getProperties().put(UPDATING_KEY, true);

        String selectedValue = comboBox.getValue();
        String editorText = comboBox.getEditor().getText();

        comboBox.getItems().setAll(sourceItems);

        if (selectedValue != null && sourceItems.contains(selectedValue)) {
            comboBox.setValue(selectedValue);
        } else if (editorText != null && sourceItems.contains(editorText)) {
            comboBox.setValue(editorText);
        } else if (selectedValue == null) {
            comboBox.getEditor().clear();
        } else {
            comboBox.setValue(null);
            comboBox.getEditor().clear();
        }

        comboBox.getProperties().put(UPDATING_KEY, false);
    }

    private static void filter(ComboBox<String> comboBox,
                               ObservableList<String> sourceItems,
                               String searchText) {
        String typedText = searchText == null ? "" : searchText;
        String normalizedSearch = typedText.trim().toLowerCase(Locale.ROOT);

        List<String> filteredItems = normalizedSearch.isBlank()
                ? List.copyOf(sourceItems)
                : sourceItems.stream()
                .filter(item -> item != null && item.toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .toList();

        comboBox.getProperties().put(UPDATING_KEY, true);
        comboBox.getItems().setAll(filteredItems.isEmpty() ? sourceItems : filteredItems);
        comboBox.getEditor().setText(typedText);
        comboBox.getEditor().positionCaret(typedText.length());
        comboBox.getProperties().put(UPDATING_KEY, false);
    }
}
