package easv.gui;

import javafx.scene.control.Label;

public class StatusBadge extends Label {
    public StatusBadge(String statusText) {
        super(statusText);
        getStyleClass().add("status-badge");
        String normalized = statusText == null ? "" : statusText.trim().toLowerCase().replace(' ', '-');
        getStyleClass().add("status-" + normalized);
    }
}
