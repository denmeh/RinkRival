package com.github.denmeh.npcaitest.bt;

/**
 * Base for nodes that actually do something.
 *
 * <p>A leaf cleans up after itself when it returns {@link Status#SUCCESS} or {@link Status#FAILURE};
 * {@link #abort()} covers only the interrupted case. That split is deliberate, and is the one place this
 * tree asks more of you than Citizens did, where {@code reset()} covered both.
 */
public abstract class Leaf implements Node {

    private String name;

    protected Leaf(String name) {
        this.name = name;
    }

    @Override
    public final String name() {
        return name;
    }

    /**
     * Relabels this leaf for the debug path, to report which phase of its work is running. Cheaper than
     * splitting a leaf in two just to see the difference from outside.
     */
    protected final void phase(String phase) {
        this.name = phase;
    }
}
