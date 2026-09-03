package com.github.denmeh.rinkrival.arena.ai;

import com.github.denmeh.rinkrival.arena.Arena;
import com.github.denmeh.rinkrival.arena.RivalDifficulty;
import com.github.denmeh.rinkrival.npc.TestNpc;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Turtle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Random;

public final class RivalContext {

    public static final double STRIKE_RANGE = 2.6;
    public static final double RETARGET_BLOCKS = 0.6;

    private static final double ATTACK_STANCE = 1.5;
    private static final double DEFEND_LERP = 0.35;
    private static final double PLAYABLE_INSET = 2.4;
    private static final double CORNER_MARGIN = 3.4;
    private static final double BOARD_MARGIN = 2.6;
    private static final double ALIGN_DOT = 0.78;
    private static final double ORBIT_RADIUS = 2.2;
    private static final double ORBIT_STEP = Math.toRadians(45);
    private static final double ORBIT_PROGRESS = 0.36;
    private static final double HEAVY_STICK_DISTANCE = 11.0;
    private static final double STICK_PREP_RANGE = 6.0;
    private static final double HOP_DISTANCE = 4.0;
    private static final int HOP_COOLDOWN_TICKS = 12;

    /** A turtle sliding on ice keeps most of its speed each tick; this is the measured-ish decay. */
    private static final double PUCK_DRAG = 0.96;
    private static final double PUCK_INSET = 1.0;
    private static final double MOVING_PUCK_SPEED = 0.08;
    private static final int PREDICT_MAX_TICKS = 30;
    private static final int PREDICT_STEP_TICKS = 2;
    /** Roughly how far the rival covers per tick at speedModifier 2.05, used to solve the intercept. */
    private static final double RIVAL_TICK_SPEED = 0.35;

    private final RivalDifficulty difficulty;

    /** Turning at roughly 25 degrees per tick reads as a skater pivoting, not a snapping turret. */
    private static final double TURN_DEGREES_PER_MS = 0.5;
    private static final double LOOK_DISTANCE = 6.0;

    /** Close enough to the puck that the player counts as carrying it. */
    private static final double PLAYER_CONTROL_RANGE = 3.6;
    /** How far off the goal line the rival sits when it plays goalie. Close, not a sweeper. */
    private static final double GOALIE_DEPTH = 2.0;
    /** How far up the shooting lane the rival stands when shading a rush. */
    private static final double LANE_DEPTH = 4.0;
    /** Further than this from the puck, shade the lane; closer, go steal it. */
    private static final double STEAL_RANGE = 6.5;
    /** Fast enough, and aimed enough at our net, to count as a shot he should try to save. */
    private static final double SHOT_SPEED = 0.20;
    private static final double SHOT_DOT = 0.5;
    private static final double SHOVE_POWER = 0.62;
    private static final double SHOVE_LIFT = 0.34;
    /** With the player breathing down its neck the rival shoots rather than keep circling. */
    private static final double PRESSURE_RANGE = 3.2;

    private final Arena arena;
    private final TestNpc rival;
    private final ItemStack lightStick;
    private final ItemStack heavyStick;
    private final Random random = new Random();

    private long nextStrikeAt;
    private int hopCooldown;
    private Location lookFocus;
    private Float lookYaw;
    private long lastLookMs;

    private double aimLateral;
    private double shotSpeed = 0.9;
    private double laneCheat;
    private boolean heavyShot;
    private boolean orbitTheLongWay;

    public RivalContext(Arena arena, TestNpc rival, ItemStack lightStick, ItemStack heavyStick) {
        this(arena, rival, lightStick, heavyStick, RivalDifficulty.EASY);
    }

    public RivalContext(Arena arena, TestNpc rival, ItemStack lightStick, ItemStack heavyStick,
            RivalDifficulty difficulty) {
        this.arena = arena;
        this.rival = rival;
        this.lightStick = lightStick;
        this.heavyStick = heavyStick;
        this.difficulty = difficulty;
        planShot();
    }

    public boolean stunned() {
        return arena.stunned();
    }

    /** Navigator off so vanilla knockback from the stick is not eaten by pathfinding. */
    public void onChecked() {
        if (!spawned()) {
            return;
        }
        cancelNavigation();
        arena.takeCheck(450L, 350L);
    }

    public RivalDifficulty difficulty() {
        return difficulty;
    }

    public Arena arena() {
        return arena;
    }

    public TestNpc rival() {
        return rival;
    }

    public NPC npc() {
        return rival.npc();
    }

    public boolean spawned() {
        return npc().isSpawned() && npc().getEntity() != null;
    }

    public Turtle puck() {
        return arena.puck();
    }

    public boolean puckAlive() {
        Turtle puck = puck();
        return puck != null && puck.isValid() && !puck.isDead();
    }

    /** False during a celebration or faceoff countdown, which is how the whole tree gets frozen. */
    public boolean playable() {
        return arena.playing();
    }

    public Player owner() {
        Player owner = Bukkit.getPlayer(arena.ownerId());
        if (owner == null || !owner.isOnline() || !owner.getWorld().equals(arena.layout().world())) {
            return null;
        }
        return owner;
    }

    /** The player is on the puck and nearer to it than the rival is, so charging it head-on would lose. */
    public boolean playerControlsPuck() {
        Player owner = owner();
        if (owner == null || !spawned() || !puckAlive()) {
            return false;
        }
        Location puckLoc = puck().getLocation();
        double playerDistSq = horizontalDistanceSquared(owner.getLocation(), puckLoc);
        return playerDistSq <= PLAYER_CONTROL_RANGE * PLAYER_CONTROL_RANGE
                && playerDistSq < horizontalDistanceSquared(npc().getEntity().getLocation(), puckLoc);
    }

    public boolean ownerWithin(double blocks) {
        Player owner = owner();
        return owner != null && distanceTo(owner.getLocation()) <= blocks;
    }

    /**
     * Looser than {@link #playerControlsPuck()}: still true once the rival has closed in and become the
     * nearest body to the puck, so a charge is not abandoned a step before contact.
     */
    public boolean ownerNearPuck() {
        Player owner = owner();
        if (owner == null || !puckAlive()) {
            return false;
        }
        double reach = PLAYER_CONTROL_RANGE + 1.0;
        return horizontalDistanceSquared(owner.getLocation(), puck().getLocation()) <= reach * reach;
    }

    public boolean pressured() {
        return ownerWithin(PRESSURE_RANGE);
    }

    /** Player is carrying the puck in their own half, skating up ice. */
    public boolean attacking() {
        return playerControlsPuck() && !defensive();
    }

    /** Too far to poke-check, so shade the rush instead of chasing it down. */
    public boolean farFromPuck() {
        return puckAlive() && distanceTo(puck().getLocation()) > STEAL_RANGE;
    }

    /**
     * The puck is already a shot at our net, not a carry. Sitting in the crease only then — camping
     * the shot line whenever the player has the puck made the net impossible to beat.
     */
    public boolean shotOnNet() {
        if (!puckAlive()) {
            return false;
        }
        Vector velocity = puck().getVelocity();
        double speed = Math.hypot(velocity.getX(), velocity.getZ());
        if (speed < SHOT_SPEED) {
            return false;
        }
        Location puckLoc = puck().getLocation();
        Vector toNet = ownGoalCenter(puckLoc.getY()).toVector().subtract(puckLoc.toVector());
        toNet.setY(0);
        if (toNet.lengthSquared() < 1.0e-4) {
            return true;
        }
        Vector dir = new Vector(velocity.getX(), 0, velocity.getZ()).normalize();
        return dir.dot(toNet.normalize()) >= SHOT_DOT;
    }

    /** Spot on the line puck → our net, a few blocks out and cheated to one side so the far post is open. */
    public Location lanePoint() {
        Location puckLoc = puck().getLocation();
        Location goal = ownGoalCenter(puckLoc.getY());
        Vector toGoal = goal.toVector().subtract(puckLoc.toVector());
        toGoal.setY(0);
        if (toGoal.lengthSquared() < 1.0e-4) {
            return clampToPlayable(puckLoc);
        }
        Vector along = toGoal.normalize();
        Location block = puckLoc.clone().add(along.clone().multiply(LANE_DEPTH));
        block.setY(puckLoc.getY());
        return clampToPlayable(offsetLateral(block, along));
    }

    /**
     * Goalie post: off our goal line toward the puck <em>as it is now</em>, cheated off the shot line.
     * Using the current puck instead of the intercept is the reaction delay.
     */
    public Location goaliePoint() {
        Location spot = puck().getLocation();
        Location net = ownGoalCenter(spot.getY());
        Vector out = spot.toVector().subtract(net.toVector());
        out.setY(0);
        if (out.lengthSquared() < 1.0e-4) {
            return clampToPlayable(net);
        }
        Vector along = out.normalize();
        Location post = net.clone().add(along.clone().multiply(GOALIE_DEPTH));
        post.setY(spot.getY());
        return clampToPlayable(offsetLateral(post, along));
    }

    /** A shove, not an attack: knocks the player off the puck without touching their health. */
    public void shove(Player target) {
        if (!spawned()) {
            return;
        }
        Vector push = target.getLocation().toVector()
                .subtract(npc().getEntity().getLocation().toVector());
        push.setY(0);
        if (push.lengthSquared() < 1.0e-4) {
            push = new Vector(0, 0, 1);
        }
        push.normalize().multiply(SHOVE_POWER).setY(SHOVE_LIFT);
        target.setVelocity(target.getVelocity().add(push));
        if (npc().getEntity() instanceof Player rivalPlayer) {
            rivalPlayer.swingMainHand();
        }
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.9f);
        target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 12, 0.3, 0.2, 0.3, 0.02);
    }

    public boolean inStrikeRange() {
        if (!spawned() || !puckAlive()) {
            return false;
        }
        return horizontalDistanceSquared(npc().getEntity().getLocation(), puck().getLocation())
                <= STRIKE_RANGE * STRIKE_RANGE;
    }

    public boolean linedUp() {
        return inStrikeRange() && alignment() >= ALIGN_DOT;
    }

    /** Judged on the predicted puck spot, so the rival commits to defence before the puck arrives. */
    public boolean defensive() {
        if (!puckAlive()) {
            return false;
        }
        Location spot = interceptPuckLocation();
        double y = spot.getY();
        return horizontalDistanceSquared(spot, ownGoalCenter(y))
                < horizontalDistanceSquared(spot, opponentGoalCenter(y));
    }

    public Location intercept() {
        Location spot = interceptPuckLocation();
        if (defensive()) {
            double y = spot.getY();
            Vector mixed = spot.toVector().multiply(1.0 - DEFEND_LERP)
                    .add(ownGoalCenter(y).toVector().multiply(DEFEND_LERP));
            return clampToPlayable(new Location(spot.getWorld(), mixed.getX(), y, mixed.getZ()));
        }
        return stancePoint();
    }

    public boolean puckMoving() {
        if (!puckAlive()) {
            return false;
        }
        Vector velocity = puck().getVelocity();
        return Math.hypot(velocity.getX(), velocity.getZ()) >= MOVING_PUCK_SPEED;
    }

    public String chaseLabel() {
        if (defensive()) {
            return puckMoving() ? "DEFEND_LEAD" : "CHASE_DEFEND";
        }
        return puckMoving() ? "CHASE_LEAD" : "CHASE_ATTACK";
    }

    public double distanceTo(Location location) {
        if (!spawned()) {
            return Double.MAX_VALUE;
        }
        return Math.sqrt(horizontalDistanceSquared(npc().getEntity().getLocation(), location));
    }

    /**
     * Earliest point along the puck's slide that the rival can actually reach: step forward through the
     * predicted path and take the first spot where travel time is no longer than the puck's flight time.
     */
    public Location interceptPuckLocation() {
        Location current = puck().getLocation();
        if (!spawned() || !puckMoving()) {
            return current;
        }
        Location best = current;
        double tickSpeed = RIVAL_TICK_SPEED * Math.max(0.35, difficulty.skateBoostMultiplier());
        for (int ticks = 0; ticks <= PREDICT_MAX_TICKS; ticks += PREDICT_STEP_TICKS) {
            best = puckAfter(ticks);
            if (distanceTo(best) / tickSpeed <= ticks) {
                break;
            }
        }
        return best;
    }

    private Location puckAfter(int ticks) {
        Location loc = puck().getLocation();
        Vector velocity = puck().getVelocity().clone();
        double x = loc.getX();
        double z = loc.getZ();
        for (int i = 0; i < ticks; i++) {
            x += velocity.getX();
            z += velocity.getZ();
            velocity.multiply(PUCK_DRAG);
        }
        BoundingBox rink = arena.layout().rinkBox();
        return new Location(loc.getWorld(),
                clamp(x, rink.getMinX() + PUCK_INSET, rink.getMaxX() - PUCK_INSET),
                loc.getY(),
                clamp(z, rink.getMinZ() + PUCK_INSET, rink.getMaxZ() - PUCK_INSET));
    }

    /** Rolls the next shot: which part of the net, how hard, which stick, which way to skate around. */
    public void planShot() {
        if (random.nextDouble() < difficulty.missChance()) {
            aimLateral = (random.nextBoolean() ? 1.0 : -1.0) * (0.85 + random.nextDouble() * 0.4);
        } else {
            aimLateral = (random.nextDouble() - 0.5) * difficulty.aimVariance();
        }
        shotSpeed = 0.7 + random.nextDouble() * 0.45;
        boolean far = puckAlive() && horizontalDistanceSquared(puck().getLocation(),
                opponentGoalCenter(puck().getLocation().getY())) > HEAVY_STICK_DISTANCE * HEAVY_STICK_DISTANCE;
        heavyShot = random.nextDouble() < difficulty.wrongStickChance() ? !far : far;
        orbitTheLongWay = random.nextDouble() < 0.15;
        rollLaneCheat();
    }

    /** Picks a far-post cheat so the next save / lane-shade does not sit on the shot line. */
    public void rollLaneCheat() {
        double gap = difficulty.guardGap();
        if (gap <= 0.05) {
            laneCheat = 0;
            return;
        }
        double min = gap * 0.35;
        laneCheat = min + random.nextDouble() * (gap - min);
        if (random.nextBoolean()) {
            laneCheat = -laneCheat;
        }
    }

    /** Swaps to the planned stick once close enough that the player can see it in his hand. */
    public void maybePrepareStick() {
        if (puckAlive() && distanceTo(puck().getLocation()) <= STICK_PREP_RANGE) {
            ensureStick();
        }
    }

    /** Where the puck should be sent: a spot inside the player net, or center ice if it is buried in a corner. */
    public Location aimPoint() {
        return aimPointFor(puck().getLocation());
    }

    private Location aimPointFor(Location puckSpot) {
        if (nearCorner(puckSpot)) {
            if (defensive()) {
                return boardClearPoint(puckSpot);
            }
            Vector center = arena.layout().rinkBox().getCenter();
            return new Location(arena.layout().world(), center.getX(), puckSpot.getY(), center.getZ());
        }
        BoundingBox goal = arena.layout().playerGoalBox();
        boolean alongX = goal.getWidthX() >= goal.getWidthZ();
        double half = (alongX ? goal.getWidthX() : goal.getWidthZ()) / 2.0;
        double offset = aimLateral * half;
        Vector center = goal.getCenter();
        return new Location(arena.layout().world(),
                center.getX() + (alongX ? offset : 0),
                puckSpot.getY(),
                center.getZ() + (alongX ? 0 : offset));
    }

    /** The spot behind the predicted puck position that lines the rival up to shoot forward. */
    public Location stancePoint() {
        Location spot = interceptPuckLocation();
        Location dest = spot.clone().subtract(shotDirectionFrom(spot).multiply(ATTACK_STANCE));
        dest.setY(spot.getY());
        return clampToPlayable(dest);
    }

    /** Next step of the skate-around, one arc segment at a time so the rival never walks through the puck. */
    public Location orbitPoint() {
        Location puckLoc = puck().getLocation();
        Location npcLoc = npc().getEntity().getLocation();
        Vector fromPuck = npcLoc.toVector().subtract(puckLoc.toVector());
        fromPuck.setY(0);
        if (fromPuck.lengthSquared() < 1.0e-4) {
            fromPuck = shotDirection().multiply(-1);
        }
        fromPuck.normalize();

        Vector behind = shotDirection().multiply(-1);
        if (Math.acos(clamp(fromPuck.dot(behind), -1.0, 1.0)) <= ORBIT_STEP) {
            return stancePoint();
        }

        Vector left = rotateXZ(fromPuck, ORBIT_STEP);
        Vector right = rotateXZ(fromPuck, -ORBIT_STEP);
        boolean leftIsShorter = left.dot(behind) >= right.dot(behind);
        boolean takeLeft = orbitTheLongWay ? !leftIsShorter : leftIsShorter;
        Location preferred = orbitDestination(puckLoc, takeLeft ? left : right);
        if (horizontalDistanceSquared(npcLoc, preferred) > ORBIT_PROGRESS) {
            return preferred;
        }
        Location other = orbitDestination(puckLoc, takeLeft ? right : left);
        return horizontalDistanceSquared(npcLoc, other) > ORBIT_PROGRESS ? other : preferred;
    }

    public Location puckLookLocation() {
        Location location = puck().getLocation();
        location.add(0, 0.25, 0);
        return location;
    }

    public Location shotAimLocation() {
        Location puckLoc = puck().getLocation();
        Location aim = puckLoc.clone().add(shotDirection().multiply(3.0));
        aim.setY(puckLoc.getY() + 0.25);
        return aim;
    }

    public Location opponentGoalCenter(double y) {
        return goalCenter(arena.layout().playerGoalBox(), y);
    }

    public Location ownGoalCenter(double y) {
        return goalCenter(arena.layout().enemyGoalBox(), y);
    }

    public boolean strikeReady() {
        return System.currentTimeMillis() >= nextStrikeAt;
    }

    public void markStruck() {
        nextStrikeAt = System.currentTimeMillis() + 380L + random.nextInt(420);
    }

    public void facePuck() {
        if (spawned() && puckAlive()) {
            lookFocus = puckLookLocation();
            npc().faceLocation(lookTarget());
        }
    }

    public void faceShot() {
        if (spawned() && puckAlive()) {
            lookFocus = shotAimLocation();
            npc().faceLocation(lookTarget());
        }
    }

    /** Only used while closing for a hit: staring at the player is right here and wrong everywhere else. */
    public void faceOwner() {
        Player owner = owner();
        if (!spawned() || owner == null) {
            return;
        }
        Location focus = owner.getLocation().clone();
        focus.setY(npc().getEntity().getLocation().getY());
        lookFocus = focus;
        npc().faceLocation(lookTarget());
    }

    /**
     * Look point for both our own facing calls and the navigator's lookAtFunction, so the two never fight.
     * Kept level with the feet: pitch stays flat instead of craning at the sky or the floor.
     */
    public Location lookTarget() {
        Location self = spawned() ? npc().getEntity().getLocation() : npc().getStoredLocation();
        Location focus = lookFocus != null ? lookFocus : (puckAlive() ? puckLookLocation() : self);
        long now = System.currentTimeMillis();
        long elapsed = lastLookMs == 0 ? 50L : Math.min(200L, now - lastLookMs);
        lastLookMs = now;

        double dx = focus.getX() - self.getX();
        double dz = focus.getZ() - self.getZ();
        if (dx * dx + dz * dz > 1.0e-6) {
            float desired = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float current = lookYaw == null ? self.getYaw() : lookYaw;
            float step = (float) (TURN_DEGREES_PER_MS * elapsed);
            float delta = wrapDegrees(desired - current);
            lookYaw = current + Math.max(-step, Math.min(step, delta));
        } else if (lookYaw == null) {
            lookYaw = self.getYaw();
        }

        double radians = Math.toRadians(lookYaw);
        Location target = self.clone().add(-Math.sin(radians) * LOOK_DISTANCE, 0, Math.cos(radians) * LOOK_DISTANCE);
        target.setY(self.getY());
        return target;
    }

    public void sprint() {
        if (spawned() && npc().getEntity() instanceof Player player) {
            player.setSprinting(true);
        }
    }

    public void tickOrbit() {
        if (!spawned() || !puckAlive()) {
            return;
        }
        npc().setMoveDestination(orbitPoint());
        if (npc().getEntity() instanceof Player player) {
            player.setSprinting(true);
        }
        facePuck();
    }

    public void tickChaseMovement() {
        if (!(npc().getEntity() instanceof Player player) || !puckAlive()) {
            return;
        }
        player.setSprinting(true);
        facePuck();
        if (hopCooldown > 0) {
            hopCooldown--;
            return;
        }
        double distSq = horizontalDistanceSquared(player.getLocation(), puck().getLocation());
        if (!player.isOnGround() || nearBoards(player.getLocation())
                || distSq <= HOP_DISTANCE * HOP_DISTANCE) {
            return;
        }
        Vector hop = puck().getLocation().toVector().subtract(player.getLocation().toVector());
        hop.setY(0);
        if (hop.lengthSquared() > 1.0e-4) {
            hop.normalize().multiply(0.22);
        }
        hop.setY(0.42);
        player.setVelocity(hop);
        hopCooldown = HOP_COOLDOWN_TICKS;
    }

    public void ensureStick() {
        if (npc().getEntity() instanceof Player player) {
            player.getInventory().setItemInMainHand((heavyShot ? heavyStick : lightStick).clone());
        }
    }

    public void hitPuck(Player player) {
        Turtle puck = puck();
        Vector shot = shotDirection();
        double power = (heavyShot ? shotSpeed * 1.35 : shotSpeed) * difficulty.shotPower();
        ensureStick();
        faceShot();
        puck.setNoDamageTicks(0);
        player.swingMainHand();
        player.attack(puck);
        puck.setVelocity(shot.multiply(power).setY(0.06));
        markStruck();
    }

    public void cancelNavigation() {
        NPC npc = npc();
        if (npc.isSpawned() && npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
    }

    private double alignment() {
        if (!spawned() || !puckAlive()) {
            return -1.0;
        }
        Vector toPuck = puck().getLocation().toVector()
                .subtract(npc().getEntity().getLocation().toVector());
        toPuck.setY(0);
        if (toPuck.lengthSquared() < 1.0e-4) {
            return -1.0;
        }
        return toPuck.normalize().dot(shotDirection());
    }

    private Vector shotDirection() {
        return shotDirectionFrom(puck().getLocation());
    }

    private Vector shotDirectionFrom(Location puckSpot) {
        Vector dir = aimPointFor(puckSpot).toVector().subtract(puckSpot.toVector());
        dir.setY(0);
        return dir.lengthSquared() < 1.0e-4 ? new Vector(1, 0, 0) : dir.normalize();
    }

    private Location boardClearPoint(Location puckSpot) {
        BoundingBox rink = arena.layout().rinkBox();
        double cx = rink.getCenter().getX();
        double cz = rink.getCenter().getZ();
        Vector toCenter = new Vector(cx - puckSpot.getX(), 0, cz - puckSpot.getZ());
        if (toCenter.lengthSquared() < 1.0e-4) {
            return puckSpot;
        }
        toCenter.normalize();
        Location aim = puckSpot.clone().add(toCenter.multiply(10.0));
        aim.setY(puckSpot.getY());
        return aim;
    }

    private Location offsetLateral(Location alongLine, Vector along) {
        if (Math.abs(laneCheat) < 1.0e-4) {
            return alongLine;
        }
        Vector perp = new Vector(-along.getZ(), 0, along.getX());
        if (perp.lengthSquared() < 1.0e-4) {
            return alongLine;
        }
        return alongLine.clone().add(perp.normalize().multiply(laneCheat));
    }

    private Location orbitDestination(Location puckLoc, Vector direction) {
        Location dest = puckLoc.clone().add(direction.clone().multiply(ORBIT_RADIUS));
        dest.setY(puckLoc.getY());
        return clampToPlayable(dest);
    }

    private Location goalCenter(BoundingBox box, double y) {
        Vector center = box.getCenter();
        return new Location(arena.layout().world(), center.getX(), y, center.getZ());
    }

    private boolean nearCorner(Location location) {
        BoundingBox rink = arena.layout().rinkBox();
        boolean x = location.getX() < rink.getMinX() + CORNER_MARGIN
                || location.getX() > rink.getMaxX() - CORNER_MARGIN;
        boolean z = location.getZ() < rink.getMinZ() + CORNER_MARGIN
                || location.getZ() > rink.getMaxZ() - CORNER_MARGIN;
        return x && z;
    }

    private boolean nearBoards(Location location) {
        BoundingBox rink = arena.layout().rinkBox();
        return location.getX() < rink.getMinX() + BOARD_MARGIN
                || location.getX() > rink.getMaxX() - BOARD_MARGIN
                || location.getZ() < rink.getMinZ() + BOARD_MARGIN
                || location.getZ() > rink.getMaxZ() - BOARD_MARGIN;
    }

    private Location clampToPlayable(Location location) {
        BoundingBox rink = arena.layout().rinkBox();
        location.setX(clamp(location.getX(), rink.getMinX() + PLAYABLE_INSET, rink.getMaxX() - PLAYABLE_INSET));
        location.setZ(clamp(location.getZ(), rink.getMinZ() + PLAYABLE_INSET, rink.getMaxZ() - PLAYABLE_INSET));
        return location;
    }

    private static Vector rotateXZ(Vector vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector(vector.getX() * cos - vector.getZ() * sin, 0,
                vector.getX() * sin + vector.getZ() * cos);
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360f;
        if (wrapped >= 180f) {
            wrapped -= 360f;
        }
        if (wrapped < -180f) {
            wrapped += 360f;
        }
        return wrapped;
    }

    private static double horizontalDistanceSquared(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }
}
