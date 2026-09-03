package com.github.denmeh.npcaitest.arena.ai;

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
 * Selector "rival"                          first branch that does not fail, re-picked every tick
 *   Guard "check"   you are on the puck, he is close, and the puck is out of his reach
 *     Cooldown 5.2s
 *       Timeout 40t
 *         BodyCheck                         charge and shove
 *   Guard "defend"  you are on the puck at his end, and it is out of his reach
 *     GuardNet                              sit in front of his own net
 *   Guard "attack"  live play, puck alive
 *     Sequence "rush"
 *       ChaseToIntercept                    skate to the intercept, succeed when in range
 *       StrikeTowardGoal                    line up, then swing
 *   Hold                                    stand still
 * </pre>
 *
 * Every precondition is a {@link Guard}, so it is written once and re-checked every tick. A guard going
 * false aborts whatever it was running and lets the {@link Selector} fall through to the next branch,
 * which is how the Rival switches roles mid-skate.
 */
public final class RivalTree {

    private static final long CHECK_COOLDOWN_MS = 5200L;
    private static final int CHECK_TIMEOUT_TICKS = 40;
    private static final double CHECK_FROM = 5.0;

    private RivalTree() {
    }

    public static Node build(RivalContext ctx) {
        return new Selector("rival",
                new Guard("check",
                        () -> live(ctx) && !ctx.inStrikeRange()
                                && ctx.ownerNearPuck() && ctx.ownerWithin(CHECK_FROM),
                        new Cooldown(CHECK_COOLDOWN_MS,
                                new Timeout(CHECK_TIMEOUT_TICKS, new BodyCheck(ctx)))),
                new Guard("defend",
                        () -> live(ctx) && !ctx.inStrikeRange()
                                && ctx.playerControlsPuck() && ctx.defensive(),
                        new GuardNet(ctx)),
                new Guard("attack",
                        () -> live(ctx),
                        new Sequence("rush", new ChaseToIntercept(ctx), new StrikeTowardGoal(ctx))),
                new Hold(ctx));
    }

    /** Shared precondition: the match is running and there is a puck to play. */
    private static boolean live(RivalContext ctx) {
        return ctx.playable() && ctx.spawned() && ctx.puckAlive();
    }
}
