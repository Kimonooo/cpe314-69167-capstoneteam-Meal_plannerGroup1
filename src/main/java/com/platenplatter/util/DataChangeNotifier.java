package com.platenplatter.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The OBSERVER pattern: pages register a listener, and fire() tells all of them that the
 * meal plan changed, so every page refreshes. A broken listener never stops the others.
 */
public class DataChangeNotifier {
    private static final Logger LOG = Logger.getLogger("PlateNPlatter");

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
            LOG.log(Level.WARNING, "A page could not refresh", e);   // logged, never shown raw
        }
    }
}
