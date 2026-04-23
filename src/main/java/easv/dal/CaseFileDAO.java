package easv.dal;

import easv.be.Box;
import easv.be.CaseFile;
import easv.be.Client;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class CaseFileDAO {
    private final Map<String, CaseFile> caseFilesByReference = new LinkedHashMap<>();

    public CaseFile saveOrGetExisting(String caseReference, Client client, Box box) {
        if (caseReference == null || caseReference.isBlank()) {
            throw new IllegalArgumentException("caseReference must not be blank");
        }
        return caseFilesByReference.computeIfAbsent(caseReference, key -> new CaseFile(key, client, box));
    }

    public Optional<CaseFile> findByReference(String caseReference) {
        if (caseReference == null || caseReference.isBlank()) {
            throw new IllegalArgumentException("caseReference must not be blank");
        }
        return Optional.ofNullable(caseFilesByReference.get(caseReference));
    }

    public Collection<CaseFile> findAll() {
        return caseFilesByReference.values();
    }
}
