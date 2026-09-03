package com.github.denmeh.rinkrival.bt;

/**
 * Base for single child nodes that change when or whether the child runs. Handles the bookkeeping every
 * decorator needs: whether the child is mid-run (so it can be aborted) and whether it ran at all this
 * tick (so the debug path is accurate).
 */
public abstract class Decorator implements Node {

    private final String name;
    protected final Node child;
    private boolean childRunning;
    private boolean childTicked;

    protected Decorator(String name, Node child) {
        this.name = name;
        this.child = child;
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public void abort() {
        childTicked = false;
        if (childRunning) {
            childRunning = false;
            child.abort();
        }
    }

    @Override
    public final Node activeChild() {
        return childTicked ? child : null;
    }

    protected final Status tickChild() {
        Status status = child.tick();
        childRunning = status == Status.RUNNING;
        childTicked = true;
        return status;
    }

    /** Whether the child is part way through a run, and so has state worth aborting. */
    protected final boolean childRunning() {
        return childRunning;
    }

    /** Marks the child as untouched this tick, for decorators that skip it entirely. */
    protected final void skipChild() {
        childTicked = false;
    }
}
