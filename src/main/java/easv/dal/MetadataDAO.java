package easv.dal;

import easv.be.ScanProfile;

import java.util.List;

public class MetadataDAO {
    private final DatabaseConnection databaseConnection;
    private final ScanProfileStore scanProfileStore;

    public MetadataDAO() {
        this(new DatabaseConnection());
    }

    public MetadataDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection == null ? new DatabaseConnection() : databaseConnection;
        this.scanProfileStore = new ScanProfileStore(this.databaseConnection);
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
}
