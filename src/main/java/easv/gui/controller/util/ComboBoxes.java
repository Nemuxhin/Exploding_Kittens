package easv.gui.controller.util;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.layout.Region;

/**
 * Global ComboBox visual behaviour.
 * <p>
 * The default JavaFX ComboBox popup is sized to the widest item, with no
 * floor — short items make a popup narrower than the trigger button, which
 * looks broken. {@link #matchPopupWidth(ComboBox)} enforces a floor: the
 * popup is at least as wide as the trigger, but it can grow larger when
 * an item needs more room (so long text never gets clipped).
 * <p>
 * {@link #applyToScene(Scene)} walks the scene graph once and watches for
 * dynamically-added nodes, so every ComboBox in the app (including those
 * created later by controllers) gets the binding for free.
 */
public final class ComboBoxes {
    private static final String BOUND_KEY = "weblager.popupWidthBound";
    private static final String SCENE_WATCHED_KEY = "weblager.comboPopupSceneWatched";

    private ComboBoxes() {
    }

    /**
     * Ensure the ComboBox popup is never narrower than the trigger button.
     * The popup is still free to grow wider when content needs more room.
     */
    public static void matchPopupWidth(ComboBox<?> combo) {
        if (combo == null) {
            return;
        }
        if (Boolean.TRUE.equals(combo.getProperties().get(BOUND_KEY))) {
            return;
        }
        combo.getProperties().put(BOUND_KEY, true);

        if (combo.getSkin() != null) {
            bindPopupWidth(combo);
        }
        combo.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                bindPopupWidth(combo);
            }
        });
    }

    /**
     * Walk a Scene's node tree, apply {@link #matchPopupWidth(ComboBox)} to
     * every ComboBox, and watch for new ComboBoxes added later.
     */
    public static void applyToScene(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        if (Boolean.TRUE.equals(scene.getProperties().get(SCENE_WATCHED_KEY))) {
            return;
        }
        scene.getProperties().put(SCENE_WATCHED_KEY, true);
        walk(scene.getRoot());
    }

    private static void bindPopupWidth(ComboBox<?> combo) {
        if (!(combo.getSkin() instanceof ComboBoxListViewSkin<?> skin)) {
            return;
        }
        Node popupContent = skin.getPopupContent();
        if (!(popupContent instanceof ListView<?> list)) {
            return;
        }
        // Floor only — the popup must never be narrower than the trigger, but
        // is free to grow when content needs more room. Leaving pref/max
        // unbound lets JavaFX size the popup to fit the longest item.
        list.minWidthProperty().bind(combo.widthProperty());
    }

    private static void walk(Node node) {
        if (node == null) {
            return;
        }
        if (node instanceof ComboBox<?> combo) {
            matchPopupWidth(combo);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                walk(child);
            }
            parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        for (Node added : change.getAddedSubList()) {
                            walk(added);
                        }
                    }
                }
            });
        }
    }
}
