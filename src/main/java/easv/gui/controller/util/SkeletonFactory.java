package easv.gui.controller.util;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * PrimeNG-style skeleton placeholders for data-loading states.
 * Builds grey rectangles/circles that mirror the shape of the data
 * they stand in for, and runs a subtle FadeTransition shimmer.
 *
 * Always pair attachShimmer with stopShimmers (or clearLabel) before
 * the skeleton is detached from the scene graph — otherwise the
 * FadeTransition keeps a strong reference to the node.
 */
public final class SkeletonFactory {
    private static final Object SHIMMER_KEY = new Object();
    private static final Duration SHIMMER_DURATION = Duration.millis(1100);
    private static final double SHIMMER_LOW = 0.45;
    private static final double SHIMMER_HIGH = 1.0;

    public enum Intensity {
        DARK("mock-line-dark"),
        MEDIUM("mock-line-medium"),
        LIGHT("mock-line-light");

        final String cssClass;
        Intensity(String cssClass) { this.cssClass = cssClass; }
    }

    private SkeletonFactory() {}

    public static Region line(double width, double height) {
        return line(width, height, Intensity.MEDIUM);
    }

    public static Region line(double width, double height, Intensity intensity) {
        Region region = new Region();
        region.getStyleClass().add(intensity.cssClass);
        sizeExactly(region, width, height);
        attachShimmer(region);
        return region;
    }

    public static Region circle(double diameter) {
        Region region = new Region();
        region.getStyleClass().add("skeleton-circle");
        sizeExactly(region, diameter, diameter);
        attachShimmer(region);
        return region;
    }

    public static void applyToLabel(Label label, double width, double height) {
        if (label == null) {
            return;
        }
        label.setText("");
        if (!label.getStyleClass().contains("skeleton-label")) {
            label.getStyleClass().add("skeleton-label");
        }
        sizeExactly(label, width, height);
        attachShimmer(label);
    }

    public static void clearLabel(Label label) {
        if (label == null) {
            return;
        }
        label.getStyleClass().remove("skeleton-label");
        resetSize(label);
        detachShimmer(label);
    }

    public static void stopShimmers(Node root) {
        if (root == null) {
            return;
        }
        detachShimmer(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                stopShimmers(child);
            }
        }
    }

    private static void sizeExactly(Region region, double width, double height) {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
    }

    private static void resetSize(Region region) {
        region.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        region.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        region.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
    }

    private static void attachShimmer(Node node) {
        detachShimmer(node);
        FadeTransition fade = new FadeTransition(SHIMMER_DURATION, node);
        fade.setFromValue(SHIMMER_HIGH);
        fade.setToValue(SHIMMER_LOW);
        fade.setAutoReverse(true);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.play();
        node.getProperties().put(SHIMMER_KEY, fade);
    }

    private static void detachShimmer(Node node) {
        Object existing = node.getProperties().remove(SHIMMER_KEY);
        if (existing instanceof FadeTransition fade) {
            fade.stop();
        }
        node.setOpacity(1.0);
    }
}
