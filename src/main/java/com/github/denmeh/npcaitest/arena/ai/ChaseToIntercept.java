package com.github.denmeh.npcaitest.arena.ai;

import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;

public final class ChaseToIntercept extends BehaviorGoalAdapter {

    private static final double RETARGET = RivalContext.RETARGET_BLOCKS * RivalContext.RETARGET_BLOCKS;
    /** Inside this range straight-line steering beats repathing, and it tracks a moving puck far better. */
    private static final double STEER_RANGE = 5.0;
    /** A predicted target moves every tick; without this the pathfinder would be rebuilt constantly. */
    private static final int REPATH_COOLDOWN_TICKS = 4;

    private final RivalContext ctx;
    private Location lastTarget;
    private int repathCooldown;

    public ChaseToIntercept(RivalContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void reset() {
        lastTarget = null;
        repathCooldown = 0;
    }

    @Override
    public BehaviorStatus run() {
        if (!ctx.spawned() || !ctx.puckAlive()) {
            return BehaviorStatus.FAILURE;
        }
        ctx.rival().setActiveNode(ctx.chaseLabel());
        ctx.tickChaseMovement();
        if (ctx.inStrikeRange()) {
            return BehaviorStatus.SUCCESS;
        }

        NPC npc = ctx.npc();
        Location target = ctx.intercept();
        if (repathCooldown > 0) {
            repathCooldown--;
        }

        if (ctx.distanceTo(target) <= STEER_RANGE) {
            ctx.cancelNavigation();
            npc.setMoveDestination(target);
            lastTarget = null;
            return BehaviorStatus.RUNNING;
        }

        boolean moved = lastTarget == null
                || lastTarget.getWorld() != target.getWorld()
                || lastTarget.distanceSquared(target) > RETARGET;
        if ((moved && repathCooldown == 0) || !npc.getNavigator().isNavigating()) {
            lastTarget = target.clone();
            repathCooldown = REPATH_COOLDOWN_TICKS;
            npc.getNavigator().setTarget(target);
        }
        return BehaviorStatus.RUNNING;
    }

    @Override
    public boolean shouldExecute() {
        return ctx.spawned() && ctx.puckAlive();
    }
}
