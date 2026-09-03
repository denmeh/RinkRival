package com.github.denmeh.npcaitest.arena.ai;

import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import org.bukkit.entity.Player;

public final class StrikeTowardGoal extends BehaviorGoalAdapter {

    /**
     * A puck pinned against the boards can never be lined up; shoot anyway rather than circling forever.
     * Pressure from the player short-circuits the same way, so the rival cannot be stripped while fussing
     * over the perfect angle.
     */
    private static final int ORBIT_LIMIT_TICKS = 45;

    private final RivalContext ctx;
    private int orbitTicks;

    public StrikeTowardGoal(RivalContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void reset() {
        orbitTicks = 0;
        ctx.planShot();
    }

    @Override
    public BehaviorStatus run() {
        if (!ctx.playable() || !ctx.spawned() || !ctx.puckAlive() || !ctx.inStrikeRange()) {
            return BehaviorStatus.FAILURE;
        }
        ctx.cancelNavigation();
        if (!ctx.linedUp() && !ctx.pressured() && orbitTicks++ < ORBIT_LIMIT_TICKS) {
            ctx.rival().setActiveNode("SKATE_AROUND");
            ctx.tickOrbit();
            return BehaviorStatus.RUNNING;
        }
        ctx.rival().setActiveNode("STRIKE");
        ctx.faceShot();
        if (!ctx.strikeReady()) {
            return BehaviorStatus.RUNNING;
        }
        if (!(ctx.npc().getEntity() instanceof Player player)) {
            return BehaviorStatus.FAILURE;
        }
        ctx.hitPuck(player);
        return BehaviorStatus.SUCCESS;
    }

    @Override
    public boolean shouldExecute() {
        return ctx.playable() && ctx.spawned() && ctx.puckAlive() && ctx.inStrikeRange();
    }
}
