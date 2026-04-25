package easv.gui.controller.Admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageUsersController {

    private static final String ALL_ROLES = "All Roles";
    private static final int DEFAULT_ROWS_PER_PAGE = 10;
    private static final List<Integer> ROWS_PER_PAGE_OPTIONS = List.of(10, 25, 50);

    private static final double NAME_COLUMN_WIDTH = 22;
    private static final double USERNAME_COLUMN_WIDTH = 17;
    private static final double EMAIL_COLUMN_WIDTH = 29;
    private static final double ROLE_COLUMN_WIDTH = 12;
    private static final double ACTIONS_COLUMN_WIDTH = 20;

    private static final String EDIT_ICON_PATH =
            "M4 17.25V20h2.75l8.12-8.12-2.75-2.75L4 17.25zm11.71-9.04a.996.996 0 0 0 0-1.41l-1.5-1.5a.996.996 0 1 0-1.41 1.41l1.5 1.5a.996.996 0 0 0 1.41 0z";

    private static final String DELETE_ICON_PATH =
            "M6 7h12l-1 13H7L6 7zm3-3h6l1 2h4v2H4V6h4l1-2z";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterComboBox;

    @FXML private Label usersCountLabel;
    @FXML private VBox userListContainer;
    @FXML private VBox emptyStateBox;

    @FXML private HBox paginationBar;
    @FXML private Label paginationSummaryLabel;
    @FXML private HBox paginationButtonsBox;
    @FXML private ComboBox<Integer> rowsPerPageComboBox;

    private final ObservableList<UserRow> masterUsers = FXCollections.observableArrayList();

    private FilteredList<UserRow> filteredUsers;
    private int currentPage = 1;
    private int rowsPerPage = DEFAULT_ROWS_PER_PAGE;

    @FXML
    private void initialize() {
        configureRoleFilter();
        configureRowsPerPageSelector();
        loadSampleUsers();
        configureFiltering();
        applyFilters();
    }

    private void configureRoleFilter() {
        roleFilterComboBox.getItems().setAll(ALL_ROLES, "Admin", "User");
        roleFilterComboBox.setValue(ALL_ROLES);
    }

    private void configureRowsPerPageSelector() {
        rowsPerPageComboBox.getItems().setAll(ROWS_PER_PAGE_OPTIONS);
        rowsPerPageComboBox.setValue(DEFAULT_ROWS_PER_PAGE);

        rowsPerPageComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue <= 0) {
                return;
            }

            rowsPerPage = newValue;
            currentPage = 1;
            renderUsers();
        });
    }

    private void configureFiltering() {
        filteredUsers = new FilteredList<>(masterUsers, user -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        roleFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        currentPage = 1;

        String searchText = normalize(searchField.getText());
        String selectedRole = roleFilterComboBox.getValue();

        filteredUsers.setPredicate(user ->
                matchesSelectedRole(user, selectedRole)
                        && matchesSearch(user, searchText)
        );

        renderUsers();
    }

    private boolean matchesSelectedRole(UserRow user, String selectedRole) {
        return selectedRole == null
                || ALL_ROLES.equals(selectedRole)
                || user.role().equalsIgnoreCase(selectedRole);
    }

    private boolean matchesSearch(UserRow user, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }

        return normalize(user.name()).contains(searchText)
                || normalize(user.username()).contains(searchText)
                || normalize(user.email()).contains(searchText)
                || normalize(user.role()).contains(searchText);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private void renderUsers() {
        List<UserRow> visibleUsers = filteredUsers.stream().toList();

        int totalUsers = visibleUsers.size();
        int totalPages = calculateTotalPages(totalUsers);

        currentPage = clamp(currentPage, 1, totalPages);

        int fromIndex = Math.min((currentPage - 1) * rowsPerPage, totalUsers);
        int toIndex = Math.min(fromIndex + rowsPerPage, totalUsers);

        List<UserRow> pageUsers = visibleUsers.subList(fromIndex, toIndex);

        userListContainer.getChildren().setAll(
                pageUsers.stream()
                        .map(this::buildUserRecord)
                        .toList()
        );

        updateEmptyState(totalUsers);

        usersCountLabel.setText(formatUserCount(totalUsers));
        renderPagination(totalPages, totalUsers, fromIndex, toIndex);
    }

    private int calculateTotalPages(int totalUsers) {
        if (totalUsers == 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalUsers / rowsPerPage);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private void updateEmptyState(int totalUsers) {
        boolean hasUsers = totalUsers > 0;

        userListContainer.setVisible(hasUsers);
        userListContainer.setManaged(hasUsers);

        emptyStateBox.setVisible(!hasUsers);
        emptyStateBox.setManaged(!hasUsers);

        paginationBar.setVisible(hasUsers);
        paginationBar.setManaged(hasUsers);
    }

    private GridPane buildUserRecord(UserRow user) {
        GridPane row = createRecordGrid();
        row.getStyleClass().add("user-record");
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(userListContainer.widthProperty());

        addCell(row, buildNameCell(user), 0, HPos.LEFT);
        addCell(row, createTableLabel(user.username(), "table-cell-text-muted"), 1, HPos.CENTER);
        addCell(row, createTableLabel(user.email(), "table-cell-text-muted"), 2, HPos.CENTER);
        addCell(row, buildRoleCell(user), 3, HPos.CENTER);
        addCell(row, buildActionsCell(user), 4, HPos.CENTER);

        return row;
    }

    private GridPane createRecordGrid() {
        GridPane grid = new GridPane();

        grid.getColumnConstraints().setAll(
                createPercentColumn(NAME_COLUMN_WIDTH),
                createPercentColumn(USERNAME_COLUMN_WIDTH),
                createPercentColumn(EMAIL_COLUMN_WIDTH),
                createPercentColumn(ROLE_COLUMN_WIDTH),
                createPercentColumn(ACTIONS_COLUMN_WIDTH)
        );

        return grid;
    }

    private ColumnConstraints createPercentColumn(double percentWidth) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percentWidth);
        column.setHgrow(Priority.ALWAYS);
        column.setFillWidth(true);
        return column;
    }

    private void addCell(GridPane row, Node content, int columnIndex, HPos horizontalAlignment) {
        GridPane.setHalignment(content, horizontalAlignment);
        GridPane.setValignment(content, VPos.CENTER);
        row.add(content, columnIndex, 0);
    }

    private HBox buildNameCell(UserRow user) {
        Label avatar = new Label(initialsFor(user.name()));
        avatar.getStyleClass().add("user-avatar-initials");

        Label nameLabel = createTableLabel(user.name(), "table-cell-text");

        HBox nameCell = new HBox(9, avatar, nameLabel);
        nameCell.getStyleClass().add("name-cell");
        nameCell.setAlignment(Pos.CENTER_LEFT);

        return nameCell;
    }

    private Label createTableLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private StackPane buildRoleCell(UserRow user) {
        Label roleBadge = new Label(user.role());
        roleBadge.getStyleClass().addAll(
                "role-badge",
                "Admin".equalsIgnoreCase(user.role())
                        ? "role-badge-admin"
                        : "role-badge-user"
        );

        StackPane wrapper = new StackPane(roleBadge);
        wrapper.getStyleClass().add("role-cell");
        wrapper.setAlignment(Pos.CENTER);

        return wrapper;
    }

    private HBox buildActionsCell(UserRow user) {
        HBox actionBox = new HBox(12);
        actionBox.getStyleClass().add("inline-actions");
        actionBox.setAlignment(Pos.CENTER);

        actionBox.getChildren().add(
                createInlineActionButton("Edit", EDIT_ICON_PATH, "edit-link-button", "edit-link-icon")
        );

        if (!user.currentUser()) {
            actionBox.getChildren().add(
                    createInlineActionButton("Delete", DELETE_ICON_PATH, "delete-link-button", "delete-link-icon")
            );
        }

        return actionBox;
    }

    private Button createInlineActionButton(String text, String iconPath, String buttonClass, String iconClass) {
        Button button = new Button(text);
        button.getStyleClass().add(buttonClass);
        button.setFocusTraversable(false);
        button.setGraphic(createActionIcon(iconPath, iconClass));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(6);
        return button;
    }

    private StackPane createActionIcon(String pathData, String iconStyleClass) {
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.getStyleClass().add(iconStyleClass);
        icon.setScaleX(0.68);
        icon.setScaleY(0.68);

        StackPane shell = new StackPane(icon);
        shell.getStyleClass().add("action-icon-shell");

        return shell;
    }

    private String initialsFor(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }

        String[] nameParts = fullName.trim().split("\\s+");

        if (nameParts.length == 1) {
            return nameParts[0]
                    .substring(0, Math.min(2, nameParts[0].length()))
                    .toUpperCase(Locale.ROOT);
        }

        return (nameParts[0].substring(0, 1) + nameParts[1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }

    private void renderPagination(int totalPages, int totalUsers, int fromIndex, int toIndex) {
        paginationButtonsBox.getChildren().clear();

        if (totalUsers == 0) {
            paginationSummaryLabel.setText("Showing 0 users");
            return;
        }

        paginationSummaryLabel.setText(formatPaginationSummary(fromIndex, toIndex, totalUsers));

        paginationButtonsBox.getChildren().add(createPaginationButton("<<", 1, currentPage == 1));
        paginationButtonsBox.getChildren().add(createPaginationButton("<", currentPage - 1, currentPage == 1));

        for (String pageItem : buildPageItems(totalPages)) {
            Node paginationItem = "...".equals(pageItem)
                    ? createPaginationEllipsis()
                    : createPaginationButton(pageItem, Integer.parseInt(pageItem), false);

            paginationButtonsBox.getChildren().add(paginationItem);
        }

        paginationButtonsBox.getChildren().add(createPaginationButton(">", currentPage + 1, currentPage == totalPages));
        paginationButtonsBox.getChildren().add(createPaginationButton(">>", totalPages, currentPage == totalPages));
    }

    private String formatPaginationSummary(int fromIndex, int toIndex, int totalUsers) {
        return "Showing " + (fromIndex + 1) + "-" + toIndex + " of " + totalUsers + " users";
    }

    private Label createPaginationEllipsis() {
        Label ellipsis = new Label("...");
        ellipsis.getStyleClass().add("pagination-ellipsis");
        return ellipsis;
    }

    private List<String> buildPageItems(int totalPages) {
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
            items.add("...");
            items.add(String.valueOf(totalPages));
            return items;
        }

        if (currentPage >= totalPages - 2) {
            items.add("1");
            items.add("...");
            items.add(String.valueOf(totalPages - 2));
            items.add(String.valueOf(totalPages - 1));
            items.add(String.valueOf(totalPages));
            return items;
        }

        items.add("1");
        items.add("...");
        items.add(String.valueOf(currentPage - 1));
        items.add(String.valueOf(currentPage));
        items.add(String.valueOf(currentPage + 1));
        items.add("...");
        items.add(String.valueOf(totalPages));

        return items;
    }

    private Button createPaginationButton(String text, int targetPage, boolean disabled) {
        Button button = new Button(text);
        button.getStyleClass().add("pagination-button");
        button.setFocusTraversable(false);
        button.setDisable(disabled);

        boolean isCurrentPageButton = text.equals(String.valueOf(currentPage));

        if (isCurrentPageButton) {
            button.getStyleClass().add("pagination-button-active");
            return button;
        }

        if (!disabled) {
            button.setOnAction(event -> {
                currentPage = targetPage;
                renderUsers();
            });
        }

        return button;
    }

    private String formatUserCount(int count) {
        return count == 1 ? "1 user" : count + " users";
    }

    private void loadSampleUsers() {
        masterUsers.setAll(
                new UserRow("John Doe", "john", "john@example.com", "Admin", true),
                new UserRow("Sarah Smith", "sarah", "sarah@example.com", "User", false),
                new UserRow("Michael Johnson", "michael", "michael@example.com", "User", false),
                new UserRow("Emily Davis", "emily", "emily@example.com", "User", false),
                new UserRow("David Wilson", "david", "david@example.com", "User", false),
                new UserRow("Olivia Brown", "olivia", "olivia@example.com", "User", false),
                new UserRow("Lucas Andersen", "lucas", "lucas@example.com", "Admin", false),
                new UserRow("Sofia Nielsen", "sofia", "sofia@example.com", "User", false)
        );
    }

    private record UserRow(
            String name,
            String username,
            String email,
            String role,
            boolean currentUser
    ) {
    }
}