package easv.gui.controller.admin;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

final class AdminKeyboard {

    private AdminKeyboard() {
    }

    static void runOnActivationKey(KeyEvent event, Runnable action) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            action.run();
            event.consume();
        }
    }

    static void makeActivatable(Node node, String accessibleText, Runnable action) {
        node.setAccessibleRole(AccessibleRole.BUTTON);
        node.setAccessibleText(accessibleText);
        node.setFocusTraversable(true);
        node.setOnKeyPressed(event -> runOnActivationKey(event, action));
    }
}
