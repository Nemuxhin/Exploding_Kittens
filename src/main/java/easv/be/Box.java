package easv.be;

import java.util.Objects;
import java.util.UUID;

public class Box {
    private final UUID id;
    private final String boxId;
    private final String description;

    public Box(String boxId, String description) {
        this(UUID.randomUUID(), boxId, description);
    }

    public Box(UUID id, String boxId, String description) {
        this.id = Objects.requireNonNull(id, "id");
        this.boxId = requireText(boxId, "boxId");
        this.description = requireText(description, "description");
    }

    public UUID getId() {
        return id;
    }

    public String getBoxId() {
        return boxId;
    }

    public String getDescription() {
        return description;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
