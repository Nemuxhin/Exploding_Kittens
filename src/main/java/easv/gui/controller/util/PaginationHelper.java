package easv.gui.controller.util;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class PaginationHelper {

    static final String ELLIPSIS = "...";

    private PaginationHelper() {
    }

    public static PageSlice slice(int requestedPage, int rowsPerPage, int totalItems) {
        int totalPages = totalPages(totalItems, rowsPerPage);
        int currentPage = clamp(requestedPage, 1, totalPages);

        int fromIndex = Math.min((currentPage - 1) * rowsPerPage, totalItems);
        int toIndex = Math.min(fromIndex + rowsPerPage, totalItems);

        return new PageSlice(currentPage, totalPages, fromIndex, toIndex);
    }

    static List<String> buildPageItems(int currentPage, int totalPages) {
        List<String> items = new ArrayList<>();

        if (totalPages <= 5) {
            for (int page = 1; page <= totalPages; page++) {
                items.add(String.valueOf(page));
            }

            return items;
        }

        if (currentPage <= 3) {
            items.add("1");
            items.add("2");
            items.add("3");
            items.add(ELLIPSIS);
            items.add(String.valueOf(totalPages));
            return items;
        }

        if (currentPage >= totalPages - 2) {
            items.add("1");
            items.add(ELLIPSIS);
            items.add(String.valueOf(totalPages - 2));
            items.add(String.valueOf(totalPages - 1));
            items.add(String.valueOf(totalPages));
            return items;
        }

        items.add("1");
        items.add(ELLIPSIS);
        items.add(String.valueOf(currentPage - 1));
        items.add(String.valueOf(currentPage));
        items.add(String.valueOf(currentPage + 1));
        items.add(ELLIPSIS);
        items.add(String.valueOf(totalPages));

        return items;
    }

    /**
     * Renders the full pagination bar (summary text + navigation buttons) into the
     * supplied nodes. Shared by every paged screen so the rendering lives in one place.
     *
     * @param itemNoun      plural noun for the summary line, e.g. "records" or "users"
     * @param onPageSelected callback invoked with the target page when a button is pressed
     */
    public static void renderInto(HBox buttonsBox, Label summaryLabel, PageSlice slice,
                           int totalItems, String itemNoun, IntConsumer onPageSelected) {
        buttonsBox.getChildren().clear();

        if (totalItems == 0) {
            summaryLabel.setText("Showing 0 " + itemNoun);
            return;
        }

        summaryLabel.setText("Showing " + (slice.fromIndex() + 1) + "-" + slice.toIndex()
                + " of " + totalItems + " " + itemNoun);

        int currentPage = slice.currentPage();
        int totalPages = slice.totalPages();

        buttonsBox.getChildren().add(pageButton("<<", 1, currentPage == 1, currentPage, onPageSelected));
        buttonsBox.getChildren().add(pageButton("<", currentPage - 1, currentPage == 1, currentPage, onPageSelected));

        for (String pageItem : buildPageItems(currentPage, totalPages)) {
            buttonsBox.getChildren().add(ELLIPSIS.equals(pageItem)
                    ? ellipsisLabel()
                    : pageButton(pageItem, Integer.parseInt(pageItem), false, currentPage, onPageSelected));
        }

        buttonsBox.getChildren().add(pageButton(">", currentPage + 1, currentPage == totalPages, currentPage, onPageSelected));
        buttonsBox.getChildren().add(pageButton(">>", totalPages, currentPage == totalPages, currentPage, onPageSelected));
    }

    private static Button pageButton(String text, int targetPage, boolean disabled,
                                     int currentPage, IntConsumer onPageSelected) {
        Button button = new Button(text);
        button.getStyleClass().add("pagination-button");
        button.setFocusTraversable(false);
        button.setDisable(disabled);

        if (text.equals(String.valueOf(currentPage))) {
            button.getStyleClass().add("pagination-button-active");
            return button;
        }

        if (!disabled) {
            button.setOnAction(event -> onPageSelected.accept(targetPage));
        }

        return button;
    }

    private static Label ellipsisLabel() {
        Label ellipsis = new Label(ELLIPSIS);
        ellipsis.getStyleClass().add("pagination-ellipsis");
        return ellipsis;
    }

    private static int totalPages(int totalItems, int rowsPerPage) {
        if (totalItems == 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalItems / rowsPerPage);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public record PageSlice(int currentPage, int totalPages, int fromIndex, int toIndex) {
    }
}
