package com.github.denmeh.npcaitest.arena;

import com.github.denmeh.npcaitest.NpcAiTest;
import com.github.denmeh.npcaitest.npc.TestNpc;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArenaService {

    private static final long TICK_PERIOD = 1L;

    private final NpcAiTest plugin;
    private final ArenaKit kit;
    private final ArenaSchematic schematic;
    private final Map<UUID, Arena> byOwner = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public ArenaService(NpcAiTest plugin) {
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

    public boolean isPuck(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Slime slime)) {
            return false;
        }
        UUID id = slime.getUniqueId();
        for (Arena arena : byOwner.values()) {
            if (arena.isPuck(id)) {
                return true;
            }
        }
        return false;
    }

    public CreateResult create(Player player) {
        if (schematic == null) {
            return CreateResult.NO_SCHEMATIC;
        }
        if (byOwner.containsKey(player.getUniqueId())) {
            return CreateResult.ALREADY_EXISTS;
        }
        PlayerSnapshot snapshot = PlayerSnapshot.capture(player);
        ArenaLayout layout = ArenaLayout.place(schematic, player);
        boolean overlaps = byOwner.values().stream().anyMatch(arena -> arena.layout().overlaps(layout));
        var originalBlocks = ArenaBuilder.snapshotAndBuild(layout);
        TestNpc rival = RivalNpc.spawn(player.getUniqueId(), layout);
        Slime puck = spawnPuck(layout);
        Arena arena = new Arena(player.getUniqueId(), layout, originalBlocks, snapshot, rival, puck);
        byOwner.put(player.getUniqueId(), arena);
        kit.equip(player);
        player.teleport(layout.playerSpawn());
        startTick();
        return overlaps ? CreateResult.CREATED_OVERLAP : CreateResult.CREATED;
    }

    public boolean leave(Player player) {
        Arena arena = byOwner.remove(player.getUniqueId());
        if (arena == null) {
            return false;
        }
        teardown(arena, player);
        if (byOwner.isEmpty()) {
            stopTick();
        }
        return true;
    }

    public void leaveAll() {
        byOwner.forEach((ownerId, arena) -> {
            Player owner = plugin.getServer().getPlayer(ownerId);
            teardown(arena, owner);
        });
        byOwner.clear();
        stopTick();
    }

    private void teardown(Arena arena, Player owner) {
        removePuck(arena.puck());
        RivalNpc.destroy(arena.npc());
        ArenaBuilder.restore(arena.layout().world(), arena.originalBlocks());
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
        arena.tickCooldown((int) TICK_PERIOD);
        Slime puck = ensurePuck(arena);
        if (puck == null) {
            return;
        }
        Location location = puck.getLocation();
        if (arena.canScore()) {
            if (puck.getBoundingBox().overlaps(arena.layout().enemyGoalBox())) {
                arena.playerScored();
                onGoal(arena, true);
                return;
            }
            if (puck.getBoundingBox().overlaps(arena.layout().playerGoalBox())) {
                arena.enemyScored();
                onGoal(arena, false);
                return;
            }
        }
        if (!arena.layout().rinkBox().contains(location.getX(), location.getY(), location.getZ())) {
            arena.resetPuck();
        }
    }

    private void onGoal(Arena arena, boolean playerScored) {
        Player owner = plugin.getServer().getPlayer(arena.ownerId());
        arena.resetPuck();
        if (owner == null) {
            if (arena.playerWon() || arena.enemyWon()) {
                arena.resetScores();
            }
            return;
        }
        String scorer = playerScored ? ChatColor.BLUE + owner.getName() : ChatColor.RED + RivalNpc.NAME;
        owner.sendMessage(ChatColor.GOLD + "Goal! " + scorer + ChatColor.GRAY + "  "
                + ChatColor.BLUE + arena.playerScore() + ChatColor.GRAY + " - "
                + ChatColor.RED + arena.enemyScore());
        if (arena.playerWon()) {
            owner.sendMessage(ChatColor.GREEN + "You win! First to " + Arena.WIN_SCORE
                    + ". Scores reset — keep playing or use the Leave item.");
            arena.resetScores();
        } else if (arena.enemyWon()) {
            owner.sendMessage(ChatColor.RED + RivalNpc.NAME + " wins! First to " + Arena.WIN_SCORE
                    + ". Scores reset — keep playing or use the Leave item.");
            arena.resetScores();
        }
    }

    private Slime ensurePuck(Arena arena) {
        Slime puck = arena.puck();
        if (puck != null && puck.isValid() && !puck.isDead()) {
            Puck.protect(puck);
            return puck;
        }
        removePuck(puck);
        Slime next = spawnPuck(arena.layout());
        arena.replacePuck(next);
        return next;
    }

    private Slime spawnPuck(ArenaLayout layout) {
        World world = layout.world();
        return world.spawn(layout.puckSpawn(), Slime.class, Puck::style);
    }

    private void removePuck(Slime puck) {
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
