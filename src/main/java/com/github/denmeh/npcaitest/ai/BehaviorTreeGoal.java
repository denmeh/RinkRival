package com.github.denmeh.npcaitest.ai;

import com.github.denmeh.npcaitest.bt.Node;
import com.github.denmeh.npcaitest.bt.Trees;
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;

import java.util.function.Consumer;

/**
 * The only bridge between Citizens and our own tree. Registered as a single goal that never finishes, so
 * Citizens keeps ticking it and the priority scheduler is left with nothing to arbitrate: all role
 * selection happens inside the tree, where it can be nested and reactive.
 *
 * <p>Citizens still owns the NPC and the {@link net.citizensnpcs.api.ai.Navigator}. Only the tree layer
 * is ours.
 */
public final class BehaviorTreeGoal extends BehaviorGoalAdapter {

    private final Node root;
    private final Consumer<String> trace;

    public BehaviorTreeGoal(Node root, Consumer<String> trace) {
        this.root = root;
        this.trace = trace;
    }

    @Override
    public boolean shouldExecute() {
        return true;
    }

    @Override
    public BehaviorStatus run() {
        root.tick();
        trace.accept(Trees.activePath(root));
        return BehaviorStatus.RUNNING;
    }

    @Override
    public void reset() {
        root.abort();
    }
}
