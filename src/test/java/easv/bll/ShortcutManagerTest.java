package easv.bll;

import org.junit.jupiter.api.Test;
import javafx.scene.input.KeyCombination;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortcutManagerTest {

    @Test
    void shortcutListContainsTheRequiredCommonActions() {
        ShortcutManager shortcutManager = new ShortcutManager();

        List<String> actionNames = shortcutManager.getShortcuts()
                .stream()
                .map(KeyboardShortcut::getActionName)
                .toList();

        assertTrue(actionNames.contains("Next section / scan page"));
        assertTrue(actionNames.contains("Previous section / scan page"));
        assertTrue(actionNames.contains("Rotate"));
        assertTrue(actionNames.contains("Delete"));
        assertTrue(actionNames.contains("Undo"));
        assertTrue(actionNames.contains("Save"));
        assertTrue(actionNames.contains("Search / jump"));
        assertTrue(actionNames.contains("Export"));
        assertTrue(actionNames.contains("Zoom in"));
        assertTrue(actionNames.contains("Zoom out"));
        assertTrue(actionNames.contains("Escape"));
        assertTrue(actionNames.contains("Shortcut help"));
    }

    @Test
    void shortcutListHasThirteenEntriesIncludingFnF1AndQuestionMarkHelp() {
        ShortcutManager shortcutManager = new ShortcutManager();

        List<KeyboardShortcut> shortcuts = shortcutManager.getShortcuts();

        assertEquals(13, shortcuts.size());
        assertTrue(shortcuts.stream().anyMatch(shortcut -> "Fn + F1".equals(shortcut.getDisplayKeys())));
        assertTrue(shortcuts.stream().anyMatch(shortcut -> "?".equals(shortcut.getDisplayKeys())));
        assertTrue(shortcuts.stream().anyMatch(shortcut -> shortcut.getDisplayKeys().contains("Backspace")));
        assertTrue(shortcuts.stream().anyMatch(shortcut -> shortcut.getDescription().contains("90 degrees")));
    }

    @Test
    void everyShortcutCanBeRegisteredByJavaFx() {
        ShortcutManager shortcutManager = new ShortcutManager();

        for (KeyboardShortcut shortcut : shortcutManager.getShortcuts()) {
            assertDoesNotThrow(() -> KeyCombination.valueOf(shortcut.getKeyCombination()));
        }
    }
}
