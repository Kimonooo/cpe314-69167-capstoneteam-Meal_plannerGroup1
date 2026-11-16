package com.platenplatter.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends one GET request and returns the status code and the body. MealDbClient talks to the
 * internet only through this interface, so a test can hand it a fake sender that answers with a
 * saved JSON file, an error code, or "no internet", without any network.
 */
@FunctionalInterface
public interface HttpSender {

    /** What came back: the HTTP status code (200 = OK) and the text of the answer. */
    record Reply(int status, String body) { }

    Reply get(URI address) throws IOException, InterruptedException;

    /**
     * The real sender: Java's HttpClient. No connection in 5 s, or no answer in 10 s, ends with
     * an exception instead of waiting forever.
     */
    static HttpSender internet() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        return address -> {
            HttpRequest request = HttpRequest.newBuilder(address)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "PlateNPlatter/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body());
        };
    }
}
