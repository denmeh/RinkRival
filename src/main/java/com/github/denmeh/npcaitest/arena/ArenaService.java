package com.github.denmeh.npcaitest.arena;

import com.github.denmeh.npcaitest.NpcAiTest;
import com.github.denmeh.npcaitest.ai.IdleBehavior;
import com.github.denmeh.npcaitest.npc.TestNpc;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArenaService {

    private static final long TICK_PERIOD = 1L;
    private static final String RIVAL_NAME = "Rival";
    private static final String PUCK_NAME = "Puck";

    private final NpcAiTest plugin;
    private final ArenaSchematic schematic;
    private final Map<UUID, Arena> byOwner = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public ArenaService(NpcAiTest plugin) {
        this.plugin = plugin;
        this.schematic = ArenaSchematic.load(plugin);
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
        ArenaLayout layout = ArenaLayout.place(schematic, player);
        boolean overlaps = byOwner.values().stream().anyMatch(arena -> arena.layout().overlaps(layout));
        var original = ArenaBuilder.snapshotAndBuild(layout);
        TestNpc rival = spawnRival(player, layout);
        Slime puck = spawnPuck(layout);
        Arena arena = new Arena(player.getUniqueId(), layout, original, rival, puck);
        byOwner.put(player.getUniqueId(), arena);
        player.teleport(layout.playerSpawn());
        startTick();
        return overlaps ? CreateResult.CREATED_OVERLAP : CreateResult.CREATED;
    }

    public boolean delete(Player player) {
        Arena arena = byOwner.remove(player.getUniqueId());
        if (arena == null) {
            return false;
        }
        teardown(arena);
        if (byOwner.isEmpty()) {
            stopTick();
        }
        return true;
    }

    public void deleteAll() {
        byOwner.values().forEach(this::teardown);
        byOwner.clear();
        stopTick();
    }

    private void teardown(Arena arena) {
        removePuck(arena.puck());
        destroyNpc(arena.npc());
        ArenaBuilder.restore(arena.layout().world(), arena.original());
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
        String scorer = playerScored ? ChatColor.BLUE + owner.getName() : ChatColor.RED + RIVAL_NAME;
        owner.sendMessage(ChatColor.GOLD + "Goal! " + scorer + ChatColor.GRAY + "  "
                + ChatColor.BLUE + arena.playerScore() + ChatColor.GRAY + " - "
                + ChatColor.RED + arena.enemyScore());
        if (arena.playerWon()) {
            owner.sendMessage(ChatColor.GREEN + "You win! First to " + Arena.WIN_SCORE + ". Scores reset — keep playing or /npctest unarena.");
            arena.resetScores();
        } else if (arena.enemyWon()) {
            owner.sendMessage(ChatColor.RED + RIVAL_NAME + " wins! First to " + Arena.WIN_SCORE + ". Scores reset — keep playing or /npctest unarena.");
            arena.resetScores();
        }
    }

    private Slime ensurePuck(Arena arena) {
        Slime puck = arena.puck();
        if (puck != null && puck.isValid() && !puck.isDead()) {
            ArenaService.protectPuck(puck);
            return puck;
        }
        removePuck(puck);
        Slime next = spawnPuck(arena.layout());
        arena.replacePuck(next);
        return next;
    }

    private TestNpc spawnRival(Player owner, ArenaLayout layout) {
        NPC npc = CitizensAPI.getTemporaryNPCRegistry().createNPC(EntityType.PLAYER, RIVAL_NAME);
        npc.data().setPersistent(NPC.Metadata.SHOULD_SAVE, false);
        npc.data().setPersistent(NPC.Metadata.REMOVE_FROM_TABLIST, true);
        npc.setProtected(true);
        npc.spawn(layout.npcSpawn());

        LookClose lookClose = npc.getOrAddTrait(LookClose.class);
        lookClose.lookClose(true);
        lookClose.setRange(24);

        TestNpc rival = new TestNpc(owner.getUniqueId(), npc);
        rival.setActiveNode("IDLE");
        npc.getDefaultGoalController().clear();
        npc.getDefaultGoalController().addBehavior(new IdleBehavior(rival), 1);
        return rival;
    }

    private Slime spawnPuck(ArenaLayout layout) {
        World world = layout.world();
        return world.spawn(layout.puckSpawn(), Slime.class, slime -> {
            slime.setSize(1);
            protectPuck(slime);
            slime.setCustomName(ChatColor.AQUA + PUCK_NAME);
            slime.setCustomNameVisible(true);
            slime.setVelocity(new Vector());
        });
    }

    static void protectPuck(Slime slime) {
        // slime.setAI(false);
        slime.setAware(false);
        slime.setSilent(true);
        slime.setInvulnerable(true);
        slime.setCollidable(false);
        slime.setRemoveWhenFarAway(false);
        slime.setPersistent(true);
        slime.setCanPickupItems(false);
        slime.setFireTicks(0);
        slime.setNoDamageTicks(20);
        slime.setHealth(slime.getMaxHealth());
    }

    private void removePuck(Slime puck) {
        if (puck != null && puck.isValid()) {
            puck.remove();
        }
    }

    private void destroyNpc(NPC npc) {
        if (npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
        npc.getDefaultGoalController().clear();
        if (npc.isSpawned()) {
            npc.despawn();
        }
        npc.destroy();
    }

    public enum CreateResult {
        CREATED,
        CREATED_OVERLAP,
        ALREADY_EXISTS,
        NO_SCHEMATIC
    }
}
