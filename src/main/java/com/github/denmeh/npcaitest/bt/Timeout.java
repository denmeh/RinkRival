package com.github.denmeh.npcaitest.bt;

/**
 * Gives up on a child that stays {@link Status#RUNNING} for too long. Keeps "how long have I been
 * trying" out of the leaf, where it is easy to forget to reset.
 */
public final class Timeout extends Decorator {

    private final int ticks;
    private int elapsed;

    public Timeout(int ticks, Node child) {
        super("", child);
        this.ticks = ticks;
    }

    @Override
    public Status tick() {
        if (++elapsed > ticks) {
            abort();
            return Status.FAILURE;
        }
        Status status = tickChild();
        if (status != Status.RUNNING) {
            elapsed = 0;
        }
        return status;
    }

    @Override
    public void abort() {
        elapsed = 0;
        super.abort();
    }
}
