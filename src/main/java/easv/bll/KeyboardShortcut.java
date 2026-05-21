package easv.bll;

/**
 * This class describes one keyboard shortcut shown in the help dialog.
 * It keeps the key, the action name, and a simple human explanation together.
 */
public class KeyboardShortcut {

    private final String displayKeys;
    private final String keyCombination;
    private final String actionName;
    private final String description;

    public KeyboardShortcut(String displayKeys, String keyCombination, String actionName, String description) {
        this.displayKeys = displayKeys;
        this.keyCombination = keyCombination;
        this.actionName = actionName;
        this.description = description;
    }

    public String getDisplayKeys() {
        return displayKeys;
    }

    public String getKeyCombination() {
        return keyCombination;
    }

    public String getActionName() {
        return actionName;
    }

    public String getDescription() {
        return description;
    }
}
