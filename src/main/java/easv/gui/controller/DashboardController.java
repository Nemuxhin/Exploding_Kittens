package easv.gui.controller;

import javafx.fxml.FXML;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DashboardController {
    private enum PageState {
        DASHBOARD,
        LOGS,
        PROFILES,
        METADATA,
        METADATA_FORM,
        MY_SCANS,
        MY_SCAN_DETAIL,
        SCAN_PROGRESS,
        SCAN_COMPLETE,
        SCANNING,
        FILE_PAGES,
        QA_REVIEW
    }

    private enum PageMode {
        SINGLE_PAGE("Single Page"),
        MULTI_PAGE("Multi Page");

        private final String label;

        PageMode(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }
    }

    private record ActionCard(String badge, String title, String subtitle, boolean accent) {
    }

    private record ScanRow(String boxId, String profile, String status, String date, int pages) {
    }

    private record MyScanRow(String file, String document, String profile, String boxId, String date, String time, int pages, String size, String status, List<String> actions) {
    }

    private record ExportRow(String fileName, String boxId, String profile, String dateCreated, String size, String status) {
    }

    private record MetadataRow(String profile, String boxId, String file, String document, boolean hasMetadata) {
    }

    private record ProfileSummary(String profileName, String autoRotate, String brightness, String removeBlankPages) {
    }

    private record ScanProfile(String title, String description, String settingsLine) {
    }

    private record ScanningFile(String badge, String fileName, String reference, String pages, int pageCount, boolean barcode) {
    }

    private record ScanningDocument(String title, String fileCountText, List<ScanningFile> files) {
    }

    private List<ActionCard> actionCards() {
        return List.of(
                new ActionCard("[]", "Start Scan", "Begin a new scanning session", true),
                new ActionCard("MY", "My Scans", "View scan history", false),
                new ActionCard("MD", "Metadata", "Create and review document metadata", false)
        );
    }

    private List<ScanRow> recentScans() {
        return List.of(
                new ScanRow("BOX-2026-042", "Standard Scan", "Completed", "2026-04-24", 125),
                new ScanRow("BOX-2026-041", "High Quality", "Processing", "2026-04-24", 89),
                new ScanRow("BOX-2026-040", "Standard Scan", "Completed", "2026-04-23", 203),
                new ScanRow("BOX-2026-039", "Archive", "Failed", "2026-04-23", 0)
        );
    }

    private List<ExportRow> exports() {
        return List.of(
                new ExportRow("StandardScan_BOX-2026-042.pdf", "BOX-2026-042", "Standard Scan", "2026-04-24 14:52", "45.2 MB", "Ready"),
                new ExportRow("HighQuality_BOX-2026-041.pdf", "BOX-2026-041", "High Quality", "2026-04-24 13:30", "89.7 MB", "Processing"),
                new ExportRow("StandardScan_BOX-2026-040.pdf", "BOX-2026-040", "Standard Scan", "2026-04-23 16:45", "67.3 MB", "Ready"),
                new ExportRow("Archive_BOX-2026-038.pdf", "BOX-2026-038", "Archive", "2026-04-22 12:02", "34.8 MB", "Ready")
        );
    }

    private List<MyScanRow> myScanRows() {
        return List.of(
                new MyScanRow("StandardScan_BOX-2026-042", "BOX-2026-042", "Standard Scan", "BOX-2026-042", "2026-04-24", "14:45", 125, "45.2 MB", "Completed", List.of("View", "Export")),
                new MyScanRow("HighQuality_BOX-2026-041", "BOX-2026-041", "High Quality", "BOX-2026-041", "2026-04-24", "13:15", 89, "89.7 MB", "Processing", List.of()),
                new MyScanRow("StandardScan_BOX-2026-040", "BOX-2026-040", "Standard Scan", "BOX-2026-040", "2026-04-23", "16:38", 203, "67.3 MB", "Completed", List.of("View", "Export")),
                new MyScanRow("Archive_BOX-2026-039", "BOX-2026-039", "Archive", "BOX-2026-039", "2026-04-23", "10:05", 0, "0 MB", "Failed", List.of()),
                new MyScanRow("Archive_BOX-2026-038", "BOX-2026-038", "Archive", "BOX-2026-038", "2026-04-22", "12:02", 98, "34.8 MB", "Completed", List.of("View", "Export"))
        );
    }

    private List<MetadataRow> metadataRows() {
        return List.of(
                new MetadataRow("Building Archive", "BOX-2026-004", "File 01", "Document 1", true),
                new MetadataRow("Building Archive", "BOX-2026-004", "File 02", "Document 1", false),
                new MetadataRow("Building Archive", "BOX-2026-004", "File 03", "Document 1", false),
                new MetadataRow("Court Records", "BOX-2026-007", "File 01", "Document 2", false),
                new MetadataRow("Court Records", "BOX-2026-007", "File 02", "Document 2", false),
                new MetadataRow("Standard Scan", "BOX-2026-043", "File 01", "Document 3", false)
        );
    }

    private ProfileSummary activeProfile() {
        return new ProfileSummary("Standard Scan", "5 deg", "+10%", "On");
    }

    private List<ScanProfile> scanProfiles() {
        return List.of(
                new ScanProfile(
                        "Building Archive",
                        "Building records, plans, and long-term archive files",
                        "Brightness: +12% | Remove blanks: On | TIFF: Multi-page"
                ),
                new ScanProfile(
                        "Finance Profile",
                        "Financial document intake and classification",
                        "Brightness: +10% | Remove blanks: On | OCR: On"
                ),
                new ScanProfile(
                        "HR Profile",
                        "Employee records and onboarding packets",
                        "Brightness: +8% | Remove blanks: On | OCR: On"
                ),
                new ScanProfile(
                        "Legal Profile",
                        "Legal files and case paperwork",
                        "Brightness: +12% | OCR: On | File split: Barcode"
                ),
                new ScanProfile(
                        "Medical Records Profile",
                        "Patient charts and healthcare forms",
                        "Brightness: +12% | OCR: On | Remove blanks: On"
                ),
                new ScanProfile(
                        "Contracts Profile",
                        "Signed agreements and supporting pages",
                        "Brightness: +15% | OCR: On | File split: Barcode"
                ),
                new ScanProfile(
                        "Invoice Profile",
                        "Fast structured invoice capture",
                        "Brightness: +10% | Remove blanks: On | OCR: On"
                )
        );
    }

    private ScanProfile defaultScanProfile() {
        List<ScanProfile> profiles = scanProfiles();
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    private List<ScanningDocument> scanningDocuments() {
        return List.of(
                new ScanningDocument(
                        "Document 1",
                        "12 files",
                        List.of(
                                new ScanningFile("File 1", "Invoice_batch_01.tiff", "Ref: REF-001", "1 page", 1, false),
                                new ScanningFile("File 2", "Contract_batch_02.tiff", "Ref: REF-002", "1 page", 1, false),
                                new ScanningFile("File 3", "Cover_sheet_03.tiff", "Ref: REF-003", "2 pages", 2, false)
                        )
                ),
                new ScanningDocument(
                        "Document 2",
                        "8 files",
                        List.of(
                                new ScanningFile("File 1", "Archive_batch_04.tiff", "Ref: REF-004", "3 pages", 3, true),
                                new ScanningFile("File 2", "Archive_batch_05.tiff", "Ref: REF-005", "1 page", 1, false)
                        )
                ),
                new ScanningDocument(
                        "Document 3",
                        "15 files",
                        List.of(
                                new ScanningFile("File 1", "Medical_record_06.tiff", "Ref: REF-006", "4 pages", 4, false),
                                new ScanningFile("File 2", "Medical_record_07.tiff", "Ref: REF-007", "2 pages", 2, false),
                                new ScanningFile("File 3", "Separator_08.tiff", "Ref: REF-008", "1 page", 1, true)
                        )
                ),
                new ScanningDocument(
                        "Document 4",
                        "9 files",
                        List.of(
                                new ScanningFile("File 1", "Case_notes_09.tiff", "Ref: REF-009", "2 pages", 2, false),
                                new ScanningFile("File 2", "Case_notes_10.tiff", "Ref: REF-010", "1 page", 1, false)
                        )
                ),
                new ScanningDocument(
                        "Document 5",
                        "11 files",
                        List.of(
                                new ScanningFile("File 1", "Finance_doc_11.tiff", "Ref: REF-011", "2 pages", 2, false),
                                new ScanningFile("File 2", "Finance_doc_12.tiff", "Ref: REF-012", "2 pages", 2, false)
                        )
                )
        );
    }

    @FXML private BorderPane root;
    @FXML private VBox sidebar;
    @FXML private HBox dashboardNav;
    @FXML private HBox profilesNav;
    @FXML private HBox metadataNav;
    @FXML private HBox myScansNav;
    @FXML private HBox exportsNav;
    @FXML private Label dashboardNavIcon;
    @FXML private Label profilesNavIcon;
    @FXML private Label metadataNavIcon;
    @FXML private Label myScansNavIcon;
    @FXML private Label exportsNavIcon;
    @FXML private Label dashboardNavText;
    @FXML private Label profilesNavText;
    @FXML private Label metadataNavText;
    @FXML private Label myScansNavText;
    @FXML private Label exportsNavText;
    @FXML private HBox darkSwitch;
    @FXML private Label darkSwitchKnob;

    private double singlePageThumbnailScrollValue = 0;

    @FXML
    private void initialize() {
        if (!root.getStyleClass().contains("app-shell")) {
            root.getStyleClass().add("app-shell");
        }

        PageState[] currentPage = {PageState.DASHBOARD};
        ScanProfile[] selectedProfile = {defaultScanProfile()};
        String[] currentBoxId = {""};
        PageMode[] currentPageMode = {PageMode.MULTI_PAGE};
        List<ScanningDocument>[] scanningDocuments = new List[]{copyScanningDocuments(currentPageMode[0])};
        List<ScanningDocument>[] scannedWorkspaceDocuments = new List[]{new ArrayList<>()};
        ScanningDocument[] activeScanningDocument = {null};
        ScanningFile[] activeScanningFile = {null};
        boolean[] darkMode = {false};
        int[] qaCurrentPage = {1};
        int[] qaRotation = {0};
        double[] qaZoom = {0.78};
        double[] qaPanX = {0};
        double[] qaPanY = {0};
        boolean[] qaHighlight = {false};
        boolean[] qaApproved = {false};
        Set<String> qaCompletedDocuments = new HashSet<>();
        Set<String> qaHighlightedText = new HashSet<>();
        MetadataRow[] activeMetadataRow = {null};
        PageState[] metadataReturnPage = {PageState.METADATA};
        MyScanRow[] activeMyScanRow = {null};
        ScanningFile[] activeMyScanDetailFile = {null};
        String[] selectedTiffAction = {""};
        double[] scanProgress = {0};
        Timeline[] scanProgressTimeline = new Timeline[1];
        Runnable[] render = new Runnable[1];

        render[0] = () -> {
            if (currentPage[0] != PageState.SCAN_PROGRESS && scanProgressTimeline[0] != null) {
                scanProgressTimeline[0].stop();
            }
            Consumer<PageState> onNavigate = page -> {
                currentPage[0] = page;
                if (page == PageState.METADATA) {
                    activeMetadataRow[0] = null;
                }
                if (page == PageState.MY_SCANS) {
                    activeMyScanRow[0] = null;
                    activeMyScanDetailFile[0] = null;
                }
                render[0].run();
            };
            Consumer<ScanProfile> onSelectProfile = profile -> {
                selectedProfile[0] = profile;
                render[0].run();
            };

            root.getStyleClass().remove("dark-theme");
            if (darkMode[0]) {
                root.getStyleClass().add("dark-theme");
            }

            if (currentPage[0] == PageState.SCAN_PROGRESS || currentPage[0] == PageState.SCAN_COMPLETE) {
                root.setLeft(null);
            } else {
                if (root.getLeft() != sidebar) {
                    root.setLeft(sidebar);
                }
                updateSidebar(currentPage[0], darkMode[0], onNavigate, () -> {
                    darkMode[0] = !darkMode[0];
                    render[0].run();
                });
            }
            root.setCenter(buildContent(
                    currentPage[0],
                    selectedProfile[0],
                    currentBoxId[0],
                    currentPageMode[0],
                    scanningDocuments[0],
                    scannedWorkspaceDocuments[0],
                    activeScanningDocument[0],
                    activeScanningFile[0],
                    qaCurrentPage,
                    qaRotation,
                    qaZoom,
                    qaPanX,
                    qaPanY,
                    qaHighlight,
                    qaApproved,
                    qaCompletedDocuments,
                    qaHighlightedText,
                    activeMetadataRow[0],
                    activeMyScanRow[0],
                    activeMyScanDetailFile[0],
                    selectedTiffAction[0],
                    scanProgress[0],
                    onNavigate,
                    onSelectProfile,
                    pageMode -> currentPageMode[0] = pageMode,
                    (boxId, pageMode) -> {
                        currentBoxId[0] = boxId;
                        currentPageMode[0] = pageMode == null ? PageMode.MULTI_PAGE : pageMode;
                        scanningDocuments[0] = copyScanningDocuments(currentPageMode[0]);
                        scannedWorkspaceDocuments[0] = new ArrayList<>();
                        activeScanningDocument[0] = null;
                        activeScanningFile[0] = null;
                        singlePageThumbnailScrollValue = 0;
                        qaCurrentPage[0] = 1;
                        qaRotation[0] = 0;
                        qaZoom[0] = 0.78;
                        qaPanX[0] = 0;
                        qaPanY[0] = 0;
                        qaHighlight[0] = false;
                        qaApproved[0] = false;
                        qaCompletedDocuments.clear();
                        qaHighlightedText.clear();
                        selectedTiffAction[0] = "";
                        scanProgress[0] = 0;
                        if (scanProgressTimeline[0] != null) {
                            scanProgressTimeline[0].stop();
                        }
                        currentPage[0] = PageState.SCANNING;
                        render[0].run();
                    },
                    document -> {
                        activeScanningDocument[0] = document;
                        if (document != null && !document.files().isEmpty()) {
                            activeScanningFile[0] = document.files().get(0);
                        }
                        render[0].run();
                    },
                    () -> {
                        for (ScanningDocument document : scanningDocuments[0]) {
                            for (ScanningFile file : document.files()) {
                                activeScanningDocument[0] = scanFileIntoWorkspace(scanningDocuments[0], scannedWorkspaceDocuments[0], file);
                                activeScanningFile[0] = file;
                            }
                        }
                        if (!scannedWorkspaceDocuments[0].isEmpty()) {
                            activeScanningDocument[0] = scannedWorkspaceDocuments[0].get(0);
                        }
                        activeScanningFile[0] = null;
                        render[0].run();
                    },
                    file -> {
                        activeScanningFile[0] = file;
                        activeScanningDocument[0] = findDocumentForFile(scannedWorkspaceDocuments[0], file);
                        if (activeScanningDocument[0] == null) {
                            activeScanningDocument[0] = findDocumentForFile(scanningDocuments[0], file);
                        }
                        render[0].run();
                    },
                    file -> {
                        activeScanningDocument[0] = scanFileIntoWorkspace(scanningDocuments[0], scannedWorkspaceDocuments[0], file);
                        activeScanningFile[0] = file;
                        render[0].run();
                    },
                    file -> {
                        activeScanningFile[0] = file;
                        currentPage[0] = PageState.FILE_PAGES;
                        render[0].run();
                    },
                    () -> {
                        if (!scannedWorkspaceDocuments[0].isEmpty()) {
                            if (activeScanningFile[0] == null) {
                                activeScanningDocument[0] = scannedWorkspaceDocuments[0].get(0);
                                if (!activeScanningDocument[0].files().isEmpty()) {
                                    activeScanningFile[0] = activeScanningDocument[0].files().get(0);
                                }
                            } else {
                                activeScanningDocument[0] = findDocumentForFile(scannedWorkspaceDocuments[0], activeScanningFile[0]);
                            }
                        }
                        qaCurrentPage[0] = 1;
                        qaRotation[0] = 0;
                        qaZoom[0] = 0.78;
                        qaPanX[0] = 0;
                        qaPanY[0] = 0;
                        qaHighlight[0] = false;
                        qaApproved[0] = false;
                        currentPage[0] = PageState.QA_REVIEW;
                        render[0].run();
                    },
                    () -> {
                        currentPage[0] = PageState.SCANNING;
                        render[0].run();
                    },
                    () -> {
                        currentPage[0] = PageState.PROFILES;
                        render[0].run();
                    },
                    file -> {
                        activeScanningFile[0] = file;
                        activeScanningDocument[0] = findDocumentForFile(scannedWorkspaceDocuments[0], file);
                        qaCurrentPage[0] = 1;
                        qaRotation[0] = 0;
                        qaPanX[0] = 0;
                        qaPanY[0] = 0;
                        render[0].run();
                    },
                    () -> {
                        currentPage[0] = PageState.SCANNING;
                        render[0].run();
                    },
                    () -> {
                        currentPage[0] = PageState.DASHBOARD;
                        render[0].run();
                    },
                    scan -> {
                        activeMyScanRow[0] = scan;
                        activeMyScanDetailFile[0] = myScanFileForRow(scan);
                        currentPage[0] = PageState.MY_SCAN_DETAIL;
                        render[0].run();
                    },
                    file -> {
                        activeMyScanDetailFile[0] = file;
                        render[0].run();
                    },
                    () -> {
                        if (!qaApproved[0]) {
                            return;
                        }
                        currentPage[0] = PageState.MY_SCANS;
                        render[0].run();
                    },
                    offset -> {
                        List<ScanningFile> qaFiles = flattenScannedFiles(scannedWorkspaceDocuments[0]);
                        if (activeScanningFile[0] == null || qaFiles.isEmpty()) {
                            return;
                        }
                        int currentIndex = findFileIndex(qaFiles, activeScanningFile[0]);
                        if (currentIndex < 0) {
                            return;
                        }
                        int nextPage = qaCurrentPage[0] + offset;
                        if (nextPage >= 1 && nextPage <= activeScanningFile[0].pageCount()) {
                            qaCurrentPage[0] = nextPage;
                            render[0].run();
                            return;
                        }
                        int nextFileIndex = currentIndex + offset;
                        if (nextFileIndex >= 0 && nextFileIndex < qaFiles.size()) {
                            activeScanningFile[0] = qaFiles.get(nextFileIndex);
                            activeScanningDocument[0] = findDocumentForFile(scannedWorkspaceDocuments[0], activeScanningFile[0]);
                            qaCurrentPage[0] = offset < 0 ? activeScanningFile[0].pageCount() : 1;
                            qaRotation[0] = 0;
                            qaZoom[0] = 0.78;
                            qaPanX[0] = 0;
                            qaPanY[0] = 0;
                            render[0].run();
                        }
                    },
                    () -> {
                        qaRotation[0] = (qaRotation[0] + 90) % 360;
                        render[0].run();
                    },
                    zoomStep -> {
                        qaZoom[0] = Math.max(0.48, Math.min(1.22, qaZoom[0] + zoomStep));
                        if (qaZoom[0] <= 0.78) {
                            qaPanX[0] = 0;
                            qaPanY[0] = 0;
                        }
                        render[0].run();
                    },
                    delta -> {
                        qaPanX[0] = Math.max(-170, Math.min(170, qaPanX[0] + delta[0]));
                        qaPanY[0] = Math.max(-170, Math.min(170, qaPanY[0] + delta[1]));
                        render[0].run();
                    },
                    () -> {
                        qaHighlight[0] = !qaHighlight[0];
                        render[0].run();
                    },
                    highlightId -> {
                        if (qaHighlightedText.contains(highlightId)) {
                            qaHighlightedText.remove(highlightId);
                        } else {
                            qaHighlightedText.add(highlightId);
                        }
                        render[0].run();
                    },
                    () -> {
                        if (activeScanningDocument[0] != null) {
                            qaCompletedDocuments.add(activeScanningDocument[0].title());
                        }
                        qaApproved[0] = true;
                        render[0].run();
                    },
                    () -> {
                        qaApproved[0] = false;
                        currentPage[0] = PageState.SCANNING;
                        render[0].run();
                    },
                    tiffAction -> {
                        selectedTiffAction[0] = tiffAction;
                        render[0].run();
                    },
                    metadata -> {
                        activeMetadataRow[0] = metadata;
                        metadataReturnPage[0] = currentPage[0] == PageState.METADATA_FORM
                                ? PageState.METADATA
                                : currentPage[0];
                        currentPage[0] = PageState.METADATA_FORM;
                        render[0].run();
                    },
                    () -> {
                        activeMetadataRow[0] = null;
                        currentPage[0] = metadataReturnPage[0];
                        metadataReturnPage[0] = PageState.METADATA;
                        render[0].run();
                    }
            ));
        };

        render[0].run();
    }

    private void updateSidebar(PageState currentPage,
                               boolean darkMode,
                               Consumer<PageState> onNavigate,
                               Runnable onToggleTheme) {
        updateNavItem(dashboardNav, dashboardNavIcon, dashboardNavText, "Dashboard", currentPage, onNavigate);
        updateNavItem(profilesNav, profilesNavIcon, profilesNavText, "Start Scan", currentPage, onNavigate);
        updateNavItem(metadataNav, metadataNavIcon, metadataNavText, "Metadata", currentPage, onNavigate);
        updateNavItem(myScansNav, myScansNavIcon, myScansNavText, "Scans", currentPage, onNavigate);
        updateNavItem(exportsNav, exportsNavIcon, exportsNavText, "Exports", currentPage, onNavigate);

        darkSwitch.getStyleClass().setAll("dark-switch");
        darkSwitchKnob.getStyleClass().setAll("dark-switch-knob");
        if (darkMode) {
            darkSwitch.getStyleClass().add("dark-switch-active");
            darkSwitchKnob.getStyleClass().add("dark-switch-knob-active");
        }
        darkSwitch.setOnMouseClicked(event -> onToggleTheme.run());
    }

    private void updateNavItem(HBox row,
                               Label icon,
                               Label text,
                               String label,
                               PageState currentPage,
                               Consumer<PageState> onNavigate) {
        PageState page = mapPage(label);
        boolean active = currentPage == page
                || (currentPage == PageState.METADATA_FORM && page == PageState.METADATA)
                || (currentPage == PageState.MY_SCAN_DETAIL && page == PageState.MY_SCANS)
                || ((currentPage == PageState.SCANNING || currentPage == PageState.QA_REVIEW) && page == PageState.PROFILES);
        row.getStyleClass().setAll(active ? "sidebar-nav-item-active" : "sidebar-nav-item");
        icon.setText(active ? "[]" : "-");
        icon.getStyleClass().setAll("nav-icon");
        text.setText(label);
        text.getStyleClass().setAll(active ? "sidebar-nav-text-active" : "sidebar-nav-text");
        row.setOnMouseClicked(event -> onNavigate.accept(page));
    }

    private BorderPane buildContent(PageState pageState,
                                    ScanProfile selectedProfile,
                                    String currentBoxId,
                                    PageMode currentPageMode,
                                    List<ScanningDocument> scanningDocuments,
                                    List<ScanningDocument> scannedWorkspaceDocuments,
                                    ScanningDocument activeScanningDocument,
                                    ScanningFile activeScanningFile,
                                    int[] qaCurrentPage,
                                    int[] qaRotation,
                                    double[] qaZoom,
                                    double[] qaPanX,
                                    double[] qaPanY,
                                    boolean[] qaHighlight,
                                    boolean[] qaApproved,
                                    Set<String> qaCompletedDocuments,
                                    Set<String> qaHighlightedText,
                                    MetadataRow activeMetadataRow,
                                    MyScanRow activeMyScanRow,
                                    ScanningFile activeMyScanDetailFile,
                                    String selectedTiffAction,
                                    double scanProgress,
                                    Consumer<PageState> onNavigate,
                                    Consumer<ScanProfile> onSelectProfile,
                                    Consumer<PageMode> onSelectPageMode,
                                    BiConsumer<String, PageMode> onStartScanning,
                                    Consumer<ScanningDocument> onSelectScanningDocument,
                                    Runnable onScanAllDocuments,
                                    Consumer<ScanningFile> onSelectScanFile,
                                    Consumer<ScanningFile> onScanFile,
                                    Consumer<ScanningFile> onOpenScanningFile,
                                    Runnable onOpenQaReview,
                                    Runnable onScanningChanged,
                                    Runnable onBackToProfiles,
                                    Consumer<ScanningFile> onSelectQaFile,
                                    Runnable onBackFromQa,
                                    Runnable onBackToDashboard,
                                    Consumer<MyScanRow> onOpenMyScanDetail,
                                    Consumer<ScanningFile> onSelectMyScanDetailFile,
                                    Runnable onExportQa,
                                    Consumer<Integer> onStepQaPage,
                                    Runnable onRotateQaPage,
                                    Consumer<Double> onZoomQaPage,
                                    Consumer<double[]> onPanQaPage,
                                    Runnable onToggleQaHighlight,
                                    Consumer<String> onToggleQaTextHighlight,
                                    Runnable onApproveQa,
                                    Runnable onRejectQa,
                                    Consumer<String> onSelectTiffAction,
                                    Consumer<MetadataRow> onOpenMetadataForm,
                                    Runnable onBackToMetadata) {
        BorderPane content = new BorderPane();
        content.getStyleClass().add("dashboard-shell");
        boolean singlePageScanning = pageState == PageState.SCANNING
                && resolvedPageMode(currentPageMode) == PageMode.SINGLE_PAGE;
        if (!singlePageScanning
                && pageState != PageState.QA_REVIEW
                && pageState != PageState.METADATA
                && pageState != PageState.METADATA_FORM
                && pageState != PageState.MY_SCAN_DETAIL
                && pageState != PageState.LOGS
                && pageState != PageState.SCAN_PROGRESS
                && pageState != PageState.SCAN_COMPLETE) {
            content.setTop(buildTopBar());
        }
        Parent pageBody = switch (pageState) {
            case DASHBOARD -> buildDashboardBody(onNavigate);
            case PROFILES -> buildProfilesBody(selectedProfile, currentBoxId, currentPageMode, onSelectProfile, onSelectPageMode, onStartScanning, onNavigate);
            case METADATA -> buildMetadataBody(onOpenMetadataForm);
            case METADATA_FORM -> buildMetadataFormBody(activeMetadataRow, onBackToMetadata);
            case LOGS -> buildLogsBody();
            case MY_SCANS -> buildMyScansBody(onOpenMyScanDetail);
            case MY_SCAN_DETAIL -> buildMyScanDetailBody(
                    activeMyScanRow,
                    activeMyScanDetailFile,
                    onSelectMyScanDetailFile,
                    () -> onNavigate.accept(PageState.MY_SCANS)
            );
            case SCAN_PROGRESS -> buildScanProgressBody(selectedProfile, currentBoxId, currentPageMode, scanProgress, onBackToProfiles);
            case SCAN_COMPLETE -> buildScanCompleteBody(selectedProfile, currentBoxId, currentPageMode, onScanningChanged, onBackToProfiles);
            case SCANNING -> buildScanningBody(selectedProfile, currentBoxId, currentPageMode, scanningDocuments, scannedWorkspaceDocuments, activeScanningDocument, activeScanningFile, qaCompletedDocuments, onSelectScanningDocument, onSelectScanFile, onScanFile, onOpenScanningFile, onOpenQaReview, onScanningChanged, onBackToProfiles, onScanAllDocuments);
            case FILE_PAGES -> buildFilePagesBody(selectedProfile, currentBoxId, activeScanningDocument, activeScanningFile, () -> onScanningChanged.run());
            case QA_REVIEW -> buildQaReviewBody(
                    selectedProfile,
                    currentBoxId,
                    scannedWorkspaceDocuments,
                    activeScanningDocument,
                    activeScanningFile,
                    qaCurrentPage,
                    qaRotation,
                    qaZoom,
                    qaPanX,
                    qaPanY,
                    qaHighlight,
                    qaApproved,
                    qaCompletedDocuments,
                    qaHighlightedText,
                    selectedTiffAction,
                    onSelectQaFile,
                    onBackFromQa,
                    onBackToDashboard,
                    onExportQa,
                    onStepQaPage,
                    onRotateQaPage,
                    onZoomQaPage,
                    onPanQaPage,
                    onToggleQaHighlight,
                    onToggleQaTextHighlight,
                    onApproveQa,
                    onRejectQa,
                    onSelectTiffAction,
                    onOpenMetadataForm
            );
        };
        content.setCenter(pageBody);
        return content;
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("dashboard-topbar");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label notification = new Label("o");
        notification.getStyleClass().add("topbar-icon");
        Label userBadge = new Label("JD");
        userBadge.getStyleClass().add("topbar-user");

        topBar.getChildren().addAll(spacer, notification, userBadge);
        return topBar;
    }

    private VBox buildDashboardBody(Consumer<PageState> onNavigate) {
        VBox body = new VBox(14);
        body.getStyleClass().addAll("dashboard-body", "dashboard-home-page");

        Label heading = new Label("Dashboard");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label("Start a new scan or review your recent activity");
        subtitle.getStyleClass().add("dashboard-subtitle");

        VBox titleBlock = new VBox(4, heading, subtitle);

        HBox actionRow = new HBox(14);
        actionRow.getStyleClass().add("dashboard-action-row");
        for (ActionCard actionCard : actionCards()) {
            actionRow.getChildren().add(buildActionCard(actionCard, onNavigate));
        }

        HBox lowerRow = new HBox(14, buildRecentScansPanel(), buildRightColumn());
        lowerRow.getStyleClass().add("dashboard-lower-row");
        HBox.setHgrow(lowerRow.getChildren().get(0), Priority.ALWAYS);

        body.getChildren().addAll(titleBlock, actionRow, lowerRow);
        return body;
    }

    private Parent buildProfilesBody(ScanProfile selectedProfile,
                                     String currentBoxId,
                                     PageMode selectedPageMode,
                                     Consumer<ScanProfile> onSelectProfile,
                                     Consumer<PageMode> onSelectPageMode,
                                     BiConsumer<String, PageMode> onStartScanning,
                                     Consumer<PageState> onNavigate) {
        StackPane body = new StackPane();
        body.getStyleClass().addAll("dashboard-body", "new-scan-page");

        VBox content = new VBox(12);
        content.getStyleClass().add("new-scan-content");

        Label heading = new Label("New Scan");
        heading.getStyleClass().add("new-scan-title");
        Label subtitle = new Label("Choose a profile and box before scanning.");
        subtitle.getStyleClass().add("new-scan-subtitle");
        VBox header = new VBox(4, heading, subtitle);
        header.getStyleClass().add("new-scan-header");

        VBox formCard = new VBox(10);
        formCard.getStyleClass().add("new-scan-card");
        formCard.setMaxWidth(Double.MAX_VALUE);

        Label profileSectionTitle = new Label("Profile");
        profileSectionTitle.getStyleClass().add("new-scan-form-label");

        Label profileInfoIcon = new Label("i");
        profileInfoIcon.getStyleClass().add("profile-info-icon");
        Label profileInfoText = new Label("Profile info");
        profileInfoText.getStyleClass().add("profile-info-text");
        HBox profileInfo = new HBox(5, profileInfoIcon, profileInfoText);
        profileInfo.getStyleClass().add("profile-info-link");
        profileInfo.setAlignment(Pos.CENTER_RIGHT);

        Region profileSpacer = new Region();
        HBox.setHgrow(profileSpacer, Priority.ALWAYS);
        HBox profileHeader = new HBox(8, profileSectionTitle, profileSpacer, profileInfo);
        profileHeader.getStyleClass().add("new-scan-profile-header");

        ObservableList<ScanProfile> allProfiles = FXCollections.observableArrayList(scanProfiles());
        FilteredList<ScanProfile> filteredProfiles = new FilteredList<>(allProfiles, profile -> true);
        ComboBox<ScanProfile> profileCombo = new ComboBox<>(filteredProfiles);
        profileCombo.setEditable(true);
        profileCombo.setMaxWidth(Double.MAX_VALUE);
        profileCombo.setVisibleRowCount(5);
        profileCombo.getStyleClass().add("profile-combo-box");
        profileCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ScanProfile profile) {
                return profile == null ? "" : profile.title();
            }

            @Override
            public ScanProfile fromString(String value) {
                return findProfile(value, allProfiles);
            }
        });
        profileCombo.setCellFactory(listView -> {
            if (!listView.getStyleClass().contains("profile-combo-popup-list")) {
                listView.getStyleClass().add("profile-combo-popup-list");
            }
            return profileComboCell();
        });
        profileCombo.setButtonCell(profileComboCell());

        ScanProfile activeProfile = selectedProfile == null ? defaultScanProfile() : selectedProfile;
        profileCombo.setValue(activeProfile);
        if (activeProfile != null) {
            profileCombo.getEditor().setText(activeProfile.title());
        }

        Label profileHint = new Label("Only profiles assigned to you are shown.");
        profileHint.getStyleClass().add("new-scan-helper-text");

        Label boxIdTitle = new Label("Box ID");
        boxIdTitle.getStyleClass().add("new-scan-form-label");

        TextField boxIdField = new TextField();
        boxIdField.setPromptText("");
        boxIdField.getStyleClass().addAll("box-id-field", "new-scan-input");
        boxIdField.setText(currentBoxId);

        Label pageModeTitle = new Label("Document Handling");
        pageModeTitle.getStyleClass().add("new-scan-form-label");

        PageMode currentMode = selectedPageMode == null ? PageMode.MULTI_PAGE : selectedPageMode;
        PageMode[] pageModeSelection = {currentMode};
        ToggleGroup pageModeGroup = new ToggleGroup();
        ToggleButton singlePageButton = buildPageModeToggle(
                PageMode.SINGLE_PAGE,
                "Single page",
                "1 file = 1 page (each page is saved as a separate file)",
                pageModeGroup,
                currentMode
        );
        ToggleButton multiPageButton = buildPageModeToggle(
                PageMode.MULTI_PAGE,
                "Multi-page TIFF",
                "1 file = multiple pages (all pages are combined into one file)",
                pageModeGroup,
                currentMode
        );
        pageModeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                }
                return;
            }
            PageMode pageMode = (PageMode) newToggle.getUserData();
            pageModeSelection[0] = pageMode;
            onSelectPageMode.accept(pageMode);
        });

        HBox pageModeRow = new HBox(10, singlePageButton, multiPageButton);
        pageModeRow.getStyleClass().add("document-handling-row");
        HBox.setHgrow(singlePageButton, Priority.ALWAYS);
        HBox.setHgrow(multiPageButton, Priority.ALWAYS);

        Button startButton = new Button("Start Scanning");
        startButton.getStyleClass().addAll("start-scanning-button", "new-scan-primary-button");
        startButton.setMaxWidth(Double.MAX_VALUE);

        Button myScansButton = new Button("View My Scans");
        myScansButton.getStyleClass().add("new-scan-secondary-button");
        myScansButton.setOnAction(event -> onNavigate.accept(PageState.MY_SCANS));

        ScanProfile[] profileSelection = {profileCombo.getValue()};
        Runnable updateStartButton = () -> {
            String boxId = boxIdField.getText() == null ? "" : boxIdField.getText().trim();
            startButton.setDisable(profileSelection[0] == null || boxId.isBlank());
        };

        profileCombo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            String search = normalizeSearch(newValue);
            filteredProfiles.setPredicate(profile ->
                    matchesSearch(search, profile.title(), profile.description(), profile.settingsLine()));
            if (profileCombo.isFocused() && !profileCombo.isShowing()) {
                profileCombo.show();
            }
        });
        profileCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            profileSelection[0] = newValue;
            updateStartButton.run();
            if (newValue != null && !newValue.equals(oldValue)) {
                onSelectProfile.accept(newValue);
            }
        });
        boxIdField.textProperty().addListener((obs, oldValue, newValue) -> updateStartButton.run());
        updateStartButton.run();

        startButton.setOnAction(event -> {
            String boxId = boxIdField.getText() == null ? "" : boxIdField.getText().trim();
            if (profileSelection[0] != null && !boxId.isEmpty()) {
                onStartScanning.accept(boxId, pageModeSelection[0]);
            }
        });

        HBox actionRow = new HBox(10, startButton, myScansButton);
        actionRow.getStyleClass().add("new-scan-action-row");
        HBox.setHgrow(startButton, Priority.ALWAYS);

        formCard.getChildren().addAll(
                profileHeader,
                profileCombo,
                profileHint,
                boxIdTitle,
                boxIdField,
                pageModeTitle,
                pageModeRow,
                actionRow
        );

        content.getChildren().addAll(header, formCard);
        body.getChildren().add(content);
        StackPane.setAlignment(content, Pos.TOP_CENTER);

        Label topHelp = new Label("?");
        topHelp.getStyleClass().addAll("new-scan-help-button", "new-scan-help-top");
        StackPane.setAlignment(topHelp, Pos.TOP_RIGHT);

        Label bottomHelp = new Label("?");
        bottomHelp.getStyleClass().addAll("new-scan-help-button", "new-scan-help-bottom");
        StackPane.setAlignment(bottomHelp, Pos.BOTTOM_RIGHT);

        body.getChildren().addAll(topHelp, bottomHelp);
        return body;
    }

    private ToggleButton buildPageModeToggle(PageMode pageMode,
                                             String titleText,
                                             String descriptionText,
                                             ToggleGroup group,
                                             PageMode selectedPageMode) {
        Label title = new Label(titleText);
        title.getStyleClass().add("document-handling-title");

        Label description = new Label(descriptionText);
        description.getStyleClass().add("document-handling-description");
        description.setWrapText(true);

        VBox content = new VBox(6, title, description);
        content.getStyleClass().add("document-handling-content");
        content.setAlignment(Pos.CENTER_LEFT);

        ToggleButton button = new ToggleButton();
        button.setGraphic(content);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().addAll("scan-mode-toggle", "document-handling-option");
        button.setToggleGroup(group);
        button.setUserData(pageMode);
        button.setSelected(pageMode == selectedPageMode);
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, Priority.ALWAYS);
        return button;
    }

    private VBox buildLogsBody() {
        VBox body = new VBox(14);
        body.getStyleClass().addAll("dashboard-body", "exports-page");

        Label heading = new Label("Exports");
        heading.getStyleClass().add("exports-title");
        Label subtitle = new Label("Download and manage your exported files");
        subtitle.getStyleClass().add("exports-subtitle");

        VBox titleBlock = new VBox(6, heading, subtitle);

        HBox summaryRow = new HBox(14,
                buildExportMetricCard("Total Files", String.valueOf(exports().size())),
                buildExportMetricCard("Ready to Download", String.valueOf(exports().stream().filter(export -> "Ready".equals(export.status())).count())),
                buildExportMetricCard("Total Size", totalExportSizeText())
        );
        summaryRow.getStyleClass().add("exports-summary-row");
        summaryRow.setFillHeight(true);
        summaryRow.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        TextField searchField = new TextField();
        searchField.setPromptText("Box ID or filename...");
        searchField.getStyleClass().add("exports-filter-input");
        HBox searchBox = buildExportSearchBox(searchField);

        Runnable[] refreshTable = new Runnable[1];
        String[] statusFilterValue = {"All"};
        HBox statusFilter = buildExportStatusMenu(statusFilterValue, () -> {
            if (refreshTable[0] != null) {
                refreshTable[0].run();
            }
        });

        TextField fromDate = buildExportDateField();
        TextField toDate = buildExportDateField();

        HBox filterRow = new HBox(14,
                buildExportFilter("Search", searchBox),
                buildExportFilter("Status", statusFilter),
                buildExportFilter("From Date", buildExportDateBox(fromDate)),
                buildExportFilter("To Date", buildExportDateBox(toDate))
        );
        filterRow.getStyleClass().add("exports-filter-panel");
        filterRow.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        VBox panel = new VBox(0);
        panel.getStyleClass().add("exports-table-panel");

        GridPane table = new GridPane();
        table.getStyleClass().add("exports-table");
        table.setMaxWidth(Double.MAX_VALUE);
        table.getColumnConstraints().addAll(
                column(44),
                column(245),
                column(122),
                column(118),
                column(148),
                column(86),
                column(98),
                column(136)
        );

        Set<String> selectedExports = new HashSet<>();

        Label selectedCount = new Label("0 selected");
        selectedCount.getStyleClass().add("exports-selection-count");
        Region selectionSpacer = new Region();
        HBox.setHgrow(selectionSpacer, Priority.ALWAYS);
        Button downloadSelected = exportSelectionButton("Download All", "exports-selection-download-button");
        SVGPath downloadAllIcon = new SVGPath();
        downloadAllIcon.setContent("M12 3V15 M7 10L12 15L17 10 M5 21H19");
        downloadAllIcon.getStyleClass().add("exports-selection-download-icon");
        downloadSelected.setGraphic(downloadAllIcon);
        downloadSelected.setContentDisplay(ContentDisplay.LEFT);
        Button deleteSelected = exportSelectionButton("Delete Selected", "exports-selection-delete-button");
        Button clearSelected = exportSelectionButton("Clear", "exports-selection-clear-button");
        HBox selectionBar = new HBox(14, selectedCount, selectionSpacer, downloadSelected, deleteSelected, clearSelected);
        selectionBar.getStyleClass().add("exports-selection-bar");
        selectionBar.setAlignment(Pos.CENTER_LEFT);
        selectionBar.setVisible(false);
        selectionBar.setManaged(false);

        Runnable updateSelectionBar = () -> {
            int count = selectedExports.size();
            selectedCount.setText(count + " selected");
            selectionBar.setVisible(count > 0);
            selectionBar.setManaged(count > 0);
        };
        clearSelected.setOnAction(event -> {
            selectedExports.clear();
            refreshTable[0].run();
        });

        refreshTable[0] = () -> {
            table.getChildren().clear();

            String search = normalizeSearch(searchField.getText());
            String status = statusFilterValue[0];
            List<ExportRow> visibleExports = new ArrayList<>();
            for (ExportRow export : exports()) {
                boolean statusMatches = "All".equals(status) || status.equals(export.status());
                if (statusMatches && matchesSearch(search,
                        export.fileName(),
                        export.boxId(),
                        export.profile(),
                        export.dateCreated(),
                        export.size(),
                        export.status())) {
                    visibleExports.add(export);
                }
            }
            Set<String> visibleExportNames = new HashSet<>();
            for (ExportRow export : visibleExports) {
                visibleExportNames.add(export.fileName());
            }
            selectedExports.removeIf(fileName -> !visibleExportNames.contains(fileName));

            List<ExportRow> visibleReadyExports = new ArrayList<>();
            for (ExportRow export : visibleExports) {
                if ("Ready".equals(export.status())) {
                    visibleReadyExports.add(export);
                }
            }
            boolean allReadySelected = !visibleReadyExports.isEmpty()
                    && visibleReadyExports.stream().allMatch(export -> selectedExports.contains(export.fileName()));

            table.add(exportCheckboxCell(false, allReadySelected, selected -> {
                for (ExportRow export : visibleReadyExports) {
                    if (selected) {
                        selectedExports.add(export.fileName());
                    } else {
                        selectedExports.remove(export.fileName());
                    }
                }
                refreshTable[0].run();
            }), 0, 0);
            table.add(exportHeaderCell("FILE NAME"), 1, 0);
            table.add(exportHeaderCell("BOX ID"), 2, 0);
            table.add(exportHeaderCell("PROFILE"), 3, 0);
            table.add(exportSortableHeaderCell("DATE"), 4, 0);
            table.add(exportHeaderCell("SIZE"), 5, 0);
            table.add(exportHeaderCell("STATUS"), 6, 0);
            table.add(exportHeaderCell("ACTION"), 7, 0);

            int row = 1;
            for (ExportRow export : visibleExports) {
                boolean disabled = !"Ready".equals(export.status());
                table.add(exportCheckboxCell(disabled, selectedExports.contains(export.fileName()), selected -> {
                    if (selected) {
                        selectedExports.add(export.fileName());
                    } else {
                        selectedExports.remove(export.fileName());
                    }
                    refreshTable[0].run();
                }), 0, row);
                table.add(exportBodyCell(export.fileName(), true), 1, row);
                table.add(exportBodyCell(export.boxId(), false), 2, row);
                table.add(exportBodyCell(export.profile(), false), 3, row);
                table.add(exportBodyCell(export.dateCreated(), false), 4, row);
                table.add(exportBodyCell(export.size(), false), 5, row);
                table.add(exportStatusCell(export.status()), 6, row);
                table.add(exportActionCell(export), 7, row);
                row++;
            }
            updateSelectionBar.run();
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable[0].run());
        fromDate.textProperty().addListener((obs, oldValue, newValue) -> refreshTable[0].run());
        toDate.textProperty().addListener((obs, oldValue, newValue) -> refreshTable[0].run());
        refreshTable[0].run();

        HBox pageSize = buildExportPageSizeMenu();

        Label showing = new Label("Showing 1 to 4 of 4");
        showing.getStyleClass().add("exports-footer-text");
        HBox footerLeft = new HBox(12, showing, pageSize);
        footerLeft.setAlignment(Pos.CENTER_LEFT);

        HBox footerRight = new HBox(10,
                exportPagerButton("First", true),
                exportPagerButton("Previous", true),
                exportPageLabel(),
                exportPagerButton("Next", true),
                exportPagerButton("Last", true)
        );
        footerRight.setAlignment(Pos.CENTER_RIGHT);
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(16, footerLeft, footerSpacer, footerRight);
        footer.getStyleClass().add("exports-table-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        panel.getChildren().addAll(table, footer);
        body.getChildren().addAll(titleBlock, summaryRow, filterRow, selectionBar, panel);
        return body;
    }

    private VBox buildExportMetricCard(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("exports-metric-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("exports-metric-value");
        VBox card = new VBox(8, label, value);
        card.getStyleClass().add("exports-metric-card");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox buildExportFilter(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("exports-filter-label");
        VBox wrap = new VBox(8, label, control);
        wrap.getStyleClass().add("exports-filter-wrap");
        wrap.setMaxWidth(Double.MAX_VALUE);
        return wrap;
    }

    private HBox buildExportSearchBox(TextField searchField) {
        SVGPath icon = new SVGPath();
        icon.setContent("M21 21L15.8 15.8 M17 10.5A6.5 6.5 0 1 1 4 10.5A6.5 6.5 0 0 1 17 10.5");
        icon.getStyleClass().add("exports-search-icon");
        HBox box = new HBox(12, icon, searchField);
        box.getStyleClass().add("exports-search-box");
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private TextField buildExportDateField() {
        TextField field = new TextField();
        field.setPromptText("dd/mm/yyyy");
        field.getStyleClass().add("exports-date-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private HBox buildExportDateBox(TextField field) {
        SVGPath icon = new SVGPath();
        icon.setContent("M7 3V6 M17 3V6 M4 8H20 M5 5H19V20H5Z M8 11H10 M12 11H14 M16 11H18 M8 15H10 M12 15H14");
        icon.getStyleClass().add("exports-calendar-icon");
        HBox box = new HBox(10, field, icon);
        box.getStyleClass().add("exports-date-box");
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private String totalExportSizeText() {
        double total = 0;
        for (ExportRow export : exports()) {
            try {
                total += Double.parseDouble(export.size().replace(" MB", "").trim());
            } catch (NumberFormatException ignored) {
                // Keep summary stable if future size text includes another unit.
            }
        }
        return String.format(java.util.Locale.US, "%.1f MB", total);
    }

    private VBox buildMyScansBody(Consumer<MyScanRow> onOpenMyScanDetail) {
        VBox body = new VBox(14);
        body.getStyleClass().addAll("dashboard-body", "my-scans-page", "exports-page");

        Label heading = new Label("Scans");
        heading.getStyleClass().add("exports-title");
        Label subtitle = new Label("View and manage your scan history");
        subtitle.getStyleClass().add("exports-subtitle");
        VBox titleBlock = new VBox(6, heading, subtitle);

        TextField searchField = new TextField();
        searchField.setPromptText("Box ID...");
        searchField.getStyleClass().add("exports-filter-input");
        HBox searchBox = buildExportSearchBox(searchField);

        Runnable[] refreshTable = new Runnable[1];
        String[] statusFilterValue = {"All"};
        HBox statusFilter = buildMyScansStatusMenu(statusFilterValue, () -> {
            if (refreshTable[0] != null) {
                refreshTable[0].run();
            }
        });

        TextField fromDate = buildExportDateField();
        TextField toDate = buildExportDateField();

        HBox filterRow = new HBox(14,
                buildExportFilter("Search", searchBox),
                buildExportFilter("Status", statusFilter),
                buildExportFilter("From Date", buildExportDateBox(fromDate)),
                buildExportFilter("To Date", buildExportDateBox(toDate))
        );
        filterRow.getStyleClass().add("exports-filter-panel");
        filterRow.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        Set<String> selectedScans = new HashSet<>();
        Label selectedCount = new Label("0 selected");
        selectedCount.getStyleClass().add("exports-selection-count");
        Region selectionSpacer = new Region();
        HBox.setHgrow(selectionSpacer, Priority.ALWAYS);
        Button exportSelected = exportSelectionButton("Export Selected", "my-scans-selection-export-button");
        Button deleteSelected = exportSelectionButton("Delete Selected", "exports-selection-delete-button");
        Button clearSelected = exportSelectionButton("Clear", "exports-selection-clear-button");
        HBox selectionBar = new HBox(14, selectedCount, selectionSpacer, exportSelected, deleteSelected, clearSelected);
        selectionBar.getStyleClass().add("exports-selection-bar");
        selectionBar.setAlignment(Pos.CENTER_LEFT);
        selectionBar.setVisible(false);
        selectionBar.setManaged(false);

        Runnable updateSelectionBar = () -> {
            int count = selectedScans.size();
            selectedCount.setText(count + " selected");
            selectionBar.setVisible(count > 0);
            selectionBar.setManaged(count > 0);
        };
        clearSelected.setOnAction(event -> {
            selectedScans.clear();
            if (refreshTable[0] != null) {
                refreshTable[0].run();
            }
        });

        VBox tablePanel = buildMyScansTablePanel(
                searchField,
                statusFilterValue,
                fromDate,
                toDate,
                selectedScans,
                updateSelectionBar,
                refreshTable,
                onOpenMyScanDetail
        );
        body.getChildren().addAll(titleBlock, filterRow, selectionBar, tablePanel);
        return body;
    }

    private VBox buildMyScansTablePanel(TextField searchField,
                                        String[] statusFilterValue,
                                        TextField fromDate,
                                        TextField toDate,
                                        Set<String> selectedScans,
                                        Runnable updateSelectionBar,
                                        Runnable[] refreshTable,
                                        Consumer<MyScanRow> onOpenMyScanDetail) {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("exports-table-panel");
        panel.setMaxWidth(Double.MAX_VALUE);

        GridPane table = new GridPane();
        table.getStyleClass().add("exports-table");
        table.setMaxWidth(Double.MAX_VALUE);
        table.getColumnConstraints().addAll(
                column(44),
                column(160),
                column(150),
                column(126),
                column(86),
                column(76),
                column(100),
                column(120),
                column(160)
        );

        refreshTable[0] = () -> {
            table.getChildren().clear();

            String search = normalizeSearch(searchField.getText());
            String selectedStatus = statusFilterValue[0];
            List<MyScanRow> visibleScans = new ArrayList<>();
            for (MyScanRow scan : myScanRows()) {
                boolean statusMatches = "All".equals(selectedStatus)
                        || selectedStatus.equals(scan.status());
                if (!statusMatches) {
                    continue;
                }
                if (!matchesSearch(search,
                        scan.file(),
                        scan.document(),
                        scan.profile(),
                        scan.boxId(),
                        scan.date(),
                        scan.time(),
                        String.valueOf(scan.pages()),
                        scan.size(),
                        scan.status())) {
                    continue;
                }
                visibleScans.add(scan);
            }

            Set<String> visibleScanKeys = new HashSet<>();
            for (MyScanRow scan : visibleScans) {
                visibleScanKeys.add(myScanSelectionKey(scan));
            }
            selectedScans.removeIf(key -> !visibleScanKeys.contains(key));

            boolean allVisibleSelected = !visibleScans.isEmpty()
                    && visibleScans.stream().allMatch(scan -> selectedScans.contains(myScanSelectionKey(scan)));

            table.add(exportCheckboxCell(false, allVisibleSelected, selected -> {
                for (MyScanRow scan : visibleScans) {
                    String key = myScanSelectionKey(scan);
                    if (selected) {
                        selectedScans.add(key);
                    } else {
                        selectedScans.remove(key);
                    }
                }
                refreshTable[0].run();
            }), 0, 0);
            table.add(exportHeaderCell("BOX ID"), 1, 0);
            table.add(exportHeaderCell("PROFILE"), 2, 0);
            table.add(exportSortableHeaderCell("DATE"), 3, 0);
            table.add(exportHeaderCell("TIME"), 4, 0);
            table.add(exportHeaderCell("PAGES"), 5, 0);
            table.add(exportHeaderCell("SIZE"), 6, 0);
            table.add(exportHeaderCell("STATUS"), 7, 0);
            table.add(exportHeaderCell("ACTIONS"), 8, 0);

            int row = 1;
            for (MyScanRow scan : visibleScans) {
                String scanKey = myScanSelectionKey(scan);
                table.add(exportCheckboxCell(false, selectedScans.contains(scanKey), selected -> {
                    if (selected) {
                        selectedScans.add(scanKey);
                    } else {
                        selectedScans.remove(scanKey);
                    }
                    refreshTable[0].run();
                }), 0, row);
                table.add(exportBodyCell(scan.boxId(), true), 1, row);
                table.add(exportBodyCell(scan.profile(), false), 2, row);
                table.add(exportBodyCell(scan.date(), false), 3, row);
                table.add(exportBodyCell(scan.time(), false), 4, row);
                table.add(exportBodyCell(String.valueOf(scan.pages()), false), 5, row);
                table.add(exportBodyCell(scan.size(), false), 6, row);
                table.add(myScansStatusCell(scan.status()), 7, row);
                table.add(myScansActionsCell(scan, onOpenMyScanDetail), 8, row);
                row++;
            }
            updateSelectionBar.run();
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable[0].run());
        fromDate.textProperty().addListener((obs, oldValue, newValue) -> refreshTable[0].run());
        toDate.textProperty().addListener((obs, oldValue, newValue) -> refreshTable[0].run());
        refreshTable[0].run();

        HBox pageSize = buildExportPageSizeMenu();
        Label showing = new Label("Showing 1 to 5 of 5");
        showing.getStyleClass().add("exports-footer-text");
        HBox footerLeft = new HBox(12, showing, pageSize);
        footerLeft.setAlignment(Pos.CENTER_LEFT);

        HBox footerRight = new HBox(10,
                exportPagerButton("First", true),
                exportPagerButton("Previous", true),
                exportPageLabel(),
                exportPagerButton("Next", true),
                exportPagerButton("Last", true)
        );
        footerRight.setAlignment(Pos.CENTER_RIGHT);
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(16, footerLeft, footerSpacer, footerRight);
        footer.getStyleClass().add("exports-table-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        panel.getChildren().addAll(table, footer);
        return panel;
    }

    private Parent buildMyScanDetailBody(MyScanRow scan,
                                         ScanningFile activeDetailFile,
                                         Consumer<ScanningFile> onSelectDetailFile,
                                         Runnable onBackToMyScans) {
        MyScanRow row = scan == null ? firstMyScanRow() : scan;
        if (row == null) {
            VBox empty = new VBox(12, new Label("No scan selected."));
            empty.getStyleClass().addAll("dashboard-body", "my-scan-detail-page");
            return empty;
        }

        ScanningDocument document = myScanDocumentFor(row);
        List<ScanningFile> documentFiles = document.files();
        ScanningFile selectedFile = resolveMyScanDetailFile(document, activeDetailFile, row);

        BorderPane body = new BorderPane();
        body.getStyleClass().addAll("dashboard-body", "qa-review-page", "my-scan-detail-page");
        BorderPane detailMain = new BorderPane();
        detailMain.getStyleClass().add("qa-main-content");
        body.setCenter(detailMain);

        Label backArrow = new Label("<");
        backArrow.getStyleClass().add("qa-back-link");
        backArrow.setOnMouseClicked(event -> onBackToMyScans.run());

        Label brandName = new Label("Scan Details");
        brandName.getStyleClass().add("qa-brand-name");

        HBox breadcrumbs = new HBox(10,
                new Label("My Scans"),
                new Label(">"),
                new Label(row.boxId()),
                new Label(">"),
                new Label(row.document())
        );
        breadcrumbs.getStyleClass().add("qa-breadcrumbs");
        breadcrumbs.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(2, brandName, breadcrumbs);
        titleBlock.getStyleClass().add("qa-title-block");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Label status = new Label(row.status());
        status.getStyleClass().addAll("my-scans-status-pill", myScansStatusClass(row.status()));
        HBox headerRow = new HBox(14, backArrow, titleBlock, headerSpacer, status);
        headerRow.getStyleClass().add("qa-app-header");
        headerRow.setAlignment(Pos.CENTER_LEFT);

        if (documentFiles.isEmpty() || selectedFile == null) {
            VBox emptyPanel = new VBox(new Label("No scanned files available for this document."));
            emptyPanel.getStyleClass().add("panel-card");
            emptyPanel.setPadding(new Insets(22));
            detailMain.setTop(headerRow);
            detailMain.setCenter(emptyPanel);
            return body;
        }

        VBox filesPanel = new VBox(10);
        filesPanel.getStyleClass().addAll("qa-file-list-panel", "qa-files-panel");
        Label filesTitle = new Label("Files in " + document.title());
        filesTitle.getStyleClass().add("panel-title");
        Label filesCount = new Label(String.valueOf(documentFiles.size()));
        filesCount.getStyleClass().add("qa-count-pill");
        HBox filesHeader = new HBox(8, filesTitle, filesCount);
        filesHeader.setAlignment(Pos.CENTER_LEFT);

        FlowPane fileRows = new FlowPane(12, 12);
        fileRows.getStyleClass().add("qa-mini-file-list");
        fileRows.setAlignment(Pos.TOP_LEFT);
        fileRows.setPrefWrapLength(206);
        int fileIndex = 1;
        for (ScanningFile file : documentFiles) {
            boolean selected = file.reference().equals(selectedFile.reference());
            fileRows.getChildren().add(buildQaScanningFileCard(
                    file,
                    fileIndex++,
                    selected,
                    () -> onSelectDetailFile.accept(file)
            ));
        }

        ScrollPane fileScroll = new ScrollPane(fileRows);
        fileScroll.setFitToWidth(true);
        fileScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fileScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        fileScroll.getStyleClass().add("scan-document-scroll");
        VBox.setVgrow(fileScroll, Priority.ALWAYS);
        filesPanel.getChildren().addAll(filesHeader, fileScroll);
        VBox.setVgrow(filesPanel, Priority.ALWAYS);

        VBox leftColumn = new VBox(filesPanel);
        leftColumn.getStyleClass().add("qa-left-column");
        leftColumn.setPrefWidth(240);
        leftColumn.setMinWidth(240);
        leftColumn.setMaxWidth(240);
        leftColumn.setMaxHeight(Double.MAX_VALUE);

        VBox previewPaper = buildQaPreviewPaper(1, false, Set.of(), ignored -> {
        });
        StackPane previewStage = new StackPane(previewPaper);
        previewStage.getStyleClass().add("qa-preview-stage");
        Rectangle previewClip = new Rectangle();
        previewClip.arcWidthProperty().set(24);
        previewClip.arcHeightProperty().set(24);
        previewClip.widthProperty().bind(previewStage.widthProperty());
        previewClip.heightProperty().bind(previewStage.heightProperty());
        previewStage.setClip(previewClip);
        VBox.setVgrow(previewStage, Priority.ALWAYS);

        Label previewTitle = new Label("SCANNED FILE PREVIEW");
        previewTitle.getStyleClass().add("qa-bottom-title");
        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);
        Label previewPage = new Label("Page 1 / " + selectedFile.pageCount());
        previewPage.getStyleClass().add("qa-page-counter");
        HBox toolbar = new HBox(8, previewTitle, toolbarSpacer, previewPage);
        toolbar.getStyleClass().add("qa-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox bottomStrip = buildQaBottomFileStrip(document.title(), documentFiles, selectedFile, onSelectDetailFile);
        VBox previewColumn = new VBox(0, toolbar, previewStage, bottomStrip);
        previewColumn.getStyleClass().add("qa-preview-column");
        HBox.setHgrow(previewColumn, Priority.ALWAYS);

        VBox selectionPanel = qaSidePanel("Current File",
                qaMetaBlock("Profile", row.profile()),
                qaMetaBlock("Box ID", row.boxId()),
                qaMetaBlock("Document", document.title()),
                qaMetaBlock("File", selectedFile.badge()),
                qaMetaBlock("Reference ID", selectedFile.reference().replace("Ref: ", "")),
                qaMetaBlock("Filename", selectedFile.fileName()),
                qaMetaBlock("Pages", selectedFile.pages()),
                qaMetaBlock("Scanned", row.date() + " " + row.time()),
                qaMetaBlock("Size", row.size())
        );

        Region rightSpacer = new Region();
        VBox.setVgrow(rightSpacer, Priority.ALWAYS);
        Button backButton = new Button("Back to My Scans");
        backButton.getStyleClass().add("qa-side-action-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(event -> onBackToMyScans.run());

        VBox rightColumn = new VBox(10, selectionPanel, rightSpacer, backButton);
        rightColumn.getStyleClass().add("qa-right-column");
        rightColumn.setPrefWidth(230);
        rightColumn.setMinWidth(230);
        rightColumn.setMaxWidth(230);

        HBox workspace = new HBox(0, leftColumn, previewColumn, rightColumn);
        workspace.getStyleClass().add("qa-workspace");
        workspace.setMinHeight(0);
        workspace.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(previewColumn, Priority.ALWAYS);

        detailMain.setTop(headerRow);
        detailMain.setCenter(workspace);
        BorderPane.setAlignment(workspace, Pos.TOP_LEFT);
        return body;
    }

    private MyScanRow firstMyScanRow() {
        List<MyScanRow> rows = myScanRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private ScanningDocument myScanDocumentFor(MyScanRow scan) {
        if (scan == null) {
            return new ScanningDocument("Document", "0 files", List.of());
        }

        List<MyScanRow> matchingRows = new ArrayList<>();
        for (MyScanRow row : myScanRows()) {
            if (sameMyScanDocument(row, scan)) {
                matchingRows.add(row);
            }
        }
        matchingRows.sort((left, right) -> Integer.compare(fileNumber(left.file()), fileNumber(right.file())));

        List<ScanningFile> files = new ArrayList<>();
        for (MyScanRow row : matchingRows) {
            files.add(myScanFileForRow(row));
        }

        return new ScanningDocument(scan.document(), scannedFileCountText(files.size()), files);
    }

    private boolean sameMyScanDocument(MyScanRow left, MyScanRow right) {
        return left != null
                && right != null
                && left.document().equals(right.document())
                && left.profile().equals(right.profile())
                && left.boxId().equals(right.boxId());
    }

    private ScanningFile resolveMyScanDetailFile(ScanningDocument document, ScanningFile requestedFile, MyScanRow sourceRow) {
        String requestedReference = requestedFile == null && sourceRow != null
                ? myScanFileForRow(sourceRow).reference()
                : requestedFile == null ? "" : requestedFile.reference();
        for (ScanningFile file : document.files()) {
            if (file.reference().equals(requestedReference)) {
                return file;
            }
        }
        return document.files().isEmpty() ? null : document.files().get(0);
    }

    private ScanningFile myScanFileForRow(MyScanRow row) {
        int pageCount = myScanPageCount(row);
        String fileId = row.file().replace(" ", "-").toUpperCase();
        return new ScanningFile(
                row.file(),
                fileBaseName(row.document() + "_" + row.file()) + ".tiff",
                "Ref: " + row.boxId() + "-" + fileId,
                pageCountText(pageCount),
                pageCount,
                false
        );
    }

    private int myScanPageCount(MyScanRow row) {
        if (row == null) {
            return 1;
        }
        if (row.pages() >= 0) {
            return row.pages();
        }
        if (row.size() == null) {
            return 1;
        }
        try {
            String sizeValue = row.size().replace("MB", "").trim();
            double megabytes = Double.parseDouble(sizeValue);
            return Math.max(1, Math.min(8, (int) Math.round(megabytes / 15.0)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private int fileNumber(String fileLabel) {
        if (fileLabel == null) {
            return Integer.MAX_VALUE;
        }
        String digits = fileLabel.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private VBox buildMetadataBody(Consumer<MetadataRow> onOpenMetadataForm) {
        VBox body = new VBox(18);
        body.getStyleClass().addAll("dashboard-body", "metadata-page");

        Label heading = new Label("Metadata");
        heading.getStyleClass().add("metadata-title");
        Label subtitle = new Label("Create metadata records for your scanned documents.");
        subtitle.getStyleClass().add("metadata-subtitle");
        VBox titleBlock = new VBox(8, heading, subtitle);
        titleBlock.getStyleClass().add("metadata-title-block");

        Label help = new Label("?");
        help.getStyleClass().add("metadata-help-button");
        HBox header = new HBox(titleBlock, help);
        header.getStyleClass().add("metadata-header");
        header.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("Search by box ID, profile, file or document...");
        searchField.getStyleClass().add("metadata-search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M21 21L15.8 15.8 M17 10.5A6.5 6.5 0 1 1 4 10.5A6.5 6.5 0 0 1 17 10.5");
        searchIcon.getStyleClass().add("metadata-search-icon");

        HBox searchBox = new HBox(14, searchIcon, searchField);
        searchBox.getStyleClass().add("metadata-search-box");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        VBox tablePanel = buildMetadataTablePanel(searchField, onOpenMetadataForm);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);

        body.getChildren().addAll(header, searchBox, tablePanel);
        return body;
    }

    private VBox buildMetadataTablePanel(TextField searchField, Consumer<MetadataRow> onOpenMetadataForm) {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("metadata-table-panel");
        panel.setMaxWidth(Double.MAX_VALUE);

        GridPane table = new GridPane();
        table.getStyleClass().add("metadata-table");
        table.setMaxWidth(Double.MAX_VALUE);
        table.getColumnConstraints().addAll(
                column(180),
                column(150),
                column(130),
                column(150),
                column(300)
        );

        table.add(metadataHeaderCell("Profile"), 0, 0);
        table.add(metadataHeaderCell("Box ID"), 1, 0);
        table.add(metadataHeaderCell("File"), 2, 0);
        table.add(metadataHeaderCell("Document"), 3, 0);
        table.add(metadataHeaderCell("Actions"), 4, 0);

        Runnable refreshTable = () -> {
            while (table.getChildren().size() > 5) {
                table.getChildren().remove(table.getChildren().size() - 1);
            }
            String search = normalizeSearch(searchField.getText());
            int row = 1;
            for (MetadataRow metadata : metadataRows()) {
                if (!matchesSearch(search,
                        metadata.profile(),
                        metadata.boxId(),
                        metadata.file(),
                        metadata.document())) {
                    continue;
                }
                table.add(metadataBodyCell(metadata.profile()), 0, row);
                table.add(metadataBodyCell(metadata.boxId()), 1, row);
                table.add(metadataBodyCell(metadata.file()), 2, row);
                table.add(metadataBodyCell(metadata.document()), 3, row);
                table.add(metadataActionsCell(metadata, onOpenMetadataForm), 4, row);
                row++;
            }
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable.run());
        refreshTable.run();

        panel.getChildren().add(table);
        return panel;
    }

    private Label metadataHeaderCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("metadata-table-header");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label metadataBodyCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("metadata-table-cell");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        return label;
    }

    private HBox metadataActionsCell(MetadataRow metadata, Consumer<MetadataRow> onOpenMetadataForm) {
        HBox cell = new HBox(10);
        cell.getStyleClass().add("metadata-actions-cell");
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMaxWidth(Double.MAX_VALUE);

        Button primary = new Button(metadata.hasMetadata() ? "Edit Metadata" : "Create Metadata");
        primary.getStyleClass().add("metadata-primary-action");
        primary.setFocusTraversable(false);
        primary.setOnAction(event -> onOpenMetadataForm.accept(metadata));
        cell.getChildren().add(primary);

        if (metadata.hasMetadata()) {
            Button delete = new Button("Delete Metadata");
            delete.getStyleClass().add("metadata-delete-action");
            delete.setFocusTraversable(false);
            cell.getChildren().add(delete);
        }

        return cell;
    }

    private Parent buildMetadataFormBody(MetadataRow metadata, Runnable onBackToMetadata) {
        MetadataRow row = metadata == null
                ? new MetadataRow("Building Archive", "BOX-2026-004", "File 01", "Document 1", true)
                : metadata;

        VBox form = new VBox(14);
        form.getStyleClass().add("metadata-form-content");

        boolean editMode = row.hasMetadata();

        TextField titleField = metadataFormTextField(editMode ? "Q1 Invoice - Acme Corp" : "");
        titleField.setPromptText("Q1 Invoice - Acme Corp");

        ComboBox<String> documentType = new ComboBox<>();
        documentType.getItems().addAll("Invoice", "Contract", "Report", "Archive Record", "Case File");
        documentType.setPromptText("Select document type");
        documentType.getStyleClass().add("metadata-form-combo");
        documentType.setMaxWidth(Double.MAX_VALUE);
        documentType.setMinHeight(42);
        documentType.setPrefHeight(42);
        if (editMode) {
            documentType.setValue("Invoice");
        }

        TextField customerField = metadataFormTextField(editMode ? "Acme Corporation" : "");
        customerField.setPromptText("Acme Corporation");

        TextField dateField = metadataFormTextField(editMode ? "15/04/2026" : "");
        dateField.setPromptText("dd/mm/yyyy");
        Label calendarIcon = new Label("□");
        calendarIcon.getStyleClass().add("metadata-calendar-icon");
        HBox dateBox = new HBox(dateField, calendarIcon);
        dateBox.getStyleClass().add("metadata-date-field");
        dateBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(dateField, Priority.ALWAYS);

        TextField tagsField = metadataFormTextField(editMode ? "invoice, Q1, finance" : "");
        tagsField.setPromptText("invoice, Q1, finance");
        Button addTag = new Button("Add Tag");
        addTag.getStyleClass().add("metadata-tag-button");
        HBox tagsRow = new HBox(12, tagsField, addTag);
        tagsRow.getStyleClass().add("metadata-tags-row");
        HBox.setHgrow(tagsField, Priority.ALWAYS);
        VBox tagsControl = new VBox(8, tagsRow);
        if (editMode) {
            HBox tagChips = new HBox(10,
                    metadataTagChip("invoice"),
                    metadataTagChip("Q1"),
                    metadataTagChip("finance")
            );
            tagChips.getStyleClass().add("metadata-tag-chip-row");
            tagsControl.getChildren().add(tagChips);
        }

        TextArea notes = new TextArea(editMode ? "Includes service and support fees" : "");
        notes.setPromptText("Includes service and support fees");
        notes.getStyleClass().add("metadata-notes-area");
        notes.setWrapText(true);

        VBox formHeader = new VBox(8);
        formHeader.getStyleClass().add("metadata-form-top-header");
        Label title = new Label(editMode ? "Edit Metadata" : "Create Metadata");
        title.getStyleClass().add("metadata-form-title");
        Label context = new Label(row.profile() + " \u00b7 " + row.boxId() + " \u00b7 " + row.file() + " \u00b7 " + row.document());
        context.getStyleClass().add("metadata-form-context");
        formHeader.getChildren().addAll(title, context);

        form.getChildren().addAll(
                metadataFormField("Document Title", true, titleField),
                metadataFormField("Document Type", true, documentType),
                metadataFormField("Customer / Client Name", true, customerField),
                metadataFormField("Date (Document Date)", true, dateBox),
                metadataFormField("Tags", false, tagsControl),
                metadataFormField("Notes / Description", false, notes, "(optional)")
        );

        ScrollPane formScroll = new ScrollPane(form);
        formScroll.getStyleClass().add("metadata-form-scroll");
        formScroll.setFitToWidth(true);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("metadata-form-cancel");
        cancel.setOnAction(event -> onBackToMetadata.run());

        Button save = new Button(editMode ? "Save Metadata" : "Create Metadata");
        save.getStyleClass().add("metadata-form-save");
        save.setOnAction(event -> onBackToMetadata.run());

        HBox footer = new HBox(18, cancel, save);
        footer.getStyleClass().add("metadata-form-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(save, Priority.ALWAYS);

        BorderPane body = new BorderPane();
        body.getStyleClass().add("metadata-form-page");
        body.setTop(formHeader);
        body.setCenter(formScroll);
        body.setBottom(footer);
        return body;
    }

    private TextField metadataFormTextField(String text) {
        TextField field = new TextField(text);
        field.getStyleClass().add("metadata-form-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private HBox metadataTagChip(String text) {
        Label value = new Label(text);
        value.getStyleClass().add("metadata-tag-chip-text");
        Label close = new Label("×");
        close.getStyleClass().add("metadata-tag-chip-close");
        HBox chip = new HBox(6, value, close);
        chip.getStyleClass().add("metadata-tag-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    private VBox metadataFormField(String labelText, boolean required, Node control) {
        return metadataFormField(labelText, required, control, "");
    }

    private VBox metadataFormField(String labelText, boolean required, Node control, String suffix) {
        Label label = new Label(labelText);
        label.getStyleClass().add("metadata-form-label");
        HBox labelRow = new HBox(4, label);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        if (required) {
            Label star = new Label("*");
            star.getStyleClass().add("metadata-required-star");
            labelRow.getChildren().add(star);
        }
        if (suffix != null && !suffix.isBlank()) {
            Label suffixLabel = new Label(suffix);
            suffixLabel.getStyleClass().add("metadata-label-suffix");
            labelRow.getChildren().add(suffixLabel);
        }

        VBox field = new VBox(8, labelRow, control);
        field.getStyleClass().add("metadata-form-row");
        return field;
    }

    private VBox buildScanProgressBody(ScanProfile selectedProfile,
                                       String currentBoxId,
                                       PageMode currentPageMode,
                                       double scanProgress,
                                       Runnable onCancelScan) {
        VBox body = new VBox();
        body.getStyleClass().addAll("dashboard-body", "scan-flow-body");

        VBox card = new VBox(14);
        card.getStyleClass().add("scan-flow-card");

        VBox hero = new VBox(8);
        hero.setAlignment(Pos.CENTER);
        Label title = new Label("Scanning in Progress");
        title.getStyleClass().add("scan-flow-title");
        Label subtitle = new Label("Please wait while we process your documents");
        subtitle.getStyleClass().add("scan-flow-subtitle");
        hero.getChildren().addAll(buildProgressRing(scanProgress, Math.round(scanProgress) + "%"), title, subtitle);

        VBox steps = new VBox(8,
                scanProgressStep("Capturing pages", scanStepState(scanProgress, 0)),
                scanProgressStep("Applying profile settings", scanStepState(scanProgress, 28)),
                scanProgressStep("Processing", scanStepState(scanProgress, 56)),
                scanProgressStep("Finalizing", scanStepState(scanProgress, 82))
        );

        GridPane details = new GridPane();
        details.getStyleClass().add("scan-flow-info");
        details.setHgap(18);
        details.setVgap(8);
        details.add(scanInfoLabel("Box ID:"), 0, 0);
        details.add(scanInfoValue(currentBoxId), 1, 0);
        details.add(scanInfoLabel("Profile:"), 0, 1);
        details.add(scanInfoValue(scanProfileName(selectedProfile)), 1, 1);
        details.add(scanInfoLabel("Page mode:"), 0, 2);
        details.add(scanInfoValue(pageModeLabel(currentPageMode)), 1, 2);
        details.add(scanInfoLabel("Started by:"), 0, 3);
        details.add(scanInfoValue("John Doe"), 1, 3);
        details.add(scanInfoLabel("Time started:"), 0, 3);
        details.add(scanInfoValue("2:45 PM"), 1, 3);
        details.getColumnConstraints().addAll(column(150), column(300));

        Button cancelButton = new Button("x  Cancel Scan");
        cancelButton.getStyleClass().add("scan-flow-secondary-button");
        cancelButton.setMaxWidth(Double.MAX_VALUE);
        cancelButton.setOnAction(event -> onCancelScan.run());

        card.getChildren().addAll(hero, steps, details, cancelButton);
        body.getChildren().add(card);
        return body;
    }

    private VBox buildScanCompleteBody(ScanProfile selectedProfile,
                                       String currentBoxId,
                                       PageMode currentPageMode,
                                       Runnable onReviewAndExport,
                                       Runnable onStartAnotherScan) {
        VBox body = new VBox();
        body.getStyleClass().addAll("dashboard-body", "scan-flow-body");

        VBox card = new VBox(14);
        card.getStyleClass().add("scan-flow-card");

        VBox hero = new VBox(8);
        hero.setAlignment(Pos.CENTER);
        Label title = new Label("Scan Completed Successfully!");
        title.getStyleClass().add("scan-flow-title");
        Label subtitle = new Label("Your documents have been processed and are ready for review");
        subtitle.getStyleClass().add("scan-flow-subtitle");
        hero.getChildren().addAll(buildSuccessBadge(), title, subtitle);

        GridPane summary = new GridPane();
        summary.getStyleClass().add("scan-flow-summary");
        summary.setHgap(40);
        summary.setVgap(8);
        Label summaryTitle = new Label("Scan Summary");
        summaryTitle.getStyleClass().add("scan-flow-summary-title");
        summary.add(summaryTitle, 0, 0, 2, 1);
        summary.add(infoBlock("Profile Used", scanProfileName(selectedProfile)), 0, 1);
        summary.add(infoBlock("Box ID", currentBoxId), 1, 1);
        summary.add(infoBlock("Page Mode", pageModeLabel(currentPageMode)), 0, 2);
        summary.add(infoBlock("Pages Scanned", scannedPageCount(currentPageMode)), 1, 2);
        summary.add(infoBlock("Time Completed", "2:52 PM"), 0, 3);
        summary.getColumnConstraints().addAll(column(220), column(220));

        Button reviewButton = new Button("Review → QA → Export");
        reviewButton.getStyleClass().add("scan-flow-primary-button");
        reviewButton.setMaxWidth(Double.MAX_VALUE);
        reviewButton.setOnAction(event -> onReviewAndExport.run());

        Button detailsButton = new Button("View Scan Details");
        detailsButton.getStyleClass().add("scan-flow-secondary-button");
        detailsButton.setMaxWidth(Double.MAX_VALUE);
        detailsButton.setOnAction(event -> onReviewAndExport.run());

        Button anotherButton = new Button("Start Another Scan");
        anotherButton.getStyleClass().add("scan-flow-secondary-button");
        anotherButton.setMaxWidth(Double.MAX_VALUE);
        anotherButton.setOnAction(event -> onStartAnotherScan.run());

        card.getChildren().addAll(hero, summary, reviewButton, detailsButton, anotherButton);
        body.getChildren().add(card);
        return body;
    }

    private StackPane buildProgressRing(double percent, String labelText) {
        double radius = 30;
        double circumference = 2 * Math.PI * radius;
        Circle track = new Circle(30);
        track.getStyleClass().add("scan-progress-ring-track");

        Circle progress = new Circle(radius);
        progress.getStyleClass().add("scan-progress-ring-fill");
        progress.getStrokeDashArray().setAll(circumference);
        progress.setStrokeDashOffset(circumference * (1 - Math.max(0, Math.min(100, percent)) / 100.0));
        progress.setRotate(-90);

        Label label = new Label(labelText);
        label.getStyleClass().add("scan-progress-percent");

        StackPane ring = new StackPane(track, progress, label);
        ring.getStyleClass().add("scan-progress-ring");
        return ring;
    }

    private String scanStepState(double progress, double stepStart) {
        if (progress >= stepStart + 26) {
            return "complete";
        }
        if (progress >= stepStart) {
            return "active";
        }
        return "pending";
    }

    private StackPane buildSuccessBadge() {
        StackPane badge = new StackPane();
        badge.getStyleClass().add("scan-success-badge");
        SVGPath check = new SVGPath();
        check.setContent("M7 12L10.5 15.5L17 8");
        check.getStyleClass().add("scan-success-check");
        badge.getChildren().add(check);
        return badge;
    }

    private HBox scanProgressStep(String text, String state) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(scanStepIcon(state), scanStepLabel(text, state));
        return row;
    }

    private StackPane scanStepIcon(String state) {
        StackPane wrap = new StackPane();
        wrap.getStyleClass().add("scan-step-icon-" + state);
        if ("complete".equals(state)) {
            SVGPath check = new SVGPath();
            check.setContent("M6 12L10 16L18 8");
            check.getStyleClass().add("scan-step-check");
            wrap.getChildren().add(check);
        } else if ("active".equals(state)) {
            SVGPath spinner = new SVGPath();
            spinner.setContent("M19 12A7 7 0 1 1 12 5");
            spinner.getStyleClass().add("scan-step-spinner");
            wrap.getChildren().add(spinner);
        } else {
            Circle dot = new Circle(4);
            dot.getStyleClass().add("scan-step-dot");
            wrap.getChildren().add(dot);
        }
        return wrap;
    }

    private Label scanStepLabel(String text, String state) {
        Label label = new Label(text);
        label.getStyleClass().add("pending".equals(state) ? "scan-step-text-muted" : "scan-step-text");
        return label;
    }

    private Label scanInfoLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("scan-flow-info-label");
        return label;
    }

    private Label scanInfoValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("scan-flow-info-value");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_RIGHT);
        return label;
    }

    private String scanProfileName(ScanProfile selectedProfile) {
        return selectedProfile == null ? activeProfile().profileName() : selectedProfile.title();
    }

    private String pageModeLabel(PageMode pageMode) {
        return resolvedPageMode(pageMode).label();
    }

    private String scannedPageCount(PageMode pageMode) {
        int total = 0;
        for (ScanningDocument document : copyScanningDocuments(pageMode)) {
            for (ScanningFile file : document.files()) {
                total += file.pageCount();
            }
        }
        return total + (total == 1 ? " page" : " pages");
    }

    private PageMode resolvedPageMode(PageMode pageMode) {
        return pageMode == null ? PageMode.MULTI_PAGE : pageMode;
    }

    private VBox buildScanningBody(ScanProfile selectedProfile,
                                   String currentBoxId,
                                   PageMode currentPageMode,
                                   List<ScanningDocument> scanningDocuments,
                                   List<ScanningDocument> scannedWorkspaceDocuments,
                                   ScanningDocument activeScanningDocument,
                                   ScanningFile activeScanningFile,
                                   Set<String> qaCompletedDocuments,
                                   Consumer<ScanningDocument> onSelectScanningDocument,
                                   Consumer<ScanningFile> onSelectScanFile,
                                   Consumer<ScanningFile> onScanFile,
                                   Consumer<ScanningFile> onOpenScanningFile,
                                   Runnable onOpenQaReview,
                                   Runnable onScanningChanged,
                                   Runnable onBackToProfiles,
                                   Runnable onScanAllDocuments) {
        VBox body = new VBox(4);
        body.getStyleClass().add("dashboard-body");

        Label heading = new Label("Scanned Files");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label(selectedProfile.title() + " / " + currentBoxId + " / " + pageModeLabel(currentPageMode));
        subtitle.getStyleClass().add("dashboard-subtitle");
        Button backButton = new Button("Back To Start Scan");
        backButton.getStyleClass().add("qa-review-button");
        backButton.setOnAction(event -> onBackToProfiles.run());
        Button qaButton = new Button("QA Review");
        qaButton.getStyleClass().add("qa-review-button");
        qaButton.setDisable(scannedWorkspaceDocuments.isEmpty());
        qaButton.setOnAction(event -> onOpenQaReview.run());
        HBox headerRow = new HBox(12, new VBox(2, heading, subtitle), backButton, qaButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerRow.getChildren().get(0), Priority.ALWAYS);

        HBox layout = new HBox(10);
        VBox.setVgrow(layout, Priority.ALWAYS);
        boolean singlePageMode = resolvedPageMode(currentPageMode) == PageMode.SINGLE_PAGE;
        if (singlePageMode) {
            backButton.setText("Back");
            backButton.getStyleClass().add("scan-single-back-button");
            qaButton.getStyleClass().add("scan-single-qa-button");
            body.setSpacing(0);
            layout.setSpacing(0);
        }

        VBox scanFileSidebar = buildScanFileSidebar(
                scanningDocuments,
                scannedWorkspaceDocuments,
                activeScanningFile,
                onSelectScanFile,
                onScanFile,
                onScanAllDocuments
        );

        VBox center = new VBox(8);
        if (singlePageMode) {
            center.setSpacing(0);
        }
        HBox.setHgrow(center, Priority.ALWAYS);

        ComboBox<String> assignDocumentCombo = new ComboBox<>();
        for (ScanningDocument document : scannedWorkspaceDocuments) {
            assignDocumentCombo.getItems().add(document.title());
        }
        assignDocumentCombo.setPromptText("Select document");
        assignDocumentCombo.getStyleClass().add("scan-top-combo");
        assignDocumentCombo.setMaxWidth(Double.MAX_VALUE);
        assignDocumentCombo.setMinHeight(30);
        assignDocumentCombo.setPrefHeight(30);
        assignDocumentCombo.setMaxHeight(30);
        if (activeScanningDocument != null) {
            assignDocumentCombo.getSelectionModel().select(activeScanningDocument.title());
        }
        assignDocumentCombo.setOnAction(event -> {
            String selectedTitle = assignDocumentCombo.getSelectionModel().getSelectedItem();
            if (selectedTitle != null) {
                onSelectScanningDocument.accept(findScanningDocument(scannedWorkspaceDocuments, selectedTitle));
            }
        });

        String activeDocumentTitle = activeScanningDocument == null ? "Document" : activeScanningDocument.title();
        HBox topPanel = new HBox(6);
        if (singlePageMode) {
            List<VBox> statBlocks = List.of(
                    compactScanBlock("SCAN DATE", "2026-04-27"),
                    compactScanBlock("BOX ID", currentBoxId),
                    compactScanBlock("OUTPUT DOCS", String.valueOf(scannedWorkspaceDocuments.size())),
                    compactScanBlock("SCANNED", String.valueOf(totalFiles(scannedWorkspaceDocuments))),
                    compactScanBlock("CURRENT DOC", activeDocumentTitle)
            );
            double[] statWidths = {118, 92, 122, 108, 166};
            for (int i = 0; i < statBlocks.size(); i++) {
                VBox statBlock = statBlocks.get(i);
                statBlock.getStyleClass().add("scan-single-stat-block");
                statBlock.setMinWidth(statWidths[i]);
                statBlock.setPrefWidth(statWidths[i]);
                statBlock.setMaxWidth(statWidths[i]);
                HBox.setHgrow(statBlock, Priority.NEVER);
            }
            topPanel.getChildren().addAll(statBlocks);
            Region topSpacer = new Region();
            HBox.setHgrow(topSpacer, Priority.ALWAYS);
            topPanel.getChildren().add(topSpacer);
            Button helpButton = new Button("?");
            helpButton.getStyleClass().add("scan-single-help-button");
            HBox topActions = new HBox(10, backButton, qaButton, helpButton);
            topActions.getStyleClass().add("scan-single-top-actions");
            topActions.setAlignment(Pos.CENTER_RIGHT);
            topPanel.getChildren().add(topActions);
        } else {
            topPanel.getChildren().addAll(
                    compactScanBlock("Scan Date", "2026-04-27"),
                    compactScanBlock("Output Documents", String.valueOf(scannedWorkspaceDocuments.size())),
                    compactScanBlock("Scanned Files", String.valueOf(totalFiles(scannedWorkspaceDocuments))),
                    compactScanComboBlock("Output Document", assignDocumentCombo)
            );
        }
        topPanel.getStyleClass().add("scan-top-strip");
        if (singlePageMode) {
            topPanel.getStyleClass().add("scan-single-top-strip");
        }
        topPanel.setAlignment(Pos.CENTER_LEFT);
        int topPanelHeight = singlePageMode ? 86 : 70;
        topPanel.setMinHeight(topPanelHeight);
        topPanel.setPrefHeight(topPanelHeight);
        topPanel.setMaxHeight(topPanelHeight);

        VBox workspacePanel = new VBox(8);
        if (singlePageMode) {
            workspacePanel.setSpacing(0);
        }
        workspacePanel.getStyleClass().add("scan-main-panel");
        HBox.setHgrow(workspacePanel, Priority.ALWAYS);
        VBox.setVgrow(workspacePanel, Priority.ALWAYS);

        Node workspaceContent;
        if (scannedWorkspaceDocuments.isEmpty()) {
            Label emptyState = new Label("No files scanned yet.");
            emptyState.getStyleClass().add("scan-empty-state");
            VBox emptyBoard = new VBox(emptyState);
            emptyBoard.getStyleClass().add("scan-empty-board");
            workspaceContent = emptyBoard;
        } else if (singlePageMode) {
            workspaceContent = buildSinglePageScanViewer(
                    scannedWorkspaceDocuments,
                    activeScanningFile,
                    onSelectScanFile,
                    onScanningChanged
            );
        } else {
            List<VBox> sections = new ArrayList<>();
            for (ScanningDocument scannedDocument : scannedWorkspaceDocuments) {
                VBox[] fileCards = new VBox[scannedDocument.files().size()];
                for (int i = 0; i < scannedDocument.files().size(); i++) {
                    ScanningFile file = scannedDocument.files().get(i);
                    fileCards[i] = buildFileCard(scannedDocument, file, i, onOpenScanningFile, onScanningChanged);
                }
                sections.add(documentSection(
                        scannedDocument.title(),
                        documentQaStatus(scannedDocument, activeScanningDocument, qaCompletedDocuments),
                        fileCards
                ));
            }
            VBox documentBoard = new VBox(10);
            documentBoard.getChildren().addAll(sections);
            ScrollPane documentScroll = new ScrollPane(documentBoard);
            documentScroll.setFitToWidth(true);
            documentScroll.getStyleClass().add("scan-document-scroll");
            VBox.setVgrow(documentScroll, Priority.ALWAYS);
            workspaceContent = documentScroll;
        }

        workspacePanel.getChildren().add(workspaceContent);
        if (singlePageMode) {
            center.getChildren().add(workspacePanel);
        } else {
            center.getChildren().addAll(topPanel, workspacePanel);
        }
        VBox.setVgrow(center, Priority.ALWAYS);

        layout.getChildren().addAll(scanFileSidebar, center);
        HBox.setHgrow(center, Priority.ALWAYS);
        headerRow.getStyleClass().add("scan-header-row");
        if (singlePageMode) {
            body.getStyleClass().add("scan-single-page-body");
            body.getChildren().addAll(topPanel, layout);
        } else {
            body.getStyleClass().add("scan-multi-page-body");
            body.getChildren().addAll(headerRow, layout);
        }
        return body;
    }

    private VBox buildSinglePageScanViewer(List<ScanningDocument> scannedWorkspaceDocuments,
                                           ScanningFile activeScanningFile,
                                           Consumer<ScanningFile> onSelectScanFile,
                                           Runnable onScanningChanged) {
        ScanningFile selectedFile = selectedScannedFile(scannedWorkspaceDocuments, activeScanningFile);
        VBox viewer = new VBox(14);
        viewer.getStyleClass().add("scan-single-viewer");
        VBox.setVgrow(viewer, Priority.ALWAYS);

        if (selectedFile == null) {
            Label emptyState = new Label("No page selected.");
            emptyState.getStyleClass().add("scan-empty-state");
            viewer.getChildren().add(emptyState);
            return viewer;
        }
        List<ScanningFile> orderedFiles = flattenScannedFiles(scannedWorkspaceDocuments);
        int selectedIndex = findFileIndex(orderedFiles, selectedFile);
        ScanningDocument selectedDocument = findDocumentForFile(scannedWorkspaceDocuments, selectedFile);
        String selectedDocumentTitle = selectedDocument == null ? "Document" : selectedDocument.title();
        String selectedIncrementalId = selectedIndex >= 0 ? "File " + (selectedIndex + 1) : selectedFile.badge();
        String selectedPageNumber = selectedIndex >= 0 ? "Page " + (selectedIndex + 1) : selectedFile.badge();

        VBox mainPaper = buildSinglePageMainPaper(selectedDocumentTitle, selectedFile, selectedIncrementalId, selectedPageNumber);

        StackPane stage = new StackPane(mainPaper);
        stage.getStyleClass().add("scan-single-stage");
        VBox.setVgrow(stage, Priority.ALWAYS);

        Label selectedFileName = new Label(selectedFile.fileName());
        selectedFileName.getStyleClass().add("scan-single-selected-file-name");
        Label selectedFileRef = new Label("Ref: " + selectedFile.reference().replace("Ref: ", ""));
        selectedFileRef.getStyleClass().add("scan-single-selected-file-ref");
        VBox selectedFileHeader = new VBox(2, selectedFileName, selectedFileRef);
        selectedFileHeader.getStyleClass().add("scan-single-selected-file-header");

        HBox thumbnails = new HBox(8);
        thumbnails.getStyleClass().add("scan-single-thumbnail-row");
        thumbnails.setAlignment(Pos.CENTER_LEFT);
        int displayIndex = 1;
        Label stripTitle = new Label(selectedDocumentTitle.toUpperCase());
        stripTitle.getStyleClass().add("scan-single-strip-title");
        Label stripCount = new Label(String.valueOf(selectedDocument == null ? orderedFiles.size() : selectedDocument.files().size()));
        stripCount.getStyleClass().add("qa-count-pill");
        HBox stripHeader = new HBox(8, stripTitle, stripCount);
        stripHeader.setAlignment(Pos.CENTER_LEFT);

        int documentIndex = 0;
        for (ScanningDocument document : scannedWorkspaceDocuments) {
            thumbnails.getChildren().add(singlePageDocumentMarker(document, documentIndex > 0));
            for (ScanningFile file : document.files()) {
                boolean selected = selectedFile.reference().equals(file.reference());
                thumbnails.getChildren().add(singlePageThumbnail(document, file, displayIndex, selected, onSelectScanFile, onScanningChanged));
                displayIndex++;
            }
            documentIndex++;
        }

        ScrollPane thumbnailScroll = new ScrollPane(thumbnails);
        thumbnailScroll.getStyleClass().add("scan-single-thumbnail-scroll");
        thumbnailScroll.setFitToHeight(true);
        thumbnailScroll.setFitToWidth(false);
        thumbnailScroll.setPannable(true);
        thumbnailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        thumbnailScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        enableHorizontalThumbnailScroll(thumbnailScroll);
        thumbnailScroll.setHvalue(clampScrollValue(singlePageThumbnailScrollValue));
        thumbnailScroll.hvalueProperty().addListener((obs, oldValue, newValue) ->
                singlePageThumbnailScrollValue = clampScrollValue(newValue.doubleValue()));

        VBox thumbnailCarousel = new VBox(8, stripHeader, thumbnailScroll);
        thumbnailCarousel.getStyleClass().add("scan-single-thumbnail-carousel");
        VBox.setVgrow(thumbnailScroll, Priority.ALWAYS);

        viewer.getChildren().addAll(selectedFileHeader, stage, thumbnailCarousel);
        return viewer;
    }

    private VBox buildSinglePageMainPaper(String documentTitle,
                                          ScanningFile file,
                                          String incrementalId,
                                          String pageNumber) {
        Label document = new Label(documentTitle);
        document.getStyleClass().add("scan-single-paper-document-pill");

        HBox documentRow = new HBox(8, document);
        documentRow.setAlignment(Pos.CENTER_LEFT);
        if (file.barcode()) {
            Label barcode = new Label("Barcode detected - new document");
            barcode.getStyleClass().add("scan-single-paper-barcode-pill");
            documentRow.getChildren().add(barcode);
        }

        Label titleLabel = new Label("Title");
        titleLabel.getStyleClass().add("scan-single-paper-label");
        Label titleValue = new Label(file.fileName());
        titleValue.getStyleClass().add("scan-single-paper-title");
        titleValue.setWrapText(true);
        VBox titleBlock = new VBox(4, titleLabel, titleValue);
        titleBlock.getStyleClass().add("scan-single-paper-title-block");

        GridPane metadata = new GridPane();
        metadata.getStyleClass().add("scan-single-paper-meta-grid");
        metadata.add(singlePagePaperMetaItem("Incremental ID", incrementalId), 0, 0);
        metadata.add(singlePagePaperMetaItem("Reference ID", file.reference().replace("Ref: ", "")), 1, 0);
        metadata.add(singlePagePaperMetaItem("Page Number", pageNumber), 0, 1);
        metadata.add(singlePagePaperMetaItem("File Pages", file.pages()), 1, 1);
        metadata.getColumnConstraints().addAll(column(145), column(145));

        VBox content = new VBox(8,
                singlePagePaperLine(1.0),
                singlePagePaperLine(0.84),
                singlePagePaperLine(0.70)
        );
        content.getStyleClass().add("scan-single-paper-content");

        VBox paper = new VBox(14, documentRow, titleBlock, metadata, content);
        paper.getStyleClass().addAll("scan-large-paper-preview", "scan-single-main-paper");
        return paper;
    }

    private VBox singlePagePaperMetaItem(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("scan-single-paper-label");

        Label value = new Label(valueText);
        value.getStyleClass().add("scan-single-paper-value");
        value.setWrapText(true);

        VBox item = new VBox(3, label, value);
        item.getStyleClass().add("scan-single-paper-meta-item");
        return item;
    }

    private Region singlePagePaperLine(double widthFactor) {
        Region line = new Region();
        line.getStyleClass().add("scan-single-paper-line");
        line.setPrefWidth(280 * widthFactor);
        line.setMaxWidth(280 * widthFactor);
        return line;
    }

    private void configureSinglePageArrow(Label arrow, boolean enabled, Runnable action) {
        if (enabled) {
            arrow.setOnMouseClicked(event -> action.run());
            return;
        }
        arrow.getStyleClass().add("scan-single-nav-arrow-disabled");
        arrow.setMouseTransparent(true);
    }

    private void enableHorizontalThumbnailScroll(ScrollPane thumbnailScroll) {
        thumbnailScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            double delta = Math.abs(event.getDeltaX()) > 0 ? event.getDeltaX() : event.getDeltaY();
            if (Math.abs(delta) <= 0) {
                return;
            }
            thumbnailScroll.setHvalue(clampScrollValue(thumbnailScroll.getHvalue() - (delta / 600)));
            event.consume();
        });
    }

    private double clampScrollValue(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private VBox singlePageDocumentMarker(ScanningDocument document,
                                          boolean newDocument) {
        Label title = new Label(newDocument ? "NEW DOCUMENT" : "DOCUMENT");
        title.getStyleClass().add(newDocument ? "scan-single-new-document-label" : "scan-single-document-label");

        Label documentName = new Label(document.title());
        documentName.getStyleClass().add("scan-single-document-name");

        Label hint = new Label(document.files().isEmpty() ? "empty" : newDocument ? "barcode split" : "start");
        hint.getStyleClass().add("scan-single-document-hint");

        VBox marker = new VBox(5, title, documentName, hint);
        marker.getStyleClass().add("scan-single-document-marker");
        if (newDocument) {
            marker.getStyleClass().add("scan-single-document-marker-new");
        }
        marker.setAlignment(Pos.CENTER);
        marker.getProperties().put("scanningDocument", document);
        marker.getProperties().put("documentDropTarget", true);
        return marker;
    }

    private VBox singlePageThumbnail(ScanningDocument document,
                                     ScanningFile file,
                                     int displayIndex,
                                     boolean selected,
                                     Consumer<ScanningFile> onSelectScanFile,
                                     Runnable onScanningChanged) {
        VBox preview = buildPaperPreview();
        preview.getStyleClass().add("scan-multi-thumbnail");
        StackPane previewWrap = new StackPane(preview);
        previewWrap.getStyleClass().add("scan-single-thumbnail-preview-wrap");
        if (file.barcode()) {
            Label barcode = new Label("Barcode");
            barcode.getStyleClass().add("scan-single-barcode-pill");
            StackPane.setAlignment(barcode, Pos.TOP_CENTER);
            StackPane.setMargin(barcode, new Insets(5, 0, 0, 0));
            previewWrap.getChildren().add(barcode);
        }

        Label label = new Label("F" + displayIndex);
        label.getStyleClass().add("scan-multi-thumbnail-label");
        Label pageCount = new Label(file.pages());
        pageCount.getStyleClass().add("scan-single-thumbnail-pages");

        VBox card = new VBox(4, previewWrap, label, pageCount);
        card.getStyleClass().addAll("scan-file-card", "scan-single-thumbnail-card");
        if (file.barcode()) {
            card.getStyleClass().add("scan-single-thumbnail-barcode");
        }
        if (selected) {
            card.getStyleClass().add("scan-multi-thumbnail-selected");
        }
        enableSmoothFileDrag(card, document, file, onSelectScanFile, onScanningChanged);
        card.getProperties().put("referenceId", file.reference());
        card.getProperties().put("scanningDocument", document);
        return card;
    }

    private VBox buildScanFileSidebar(List<ScanningDocument> scanningDocuments,
                                      List<ScanningDocument> scannedWorkspaceDocuments,
                                      ScanningFile activeScanningFile,
                                      Consumer<ScanningFile> onSelectScanFile,
                                      Consumer<ScanningFile> onScanFile,
                                      Runnable onScanAllDocuments) {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("scan-file-sidebar-panel");
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(250);

        Label title = new Label("Scanned Files");
        title.getStyleClass().add("scan-file-sidebar-title");

        ScanningFile firstPendingFile = firstUnscannedFile(scanningDocuments, scannedWorkspaceDocuments);
        ScanningFile scanTarget = activeScanningFile != null && !containsScannedFile(scannedWorkspaceDocuments, activeScanningFile)
                ? activeScanningFile
                : firstPendingFile;

        Button scanFileButton = new Button("Scan File");
        scanFileButton.getStyleClass().addAll("scan-sidebar-action-button", "scan-sidebar-action-button-primary");
        scanFileButton.setMaxWidth(Double.MAX_VALUE);
        scanFileButton.setDisable(scanTarget == null);
        scanFileButton.setOnAction(event -> {
            if (scanTarget != null) {
                onScanFile.accept(scanTarget);
            }
        });

        Button scanAllButton = new Button("Scan All");
        scanAllButton.getStyleClass().add("scan-sidebar-action-button");
        scanAllButton.setMaxWidth(Double.MAX_VALUE);
        scanAllButton.setDisable(firstPendingFile == null);
        scanAllButton.setOnAction(event -> onScanAllDocuments.run());

        VBox actionRow = new VBox(8, scanFileButton, scanAllButton);
        actionRow.getStyleClass().add("scan-sidebar-action-row");

        VBox fileList = new VBox(12);
        fileList.setAlignment(Pos.TOP_CENTER);
        int displayIndex = 1;
        for (ScanningDocument document : scanningDocuments) {
            for (ScanningFile file : document.files()) {
                boolean scanned = containsScannedFile(scannedWorkspaceDocuments, file);
                boolean selected = activeScanningFile != null && activeScanningFile.reference().equals(file.reference());
                fileList.getChildren().add(scanFileSidebarThumbnail(displayIndex, file, scanned, selected, onSelectScanFile));
                displayIndex++;
            }
        }

        ScrollPane fileScroll = new ScrollPane(fileList);
        fileScroll.setFitToWidth(true);
        fileScroll.getStyleClass().add("scan-sidebar-file-scroll");
        VBox.setVgrow(fileScroll, Priority.ALWAYS);

        sidebar.getChildren().addAll(title, actionRow, fileScroll);
        return sidebar;
    }

    private VBox scanFileSidebarThumbnail(int displayIndex,
                                          ScanningFile file,
                                          boolean scanned,
                                          boolean selected,
                                          Consumer<ScanningFile> onSelectScanFile) {
        VBox preview = buildPaperPreview();
        preview.getStyleClass().add("scan-sidebar-thumbnail-paper");

        Label pageNumber = new Label("File " + displayIndex);
        pageNumber.getStyleClass().add("scan-sidebar-thumbnail-number");

        Label fileName = new Label(file.fileName());
        fileName.getStyleClass().add("scan-sidebar-thumbnail-name");
        fileName.setWrapText(true);

        Label detail = new Label(file.pages() + (file.barcode() ? " / Barcode" : ""));
        detail.getStyleClass().add(file.barcode() ? "scan-sidebar-thumbnail-barcode" : "scan-sidebar-thumbnail-detail");

        VBox row = new VBox(8, preview, pageNumber, fileName, detail);
        row.getStyleClass().add("scan-sidebar-thumbnail");
        if (selected) {
            row.getStyleClass().add("scan-sidebar-thumbnail-selected");
        }
        if (scanned) {
            row.getStyleClass().add("scan-sidebar-thumbnail-scanned");
        }
        row.setAlignment(Pos.CENTER);
        row.setOnMouseClicked(event -> onSelectScanFile.accept(file));
        return row;
    }

    private VBox buildProfileCard(ScanProfile profile,
                                  boolean selected,
                                  Consumer<ScanProfile> onSelectProfile) {
        Label title = new Label(profile.title());
        title.getStyleClass().add("profile-card-title");

        Label description = new Label(profile.description());
        description.getStyleClass().add("profile-card-description");

        VBox card = new VBox(5, title, description);
        card.getStyleClass().add(selected ? "profile-select-card-active" : "profile-select-card");
        card.setOnMouseClicked(event -> onSelectProfile.accept(profile));
        return card;
    }

    private ListCell<ScanProfile> profileComboCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ScanProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                setText(empty || profile == null ? null : profile.title());
            }
        };
    }

    private ScanProfile findProfile(String value, List<ScanProfile> profiles) {
        String search = normalizeSearch(value);
        if (search.isBlank()) {
            return null;
        }
        for (ScanProfile profile : profiles) {
            if (normalizeSearch(profile.title()).equals(search)) {
                return profile;
            }
        }
        for (ScanProfile profile : profiles) {
            if (matchesSearch(search, profile.title(), profile.description(), profile.settingsLine())) {
                return profile;
            }
        }
        return null;
    }

    private VBox buildPlaceholderPage(String titleText, String subtitleText) {
        VBox body = new VBox(14);
        body.getStyleClass().add("dashboard-body");

        Label title = new Label(titleText);
        title.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("dashboard-subtitle");

        VBox panel = new VBox(new Label("Content coming next"));
        panel.getStyleClass().add("panel-card");
        panel.setPadding(new Insets(24));

        body.getChildren().addAll(title, subtitle, panel);
        return body;
    }

    private Parent buildQaReviewBody(ScanProfile selectedProfile,
                                     String currentBoxId,
                                     List<ScanningDocument> scannedWorkspaceDocuments,
                                     ScanningDocument activeScanningDocument,
                                     ScanningFile activeScanningFile,
                                     int[] qaCurrentPage,
                                     int[] qaRotation,
                                     double[] qaZoom,
                                     double[] qaPanX,
                                     double[] qaPanY,
                                     boolean[] qaHighlight,
                                     boolean[] qaApproved,
                                     Set<String> qaCompletedDocuments,
                                     Set<String> qaHighlightedText,
                                     String selectedTiffAction,
                                     Consumer<ScanningFile> onSelectQaFile,
                                     Runnable onBackToScanning,
                                     Runnable onDashboard,
                                     Runnable onExport,
                                     Consumer<Integer> onStepPage,
                                     Runnable onRotatePage,
                                     Consumer<Double> onZoomPage,
                                     Consumer<double[]> onPanPage,
                                     Runnable onToggleHighlight,
                                     Consumer<String> onToggleTextHighlight,
                                     Runnable onApprove,
                                     Runnable onReject,
                                     Consumer<String> onSelectTiffAction,
                                     Consumer<MetadataRow> onOpenMetadataForm) {
        BorderPane body = new BorderPane();
        body.getStyleClass().addAll("dashboard-body", "qa-review-page");
        BorderPane qaMain = new BorderPane();
        qaMain.getStyleClass().add("qa-main-content");
        body.setCenter(qaMain);

        List<ScanningFile> qaFiles = flattenScannedFiles(scannedWorkspaceDocuments);
        ScanningFile selectedFile = activeScanningFile;
        if (selectedFile == null && !qaFiles.isEmpty()) {
            selectedFile = qaFiles.get(0);
        }
        ScanningDocument selectedDocument = activeScanningDocument;
        if (selectedDocument == null && selectedFile != null) {
            selectedDocument = findDocumentForFile(scannedWorkspaceDocuments, selectedFile);
        }
        if (selectedDocument == null && !scannedWorkspaceDocuments.isEmpty()) {
            selectedDocument = scannedWorkspaceDocuments.get(0);
        }
        List<ScanningFile> documentFiles = selectedDocument == null ? List.of() : selectedDocument.files();

        String profileName = selectedProfile == null ? "Profile" : selectedProfile.title().replace(" Profile", "");
        String documentName = selectedDocument == null ? "Document" : selectedDocument.title();
        String exportFileName = buildExportFileName(profileName, currentBoxId);
        boolean exportReady = !qaFiles.isEmpty();

        Label backArrow = new Label("<");
        backArrow.getStyleClass().add("qa-back-link");
        backArrow.setOnMouseClicked(event -> onBackToScanning.run());

        Label brandName = new Label("PrismScan");
        brandName.getStyleClass().add("qa-brand-name");

        HBox breadcrumbs = new HBox(10,
                new Label("Start Scan"),
                new Label(">"),
                new Label(profileName),
                new Label(">"),
                new Label(currentBoxId.isBlank() ? "Box" : currentBoxId),
                new Label(">"),
                new Label(documentName)
        );
        breadcrumbs.getStyleClass().add("qa-breadcrumbs");
        breadcrumbs.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(2, brandName, breadcrumbs);
        titleBlock.getStyleClass().add("qa-title-block");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Label helpButton = new Label("?");
        helpButton.getStyleClass().add("qa-help-button");
        HBox headerRow = new HBox(14, backArrow, titleBlock, headerSpacer, helpButton);
        headerRow.getStyleClass().add("qa-app-header");
        headerRow.setAlignment(Pos.CENTER_LEFT);

        if (qaFiles.isEmpty() || selectedFile == null) {
            VBox emptyPanel = new VBox(new Label("No scanned files available for QA yet."));
            emptyPanel.getStyleClass().add("panel-card");
            emptyPanel.setPadding(new Insets(22));
            qaMain.setTop(headerRow);
            qaMain.setCenter(emptyPanel);
            return body;
        }

        VBox documentsPanel = new VBox(10);
        documentsPanel.getStyleClass().addAll("qa-file-list-panel", "qa-documents-panel");
        Label documentsTitle = new Label("Documents");
        documentsTitle.getStyleClass().add("panel-title");
        Label documentsCount = new Label(String.valueOf(scannedWorkspaceDocuments.size()));
        documentsCount.getStyleClass().add("qa-count-pill");
        HBox documentsHeader = new HBox(8, documentsTitle, documentsCount);
        documentsHeader.setAlignment(Pos.CENTER_LEFT);
        FlowPane documentRows = new FlowPane(12, 12);
        documentRows.setAlignment(Pos.TOP_LEFT);
        documentRows.setPrefWrapLength(188);
        for (ScanningDocument document : scannedWorkspaceDocuments) {
            documentRows.getChildren().add(buildQaScanningDocumentCard(
                    document,
                    selectedDocument != null && document.title().equals(selectedDocument.title()),
                    documentQaStatus(document, selectedDocument, qaCompletedDocuments),
                    () -> {
                        if (!document.files().isEmpty()) {
                            onSelectQaFile.accept(document.files().get(0));
                        }
                    }
            ));
        }
        ScrollPane documentScroll = new ScrollPane(documentRows);
        documentScroll.setFitToWidth(true);
        documentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        documentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        documentScroll.getStyleClass().add("scan-document-scroll");
        VBox.setVgrow(documentScroll, Priority.ALWAYS);
        documentsPanel.getChildren().addAll(documentsHeader, documentScroll);

        VBox filesPanel = new VBox(10);
        filesPanel.getStyleClass().addAll("qa-file-list-panel", "qa-files-panel");
        Label filesTitle = new Label("Files in " + (selectedDocument == null ? "Document" : selectedDocument.title()));
        filesTitle.getStyleClass().add("panel-title");
        Label filesCount = new Label(String.valueOf(documentFiles.size()));
        filesCount.getStyleClass().add("qa-count-pill");
        HBox filesHeader = new HBox(8, filesTitle, filesCount);
        filesHeader.setAlignment(Pos.CENTER_LEFT);
        FlowPane fileRows = new FlowPane(12, 12);
        fileRows.getStyleClass().add("qa-mini-file-list");
        fileRows.setAlignment(Pos.TOP_LEFT);
        fileRows.setPrefWrapLength(206);
        int fileIndex = 1;
        for (ScanningFile file : documentFiles) {
            fileRows.getChildren().add(buildQaScanningFileCard(
                    file,
                    fileIndex++,
                    selectedFile != null && file.reference().equals(selectedFile.reference()),
                    () -> onSelectQaFile.accept(file)
            ));
        }
        ScrollPane fileScroll = new ScrollPane(fileRows);
        fileScroll.setFitToWidth(true);
        fileScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fileScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        fileScroll.getStyleClass().add("scan-document-scroll");
        VBox.setVgrow(fileScroll, Priority.ALWAYS);
        filesPanel.getChildren().addAll(filesHeader, fileScroll);
        VBox.setVgrow(filesPanel, Priority.ALWAYS);

        VBox leftColumn = new VBox(10, documentsPanel, filesPanel);
        leftColumn.getStyleClass().add("qa-left-column");
        leftColumn.setPrefWidth(240);
        leftColumn.setMinWidth(240);
        leftColumn.setMaxWidth(240);
        leftColumn.setMaxHeight(Double.MAX_VALUE);

        VBox previewPaper = buildQaPreviewPaper(qaCurrentPage[0], qaHighlight[0], qaHighlightedText, onToggleTextHighlight);
        previewPaper.setRotate(qaRotation[0]);
        previewPaper.setScaleX(qaZoom[0]);
        previewPaper.setScaleY(qaZoom[0]);
        previewPaper.setTranslateX(qaPanX[0]);
        previewPaper.setTranslateY(qaPanY[0]);
        StackPane previewStage = new StackPane(previewPaper);
        previewStage.getStyleClass().add("qa-preview-stage");
        Rectangle previewClip = new Rectangle();
        previewClip.arcWidthProperty().set(24);
        previewClip.arcHeightProperty().set(24);
        previewClip.widthProperty().bind(previewStage.widthProperty());
        previewClip.heightProperty().bind(previewStage.heightProperty());
        previewStage.setClip(previewClip);
        VBox.setVgrow(previewStage, Priority.ALWAYS);

        Button highlightButton = qaToolbarButton("\u270E");
        highlightButton.getStyleClass().add("qa-highlight-button");
        if (qaHighlight[0]) {
            highlightButton.getStyleClass().add("qa-toolbar-button-active");
        }
        highlightButton.setOnAction(event -> onToggleHighlight.run());
        Button zoomOutButton = qaToolbarButton("-");
        zoomOutButton.setDisable(qaZoom[0] <= 0.48);
        zoomOutButton.setOnAction(event -> onZoomPage.accept(-0.1));
        Button zoomInButton = qaToolbarButton("+");
        zoomInButton.setDisable(qaZoom[0] >= 1.22);
        zoomInButton.setOnAction(event -> onZoomPage.accept(0.1));
        boolean panDisabled = qaZoom[0] <= 0.78;
        Button panLeftButton = qaToolbarButton("\u2190");
        panLeftButton.getStyleClass().add("qa-pan-button");
        panLeftButton.setDisable(panDisabled);
        panLeftButton.setOnAction(event -> onPanPage.accept(new double[]{30, 0}));
        Button panRightButton = qaToolbarButton("\u2192");
        panRightButton.getStyleClass().add("qa-pan-button");
        panRightButton.setDisable(panDisabled);
        panRightButton.setOnAction(event -> onPanPage.accept(new double[]{-30, 0}));
        Button panUpButton = qaToolbarButton("\u2191");
        panUpButton.getStyleClass().add("qa-pan-button");
        panUpButton.setDisable(panDisabled);
        panUpButton.setOnAction(event -> onPanPage.accept(new double[]{0, 30}));
        Button panDownButton = qaToolbarButton("\u2193");
        panDownButton.getStyleClass().add("qa-pan-button");
        panDownButton.setDisable(panDisabled);
        panDownButton.setOnAction(event -> onPanPage.accept(new double[]{0, -30}));
        Button rotateButton = qaPagerButton("Rotate", onRotatePage);
        rotateButton.getStyleClass().add("qa-preview-rotate-button");

        HBox toolbar = new HBox(8,
                highlightButton,
                zoomOutButton,
                zoomInButton,
                qaToolbarSelect(Math.round((qaZoom[0] / 0.78) * 100) + "%"),
                panLeftButton,
                panRightButton,
                panUpButton,
                panDownButton,
                rotateButton
        );
        toolbar.getStyleClass().add("qa-toolbar");

        VBox bottomStrip = buildQaBottomFileStrip(documentName, documentFiles, selectedFile, onSelectQaFile);

        VBox previewColumn = new VBox(0, toolbar, previewStage, bottomStrip);
        previewColumn.getStyleClass().add("qa-preview-column");
        HBox.setHgrow(previewColumn, Priority.ALWAYS);

        VBox selectionPanel = qaSidePanel("Current Selection",
                qaMetaBlock(profileName, currentBoxId.isBlank() ? "Box" : currentBoxId),
                qaMetaBlock("Date", "May 15, 2024 10:15 AM"),
                qaMetaBlock("Document", selectedDocument == null ? "-" : selectedDocument.title()),
                qaMetaBlock("File", selectedFile.badge()),
                qaMetaBlock("Reference ID", selectedFile.reference().replace("Ref: ", "")),
                qaMetaBlock("Filename", selectedFile.fileName()),
                qaMetaBlock("Pages", selectedFile.pages())
        );

        Button rejectButton = qaActionButton("Reject, Send Back", true, onReject);
        VBox toolsPanel = qaUntitledSidePanel(rejectButton);

        VBox exportFilenamePanel = qaExportFilenamePanel(exportFileName);
        VBox actionsPanel = qaUntitledSidePanel(exportFilenamePanel);

        Button exportDocumentButton = new Button("Report Document");
        SVGPath exportIcon = new SVGPath();
        exportIcon.setContent("M12 3V15 M7 10L12 15L17 10 M5 21H19");
        exportIcon.getStyleClass().add("qa-export-button-icon");
        exportDocumentButton.setGraphic(exportIcon);
        exportDocumentButton.setGraphicTextGap(8);
        exportDocumentButton.getStyleClass().addAll("approve-button", "qa-export-document-button");
        exportDocumentButton.setMaxWidth(Double.MAX_VALUE);
        exportDocumentButton.setDisable(!exportReady);
        exportDocumentButton.setOnAction(event -> {
            onApprove.run();
            onExport.run();
        });

        ScanningDocument metadataDocument = selectedDocument;
        ScanningFile metadataFile = selectedFile;
        Button createMetadataButton = new Button("Create Metadata");
        createMetadataButton.getStyleClass().addAll("qa-side-action-button", "qa-create-metadata-button");
        createMetadataButton.setMaxWidth(Double.MAX_VALUE);
        createMetadataButton.setDisable(metadataFile == null);
        createMetadataButton.setOnAction(event -> onOpenMetadataForm.accept(new MetadataRow(
                profileName,
                currentBoxId.isBlank() ? "Box" : currentBoxId,
                metadataFile.badge(),
                metadataDocument == null ? "Document" : metadataDocument.title(),
                false
        )));

        HBox sidePager = new HBox(10,
                qaPagerButton("<", () -> onStepPage.accept(-1)),
                new Label(qaCurrentPage[0] + " / " + selectedFile.pageCount()),
                qaPagerButton(">", () -> onStepPage.accept(1))
        );
        sidePager.getStyleClass().add("qa-side-pager");
        sidePager.setAlignment(Pos.CENTER);

        Region rightSpacer = new Region();
        VBox.setVgrow(rightSpacer, Priority.ALWAYS);
        VBox rightColumn = new VBox(10, selectionPanel, toolsPanel, actionsPanel, rightSpacer, sidePager, createMetadataButton, exportDocumentButton);
        rightColumn.getStyleClass().add("qa-right-column");
        rightColumn.setPrefWidth(230);
        rightColumn.setMinWidth(230);
        rightColumn.setMaxWidth(230);

        HBox workspace = new HBox(0, leftColumn, previewColumn, rightColumn);
        workspace.getStyleClass().add("qa-workspace");
        workspace.setMinHeight(0);
        workspace.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(previewColumn, Priority.ALWAYS);

        qaMain.setTop(headerRow);
        qaMain.setCenter(workspace);
        BorderPane.setAlignment(workspace, Pos.TOP_LEFT);
        return body;
    }

    private VBox buildQaBottomFileStrip(String documentName,
                                        List<ScanningFile> documentFiles,
                                        ScanningFile selectedFile,
                                        Consumer<ScanningFile> onSelectQaFile) {
        Label title = new Label("FILES IN " + documentName.toUpperCase());
        title.getStyleClass().add("qa-bottom-title");
        Label count = new Label(String.valueOf(documentFiles.size()));
        count.getStyleClass().add("qa-count-pill");
        HBox header = new HBox(8, title, count);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox thumbnails = new HBox(10);
        thumbnails.getStyleClass().add("qa-bottom-thumbnail-row");
        for (ScanningFile file : documentFiles) {
            boolean selected = selectedFile != null && file.reference().equals(selectedFile.reference());
            thumbnails.getChildren().add(buildQaBottomFileCard(file, selected, () -> onSelectQaFile.accept(file)));
        }

        ScrollPane scroller = new ScrollPane(thumbnails);
        scroller.setFitToHeight(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.getStyleClass().add("qa-bottom-file-scroll");

        VBox strip = new VBox(8, header, scroller);
        strip.getStyleClass().add("qa-bottom-file-strip");
        return strip;
    }

    private VBox buildQaBottomFileCard(ScanningFile file, boolean selected, Runnable onSelect) {
        VBox preview = buildPaperPreview();
        preview.getStyleClass().add("qa-bottom-file-preview");
        StackPane previewWrap = new StackPane(preview);
        previewWrap.getStyleClass().add("qa-bottom-preview-wrap");
        if (file.barcode()) {
            Label barcode = new Label("BARCODE");
            barcode.getStyleClass().add("qa-bottom-barcode");
            StackPane.setAlignment(barcode, Pos.BOTTOM_CENTER);
            previewWrap.getChildren().add(barcode);
        }

        Label name = new Label(ellipsize(file.fileName(), 16));
        name.getStyleClass().add("qa-bottom-file-name");
        Label pages = new Label(file.pages());
        pages.getStyleClass().add("qa-bottom-file-pages");

        VBox card = new VBox(6, previewWrap, name, pages);
        card.getStyleClass().add(selected ? "qa-bottom-file-card-selected" : "qa-bottom-file-card");
        card.setOnMouseClicked(event -> onSelect.run());
        return card;
    }

    private VBox buildQaFileRow(ScanningFile file,
                                boolean selected,
                                Consumer<ScanningFile> onSelectQaFile) {
        Label title = new Label(file.fileName());
        title.getStyleClass().add("paper-title");
        title.setWrapText(true);
        Label badge = new Label(file.badge());
        badge.getStyleClass().add("file-badge");
        Label reference = new Label("Reference ID: " + file.reference().replace("Ref: ", ""));
        reference.getStyleClass().add("scan-file-reference");
        Label pageCount = new Label(file.pages());
        pageCount.getStyleClass().add("muted");
        Label type = new Label(file.barcode() ? "Barcode detected - new document" : "");
        type.getStyleClass().add(file.barcode() ? "barcode-text" : "muted");
        VBox preview = buildPaperPreview();

        VBox card = new VBox(8, badge, preview, title, reference, pageCount, type);
        card.getStyleClass().add("scan-file-card");
        card.getStyleClass().add("qa-sidebar-file-card");
        card.getStyleClass().add("qa-scan-file-card");
        if (file.barcode()) {
            card.getStyleClass().add("scan-file-card-barcode");
        }
        if (selected) {
            card.getStyleClass().add("qa-sidebar-file-selected");
        }
        card.setOnMouseClicked(event -> onSelectQaFile.accept(file));

        VBox wrap = new VBox(card);
        wrap.getStyleClass().add("qa-sidebar-file-wrap");
        wrap.setAlignment(Pos.TOP_CENTER);
        return wrap;
    }

    private String ellipsize(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private VBox qaMetaBlock(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("qa-side-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("qa-side-value");
        return new VBox(2, label, value);
    }

    private VBox buildQaScanningDocumentCard(ScanningDocument document,
                                             boolean selected,
                                             String status,
                                             Runnable onSelect) {
        VBox card = scanningDocumentItem(document, selected, ignored -> {
        });
        card.getStyleClass().add("qa-document-card");
        card.getChildren().add(qaStatusBadge(status));
        card.setOnMouseClicked(event -> onSelect.run());
        return card;
    }

    private VBox buildQaScanningFileCard(ScanningFile file,
                                         int displayIndex,
                                         boolean selected,
                                         Runnable onSelect) {
        Label badge = new Label("File " + displayIndex);
        badge.getStyleClass().add("file-badge");
        VBox preview = buildPaperPreview();
        Label name = new Label(file.fileName());
        name.setWrapText(true);
        name.getStyleClass().add("paper-title");
        Label ref = new Label("Reference ID: " + file.reference().replace("Ref: ", ""));
        ref.getStyleClass().add("scan-file-reference");
        Label pageCount = new Label(file.pages());
        pageCount.getStyleClass().add("muted");

        VBox card;
        if (file.barcode()) {
            Label type = new Label("Barcode detected - new document");
            type.getStyleClass().add("barcode-text");
            card = new VBox(8, badge, preview, name, ref, pageCount, type);
        } else {
            card = new VBox(8, badge, preview, name, ref, pageCount);
        }
        card.getStyleClass().add("scan-file-card");
        card.getStyleClass().add("qa-sidebar-file-card");
        card.getStyleClass().add("qa-scan-file-card");
        if (file.barcode()) {
            card.getStyleClass().add("scan-file-card-barcode");
        }
        if (selected) {
            card.getStyleClass().add("qa-sidebar-file-selected");
        }
        card.setOnMouseClicked(event -> onSelect.run());
        return card;
    }

    private Button qaToolbarButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("qa-toolbar-button");
        return button;
    }

    private ComboBox<String> qaToolbarSelect(String value) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(value);
        combo.getSelectionModel().selectFirst();
        combo.getStyleClass().add("qa-toolbar-select");
        return combo;
    }

    private Button qaPagerButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("qa-pager-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private VBox qaSidePanel(String titleText, Node... items) {
        Label title = new Label(titleText);
        title.getStyleClass().add("panel-title");
        VBox panel = new VBox(8);
        panel.getStyleClass().add("qa-side-panel");
        panel.getChildren().add(title);
        panel.getChildren().addAll(items);
        return panel;
    }

    private VBox qaUntitledSidePanel(Node... items) {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("qa-side-panel");
        panel.getChildren().addAll(items);
        return panel;
    }

    private Button qaActionButton(String text, boolean destructive, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(destructive ? "reject-button" : "page-window-cancel-button");
        button.getStyleClass().add("qa-side-action-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    private VBox qaFooterStat(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("qa-footer-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("qa-footer-value");
        VBox box = new VBox(8, label, value);
        box.getStyleClass().add("qa-footer-card");
        return box;
    }

    private VBox qaProgressStat(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("qa-footer-label");
        Region track = new Region();
        track.getStyleClass().add("qa-progress-track");
        Region fill = new Region();
        fill.getStyleClass().add("qa-progress-fill");
        StackPane progress = new StackPane(track, fill);
        progress.getStyleClass().add("qa-progress-wrap");
        Label value = new Label(valueText);
        value.getStyleClass().add("qa-progress-value");
        VBox box = new VBox(8, label, progress, value);
        box.getStyleClass().add("qa-footer-card");
        return box;
    }

    private VBox qaFooterControl(String labelText, ComboBox<String> comboBox) {
        Label label = new Label(labelText);
        label.getStyleClass().add("qa-footer-label");
        VBox box = new VBox(8, label, comboBox);
        box.getStyleClass().add("qa-footer-card");
        return box;
    }

    private VBox qaExportFilenamePanel(String fileName) {
        Label label = new Label("Export filename:");
        label.getStyleClass().add("qa-export-filename-label");
        Label value = new Label(fileName);
        value.getStyleClass().add("qa-export-filename-value");
        value.setWrapText(true);

        VBox panel = new VBox(4, label, value);
        panel.getStyleClass().add("qa-export-filename-panel");
        panel.setMaxWidth(Double.MAX_VALUE);
        return panel;
    }

    private String buildExportFileName(String profileName, String boxId) {
        String safeProfileName = profileName == null || profileName.isBlank()
                ? "Profile"
                : profileName.replaceAll("\\s+", "");
        String safeBoxId = boxId == null || boxId.isBlank() ? "Box" : boxId.strip();
        return safeProfileName + "_" + safeBoxId + ".pdf";
    }

    private VBox buildQaPreviewPaper(int pageNumber,
                                     boolean highlightMode,
                                     Set<String> highlightedText,
                                     Consumer<String> onToggleTextHighlight) {
        Label logo = new Label("C");
        logo.getStyleClass().add("qa-invoice-logo");
        Label companyName = qaPreviewText("company-name", "Acme Corporation", highlightMode, highlightedText, onToggleTextHighlight);
        companyName.getStyleClass().add("qa-invoice-strong");
        VBox company = new VBox(2,
                companyName,
                qaPreviewText("company-address-1", "123 Business Rd.", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("company-address-2", "Suite 100", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("company-address-3", "New York, NY 10001", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("company-phone", "(212) 555-0199", highlightMode, highlightedText, onToggleTextHighlight)
        );
        company.getStyleClass().add("qa-invoice-company");

        Label invoiceTitle = new Label("INVOICE");
        invoiceTitle.getStyleClass().add("qa-invoice-title");
        GridPane invoiceDetails = new GridPane();
        invoiceDetails.setHgap(16);
        invoiceDetails.setVgap(6);
        invoiceDetails.add(new Label("Invoice #:"), 0, 0);
        invoiceDetails.add(qaPreviewText("invoice-number", "INV-1001", highlightMode, highlightedText, onToggleTextHighlight), 1, 0);
        invoiceDetails.add(new Label("Invoice Date:"), 0, 1);
        invoiceDetails.add(qaPreviewText("invoice-date", "May 01, 2024", highlightMode, highlightedText, onToggleTextHighlight), 1, 1);
        invoiceDetails.add(new Label("Due Date:"), 0, 2);
        invoiceDetails.add(qaPreviewText("due-date", "May 31, 2024", highlightMode, highlightedText, onToggleTextHighlight), 1, 2);
        invoiceDetails.getStyleClass().add("qa-invoice-details");
        VBox invoiceBlock = new VBox(16, invoiceTitle, invoiceDetails);
        invoiceBlock.setAlignment(Pos.TOP_RIGHT);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(12, logo, company, topSpacer, invoiceBlock);
        topRow.setAlignment(Pos.TOP_LEFT);

        Label billToTitle = new Label("Bill To:");
        billToTitle.getStyleClass().add("qa-invoice-strong");
        VBox billTo = new VBox(3,
                billToTitle,
                qaPreviewText("bill-company", "ABC Company", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("bill-address-1", "456 Market St.", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("bill-address-2", "San Francisco, CA 94105", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("bill-phone", "(415) 555-0123", highlightMode, highlightedText, onToggleTextHighlight)
        );
        billTo.getStyleClass().add("qa-invoice-bill");

        GridPane invoiceTable = new GridPane();
        invoiceTable.getStyleClass().add("qa-invoice-table");
        Label consultingDescription = qaPreviewText("line-consulting", "Consulting Services", highlightMode, highlightedText, onToggleTextHighlight);
        Label softwareDescription = qaPreviewText("line-software", "Software License", highlightMode, highlightedText, onToggleTextHighlight);
        Label supportDescription = qaPreviewText("line-support", "Support & Maintenance", highlightMode, highlightedText, onToggleTextHighlight);
        invoiceTable.add(new Label("Description"), 0, 0);
        invoiceTable.add(new Label("Quantity"), 1, 0);
        invoiceTable.add(new Label("Unit Price"), 2, 0);
        invoiceTable.add(new Label("Amount"), 3, 0);
        invoiceTable.add(consultingDescription, 0, 1);
        invoiceTable.add(new Label("10"), 1, 1);
        invoiceTable.add(new Label("$150.00"), 2, 1);
        invoiceTable.add(new Label("$1,500.00"), 3, 1);
        invoiceTable.add(softwareDescription, 0, 2);
        invoiceTable.add(new Label("2"), 1, 2);
        invoiceTable.add(new Label("$500.00"), 2, 2);
        invoiceTable.add(new Label("$1,000.00"), 3, 2);
        invoiceTable.add(supportDescription, 0, 3);
        invoiceTable.add(new Label("1"), 1, 3);
        invoiceTable.add(new Label("$250.00"), 2, 3);
        invoiceTable.add(new Label("$250.00"), 3, 3);
        invoiceTable.getColumnConstraints().addAll(column(140), column(52), column(68), column(72));

        Label totalLabel = new Label("Total          $2,990.63");
        totalLabel.getStyleClass().add("qa-invoice-strong");
        VBox totalBlock = new VBox(4,
                qaPreviewText("subtotal", "Subtotal        $2,750.00", highlightMode, highlightedText, onToggleTextHighlight),
                qaPreviewText("tax", "Tax (8.75%)       $240.63", highlightMode, highlightedText, onToggleTextHighlight),
                totalLabel
        );
        totalBlock.getStyleClass().add("qa-invoice-total");
        totalBlock.setAlignment(Pos.CENTER_RIGHT);

        Region bodySpacer = new Region();
        VBox.setVgrow(bodySpacer, Priority.ALWAYS);
        Label notes = new Label("Notes:\nThank you for your business!");
        notes.getStyleClass().add("qa-invoice-notes");
        Label pageFooter = new Label("Page " + pageNumber + " of 1");
        pageFooter.getStyleClass().add("qa-invoice-footer");
        pageFooter.setMaxWidth(Double.MAX_VALUE);
        pageFooter.setAlignment(Pos.CENTER);

        VBox paper = new VBox(22, topRow, billTo, invoiceTable, totalBlock, bodySpacer, notes, pageFooter);
        paper.getStyleClass().add("qa-preview-paper");
        paper.setAlignment(Pos.TOP_LEFT);
        return paper;
    }

    private Label qaPreviewText(String id,
                                String text,
                                boolean highlightMode,
                                Set<String> highlightedText,
                                Consumer<String> onToggleTextHighlight) {
        Label label = new Label(text);
        if (highlightedText.contains(id)) {
            label.getStyleClass().add("qa-text-highlight");
        }
        if (highlightMode) {
            label.getStyleClass().add("qa-highlight-target");
            label.setOnMouseClicked(event -> onToggleTextHighlight.accept(id));
        }
        return label;
    }

    private VBox scanningDocumentItem(ScanningDocument document,
                                      boolean selected,
                                      Consumer<ScanningDocument> onSelectScanningDocument) {
        VBox iconGlyph = new VBox(2);
        iconGlyph.getStyleClass().add("scan-document-glyph");
        Label iconCorner = new Label("");
        iconCorner.getStyleClass().add("scan-document-corner");
        Label iconLineOne = new Label("");
        iconLineOne.getStyleClass().add("scan-document-line");
        Label iconLineTwo = new Label("");
        iconLineTwo.getStyleClass().add("scan-document-line-short");
        iconGlyph.getChildren().addAll(iconCorner, iconLineOne, iconLineTwo);

        StackPane iconWrap = new StackPane(iconGlyph);
        iconWrap.getStyleClass().add("scan-document-icon-wrap");

        Label title = new Label(document.title());
        title.getStyleClass().add("scan-document-title");
        title.setWrapText(true);
        Label count = new Label(document.fileCountText());
        count.getStyleClass().add("scan-document-count");

        VBox text = new VBox(2, title, count);
        text.setAlignment(Pos.CENTER);
        VBox row = new VBox(8, iconWrap, text);
        row.getStyleClass().addAll("scan-document-row", selected ? "scan-document-row-selected" : "scan-document-row-plain");
        row.setAlignment(Pos.TOP_CENTER);
        row.setOnDragDetected(event -> {
            Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(document.title());
            dragboard.setContent(content);
            event.consume();
        });
        return row;
    }

    private VBox compactScanBlock(String labelText, String valueText) {
        VBox card = new VBox(3);
        card.getStyleClass().add("scan-top-block");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(58);
        card.setPrefHeight(58);
        card.setMaxHeight(58);
        Label label = new Label(labelText);
        label.getStyleClass().add("scan-top-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("scan-top-value");
        card.getChildren().addAll(label, value);
        return card;
    }

    private VBox compactScanComboBlock(String labelText, ComboBox<String> comboBox) {
        VBox card = new VBox(3);
        card.getStyleClass().add("scan-top-block");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(58);
        card.setPrefHeight(58);
        card.setMaxHeight(58);
        Label label = new Label(labelText);
        label.getStyleClass().add("scan-top-label");
        card.getChildren().addAll(label, comboBox);
        return card;
    }

    private VBox documentSection(String titleText, String status, VBox... cards) {
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        HBox header = new HBox(8, title, qaStatusBadge(status));
        header.setAlignment(Pos.CENTER_LEFT);
        FlowPane flow = new FlowPane(12, 12);
        flow.getChildren().addAll(cards);
        VBox section = new VBox(8, header, flow);
        section.getStyleClass().add("document-section");
        return section;
    }

    private String documentQaStatus(ScanningDocument document,
                                    ScanningDocument activeDocument,
                                    Set<String> qaCompletedDocuments) {
        if (document != null && qaCompletedDocuments.contains(document.title())) {
            return "QA Completed";
        }
        if (document != null && activeDocument != null && document.title().equals(activeDocument.title())) {
            return "In Progress";
        }
        return "Waiting for QA";
    }

    private Label qaStatusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("document-status-pill");
        if ("QA Completed".equals(status)) {
            badge.getStyleClass().add("document-status-completed");
        } else if ("In Progress".equals(status)) {
            badge.getStyleClass().add("document-status-progress");
        } else {
            badge.getStyleClass().add("document-status-waiting");
        }
        return badge;
    }

    private VBox buildFileCard(ScanningDocument document,
                               ScanningFile file,
                               int index,
                               Consumer<ScanningFile> onOpenScanningFile,
                               Runnable onScanningChanged) {
        Label badge = new Label(file.badge());
        badge.getStyleClass().add("file-badge");
        VBox preview = buildPaperPreview();
        Label name = new Label(file.fileName());
        name.setWrapText(true);
        name.getStyleClass().add("paper-title");
        Label ref = new Label("Reference ID: " + file.reference().replace("Ref: ", ""));
        ref.getStyleClass().add("scan-file-reference");
        Label pageCount = new Label(file.pages());
        pageCount.getStyleClass().add("muted");
        VBox card;
        if (file.barcode()) {
            Label type = new Label("Barcode detected - new document");
            type.getStyleClass().add("barcode-text");
            card = new VBox(8, badge, preview, name, ref, pageCount, type);
        } else {
            card = new VBox(8, badge, preview, name, ref, pageCount);
        }
        card.getStyleClass().add("scan-file-card");
        if (file.barcode()) {
            card.getStyleClass().add("scan-file-card-barcode");
        }
        enableSmoothFileDrag(card, document, file, ignored -> openFilePagesWindow(document, file), onScanningChanged);
        animateFileCard(card);
        return card;
    }

    private VBox buildFilePagesBody(ScanProfile selectedProfile,
                                    String currentBoxId,
                                    ScanningDocument activeScanningDocument,
                                    ScanningFile activeScanningFile,
                                    Runnable onBackToScanning) {
        VBox body = new VBox(12);
        body.getStyleClass().add("dashboard-body");

        Label heading = new Label("File Pages");
        heading.getStyleClass().add("dashboard-heading");
        String context = activeScanningFile == null
                ? "No file selected"
                : selectedProfile.title() + " / " + currentBoxId + " / " + activeScanningFile.fileName();
        Label subtitle = new Label(context);
        subtitle.getStyleClass().add("dashboard-subtitle");

        Button backButton = new Button("Back To Scanning");
        backButton.getStyleClass().add("start-scanning-button");
        backButton.setOnAction(event -> onBackToScanning.run());

        VBox panel = new VBox(14);
        panel.getStyleClass().add("scan-main-panel");

        Label detailsTitle = new Label(activeScanningDocument == null || activeScanningFile == null
                ? "Pages"
                : activeScanningDocument.title() + " / " + activeScanningFile.reference().replace("Ref: ", ""));
        detailsTitle.getStyleClass().add("panel-title");

        FlowPane pages = new FlowPane(12, 12);
        if (activeScanningFile != null) {
            List<Integer> pageNumbers = new ArrayList<>();
            for (int i = 1; i <= activeScanningFile.pageCount(); i++) {
                pageNumbers.add(i);
            }
            Runnable[] renderPages = new Runnable[1];
            renderPages[0] = () -> {
                pages.getChildren().clear();
                for (Integer pageNumber : pageNumbers) {
                    pages.getChildren().add(buildPagePreviewCard(pageNumber, pageNumbers, renderPages));
                }
            };
            renderPages[0].run();
        }

        panel.getChildren().addAll(detailsTitle, pages);
        body.getChildren().addAll(heading, subtitle, backButton, panel);
        return body;
    }

    private VBox buildPagePreviewCard(int pageNumber, List<Integer> workingPages, Runnable[] renderPages) {
        VBox preview = buildLargePaperPreview();
        Label title = new Label("Page " + pageNumber);
        title.getStyleClass().add("paper-title");

        VBox card = new VBox(8, preview, title);
        card.getStyleClass().add("scan-page-card");
        enableSmoothPageDrag(card, pageNumber, workingPages, renderPages);
        animateFileCard(card);
        return card;
    }

    private VBox buildPaperPreview() {
        Label corner = new Label("");
        corner.getStyleClass().add("paper-corner");
        Label lineOne = new Label("");
        lineOne.getStyleClass().add("paper-line");
        Label lineTwo = new Label("");
        lineTwo.getStyleClass().add("paper-line");
        Label lineThree = new Label("");
        lineThree.getStyleClass().add("paper-line-short");

        VBox preview = new VBox(7, corner, lineOne, lineTwo, lineThree);
        preview.getStyleClass().add("paper-preview");
        return preview;
    }

    private VBox buildActionCard(ActionCard card, Consumer<PageState> onNavigate) {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("dashboard-card");
        HBox.setHgrow(panel, Priority.ALWAYS);
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setOnMouseClicked(event -> onNavigate.accept(actionCardTarget(card.title())));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane badge = buildActionIconBadge(card);

        Label title = new Label(card.title());
        title.getStyleClass().add("dashboard-card-title");
        Label subtitle = new Label(card.subtitle());
        subtitle.getStyleClass().add("dashboard-card-subtitle");
        subtitle.setWrapText(true);

        header.getChildren().addAll(badge, title);
        panel.getChildren().addAll(header, subtitle);
        return panel;
    }

    private StackPane buildActionIconBadge(ActionCard card) {
        StackPane badge = new StackPane();
        badge.getStyleClass().add(card.accent() ? "dashboard-card-badge-accent" : "dashboard-card-badge");

        SVGPath icon = new SVGPath();
        icon.getStyleClass().add(card.accent() ? "dashboard-card-icon-accent" : "dashboard-card-icon");
        icon.setContent(actionIconPath(card.title()));

        badge.getChildren().add(icon);
        return badge;
    }

    private String actionIconPath(String title) {
        return switch (title) {
            case "Start Scan" -> "M7 3H5A2 2 0 0 0 3 5V7 M17 3H19A2 2 0 0 1 21 5V7 M21 17V19A2 2 0 0 1 19 21H17 M7 21H5A2 2 0 0 1 3 19V17 M8 12H16";
            case "Scans", "My Scans" -> "M14 2H6A2 2 0 0 0 4 4V20A2 2 0 0 0 6 22H18A2 2 0 0 0 20 20V8L14 2Z M14 2V8H20 M8 13H16 M8 17H14";
            case "Metadata" -> "M4 5H20V19H4Z M8 9H16 M8 13H16 M8 17H13";
            case "Exports" -> "M21 8V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V8 M1 3H23V8H1Z M10 12H14";
            default -> "M12 5V19 M5 12H19";
        };
    }

    private PageState actionCardTarget(String title) {
        return switch (title) {
            case "Start Scan" -> PageState.PROFILES;
            case "Metadata" -> PageState.METADATA;
            case "Exports" -> PageState.LOGS;
            case "Scans", "My Scans" -> PageState.MY_SCANS;
            default -> PageState.DASHBOARD;
        };
    }

    private VBox buildRecentScansPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().addAll("panel-card", "recent-scans-panel");
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label title = new Label("Recent Scans");
        title.getStyleClass().add("panel-title");
        VBox header = new VBox(title);
        header.getStyleClass().add("dashboard-panel-header");

        GridPane table = new GridPane();
        table.getStyleClass().add("recent-table");
        table.setMaxWidth(Double.MAX_VALUE);
        table.getColumnConstraints().addAll(
                column(190),
                column(180),
                column(160),
                column(150),
                column(90)
        );

        table.add(headerCell("Box ID"), 0, 0);
        table.add(headerCell("Profile"), 1, 0);
        table.add(headerCell("Status"), 2, 0);
        table.add(headerCell("Date"), 3, 0);
        table.add(headerCell("Pages"), 4, 0);

        int row = 1;
        for (ScanRow scan : recentScans()) {
            table.add(bodyCell(scan.boxId()), 0, row);
            table.add(bodyCell(scan.profile()), 1, row);
            table.add(statusCell(scan.status()), 2, row);
            table.add(bodyCell(scan.date()), 3, row);
            table.add(bodyCell(String.valueOf(scan.pages())), 4, row);
            row++;
        }

        panel.getChildren().addAll(header, table);
        return panel;
    }

    private VBox buildRightColumn() {
        VBox column = new VBox(14, buildActiveProfilePanel(), buildExportNamingPanel());
        column.setPrefWidth(320);
        column.setMaxWidth(320);
        return column;
    }

    private VBox buildActiveProfilePanel() {
        ProfileSummary profile = activeProfile();

        VBox panel = new VBox(18);
        panel.getStyleClass().addAll("panel-card", "active-profile-panel");
        panel.setPadding(new Insets(22));

        Label title = new Label("Active Profile");
        title.getStyleClass().add("panel-title");

        panel.getChildren().addAll(
                title,
                infoBlock("Profile Name", profile.profileName()),
                infoBlock("Auto-Rotate", profile.autoRotate()),
                infoBlock("Brightness", profile.brightness()),
                infoBlock("Remove Blank Pages", profile.removeBlankPages())
        );
        return panel;
    }

    private VBox buildExportNamingPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("export-naming-panel");
        panel.setPadding(new Insets(18));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("!");
        icon.getStyleClass().add("export-naming-icon");
        Label title = new Label("Export Naming");
        title.getStyleClass().add("export-naming-title");
        header.getChildren().addAll(icon, title);

        Label formatLabel = new Label("Format:");
        formatLabel.getStyleClass().add("info-panel-text");
        Label token = new Label("{profileName}_{boxId}");
        token.getStyleClass().add("info-token");

        VBox format = new VBox(8, formatLabel, token);
        format.setPadding(new Insets(0, 0, 0, 36));
        panel.getChildren().addAll(header, format);
        return panel;
    }

    private VBox infoBlock(String labelText, String valueText) {
        VBox block = new VBox(4);
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("field-value");
        block.getChildren().addAll(label, value);
        return block;
    }

    private ColumnConstraints column(double width) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPrefWidth(width);
        constraints.setHgrow(Priority.ALWAYS);
        return constraints;
    }

    private Label headerCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-header-cell");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label bodyCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-body-cell");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label myScansHeaderCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("my-scans-table-header");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        return label;
    }

    private Label myScansBodyCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("my-scans-table-cell");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        return label;
    }

    private VBox myScansDateCell(String date, String time) {
        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("my-scans-date-value");
        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("my-scans-time-value");

        VBox cell = new VBox(4, dateLabel, timeLabel);
        cell.getStyleClass().add("my-scans-table-cell");
        cell.setMaxWidth(Double.MAX_VALUE);
        return cell;
    }

    private HBox myScansStatusCell(String status) {
        Label value = new Label(status);
        value.getStyleClass().addAll("my-scans-status-text", myScansStatusClass(status));

        HBox cell = new HBox(value);
        cell.getStyleClass().add("exports-table-cell-wrap");
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMaxWidth(Double.MAX_VALUE);
        return cell;
    }

    private HBox myScansActionsCell(MyScanRow scan, Consumer<MyScanRow> onOpenMyScanDetail) {
        HBox cell = new HBox(8);
        cell.getStyleClass().add("exports-table-cell-wrap");
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMaxWidth(Double.MAX_VALUE);

        for (String action : scan.actions()) {
            HBox actionButton = myScansActionLink(action);
            if ("View".equals(action) || "View Scan".equals(action)) {
                actionButton.setOnMouseClicked(event -> onOpenMyScanDetail.accept(scan));
            }
            cell.getChildren().add(actionButton);
        }

        return cell;
    }

    private HBox myScansActionLink(String text) {
        SVGPath icon = new SVGPath();
        if ("Export".equals(text)) {
            icon.setContent("M12 3V15 M7 10L12 15L17 10 M5 21H19");
        } else {
            icon.setContent("M2 12S5.5 5 12 5S22 12 22 12S18.5 19 12 19S2 12 2 12 M12 15A3 3 0 1 0 12 9A3 3 0 0 0 12 15");
        }
        icon.getStyleClass().add("my-scans-action-icon");

        Label label = new Label(text);
        label.getStyleClass().add("my-scans-action-text");

        HBox link = new HBox(5, icon, label);
        link.getStyleClass().add("my-scans-action-link");
        link.setAlignment(Pos.CENTER_LEFT);
        return link;
    }

    private String myScanSelectionKey(MyScanRow scan) {
        return scan.boxId() + "|" + scan.file();
    }

    private Button myScansActionButton(String text, boolean primary) {
        Button button = new Button(text);
        button.getStyleClass().add(primary ? "my-scans-primary-action" : "my-scans-secondary-action");
        button.setFocusTraversable(false);
        return button;
    }

    private String myScansStatusClass(String status) {
        return switch (status) {
            case "Completed", "QA Completed", "Exported" -> "my-scans-status-completed";
            case "Processing", "In Progress", "Waiting for QA" -> "my-scans-status-processing";
            case "Failed", "Metadata Required" -> "my-scans-status-failed";
            default -> "my-scans-status-processing";
        };
    }

    private HBox statusCell(String status) {
        Label pill = new Label(status);
        pill.getStyleClass().add("status-pill");
        if ("Completed".equals(status)) {
            pill.getStyleClass().add("status-completed");
        } else if ("Processing".equals(status)) {
            pill.getStyleClass().add("status-processing");
        } else {
            pill.getStyleClass().add("status-failed");
        }

        HBox wrap = new HBox(pill);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.getStyleClass().add("table-row-wrap");
        return wrap;
    }

    private HBox exportStatusCell(String status) {
        Label pill = new Label(status);
        pill.getStyleClass().add("exports-status-text");
        if ("Ready".equals(status)) {
            pill.getStyleClass().add("exports-status-ready");
        } else {
            pill.getStyleClass().add("exports-status-processing");
        }

        HBox wrap = new HBox(pill);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.getStyleClass().add("exports-table-cell-wrap");
        return wrap;
    }

    private HBox exportActionCell(ExportRow export) {
        HBox wrap = new HBox(8);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.getStyleClass().add("exports-table-cell-wrap");
        if (!"Ready".equals(export.status())) {
            Label pending = new Label("Processing");
            pending.getStyleClass().add("export-pending-text");
            wrap.getChildren().add(pending);
            return wrap;
        }

        SVGPath downloadIcon = new SVGPath();
        downloadIcon.setContent("M12 3V15 M7 10L12 15L17 10 M5 21H19");
        downloadIcon.getStyleClass().add("export-download-icon");
        Label download = new Label("Download");
        download.getStyleClass().add("export-download-text");
        HBox downloadButton = new HBox(5, downloadIcon, download);
        downloadButton.getStyleClass().add("export-download-button");
        downloadButton.setAlignment(Pos.CENTER_LEFT);

        SVGPath shareIcon = new SVGPath();
        shareIcon.setContent("M18 8A3 3 0 1 0 15 5A3 3 0 0 0 18 8Z M6 15A3 3 0 1 0 3 12A3 3 0 0 0 6 15Z M18 22A3 3 0 1 0 15 19A3 3 0 0 0 18 22Z M8.6 13.4L15.4 17.6 M15.4 6.4L8.6 10.6");
        shareIcon.getStyleClass().add("export-action-icon");
        StackPane shareButton = exportIconButton(shareIcon, "export-share-button");

        SVGPath deleteIcon = new SVGPath();
        deleteIcon.setContent("M4 7H20 M10 11V17 M14 11V17 M6 7L7 21H17L18 7 M9 7V4H15V7");
        deleteIcon.getStyleClass().addAll("export-action-icon", "export-delete-icon");
        StackPane deleteButton = exportIconButton(deleteIcon, "export-delete-button");

        wrap.getChildren().addAll(downloadButton, shareButton, deleteButton);
        return wrap;
    }

    private StackPane exportIconButton(SVGPath icon, String styleClass) {
        StackPane button = new StackPane(icon);
        button.getStyleClass().addAll("export-icon-button", styleClass);
        return button;
    }

    private Label exportHeaderCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-table-header");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label exportBodyCell(String text, boolean primary) {
        Label label = new Label(text);
        label.getStyleClass().add(primary ? "exports-table-cell-primary" : "exports-table-cell");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private HBox exportCheckboxCell(boolean disabled, boolean selected, Consumer<Boolean> onSelected) {
        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("exports-checkbox");
        checkBox.setDisable(disabled);
        checkBox.setSelected(selected);
        checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (oldValue != newValue) {
                onSelected.accept(newValue);
            }
        });
        HBox wrap = new HBox(checkBox);
        wrap.getStyleClass().add("exports-table-cell-wrap");
        wrap.setAlignment(Pos.CENTER_LEFT);
        return wrap;
    }

    private Button exportSelectionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private HBox buildExportPageSizeMenu() {
        double menuWidth = 170;
        Label value = new Label("25 per page");
        value.getStyleClass().add("exports-page-size-value");
        StackPane arrow = exportDropdownArrowBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox button = new HBox(8, value, spacer, arrow);
        button.getStyleClass().add("exports-page-size");
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMinWidth(menuWidth);
        button.setPrefWidth(menuWidth);
        button.setMaxWidth(menuWidth);

        VBox menu = new VBox(0);
        menu.setMinWidth(menuWidth);
        menu.setPrefWidth(menuWidth);
        menu.setMaxWidth(menuWidth);
        menu.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d9dee6; -fx-border-width: 1; -fx-padding: 0;");
        Popup popup = exportDropdownPopup(menu);

        List<Label> items = new ArrayList<>();
        for (String option : List.of("10 per page", "25 per page", "50 per page", "100 per page")) {
            Label item = new Label(option);
            item.setMinWidth(menuWidth);
            item.setPrefWidth(menuWidth);
            item.setMaxWidth(menuWidth);
            item.setTextOverrun(OverrunStyle.CLIP);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setOnMouseClicked(event -> {
                value.setText(option);
                updateExportPageSizeSelection(items, option);
                popup.hide();
            });
            items.add(item);
            menu.getChildren().add(item);
        }
        updateExportPageSizeSelection(items, value.getText());

        button.setOnMouseClicked(event -> {
            showExportDropdown(button, popup);
        });
        return button;
    }

    private HBox buildExportStatusMenu(String[] selectedStatus, Runnable onChange) {
        Label value = new Label(selectedStatus[0]);
        value.getStyleClass().add("exports-status-value");
        StackPane arrow = exportDropdownArrowBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox button = new HBox(8, value, spacer, arrow);
        button.getStyleClass().add("exports-status-filter");
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);

        VBox menu = new VBox(0);
        menu.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d9dee6; -fx-border-width: 1; -fx-padding: 0;");
        Popup popup = exportDropdownPopup(menu);

        List<Label> items = new ArrayList<>();
        for (String option : List.of("All", "Ready", "Processing")) {
            Label item = new Label(option);
            item.setMaxWidth(Double.MAX_VALUE);
            item.setTextOverrun(OverrunStyle.CLIP);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setOnMouseClicked(event -> {
                selectedStatus[0] = option;
                value.setText(option);
                updateExportStatusSelection(items, option);
                popup.hide();
                onChange.run();
            });
            items.add(item);
            menu.getChildren().add(item);
        }
        updateExportStatusSelection(items, selectedStatus[0]);

        button.setOnMouseClicked(event -> {
            double width = button.getWidth();
            menu.setMinWidth(width);
            menu.setPrefWidth(width);
            menu.setMaxWidth(width);
            for (Label item : items) {
                item.setMinWidth(width);
                item.setPrefWidth(width);
                item.setMaxWidth(width);
            }
            showExportDropdown(button, popup);
        });
        return button;
    }

    private HBox buildMyScansStatusMenu(String[] selectedStatus, Runnable onChange) {
        Label value = new Label(selectedStatus[0]);
        value.getStyleClass().add("exports-status-value");
        StackPane arrow = exportDropdownArrowBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox button = new HBox(8, value, spacer, arrow);
        button.getStyleClass().add("exports-status-filter");
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);

        VBox menu = new VBox(0);
        menu.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d9dee6; -fx-border-width: 1; -fx-padding: 0;");
        Popup popup = exportDropdownPopup(menu);

        List<Label> items = new ArrayList<>();
        for (String option : List.of("All", "Completed", "Processing", "Failed")) {
            Label item = new Label(option);
            item.setMaxWidth(Double.MAX_VALUE);
            item.setTextOverrun(OverrunStyle.CLIP);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setOnMouseClicked(event -> {
                selectedStatus[0] = option;
                value.setText(option);
                updateExportStatusSelection(items, option);
                popup.hide();
                onChange.run();
            });
            items.add(item);
            menu.getChildren().add(item);
        }
        updateExportStatusSelection(items, selectedStatus[0]);

        button.setOnMouseClicked(event -> {
            double width = button.getWidth();
            menu.setMinWidth(width);
            menu.setPrefWidth(width);
            menu.setMaxWidth(width);
            for (Label item : items) {
                item.setMinWidth(width);
                item.setPrefWidth(width);
                item.setMaxWidth(width);
            }
            showExportDropdown(button, popup);
        });
        return button;
    }

    private void updateExportStatusSelection(List<Label> items, String selectedOption) {
        for (Label item : items) {
            styleExportDropdownItem(item, item.getText().equals(selectedOption));
        }
    }

    private void updateExportPageSizeSelection(List<Label> items, String selectedOption) {
        for (Label item : items) {
            styleExportDropdownItem(item, item.getText().equals(selectedOption));
        }
    }

    private SVGPath exportChevronIcon() {
        SVGPath arrow = new SVGPath();
        arrow.setContent("M6 9L12 15L18 9");
        arrow.getStyleClass().add("exports-dropdown-arrow");
        arrow.setMouseTransparent(true);
        return arrow;
    }

    private StackPane exportDropdownArrowBox() {
        StackPane box = new StackPane(exportChevronIcon());
        box.getStyleClass().add("exports-dropdown-arrow-box");
        box.setMouseTransparent(true);
        return box;
    }

    private SVGPath exportSortArrowIcon() {
        SVGPath arrow = new SVGPath();
        arrow.setContent("M6 8L12 14L18 8");
        arrow.getStyleClass().add("exports-sort-arrow");
        arrow.setMouseTransparent(true);
        return arrow;
    }

    private Popup exportDropdownPopup(VBox menu) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(false);
        popup.getContent().add(menu);
        return popup;
    }

    private void showExportDropdown(Node owner, Popup popup) {
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        Point2D point = owner.localToScreen(0, owner.getBoundsInLocal().getHeight());
        popup.show(owner, point.getX(), point.getY());
    }

    private void styleExportDropdownItem(Label item, boolean selected) {
        String background = selected ? "#8a8986" : "#ffffff";
        String text = selected ? "#ffffff" : "#000000";
        item.setStyle(
                "-fx-background-color: " + background + ";"
                        + "-fx-text-fill: " + text + ";"
                        + "-fx-font-size: 14px;"
                        + "-fx-padding: 8 16;"
                        + "-fx-min-height: 34;"
                        + "-fx-pref-height: 34;"
                        + "-fx-max-height: 34;"
                        + "-fx-cursor: hand;"
        );
        item.setOnMouseEntered(event -> {
            if (!selected) {
                item.setStyle(
                        "-fx-background-color: #eeeeee;"
                                + "-fx-text-fill: #000000;"
                                + "-fx-font-size: 14px;"
                                + "-fx-padding: 8 16;"
                                + "-fx-min-height: 34;"
                                + "-fx-pref-height: 34;"
                                + "-fx-max-height: 34;"
                                + "-fx-cursor: hand;"
                );
            }
        });
        item.setOnMouseExited(event -> styleExportDropdownItem(item, selected));
    }

    private Button exportPagerButton(String text, boolean disabled) {
        Button button = new Button(text);
        button.getStyleClass().add("exports-pager-button");
        button.setDisable(disabled);
        return button;
    }

    private Label exportPageLabel() {
        Label label = new Label("Page 1 of 1");
        label.getStyleClass().add("exports-footer-text");
        return label;
    }

    private HBox exportSortableHeaderCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exports-table-header-text");
        HBox cell = new HBox(6, label, exportSortArrowIcon());
        cell.getStyleClass().add("exports-table-header");
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMaxWidth(Double.MAX_VALUE);
        return cell;
    }

    private PageState mapPage(String label) {
        return switch (label) {
            case "Dashboard" -> PageState.DASHBOARD;
            case "Exports" -> PageState.LOGS;
            case "Profiles", "Start Scan" -> PageState.PROFILES;
            case "Metadata" -> PageState.METADATA;
            default -> PageState.MY_SCANS;
        };
    }

    private List<ScanningDocument> copyScanningDocuments(PageMode pageMode) {
        PageMode mode = resolvedPageMode(pageMode);
        List<ScanningDocument> copies = new ArrayList<>();
        for (ScanningDocument document : scanningDocuments()) {
            List<ScanningFile> files = new ArrayList<>();
            if (mode == PageMode.SINGLE_PAGE) {
                int pageIndex = 1;
                for (ScanningFile file : document.files()) {
                    for (int pageNumber = 1; pageNumber <= file.pageCount(); pageNumber++) {
                        files.add(new ScanningFile(
                                "Page " + pageIndex,
                                singlePageFileName(file.fileName(), pageNumber),
                                singlePageReference(file.reference(), pageNumber),
                                "1 page",
                                1,
                                file.barcode() && pageNumber == 1
                        ));
                        pageIndex++;
                    }
                }
            } else {
                for (ScanningFile file : document.files()) {
                    files.add(new ScanningFile(
                            file.badge(),
                            multiPageFileName(file.fileName()),
                            file.reference(),
                            pageCountText(file.pageCount()),
                            file.pageCount(),
                            file.barcode()
                    ));
                }
            }
            copies.add(new ScanningDocument(
                    document.title(),
                    scannedFileCountText(files.size()),
                    files
            ));
        }
        return copies;
    }

    private String singlePageFileName(String fileName, int pageNumber) {
        return fileBaseName(fileName) + "_page_" + String.format("%02d", pageNumber) + ".tiff";
    }

    private String singlePageReference(String reference, int pageNumber) {
        return reference + "-P" + String.format("%02d", pageNumber);
    }

    private String multiPageFileName(String fileName) {
        return fileBaseName(fileName) + "_multi_page.tiff";
    }

    private String fileBaseName(String value) {
        String cleaned = value == null || value.isBlank() ? "scan" : value.trim();
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex > 0) {
            cleaned = cleaned.substring(0, dotIndex);
        }
        cleaned = cleaned.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return cleaned.isBlank() ? "scan" : cleaned;
    }

    private String pageCountText(int pageCount) {
        return pageCount + (pageCount == 1 ? " page" : " pages");
    }

    private ScanningDocument scanFileIntoWorkspace(List<ScanningDocument> sourceDocuments,
                                                   List<ScanningDocument> scannedDocuments,
                                                   ScanningFile file) {
        if (file == null) {
            return null;
        }

        ScanningDocument existingDocument = findDocumentForFile(scannedDocuments, file);
        if (existingDocument != null) {
            return existingDocument;
        }

        ScanningDocument scannedDocument;
        if (file.barcode() || scannedDocuments.isEmpty()) {
            scannedDocument = new ScanningDocument(nextScannedDocumentTitle(scannedDocuments), scannedFileCountText(0), new ArrayList<>());
            scannedDocuments.add(scannedDocument);
        } else {
            scannedDocument = scannedDocuments.get(scannedDocuments.size() - 1);
        }

        List<ScanningFile> files = scannedDocument.files();
        files.add(file);

        ScanningDocument updatedDocument = new ScanningDocument(scannedDocument.title(), scannedFileCountText(files.size()), files);
        int index = scannedDocuments.indexOf(scannedDocument);
        if (index >= 0) {
            scannedDocuments.set(index, updatedDocument);
        }
        return updatedDocument;
    }

    private String nextScannedDocumentTitle(List<ScanningDocument> scannedDocuments) {
        return "Document " + (scannedDocuments.size() + 1);
    }

    private ScanningFile firstUnscannedFile(List<ScanningDocument> sourceDocuments,
                                            List<ScanningDocument> scannedDocuments) {
        for (ScanningDocument document : sourceDocuments) {
            for (ScanningFile file : document.files()) {
                if (!containsScannedFile(scannedDocuments, file)) {
                    return file;
                }
            }
        }
        return null;
    }

    private boolean containsScannedFile(List<ScanningDocument> documents, ScanningFile file) {
        if (file == null) {
            return false;
        }
        return findDocumentForFile(documents, file) != null;
    }

    private ScanningFile selectedScannedFile(List<ScanningDocument> scannedDocuments, ScanningFile activeFile) {
        if (activeFile != null && containsScannedFile(scannedDocuments, activeFile)) {
            return activeFile;
        }
        for (ScanningDocument document : scannedDocuments) {
            if (!document.files().isEmpty()) {
                return document.files().get(0);
            }
        }
        return null;
    }

    private String scannedFileCountText(int count) {
        return count + (count == 1 ? " file" : " files");
    }

    private ScanningDocument findScanningDocument(List<ScanningDocument> scanningDocuments, String title) {
        for (ScanningDocument document : scanningDocuments) {
            if (document.title().equals(title)) {
                return document;
            }
        }
        return scanningDocuments.isEmpty() ? null : scanningDocuments.get(0);
    }

    private ScanningDocument findDocumentForFile(List<ScanningDocument> documents,
                                                                 ScanningFile file) {
        if (file == null) {
            return null;
        }
        for (ScanningDocument document : documents) {
            for (ScanningFile candidate : document.files()) {
                if (candidate.reference().equals(file.reference())) {
                    return document;
                }
            }
        }
        return null;
    }

    private List<ScanningFile> flattenScannedFiles(List<ScanningDocument> documents) {
        List<ScanningFile> files = new ArrayList<>();
        for (ScanningDocument document : documents) {
            files.addAll(document.files());
        }
        return files;
    }

    private int findFileIndex(List<ScanningFile> files, ScanningFile target) {
        if (target == null) {
            return -1;
        }
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).reference().equals(target.reference())) {
                return i;
            }
        }
        return -1;
    }

    private boolean moveOrReorderFile(ScanningDocument sourceDocument,
                                      ScanningDocument targetDocument,
                                      String sourceReference,
                                      String targetReference,
                                      boolean insertAfterTarget) {
        if (sourceDocument == null || targetDocument == null || sourceReference == null || targetReference == null || sourceReference.equals(targetReference)) {
            return false;
        }
        List<ScanningFile> sourceFiles = sourceDocument.files();
        List<ScanningFile> targetFiles = targetDocument.files();
        int sourceIndex = -1;
        int targetIndex = -1;
        for (int i = 0; i < sourceFiles.size(); i++) {
            if (sourceFiles.get(i).reference().equals(sourceReference)) {
                sourceIndex = i;
            }
        }
        for (int i = 0; i < targetFiles.size(); i++) {
            if (targetFiles.get(i).reference().equals(targetReference)) {
                targetIndex = i;
            }
        }
        if (sourceIndex < 0 || targetIndex < 0) {
            return false;
        }
        if (sourceDocument == targetDocument && sourceIndex == targetIndex) {
            return false;
        }
        ScanningFile movingFile = sourceFiles.remove(sourceIndex);
        if (sourceDocument == targetDocument && sourceIndex < targetIndex) {
            targetIndex--;
        }
        if (insertAfterTarget) {
            targetIndex++;
        }
        targetIndex = Math.max(0, Math.min(targetIndex, targetFiles.size()));
        targetFiles.add(targetIndex, movingFile);
        return true;
    }

    private boolean moveFileToDocumentStart(ScanningDocument sourceDocument,
                                            ScanningDocument targetDocument,
                                            String sourceReference) {
        if (sourceDocument == null || targetDocument == null || sourceReference == null) {
            return false;
        }
        List<ScanningFile> sourceFiles = sourceDocument.files();
        List<ScanningFile> targetFiles = targetDocument.files();
        int sourceIndex = -1;
        for (int i = 0; i < sourceFiles.size(); i++) {
            if (sourceFiles.get(i).reference().equals(sourceReference)) {
                sourceIndex = i;
                break;
            }
        }
        if (sourceIndex < 0 || sourceDocument == targetDocument && sourceIndex == 0) {
            return false;
        }
        ScanningFile movingFile = sourceFiles.remove(sourceIndex);
        targetFiles.add(0, movingFile);
        return true;
    }

    private void animateFileCard(VBox card) {
        card.setOpacity(0);
        card.setTranslateY(8);

        FadeTransition fade = new FadeTransition(Duration.millis(180), card);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), card);
        slide.setFromY(8);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    private void openFilePagesWindow(ScanningDocument document, ScanningFile file) {
        List<Integer> originalPages = new ArrayList<>();
        for (int i = 1; i <= file.pageCount(); i++) {
            originalPages.add(i);
        }
        List<Integer> workingPages = new ArrayList<>(originalPages);
        int[] selectedPage = {workingPages.isEmpty() ? 1 : workingPages.get(0)};

        VBox paper = buildPageWindowPaper();
        StackPane previewStage = new StackPane(paper);
        previewStage.getStyleClass().add("page-window-preview-stage");
        BorderPane.setAlignment(previewStage, Pos.CENTER);

        HBox pages = new HBox(12);
        pages.getStyleClass().add("page-window-thumbnail-row");
        pages.setAlignment(Pos.CENTER_LEFT);
        Runnable[] renderPages = new Runnable[1];
        renderPages[0] = () -> {
            pages.getChildren().clear();
            for (Integer pageNumber : workingPages) {
                pages.getChildren().add(buildPageWindowThumbnailCard(
                        pageNumber,
                        pageNumber == selectedPage[0],
                        workingPages,
                        renderPages,
                        () -> {
                            selectedPage[0] = pageNumber;
                            renderPages[0].run();
                        }
                ));
            }
        };
        renderPages[0].run();

        Label stripTitle = new Label(document.title().toUpperCase());
        stripTitle.getStyleClass().add("page-window-strip-title");
        Label stripCount = new Label(String.valueOf(file.pageCount()));
        stripCount.getStyleClass().add("qa-count-pill");
        HBox stripHeader = new HBox(8, stripTitle, stripCount);
        stripHeader.setAlignment(Pos.CENTER_LEFT);

        ScrollPane thumbnailScroll = new ScrollPane(pages);
        thumbnailScroll.getStyleClass().add("page-window-thumbnail-scroll");
        thumbnailScroll.setFitToHeight(true);
        thumbnailScroll.setFitToWidth(false);
        thumbnailScroll.setPannable(true);
        thumbnailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        thumbnailScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        enableHorizontalThumbnailScroll(thumbnailScroll);

        VBox bottomStrip = new VBox(8, stripHeader, thumbnailScroll);
        bottomStrip.getStyleClass().add("page-window-bottom-strip");

        Stage stage = new Stage();
        stage.setTitle("File Pages - " + file.fileName());
        BorderPane windowRoot = new BorderPane();
        windowRoot.getStyleClass().add("page-window-root");
        windowRoot.setCenter(previewStage);
        windowRoot.setBottom(bottomStrip);

        Scene windowScene = new Scene(windowRoot, 700, 620);
        windowScene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setScene(windowScene);
        stage.setMinWidth(640);
        stage.setMinHeight(560);

        stage.show();
    }

    private VBox buildPageWindowPaper() {
        VBox lines = new VBox(10,
                pageWindowLine(1.00),
                pageWindowLine(0.92),
                pageWindowLine(0.80),
                spacer(18),
                pageWindowLine(1.00),
                pageWindowLine(1.00),
                pageWindowLine(0.83),
                spacer(16),
                pageWindowLine(1.00),
                pageWindowLine(0.92),
                pageWindowLine(0.75),
                spacer(16),
                pageWindowLine(1.00),
                pageWindowLine(1.00),
                pageWindowLine(0.83),
                pageWindowLine(0.66)
        );
        lines.getStyleClass().add("page-window-paper-lines");

        VBox paper = new VBox(lines);
        paper.getStyleClass().add("page-window-main-paper");
        return paper;
    }

    private Region pageWindowLine(double widthFactor) {
        Region line = new Region();
        line.getStyleClass().add("page-window-paper-line");
        line.setPrefWidth(300 * widthFactor);
        line.setMaxWidth(300 * widthFactor);
        return line;
    }

    private Region spacer(double height) {
        Region spacer = new Region();
        spacer.setMinHeight(height);
        spacer.setPrefHeight(height);
        spacer.setMaxHeight(height);
        return spacer;
    }

    private VBox buildPageWindowThumbnailCard(int pageNumber,
                                             boolean selected,
                                             List<Integer> workingPages,
                                             Runnable[] renderPages,
                                             Runnable onSelectPage) {
        VBox preview = buildPaperPreview();
        preview.getStyleClass().add("page-window-thumbnail-preview");
        Label title = new Label("F" + pageNumber);
        title.getStyleClass().add("page-window-thumbnail-label");

        VBox card = new VBox(5, preview, title);
        card.getStyleClass().add("page-window-thumbnail-card");
        if (selected) {
            card.getStyleClass().add("page-window-thumbnail-selected");
        }
        enableSmoothPageDrag(card, pageNumber, workingPages, renderPages);
        card.setOnMouseClicked(event -> {
            if (event.isStillSincePress()) {
                onSelectPage.run();
            }
        });
        animateFileCard(card);
        return card;
    }

    private void enableSmoothFileDrag(VBox card,
                                      ScanningDocument document,
                                      ScanningFile file,
                                      Consumer<ScanningFile> onOpenScanningFile,
                                      Runnable onScanningChanged) {
        final double[] dragOffsetX = new double[1];
        final double[] dragOffsetY = new double[1];
        final boolean[] dragging = new boolean[1];
        final Node[] activeDropTarget = new Node[1];

        card.setOnMousePressed(event -> {
            dragOffsetX[0] = event.getSceneX();
            dragOffsetY[0] = event.getSceneY();
            dragging[0] = false;
            clearFileDropTarget(activeDropTarget);
        });

        card.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - dragOffsetX[0];
            double deltaY = event.getSceneY() - dragOffsetY[0];
            if (!dragging[0] && Math.hypot(deltaX, deltaY) > 8) {
                dragging[0] = true;
                card.toFront();
                if (!card.getStyleClass().contains("scan-file-card-dragging")) {
                    card.getStyleClass().add("scan-file-card-dragging");
                }
            }
            if (!dragging[0]) {
                return;
            }
            card.setTranslateX(deltaX);
            card.setTranslateY(deltaY);
            updateFileDropTarget(activeDropTarget, findClosestFileCard(card, event.getSceneX(), event.getSceneY()));
        });

        card.setOnMouseReleased(event -> {
            if (!dragging[0]) {
                onOpenScanningFile.accept(file);
                return;
            }
            Node targetNode = findClosestFileCard(card, event.getSceneX(), event.getSceneY());
            card.setTranslateX(0);
            card.setTranslateY(0);
            card.getStyleClass().remove("scan-file-card-dragging");
            clearFileDropTarget(activeDropTarget);
            dragging[0] = false;

            if (targetNode instanceof VBox targetCard) {
                String targetReference = (String) targetCard.getProperties().get("referenceId");
                ScanningDocument targetDocument = (ScanningDocument) targetCard.getProperties().get("scanningDocument");
                boolean moved = false;
                if (targetReference != null) {
                    boolean insertAfterTarget = isDropAfterTarget(targetCard, event.getSceneX(), event.getSceneY());
                    moved = moveOrReorderFile(document, targetDocument, file.reference(), targetReference, insertAfterTarget);
                } else if (Boolean.TRUE.equals(targetCard.getProperties().get("documentDropTarget"))) {
                    moved = moveFileToDocumentStart(document, targetDocument, file.reference());
                }
                if (moved) {
                    onScanningChanged.run();
                }
            }
        });

        card.getProperties().put("referenceId", file.reference());
        card.getProperties().put("scanningDocument", document);
    }

    private void updateFileDropTarget(Node[] activeDropTarget, Node targetNode) {
        if (activeDropTarget[0] == targetNode) {
            return;
        }
        clearFileDropTarget(activeDropTarget);
        if (targetNode != null && !targetNode.getStyleClass().contains("scan-file-card-drop-target")) {
            targetNode.getStyleClass().add("scan-file-card-drop-target");
        }
        activeDropTarget[0] = targetNode;
    }

    private void clearFileDropTarget(Node[] activeDropTarget) {
        if (activeDropTarget[0] != null) {
            activeDropTarget[0].getStyleClass().remove("scan-file-card-drop-target");
            activeDropTarget[0] = null;
        }
    }

    private void enableSmoothPageDrag(VBox card,
                                      int pageNumber,
                                      List<Integer> workingPages,
                                      Runnable[] renderPages) {
        final double[] dragOffsetX = new double[1];
        final double[] dragOffsetY = new double[1];
        final boolean[] dragging = new boolean[1];

        card.setOnMousePressed(event -> {
            dragOffsetX[0] = event.getSceneX();
            dragOffsetY[0] = event.getSceneY();
            dragging[0] = false;
        });

        card.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - dragOffsetX[0];
            double deltaY = event.getSceneY() - dragOffsetY[0];
            if (!dragging[0] && Math.hypot(deltaX, deltaY) > 8) {
                dragging[0] = true;
                card.toFront();
                if (!card.getStyleClass().contains("scan-page-card-dragging")) {
                    card.getStyleClass().add("scan-page-card-dragging");
                }
            }
            if (!dragging[0]) {
                return;
            }
            card.setTranslateX(deltaX);
            card.setTranslateY(deltaY);
        });

        card.setOnMouseReleased(event -> {
            if (!dragging[0]) {
                return;
            }
            Node targetNode = findClosestPageCard(card, event.getSceneX(), event.getSceneY());
            card.setTranslateX(0);
            card.setTranslateY(0);
            card.getStyleClass().remove("scan-page-card-dragging");
            dragging[0] = false;

            if (targetNode instanceof VBox targetCard) {
                Integer targetPage = (Integer) targetCard.getProperties().get("pageNumber");
                if (targetPage != null && reorderPages(workingPages, pageNumber, targetPage)) {
                    renderPages[0].run();
                }
            }
        });

        card.getProperties().put("pageNumber", pageNumber);
    }

    private VBox buildLargePaperPreview() {
        Label corner = new Label("");
        corner.getStyleClass().add("paper-corner");
        Label lineOne = new Label("");
        lineOne.getStyleClass().add("paper-line");
        Label lineTwo = new Label("");
        lineTwo.getStyleClass().add("paper-line");
        Label lineThree = new Label("");
        lineThree.getStyleClass().add("paper-line");
        Label lineFour = new Label("");
        lineFour.getStyleClass().add("paper-line-short");
        Label lineFive = new Label("");
        lineFive.getStyleClass().add("paper-line");

        VBox preview = new VBox(6, corner, lineOne, lineTwo, lineThree, lineFour, lineFive);
        preview.getStyleClass().add("scan-large-paper-preview");
        return preview;
    }

    private Node findClosestFileCard(VBox draggedCard, double sceneX, double sceneY) {
        if (draggedCard.getScene() == null) {
            return null;
        }
        Node closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Node node : draggedCard.getScene().getRoot().lookupAll(".scan-file-card")) {
            if (node == draggedCard) {
                continue;
            }
            double distance = distanceToNodeCenter(node, sceneX, sceneY);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = node;
            }
        }
        for (Node node : draggedCard.getScene().getRoot().lookupAll(".scan-single-document-marker")) {
            if (!Boolean.TRUE.equals(node.getProperties().get("documentDropTarget"))) {
                continue;
            }
            double distance = distanceToNodeCenter(node, sceneX, sceneY);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = node;
            }
        }

        return closestDistance <= 140 ? closest : null;
    }

    private double distanceToNodeCenter(Node node, double sceneX, double sceneY) {
        var bounds = node.localToScene(node.getBoundsInLocal());
        double centerX = (bounds.getMinX() + bounds.getMaxX()) / 2;
        double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2;
        return Math.hypot(sceneX - centerX, sceneY - centerY);
    }

    private boolean isDropAfterTarget(Node targetNode, double sceneX, double sceneY) {
        var bounds = targetNode.localToScene(targetNode.getBoundsInLocal());
        double centerX = (bounds.getMinX() + bounds.getMaxX()) / 2;
        double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2;
        double horizontalDistance = Math.abs(sceneX - centerX);
        double verticalDistance = Math.abs(sceneY - centerY);
        return horizontalDistance >= verticalDistance
                ? sceneX > centerX
                : sceneY > centerY;
    }

    private Node findClosestPageCard(VBox draggedCard, double sceneX, double sceneY) {
        if (!(draggedCard.getParent() instanceof Pane pageContainer)) {
            return null;
        }
        Node closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Node node : pageContainer.getChildren()) {
            if (node == draggedCard) {
                continue;
            }
            var bounds = node.localToScene(node.getBoundsInLocal());
            double centerX = (bounds.getMinX() + bounds.getMaxX()) / 2;
            double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2;
            double distance = Math.hypot(sceneX - centerX, sceneY - centerY);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = node;
            }
        }

        return closestDistance <= 170 ? closest : null;
    }

    private boolean reorderPages(List<Integer> workingPages, int sourcePage, int targetPage) {
        int sourceIndex = workingPages.indexOf(sourcePage);
        int targetIndex = workingPages.indexOf(targetPage);
        if (sourceIndex < 0 || targetIndex < 0 || sourceIndex == targetIndex) {
            return false;
        }
        Integer movingPage = workingPages.remove(sourceIndex);
        if (sourceIndex < targetIndex) {
            targetIndex--;
        }
        workingPages.add(targetIndex, movingPage);
        return true;
    }

    private boolean containsDocument(List<ScanningDocument> documents, String title) {
        for (ScanningDocument document : documents) {
            if (document.title().equals(title)) {
                return true;
            }
        }
        return false;
    }

    private int totalFiles(List<ScanningDocument> documents) {
        int total = 0;
        for (ScanningDocument document : documents) {
            total += document.files().size();
        }
        return total;
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean matchesSearch(String search, String... values) {
        if (search.isBlank()) {
            return true;
        }
        for (String value : values) {
            if (value != null && value.toLowerCase().contains(search)) {
                return true;
            }
        }
        return false;
    }
}
