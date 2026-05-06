package easv.gui.controller.admin;

import java.util.ArrayList;
import java.util.List;

final class PaginationHelper {

    static final String ELLIPSIS = "...";

    private PaginationHelper() {
    }

    static PageSlice slice(int requestedPage, int rowsPerPage, int totalItems) {
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

    private static int totalPages(int totalItems, int rowsPerPage) {
        if (totalItems == 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalItems / rowsPerPage);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    record PageSlice(int currentPage, int totalPages, int fromIndex, int toIndex) {
    }
}
