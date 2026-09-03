package com.github.denmeh.rinkrival.arena.ai;

import com.github.denmeh.rinkrival.arena.RivalDifficulty;
import com.github.denmeh.rinkrival.bt.Cooldown;
import com.github.denmeh.rinkrival.bt.Guard;
import com.github.denmeh.rinkrival.bt.Node;
import com.github.denmeh.rinkrival.bt.Selector;
import com.github.denmeh.rinkrival.bt.Sequence;
import com.github.denmeh.rinkrival.bt.Timeout;

/**
 * The Rival's whole brain, in one place.
 *
 * <pre>
 * Selector "rival"
 *   Guard "check"   you are on the puck, he is close, puck out of reach
 *     Cooldown → Timeout → BodyCheck
 *   Guard "block"   you are rushing with the puck and he is too far to steal
 *     BlockLane
 *   Guard "defend"  the puck is already a shot at his net
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
        RivalDifficulty difficulty = ctx.difficulty();
        return new Selector("rival",
                new Guard("check",
                        () -> live(ctx) && !ctx.inStrikeRange()
                                && ctx.ownerNearPuck() && ctx.ownerWithin(CHECK_FROM),
                        new Cooldown(difficulty.checkCooldownMs(),
                                new Timeout(CHECK_TIMEOUT_TICKS, new BodyCheck(ctx)))),
                new Guard("block",
                        () -> live(ctx) && !ctx.inStrikeRange() && ctx.attacking() && ctx.farFromPuck(),
                        new BlockLane(ctx)),
                new Guard("defend",
                        () -> live(ctx) && !ctx.inStrikeRange() && ctx.shotOnNet(),
                        new GuardNet(ctx)),
                new Guard("attack",
                        () -> live(ctx),
                        new Sequence("rush", new ChaseToIntercept(ctx), new StrikeTowardGoal(ctx))),
                new Hold(ctx));
    }

    private static boolean live(RivalContext ctx) {
        return ctx.playable() && ctx.spawned() && ctx.puckAlive() && !ctx.stunned();
    }
}
