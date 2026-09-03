package com.github.denmeh.npcaitest.arena;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Turtle;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Makes the turtle behave like a puck instead of a mob. Minecraft entities do not bounce, they stop dead
 * against a wall, so rebounds off the boards are reflected by hand before the collision happens.
 */
final class PuckPhysics {

    /** Below this the puck is basically parked and there is nothing to reflect. */
    private static final double MIN_SPEED = 0.04;
    /** Fast enough to be exciting, slow enough that a rebound cannot skip past the one block deep net. */
    private static final double MAX_SPEED = 1.7;
    /** Boards give back most of the speed; a little loss stops infinite ping-pong. */
    private static final double RESTITUTION = 0.82;
    private static final double HIT_BOOST = 1.5;
    private static final double MIN_HIT_SPEED = 0.6;

    private PuckPhysics() {
    }

    static void tick(Arena arena, Turtle puck) {
        Vector velocity = puck.getVelocity();
        double vx = velocity.getX();
        double vz = velocity.getZ();
        double speed = Math.hypot(vx, vz);
        if (speed < MIN_SPEED) {
            return;
        }

        boolean clamped = false;
        if (speed > MAX_SPEED) {
            double scale = MAX_SPEED / speed;
            vx *= scale;
            vz *= scale;
            clamped = true;
        }

        Location location = puck.getLocation();
        double nextX = location.getX() + vx;
        double nextZ = location.getZ() + vz;
        boolean bounced = false;
        if (!arena.layout().nearGoalMouth(nextX, location.getY(), nextZ)) {
            BoundingBox ice = arena.layout().interiorBox();
            if ((nextX <= ice.getMinX() && vx < 0) || (nextX >= ice.getMaxX() && vx > 0)) {
                vx = -vx * RESTITUTION;
                bounced = true;
            }
            if ((nextZ <= ice.getMinZ() && vz < 0) || (nextZ >= ice.getMaxZ() && vz > 0)) {
                vz = -vz * RESTITUTION;
                bounced = true;
            }
        }

        if (bounced) {
            puck.getWorld().playSound(location, Sound.BLOCK_STONE_HIT, 0.8f, 1.4f);
            puck.getWorld().spawnParticle(Particle.SNOWFLAKE, location, 6, 0.2, 0.1, 0.2, 0.02);
        }
        if (bounced || clamped) {
            puck.setVelocity(new Vector(vx, velocity.getY(), vz));
        }
    }

    /** Stick on puck: played for every hit, whoever swung. */
    static void hitEffects(Turtle puck) {
        puck.getWorld().playSound(puck.getLocation(), Sound.BLOCK_WOOD_HIT, 1.0f, 0.7f);
        puck.getWorld().spawnParticle(Particle.CRIT, puck.getLocation().add(0, 0.3, 0),
                8, 0.15, 0.1, 0.15, 0.05);
    }

    /**
     * Vanilla knockback alone leaves the puck feeling heavy, so a hit is scaled up to a floor speed. Must
     * run a tick after the damage event: knockback is applied once the event handlers have returned, so
     * reading the velocity during the event would still see the puck standing still.
     */
    static void boostAfterHit(Turtle puck) {
        if (!puck.isValid()) {
            return;
        }
        Vector velocity = puck.getVelocity();
        double speed = Math.hypot(velocity.getX(), velocity.getZ());
        if (speed < MIN_SPEED) {
            return;
        }
        double target = Math.min(MAX_SPEED, Math.max(MIN_HIT_SPEED, speed * HIT_BOOST));
        double scale = target / speed;
        puck.setVelocity(new Vector(velocity.getX() * scale, velocity.getY(), velocity.getZ() * scale));
    }
}
