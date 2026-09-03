package com.github.denmeh.npcaitest.arena.ai;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Citizens pathfinding caps NPC speed well below a sprinting player. A small velocity nudge each tick,
 * scaled by difficulty, closes that gap without teleporting.
 */
final class SkateBoost {

    private static final double NUDGE = 0.10;
    /** ~6.2 blocks/s horizontal cap at multiplier 1.0. */
    private static final double MAX_HORIZONTAL = 0.31;

    private SkateBoost() {
    }

    static void toward(RivalContext ctx, Location target) {
        if (!(ctx.npc().getEntity() instanceof Player player) || !player.isOnGround()) {
            return;
        }
        Vector dir = target.toVector().subtract(player.getLocation().toVector());
        dir.setY(0);
        if (dir.lengthSquared() < 0.25) {
            return;
        }
        double cap = MAX_HORIZONTAL * ctx.difficulty().skateBoostMultiplier();
        dir.normalize().multiply(NUDGE * ctx.difficulty().skateBoostMultiplier());

        Vector vel = player.getVelocity();
        double hx = vel.getX() + dir.getX();
        double hz = vel.getZ() + dir.getZ();
        double speed = Math.hypot(hx, hz);
        if (speed > cap) {
            double scale = cap / speed;
            hx *= scale;
            hz *= scale;
        }
        player.setVelocity(new Vector(hx, vel.getY(), hz));
    }
}
