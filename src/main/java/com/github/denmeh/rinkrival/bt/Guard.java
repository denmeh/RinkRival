package com.github.denmeh.rinkrival.bt;

import java.util.function.BooleanSupplier;

/**
 * Runs its child only while a condition holds, and aborts the child the moment it stops holding. This is
 * where a precondition belongs: written once, in the tree, re-checked every tick. It replaces the
 * duplication you get from a {@code shouldExecute()} that is only consulted at selection time and so has
 * to be repeated inside the running node.
 */
public final class Guard extends Decorator {

    private final BooleanSupplier condition;

    public Guard(String name, BooleanSupplier condition, Node child) {
        super(name, child);
        this.condition = condition;
    }

    @Override
    public Status tick() {
        if (!condition.getAsBoolean()) {
            abort();
            return Status.FAILURE;
        }
        return tickChild();
    }
}
