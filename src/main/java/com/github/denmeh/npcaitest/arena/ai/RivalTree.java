package com.github.denmeh.npcaitest.arena.ai;

import com.github.denmeh.npcaitest.arena.RivalDifficulty;
import com.github.denmeh.npcaitest.bt.Cooldown;
import com.github.denmeh.npcaitest.bt.Guard;
import com.github.denmeh.npcaitest.bt.Node;
import com.github.denmeh.npcaitest.bt.Selector;
import com.github.denmeh.npcaitest.bt.Sequence;
import com.github.denmeh.npcaitest.bt.Timeout;

/**
 * The Rival's whole brain, in one place.
 *
 * <pre>
 * Selector "rival"
 *   Guard "check"   you are on the puck, he is close, puck out of reach
 *     Cooldown → Timeout → BodyCheck
 *   Guard "block"   you are attacking with the puck, puck out of reach
 *     BlockLane
 *   Guard "defend"  you are on the puck at his end, puck out of reach
 *     GuardNet
 *   Guard "attack"  live play, puck alive
 *     Sequence "rush" → ChaseToIntercept → StrikeTowardGoal
 *   Hold
 * </pre>
 */
public final class RivalTree {

    private static final int CHECK_TIMEOUT_TICKS = 40;
    private static final double CHECK_FROM = 5.0;

    private RivalTree() {
    }

    public static Node build(RivalContext ctx) {
        return build(ctx, ctx.difficulty());
    }

    public static Node build(RivalContext ctx, RivalDifficulty difficulty) {
        return new Selector("rival",
                new Guard("check",
                        () -> live(ctx) && !ctx.inStrikeRange()
                                && ctx.ownerNearPuck() && ctx.ownerWithin(CHECK_FROM),
                        new Cooldown(difficulty.checkCooldownMs(),
                                new Timeout(CHECK_TIMEOUT_TICKS, new BodyCheck(ctx)))),
                new Guard("block",
                        () -> live(ctx) && !ctx.inStrikeRange() && ctx.attacking(),
                        new BlockLane(ctx)),
                new Guard("defend",
                        () -> live(ctx) && !ctx.inStrikeRange()
                                && ctx.playerControlsPuck() && ctx.defensive(),
                        new GuardNet(ctx)),
                new Guard("attack",
                        () -> live(ctx),
                        new Sequence("rush", new ChaseToIntercept(ctx), new StrikeTowardGoal(ctx))),
                new Hold(ctx));
    }

    private static boolean live(RivalContext ctx) {
        return ctx.playable() && ctx.spawned() && ctx.puckAlive();
    }
}
