package com.github.denmeh.npcaitest.arena.ai;

import com.github.denmeh.npcaitest.bt.Leaf;
import com.github.denmeh.npcaitest.bt.Status;
import org.bukkit.entity.Player;

/**
 * Circles until the puck is between the rival and the net, then swings.
 */
public final class StrikeTowardGoal extends Leaf {

    /**
     * A puck pinned against the boards can never be lined up; shoot anyway rather than circling forever.
     * Pressure from the player short-circuits the same way, so the rival cannot be stripped while fussing
     * over the perfect angle.
     */
    private static final int ORBIT_LIMIT_TICKS = 45;

    private final RivalContext ctx;
    private int orbitTicks;

    public StrikeTowardGoal(RivalContext ctx) {
        super("STRIKE");
        this.ctx = ctx;
    }

    @Override
    public Status tick() {
        if (!ctx.inStrikeRange()) {
            return done(Status.FAILURE);
        }
        ctx.cancelNavigation();
        if (!ctx.linedUp() && !ctx.pressured() && orbitTicks++ < ORBIT_LIMIT_TICKS) {
            phase("SKATE_AROUND");
            ctx.tickOrbit();
            return Status.RUNNING;
        }
        phase("STRIKE");
        ctx.faceShot();
        if (!ctx.strikeReady()) {
            return Status.RUNNING;
        }
        if (!(ctx.npc().getEntity() instanceof Player player)) {
            return done(Status.FAILURE);
        }
        ctx.hitPuck(player);
        return done(Status.SUCCESS);
    }

    @Override
    public void abort() {
        done(Status.FAILURE);
    }

    /** Every shot gets a fresh plan: new aim, new power, new stick, new way round the puck. */
    private Status done(Status status) {
        orbitTicks = 0;
        ctx.planShot();
        return status;
    }
}
