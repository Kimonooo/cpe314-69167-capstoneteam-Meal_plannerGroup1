package com.platenplatter.controller;

/** Something the user typed cannot be used. Its message is a sentence the page shows as it is. */
public class InvalidInputException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
