package easv.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UserDashboardView {
    private final UserPortalModel portalModel;
    private final Consumer<UserPortalModel.PortalSession> onStartScanning;
    private final UserPortalModel.PortalSession lastSession;
    private final UserPortalModel.ProfileItem initialProfile;
    private final UserPortalModel.BoxItem initialBox;

    public UserDashboardView(UserPortalModel portalModel, Consumer<UserPortalModel.PortalSession> onStartScanning, UserPortalModel.PortalSession lastSession) {
        this(portalModel, onStartScanning, lastSession, null, null);
    }

    public UserDashboardView(UserPortalModel portalModel,
                             Consumer<UserPortalModel.PortalSession> onStartScanning,
                             UserPortalModel.PortalSession lastSession,
                             UserPortalModel.ProfileItem initialProfile,
                             UserPortalModel.BoxItem initialBox) {
        this.portalModel = portalModel;
        this.onStartScanning = onStartScanning;
        this.lastSession = lastSession;
        this.initialProfile = initialProfile;
        this.initialBox = initialBox;
    }

    public Parent create() {
        List<UserPortalModel.ProfileItem> profiles = portalModel.fetchProfilesForUser();
        UserPortalModel.ProfileItem[] selectedProfile = new UserPortalModel.ProfileItem[]{initialProfile};
        UserPortalModel.BoxItem[] selectedBox = new UserPortalModel.BoxItem[]{initialBox};
        UserPortalModel.DashboardDocument[] selectedDocument = new UserPortalModel.DashboardDocument[1];
        boolean[] browsingProfiles = new boolean[]{initialProfile != null};
        boolean[] browsingLogs = new boolean[1];
        Set<String> selectedFiles = new LinkedHashSet<>();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("page");

        Label pageTitle = new Label("User Dashboard");
        pageTitle.getStyleClass().add("page-title");
        Label pageSubtitle = new Label("Choose a profile, then move through boxes, documents, and files.");
        pageSubtitle.getStyleClass().add("muted");
        root.setTop(new VBox(3, pageTitle, pageSubtitle));
        BorderPane.setMargin(root.getTop(), new Insets(0, 0, 8, 0));

        Runnable[] renderPage = new Runnable[1];
        renderPage[0] = () -> {
            if (!browsingProfiles[0] && selectedProfile[0] == null) {
                if (browsingLogs[0]) {
                    root.setCenter(buildLogsPage(browsingLogs, renderPage[0]));
                } else {
                    root.setCenter(buildDashboardHome(profiles, browsingProfiles, browsingLogs, renderPage[0]));
                }
            } else if (selectedProfile[0] == null) {
                root.setCenter(buildProfilesPage(profiles, selectedProfile, browsingProfiles, renderPage[0]));
            } else if (selectedBox[0] == null) {
                root.setCenter(buildBoxesPage(selectedProfile[0], selectedProfile, selectedBox, selectedDocument, selectedFiles, browsingProfiles, renderPage[0]));
            } else {
                root.setCenter(buildDocumentsPage(selectedProfile[0], selectedBox[0], selectedProfile, selectedBox, selectedDocument, selectedFiles, browsingProfiles, renderPage[0]));
            }
        };

        renderPage[0].run();
        return root;
    }

    private Parent buildDashboardHome(List<UserPortalModel.ProfileItem> profiles,
                                      boolean[] browsingProfiles,
                                      boolean[] browsingLogs,
                                      Runnable renderPage) {
        Label heading = new Label("Dashboard");
        heading.getStyleClass().add("dashboard-home-title");
        Label subtitle = new Label("Start a new scan or review your recent activity.");
        subtitle.getStyleClass().add("muted");

        HBox actionCards = new HBox(14,
                buildHomeCard("P", "Profiles", "Begin a new scanning session", true, () -> {
                    browsingProfiles[0] = true;
                    browsingLogs[0] = false;
                    renderPage.run();
                }),
                buildHomeCard("S", "My Scans", "View scan history", false, null),
                buildHomeCard("L", "Logs / Scan History", "View important scan actions", false, () -> {
                    browsingLogs[0] = true;
                    renderPage.run();
                })
        );

        VBox recentScansPanel = new VBox(0);
        recentScansPanel.getStyleClass().add("panel");
        Label recentTitle = new Label("Recent Scans");
        recentTitle.getStyleClass().add("dashboard-panel-title");
        VBox recentHeader = new VBox(recentTitle);
        recentHeader.setPadding(new Insets(12, 14, 12, 14));

        GridPane table = new GridPane();
        table.getStyleClass().add("dashboard-table");
        table.add(buildTableHeader("Box ID"), 0, 0);
        table.add(buildTableHeader("Profile"), 1, 0);
        table.add(buildTableHeader("Status"), 2, 0);
        table.add(buildTableHeader("Date"), 3, 0);
        table.add(buildTableHeader("Pages"), 4, 0);

        String[][] rows = {
                {"BOX-2026-042", "Standard Scan", "Completed", "2026-04-24", "125"},
                {"BOX-2026-041", "High Quality", "Processing", "2026-04-24", "89"},
                {"BOX-2026-040", "Standard Scan", "Completed", "2026-04-23", "203"},
                {"BOX-2026-039", "Archive", "Failed", "2026-04-23", "0"}
        };

        for (int row = 0; row < rows.length; row++) {
            table.add(buildTableCell(rows[row][0]), 0, row + 1);
            table.add(buildTableCell(rows[row][1]), 1, row + 1);
            table.add(buildStatusCell(rows[row][2]), 2, row + 1);
            table.add(buildTableCell(rows[row][3]), 3, row + 1);
            table.add(buildTableCell(rows[row][4]), 4, row + 1);
        }
        recentScansPanel.getChildren().addAll(recentHeader, table);
        HBox.setHgrow(recentScansPanel, Priority.ALWAYS);

        VBox activeProfilePanel = new VBox(10);
        activeProfilePanel.getStyleClass().add("panel");
        activeProfilePanel.getStyleClass().add("dashboard-side-panel");
        Label activeTitle = new Label("Active Profile");
        activeTitle.getStyleClass().add("dashboard-panel-title");
        UserPortalModel.ProfileItem activeProfile = profiles.get(0);
        activeProfilePanel.getChildren().addAll(
                activeTitle,
                dashboardField("Profile Name", activeProfile.name()),
                dashboardField("Split Rule", activeProfile.splitRule()),
                dashboardField("Available Boxes", String.valueOf(activeProfile.boxes().size()))
        );

        VBox summaryPanel = new VBox(10);
        summaryPanel.getStyleClass().add("panel");
        summaryPanel.getStyleClass().add("dashboard-summary-panel");
        Label summaryTitle = new Label("Recent Activity");
        summaryTitle.getStyleClass().add("dashboard-panel-title");
        Label summaryText = new Label(lastSession == null
                ? "No finished session yet. Start from Profiles to open a new scanning run."
                : "Last export: " + lastSession.getExportLabel() + "\n"
                + lastSession.getDocuments().size() + " documents and "
                + lastSession.getAllFiles().size() + " scanned files.");
        summaryText.getStyleClass().add("muted");
        summaryText.setWrapText(true);
        summaryPanel.getChildren().addAll(summaryTitle, summaryText);

        VBox rightColumn = new VBox(14, activeProfilePanel, summaryPanel);
        rightColumn.setPrefWidth(300);
        rightColumn.setMaxWidth(300);

        HBox lowerGrid = new HBox(14, recentScansPanel, rightColumn);
        HBox.setHgrow(recentScansPanel, Priority.ALWAYS);

        return wrapPageContent(new VBox(16, heading, subtitle, actionCards, lowerGrid));
    }

    private Parent buildLogsPage(boolean[] browsingLogs, Runnable renderPage) {
        Button backButton = backButton("Back To Dashboard", () -> {
            browsingLogs[0] = false;
            renderPage.run();
        });

        Label heading = new Label("Logs / Scan History");
        heading.getStyleClass().add("dashboard-home-title");
        Label subtitle = new Label("Track who did what and when during the scanning flow.");
        subtitle.getStyleClass().add("muted");
        TextField searchField = searchField("Search logs");

        VBox logPanel = new VBox(0);
        logPanel.getStyleClass().add("panel");
        logPanel.setMaxWidth(Double.MAX_VALUE);
        Label panelTitle = new Label("Activity Log");
        panelTitle.getStyleClass().add("dashboard-panel-title");
        VBox panelHeader = new VBox(panelTitle);
        panelHeader.setPadding(new Insets(12, 14, 12, 14));

        GridPane table = new GridPane();
        table.getStyleClass().add("dashboard-table");
        table.setMaxWidth(Double.MAX_VALUE);
        configureLogTableColumns(table);

        Runnable refreshTable = () -> {
            table.getChildren().clear();
            table.add(buildTableHeader("User"), 0, 0);
            table.add(buildTableHeader("Action"), 1, 0);
            table.add(buildTableHeader("File"), 2, 0);
            table.add(buildTableHeader("Document"), 3, 0);
            table.add(buildTableHeader("Profile"), 4, 0);
            table.add(buildTableHeader("Box ID"), 5, 0);
            table.add(buildTableHeader("Date"), 6, 0);
            table.add(buildTableHeader("Time"), 7, 0);

            String search = normalized(searchField.getText());
            int visibleRow = 1;
            for (UserPortalModel.ActivityLogEntry entry : portalModel.getActivityLog()) {
                if (!matches(search,
                        entry.user(),
                        entry.action(),
                        entry.file(),
                        entry.document(),
                        entry.profile(),
                        entry.boxId(),
                        entry.date(),
                        entry.time())) {
                    continue;
                }
                table.add(buildTableCell(entry.user()), 0, visibleRow);
                table.add(buildTableCell(entry.action()), 1, visibleRow);
                table.add(buildTableCell(entry.file()), 2, visibleRow);
                table.add(buildTableCell(entry.document()), 3, visibleRow);
                table.add(buildTableCell(entry.profile()), 4, visibleRow);
                table.add(buildTableCell(entry.boxId()), 5, visibleRow);
                table.add(buildTableCell(entry.date()), 6, visibleRow);
                table.add(buildTableCell(entry.time()), 7, visibleRow);
                visibleRow++;
            }
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable.run());
        refreshTable.run();

        logPanel.getChildren().addAll(panelHeader, table);
        return wrapPageContent(new VBox(14, backButton, heading, subtitle, searchField, logPanel));
    }

    private Parent buildProfilesPage(List<UserPortalModel.ProfileItem> profiles,
                                     UserPortalModel.ProfileItem[] selectedProfile,
                                     boolean[] browsingProfiles,
                                     Runnable renderPage) {
        Button backButton = backButton("Back To Dashboard", () -> {
            browsingProfiles[0] = false;
            renderPage.run();
        });
        Label heading = sectionLabel("Profiles");
        TextField searchField = searchField("Search profiles");
        FlowPane cardRow = new FlowPane(12, 12);
        cardRow.setPrefWrapLength(980);

        Runnable refreshCards = () -> {
            cardRow.getChildren().clear();
            String search = normalized(searchField.getText());
            for (UserPortalModel.ProfileItem profile : profiles) {
                if (!matches(search, profile.name(), profile.splitRule())) {
                    continue;
                }
                VBox card = buildStructureCard("Profile", profile.name(), profile.splitRule(), false);
                card.setOnMouseClicked(event -> {
                    selectedProfile[0] = profile;
                    renderPage.run();
                });
                cardRow.getChildren().add(card);
            }
        };

        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshCards.run());
        refreshCards.run();
        return wrapPageContent(new VBox(10, backButton, heading, searchField, cardRow));
    }

    private Parent buildBoxesPage(UserPortalModel.ProfileItem profile,
                                  UserPortalModel.ProfileItem[] selectedProfile,
                                  UserPortalModel.BoxItem[] selectedBox,
                                  UserPortalModel.DashboardDocument[] selectedDocument,
                                  Set<String> selectedFiles,
                                  boolean[] browsingProfiles,
                                  Runnable renderPage) {
        Button backButton = backButton("Back To Profiles", () -> {
            selectedProfile[0] = null;
            selectedBox[0] = null;
            selectedDocument[0] = null;
            selectedFiles.clear();
            browsingProfiles[0] = true;
            renderPage.run();
        });
        Button dashboardButton = backButton("Back To Dashboard", () -> {
            selectedProfile[0] = null;
            selectedBox[0] = null;
            selectedDocument[0] = null;
            selectedFiles.clear();
            browsingProfiles[0] = false;
            renderPage.run();
        });

        Label pageName = new Label(profile.name());
        pageName.getStyleClass().add("page-title");
        HBox header = new HBox(12, backButton, dashboardButton, pageName);
        header.setAlignment(Pos.CENTER_LEFT);

        Label heading = sectionLabel("Boxes");
        TextField searchField = searchField("Search boxes");
        FlowPane cardRow = new FlowPane(12, 12);
        cardRow.setPrefWrapLength(980);

        Runnable refreshCards = () -> {
            cardRow.getChildren().clear();
            String search = normalized(searchField.getText());
            for (UserPortalModel.BoxItem box : profile.boxes()) {
                if (!matches(search, box.id(), box.description())) {
                    continue;
                }
                VBox card = buildStructureCard("Box", box.id(), box.description(), false);
                card.setOnMouseClicked(event -> {
                    selectedBox[0] = box;
                    selectedDocument[0] = null;
                    selectedFiles.clear();
                    renderPage.run();
                });
                cardRow.getChildren().add(card);
            }
        };

        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshCards.run());
        refreshCards.run();
        return wrapPageContent(new VBox(10, header, heading, searchField, cardRow));
    }

    private Parent buildDocumentsPage(UserPortalModel.ProfileItem profile,
                                      UserPortalModel.BoxItem box,
                                      UserPortalModel.ProfileItem[] selectedProfile,
                                      UserPortalModel.BoxItem[] selectedBox,
                                      UserPortalModel.DashboardDocument[] selectedDocument,
                                      Set<String> selectedFiles,
                                      boolean[] browsingProfiles,
                                      Runnable renderPage) {
        Button backButton = backButton("Back To Boxes", () -> {
            selectedBox[0] = null;
            selectedDocument[0] = null;
            selectedFiles.clear();
            browsingProfiles[0] = true;
            renderPage.run();
        });
        Button dashboardButton = backButton("Back To Dashboard", () -> {
            selectedProfile[0] = null;
            selectedBox[0] = null;
            selectedDocument[0] = null;
            selectedFiles.clear();
            browsingProfiles[0] = false;
            renderPage.run();
        });

        VBox titleBlock = new VBox(2,
                titleLabel(profile.name()),
                mutedLabel(box.id() + " - " + box.description()));
        HBox header = new HBox(12, backButton, dashboardButton, titleBlock);
        header.setAlignment(Pos.CENTER_LEFT);

        Label heading = sectionLabel("Documents");
        TextField searchField = searchField("Search documents");
        FlowPane documentRow = new FlowPane(12, 12);
        documentRow.setPrefWrapLength(740);

        VBox filesArea = new VBox(10);
        filesArea.getStyleClass().add("panel");
        HBox.setHgrow(filesArea, Priority.ALWAYS);

        Label selectionProfile = detailLabel("Profile: " + profile.name());
        Label selectionBox = detailLabel("Box: " + box.id());
        Label selectionDocument = detailLabel("Document: none");
        Label selectionFiles = detailLabel("Files: none selected");

        VBox currentSelection = new VBox(4, new Label("Current Selection"), selectionProfile, selectionBox, selectionDocument, selectionFiles);
        currentSelection.getStyleClass().add("panel");
        currentSelection.getStyleClass().add("current-selection-card");
        if (lastSession != null) {
            currentSelection.getChildren().add(detailLabel("Last session: " + lastSession.getDocuments().size()
                    + " documents / " + lastSession.getAllFiles().size() + " files"));
        }

        Button startButton = new Button("Open Scanning");
        startButton.getStyleClass().addAll("primary-button", "square-action-button");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(event -> {
            if (selectedDocument[0] == null) {
                return;
            }
            List<String> files = selectedFiles.isEmpty() ? selectedDocument[0].files() : List.copyOf(selectedFiles);
            onStartScanning.accept(portalModel.startSession(profile, box, files));
        });

        VBox actionColumn = new VBox(8, currentSelection, startButton);
        actionColumn.setAlignment(Pos.TOP_CENTER);
        actionColumn.setPrefWidth(190);
        actionColumn.setMaxWidth(190);

        Runnable[] refreshFiles = new Runnable[1];
        refreshFiles[0] = () -> {
            filesArea.getChildren().clear();
            Label filesHeading = sectionLabel("Files");
            filesArea.getChildren().add(filesHeading);

            if (selectedDocument[0] == null) {
                filesArea.getChildren().add(mutedLabel("Choose a document to see its files."));
                selectionDocument.setText("Document: none");
                selectionFiles.setText("Files: none selected");
                return;
            }

            selectionDocument.setText("Document: " + selectedDocument[0].name());
            selectionFiles.setText(selectedFiles.isEmpty()
                    ? "Files: all files in this document"
                    : "Files selected: " + selectedFiles.size());

            FlowPane fileRow = new FlowPane(10, 10);
            fileRow.setPrefWrapLength(740);
            for (String file : selectedDocument[0].files()) {
                VBox fileCard = buildStructureCard("File", file, fileMeta(file), true);
                setCardSelected(fileCard, selectedFiles.contains(file));
                fileCard.setOnMouseClicked(event -> {
                    if (selectedFiles.contains(file)) {
                        selectedFiles.remove(file);
                        setCardSelected(fileCard, false);
                    } else {
                        selectedFiles.add(file);
                        setCardSelected(fileCard, true);
                    }
                    selectionFiles.setText(selectedFiles.isEmpty()
                            ? "Files: all files in this document"
                            : "Files selected: " + selectedFiles.size());
                });
                fileRow.getChildren().add(fileCard);
            }
            filesArea.getChildren().add(fileRow);
        };

        Runnable[] refreshDocuments = new Runnable[1];
        refreshDocuments[0] = () -> {
            documentRow.getChildren().clear();
            String search = normalized(searchField.getText());
            for (UserPortalModel.DashboardDocument document : portalModel.fetchDashboardDocuments(profile, box)) {
                if (!matches(search, document.name())) {
                    continue;
                }
                VBox card = buildStructureCard("Document", document.name(), document.files().size() + " files", true);
                setCardSelected(card, selectedDocument[0] != null && selectedDocument[0].name().equals(document.name()));
                card.setOnMouseClicked(event -> {
                    boolean sameDocument = selectedDocument[0] != null && selectedDocument[0].name().equals(document.name());
                    selectedDocument[0] = sameDocument ? null : document;
                    selectedFiles.clear();
                    refreshDocuments[0].run();
                    refreshFiles[0].run();
                });
                documentRow.getChildren().add(card);
            }
        };

        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshDocuments[0].run());
        refreshDocuments[0].run();
        refreshFiles[0].run();

        VBox mainColumn = new VBox(10, heading, searchField, documentRow, filesArea);
        HBox body = new HBox(10, mainColumn, actionColumn);
        HBox.setHgrow(mainColumn, Priority.ALWAYS);

        return wrapPageContent(new VBox(10, header, body));
    }

    private VBox buildHomeCard(String iconText, String title, String subtitle, boolean accent, Runnable action) {
        Label icon = new Label(iconText);
        icon.getStyleClass().add(accent ? "home-card-icon-accent" : "home-card-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("home-card-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("home-card-subtitle");
        subtitleLabel.setWrapText(true);

        HBox topRow = new HBox(12, icon, titleLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(20, topRow, subtitleLabel);
        card.getStyleClass().add("dashboard-home-card");
        card.setPrefWidth(250);
        card.setMinHeight(132);
        if (action != null) {
            card.setOnMouseClicked(event -> action.run());
        }
        return card;
    }

    private Label buildTableHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("dashboard-table-header");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label buildTableCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("dashboard-table-cell");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private HBox buildStatusCell(String status) {
        Label label = new Label(status);
        label.getStyleClass().add("status-pill");
        String style = switch (status) {
            case "Completed" -> "status-completed";
            case "Processing" -> "status-processing";
            default -> "status-failed";
        };
        label.getStyleClass().add(style);
        HBox box = new HBox(label);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("dashboard-table-cell-box");
        return box;
    }

    private VBox dashboardField(String label, String value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("muted");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("dashboard-field-value");
        return new VBox(4, labelNode, valueNode);
    }

    private void configureLogTableColumns(GridPane table) {
        table.getColumnConstraints().setAll(
                percentColumn(10),
                percentColumn(10),
                percentColumn(18),
                percentColumn(16),
                percentColumn(12),
                percentColumn(10),
                percentColumn(12),
                percentColumn(12)
        );
    }

    private ColumnConstraints percentColumn(double percentWidth) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(percentWidth);
        constraints.setFillWidth(true);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
    }

    private ScrollPane wrapPageContent(VBox content) {
        content.setPadding(new Insets(2, 2, 8, 2));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        return scrollPane;
    }

    private Button backButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("back-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private TextField searchField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("dashboard-search");
        return field;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("structure-heading");
        return label;
    }

    private Label titleLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-title");
        return label;
    }

    private Label mutedLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }

    private VBox buildStructureCard(String type, String title, String subtitle, boolean showSelector) {
        Label icon = new Label(type.substring(0, 1));
        icon.getStyleClass().add("structure-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("structure-title");
        titleLabel.setWrapText(true);
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("structure-subtitle");
        subtitleLabel.setWrapText(true);
        VBox text = new VBox(3, titleLabel, subtitleLabel);
        text.setAlignment(Pos.CENTER_LEFT);
        Label arrow = new Label(type.equals("File") ? "" : ">");
        arrow.getStyleClass().add("structure-arrow");
        Label selector = new Label("");
        if (showSelector) {
            selector.getStyleClass().add("structure-selector");
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, icon, text, spacer, selector, arrow);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(row);
        card.getStyleClass().addAll("structure-card", "structure-type-" + type.toLowerCase());
        card.getProperties().put("selector", selector);
        return card;
    }

    private void setCardSelected(VBox card, boolean selected) {
        Label selector = (Label) card.getProperties().get("selector");
        if (selected) {
            if (!card.getStyleClass().contains("structure-selected")) {
                card.getStyleClass().add("structure-selected");
            }
            if (selector != null && !selector.getStyleClass().contains("structure-selector-selected")) {
                selector.getStyleClass().add("structure-selector-selected");
            }
            if (selector != null) {
                selector.setText("\u2713");
            }
        } else {
            card.getStyleClass().remove("structure-selected");
            if (selector != null) {
                selector.getStyleClass().remove("structure-selector-selected");
                selector.setText("");
            }
        }
    }

    private boolean matches(String search, String... values) {
        if (search.isBlank()) {
            return true;
        }
        for (String value : values) {
            if (normalized(value).contains(search)) {
                return true;
            }
        }
        return false;
    }

    private String normalized(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String fileMeta(String fileName) {
        int pages = fileName.toLowerCase().contains("multi") || fileName.toLowerCase().contains("pages") ? 3 : 1;
        int size = 160 + pages * 42;
        return pages + " page(s) - " + size + " KB";
    }

    private Label detailLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        label.setWrapText(true);
        return label;
    }
}
