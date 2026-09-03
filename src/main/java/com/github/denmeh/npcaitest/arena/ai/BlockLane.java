package com.github.denmeh.npcaitest.arena.ai;

import com.github.denmeh.npcaitest.bt.Leaf;
import com.github.denmeh.npcaitest.bt.Status;
import org.bukkit.Location;

/**
 * Steps into the shooting lane when the player is carrying the puck on the attack, instead of chasing
 * the puck head-on and getting walked around.
 */
public final class BlockLane extends Leaf {

    private final RivalContext ctx;
    private final SkateTo skate = new SkateTo();

    public BlockLane(RivalContext ctx) {
        super("BLOCK_LANE");
        this.ctx = ctx;
    }

    @Override
    public Status tick() {
        ctx.sprint();
        Location target = ctx.lanePoint();
        skate.moveTo(ctx, target);
        SkateBoost.toward(ctx, target);
        ctx.facePuck();
        return Status.RUNNING;
    }

    @Override
    public void abort() {
        skate.reset();
    }
}
