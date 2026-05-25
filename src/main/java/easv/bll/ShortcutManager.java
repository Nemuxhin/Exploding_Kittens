package easv.bll;

import java.util.List;

/**
 * This class stores the shortcut list in one place.
 * The GUI uses the same list to register shortcuts and to show the help dialog.
 */
public class ShortcutManager {

    public List<KeyboardShortcut> getShortcuts() {
        return List.of(
                new KeyboardShortcut("Right Arrow", "RIGHT", "Next section / scan page", "Move to the next portal section, or the selected page while scanning."),
                new KeyboardShortcut("Left Arrow", "LEFT", "Previous section / scan page", "Move to the previous portal section, or the selected page while scanning."),
                new KeyboardShortcut("R", "R", "Rotate", "Rotate the selected page by 1 degree."),
                new KeyboardShortcut("Delete / Backspace", "DELETE", "Delete", "Delete the selected page."),
                new KeyboardShortcut("Ctrl + Z", "CTRL+Z", "Undo", "Undo the last supported action."),
                new KeyboardShortcut("Ctrl + S", "CTRL+S", "Save", "Save progress when the current page supports it."),
                new KeyboardShortcut("Ctrl + F", "CTRL+F", "Search / jump", "Find a page by reference, file name, or document number."),
                new KeyboardShortcut("Ctrl + E", "CTRL+E", "Export", "Open the Exports section."),
                new KeyboardShortcut("+", "PLUS", "Zoom in", "Zoom in on the current page."),
                new KeyboardShortcut("-", "MINUS", "Zoom out", "Zoom out from the current page."),
                new KeyboardShortcut("Esc", "ESCAPE", "Escape", "Close modals or cancel the current action."),
                new KeyboardShortcut("Fn + F1", "F1", "Shortcut help", "Open the keyboard shortcuts help dialog."),
                new KeyboardShortcut("?", "SHIFT+SLASH", "Shortcut help", "Open the keyboard shortcuts help dialog.")
        );
    }
}
