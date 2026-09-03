package com.github.denmeh.npcaitest.arena.ai;

import com.github.denmeh.npcaitest.bt.Leaf;
import com.github.denmeh.npcaitest.bt.Status;
import org.bukkit.entity.Player;

/**
 * Charges the player and shoves them off the puck. The shove moves them without damaging them.
 */
public final class BodyCheck extends Leaf {

    private static final double CONTACT = 1.9;

    private final RivalContext ctx;
    private final SkateTo skate = new SkateTo();

    public BodyCheck(RivalContext ctx) {
        super("BODY_CHECK");
        this.ctx = ctx;
    }

    @Override
    public Status tick() {
        Player target = ctx.owner();
        if (target == null) {
            return Status.FAILURE;
        }
        ctx.sprint();
        if (ctx.distanceTo(target.getLocation()) <= CONTACT) {
            ctx.shove(target);
            skate.reset();
            return Status.SUCCESS;
        }
        skate.moveTo(ctx, target.getLocation());
        SkateBoost.toward(ctx, target.getLocation());
        ctx.faceOwner();
        return Status.RUNNING;
    }

    @Override
    public void abort() {
        skate.reset();
    }
}
