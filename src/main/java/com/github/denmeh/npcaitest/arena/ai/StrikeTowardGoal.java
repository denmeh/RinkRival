package com.github.denmeh.npcaitest.arena.ai;

import com.github.denmeh.npcaitest.bt.Leaf;
import com.github.denmeh.npcaitest.bt.Status;
import org.bukkit.entity.Player;

/**
 * Circles until the puck is between the rival and the net, then swings. In his own zone he clears rather
 * than fussing over the perfect angle.
 */
public final class StrikeTowardGoal extends Leaf {

    private static final int ORBIT_LIMIT_ATTACK = 45;
    private static final int ORBIT_LIMIT_DEFEND = 20;

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
        ctx.maybePrepareStick();
        int orbitLimit = ctx.defensive() ? ORBIT_LIMIT_DEFEND : ORBIT_LIMIT_ATTACK;
        boolean skipOrbit = ctx.defensive() && ctx.linedUp();
        if (!skipOrbit && !ctx.linedUp() && !ctx.pressured() && orbitTicks++ < orbitLimit) {
            phase("SKATE_AROUND");
            ctx.tickOrbit();
            return Status.RUNNING;
        }
        phase(ctx.defensive() ? "CLEAR" : "STRIKE");
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

    private Status done(Status status) {
        orbitTicks = 0;
        ctx.planShot();
        return status;
    }
}
