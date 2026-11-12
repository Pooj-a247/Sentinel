package com.sentinel.model;

import com.sentinel.HashUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Shared evidence model; concrete types supply type-specific details. */
public abstract class Evidence implements Verifiable, Transferable, Loggable {
    private final String evidenceId;
    private final String name;
    private final Path filePath;
    private final String evidenceType;

    protected Evidence(String evidenceId, String name, Path filePath, String evidenceType) {
        this.evidenceId = Objects.requireNonNull(evidenceId);
        this.name = Objects.requireNonNull(name);
        this.filePath = Objects.requireNonNull(filePath);
        this.evidenceType = Objects.requireNonNull(evidenceType);
    }
    @Override public String getEvidenceId() { return evidenceId; }
    public String getName() { return name; }
    public Path getFilePath() { return filePath; }
    public String getEvidenceType() { return evidenceType; }
    @Override public String calculateHash() throws IOException { return HashUtil.sha256(filePath); }
    @Override public boolean verifyIntegrity(String originalHash) throws IOException {
        return calculateHash().equalsIgnoreCase(originalHash);
    }
    @Override public boolean canTransfer() { return true; }
    @Override public String getAuditDescription() { return evidenceType + " evidence '" + name + "' (" + evidenceId + ")"; }
    public abstract String getEvidenceDetails();
}
