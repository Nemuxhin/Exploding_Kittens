package easv.gui.controller.user;

import easv.gui.UserPortalModel;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AssignedQaController {

    private static final double PREVIEW_PAGE_WIDTH = 500;
    private static final double PREVIEW_PAGE_HEIGHT = 560;

    @FXML private ScrollPane assignedQaListView;
    @FXML private BorderPane qaReviewWorkspaceView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> profileFilterComboBox;
    @FXML private VBox qaCardListContainer;

    @FXML private Label qaBoxIdLabel;
    @FXML private Label qaProfileLabel;
    @FXML private Label qaProgressLabel;
    @FXML private Label reviewStatusBadge;
    @FXML private VBox qaDocumentTreeContainer;
    @FXML private Label selectedQaPageTitleLabel;
    @FXML private Label selectedQaPageSubtitleLabel;
    @FXML private Label zoomPercentLabel;
    @FXML private StackPane qaPreviewHost;
    @FXML private Label qaTrayCountLabel;
    @FXML private HBox qaPageTrayContainer;
    @FXML private Label currentPageStatusLabel;
    @FXML private CheckBox pageReadableCheckBox;
    @FXML private CheckBox rotationCorrectCheckBox;
    @FXML private CheckBox splitCorrectCheckBox;
    @FXML private CheckBox pageCountCorrectCheckBox;
    @FXML private TextArea qaCommentTextArea;

    private final List<QaQueueItem> allItems = new ArrayList<>();
    private UserPortalModel portalModel = new UserPortalModel();
    private QaQueueItem currentItem;
    private QaPage currentPage;
    private double zoomMultiplier = 1.0;
    private boolean syncingPageControls;

    @FXML
    private void initialize() {
        if (statusFilterComboBox != null) {
            statusFilterComboBox.getItems().setAll("All Statuses", "In Review", "Needs Fix", "Ready");
            statusFilterComboBox.getSelectionModel().selectFirst();
            statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderAssignedQaCards());
        }

        if (profileFilterComboBox != null) {
            profileFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderAssignedQaCards());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> renderAssignedQaCards());
        }

        configurePageEditors();
        rebuildMockItems();
        populateProfileFilter();
        renderAssignedQaCards();
        showAssignedQaList();
    }

    public void setPortalModel(UserPortalModel portalModel) {
        this.portalModel = portalModel == null ? new UserPortalModel() : portalModel;
        rebuildMockItems();
        populateProfileFilter();
        renderAssignedQaCards();
    }

    public void refresh() {
        renderAssignedQaCards();
        if (currentItem != null) {
            renderReviewWorkspace();
        }
    }

    private void configurePageEditors() {
        pageReadableCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!syncingPageControls && currentPage != null) {
                currentPage.readable = newValue;
            }
        });
        rotationCorrectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!syncingPageControls && currentPage != null) {
                currentPage.rotationCorrect = newValue;
            }
        });
        splitCorrectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!syncingPageControls && currentPage != null) {
                currentPage.splitCorrect = newValue;
            }
        });
        pageCountCorrectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!syncingPageControls && currentPage != null) {
                currentPage.pageCountCorrect = newValue;
            }
        });
        qaCommentTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!syncingPageControls && currentPage != null) {
                currentPage.comment = newValue == null ? "" : newValue;
            }
        });
    }

    private void populateProfileFilter() {
        List<String> profiles = new ArrayList<>();
        profiles.add("All Profiles");
        for (UserPortalModel.ProfileItem profile : portalModel.fetchProfilesForUser()) {
            profiles.add(profile.name());
        }

        profileFilterComboBox.getItems().setAll(profiles);
        profileFilterComboBox.getSelectionModel().selectFirst();
    }

    private void rebuildMockItems() {
        allItems.clear();
        List<String> profileNames = portalModel.fetchProfilesForUser().stream().map(UserPortalModel.ProfileItem::name).toList();
        String firstProfile = profileNames.isEmpty() ? "Standard Scan" : profileNames.get(0);
        String secondProfile = profileNames.size() > 1 ? profileNames.get(1) : firstProfile;
        String thirdProfile = profileNames.size() > 2 ? profileNames.get(2) : firstProfile;

        QaQueueItem inReview = new QaQueueItem("BOX-2026-042", firstProfile, "In Review", createPages(1, 3, 2, false));
        if (!inReview.pages.isEmpty()) {
            inReview.pages.get(0).status = "Approved";
        }

        QaQueueItem needsFix = new QaQueueItem("BOX-2026-041", secondProfile, "Needs Fix", createPages(1, 2, 1, true));
        QaQueueItem ready = new QaQueueItem("BOX-2026-039", thirdProfile, "Ready", createPages(1, 4, 2, false));
        for (QaPage page : ready.pages) {
            page.status = "Approved";
        }

        allItems.add(inReview);
        allItems.add(needsFix);
        allItems.add(ready);
    }

    private List<QaPage> createPages(int firstDocument, int pageCount, int documentCount, boolean seedNeedsFix) {
        List<QaPage> pages = new ArrayList<>();
        int referenceCounter = 1;

        for (int document = 0; document < documentCount; document++) {
            for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
                QaPage page = new QaPage(referenceCounter++, firstDocument + document, pageIndex);
                if (seedNeedsFix && pageIndex == 1 && document == 0) {
                    page.status = "Needs Fix";
                    page.comment = "Rotation needs correction.";
                }
                pages.add(page);
            }
        }

        return pages;
    }

    private void renderAssignedQaCards() {
        qaCardListContainer.getChildren().clear();

        List<QaQueueItem> filteredItems = filteredItems();
        if (filteredItems.isEmpty()) {
            Label emptyLabel = new Label("No assigned QA work matches the current filters.");
            emptyLabel.getStyleClass().add("assigned-qa-card-copy");
            qaCardListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (QaQueueItem item : filteredItems) {
            qaCardListContainer.getChildren().add(createQaCard(item));
        }
    }

    private List<QaQueueItem> filteredItems() {
        String searchTerm = normalize(searchField.getText());
        String selectedStatus = statusFilterComboBox.getValue();
        String selectedProfile = profileFilterComboBox.getValue();

        List<QaQueueItem> filtered = new ArrayList<>();
        for (QaQueueItem item : allItems) {
            boolean matchesSearch = searchTerm.isBlank()
                    || normalize(item.boxId).contains(searchTerm)
                    || normalize(item.profileName).contains(searchTerm);
            boolean matchesStatus = selectedStatus == null
                    || "All Statuses".equals(selectedStatus)
                    || item.status.equalsIgnoreCase(selectedStatus);
            boolean matchesProfile = selectedProfile == null
                    || "All Profiles".equals(selectedProfile)
                    || item.profileName.equalsIgnoreCase(selectedProfile);

            if (matchesSearch && matchesStatus && matchesProfile) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private VBox createQaCard(QaQueueItem item) {
        Label title = new Label(item.boxId);
        title.getStyleClass().add("assigned-qa-card-title");

        Label meta = new Label(item.profileName + " · " + item.pages.size() + " pages");
        meta.getStyleClass().add("assigned-qa-card-meta");

        Label copy = new Label(item.reviewedPageCount() + " / " + item.pages.size() + " pages reviewed");
        copy.getStyleClass().add("assigned-qa-card-copy");

        Label statusChip = new Label(item.status);
        statusChip.getStyleClass().addAll("assigned-qa-status-chip", statusClassFor(item.status));

        Button openButton = new Button(item.hasRemainingPages() ? "Continue QA" : "Open");
        openButton.getStyleClass().add(item.hasRemainingPages() ? "qa-approve-button" : "qa-neutral-button");
        openButton.setOnAction(event -> openItem(item));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(12, new VBox(4, title, meta), spacer, statusChip);
        topRow.setMinWidth(0);

        HBox actionRow = new HBox(12, spacerRegion(), openButton);
        actionRow.setMinWidth(0);
        HBox.setHgrow(actionRow.getChildren().get(0), Priority.ALWAYS);

        VBox card = new VBox(14, topRow, copy, actionRow);
        card.getStyleClass().add("assigned-qa-card");
        return card;
    }

    private Region spacerRegion() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void openItem(QaQueueItem item) {
        currentItem = item;
        currentPage = item.firstSelectablePage();
        zoomMultiplier = 1.0;
        renderReviewWorkspace();
        showReviewWorkspace();
    }

    @FXML
    private void onBackToAssignedQaList() {
        showAssignedQaList();
        renderAssignedQaCards();
    }

    @FXML
    private void onZoomOut() {
        zoomMultiplier = Math.max(0.5, zoomMultiplier - 0.1);
        renderReviewPreview();
    }

    @FXML
    private void onZoomIn() {
        zoomMultiplier = Math.min(2.5, zoomMultiplier + 0.1);
        renderReviewPreview();
    }

    @FXML
    private void onResetView() {
        zoomMultiplier = 1.0;
        renderReviewPreview();
    }

    @FXML
    private void onPreviousPage() {
        selectRelativePage(-1);
    }

    @FXML
    private void onNextPage() {
        selectRelativePage(1);
    }

    @FXML
    private void onApprovePage() {
        if (currentPage == null) {
            return;
        }

        applyFormStateToCurrentPage();
        currentPage.status = "Approved";
        currentItem.refreshStatus();
        renderReviewWorkspace();
    }

    @FXML
    private void onMarkNeedsFix() {
        if (currentPage == null) {
            return;
        }

        applyFormStateToCurrentPage();
        currentPage.status = "Needs Fix";
        currentItem.refreshStatus();
        renderReviewWorkspace();
    }

    @FXML
    private void onNextUnreviewed() {
        if (currentItem == null) {
            return;
        }

        QaPage nextUnreviewed = currentItem.nextUnreviewedPageAfter(currentPage);
        if (nextUnreviewed == null) {
            nextUnreviewed = currentItem.firstUnreviewedPage();
        }

        if (nextUnreviewed != null) {
            currentPage = nextUnreviewed;
            renderReviewWorkspace();
        }
    }

    @FXML
    private void onRotateLeft() {
        rotateCurrentPage(-90);
    }

    @FXML
    private void onRotateRight() {
        rotateCurrentPage(90);
    }

    @FXML
    private void onCompleteQa() {
        if (currentItem == null) {
            return;
        }

        currentItem.status = currentItem.hasNeedsFixPage() ? "Needs Fix" : "Ready";
        showAssignedQaList();
        renderAssignedQaCards();
    }

    private void rotateCurrentPage(int delta) {
        if (currentPage == null) {
            return;
        }

        currentPage.rotationDegrees = normalizeRotation(currentPage.rotationDegrees + delta);
        renderReviewWorkspace();
    }

    private int normalizeRotation(int rotationDegrees) {
        int normalized = rotationDegrees % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    private void selectRelativePage(int direction) {
        if (currentItem == null || currentPage == null) {
            return;
        }

        int currentIndex = currentItem.pages.indexOf(currentPage);
        int nextIndex = currentIndex + direction;
        if (nextIndex < 0 || nextIndex >= currentItem.pages.size()) {
            return;
        }

        currentPage = currentItem.pages.get(nextIndex);
        renderReviewWorkspace();
    }

    private void renderReviewWorkspace() {
        if (currentItem == null) {
            return;
        }

        qaBoxIdLabel.setText(currentItem.boxId);
        qaProfileLabel.setText(currentItem.profileName);
        qaProgressLabel.setText(currentItem.reviewedPageCount() + " / " + currentItem.pages.size() + " pages reviewed");
        qaTrayCountLabel.setText(currentItem.pages.size() + " pages");

        renderReviewStatus();
        renderDocumentTree();
        renderReviewPreview();
        renderPageTray();
        syncPageControls();
    }

    private void renderReviewStatus() {
        reviewStatusBadge.setText(currentItem.status);
        reviewStatusBadge.getStyleClass().removeAll("qa-review-status-review", "assigned-qa-status-ready", "assigned-qa-status-fix");
        reviewStatusBadge.getStyleClass().add(statusClassFor(currentItem.status));
    }

    private void renderDocumentTree() {
        qaDocumentTreeContainer.getChildren().clear();

        Map<Integer, List<QaPage>> groupedPages = new LinkedHashMap<>();
        for (QaPage page : currentItem.pages) {
            groupedPages.computeIfAbsent(page.documentNumber, key -> new ArrayList<>()).add(page);
        }

        for (Map.Entry<Integer, List<QaPage>> entry : groupedPages.entrySet()) {
            VBox block = new VBox(0);
            block.getStyleClass().add("document-tree-document-block");

            Label title = new Label("Document " + entry.getKey());
            title.getStyleClass().add("document-tree-document-title");

            Label count = new Label(entry.getValue().size() + " pages");
            count.getStyleClass().add("document-tree-count");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(9, title, spacer, count);
            header.getStyleClass().add("document-tree-document-header");

            block.getChildren().add(header);
            int pageIndex = 1;
            for (QaPage page : entry.getValue()) {
                block.getChildren().add(createTreeRow(page, pageIndex++));
            }

            qaDocumentTreeContainer.getChildren().add(block);
        }
    }

    private HBox createTreeRow(QaPage page, int pageNumberInDocument) {
        Label name = new Label("Page " + pageNumberInDocument);
        name.getStyleClass().add("document-tree-page-title");

        Label warning = new Label("Needs Fix".equals(page.status) ? "!" : "");
        warning.getStyleClass().add("document-tree-warning");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(9, name, spacer, warning);
        row.getStyleClass().add("document-tree-page-row");
        if (page == currentPage) {
            row.getStyleClass().add("document-tree-page-selected");
        }
        row.setOnMouseClicked(event -> {
            currentPage = page;
            renderReviewWorkspace();
        });
        return row;
    }

    private void renderReviewPreview() {
        if (currentPage == null) {
            qaPreviewHost.getChildren().setAll(emptyPreview("Select a page to review."));
            selectedQaPageTitleLabel.setText("No page selected");
            selectedQaPageSubtitleLabel.setText("Select a page from the document tree or tray.");
            zoomPercentLabel.setText("100%");
            return;
        }

        selectedQaPageTitleLabel.setText("Document " + currentPage.documentNumber + " · Page " + currentPage.pageNumber);
        selectedQaPageSubtitleLabel.setText("Ref " + currentPage.referenceId + " · " + currentPage.status);
        zoomPercentLabel.setText(Math.round(zoomMultiplier * 100) + "%");

        VBox previewPage = new VBox(15,
                mockLine("mock-line-dark", 220, 15),
                mockLine("mock-line-medium", 160, 9),
                mockLine("mock-line-light", 320, 9),
                mockLine("mock-line-light", 290, 9),
                mockLine("mock-line-light", 340, 9),
                mockLine("mock-line-light", 300, 9)
        );
        previewPage.getStyleClass().add("mock-document-page");
        previewPage.setMinSize(PREVIEW_PAGE_WIDTH, PREVIEW_PAGE_HEIGHT);
        previewPage.setPrefSize(PREVIEW_PAGE_WIDTH, PREVIEW_PAGE_HEIGHT);
        previewPage.setMaxSize(PREVIEW_PAGE_WIDTH, PREVIEW_PAGE_HEIGHT);
        previewPage.setRotate(currentPage.rotationDegrees);
        previewPage.setScaleX(zoomMultiplier);
        previewPage.setScaleY(zoomMultiplier);

        qaPreviewHost.getChildren().setAll(previewPage);
    }

    private Region mockLine(String styleClass, double width, double height) {
        Region line = new Region();
        line.getStyleClass().add(styleClass);
        line.setMinWidth(width);
        line.setPrefWidth(width);
        line.setMaxWidth(width);
        line.setMinHeight(height);
        line.setPrefHeight(height);
        line.setMaxHeight(height);
        return line;
    }

    private Node emptyPreview(String text) {
        VBox emptyState = new VBox(9);
        emptyState.getStyleClass().add("scan-preview-empty");

        Label title = new Label("Ready for QA");
        title.getStyleClass().add("scan-preview-empty-title");

        Label copy = new Label(text);
        copy.getStyleClass().add("scan-preview-empty-copy");
        copy.setWrapText(true);

        emptyState.getChildren().addAll(title, copy);
        return emptyState;
    }

    private void renderPageTray() {
        qaPageTrayContainer.getChildren().clear();

        for (QaPage page : currentItem.pages) {
            VBox item = new VBox(6);
            item.getStyleClass().add("page-tray-item");
            if (page == currentPage) {
                item.getStyleClass().add("page-tray-item-selected");
            }
            if ("Needs Fix".equals(page.status)) {
                item.getStyleClass().add("page-tray-item-warning");
            }

            Region thumbnail = new Region();
            thumbnail.getStyleClass().add("page-tray-thumbnail");

            Label pageLabel = new Label("P" + page.pageNumber);
            pageLabel.getStyleClass().add("page-tray-number");

            item.getChildren().addAll(thumbnail, pageLabel);
            item.setOnMouseClicked(event -> {
                currentPage = page;
                renderReviewWorkspace();
            });

            qaPageTrayContainer.getChildren().add(item);
        }
    }

    private void syncPageControls() {
        syncingPageControls = true;
        try {
            if (currentPage == null) {
                currentPageStatusLabel.setText("Not Reviewed");
                currentPageStatusLabel.getStyleClass().removeAll("qa-current-status-approved", "qa-current-status-fix");
                currentPageStatusLabel.getStyleClass().add("qa-current-status-pending");
                pageReadableCheckBox.setSelected(false);
                rotationCorrectCheckBox.setSelected(false);
                splitCorrectCheckBox.setSelected(false);
                pageCountCorrectCheckBox.setSelected(false);
                qaCommentTextArea.setText("");
                return;
            }

            currentPageStatusLabel.setText(currentPage.status);
            currentPageStatusLabel.getStyleClass().removeAll(
                    "qa-current-status-pending",
                    "qa-current-status-approved",
                    "qa-current-status-fix"
            );
            currentPageStatusLabel.getStyleClass().add(statusClassForCurrentPage(currentPage.status));

            pageReadableCheckBox.setSelected(currentPage.readable);
            rotationCorrectCheckBox.setSelected(currentPage.rotationCorrect);
            splitCorrectCheckBox.setSelected(currentPage.splitCorrect);
            pageCountCorrectCheckBox.setSelected(currentPage.pageCountCorrect);
            qaCommentTextArea.setText(currentPage.comment);
        } finally {
            syncingPageControls = false;
        }
    }

    private void applyFormStateToCurrentPage() {
        if (currentPage == null) {
            return;
        }

        currentPage.readable = pageReadableCheckBox.isSelected();
        currentPage.rotationCorrect = rotationCorrectCheckBox.isSelected();
        currentPage.splitCorrect = splitCorrectCheckBox.isSelected();
        currentPage.pageCountCorrect = pageCountCorrectCheckBox.isSelected();
        currentPage.comment = qaCommentTextArea.getText() == null ? "" : qaCommentTextArea.getText().trim();
    }

    private void showAssignedQaList() {
        assignedQaListView.setVisible(true);
        assignedQaListView.setManaged(true);
        qaReviewWorkspaceView.setVisible(false);
        qaReviewWorkspaceView.setManaged(false);
    }

    private void showReviewWorkspace() {
        assignedQaListView.setVisible(false);
        assignedQaListView.setManaged(false);
        qaReviewWorkspaceView.setVisible(true);
        qaReviewWorkspaceView.setManaged(true);
    }

    private String statusClassFor(String status) {
        return switch (normalize(status)) {
            case "ready" -> "assigned-qa-status-ready";
            case "needs fix" -> "assigned-qa-status-fix";
            default -> "qa-review-status-review";
        };
    }

    private String statusClassForCurrentPage(String status) {
        return switch (normalize(status)) {
            case "approved" -> "qa-current-status-approved";
            case "needs fix" -> "qa-current-status-fix";
            default -> "qa-current-status-pending";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class QaQueueItem {
        private final String boxId;
        private final String profileName;
        private final List<QaPage> pages;
        private String status;

        private QaQueueItem(String boxId, String profileName, String status, List<QaPage> pages) {
            this.boxId = boxId;
            this.profileName = profileName;
            this.status = status;
            this.pages = pages;
        }

        private int reviewedPageCount() {
            int reviewed = 0;
            for (QaPage page : pages) {
                if (!"Not Reviewed".equals(page.status)) {
                    reviewed++;
                }
            }
            return reviewed;
        }

        private boolean hasRemainingPages() {
            return firstUnreviewedPage() != null;
        }

        private QaPage firstSelectablePage() {
            QaPage firstUnreviewed = firstUnreviewedPage();
            return firstUnreviewed != null ? firstUnreviewed : pages.get(0);
        }

        private QaPage firstUnreviewedPage() {
            for (QaPage page : pages) {
                if ("Not Reviewed".equals(page.status)) {
                    return page;
                }
            }
            return null;
        }

        private QaPage nextUnreviewedPageAfter(QaPage currentPage) {
            if (currentPage == null) {
                return firstUnreviewedPage();
            }

            int currentIndex = pages.indexOf(currentPage);
            for (int index = currentIndex + 1; index < pages.size(); index++) {
                if ("Not Reviewed".equals(pages.get(index).status)) {
                    return pages.get(index);
                }
            }
            return null;
        }

        private boolean hasNeedsFixPage() {
            for (QaPage page : pages) {
                if ("Needs Fix".equals(page.status)) {
                    return true;
                }
            }
            return false;
        }

        private void refreshStatus() {
            if (hasNeedsFixPage()) {
                status = "Needs Fix";
                return;
            }

            if (reviewedPageCount() == pages.size()) {
                status = "Ready";
                return;
            }

            status = "In Review";
        }
    }

    private static final class QaPage {
        private final int referenceId;
        private final int documentNumber;
        private final int pageNumber;
        private int rotationDegrees;
        private boolean readable;
        private boolean rotationCorrect;
        private boolean splitCorrect;
        private boolean pageCountCorrect;
        private String comment = "";
        private String status = "Not Reviewed";

        private QaPage(int referenceId, int documentNumber, int pageNumber) {
            this.referenceId = referenceId;
            this.documentNumber = documentNumber;
            this.pageNumber = pageNumber;
            this.readable = true;
            this.rotationCorrect = true;
            this.splitCorrect = true;
            this.pageCountCorrect = true;
        }
    }
}
