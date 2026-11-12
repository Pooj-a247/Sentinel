package com.sentinel.model;
public interface Verifiable {
    String calculateHash() throws java.io.IOException;
    boolean verifyIntegrity(String originalHash) throws java.io.IOException;
}
