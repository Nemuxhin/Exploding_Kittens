package easv.gui.controller.util;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reusable month-grid date picker shared by the activity-log, review, and
 * assigned-QA date-range filters.
 *
 * <p>The widget owns the popover chrome (month header with prev/next nav and a
 * 6×7 day grid) plus month navigation and rendering. Each host keeps its own
 * filter-state model and supplies behaviour through the constructor:
 * <ul>
 *   <li>{@code isSelectedBoundary} — is this date a selected range endpoint?</li>
 *   <li>{@code isInRange} — does this date fall strictly inside the range?</li>
 *   <li>{@code onDaySelected} — invoked when a day cell is clicked.</li>
 * </ul>
 *
 * <p>All style classes are derived from {@code stylePrefix} so each screen keeps
 * its own CSS (e.g. {@code "review"} → {@code review-calendar-day-button}).
 */
public final class DateCalendarView {

    private static final String[] DAY_NAMES = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
    private static final int VISIBLE_CELLS = 42;

    private final String stylePrefix;
    private final Predicate<LocalDate> isSelectedBoundary;
    private final Predicate<LocalDate> isInRange;
    private final Consumer<LocalDate> onDaySelected;

    private Label monthLabel;
    private GridPane grid;
    private YearMonth displayedMonth = YearMonth.now();

    public DateCalendarView(String stylePrefix,
                            Predicate<LocalDate> isSelectedBoundary,
                            Predicate<LocalDate> isInRange,
                            Consumer<LocalDate> onDaySelected) {
        this.stylePrefix = stylePrefix;
        this.isSelectedBoundary = isSelectedBoundary;
        this.isInRange = isInRange;
        this.onDaySelected = onDaySelected;
    }

    public void setDisplayedMonth(YearMonth month) {
        if (month != null) {
            displayedMonth = month;
        }
    }

    /** Builds a fresh popover; replaces the live month label and grid nodes. */
    public VBox buildPopover() {
        VBox popover = new VBox(0);
        popover.getStyleClass().add(stylePrefix + "-date-popover");

        VBox panel = new VBox(0);
        panel.getStyleClass().add(stylePrefix + "-calendar-panel");

        HBox header = new HBox();
        header.getStyleClass().add(stylePrefix + "-calendar-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);

        Button previousButton = new Button("<");
        previousButton.setFocusTraversable(false);
        previousButton.getStyleClass().add(stylePrefix + "-calendar-nav-button");
        previousButton.setOnAction(event -> showPreviousMonth());

        Button nextButton = new Button(">");
        nextButton.setFocusTraversable(false);
        nextButton.getStyleClass().add(stylePrefix + "-calendar-nav-button");
        nextButton.setOnAction(event -> showNextMonth());

        monthLabel = new Label();
        monthLabel.getStyleClass().add(stylePrefix + "-calendar-month-label");

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        header.getChildren().addAll(previousButton, leftSpacer, monthLabel, rightSpacer, nextButton);

        grid = new GridPane();
        grid.setHgap(3);
        grid.setVgap(6);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.getStyleClass().add(stylePrefix + "-calendar-grid");

        panel.getChildren().addAll(header, grid);
        popover.getChildren().add(panel);

        render();
        return popover;
    }

    /** Re-renders the month label and day grid. No-op until a popover is built. */
    public void render() {
        if (grid == null || monthLabel == null || displayedMonth == null) {
            return;
        }

        grid.getChildren().clear();
        monthLabel.setText(displayedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + displayedMonth.getYear());

        for (int column = 0; column < DAY_NAMES.length; column++) {
            Label label = new Label(DAY_NAMES[column]);
            label.getStyleClass().add(stylePrefix + "-calendar-day-name");
            grid.add(label, column, 0);
        }

        LocalDate firstOfMonth = displayedMonth.atDay(1);
        int leadingDays = firstOfMonth.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        LocalDate firstVisibleDate = firstOfMonth.minusDays(leadingDays);

        for (int index = 0; index < VISIBLE_CELLS; index++) {
            LocalDate date = firstVisibleDate.plusDays(index);
            grid.add(buildDayButton(date), index % 7, index / 7 + 1);
        }
    }

    private void showPreviousMonth() {
        displayedMonth = displayedMonth.minusMonths(1);
        render();
    }

    private void showNextMonth() {
        displayedMonth = displayedMonth.plusMonths(1);
        render();
    }

    private Button buildDayButton(LocalDate date) {
        Button dayButton = new Button(String.valueOf(date.getDayOfMonth()));
        dayButton.getStyleClass().add(stylePrefix + "-calendar-day-button");
        dayButton.setFocusTraversable(false);
        dayButton.setMinSize(30, 30);
        dayButton.setPrefSize(30, 30);
        dayButton.setMaxSize(30, 30);

        boolean selectedBoundary = isSelectedBoundary.test(date);

        if (!YearMonth.from(date).equals(displayedMonth)) {
            dayButton.getStyleClass().add(stylePrefix + "-calendar-day-outside");
        }

        if (date.equals(LocalDate.now()) && !selectedBoundary) {
            dayButton.getStyleClass().add(stylePrefix + "-calendar-day-today");
        }

        if (selectedBoundary) {
            dayButton.getStyleClass().add(stylePrefix + "-calendar-day-selected");
        } else if (isInRange.test(date)) {
            dayButton.getStyleClass().add(stylePrefix + "-calendar-day-in-range");
        }

        dayButton.setOnAction(event -> onDaySelected.accept(date));
        return dayButton;
    }
}
