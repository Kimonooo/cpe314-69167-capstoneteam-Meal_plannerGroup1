package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DataChangeNotifierTest {

    @Test
    void everyListenerIsCalledOnFire() {
        DataChangeNotifier notifier = new DataChangeNotifier();
        AtomicInteger calls = new AtomicInteger();
        notifier.addListener(calls::incrementAndGet);
        notifier.addListener(calls::incrementAndGet);
        notifier.fire();
        assertEquals(2, calls.get());
        assertEquals(2, notifier.listenerCount());
    }

    @Test
    void aBrokenListenerDoesNotStopTheOthers() {
        DataChangeNotifier notifier = new DataChangeNotifier();
        AtomicInteger calls = new AtomicInteger();
        notifier.addListener(() -> {
            throw new IllegalStateException("broken page");
        });
        notifier.addListener(calls::incrementAndGet);
        notifier.fire();
        assertEquals(1, calls.get());
    }

    @Test
    void firingWithNoListenersIsFine() {
        new DataChangeNotifier().fire();
    }
}
