package com.platenplatter.service;

/** A sign-up, log-in or rename problem the user can fix. The message is shown on screen as it is. */
public class AuthException extends Exception {
    private static final long serialVersionUID = 1L;

    public AuthException(String message) {
        super(message);
    }
}
