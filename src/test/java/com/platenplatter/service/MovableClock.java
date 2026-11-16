package com.platenplatter.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock the tests can move forward, so a five-minute lock can be tested without waiting. */
class MovableClock extends Clock {
    private Instant now = Instant.parse("2026-11-15T08:00:00Z");

    void forward(Duration time) {
        now = now.plus(time);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
