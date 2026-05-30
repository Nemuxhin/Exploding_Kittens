package easv.bll;

import easv.be.ScanProfile;

import java.util.prefs.Preferences;

/**
 * Per-profile autosave configuration kept in Java {@link Preferences} (the same
 * local store the portal uses for the dark-mode setting) rather than the
 * database, so enabling this feature never requires a schema change. Keyed by
 * profile name because that is the only handle the scan workspace has when it
 * needs to look the settings back up.
 */
public class AutosaveSettingsStore {

    private static final String NODE = "easv.gui.autosave";
    private static final String ENABLED_SUFFIX = ".enabled";
    private static final String INTERVAL_SUFFIX = ".interval";
    private static final String LOCKED_SUFFIX = ".locked";

    private final Preferences preferences = Preferences.userRoot().node(NODE);

    public record Settings(boolean enabled, int intervalSeconds, boolean locked) {}

    public Settings read(String profileName) {
        String key = keyFor(profileName);
        return new Settings(
                preferences.getBoolean(key + ENABLED_SUFFIX, true),
                preferences.getInt(key + INTERVAL_SUFFIX, ScanProfile.DEFAULT_AUTOSAVE_INTERVAL_SECONDS),
                preferences.getBoolean(key + LOCKED_SUFFIX, false)
        );
    }

    public void write(String profileName, boolean enabled, int intervalSeconds, boolean locked) {
        String key = keyFor(profileName);
        preferences.putBoolean(key + ENABLED_SUFFIX, enabled);
        preferences.putInt(key + INTERVAL_SUFFIX, intervalSeconds);
        preferences.putBoolean(key + LOCKED_SUFFIX, locked);
    }

    private String keyFor(String profileName) {
        return "profile." + (profileName == null ? "" : profileName.trim().toLowerCase());
    }
}
