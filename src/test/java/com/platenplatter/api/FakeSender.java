package com.platenplatter.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * An HttpSender for the tests: it never uses the internet. It answers with a saved TheMealDB
 * file from src/test/resources/mealdb, with an HTTP error code, or with "no internet", and it
 * remembers every address it was asked for.
 */
public final class FakeSender implements HttpSender {
    private final int status;
    private final String body;
    private final boolean offline;
    private final List<URI> asked = new ArrayList<>();

    private FakeSender(int status, String body, boolean offline) {
        this.status = status;
        this.body = body;
        this.offline = offline;
    }

    /** Answers 200 with the saved file, for example "search_arrabiata.json". */
    public static FakeSender file(String name) {
        return new FakeSender(200, read(name), false);
    }

    /** Answers with this status code and an empty body (500, 429 ...). */
    public static FakeSender status(int code) {
        return new FakeSender(code, "", false);
    }

    /** Behaves like a computer with the Wi-Fi off. */
    public static FakeSender offline() {
        return new FakeSender(0, "", true);
    }

    public List<URI> asked() {
        return asked;
    }

    @Override
    public Reply get(URI address) throws IOException {
        asked.add(address);
        if (offline) {
            throw new ConnectException("No route to host (fake)");
        }
        return new Reply(status, body);
    }

    /** The text of a saved answer in src/test/resources/mealdb. */
    public static String read(String name) {
        try (InputStream in = FakeSender.class.getResourceAsStream("/mealdb/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("No saved answer called " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
