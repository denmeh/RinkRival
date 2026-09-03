package com.github.denmeh.rinkrival.arena.ai;

import com.github.denmeh.rinkrival.bt.Leaf;
import com.github.denmeh.rinkrival.bt.Status;
import org.bukkit.Location;

/**
 * Shades the rush from a distance, cheated off the shot line so you can go around him. Once he is
 * close enough to steal, the tree drops this and chases the puck instead of camping the crease.
 */
public final class BlockLane extends Leaf {

    private final RivalContext ctx;
    private final SkateTo skate = new SkateTo();
    private boolean engaged;

    public BlockLane(RivalContext ctx) {
        super("BLOCK_LANE");
        this.ctx = ctx;
    }

    @Override
    public Status tick() {
        engaged = true;
        ctx.sprint();
        Location target = ctx.lanePoint();
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
