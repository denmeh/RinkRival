package com.github.denmeh.npcaitest.arena.ai;

import com.github.denmeh.npcaitest.bt.Leaf;
import com.github.denmeh.npcaitest.bt.Status;

/**
 * Skates to where the puck is going. Succeeds once the puck is in range, which is what advances the
 * attack sequence to the swing.
 */
public final class ChaseToIntercept extends Leaf {

    private final RivalContext ctx;
    private final SkateTo skate = new SkateTo();

    public ChaseToIntercept(RivalContext ctx) {
        super("CHASE");
        this.ctx = ctx;
    }

    @Override
    public Status tick() {
        phase(ctx.chaseLabel());
        ctx.tickChaseMovement();
        if (ctx.inStrikeRange()) {
            return Status.SUCCESS;
        }
        skate.moveTo(ctx, ctx.intercept());
        return Status.RUNNING;
    }

    @Override
    public void abort() {
        skate.reset();
    }
}
