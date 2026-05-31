package easv.dal;

import easv.be.ReviewRecord;
import easv.be.ScanProfile;

import java.util.List;

public class MetadataDAO {
    private final ScanProfileStore scanProfileStore;
    private final ReviewRecordDAO reviewRecordDAO;

    public MetadataDAO() {
        this(new DatabaseConnection());
    }

    public MetadataDAO(DatabaseConnection databaseConnection) {
        DatabaseConnection connection = databaseConnection == null ? new DatabaseConnection() : databaseConnection;
        this.scanProfileStore = new ScanProfileStore(connection);
        this.reviewRecordDAO = new ReviewRecordDAO(connection);
    }

    public List<ScanProfile> getProfiles() {
        return scanProfileStore.listAll();
    }

    public int nextProfileId() {
        return scanProfileStore.nextProfileId();
    }

    public ScanProfile saveProfile(ScanProfile profile) {
        return scanProfileStore.insert(profile);
    }

    public void updateProfile(ScanProfile profile) {
        scanProfileStore.update(profile);
    }

    public void deleteProfile(int profileId) {
        scanProfileStore.delete(profileId);
    }

    public List<ReviewRecord> getReviewRecords() {
        return reviewRecordDAO.getReviewRecords();
    }

    public void saveReviewRecord(ReviewRecord record) {
        reviewRecordDAO.saveReviewRecord(record);
    }
}
