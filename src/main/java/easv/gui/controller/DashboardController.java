package easv.gui.controller;

import javafx.fxml.FXML;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

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
        MY_SCANS,
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

    private record ActivityLogEntry(String user, String action, String file, String document, String profile, String boxId, String date, String time) {
    }

    private record ExportRow(String fileName, String boxId, String profile, String dateCreated, String size, String status) {
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
                new ActionCard("EX", "Exports", "Download your files", false)
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

    private List<ActivityLogEntry> activityLog() {
        return List.of(
                new ActivityLogEntry("Mike", "Deleted", "File 05", "Old Contract", "Legal", "Box 18", "2026-04-23", "11:10"),
                new ActivityLogEntry("Admin", "Created User", "New Scanner User", "-", "-", "-", "2026-04-23", "11:30"),
                new ActivityLogEntry("Emma", "Exported", "Document A", "Invoice A", "Finance", "Box 12", "2026-04-23", "10:45"),
                new ActivityLogEntry("Emma", "QA Approved", "Document A", "Invoice A", "Finance", "Box 12", "2026-04-23", "10:30"),
                new ActivityLogEntry("John", "Sent to QA", "Document A", "Invoice A", "Finance", "Box 12", "2026-04-23", "10:00"),
                new ActivityLogEntry("Sara", "Updated", "File 03", "Contract B", "Legal", "Box 15", "2026-04-23", "09:25"),
                new ActivityLogEntry("Sara", "Moved", "File 03", "Contract B", "Legal", "Box 15", "2026-04-23", "09:20"),
                new ActivityLogEntry("JD", "Scanned", "File 03", "Invoice A", "Standard Scan", "BOX-2026-043", "2026-04-27", "09:08"),
                new ActivityLogEntry("JD", "Sent to QA", "File 02", "Invoice A", "Standard Scan", "BOX-2026-043", "2026-04-27", "09:18"),
                new ActivityLogEntry("JD", "Exported", "File 01", "Invoice A", "Standard Scan", "BOX-2026-043", "2026-04-27", "09:40")
        );
    }

    private List<ExportRow> exports() {
        return List.of(
                new ExportRow("StandardScan_BOX-2026-042.pdf", "BOX-2026-042", "Standard Scan", "2026-04-24", "45.2 MB", "Ready"),
                new ExportRow("HighQuality_BOX-2026-041.pdf", "BOX-2026-041", "High Quality", "2026-04-24", "89.7 MB", "Processing"),
                new ExportRow("StandardScan_BOX-2026-040.pdf", "BOX-2026-040", "Standard Scan", "2026-04-23", "67.3 MB", "Ready"),
                new ExportRow("Archive_BOX-2026-038.pdf", "BOX-2026-038", "Archive", "2026-04-22", "34.8 MB", "Ready")
        );
    }

    private List<ActivityLogEntry> myScanActivity() {
        return activityLog().stream()
                .filter(entry -> "JD".equals(entry.user()))
                .toList();
    }

    private ProfileSummary activeProfile() {
        return new ProfileSummary("Standard Scan", "5 deg", "+10%", "On");
    }

    private List<ScanProfile> scanProfiles() {
        return List.of(
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
    @FXML private HBox exportsNav;
    @FXML private HBox profilesNav;
    @FXML private HBox myScansNav;
    @FXML private Label dashboardNavIcon;
    @FXML private Label exportsNavIcon;
    @FXML private Label profilesNavIcon;
    @FXML private Label myScansNavIcon;
    @FXML private Label dashboardNavText;
    @FXML private Label exportsNavText;
    @FXML private Label profilesNavText;
    @FXML private Label myScansNavText;
    @FXML private HBox darkSwitch;
    @FXML private Label darkSwitchKnob;

    @FXML
    private void initialize() {
        if (!root.getStyleClass().contains("app-shell")) {
            root.getStyleClass().add("app-shell");
        }

        PageState[] currentPage = {PageState.DASHBOARD};
        ScanProfile[] selectedProfile = {null};
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
                        currentPage[0] = PageState.SCAN_PROGRESS;
                        render[0].run();
                        scanProgressTimeline[0] = new Timeline(new KeyFrame(Duration.millis(35), event -> {
                            if (currentPage[0] != PageState.SCAN_PROGRESS) {
                                scanProgressTimeline[0].stop();
                                return;
                            }
                            scanProgress[0] = Math.min(100, scanProgress[0] + 1.2);
                            if (scanProgress[0] >= 100) {
                                for (ScanningDocument document : scanningDocuments[0]) {
                                    if (!containsDocument(scannedWorkspaceDocuments[0], document.title())) {
                                        scannedWorkspaceDocuments[0].add(document);
                                    }
                                }
                                if (!scannedWorkspaceDocuments[0].isEmpty()) {
                                    activeScanningDocument[0] = scannedWorkspaceDocuments[0].get(0);
                                }
                                activeScanningFile[0] = null;
                                currentPage[0] = PageState.SCAN_COMPLETE;
                            }
                            render[0].run();
                        }));
                        scanProgressTimeline[0].setCycleCount(Timeline.INDEFINITE);
                        scanProgressTimeline[0].play();
                    },
                    document -> {
                        if (!containsDocument(scannedWorkspaceDocuments[0], document.title())) {
                            scannedWorkspaceDocuments[0].add(document);
                        }
                        activeScanningDocument[0] = document;
                        activeScanningFile[0] = null;
                        render[0].run();
                    },
                    () -> {
                        for (ScanningDocument document : scanningDocuments[0]) {
                            if (!containsDocument(scannedWorkspaceDocuments[0], document.title())) {
                                scannedWorkspaceDocuments[0].add(document);
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
        updateNavItem(exportsNav, exportsNavIcon, exportsNavText, "Exports", currentPage, onNavigate);
        updateNavItem(profilesNav, profilesNavIcon, profilesNavText, "Profiles", currentPage, onNavigate);
        updateNavItem(myScansNav, myScansNavIcon, myScansNavText, "My Scans", currentPage, onNavigate);

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
                                    String selectedTiffAction,
                                    double scanProgress,
                                    Consumer<PageState> onNavigate,
                                    Consumer<ScanProfile> onSelectProfile,
                                    Consumer<PageMode> onSelectPageMode,
                                    BiConsumer<String, PageMode> onStartScanning,
                                    Consumer<ScanningDocument> onSelectScanningDocument,
                                    Runnable onScanAllDocuments,
                                    Consumer<ScanningFile> onOpenScanningFile,
                                    Runnable onOpenQaReview,
                                    Runnable onScanningChanged,
                                    Runnable onBackToProfiles,
                                    Consumer<ScanningFile> onSelectQaFile,
                                    Runnable onBackFromQa,
                                    Runnable onBackToDashboard,
                                    Runnable onExportQa,
                                    Consumer<Integer> onStepQaPage,
                                    Runnable onRotateQaPage,
                                    Consumer<Double> onZoomQaPage,
                                    Consumer<double[]> onPanQaPage,
                                    Runnable onToggleQaHighlight,
                                    Consumer<String> onToggleQaTextHighlight,
                                    Runnable onApproveQa,
                                    Runnable onRejectQa,
                                    Consumer<String> onSelectTiffAction) {
        BorderPane content = new BorderPane();
        content.getStyleClass().add("dashboard-shell");
        if (pageState != PageState.QA_REVIEW && pageState != PageState.SCAN_PROGRESS && pageState != PageState.SCAN_COMPLETE) {
            content.setTop(buildTopBar());
        }
        Parent pageBody = switch (pageState) {
            case DASHBOARD -> buildDashboardBody(onNavigate);
            case PROFILES -> buildProfilesBody(selectedProfile, currentBoxId, currentPageMode, onSelectProfile, onSelectPageMode, onStartScanning);
            case LOGS -> buildLogsBody();
            case MY_SCANS -> buildMyScansBody();
            case SCAN_PROGRESS -> buildScanProgressBody(selectedProfile, currentBoxId, currentPageMode, scanProgress, onBackToProfiles);
            case SCAN_COMPLETE -> buildScanCompleteBody(selectedProfile, currentBoxId, currentPageMode, onScanningChanged, onBackToProfiles);
            case SCANNING -> buildScanningBody(selectedProfile, currentBoxId, scanningDocuments, scannedWorkspaceDocuments, activeScanningDocument, qaCompletedDocuments, onSelectScanningDocument, onOpenScanningFile, onOpenQaReview, onScanningChanged, onBackToProfiles, onScanAllDocuments);
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
                    onSelectTiffAction
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
        body.getStyleClass().add("dashboard-body");

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
                                     BiConsumer<String, PageMode> onStartScanning) {
        VBox body = new VBox(8);
        body.getStyleClass().addAll("dashboard-body", "qa-page-body");

        Label heading = new Label("Start New Scan");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label("Select a profile and enter box information");
        subtitle.getStyleClass().add("dashboard-subtitle");
        TextField searchField = new TextField();
        searchField.setPromptText("Search profiles");
        searchField.getStyleClass().addAll("box-id-field", "compact-search-field");

        VBox formCard = new VBox(10);
        formCard.getStyleClass().add("panel-card");
        formCard.setPadding(new Insets(14));
        formCard.setMaxWidth(860);

        Label profileSectionTitle = new Label("Select Profile");
        profileSectionTitle.getStyleClass().add("form-section-title");

        VBox profilesList = new VBox(6);
        Runnable refreshProfiles = () -> {
            profilesList.getChildren().clear();
            String search = normalizeSearch(searchField.getText());
            for (ScanProfile profile : scanProfiles()) {
                if (!matchesSearch(search, profile.title(), profile.description(), profile.settingsLine())) {
                    continue;
                }
                profilesList.getChildren().add(buildProfileCard(profile, profile.equals(selectedProfile), onSelectProfile));
            }
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshProfiles.run());
        refreshProfiles.run();

        Label boxIdTitle = new Label("Enter Box ID");
        boxIdTitle.getStyleClass().add("form-section-title");

        TextField boxIdField = new TextField();
        boxIdField.setPromptText("e.g., BOX-2026-043");
        boxIdField.getStyleClass().add("box-id-field");
        boxIdField.setText(currentBoxId);

        Label boxIdHint = new Label("Box ID will be used in the export filename");
        boxIdHint.getStyleClass().add("form-helper-text");

        Label pageModeTitle = new Label("Page Mode");
        pageModeTitle.getStyleClass().add("form-section-title");

        PageMode currentMode = selectedPageMode == null ? PageMode.MULTI_PAGE : selectedPageMode;
        PageMode[] pageModeSelection = {currentMode};
        ToggleGroup pageModeGroup = new ToggleGroup();
        ToggleButton singlePageButton = buildPageModeToggle(PageMode.SINGLE_PAGE, pageModeGroup, currentMode);
        ToggleButton multiPageButton = buildPageModeToggle(PageMode.MULTI_PAGE, pageModeGroup, currentMode);
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

        HBox pageModeRow = new HBox(8, singlePageButton, multiPageButton);
        pageModeRow.getStyleClass().add("scan-mode-toggle-row");

        Button startButton = new Button("Start Scanning");
        startButton.getStyleClass().add("start-scanning-button");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setDisable(selectedProfile == null || currentBoxId == null || currentBoxId.isBlank());
        boxIdField.textProperty().addListener((obs, oldValue, newValue) ->
                startButton.setDisable(selectedProfile == null || newValue == null || newValue.isBlank()));
        startButton.setOnAction(event -> {
            String boxId = boxIdField.getText() == null ? "" : boxIdField.getText().trim();
            if (selectedProfile != null && !boxId.isEmpty()) {
                onStartScanning.accept(boxId, pageModeSelection[0]);
            }
        });

        formCard.getChildren().addAll(profileSectionTitle, profilesList, boxIdTitle, boxIdField, pageModeTitle, pageModeRow, boxIdHint, startButton);

        ScrollPane formScroll = new ScrollPane(formCard);
        formScroll.setFitToWidth(true);
        formScroll.getStyleClass().add("profile-form-scroll");
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        body.getChildren().addAll(heading, subtitle, searchField, formScroll);
        return body;
    }

    private ToggleButton buildPageModeToggle(PageMode pageMode, ToggleGroup group, PageMode selectedPageMode) {
        ToggleButton button = new ToggleButton(pageMode.label());
        button.getStyleClass().add("scan-mode-toggle");
        button.setToggleGroup(group);
        button.setUserData(pageMode);
        button.setSelected(pageMode == selectedPageMode);
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, Priority.ALWAYS);
        return button;
    }

    private VBox buildLogsBody() {
        VBox body = new VBox(14);
        body.getStyleClass().add("dashboard-body");

        Label heading = new Label("Exports");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label("Download your exported scan files");
        subtitle.getStyleClass().add("dashboard-subtitle");
        TextField searchField = new TextField();
        searchField.setPromptText("Search exports");
        searchField.getStyleClass().addAll("box-id-field", "compact-search-field");

        VBox panel = new VBox(0);
        panel.getStyleClass().addAll("panel-card", "exports-panel");

        GridPane table = new GridPane();
        table.getStyleClass().addAll("recent-table", "exports-table");
        table.setMaxWidth(Double.MAX_VALUE);
        table.getColumnConstraints().addAll(
                column(310),
                column(170),
                column(170),
                column(150),
                column(110),
                column(145),
                column(130)
        );

        table.add(headerCell("File Name"), 0, 0);
        table.add(headerCell("Box ID"), 1, 0);
        table.add(headerCell("Profile"), 2, 0);
        table.add(headerCell("Date Created"), 3, 0);
        table.add(headerCell("Size"), 4, 0);
        table.add(headerCell("Status"), 5, 0);
        table.add(headerCell("Action"), 6, 0);

        Runnable refreshTable = () -> {
            while (table.getChildren().size() > 7) {
                table.getChildren().remove(table.getChildren().size() - 1);
            }
            String search = normalizeSearch(searchField.getText());
            int row = 1;
            for (ExportRow export : exports()) {
                if (!matchesSearch(search,
                        export.fileName(),
                        export.boxId(),
                        export.profile(),
                        export.dateCreated(),
                        export.size(),
                        export.status())) {
                    continue;
                }
                table.add(bodyCell(export.fileName()), 0, row);
                table.add(bodyCell(export.boxId()), 1, row);
                table.add(bodyCell(export.profile()), 2, row);
                table.add(bodyCell(export.dateCreated()), 3, row);
                table.add(bodyCell(export.size()), 4, row);
                table.add(exportStatusCell(export.status()), 5, row);
                table.add(exportActionCell(export), 6, row);
                row++;
            }
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable.run());
        refreshTable.run();

        panel.getChildren().add(table);
        body.getChildren().addAll(heading, subtitle, searchField, panel);
        return body;
    }

    private VBox buildMyScansBody() {
        VBox body = new VBox(14);
        body.getStyleClass().add("dashboard-body");

        Label heading = new Label("My Scans");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label("Only your own scan activity and export history.");
        subtitle.getStyleClass().add("dashboard-subtitle");
        TextField searchField = new TextField();
        searchField.setPromptText("Search my scans");
        searchField.getStyleClass().addAll("box-id-field", "compact-search-field");

        HBox contentRow = new HBox(14, buildMyScansTablePanel(searchField), buildMyScansSideColumn());
        HBox.setHgrow(contentRow.getChildren().get(0), Priority.ALWAYS);

        body.getChildren().addAll(heading, subtitle, searchField, contentRow);
        return body;
    }

    private VBox buildMyScansTablePanel(TextField searchField) {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("panel-card");
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label title = new Label("My Activity");
        title.getStyleClass().add("panel-title");
        VBox header = new VBox(title);
        header.setPadding(new Insets(18, 22, 18, 22));

        GridPane table = new GridPane();
        table.getStyleClass().add("recent-table");
        table.setMaxWidth(Double.MAX_VALUE);
        table.getColumnConstraints().addAll(
                column(130),
                column(130),
                column(145),
                column(150),
                column(150),
                column(120),
                column(90)
        );

        table.add(headerCell("Action"), 0, 0);
        table.add(headerCell("File"), 1, 0);
        table.add(headerCell("Document"), 2, 0);
        table.add(headerCell("Profile"), 3, 0);
        table.add(headerCell("Box ID"), 4, 0);
        table.add(headerCell("Date"), 5, 0);
        table.add(headerCell("Time"), 6, 0);

        Runnable refreshTable = () -> {
            while (table.getChildren().size() > 7) {
                table.getChildren().remove(table.getChildren().size() - 1);
            }
            String search = normalizeSearch(searchField.getText());
            int row = 1;
            for (ActivityLogEntry entry : myScanActivity()) {
                if (!matchesSearch(search,
                        entry.action(),
                        entry.file(),
                        entry.document(),
                        entry.profile(),
                        entry.boxId(),
                        entry.date(),
                        entry.time())) {
                    continue;
                }
                table.add(bodyCell(entry.action()), 0, row);
                table.add(bodyCell(entry.file()), 1, row);
                table.add(bodyCell(entry.document()), 2, row);
                table.add(bodyCell(entry.profile()), 3, row);
                table.add(bodyCell(entry.boxId()), 4, row);
                table.add(bodyCell(entry.date()), 5, row);
                table.add(bodyCell(entry.time()), 6, row);
                row++;
            }
        };
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshTable.run());
        refreshTable.run();

        panel.getChildren().addAll(header, table);
        return panel;
    }

    private VBox buildMyScansSideColumn() {
        List<ActivityLogEntry> myActivity = myScanActivity();
        long exportCount = myActivity.stream().filter(entry -> "Exported".equals(entry.action())).count();
        long qaCount = myActivity.stream().filter(entry -> entry.action().contains("QA")).count();

        VBox column = new VBox(18);
        column.setPrefWidth(220);
        column.setMinWidth(220);
        column.setMaxWidth(220);

        VBox summaryPanel = new VBox(16);
        summaryPanel.getStyleClass().add("panel-card");
        summaryPanel.setPadding(new Insets(18));
        Label summaryTitle = new Label("My Summary");
        summaryTitle.getStyleClass().add("panel-title");
        summaryPanel.getChildren().addAll(
                summaryTitle,
                infoBlock("My Log Entries", String.valueOf(myActivity.size())),
                infoBlock("Exports", String.valueOf(exportCount)),
                infoBlock("QA Actions", String.valueOf(qaCount))
        );

        column.getChildren().add(summaryPanel);
        return column;
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
                                   List<ScanningDocument> scanningDocuments,
                                   List<ScanningDocument> scannedWorkspaceDocuments,
                                   ScanningDocument activeScanningDocument,
                                   Set<String> qaCompletedDocuments,
                                   Consumer<ScanningDocument> onSelectScanningDocument,
                                   Consumer<ScanningFile> onOpenScanningFile,
                                   Runnable onOpenQaReview,
                                   Runnable onScanningChanged,
                                   Runnable onBackToProfiles,
                                   Runnable onScanAllDocuments) {
        VBox body = new VBox(4);
        body.getStyleClass().add("dashboard-body");

        Label heading = new Label("Scanned Files");
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label(selectedProfile.title() + " / " + currentBoxId);
        subtitle.getStyleClass().add("dashboard-subtitle");
        Button backButton = new Button("Back To Profiles");
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

        VBox center = new VBox(8);
        HBox.setHgrow(center, Priority.ALWAYS);

        ComboBox<String> assignDocumentCombo = new ComboBox<>();
        for (ScanningDocument document : scannedWorkspaceDocuments.isEmpty() ? scanningDocuments : scannedWorkspaceDocuments) {
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
                onSelectScanningDocument.accept(findScanningDocument(scannedWorkspaceDocuments.isEmpty() ? scanningDocuments : scannedWorkspaceDocuments, selectedTitle));
            }
        });

        HBox topPanel = new HBox(6,
                compactScanBlock("Scan Date", "2026-04-27"),
                compactScanBlock("Scanned Documents", String.valueOf(scannedWorkspaceDocuments.size())),
                compactScanBlock("Scanned Files", String.valueOf(totalFiles(scannedWorkspaceDocuments))),
                compactScanComboBlock("Assign To Document", assignDocumentCombo)
        );
        topPanel.getStyleClass().add("scan-top-strip");
        topPanel.setMinHeight(70);
        topPanel.setPrefHeight(70);
        topPanel.setMaxHeight(70);

        VBox workspacePanel = new VBox(8);
        workspacePanel.getStyleClass().add("scan-main-panel");
        HBox.setHgrow(workspacePanel, Priority.ALWAYS);
        VBox.setVgrow(workspacePanel, Priority.ALWAYS);

        VBox documentBoard;
        if (scannedWorkspaceDocuments.isEmpty()) {
            Label emptyState = new Label("No document scanned yet.");
            emptyState.getStyleClass().add("scan-empty-state");
            documentBoard = new VBox(emptyState);
            documentBoard.getStyleClass().add("scan-empty-board");
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
            documentBoard = new VBox(10);
            documentBoard.getChildren().addAll(sections);
        }

        ScrollPane documentScroll = new ScrollPane(documentBoard);
        documentScroll.setFitToWidth(true);
        documentScroll.getStyleClass().add("scan-document-scroll");
        VBox.setVgrow(documentScroll, Priority.ALWAYS);

        workspacePanel.getChildren().add(documentScroll);
        center.getChildren().addAll(topPanel, workspacePanel);
        VBox.setVgrow(center, Priority.ALWAYS);

        layout.getChildren().add(center);
        HBox.setHgrow(center, Priority.ALWAYS);
        headerRow.getStyleClass().add("scan-header-row");
        body.getChildren().addAll(headerRow, layout);
        return body;
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
                                     Consumer<String> onSelectTiffAction) {
        BorderPane body = new BorderPane();
        body.getStyleClass().add("dashboard-body");

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
        boolean exportReady = !selectedTiffAction.isBlank() && !qaFiles.isEmpty();

        Label brandMark = new Label("P");
        brandMark.getStyleClass().add("qa-brand-mark");
        Label brandName = new Label("PrismScan");
        brandName.getStyleClass().add("qa-brand-name");
        HBox brandBlock = new HBox(8, brandMark, brandName);
        brandBlock.setAlignment(Pos.CENTER_LEFT);

        HBox breadcrumbs = new HBox(10,
                new Label("Profiles"),
                new Label(">"),
                new Label(profileName),
                new Label(">"),
                new Label(currentBoxId.isBlank() ? "Box" : currentBoxId),
                new Label(">"),
                new Label(documentName)
        );
        breadcrumbs.getStyleClass().add("qa-breadcrumbs");
        breadcrumbs.setAlignment(Pos.CENTER_LEFT);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerRow = new HBox(14, brandBlock, breadcrumbs, headerSpacer);
        headerRow.getStyleClass().add("qa-app-header");
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Button backButton = new Button("Back To Scanning");
        backButton.getStyleClass().add("page-window-cancel-button");
        backButton.setOnAction(event -> onBackToScanning.run());

        Button dashboardButton = new Button("Back To Dashboard");
        dashboardButton.getStyleClass().add("page-window-cancel-button");
        dashboardButton.setOnAction(event -> onDashboard.run());

        if (qaFiles.isEmpty() || selectedFile == null) {
            VBox emptyPanel = new VBox(new Label("No scanned files available for QA yet."));
            emptyPanel.getStyleClass().add("panel-card");
            emptyPanel.setPadding(new Insets(22));
            body.setTop(headerRow);
            body.setCenter(emptyPanel);
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
        TextField documentSearch = new TextField();
        documentSearch.setPromptText("Search documents");
        documentSearch.getStyleClass().addAll("box-id-field", "qa-side-search");
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
        documentsPanel.getChildren().addAll(documentsHeader, documentSearch, documentScroll);

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

        VBox leftColumn = new VBox(10, documentsPanel, filesPanel);
        leftColumn.getStyleClass().add("qa-left-column");
        leftColumn.setPrefWidth(240);
        leftColumn.setMinWidth(240);
        leftColumn.setMaxWidth(240);

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

        HBox toolbar = new HBox(8,
                highlightButton,
                zoomOutButton,
                zoomInButton,
                qaToolbarSelect(Math.round((qaZoom[0] / 0.78) * 100) + "%"),
                panLeftButton,
                panRightButton,
                panUpButton,
                panDownButton
        );
        toolbar.getStyleClass().add("qa-toolbar");

        Button previousButton = qaPagerButton("<", () -> onStepPage.accept(-1));
        Button nextButton = qaPagerButton(">", () -> onStepPage.accept(1));
        Button rotateButton = qaPagerButton("Rotate", onRotatePage);
        Label pageCounter = new Label(qaCurrentPage[0] + " / " + selectedFile.pageCount());
        pageCounter.getStyleClass().add("qa-pager-label");
        HBox pageControls = new HBox(10, previousButton, pageCounter, nextButton, rotateButton);
        pageControls.getStyleClass().add("qa-page-controls");
        pageControls.setAlignment(Pos.CENTER);

        VBox previewColumn = new VBox(10, toolbar, previewStage, pageControls);
        previewColumn.getStyleClass().add("qa-preview-column");
        previewColumn.setMinHeight(570);
        previewColumn.setPrefHeight(570);
        previewColumn.setMaxHeight(570);
        HBox.setHgrow(previewColumn, Priority.ALWAYS);

        VBox selectionPanel = qaSidePanel("Current Selection",
                qaMetaBlock("Profile", profileName),
                qaMetaBlock("Box", currentBoxId),
                qaMetaBlock("Document", selectedDocument == null ? "-" : selectedDocument.title()),
                qaMetaBlock("File", selectedFile.badge()),
                qaMetaBlock("File Reference ID", selectedFile.reference().replace("Ref: ", "")),
                qaMetaBlock("File ID", selectedFile.badge().replace("File ", "")),
                qaMetaBlock("Scanned At", "May 15, 2024 10:15 AM"),
                qaMetaBlock("Scanned By", "John Doe")
        );

        Button rejectButton = qaActionButton("Reject, Send Back To Scanning", true, onReject);
        VBox toolsPanel = qaUntitledSidePanel(rejectButton);

        Button multiPageTiff = qaActionButton("Multi Page TIFF", false, () -> {
            onSelectTiffAction.accept("Multi Page TIFF");
            onApprove.run();
        });
        Button singlePageTiff = qaActionButton("Single Page TIFF", false, () -> {
            onSelectTiffAction.accept("Single Page TIFF");
            onApprove.run();
        });
        if ("Multi Page TIFF".equals(selectedTiffAction)) {
            multiPageTiff.getStyleClass().add("qa-action-selected");
        }
        if ("Single Page TIFF".equals(selectedTiffAction)) {
            singlePageTiff.getStyleClass().add("qa-action-selected");
        }
        VBox exportFilenamePanel = qaExportFilenamePanel(exportFileName);
        VBox actionsPanel = qaUntitledSidePanel(multiPageTiff, singlePageTiff, exportFilenamePanel);

        VBox rightColumn = new VBox(10, selectionPanel, toolsPanel, actionsPanel);
        rightColumn.getStyleClass().add("qa-right-column");
        rightColumn.setPrefWidth(190);
        rightColumn.setMinWidth(190);
        rightColumn.setMaxWidth(190);
        rightColumn.setMinHeight(570);
        rightColumn.setPrefHeight(570);
        rightColumn.setMaxHeight(570);

        HBox workspace = new HBox(10, leftColumn, previewColumn, rightColumn);
        workspace.getStyleClass().add("qa-workspace");
        workspace.setMinHeight(570);
        workspace.setPrefHeight(570);
        workspace.setMaxHeight(570);
        HBox.setHgrow(previewColumn, Priority.ALWAYS);

        ComboBox<String> rotationCombo = new ComboBox<>();
        rotationCombo.getItems().addAll("0 deg (No rotation)", "90 deg", "180 deg", "270 deg");
        rotationCombo.getSelectionModel().select(0);
        rotationCombo.getStyleClass().add("qa-export-combo");
        rotationCombo.setMaxWidth(Double.MAX_VALUE);

        Button exportDocumentButton = new Button("Export Document");
        SVGPath exportIcon = new SVGPath();
        exportIcon.setContent("M12 3V15 M7 10L12 15L17 10 M5 21H19");
        exportIcon.getStyleClass().add("qa-export-button-icon");
        exportDocumentButton.setGraphic(exportIcon);
        exportDocumentButton.setGraphicTextGap(8);
        exportDocumentButton.getStyleClass().addAll("approve-button", "qa-export-document-button");
        exportDocumentButton.setMaxWidth(Double.MAX_VALUE);
        exportDocumentButton.setDisable(!exportReady || !qaApproved[0]);
        exportDocumentButton.setOnAction(event -> onExport.run());

        HBox footer = new HBox(12,
                qaProgressStat("Scanning Progress", qaFiles.size() + " / 250 files"),
                qaFooterControl("Profile Rotation", rotationCombo),
                exportDocumentButton
        );
        footer.getStyleClass().add("qa-footer-bar");
        footer.setMinHeight(54);
        footer.setPrefHeight(54);
        footer.setMaxHeight(54);
        HBox.setHgrow(footer.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);

        body.setTop(headerRow);
        body.setCenter(workspace);
        body.setBottom(footer);
        return body;
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
        Label badge = new Label("File " + (index + 1));
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
            case "My Scans" -> "M14 2H6A2 2 0 0 0 4 4V20A2 2 0 0 0 6 22H18A2 2 0 0 0 20 20V8L14 2Z M14 2V8H20 M8 13H16 M8 17H14";
            case "Exports" -> "M21 8V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V8 M1 3H23V8H1Z M10 12H14";
            default -> "M12 5V19 M5 12H19";
        };
    }

    private PageState actionCardTarget(String title) {
        return switch (title) {
            case "Start Scan" -> PageState.PROFILES;
            case "Exports" -> PageState.LOGS;
            case "My Scans" -> PageState.MY_SCANS;
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
        header.setPadding(new Insets(18, 22, 18, 22));

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
        pill.getStyleClass().add("status-pill");
        if ("Ready".equals(status)) {
            pill.getStyleClass().add("status-ready");
        } else {
            pill.getStyleClass().add("status-processing");
        }

        HBox wrap = new HBox(pill);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.getStyleClass().add("table-row-wrap");
        return wrap;
    }

    private HBox exportActionCell(ExportRow export) {
        HBox wrap = new HBox(6);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.getStyleClass().add("table-row-wrap");
        if (!"Ready".equals(export.status())) {
            Label pending = new Label("Pending");
            pending.getStyleClass().add("export-pending-text");
            wrap.getChildren().add(pending);
            return wrap;
        }

        SVGPath downloadIcon = new SVGPath();
        downloadIcon.setContent("M12 3V15 M7 10L12 15L17 10 M5 21H19");
        downloadIcon.getStyleClass().add("export-download-icon");
        Label download = new Label("Download");
        download.getStyleClass().add("export-download-text");
        wrap.getStyleClass().add("export-download-action");
        wrap.getChildren().addAll(downloadIcon, download);
        return wrap;
    }

    private PageState mapPage(String label) {
        return switch (label) {
            case "Dashboard" -> PageState.DASHBOARD;
            case "Exports" -> PageState.LOGS;
            case "Profiles" -> PageState.PROFILES;
            default -> PageState.MY_SCANS;
        };
    }

    private List<ScanningDocument> copyScanningDocuments(PageMode pageMode) {
        PageMode mode = resolvedPageMode(pageMode);
        List<ScanningDocument> copies = new ArrayList<>();
        for (ScanningDocument document : scanningDocuments()) {
            List<ScanningFile> files = new ArrayList<>();
            for (ScanningFile file : document.files()) {
                files.add(mode == PageMode.SINGLE_PAGE
                        ? new ScanningFile(file.badge(), file.fileName(), file.reference(), "1 page", 1, file.barcode())
                        : file);
            }
            copies.add(new ScanningDocument(
                    document.title(),
                    document.fileCountText(),
                    files
            ));
        }
        return copies;
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
                                      String targetReference) {
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
        targetFiles.add(targetIndex, movingFile);
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

        VBox content = new VBox(12);
        content.getStyleClass().add("dashboard-body");

        Label heading = new Label(file.fileName());
        heading.getStyleClass().add("dashboard-heading");
        Label subtitle = new Label(document.title() + " / " + file.reference().replace("Ref: ", "") + " / " + file.pages());
        subtitle.getStyleClass().add("dashboard-subtitle");

        FlowPane pages = new FlowPane(12, 12);
        Runnable[] renderPages = new Runnable[1];
        renderPages[0] = () -> {
            pages.getChildren().clear();
            for (Integer pageNumber : workingPages) {
                pages.getChildren().add(buildPagePreviewCard(pageNumber, workingPages, renderPages));
            }
        };
        renderPages[0].run();

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("page-window-cancel-button");
        Button saveButton = new Button("Save");
        saveButton.getStyleClass().add("page-window-save-button");

        HBox footer = new HBox(10, cancelButton, saveButton);
        footer.getStyleClass().add("page-window-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox panel = new VBox(14, heading, subtitle, pages);
        panel.getStyleClass().add("scan-main-panel");
        panel.setPadding(new Insets(14));

        Stage stage = new Stage();
        stage.setTitle("File Pages - " + file.fileName());
        BorderPane windowRoot = new BorderPane();
        windowRoot.getStyleClass().add("dashboard-shell");
        windowRoot.setCenter(panel);
        windowRoot.setBottom(footer);
        BorderPane.setMargin(footer, new Insets(0, 14, 14, 14));

        Scene windowScene = new Scene(windowRoot, 760, 560);
        windowScene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setScene(windowScene);
        stage.setMinWidth(640);
        stage.setMinHeight(480);

        cancelButton.setOnAction(event -> {
            workingPages.clear();
            workingPages.addAll(originalPages);
            stage.close();
        });
        saveButton.setOnAction(event -> stage.close());

        stage.show();
    }

    private void enableSmoothFileDrag(VBox card,
                                      ScanningDocument document,
                                      ScanningFile file,
                                      Consumer<ScanningFile> onOpenScanningFile,
                                      Runnable onScanningChanged) {
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
                if (!card.getStyleClass().contains("scan-file-card-dragging")) {
                    card.getStyleClass().add("scan-file-card-dragging");
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
                onOpenScanningFile.accept(file);
                return;
            }
            Node targetNode = findClosestFileCard(card, event.getSceneX(), event.getSceneY());
            card.setTranslateX(0);
            card.setTranslateY(0);
            card.getStyleClass().remove("scan-file-card-dragging");
            dragging[0] = false;

            if (targetNode instanceof VBox targetCard) {
                String targetReference = (String) targetCard.getProperties().get("referenceId");
                ScanningDocument targetDocument = (ScanningDocument) targetCard.getProperties().get("scanningDocument");
                if (targetReference != null && moveOrReorderFile(document, targetDocument, file.reference(), targetReference)) {
                    onScanningChanged.run();
                }
            }
        });

        card.getProperties().put("referenceId", file.reference());
        card.getProperties().put("scanningDocument", document);
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
            if (!node.getProperties().containsKey("referenceId")) {
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

        return closestDistance <= 140 ? closest : null;
    }

    private Node findClosestPageCard(VBox draggedCard, double sceneX, double sceneY) {
        if (!(draggedCard.getParent() instanceof FlowPane flowPane)) {
            return null;
        }
        Node closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Node node : flowPane.getChildren()) {
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
