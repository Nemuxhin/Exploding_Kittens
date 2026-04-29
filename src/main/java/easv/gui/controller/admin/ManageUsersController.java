package easv.gui.controller.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.List;
import java.util.Locale;

public class ManageUsersController {

    private static final String ALL_ROLES = "All Roles";
    private static final String ROLE_ADMIN = "Admin";
    private static final String ROLE_USER = "User";
    private static final String STATUS_ACTIVE = "Active";

    private static final int DEFAULT_ROWS_PER_PAGE = 10;
    private static final List<Integer> ROWS_PER_PAGE_OPTIONS = List.of(10, 25, 50);

    private static final double NAME_COLUMN_WIDTH = 20;
    private static final double USERNAME_COLUMN_WIDTH = 15;
    private static final double EMAIL_COLUMN_WIDTH = 25;
    private static final double ROLE_COLUMN_WIDTH = 10;
    private static final double PROFILES_COLUMN_WIDTH = 15;
    private static final double ACTIONS_COLUMN_WIDTH = 15;

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

    @FXML
    private void openCreateUserDialog() {
        CreateUserDialog dialog = new CreateUserDialog(userListContainer, this::usernameAlreadyExists);

        dialog.showAndWait().ifPresent(createdUser -> {
            masterUsers.add(createdUser);
            applyFilters();
            showCreateUserSuccess(createdUser);
        });
    }

    private void configureRoleFilter() {
        roleFilterComboBox.getItems().setAll(ALL_ROLES, ROLE_ADMIN, ROLE_USER);
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
                || normalize(user.role()).contains(searchText)
                || normalize(user.status()).contains(searchText)
                || normalize(user.searchableProfileText()).contains(searchText);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private void renderUsers() {
        List<UserRow> visibleUsers = filteredUsers.stream().toList();

        int totalUsers = visibleUsers.size();
        PaginationHelper.PageSlice pageSlice = PaginationHelper.slice(currentPage, rowsPerPage, totalUsers);

        currentPage = pageSlice.currentPage();

        List<UserRow> pageUsers = visibleUsers.subList(pageSlice.fromIndex(), pageSlice.toIndex());

        userListContainer.getChildren().setAll(
                pageUsers.stream()
                        .map(this::buildUserRecord)
                        .toList()
        );

        updateEmptyState(totalUsers);

        usersCountLabel.setText(formatUserCount(totalUsers));
        renderPagination(pageSlice, totalUsers);
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
        addCell(row, buildCenteredTextCell(user.username()), 1, HPos.CENTER);
        addCell(row, buildCenteredTextCell(user.email()), 2, HPos.CENTER);
        addCell(row, buildRoleCell(user), 3, HPos.CENTER);
        addCell(row, buildCenteredTextCell(user.profileAccessSummary()), 4, HPos.CENTER);
        addCell(row, buildActionsCell(user), 5, HPos.CENTER);

        return row;
    }

    private GridPane createRecordGrid() {
        GridPane grid = new GridPane();

        grid.getColumnConstraints().setAll(
                createPercentColumn(NAME_COLUMN_WIDTH),
                createPercentColumn(USERNAME_COLUMN_WIDTH),
                createPercentColumn(EMAIL_COLUMN_WIDTH),
                createPercentColumn(ROLE_COLUMN_WIDTH),
                createPercentColumn(PROFILES_COLUMN_WIDTH),
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

        Label nameLabel = createLeftTableLabel(user.name(), "table-cell-text");

        HBox nameCell = new HBox(9, avatar, nameLabel);
        nameCell.getStyleClass().add("name-cell");
        nameCell.setAlignment(Pos.CENTER_LEFT);

        return nameCell;
    }

    private Label createLeftTableLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setAlignment(Pos.CENTER_LEFT);
        return label;
    }

    private StackPane buildCenteredTextCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-cell-text-muted");
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setAlignment(Pos.CENTER);

        StackPane wrapper = new StackPane(label);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        return wrapper;
    }

    private StackPane buildRoleCell(UserRow user) {
        Label roleBadge = new Label(user.role());
        roleBadge.getStyleClass().addAll(
                "role-badge",
                ROLE_ADMIN.equalsIgnoreCase(user.role())
                        ? "role-badge-admin"
                        : "role-badge-user"
        );

        StackPane wrapper = new StackPane(roleBadge);
        wrapper.getStyleClass().add("role-cell");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(Double.MAX_VALUE);

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

    private void renderPagination(PaginationHelper.PageSlice pageSlice, int totalUsers) {
        paginationButtonsBox.getChildren().clear();

        if (totalUsers == 0) {
            paginationSummaryLabel.setText("Showing 0 users");
            return;
        }

        paginationSummaryLabel.setText(formatPaginationSummary(
                pageSlice.fromIndex(),
                pageSlice.toIndex(),
                totalUsers
        ));

        paginationButtonsBox.getChildren().add(createPaginationButton("<<", 1, currentPage == 1));
        paginationButtonsBox.getChildren().add(createPaginationButton("<", currentPage - 1, currentPage == 1));

        for (String pageItem : PaginationHelper.buildPageItems(currentPage, pageSlice.totalPages())) {
            Node paginationItem = PaginationHelper.ELLIPSIS.equals(pageItem)
                    ? createPaginationEllipsis()
                    : createPaginationButton(pageItem, Integer.parseInt(pageItem), false);

            paginationButtonsBox.getChildren().add(paginationItem);
        }

        paginationButtonsBox.getChildren().add(createPaginationButton(
                ">",
                currentPage + 1,
                currentPage == pageSlice.totalPages()
        ));
        paginationButtonsBox.getChildren().add(createPaginationButton(
                ">>",
                pageSlice.totalPages(),
                currentPage == pageSlice.totalPages()
        ));
    }

    private String formatPaginationSummary(int fromIndex, int toIndex, int totalUsers) {
        return "Showing " + (fromIndex + 1) + "-" + toIndex + " of " + totalUsers + " users";
    }

    private Label createPaginationEllipsis() {
        Label ellipsis = new Label("...");
        ellipsis.getStyleClass().add("pagination-ellipsis");
        return ellipsis;
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

    private boolean usernameAlreadyExists(String username) {
        String normalizedUsername = normalize(username);

        return masterUsers.stream()
                .map(UserRow::username)
                .map(this::normalize)
                .anyMatch(existingUsername -> existingUsername.equals(normalizedUsername));
    }

    private void showCreateUserSuccess(UserRow user) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User created");
        alert.setHeaderText("User created successfully.");
        alert.setContentText(
                user.name() + " can now log in with username \"" + user.username() + "\".\n"
                        + "Assigned profiles: " + user.successProfileText() + "."
        );

        if (userListContainer.getScene() != null) {
            alert.initOwner(userListContainer.getScene().getWindow());
        }

        applyDialogStyles(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyDialogStyles(DialogPane dialogPane) {
        AdminDialogStyler.apply(dialogPane, userListContainer);
    }

    private void loadSampleUsers() {
        masterUsers.setAll(AdminDemoData.users());
    }

    record UserRow(
            String name,
            String username,
            String email,
            String role,
            String status,
            List<String> assignedProfiles,
            boolean currentUser
    ) {
        UserRow {
            assignedProfiles = assignedProfiles == null
                    ? List.of()
                    : List.copyOf(assignedProfiles);
        }

        private String profileAccessSummary() {
            if (ROLE_ADMIN.equalsIgnoreCase(role)) {
                return "All profiles";
            }

            int profileCount = assignedProfiles.size();

            if (profileCount == 0) {
                return "0 profiles assigned";
            }

            if (profileCount == 1) {
                return "1 profile assigned";
            }

            return profileCount + " profiles assigned";
        }

        private String searchableProfileText() {
            return profileAccessSummary() + " " + String.join(" ", assignedProfiles);
        }

        private String successProfileText() {
            if (ROLE_ADMIN.equalsIgnoreCase(role)) {
                return "All profiles";
            }

            if (assignedProfiles.isEmpty()) {
                return "0 profiles assigned";
            }

            return String.join(", ", assignedProfiles);
        }
    }
}
