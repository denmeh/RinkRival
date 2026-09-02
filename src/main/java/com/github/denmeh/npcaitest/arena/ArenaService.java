package com.github.denmeh.npcaitest.arena;

import com.github.denmeh.npcaitest.NpcAiTest;
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
        if (!(entity instanceof Turtle turtle)) {
            return false;
        }
        UUID id = turtle.getUniqueId();
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
        Arena arena = new Arena(player.getUniqueId(), layout, snapshot);
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
        arena.finishBuild(original, RivalNpc.spawn(ownerId, arena.layout()), spawnPuck(arena.layout()));
        player.sendMessage(ChatColor.GREEN + "Rink ready. Hotbar: KB1, KB2, and Leave on the last slot.");
        player.sendMessage(ChatColor.GRAY + "Left-click the puck. First to 3. Leaving restores inventory, gamemode and location.");
    }

    private void teardown(Arena arena, Player owner, boolean immediate) {
        arena.cancelWorldTask();
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
        arena.tickCooldown((int) TICK_PERIOD);
        Turtle puck = ensurePuck(arena);
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
