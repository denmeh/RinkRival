package com.github.denmeh.rinkrival.bt;

import java.util.List;

/**
 * Reactive fallback: every tick it tries its children in declared order and runs the first that does not
 * fail. Because it always restarts from the highest priority child, a child that becomes runnable
 * interrupts a lower priority one that was mid-run — which is the behaviour Citizens' own
 * {@code Selector} cannot provide, since that one commits to a child until the child finishes.
 *
 * <p>Children are ticked to find the winner, so a child that cannot run must return
 * {@link Status#FAILURE} without touching the world. Wrapping it in a {@link Guard} is the usual way.
 */
public final class Selector implements Node {

    private final String name;
    private final List<Node> children;
    private Node running;
    private Node lastPicked;

    public Selector(String name, Node... children) {
        this.name = name;
        this.children = List.of(children);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Status tick() {
        for (Node child : children) {
            Status status = child.tick();
            if (status == Status.FAILURE) {
                continue;
            }
            if (running != null && running != child) {
                Node interrupted = running;
                running = null;
                interrupted.abort();
            }
            running = status == Status.RUNNING ? child : null;
            lastPicked = child;
            return status;
        }
        abortRunning();
        lastPicked = null;
        return Status.FAILURE;
    }

    @Override
    public void abort() {
        abortRunning();
        lastPicked = null;
    }

    @Override
    public Node activeChild() {
        return lastPicked;
    }

    private void abortRunning() {
        if (running == null) {
            return;
        }
        Node interrupted = running;
        running = null;
        interrupted.abort();
    }
}
