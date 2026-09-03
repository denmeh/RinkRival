package com.github.denmeh.rinkrival.bt;

import java.util.List;

/**
 * Runs children in order and remembers how far it got, so a long running child is not restarted every
 * tick. A child returning {@link Status#SUCCESS} advances to the next one immediately, within the same
 * tick; any {@link Status#FAILURE} fails the whole sequence and rewinds it to the first child.
 */
public final class Sequence implements Node {

    private final String name;
    private final List<Node> children;
    private int index;
    private Node running;
    private Node lastPicked;

    public Sequence(String name, Node... children) {
        this.name = name;
        this.children = List.of(children);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Status tick() {
        while (index < children.size()) {
            Node child = children.get(index);
            Status status = child.tick();
            lastPicked = child;
            running = status == Status.RUNNING ? child : null;
            if (status == Status.RUNNING) {
                return Status.RUNNING;
            }
            if (status == Status.FAILURE) {
                index = 0;
                return Status.FAILURE;
            }
            index++;
        }
        index = 0;
        return Status.SUCCESS;
    }

    @Override
    public void abort() {
        index = 0;
        lastPicked = null;
        if (running == null) {
            return;
        }
        Node interrupted = running;
        running = null;
        interrupted.abort();
    }

    @Override
    public Node activeChild() {
        return lastPicked;
    }
}
