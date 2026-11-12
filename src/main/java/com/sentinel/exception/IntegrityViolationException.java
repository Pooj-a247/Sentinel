package com.sentinel.exception;
public class IntegrityViolationException extends RuntimeException {
    public IntegrityViolationException(String evidenceId) { super("Integrity verification failed for evidence: "+evidenceId); }
}
