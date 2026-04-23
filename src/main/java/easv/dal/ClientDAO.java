package easv.dal;

import easv.be.Client;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ClientDAO {
    private final Map<String, Client> clientsByNumber = new LinkedHashMap<>();

    public Client saveOrGetExisting(String clientNumber, String name) {
        validateKey(clientNumber, "clientNumber");
        validateKey(name, "name");
        return clientsByNumber.computeIfAbsent(clientNumber, key -> new Client(key, name));
    }

    public Optional<Client> findByClientNumber(String clientNumber) {
        validateKey(clientNumber, "clientNumber");
        return Optional.ofNullable(clientsByNumber.get(clientNumber));
    }

    public Collection<Client> findAll() {
        return clientsByNumber.values();
    }

    private static void validateKey(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
