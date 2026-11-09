package com.platenplatter.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A tiny "something changed" bell. When a meal is added, deleted or imported, the screen that did it
 * rings the bell (fire), and every other tab that registered a listener refreshes itself.
 * Always call fire() on the JavaFX thread, because listeners usually touch the window.
 */
public class DataChangeNotifier {
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void fire() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                System.err.println("A listener failed: " + e);
            }
        }
    }

    public int listenerCount() {
        return listeners.size();
    }
}
