package com.github.denmeh.npcaitest.arena.ai;

import com.github.denmeh.npcaitest.bt.Leaf;
import com.github.denmeh.npcaitest.bt.Status;

/**
 * Last resort, and the reason the tree never fails: stand still. Reached between plays, when the puck is
 * respawning, and during the faceoff countdown.
 */
public final class Hold extends Leaf {

    private final RivalContext ctx;

    public Hold(RivalContext ctx) {
        super("IDLE");
        this.ctx = ctx;
    }

    @Override
    public Status tick() {
        if (ctx.stunned()) {
            phase("CHECKED");
            ctx.cancelNavigation();
            return Status.RUNNING;
        }
        phase(ctx.playable() ? "IDLE" : "FACEOFF");
        ctx.cancelNavigation();
        return Status.RUNNING;
    }
}
