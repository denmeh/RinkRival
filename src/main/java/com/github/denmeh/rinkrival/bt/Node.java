package com.github.denmeh.rinkrival.bt;

/**
 * One node of a behavior tree.
 *
 * <p>There is no {@code shouldExecute()}. A node that cannot run says so by returning
 * {@link Status#FAILURE} from {@link #tick()}, which means a precondition is checked in exactly one
 * place and is re-checked every tick. Nodes are ticked from the root every tick, so preconditions must
 * be cheap and must not act on the world before they pass.
 *
 * <p><b>Abort discipline:</b> {@link #abort()} may only clear the node's own state. It must never
 * release shared resources such as the navigator, because by the time a node is aborted a sibling has
 * usually already claimed them for this tick.
 */
public interface Node {

    /** Short label for the debug path. Return an empty string to be skipped in the trace. */
    String name();

    Status tick();

    /** Called when this node is interrupted before it finished. Clear per-run state here. */
    default void abort() {
    }

    /** The child ticked on the most recent tick, or null for leaves. Used to trace the active path. */
    default Node activeChild() {
        return null;
    }
}
