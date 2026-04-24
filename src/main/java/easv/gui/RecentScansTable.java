package easv.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class RecentScansTable extends VBox {
    private final TableView<UserPortalModel.RecentScanItem> tableView = new TableView<>();

    public RecentScansTable(List<UserPortalModel.RecentScanItem> items, Consumer<UserPortalModel.RecentScanItem> onExport,
                            Consumer<UserPortalModel.RecentScanItem> onSelected, Runnable onViewAll) {
        getStyleClass().addAll("dashboard-card", "recent-scans-card");
        setSpacing(12);

        Label title = new Label("Recent Scans");
        title.getStyleClass().add("section-title");

        tableView.getStyleClass().add("recent-scans-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setItems(FXCollections.observableArrayList(items));

        TableColumn<UserPortalModel.RecentScanItem, String> boxColumn = createTextColumn("Box ID", UserPortalModel.RecentScanItem::boxId);
        TableColumn<UserPortalModel.RecentScanItem, String> profileColumn = createTextColumn("Profile", UserPortalModel.RecentScanItem::profileName);
        TableColumn<UserPortalModel.RecentScanItem, UserPortalModel.RecentScanItem> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(UserPortalModel.RecentScanItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(new StatusBadge(item.status()));
                }
                setText(null);
            }
        });
        TableColumn<UserPortalModel.RecentScanItem, String> startedColumn = createTextColumn("Started", UserPortalModel.RecentScanItem::startedAt);
        TableColumn<UserPortalModel.RecentScanItem, Number> pagesColumn = new TableColumn<>("Pages");
        pagesColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().pages()));
        TableColumn<UserPortalModel.RecentScanItem, String> exportColumn = new TableColumn<>("Export");
        exportColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper("DL"));
        exportColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(item);
                    label.getStyleClass().add("table-export-icon");
                    label.setOnMouseClicked(event -> onExport.accept(getTableRow().getItem()));
                    VBox wrapper = new VBox(label);
                    wrapper.setAlignment(Pos.CENTER);
                    setGraphic(wrapper);
                }
                setText(null);
            }
        });

        tableView.getColumns().add(boxColumn);
        tableView.getColumns().add(profileColumn);
        tableView.getColumns().add(statusColumn);
        tableView.getColumns().add(startedColumn);
        tableView.getColumns().add(pagesColumn);
        tableView.getColumns().add(exportColumn);
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                onSelected.accept(newValue);
            }
        });
        VBox.setVgrow(tableView, Priority.ALWAYS);

        Label footerLink = new Label("View all scans ->");
        footerLink.getStyleClass().add("dashboard-link");
        footerLink.setOnMouseClicked(event -> onViewAll.run());

        getChildren().addAll(title, tableView, footerLink);
    }

    private TableColumn<UserPortalModel.RecentScanItem, String> createTextColumn(String title, java.util.function.Function<UserPortalModel.RecentScanItem, String> mapper) {
        TableColumn<UserPortalModel.RecentScanItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(mapper.apply(data.getValue())));
        return column;
    }
}
