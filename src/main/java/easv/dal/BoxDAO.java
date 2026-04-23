package easv.dal;

import easv.be.Box;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class BoxDAO {
    private final Map<String, Box> boxesById = new LinkedHashMap<>();

    public Box saveOrGetExisting(String boxId, String description) {
        validateKey(boxId, "boxId");
        validateKey(description, "description");
        return boxesById.computeIfAbsent(boxId, key -> new Box(key, description));
    }

    public Optional<Box> findByBoxId(String boxId) {
        validateKey(boxId, "boxId");
        return Optional.ofNullable(boxesById.get(boxId));
    }

    public Collection<Box> findAll() {
        return boxesById.values();
    }

    private static void validateKey(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
