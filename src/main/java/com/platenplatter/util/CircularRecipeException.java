package com.platenplatter.util;

/** Thrown when a recipe contains itself, directly or through other recipes. */
public class CircularRecipeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CircularRecipeException(String message) {
        super(message);
    }
}
