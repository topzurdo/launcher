package com.topzurdo.launcher.service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Auth service (offline only in this build).
 */
public class AuthService {

    private Consumer<String> statusCallback;

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    public boolean hasExistingAuth() {
        return false;
    }

    public CompletableFuture<AuthResult> authenticateMicrosoft() {
        if (statusCallback != null) statusCallback.accept("Офлайн-режим");
        return CompletableFuture.completedFuture(new AuthResult(false, null, "Офлайн-режим"));
    }

    public static class AuthResult {
        private final boolean success;
        private final String username;
        private final String errorMessage;

        public AuthResult(boolean success, String username, String errorMessage) {
            this.success = success;
            this.username = username;
            this.errorMessage = errorMessage;
        }
        public boolean isSuccess() { return success; }
        public String getUsername() { return username; }
        public String getErrorMessage() { return errorMessage; }
    }
}
