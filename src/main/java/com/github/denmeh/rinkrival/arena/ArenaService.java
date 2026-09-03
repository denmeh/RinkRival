package com.github.denmeh.rinkrival.arena;

import com.github.denmeh.rinkrival.RinkRival;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Turtle;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArenaService {

    private static final long TICK_PERIOD = 1L;
    private static final int CELEBRATION_TICKS = 50;
    private static final int WIN_CELEBRATION_TICKS = 80;
    /** 60 ticks split into three beats: 3 at the start, then 2, then 1, then the drop. */
    private static final int FACEOFF_TICKS = 60;
    private static final int FACEOFF_BEAT = 20;

    private final RinkRival plugin;
    private final ArenaKit kit;
    private final ArenaSchematic schematic;
    private final Map<UUID, Arena> byOwner = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public ArenaService(RinkRival plugin) {
        this.plugin = plugin;
        this.kit = new ArenaKit(plugin);
        this.schematic = ArenaSchematic.load(plugin);
    }

    public ArenaKit kit() {
        return kit;
    }

    public boolean isPlaying(Player player) {
        return byOwner.containsKey(player.getUniqueId());
    }

    public Arena arenaOf(Player player) {
        return byOwner.get(player.getUniqueId());
    }

    /** Pause pathfinding so vanilla stick knockback is not cancelled by the navigator. */
    public void tryPlayerCheck(Player player, NPC npc) {
        Arena arena = arenaOf(player);
        if (arena == null || !arena.playing() || npc == null || arena.npc() == null) {
            return;
        }
        if (!npc.equals(arena.npc()) || !arena.playerCheckReady()) {
            return;
        }
        var ctx = arena.rivalContext();
        if (ctx == null) {
            return;
        }
        ctx.onChecked();
    }

    public Arena arenaOfRival(org.bukkit.entity.Entity entity) {
        if (entity == null) {
            return null;
        }
        for (Arena arena : byOwner.values()) {
            NPC npc = arena.npc();
            if (npc != null && npc.isSpawned() && npc.getEntity() != null
                    && npc.getEntity().getUniqueId().equals(entity.getUniqueId())) {
                return arena;
            }
        }
        return null;
    }

    public boolean isPuck(org.bukkit.entity.Entity entity) {
        return arenaOfPuck(entity) != null;
    }

    public Arena arenaOfPuck(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Turtle turtle)) {
            return null;
        }
        UUID id = turtle.getUniqueId();
        for (Arena arena : byOwner.values()) {
            if (arena.isPuck(id)) {
                return arena;
            }
        }
        return null;
    }

    /**
     * Knockback lands after the damage event returns, so the shot is scaled on the following tick.
     */
    void boostPuckNextTick(Turtle puck) {
        plugin.getServer().getScheduler().runTask(plugin, () -> PuckPhysics.boostAfterHit(puck));
    }

    public CreateResult create(Player player, RivalDifficulty difficulty) {
        if (schematic == null) {
            return CreateResult.NO_SCHEMATIC;
        }
        if (byOwner.containsKey(player.getUniqueId())) {
            return CreateResult.ALREADY_EXISTS;
        }
        PlayerSnapshot snapshot = PlayerSnapshot.capture(player);
        ArenaLayout layout = ArenaLayout.place(schematic, player);
        boolean overlaps = byOwner.values().stream().anyMatch(arena -> arena.layout().overlaps(layout));
        Arena arena = new Arena(player.getUniqueId(), layout, snapshot, difficulty);
        byOwner.put(player.getUniqueId(), arena);
        BukkitTask paste = ArenaBuilder.paste(plugin, layout, arena::setOriginalBlocks,
                original -> completeCreate(player.getUniqueId(), layout.world(), original));
        arena.setWorldTask(paste);
        startTick();
        return overlaps ? CreateResult.CREATED_OVERLAP : CreateResult.CREATED;
    }

    public boolean leave(Player player) {
        Arena arena = byOwner.remove(player.getUniqueId());
        if (arena == null) {
            return false;
        }
        teardown(arena, player, false);
        if (byOwner.isEmpty()) {
            stopTick();
        }
        return true;
    }

    public void leaveAll() {
        byOwner.forEach((ownerId, arena) -> teardown(arena, plugin.getServer().getPlayer(ownerId), true));
        byOwner.clear();
        stopTick();
    }

    private void completeCreate(UUID ownerId, World world, List<ArenaBuilder.SavedBlock> original) {
        Arena arena = byOwner.get(ownerId);
        if (arena == null) {
            ArenaBuilder.restoreLater(plugin, world, original);
            return;
        }
        Player player = plugin.getServer().getPlayer(ownerId);
        if (player == null || !player.isOnline()) {
            byOwner.remove(ownerId);
            ArenaBuilder.restoreLater(plugin, arena.layout().world(), original);
            if (byOwner.isEmpty()) {
                stopTick();
            }
            return;
        }
        arena.setOriginalBlocks(original);
        kit.equip(player);
        player.teleport(arena.layout().playerSpawn());
        arena.finishBuild(original, RivalNpc.spawn(arena, kit), spawnPuck(arena.layout()));
        arena.hud().attach(player);
        arena.hud().score(arena);
        player.sendMessage(ChatColor.GREEN + "Rink ready. " + ChatColor.YELLOW + arena.difficulty().displayName()
                + ChatColor.GREEN + "  ·  KB1, KB2, Leave on the last slot.");
        player.sendMessage(ChatColor.GRAY + "Left-click the puck to shoot, or the rival to body-check. First to 3.");
        startFaceoff(arena, player);
    }

    private void teardown(Arena arena, Player owner, boolean immediate) {
        arena.cancelWorldTask();
        arena.hud().dispose();
        removePuck(arena.puck());
        RivalNpc.destroy(arena.npc());
        if (immediate) {
            ArenaBuilder.restoreNow(arena.layout().world(), arena.originalBlocks());
        } else {
            ArenaBuilder.restoreLater(plugin, arena.layout().world(), arena.originalBlocks());
        }
        if (owner != null && owner.isOnline()) {
            arena.ownerSnapshot().restore(owner);
        }
    }

    private void startTick() {
        if (tickTask != null) {
            return;
        }
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, TICK_PERIOD, TICK_PERIOD);
    }

    private void stopTick() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void tick() {
        byOwner.values().forEach(this::tickArena);
    }

    private void tickArena(Arena arena) {
        if (!arena.ready()) {
            return;
        }
        Turtle puck = ensurePuck(arena);
        if (puck == null) {
            return;
        }
        Player owner = plugin.getServer().getPlayer(arena.ownerId());
        switch (arena.phase()) {
            case PLAYING -> tickPlaying(arena, puck, owner);
            case CELEBRATION -> tickCelebration(arena, owner);
            case FACEOFF -> tickFaceoff(arena, owner);
            default -> {
            }
        }
    }

    private void tickPlaying(Arena arena, Turtle puck, Player owner) {
        if (puck.getBoundingBox().overlaps(arena.layout().enemyGoalBox())) {
            arena.playerScored();
            onGoal(arena, true, owner);
            return;
        }
        if (puck.getBoundingBox().overlaps(arena.layout().playerGoalBox())) {
            arena.enemyScored();
            onGoal(arena, false, owner);
            return;
        }
        PuckPhysics.tick(arena, puck);
        Location location = puck.getLocation();
        if (!arena.layout().rinkBox().contains(location.getX(), location.getY(), location.getZ())) {
            arena.resetPuck();
        }
    }

    /** The puck is left sitting in the net while the sparks fly, then the faceoff resets everything. */
    private void tickCelebration(Arena arena, Player owner) {
        if (arena.phaseTicks() % 6 == 0) {
            arena.hud().celebrate(arena, arena.lastGoalByPlayer());
        }
        if (arena.phaseElapsed()) {
            startFaceoff(arena, owner);
        }
    }

    private void tickFaceoff(Arena arena, Player owner) {
        arena.holdPuck();
        if (arena.phaseElapsed()) {
            arena.hud().faceoffGo(arena, owner);
            arena.enterPhase(Arena.Phase.PLAYING, 0);
            return;
        }
        int left = arena.phaseTicks();
        if (left > 0 && left < FACEOFF_TICKS && left % FACEOFF_BEAT == 0) {
            arena.hud().faceoffCount(owner, left / FACEOFF_BEAT);
        }
    }

    private void startFaceoff(Arena arena, Player owner) {
        arena.resetPuck();
        RivalNpc.toFaceoff(arena);
        if (owner != null && owner.isOnline()) {
            owner.teleport(arena.layout().playerSpawn());
        }
        arena.enterPhase(Arena.Phase.FACEOFF, FACEOFF_TICKS);
        arena.hud().faceoffCount(owner, FACEOFF_TICKS / FACEOFF_BEAT);
    }

    private void onGoal(Arena arena, boolean playerScored, Player owner) {
        arena.hud().score(arena);
        arena.hud().goal(arena, playerScored, owner);
        if (owner != null) {
            String scorer = playerScored
                    ? ChatColor.AQUA + owner.getName()
                    : ChatColor.LIGHT_PURPLE + arena.rivalName();
            owner.sendMessage(ChatColor.GOLD + "Goal! " + scorer + ChatColor.GRAY + "  "
                    + ChatColor.AQUA + arena.playerScore() + ChatColor.GRAY + " - "
                    + ChatColor.LIGHT_PURPLE + arena.enemyScore());
        }
        boolean decided = arena.playerWon() || arena.enemyWon();
        if (decided) {
            arena.hud().win(arena, owner, arena.playerWon());
            arena.resetScores();
            arena.hud().score(arena);
        }
        arena.enterPhase(Arena.Phase.CELEBRATION, decided ? WIN_CELEBRATION_TICKS : CELEBRATION_TICKS);
    }

    private Turtle ensurePuck(Arena arena) {
        Turtle puck = arena.puck();
        if (puck != null && puck.isValid() && !puck.isDead()) {
            Puck.protect(puck);
            return puck;
        }
        removePuck(puck);
        Turtle next = spawnPuck(arena.layout());
        arena.replacePuck(next);
        return next;
    }

    private Turtle spawnPuck(ArenaLayout layout) {
        World world = layout.world();
        return world.spawn(layout.puckSpawn(), Turtle.class, Puck::style);
    }

    private void removePuck(Turtle puck) {
        if (puck != null && puck.isValid()) {
            puck.remove();
        }
    }

    public enum CreateResult {
        CREATED,
        CREATED_OVERLAP,
        ALREADY_EXISTS,
        NO_SCHEMATIC
    }
}
