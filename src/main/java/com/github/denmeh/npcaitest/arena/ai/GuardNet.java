package com.github.denmeh.npcaitest.arena.ai;

import com.github.denmeh.npcaitest.bt.Leaf;
import com.github.denmeh.npcaitest.bt.Status;

/**
 * Goalie. Holds the line between his own net and the puck instead of chasing a puck the player is
 * already on, which would just get him walked around.
 *
 * <p>It has no exit condition of its own: it holds the post and stays {@link Status#RUNNING} until its
 * guard drops it, either because the player lost the puck or because the puck came within reach and the
 * attack branch should clear it.
 */
public final class GuardNet extends Leaf {

    private final RivalContext ctx;
    private final SkateTo skate = new SkateTo();

    public GuardNet(RivalContext ctx) {
        super("GUARD_NET");
        this.ctx = ctx;
    }

    @Override
    public Status tick() {
        ctx.sprint();
        skate.moveTo(ctx, ctx.goaliePoint());
        ctx.facePuck();
        return Status.RUNNING;
    }

    @Override
    public void abort() {
        skate.reset();
    }
}
