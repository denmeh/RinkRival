package com.github.denmeh.npcaitest.arena.ai;

import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;

public final class ChaseToIntercept extends BehaviorGoalAdapter {

    private final RivalContext ctx;
    private final SkateTo skate = new SkateTo();

    public ChaseToIntercept(RivalContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void reset() {
        skate.reset();
    }

    @Override
    public BehaviorStatus run() {
        if (!ctx.playable() || !ctx.spawned() || !ctx.puckAlive()) {
            return BehaviorStatus.FAILURE;
        }
        ctx.rival().setActiveNode(ctx.chaseLabel());
        ctx.tickChaseMovement();
        if (ctx.inStrikeRange()) {
            return BehaviorStatus.SUCCESS;
        }
        skate.moveTo(ctx, ctx.intercept());
        return BehaviorStatus.RUNNING;
    }

    @Override
    public boolean shouldExecute() {
        return ctx.playable() && ctx.spawned() && ctx.puckAlive();
    }
}
