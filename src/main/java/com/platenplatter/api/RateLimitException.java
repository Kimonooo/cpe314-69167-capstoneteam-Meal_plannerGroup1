package com.platenplatter.api;

import java.io.IOException;

/** TheMealDB answered 429 Too Many Requests: the free key was used too often in a short time. */
public class RateLimitException extends IOException {
    private static final long serialVersionUID = 1L;

    public RateLimitException() {
        super("TheMealDB answered HTTP 429 (too many requests)");
    }
}
