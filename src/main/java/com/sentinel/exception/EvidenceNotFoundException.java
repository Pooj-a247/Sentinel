package com.sentinel.exception;
public class EvidenceNotFoundException extends RuntimeException {
    public EvidenceNotFoundException(String evidenceId) { super("Evidence not found: "+evidenceId); }
}
