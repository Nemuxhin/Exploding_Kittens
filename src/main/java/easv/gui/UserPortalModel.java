package easv.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserPortalModel {
    private static final String ACTIVE_USER = "Current User";
    private static final String CURRENT_DATE = "24/04/2026";
    private final List<ActivityLogEntry> activityLog = new ArrayList<>();

    public enum ExportMode {
        MULTI_PAGE("Multi page TIFF"),
        SINGLE_PAGE("Single page TIFF");

        private final String label;

        ExportMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record BoxItem(String id, String description) {
        @Override
        public String toString() {
            return id + " - " + description;
        }
    }

    public record ProfileItem(int id, String name, String splitRule, List<BoxItem> boxes) {
        @Override
        public String toString() {
            return name;
        }
    }

    public record DashboardDocument(String name, List<String> files) {
        @Override
        public String toString() {
            return name + " (" + files.size() + " files)";
        }
    }

    public record ActivityLogEntry(String user, String action, String file, String document, String profile, String boxId, String date, String time) {
    }

    public static class PortalFile {
        private final int referenceId;
        private int fileId;
        private final String apiFileName;
        private final boolean barcode;
        private final int pageCount;
        private int rotationDegrees;
        private boolean qaApproved;

        PortalFile(int referenceId, int fileId, String apiFileName, boolean barcode, int pageCount) {
            this.referenceId = referenceId;
            this.fileId = fileId;
            this.apiFileName = apiFileName;
            this.barcode = barcode;
            this.pageCount = pageCount;
        }

        public int getReferenceId() {
            return referenceId;
        }

        public int getFileId() {
            return fileId;
        }

        public void setFileId(int fileId) {
            this.fileId = fileId;
        }

        public String getApiFileName() {
            return apiFileName;
        }

        public boolean isBarcode() {
            return barcode;
        }

        public int getPageCount() {
            return pageCount;
        }

        public int getRotationDegrees() {
            return rotationDegrees;
        }

        public void rotateClockwise() {
            rotationDegrees = (rotationDegrees + 90) % 360;
        }

        public boolean isQaApproved() {
            return qaApproved;
        }

        public void setQaApproved(boolean qaApproved) {
            this.qaApproved = qaApproved;
        }

        public String getDisplayName() {
            return "File " + fileId + " (ref " + referenceId + ", " + pageCount + " page" + (pageCount == 1 ? "" : "s") + ")" + (barcode ? " [BARCODE]" : "");
        }

        @Override
        public String toString() {
            return getDisplayName() + (qaApproved ? " [QA]" : "");
        }
    }

    public static class PortalDocument {
        private int id;
        private final List<PortalFile> files = new ArrayList<>();

        PortalDocument(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public List<PortalFile> getFiles() {
            return files;
        }

        @Override
        public String toString() {
            return "Document " + id + " (" + files.size() + " files)";
        }
    }

    public static class PortalSession {
        private final ProfileItem profile;
        private final BoxItem box;
        private final List<String> readyFiles = new ArrayList<>();
        private final List<PortalDocument> documents = new ArrayList<>();
        private int totalScans;
        private int nextReferenceId = 1;

        PortalSession(ProfileItem profile, BoxItem box, List<String> selectedFiles) {
            this.profile = profile;
            this.box = box;
            this.readyFiles.addAll(selectedFiles);
            documents.add(new PortalDocument(1));
        }

        public ProfileItem getProfile() {
            return profile;
        }

        public BoxItem getBox() {
            return box;
        }

        public List<PortalDocument> getDocuments() {
            return documents;
        }

        public List<String> getReadyFiles() {
            return readyFiles;
        }

        public int getTotalScans() {
            return totalScans;
        }

        public String getExportLabel() {
            return profile.name().replaceAll("\\s+", "") + "_" + box.id();
        }

        public List<PortalFile> getAllFiles() {
            List<PortalFile> files = new ArrayList<>();
            for (PortalDocument document : documents) {
                files.addAll(document.getFiles());
            }
            return files;
        }
    }

    public record ScanResult(PortalFile file, boolean createdNewDocument, String message) {
    }

    public List<ProfileItem> fetchProfilesForUser() {
        List<ProfileItem> profiles = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            String prefix = "P" + String.format("%02d", i);
            profiles.add(new ProfileItem(
                    i,
                    "Profile " + i,
                    "Split when " + prefix + " barcode is found",
                    buildBoxes(prefix, "Archive box", 1000 + (i * 100), 100)
            ));
        }
        return profiles;
    }

    public List<ActivityLogEntry> getActivityLog() {
        if (activityLog.isEmpty()) {
            seedActivityLog();
        }
        return List.copyOf(activityLog);
    }

    public PortalSession startSession(ProfileItem profile, BoxItem box) {
        return startSession(profile, box, previewDashboardDocuments(profile, box));
    }

    public PortalSession startSession(ProfileItem profile, BoxItem box, List<String> selectedFiles) {
        List<String> files = selectedFiles == null || selectedFiles.isEmpty()
                ? previewDashboardDocuments(profile, box)
                : selectedFiles;
        logActivity(ACTIVE_USER, "Opened", files.size() + " selected file(s)", "-", profile.name(), box.id(), CURRENT_DATE, nextLogTime());
        return new PortalSession(profile, box, files);
    }

    public List<String> previewDashboardDocuments(ProfileItem profile, BoxItem box) {
        List<String> files = new ArrayList<>();
        for (DashboardDocument document : fetchDashboardDocuments(profile, box)) {
            files.addAll(document.files());
        }
        return files;
    }

    public List<DashboardDocument> fetchDashboardDocuments(ProfileItem profile, BoxItem box) {
        String prefix = profile.name() + " - " + box.id();
        List<DashboardDocument> documents = new ArrayList<>();
        int fileNumber = 1;
        for (int docIndex = 1; docIndex <= 20; docIndex++) {
            List<String> files = new ArrayList<>();
            for (int fileIndex = 1; fileIndex <= 12; fileIndex++) {
                String fileName = prefix + " - Document " + docIndex + " - File " + fileNumber;
                if (fileIndex == 3) {
                    fileName += " multi pages";
                } else if (fileIndex == 8) {
                    fileName += " barcode split";
                }
                files.add(fileName);
                fileNumber++;
            }
            documents.add(new DashboardDocument("Document " + docIndex, files));
        }
        return documents;
    }

    public ScanResult scanNextFile(PortalSession session) {
        boolean barcode = session.nextReferenceId > 1 && session.nextReferenceId % 4 == 1;
        return addScannedFile(session, barcode);
    }

    public ScanResult scanNormalFile(PortalSession session) {
        return addScannedFile(session, false);
    }

    public ScanResult scanBarcodeFile(PortalSession session) {
        return addScannedFile(session, true);
    }

    public ScanResult scanDroppedFile(PortalSession session, String fileName) {
        session.getReadyFiles().remove(fileName);
        String normalizedName = fileName == null ? "" : fileName.toLowerCase();
        boolean barcode = normalizedName.contains("barcode") || normalizedName.contains("split");
        int pageCount = normalizedName.contains("multi") || normalizedName.contains("pages") ? 3 : 1;
        return addScannedFile(session, barcode, fileName == null || fileName.isBlank() ? null : fileName, pageCount);
    }

    public List<ScanResult> scanSampleBatch(PortalSession session) {
        return List.of(
                addScannedFile(session, false),
                addScannedFile(session, false),
                addScannedFile(session, true),
                addScannedFile(session, false),
                addScannedFile(session, false),
                addScannedFile(session, true),
                addScannedFile(session, false)
        );
    }

    private ScanResult addScannedFile(PortalSession session, boolean barcode) {
        return addScannedFile(session, barcode, null, 1);
    }

    private ScanResult addScannedFile(PortalSession session, boolean barcode, String fileName, int pageCount) {
        int referenceId = session.nextReferenceId++;
        String sourceName = fileName == null ? "api-scan-" + referenceId + ".tiff" : fileName;
        PortalFile file = new PortalFile(referenceId, referenceId, sourceName, barcode, pageCount);
        session.totalScans++;

        PortalDocument targetDocument = session.documents.get(session.documents.size() - 1);
        boolean createdNewDocument = false;
        if (file.isBarcode() && !targetDocument.getFiles().isEmpty()) {
            targetDocument = new PortalDocument(session.documents.size() + 1);
            session.documents.add(targetDocument);
            createdNewDocument = true;
        }
        targetDocument.getFiles().add(file);
        renumberFiles(session);
        String message = createdNewDocument
                ? "Barcode found. A new document was created and " + file.getDisplayName() + " was added there."
                : file.getDisplayName() + " was added to Document " + targetDocument.getId() + ".";
        logActivity(ACTIVE_USER,
                "Scanned",
                sourceName,
                "Document " + targetDocument.getId(),
                session.getProfile().name(),
                session.getBox().id(),
                CURRENT_DATE,
                nextLogTime());
        if (createdNewDocument) {
            logActivity(ACTIVE_USER,
                    "Created",
                    sourceName,
                    "Document " + targetDocument.getId(),
                    session.getProfile().name(),
                    session.getBox().id(),
                    CURRENT_DATE,
                    nextLogTime());
        }
        return new ScanResult(file, createdNewDocument, message);
    }

    public void moveFileUp(PortalSession session, PortalDocument document, PortalFile file) {
        int index = document == null ? -1 : document.getFiles().indexOf(file);
        if (index > 0) {
            Collections.swap(document.getFiles(), index, index - 1);
            renumberFiles(session);
        }
    }

    public void moveFileDown(PortalSession session, PortalDocument document, PortalFile file) {
        int index = document == null ? -1 : document.getFiles().indexOf(file);
        if (index >= 0 && index < document.getFiles().size() - 1) {
            Collections.swap(document.getFiles(), index, index + 1);
            renumberFiles(session);
        }
    }

    public void moveFileToDocument(PortalSession session, PortalDocument source, PortalDocument target, PortalFile file) {
        if (source == null || target == null || file == null || source == target) {
            return;
        }
        if (source.getFiles().remove(file)) {
            target.getFiles().add(file);
            cleanupDocuments(session);
            renumberFiles(session);
            logActivity(ACTIVE_USER,
                    "Moved",
                    file.getApiFileName(),
                    "Document " + target.getId(),
                    session.getProfile().name(),
                    session.getBox().id(),
                    CURRENT_DATE,
                    nextLogTime());
        }
    }

    public PortalDocument createDocument(PortalSession session) {
        PortalDocument document = new PortalDocument(session.getDocuments().size() + 1);
        session.getDocuments().add(document);
        logActivity(ACTIVE_USER, "Created", "-", "Document " + document.getId(), session.getProfile().name(), session.getBox().id(), CURRENT_DATE, nextLogTime());
        return document;
    }

    public void logCustomAction(String action) {
        logActivity(ACTIVE_USER, action, "-", "-", "-", "-", CURRENT_DATE, nextLogTime());
    }

    public boolean isQaComplete(PortalSession session) {
        return session != null && !session.getAllFiles().isEmpty()
                && session.getAllFiles().stream().allMatch(PortalFile::isQaApproved);
    }

    public List<String> previewExportFiles(PortalSession session, ExportMode mode) {
        List<String> names = new ArrayList<>();
        if (mode == ExportMode.MULTI_PAGE) {
            for (PortalDocument document : session.getDocuments()) {
                names.add(session.getExportLabel() + "_DOC-" + document.getId() + ".tiff");
            }
        } else {
            for (PortalDocument document : session.getDocuments()) {
                for (PortalFile file : document.getFiles()) {
                    names.add(session.getExportLabel() + "_DOC-" + document.getId() + "_FILE-" + file.getFileId() + ".tiff");
                }
            }
        }
        return names;
    }

    private void cleanupDocuments(PortalSession session) {
        if (session.getDocuments().size() > 1) {
            session.getDocuments().removeIf(document -> document.getFiles().isEmpty());
        }
        for (int i = 0; i < session.getDocuments().size(); i++) {
            session.getDocuments().get(i).setId(i + 1);
        }
    }

    private void renumberFiles(PortalSession session) {
        int fileId = 1;
        for (PortalDocument document : session.getDocuments()) {
            for (PortalFile file : document.getFiles()) {
                file.setFileId(fileId++);
            }
        }
    }

    private List<BoxItem> buildBoxes(String prefix, String descriptionPrefix, int startNumber, int count) {
        List<BoxItem> boxes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int boxNumber = startNumber + i;
            boxes.add(new BoxItem(prefix + "-BOX-" + boxNumber, descriptionPrefix + " " + (i + 1)));
        }
        return boxes;
    }

    private void logActivity(String user, String action, String file, String document, String profile, String boxId, String date, String time) {
        activityLog.add(0, new ActivityLogEntry(user, action, file, document, profile, boxId, date, time));
        if (activityLog.size() > 60) {
            activityLog.remove(activityLog.size() - 1);
        }
    }

    private void seedActivityLog() {
        activityLog.clear();
        activityLog.add(new ActivityLogEntry("Mike", "Deleted", "File 05", "Old Contract", "Legal", "Box 18", "23/04/2026", "11:10"));
        activityLog.add(new ActivityLogEntry("Admin", "Created User", "New Scanner User", "-", "-", "-", "23/04/2026", "11:30"));
        activityLog.add(new ActivityLogEntry("Emma", "Exported", "Document A", "Invoice A", "Finance", "Box 12", "23/04/2026", "10:45"));
        activityLog.add(new ActivityLogEntry("Emma", "QA Approved", "Document A", "Invoice A", "Finance", "Box 12", "23/04/2026", "10:30"));
        activityLog.add(new ActivityLogEntry("John", "Sent to QA", "Document A", "Invoice A", "Finance", "Box 12", "23/04/2026", "10:00"));
        activityLog.add(new ActivityLogEntry("Sara", "Updated", "File 03", "Contract B", "Legal", "Box 15", "23/04/2026", "09:25"));
        activityLog.add(new ActivityLogEntry("Sara", "Moved", "File 03", "Contract B", "Legal", "Box 15", "23/04/2026", "09:20"));
        activityLog.add(new ActivityLogEntry("Ahmed", "Scanned", "File 03", "Invoice A", "Finance", "Box 12", "23/04/2026", "09:08"));
        activityLog.add(new ActivityLogEntry("Ahmed", "Scanned", "File 02", "Invoice A", "Finance", "Box 12", "23/04/2026", "09:05"));
        activityLog.add(new ActivityLogEntry("Ahmed", "Created", "File 01", "Invoice A", "Finance", "Box 12", "23/04/2026", "09:00"));
    }

    private String nextLogTime() {
        int index = activityLog.size();
        int hour = 9 + (index / 6);
        int minute = 10 + (index * 7 % 50);
        boolean pm = hour >= 12;
        int displayHour = hour % 12 == 0 ? 12 : hour % 12;
        return String.format("%d:%02d %s", displayHour, minute, pm ? "PM" : "AM");
    }
}
