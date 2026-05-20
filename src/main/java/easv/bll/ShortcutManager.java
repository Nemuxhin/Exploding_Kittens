package easv.bll;

import java.util.List;

/**
 * This class stores the shortcut list in one place.
 * The GUI uses the same list to register shortcuts and to show the help dialog.
 */
public class ShortcutManager {

    public List<KeyboardShortcut> getShortcuts() {
        return List.of(
                new KeyboardShortcut("Right Arrow", "RIGHT", "Next page", "Move to the next scanned page."),
                new KeyboardShortcut("Left Arrow", "LEFT", "Previous page", "Move to the previous scanned page."),
                new KeyboardShortcut("R", "R", "Rotate", "Rotate the selected page."),
                new KeyboardShortcut("Delete", "DELETE", "Delete", "Delete the selected page."),
                new KeyboardShortcut("Ctrl + Z", "CTRL+Z", "Undo", "Undo the last supported action."),
                new KeyboardShortcut("Ctrl + S", "CTRL+S", "Save", "Save the current work."),
                new KeyboardShortcut("Ctrl + F", "CTRL+F", "Search / jump", "Jump to a scanned page by reference number."),
                new KeyboardShortcut("Ctrl + E", "CTRL+E", "Export", "Open the TIFF export window."),
                new KeyboardShortcut("+", "PLUS", "Zoom in", "Zoom in on the current page."),
                new KeyboardShortcut("-", "MINUS", "Zoom out", "Zoom out from the current page."),
                new KeyboardShortcut("Esc", "ESCAPE", "Escape", "Close modals or cancel the current action."),
                new KeyboardShortcut("F1", "F1", "Shortcut help", "Open the keyboard shortcuts help page."),
                new KeyboardShortcut("?", "SHIFT+SLASH", "Shortcut help", "Open the keyboard shortcuts help page.")
        );
    }
}
