package com.github.denmeh.npcaitest.arena.ai;

import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;

/**
 * Goalie. When the player is carrying the puck at our end, skating straight at it just gets the rival
 * walked around, so it drops back onto the line between its net and the puck and waits there instead.
 * Succeeds the moment the puck comes within reach, which hands over to the attack branch to clear it.
 */
public final class GuardNet extends BehaviorGoalAdapter {

    private final RivalContext ctx;
    private final SkateTo skate = new SkateTo();

    public GuardNet(RivalContext ctx) {
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
        if (!ctx.playerControlsPuck() || !ctx.defensive()) {
            return BehaviorStatus.FAILURE;
        }
        if (ctx.inStrikeRange()) {
            return BehaviorStatus.SUCCESS;
        }
        ctx.rival().setActiveNode("GUARD_NET");
        ctx.sprint();
        skate.moveTo(ctx, ctx.goaliePoint());
        ctx.facePuck();
        return BehaviorStatus.RUNNING;
    }

    /**
     * Stands down once the puck is within reach: guarding would keep succeeding on the spot and starve the
     * attack branch that is supposed to clear it.
     */
    @Override
    public boolean shouldExecute() {
        return ctx.playable() && ctx.spawned() && ctx.puckAlive() && !ctx.inStrikeRange()
                && ctx.playerControlsPuck() && ctx.defensive();
    }
}
