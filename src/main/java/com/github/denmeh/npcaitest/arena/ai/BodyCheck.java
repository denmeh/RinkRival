package com.github.denmeh.npcaitest.arena.ai;

import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import org.bukkit.entity.Player;

/**
 * Charges the player while they are carrying the puck and shoves them off it. On a long cooldown on
 * purpose: it is a punctuation mark, not a way to play. The shove moves the player without damaging them.
 */
public final class BodyCheck extends BehaviorGoalAdapter {

    /** Give up rather than chase a player who keeps skating away. */
    private static final int TIMEOUT_TICKS = 40;
    private static final double CHARGE_FROM = 5.0;
    private static final double CONTACT = 1.9;

    private final RivalContext ctx;
    private final SkateTo skate = new SkateTo();
    private int ticks;

    public BodyCheck(RivalContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void reset() {
        ticks = 0;
        skate.reset();
        ctx.markBodyCheck();
    }

    @Override
    public BehaviorStatus run() {
        Player target = ctx.owner();
        if (!ctx.playable() || !ctx.spawned() || target == null || !ctx.puckAlive()) {
            return BehaviorStatus.FAILURE;
        }
        if (++ticks > TIMEOUT_TICKS || !ctx.ownerNearPuck()) {
            return BehaviorStatus.FAILURE;
        }
        ctx.rival().setActiveNode("BODY_CHECK");
        ctx.sprint();
        if (ctx.distanceTo(target.getLocation()) <= CONTACT) {
            ctx.shove(target);
            return BehaviorStatus.SUCCESS;
        }
        skate.moveTo(ctx, target.getLocation());
        ctx.faceOwner();
        return BehaviorStatus.RUNNING;
    }

    @Override
    public boolean shouldExecute() {
        return ctx.playable() && ctx.spawned() && ctx.puckAlive()
                && ctx.bodyCheckReady() && ctx.playerControlsPuck() && ctx.ownerWithin(CHARGE_FROM);
    }
}
