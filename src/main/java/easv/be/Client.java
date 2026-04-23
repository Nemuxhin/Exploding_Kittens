package easv.be;

import java.util.Objects;
import java.util.UUID;

public class Client {
    private final UUID id;
    private final String clientNumber;
    private final String name;

    public Client(String clientNumber, String name) {
        this(UUID.randomUUID(), clientNumber, name);
    }

    public Client(UUID id, String clientNumber, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.clientNumber = requireText(clientNumber, "clientNumber");
        this.name = requireText(name, "name");
    }

    public UUID getId() {
        return id;
    }

    public String getClientNumber() {
        return clientNumber;
    }

    public String getName() {
        return name;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
