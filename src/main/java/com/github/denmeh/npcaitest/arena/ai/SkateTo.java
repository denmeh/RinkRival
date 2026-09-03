package com.github.denmeh.npcaitest.arena.ai;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;

/**
 * Shared movement for leaf nodes that need to get somewhere. Close in it steers straight, which tracks a
 * target that moves every tick; further out it hands over to the pathfinder, but only on a cooldown so the
 * path is not rebuilt from scratch each tick. One instance per node, since the last target is node state.
 */
final class SkateTo {

    private static final double RETARGET = RivalContext.RETARGET_BLOCKS * RivalContext.RETARGET_BLOCKS;
    private static final double STEER_RANGE = 5.0;
    private static final int REPATH_COOLDOWN_TICKS = 4;

    private Location lastTarget;
    private int repathCooldown;

    void reset() {
        lastTarget = null;
        repathCooldown = 0;
    }

    void moveTo(RivalContext ctx, Location target) {
        NPC npc = ctx.npc();
        if (repathCooldown > 0) {
            repathCooldown--;
        }
        if (ctx.distanceTo(target) <= STEER_RANGE) {
            ctx.cancelNavigation();
            npc.setMoveDestination(target);
            lastTarget = null;
            return;
        }
        boolean moved = lastTarget == null
                || lastTarget.getWorld() != target.getWorld()
                || lastTarget.distanceSquared(target) > RETARGET;
        if ((moved && repathCooldown == 0) || !npc.getNavigator().isNavigating()) {
            lastTarget = target.clone();
            repathCooldown = REPATH_COOLDOWN_TICKS;
            npc.getNavigator().setTarget(target);
        }
    }
}
