package com.github.denmeh.rinkrival.bt;

/**
 * Fails without touching the child until the cooldown has elapsed. The clock starts when an attempt
 * ends, whether it succeeded, failed or was interrupted, so a move that misses is not retried instantly.
 * Put this <em>inside</em> the {@link Guard} rather than outside it: outside, a failing precondition
 * would be treated as a spent attempt and start the cooldown for nothing.
 */
public final class Cooldown extends Decorator {

    private final long millis;
    private long readyAt;

    public Cooldown(long millis, Node child) {
        super("", child);
        this.millis = millis;
    }

    @Override
    public Status tick() {
        if (System.currentTimeMillis() < readyAt) {
            skipChild();
            return Status.FAILURE;
        }
        Status status = tickChild();
        if (status != Status.RUNNING) {
            readyAt = System.currentTimeMillis() + millis;
        }
        return status;
    }

    @Override
    public void abort() {
        if (childRunning()) {
            readyAt = System.currentTimeMillis() + millis;
        }
        super.abort();
    }
}
