package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DataChangeNotifierTest {

    @Test
    void everyListenerIsCalledOnFire() {
        DataChangeNotifier n = new DataChangeNotifier();
        AtomicInteger calls = new AtomicInteger();
        n.addListener(calls::incrementAndGet);
        n.addListener(calls::incrementAndGet);
        n.fire();
        assertEquals(2, calls.get());
    }

    @Test
    void aBrokenListenerDoesNotStopTheOthers() {
        DataChangeNotifier n = new DataChangeNotifier();
        AtomicInteger calls = new AtomicInteger();
        n.addListener(() -> { throw new IllegalStateException("oops"); });
        n.addListener(calls::incrementAndGet);
        n.fire();
        assertEquals(1, calls.get());
    }

    @Test
    void firingWithNoListenersIsFine() {
        new DataChangeNotifier().fire();
    }
}
