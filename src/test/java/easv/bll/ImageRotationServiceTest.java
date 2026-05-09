package easv.bll;

import easv.be.PageImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Rotation is tested as business state, not as image rendering.
 * The GUI can later use the stored rotation value to render the preview/export.
 */
class ImageRotationServiceTest {

    private ImageRotationService rotationService;

    @BeforeEach
    void setUp() {
        rotationService = new ImageRotationService();
    }

    @Test
    void normalizeRotation_shouldWrapDegreesIntoZeroToThreeHundredFiftyNineRange() {
        assertAll(
                () -> assertEquals(0, rotationService.normalizeRotation(0)),
                () -> assertEquals(90, rotationService.normalizeRotation(90)),
                () -> assertEquals(0, rotationService.normalizeRotation(360)),
                () -> assertEquals(90, rotationService.normalizeRotation(450)),
                () -> assertEquals(270, rotationService.normalizeRotation(-90)),
                () -> assertEquals(180, rotationService.normalizeRotation(-180))
        );
    }

    @Test
    void rotate_shouldUpdatePageRotationUsingNormalizedDegrees() {
        PageImage page = new PageImage(1, 1, "page-001.png");

        rotationService.rotate(page, 90);
        rotationService.rotate(page, 270);

        assertEquals(0, page.getRotationDegrees(), "90 + 270 should wrap back to 0 degrees.");
    }

    @Test
    void rotate_shouldRejectNonRightAngleRotations() {
        PageImage page = new PageImage(1, 1, "page-001.png");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> rotationService.rotate(page, 45)),
                () -> assertThrows(IllegalArgumentException.class, () -> rotationService.rotate(page, 135))
        );
    }

    @Test
    void applyBoxRotation_shouldApplySameRotationToEveryScannedPage() {
        PageImage firstPage = new PageImage(1, 1, "page-001.png");
        PageImage secondPage = new PageImage(2, 2, "page-002.png");
        PageImage thirdPage = new PageImage(3, 3, "page-003.png");

        rotationService.applyRotationToAll(List.of(firstPage, secondPage, thirdPage), 180);

        assertAll(
                () -> assertEquals(180, firstPage.getRotationDegrees()),
                () -> assertEquals(180, secondPage.getRotationDegrees()),
                () -> assertEquals(180, thirdPage.getRotationDegrees())
        );
    }
}
