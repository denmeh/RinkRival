package com.github.denmeh.rinkrival.arena.ai;

import com.github.denmeh.rinkrival.bt.Leaf;
import com.github.denmeh.rinkrival.bt.Status;
import org.bukkit.Location;

/**
 * Goalie, only while a shot is already coming. Stands off the shot line (see {@code guardGap}) so the
 * far post is open; no skate-boost, so he is late if you release quickly.
 */
public final class GuardNet extends Leaf {

    private final RivalContext ctx;
    private final SkateTo skate = new SkateTo();
    private boolean engaged;

    public GuardNet(RivalContext ctx) {
        super("GUARD_NET");
        this.ctx = ctx;
    }

    @Override
    public Status tick() {
        engaged = true;
        ctx.sprint();
        Location target = ctx.goaliePoint();
        skate.moveTo(ctx, target);
        ctx.facePuck();
        return Status.RUNNING;
    }

    @Override
    public void abort() {
        skate.reset();
        if (engaged) {
            engaged = false;
            ctx.rollLaneCheat();
        }
    }
}
