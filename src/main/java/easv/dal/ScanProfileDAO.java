package easv.dal;

import easv.be.ScanProfile;

import java.util.List;
import java.util.Optional;

public class ScanProfileDAO {
    private final ScanProfileStore scanProfileStore;
    private volatile boolean defaultProfilesChecked = false;

    public ScanProfileDAO() {
        this(new DatabaseConnection());
    }

    public ScanProfileDAO(DatabaseConnection databaseConnection) {
        this.scanProfileStore = new ScanProfileStore(databaseConnection == null ? new DatabaseConnection() : databaseConnection);
    }

    public List<ScanProfile> findAll() {
        ensureDefaultProfilesIfNeeded();
        return scanProfileStore.listAll();
    }

    public Optional<ScanProfile> findById(int profileId) {
        return scanProfileStore.findById(profileId);
    }

    public Optional<ScanProfile> findByName(String profileName) {
        return scanProfileStore.findByName(profileName);
    }

    public ScanProfile save(ScanProfile profile) {
        if (profile.getId() > 0) {
            return update(profile);
        }
        return scanProfileStore.insert(profile);
    }

    public ScanProfile update(ScanProfile profile) {
        return scanProfileStore.update(profile);
    }

    private void ensureDefaultProfiles() {
        if (!scanProfileStore.listAll().isEmpty()) {
            return;
        }

        save(new ScanProfile(0, "Building Archive", "building-archive", "Used for municipal building archive scans.",
                "Active", "", ScanProfile.DEFAULT_EXPORT_NAMING, "Created just now", false, true,
                "Continue scanning and split automatically", "Remove barcode page from final document",
                "0 deg", "Normal", "Normal", true, "PDF", true));
        save(new ScanProfile(0, "Technical Drawings", "technical-drawings", "Large format technical drawing imports.",
                "Draft", "", ScanProfile.DEFAULT_EXPORT_NAMING, "Created just now", false, false,
                "Start new document", "Keep barcode page in final document",
                "0 deg", "Normal", "Higher", true, "PDF/A", true));
    }

    private void ensureDefaultProfilesIfNeeded() {
        if (defaultProfilesChecked) {
            return;
        }

        synchronized (this) {
            if (defaultProfilesChecked) {
                return;
            }

            ensureDefaultProfiles();
            defaultProfilesChecked = true;
        }
    }
}
