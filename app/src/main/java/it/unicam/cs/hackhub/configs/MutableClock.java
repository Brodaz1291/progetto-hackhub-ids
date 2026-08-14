package it.unicam.cs.hackhub.configs;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A clock stopped on a given instant, which can be moved at runtime.
 *
 * {@link Clock#fixed(Instant, ZoneId)} would not fit: the clocks of java.time are immutable,
 * while the platform needs to simulate the passing of time inside a single execution, so that
 * the whole life cycle of a hackathon (registration, running, evaluation) can be exercised.
 */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private volatile Instant instant;

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    @Override
    public Instant instant() {
        return instant;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (zone.equals(this.zone)) {
            return this;
        }
        return new MutableClock(instant, zone);
    }
}
