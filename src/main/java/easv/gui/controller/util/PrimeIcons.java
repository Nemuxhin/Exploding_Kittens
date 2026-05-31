package easv.gui.controller.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

public final class PrimeIcons {

    private static final String DEFAULT_PRIME_ICON_FONT_FAMILY = "primeicons";
    private static final String PRIME_ICON_CLASS = "prime-icon";
    private static final String PRIME_ICON_CONFIGURED_FAMILY_KEY = "weblager.primeIconConfiguredFamily";

    private static String primeIconFontFamily = DEFAULT_PRIME_ICON_FONT_FAMILY;
    private static boolean primeIconFontLoaded;

    private PrimeIcons() {
    }

    public static Label create(String glyph, String... styleClasses) {
        Label icon = new Label(glyph);
        icon.getStyleClass().add(PRIME_ICON_CLASS);

        if (styleClasses != null) {
            icon.getStyleClass().addAll(styleClasses);
        }

        applyFont(icon);
        return icon;
    }

    public static void registerFont(Font font) {
        if (font == null || font.getFamily() == null || font.getFamily().isBlank()) {
            primeIconFontFamily = DEFAULT_PRIME_ICON_FONT_FAMILY;
            primeIconFontLoaded = false;
            return;
        }

        primeIconFontFamily = font.getFamily();
        primeIconFontLoaded = true;
    }

    public static void applyFont(Node node) {
        if (node == null) {
            return;
        }

        normalizeStyleClasses(node);

        if (node instanceof Label label && isPrimeIconLabel(label)) {
            applyFont(label);
        }

        if (node instanceof Labeled labeled && labeled.getGraphic() != null) {
            applyFont(labeled.getGraphic());
        }

        if (node instanceof ScrollPane scrollPane) {
            applyFont(scrollPane.getContent());
        }

        if (node instanceof TitledPane titledPane) {
            applyFont(titledPane.getContent());
        }

        if (node instanceof TabPane tabPane) {
            tabPane.getTabs().forEach(tab -> applyFont(tab.getContent()));
        }

        if (node instanceof SplitPane splitPane) {
            splitPane.getItems().forEach(PrimeIcons::applyFont);
        }

        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(PrimeIcons::applyFont);
        }
    }

    public static void applyFont(Label label) {
        if (label == null) {
            return;
        }

        normalizeStyleClasses(label);

        if (!label.getStyleClass().contains(PRIME_ICON_CLASS)) {
            label.getStyleClass().add(PRIME_ICON_CLASS);
        }

        if (!primeIconFontLoaded && applyVectorPrimeIcon(label)) {
            return;
        }

        double size = label.getFont() == null ? 12 : label.getFont().getSize();
        label.setFont(Font.font(primeIconFontFamily, size));

        if (primeIconFontFamily.equals(label.getProperties().get(PRIME_ICON_CONFIGURED_FAMILY_KEY))) {
            return;
        }

        String currentStyle = label.getStyle();
        String primeIconStyle = "-fx-font-family: \"" + primeIconFontFamily + "\"; -fx-font-weight: 400;";
        label.setStyle(currentStyle == null || currentStyle.isBlank()
                ? primeIconStyle
                : currentStyle + "; " + primeIconStyle);
        label.getProperties().put(PRIME_ICON_CONFIGURED_FAMILY_KEY, primeIconFontFamily);
    }

    private static boolean isPrimeIconLabel(Label label) {
        if (label == null) {
            return false;
        }

        if (label.getStyleClass().contains(PRIME_ICON_CLASS)) {
            return true;
        }

        String text = label.getText();
        return text != null && text.codePoints().anyMatch(PrimeIcons::isPrivateUseGlyph);
    }

    private static void normalizeStyleClasses(Node node) {
        if (node == null || node.getStyleClass().isEmpty()) {
            return;
        }

        List<String> normalizedClasses = new ArrayList<>();
        boolean changed = false;

        for (String styleClass : node.getStyleClass()) {
            if (styleClass == null || styleClass.isBlank()) {
                changed = true;
                continue;
            }

            String trimmedClass = styleClass.trim();
            String[] splitClasses = trimmedClass.split("\\s+");

            if (splitClasses.length != 1 || !trimmedClass.equals(styleClass)) {
                changed = true;
            }

            for (String splitClass : splitClasses) {
                if (!splitClass.isBlank() && !normalizedClasses.contains(splitClass)) {
                    normalizedClasses.add(splitClass);
                }
            }
        }

        if (changed) {
            node.getStyleClass().setAll(normalizedClasses);
        }
    }

    private static boolean isPrivateUseGlyph(int codePoint) {
        return codePoint >= 0xE000 && codePoint <= 0xF8FF;
    }

    private static boolean applyVectorPrimeIcon(Label label) {
        String text = label.getText();

        if (text == null || text.isBlank()) {
            return label.getGraphic() instanceof SVGPath;
        }

        int codePoint = text.codePointAt(0);

        if (!isPrivateUseGlyph(codePoint)) {
            return false;
        }

        String pathContent = vectorPathForPrimeIcon(codePoint);

        if (pathContent == null) {
            return false;
        }

        SVGPath iconPath = new SVGPath();
        iconPath.setContent(pathContent);
        iconPath.getStyleClass().add("prime-icon-vector");
        iconPath.getStyleClass().addAll(label.getStyleClass());
        iconPath.setScaleX(0.72);
        iconPath.setScaleY(0.72);

        label.setText(null);
        label.setGraphic(iconPath);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.setGraphicTextGap(0);

        return true;
    }

    private static String vectorPathForPrimeIcon(int codePoint) {
        return switch (codePoint) {
            case 0xE908 -> "M10 4a6 6 0 1 0 0 12a6 6 0 0 0 0-12zm0 2a4 4 0 1 1 0 8a4 4 0 0 1 0-8zm4.9 8.5l5.3 5.3-1.4 1.4-5.3-5.3z";
            case 0xE90A -> "M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
            case 0xE90B -> "M12 4a8 8 0 1 0 0 16a8 8 0 0 0 0-16zm3.5 10.1-1.4 1.4L12 13.4l-2.1 2.1-1.4-1.4 2.1-2.1-2.1-2.1 1.4-1.4 2.1 2.1 2.1-2.1 1.4 1.4-2.1 2.1z";
            case 0xE915 -> "M7 6h10l-5 5zm0 12h10l-5-5z";
            case 0xE922 -> "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
            case 0xE925 -> "M3 10.5 12 3l9 7.5V21h-6v-6H9v6H3z";
            case 0xE92C -> "M16 1H6a2 2 0 0 0-2 2v12h2V3h10zm3 4H10a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H10V7h9z";
            case 0xE927 -> "M7 2h2v3h6V2h2v3h3v16H4V5h3zm11 8H6v9h12z";
            case 0xE930 -> "M7 9.5 12 14.5 17 9.5z";
            case 0xE934 -> "M11 16h2V8l3.5 3.5L18 10 12 4 6 10l1.5 1.5L11 8zm-6 2h14v2H5z";
            case 0xE938 -> "M17.7 6.3A8 8 0 1 0 20 12h-2a6 6 0 1 1-1.8-4.2L13 11h8V3z";
            case 0xE939 -> "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z";
            case 0xE93D -> "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
            case 0xE93F -> "M10 11a4 4 0 1 0 0-8a4 4 0 0 0 0 8zm-7 9a7 7 0 0 1 10-6.3 5.5 5.5 0 0 0-1 6.3zm14.2 1L14 17.8l1.4-1.4 1.8 1.8 4-4 1.4 1.4z";
            case 0xE940 -> "M12 3a9 9 0 1 0 0 18a9 9 0 0 0 0-18zm1 9.2 4 2.3-1 1.7-5-3V7h2z";
            case 0xE941 -> "M8.5 11a3.5 3.5 0 1 0 0-7a3.5 3.5 0 0 0 0 7zm7 0a3 3 0 1 0 0-6a3 3 0 0 0 0 6zM2 20a6.5 6.5 0 0 1 13 0zm11.5-6a5.5 5.5 0 0 1 6.5 5.4V20h-3a8.4 8.4 0 0 0-3.5-6z";
            case 0xE942 -> "M4 17.2V21h3.8L18.9 9.9l-3.8-3.8zM17 4l3 3-1.5 1.5-3-3z";
            case 0xE94C -> "M3 5h18l-7 8v5l-4 2v-7z";
            case 0xE94A -> "M19.4 13.5a7.7 7.7 0 0 0 0-3l2-1.5-2-3.4-2.4 1a8 8 0 0 0-2.6-1.5L14 2h-4l-.4 3.1A8 8 0 0 0 7 6.6l-2.4-1-2 3.4 2 1.5a7.7 7.7 0 0 0 0 3l-2 1.5 2 3.4 2.4-1a8 8 0 0 0 2.6 1.5L10 22h4l.4-3.1a8 8 0 0 0 2.6-1.5l2.4 1 2-3.4zM12 15a3 3 0 1 1 0-6a3 3 0 0 1 0 6z";
            case 0xE956 -> "M11 4h2v8l3.5-3.5L18 10l-6 6-6-6 1.5-1.5L11 12zm-6 14h14v2H5z";
            case 0xE958 -> "M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm-1 7V3.5L18.5 9H13z";
            case 0xE959 -> "M12 3a9 9 0 1 0 0 18a9 9 0 0 0 0-18zm-1 14h2v2h-2zm1-12a3.5 3.5 0 0 1 2 6.4c-.8.6-1 1-1 2.1V14h-2v-.7c0-1.7.6-2.6 1.7-3.4A1.5 1.5 0 1 0 10.5 8H8.6A3.4 3.4 0 0 1 12 5z";
            case 0xE95C -> "M5 3h12l2 2v16H5zm3 2v5h8V5zm0 9v5h8v-5z";
            case 0xE97C -> "M12 22a2.5 2.5 0 0 0 2.4-2h-4.8A2.5 2.5 0 0 0 12 22zm7-5-2-2v-4a5 5 0 0 0-4-4.9V4h-2v2.1A5 5 0 0 0 7 11v4l-2 2v1h14z";
            case 0xE981 -> "M3 5h18v14H3zm2 3v8h14V8zm2 2h2v2H7zm3 0h2v2h-2zm3 0h2v2h-2zM7 13h10v2H7z";
            case 0xE992 -> "M5 4h14v4H5zm0 6h14v4H5zm0 6h14v4H5zm2-10v1h2V6zm0 6v1h2v-1zm0 6v1h2v-1z";
            case 0xE9C7 -> "M20 15.5A8 8 0 0 1 8.5 4 8.5 8.5 0 1 0 20 15.5z";
            case 0xE9C8 -> "M12 7a5 5 0 1 0 0 10a5 5 0 0 0 0-10zm-1-5h2v3h-2zm0 17h2v3h-2zM2 11h3v2H2zm17 0h3v2h-3zM4.2 5.6l1.4-1.4 2.1 2.1-1.4 1.4zm12.1 12.1 1.4-1.4 2.1 2.1-1.4 1.4zM18.4 4.2l1.4 1.4-2.1 2.1-1.4-1.4zM6.3 16.3l1.4 1.4-2.1 2.1-1.4-1.4z";
            case 0xE9E4 -> "M4 17h16v2H4zm2-4 4-4 3 3 5-6 2 2-7 8-3-3-3 3z";
            case 0xEA0C -> "M3 13h4l2-6 4 12 2-6h6v-2h-7l-1 3-4-12-3 9H3z";
            case 0xEA1B -> "M6 3h9l3 3v15H6zm8 1.5V7h2.5zM9 12h6v2H9zm0 4h6v2H9z";
            default -> null;
        };
    }

}
