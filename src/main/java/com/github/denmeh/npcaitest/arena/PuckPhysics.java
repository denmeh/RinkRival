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

    private static final double MIN_SPEED = 0.04;
    private static final double MAX_SPEED = 1.7;
    private static final double DRAG = 0.96;
    private static final double RESTITUTION = 0.93;
    /** Nudge away from the wall so the puck does not re-stick inside the block hitbox. */
    private static final double SEPARATION = 0.15;
    private static final double HIT_BOOST = 1.5;
    private static final double MIN_HIT_SPEED = 0.6;
    private static final double HARD_BOUNCE = 0.8;

    private PuckPhysics() {
    }

    static void tick(Arena arena, Turtle puck) {
        Vector velocity = puck.getVelocity();
        double vx = velocity.getX();
        double vz = velocity.getZ();
        vx *= DRAG;
        vz *= DRAG;
        double speed = Math.hypot(vx, vz);
        if (speed < MIN_SPEED) {
            if (speed > 0.001) {
                puck.setVelocity(new Vector(0, velocity.getY(), 0));
            }
            return;
        }

        boolean clamped = false;
        if (speed > MAX_SPEED) {
            double scale = MAX_SPEED / speed;
            vx *= scale;
            vz *= scale;
            speed = MAX_SPEED;
            clamped = true;
        }

        Location location = puck.getLocation();
        double x = location.getX();
        double z = location.getZ();
        double nextX = x + vx;
        double nextZ = z + vz;
        boolean bounced = false;
        boolean hard = speed >= HARD_BOUNCE;

        if (!arena.layout().nearGoalMouth(nextX, location.getY(), nextZ)) {
            BoundingBox ice = arena.layout().interiorBox();
            boolean hitXMin = nextX <= ice.getMinX() && vx < 0;
            boolean hitXMax = nextX >= ice.getMaxX() && vx > 0;
            boolean hitZMin = nextZ <= ice.getMinZ() && vz < 0;
            boolean hitZMax = nextZ >= ice.getMaxZ() && vz > 0;

            if (hitXMin || hitXMax) {
                vx = -vx * RESTITUTION;
                bounced = true;
            }
            if (hitZMin || hitZMax) {
                vz = -vz * RESTITUTION;
                bounced = true;
            }
            if (bounced) {
                if (hitXMin) {
                    x = ice.getMinX() + SEPARATION;
                } else if (hitXMax) {
                    x = ice.getMaxX() - SEPARATION;
                }
                if (hitZMin) {
                    z = ice.getMinZ() + SEPARATION;
                } else if (hitZMax) {
                    z = ice.getMaxZ() - SEPARATION;
                }
            }
        }

        if (bounced) {
            float volume = hard ? 1.0f : 0.8f;
            puck.getWorld().playSound(location, Sound.BLOCK_STONE_HIT, volume, hard ? 1.6f : 1.4f);
            puck.getWorld().spawnParticle(Particle.SNOWFLAKE, location, hard ? 10 : 6,
                    0.2, 0.1, 0.2, 0.02);
        }
        if (bounced || clamped || speed < Math.hypot(velocity.getX(), velocity.getZ())) {
            if (bounced && (x != location.getX() || z != location.getZ())) {
                location.setX(x);
                location.setZ(z);
                puck.teleport(location);
            }
            puck.setVelocity(new Vector(vx, velocity.getY(), vz));
        }
    }

    static void hitEffects(Turtle puck) {
        puck.getWorld().playSound(puck.getLocation(), Sound.BLOCK_WOOD_HIT, 1.0f, 0.7f);
        puck.getWorld().spawnParticle(Particle.CRIT, puck.getLocation().add(0, 0.3, 0),
                8, 0.15, 0.1, 0.15, 0.05);
    }

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
