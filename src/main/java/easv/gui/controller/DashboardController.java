package easv.gui.controller;

import easv.gui.UserPortalModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DashboardController {

    public enum PageState {
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

    private final UserPortalModel portalModel;

    private PageState currentPage = PageState.DASHBOARD;
    private UserPortalModel.ScanProfile selectedProfile;
    private String currentBoxId = "";
    private List<UserPortalModel.ScanningDocument> scanningDocuments;
    private List<UserPortalModel.ScanningDocument> scannedWorkspaceDocuments = new ArrayList<>();
    private UserPortalModel.ScanningDocument activeScanningDocument;
    private UserPortalModel.ScanningFile activeScanningFile;
    private boolean darkMode;
    private int qaCurrentPage = 1;
    private int qaRotation;
    private double qaZoom = 0.78;
    private double qaPanX;
    private double qaPanY;
    private boolean qaHighlight;
    private boolean qaApproved;
    private final Set<String> qaCompletedDocuments = new HashSet<>();
    private final Set<String> qaHighlightedText = new HashSet<>();
    private String selectedTiffAction = "";
    private double scanProgress;
    private Timeline scanProgressTimeline;
    private Runnable onStateChanged = () -> {
    };

    public DashboardController(UserPortalModel portalModel) {
        this.portalModel = portalModel;
        this.scanningDocuments = copyScanningDocuments();
    }

    public void setOnStateChanged(Runnable onStateChanged) {
        this.onStateChanged = onStateChanged == null ? () -> {
        } : onStateChanged;
    }

    public PageState currentPage() {
        return currentPage;
    }

    public UserPortalModel.ScanProfile selectedProfile() {
        return selectedProfile;
    }

    public String currentBoxId() {
        return currentBoxId;
    }

    public List<UserPortalModel.ScanningDocument> scanningDocuments() {
        return scanningDocuments;
    }

    public List<UserPortalModel.ScanningDocument> scannedWorkspaceDocuments() {
        return scannedWorkspaceDocuments;
    }

    public UserPortalModel.ScanningDocument activeScanningDocument() {
        return activeScanningDocument;
    }

    public UserPortalModel.ScanningFile activeScanningFile() {
        return activeScanningFile;
    }

    public boolean darkMode() {
        return darkMode;
    }

    public int qaCurrentPage() {
        return qaCurrentPage;
    }

    public int qaRotation() {
        return qaRotation;
    }

    public double qaZoom() {
        return qaZoom;
    }

    public double qaPanX() {
        return qaPanX;
    }

    public double qaPanY() {
        return qaPanY;
    }

    public boolean qaHighlight() {
        return qaHighlight;
    }

    public boolean qaApproved() {
        return qaApproved;
    }

    public Set<String> qaCompletedDocuments() {
        return qaCompletedDocuments;
    }

    public Set<String> qaHighlightedText() {
        return qaHighlightedText;
    }

    public String selectedTiffAction() {
        return selectedTiffAction;
    }

    public double scanProgress() {
        return scanProgress;
    }

    public void navigateTo(PageState page) {
        if (page != PageState.SCAN_PROGRESS) {
            stopScanProgress();
        }
        currentPage = page;
        requestRender();
    }

    public void toggleDarkMode() {
        darkMode = !darkMode;
        requestRender();
    }

    public void selectProfile(UserPortalModel.ScanProfile profile) {
        selectedProfile = profile;
        requestRender();
    }

    public void startScanning(String boxId) {
        currentBoxId = boxId;
        scanningDocuments = copyScanningDocuments();
        scannedWorkspaceDocuments = new ArrayList<>();
        activeScanningDocument = null;
        activeScanningFile = null;
        resetQaReviewState();
        qaCompletedDocuments.clear();
        qaHighlightedText.clear();
        selectedTiffAction = "";
        scanProgress = 0;
        stopScanProgress();
        currentPage = PageState.SCAN_PROGRESS;
        requestRender();

        scanProgressTimeline = new Timeline(new KeyFrame(Duration.millis(35), event -> advanceScanProgress()));
        scanProgressTimeline.setCycleCount(Timeline.INDEFINITE);
        scanProgressTimeline.play();
    }

    public void selectScanningDocument(UserPortalModel.ScanningDocument document) {
        if (!containsDocument(scannedWorkspaceDocuments, document.title())) {
            scannedWorkspaceDocuments.add(document);
        }
        activeScanningDocument = document;
        activeScanningFile = null;
        requestRender();
    }

    public void scanAllDocuments() {
        addAllScannedDocuments();
        if (!scannedWorkspaceDocuments.isEmpty()) {
            activeScanningDocument = scannedWorkspaceDocuments.get(0);
        }
        activeScanningFile = null;
        requestRender();
    }

    public void openScanningFile(UserPortalModel.ScanningFile file) {
        activeScanningFile = file;
        currentPage = PageState.FILE_PAGES;
        requestRender();
    }

    public void openQaReview() {
        if (!scannedWorkspaceDocuments.isEmpty()) {
            if (activeScanningFile == null) {
                activeScanningDocument = scannedWorkspaceDocuments.get(0);
                if (!activeScanningDocument.files().isEmpty()) {
                    activeScanningFile = activeScanningDocument.files().get(0);
                }
            } else {
                activeScanningDocument = findDocumentForFile(scannedWorkspaceDocuments, activeScanningFile);
            }
        }
        resetQaReviewState();
        currentPage = PageState.QA_REVIEW;
        requestRender();
    }

    public void openScanningWorkspace() {
        currentPage = PageState.SCANNING;
        requestRender();
    }

    public void showProfiles() {
        stopScanProgress();
        currentPage = PageState.PROFILES;
        requestRender();
    }

    public void selectQaFile(UserPortalModel.ScanningFile file) {
        activeScanningFile = file;
        activeScanningDocument = findDocumentForFile(scannedWorkspaceDocuments, file);
        qaCurrentPage = 1;
        qaRotation = 0;
        qaPanX = 0;
        qaPanY = 0;
        requestRender();
    }

    public void showDashboard() {
        stopScanProgress();
        currentPage = PageState.DASHBOARD;
        requestRender();
    }

    public void exportQa() {
        if (!qaApproved) {
            return;
        }
        currentPage = PageState.MY_SCANS;
        requestRender();
    }

    public void stepQaPage(int offset) {
        List<UserPortalModel.ScanningFile> qaFiles = flattenScannedFiles(scannedWorkspaceDocuments);
        if (activeScanningFile == null || qaFiles.isEmpty()) {
            return;
        }

        int currentIndex = findFileIndex(qaFiles, activeScanningFile);
        if (currentIndex < 0) {
            return;
        }

        int nextPage = qaCurrentPage + offset;
        if (nextPage >= 1 && nextPage <= activeScanningFile.pageCount()) {
            qaCurrentPage = nextPage;
            requestRender();
            return;
        }

        int nextFileIndex = currentIndex + offset;
        if (nextFileIndex >= 0 && nextFileIndex < qaFiles.size()) {
            activeScanningFile = qaFiles.get(nextFileIndex);
            activeScanningDocument = findDocumentForFile(scannedWorkspaceDocuments, activeScanningFile);
            qaCurrentPage = offset < 0 ? activeScanningFile.pageCount() : 1;
            qaRotation = 0;
            qaZoom = 0.78;
            qaPanX = 0;
            qaPanY = 0;
            requestRender();
        }
    }

    public void rotateQaPage() {
        qaRotation = (qaRotation + 90) % 360;
        requestRender();
    }

    public void zoomQaPage(double zoomStep) {
        qaZoom = Math.max(0.48, Math.min(1.22, qaZoom + zoomStep));
        if (qaZoom <= 0.78) {
            qaPanX = 0;
            qaPanY = 0;
        }
        requestRender();
    }

    public void panQaPage(double deltaX, double deltaY) {
        qaPanX = Math.max(-170, Math.min(170, qaPanX + deltaX));
        qaPanY = Math.max(-170, Math.min(170, qaPanY + deltaY));
        requestRender();
    }

    public void toggleQaHighlight() {
        qaHighlight = !qaHighlight;
        requestRender();
    }

    public void toggleQaTextHighlight(String highlightId) {
        if (qaHighlightedText.contains(highlightId)) {
            qaHighlightedText.remove(highlightId);
        } else {
            qaHighlightedText.add(highlightId);
        }
        requestRender();
    }

    public void approveQa() {
        if (activeScanningDocument != null) {
            qaCompletedDocuments.add(activeScanningDocument.title());
        }
        qaApproved = true;
        requestRender();
    }

    public void rejectQa() {
        qaApproved = false;
        currentPage = PageState.SCANNING;
        requestRender();
    }

    public void selectTiffAction(String tiffAction) {
        selectedTiffAction = tiffAction;
        requestRender();
    }

    public void refresh() {
        requestRender();
    }

    public String scanStepState(double progress, double stepStart) {
        if (progress >= stepStart + 26) {
            return "complete";
        }
        if (progress >= stepStart) {
            return "active";
        }
        return "pending";
    }

    public String scanProfileName(UserPortalModel.ScanProfile profile) {
        return profile == null ? portalModel.activeProfile().profileName() : profile.title();
    }

    public String documentQaStatus(UserPortalModel.ScanningDocument document) {
        if (document != null && qaCompletedDocuments.contains(document.title())) {
            return "QA Completed";
        }
        if (document != null && activeScanningDocument != null && document.title().equals(activeScanningDocument.title())) {
            return "In Progress";
        }
        return "Waiting for QA";
    }

    public String buildExportFileName(String profileName, String boxId) {
        String safeProfileName = profileName == null || profileName.isBlank()
                ? "Profile"
                : profileName.replaceAll("\\s+", "");
        String safeBoxId = boxId == null || boxId.isBlank() ? "Box" : boxId.strip();
        return safeProfileName + "_" + safeBoxId + ".pdf";
    }

    public PageState mapPage(String label) {
        return switch (label) {
            case "Dashboard" -> PageState.DASHBOARD;
            case "Exports" -> PageState.LOGS;
            case "Profiles" -> PageState.PROFILES;
            default -> PageState.MY_SCANS;
        };
    }

    public PageState actionCardTarget(String title) {
        return switch (title) {
            case "Start Scan" -> PageState.PROFILES;
            case "Exports" -> PageState.LOGS;
            case "My Scans" -> PageState.MY_SCANS;
            default -> PageState.DASHBOARD;
        };
    }

    public UserPortalModel.ScanningDocument findScanningDocument(List<UserPortalModel.ScanningDocument> documents, String title) {
        for (UserPortalModel.ScanningDocument document : documents) {
            if (document.title().equals(title)) {
                return document;
            }
        }
        return documents.isEmpty() ? null : documents.get(0);
    }

    public UserPortalModel.ScanningDocument findDocumentForFile(List<UserPortalModel.ScanningDocument> documents,
                                                                UserPortalModel.ScanningFile file) {
        if (file == null) {
            return null;
        }
        for (UserPortalModel.ScanningDocument document : documents) {
            for (UserPortalModel.ScanningFile candidate : document.files()) {
                if (candidate.reference().equals(file.reference())) {
                    return document;
                }
            }
        }
        return null;
    }

    public List<UserPortalModel.ScanningFile> flattenScannedFiles(List<UserPortalModel.ScanningDocument> documents) {
        List<UserPortalModel.ScanningFile> files = new ArrayList<>();
        for (UserPortalModel.ScanningDocument document : documents) {
            files.addAll(document.files());
        }
        return files;
    }

    public boolean moveOrReorderFile(UserPortalModel.ScanningDocument sourceDocument,
                                     UserPortalModel.ScanningDocument targetDocument,
                                     String sourceReference,
                                     String targetReference) {
        if (sourceDocument == null
                || targetDocument == null
                || sourceReference == null
                || targetReference == null
                || sourceReference.equals(targetReference)) {
            return false;
        }

        List<UserPortalModel.ScanningFile> sourceFiles = sourceDocument.files();
        List<UserPortalModel.ScanningFile> targetFiles = targetDocument.files();
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

        UserPortalModel.ScanningFile movingFile = sourceFiles.remove(sourceIndex);
        if (sourceDocument == targetDocument && sourceIndex < targetIndex) {
            targetIndex--;
        }
        targetFiles.add(targetIndex, movingFile);
        return true;
    }

    public boolean reorderPages(List<Integer> workingPages, int sourcePage, int targetPage) {
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

    public int totalFiles(List<UserPortalModel.ScanningDocument> documents) {
        int total = 0;
        for (UserPortalModel.ScanningDocument document : documents) {
            total += document.files().size();
        }
        return total;
    }

    public String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    public boolean matchesSearch(String search, String... values) {
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

    private void advanceScanProgress() {
        if (currentPage != PageState.SCAN_PROGRESS) {
            stopScanProgress();
            return;
        }

        scanProgress = Math.min(100, scanProgress + 1.2);
        if (scanProgress >= 100) {
            addAllScannedDocuments();
            if (!scannedWorkspaceDocuments.isEmpty()) {
                activeScanningDocument = scannedWorkspaceDocuments.get(0);
            }
            activeScanningFile = null;
            currentPage = PageState.SCAN_COMPLETE;
            stopScanProgress();
        }
        requestRender();
    }

    private void addAllScannedDocuments() {
        for (UserPortalModel.ScanningDocument document : scanningDocuments) {
            if (!containsDocument(scannedWorkspaceDocuments, document.title())) {
                scannedWorkspaceDocuments.add(document);
            }
        }
    }

    private void resetQaReviewState() {
        qaCurrentPage = 1;
        qaRotation = 0;
        qaZoom = 0.78;
        qaPanX = 0;
        qaPanY = 0;
        qaHighlight = false;
        qaApproved = false;
    }

    private void stopScanProgress() {
        if (scanProgressTimeline != null) {
            scanProgressTimeline.stop();
            scanProgressTimeline = null;
        }
    }

    private List<UserPortalModel.ScanningDocument> copyScanningDocuments() {
        List<UserPortalModel.ScanningDocument> copies = new ArrayList<>();
        for (UserPortalModel.ScanningDocument document : portalModel.scanningDocuments()) {
            copies.add(new UserPortalModel.ScanningDocument(
                    document.title(),
                    document.fileCountText(),
                    new ArrayList<>(document.files())
            ));
        }
        return copies;
    }

    private boolean containsDocument(List<UserPortalModel.ScanningDocument> documents, String title) {
        for (UserPortalModel.ScanningDocument document : documents) {
            if (document.title().equals(title)) {
                return true;
            }
        }
        return false;
    }

    private int findFileIndex(List<UserPortalModel.ScanningFile> files, UserPortalModel.ScanningFile target) {
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

    private void requestRender() {
        onStateChanged.run();
    }
}
