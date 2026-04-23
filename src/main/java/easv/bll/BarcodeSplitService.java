package easv.bll;

import easv.be.PageImage;

import java.util.Locale;

public class BarcodeSplitService {
    public PageImage.PageType classify(String sourceReference) {
        if (sourceReference == null || sourceReference.isBlank()) {
            throw new IllegalArgumentException("sourceReference must not be blank");
        }

        String normalized = sourceReference.toLowerCase(Locale.ROOT);
        if (normalized.contains("barcode") || normalized.contains("separator") || normalized.startsWith("bc_")) {
            return PageImage.PageType.BARCODE;
        }

        return PageImage.PageType.TIFF;
    }
}
