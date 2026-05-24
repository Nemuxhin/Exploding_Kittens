package easv.bll;

import easv.be.PageImage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ImageRotationService is an empty stub — rotation state lives on PageImage.
 * These tests pin the PageImage rotation rules so a future service can
 * delegate to them without guessing the expected behaviour.
 */
class ImageRotationServiceTest {

    @Test
    void setRotationDegrees_shouldNormalizeToZeroToThreeHundredFiftyNineRange() {
        PageImage page = new PageImage(1, PageImage.PageType.TIFF, "page-001.png");

        page.setRotationDegrees(0);
        int at0 = page.getRotationDegrees();

        page.setRotationDegrees(90);
        int at90 = page.getRotationDegrees();

        page.setRotationDegrees(360);
        int at360 = page.getRotationDegrees();

        page.setRotationDegrees(450);
        int at450 = page.getRotationDegrees();

        page.setRotationDegrees(-90);
        int atMinus90 = page.getRotationDegrees();

        page.setRotationDegrees(-180);
        int atMinus180 = page.getRotationDegrees();

        assertAll(
                () -> assertEquals(0,   at0),
                () -> assertEquals(90,  at90),
                () -> assertEquals(0,   at360,    "360 should wrap back to 0."),
                () -> assertEquals(90,  at450,    "450 is one full rotation past 90."),
                () -> assertEquals(270, atMinus90,  "-90 is equivalent to 270."),
                () -> assertEquals(180, atMinus180, "-180 is equivalent to 180.")
        );
    }

    @Test
    void constructorRotation_shouldAlsoBeNormalized() {
        PageImage page = new PageImage(UUID.randomUUID(), 1, PageImage.PageType.TIFF, "page-001.png",
                0, 450, "", null);

        assertEquals(90, page.getRotationDegrees(), "Constructor should normalize 450 degrees to 90.");
    }

    @Test
    void cumulativeRotation_shouldWrapAroundZero() {
        PageImage page = new PageImage(1, PageImage.PageType.TIFF, "page-001.png");

        page.setRotationDegrees(90);
        page.setRotationDegrees(page.getRotationDegrees() + 270);

        assertEquals(0, page.getRotationDegrees(), "90 + 270 should wrap back to 0.");
    }

    @Test
    void negativeRotation_shouldProduceEquivalentPositiveAngle() {
        PageImage page = new PageImage(1, PageImage.PageType.TIFF, "page-001.png");

        page.setRotationDegrees(-90);
        int afterMinus90 = page.getRotationDegrees();

        page.setRotationDegrees(-360);
        int afterMinus360 = page.getRotationDegrees();

        assertAll(
                () -> assertEquals(270, afterMinus90),
                () -> assertEquals(0,   afterMinus360)
        );
    }
}
