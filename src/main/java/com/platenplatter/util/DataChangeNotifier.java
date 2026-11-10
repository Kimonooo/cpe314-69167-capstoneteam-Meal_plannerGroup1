package com.platenplatter.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The OBSERVER pattern: pages register a listener, and fire() tells all of them that the
 * meal plan changed, so every page refreshes. A broken listener never stops the others.
 */
public class DataChangeNotifier {
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void fire() {
        listeners.forEach(DataChangeNotifier::runSafely);
    }

    public int listenerCount() {
        return listeners.size();
    }

    private static void runSafely(Runnable listener) {
        try {
            listener.run();
        } catch (RuntimeException e) {
            System.err.println("A listener failed: " + e);
        }
    }
}
