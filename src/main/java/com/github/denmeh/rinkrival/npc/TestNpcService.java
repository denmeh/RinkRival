package com.github.denmeh.rinkrival.npc;

import com.github.denmeh.rinkrival.RinkRival;
import com.github.denmeh.rinkrival.ai.FollowPlayerBehavior;
import com.github.denmeh.rinkrival.ai.IdleBehavior;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.ai.goals.MoveToGoal;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.LookClose;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TestNpcService {

    public static final double FOLLOW_RANGE = 12.0;

    private final RinkRival plugin;
    private final Map<UUID, TestNpc> byOwner = new ConcurrentHashMap<>();
    private BukkitTask debugTask;

    public TestNpcService(RinkRival plugin) {
        this.plugin = plugin;
    }

    public TestNpc spawn(Player player, String name) {
        remove(player);
        Location location = player.getLocation();
        NPC npc = registry().createNPC(EntityType.PLAYER, name);
        npc.data().setPersistent(NPC.Metadata.SHOULD_SAVE, false);
        npc.data().setPersistent(NPC.Metadata.REMOVE_FROM_TABLIST, true);
        npc.setProtected(true);
        npc.spawn(location);

        LookClose lookClose = npc.getOrAddTrait(LookClose.class);
        lookClose.lookClose(true);
        lookClose.setRange(16);

        TestNpc testNpc = new TestNpc(player.getUniqueId(), npc);
        testNpc.setActiveNode("SPAWNED");
        byOwner.put(player.getUniqueId(), testNpc);
        startDebugTask();
        return testNpc;
    }

    public boolean come(Player player) {
        TestNpc testNpc = requireOwned(player);
        if (testNpc == null) {
            return false;
        }
        NPC npc = testNpc.npc();
        GoalController controller = npc.getDefaultGoalController();
        controller.clear();
        controller.addBehavior(new MoveToGoal(npc, player.getLocation()), 1);
        testNpc.setActiveNode("MOVE_TO");
        return true;
    }

    public boolean attachFollowTree(Player player) {
        TestNpc testNpc = requireOwned(player);
        if (testNpc == null) {
            return false;
        }
        NPC npc = testNpc.npc();
        GoalController controller = npc.getDefaultGoalController();
        controller.clear();
        controller.addBehavior(new FollowPlayerBehavior(testNpc, FOLLOW_RANGE), 2);
        controller.addBehavior(new IdleBehavior(testNpc), 1);
        testNpc.setActiveNode("TREE");
        return true;
    }

    public TestNpc ownedBy(Player player) {
        TestNpc testNpc = byOwner.get(player.getUniqueId());
        if (testNpc == null || !testNpc.npc().isSpawned()) {
            return testNpc == null ? null : remove(player);
        }
        return testNpc;
    }

    public TestNpc remove(Player player) {
        TestNpc testNpc = byOwner.remove(player.getUniqueId());
        if (testNpc != null) {
            destroy(testNpc.npc());
        }
        if (byOwner.isEmpty()) {
            stopDebugTask();
        }
        return null;
    }

    public void removeAll() {
        byOwner.values().forEach(testNpc -> destroy(testNpc.npc()));
        byOwner.clear();
        stopDebugTask();
    }

    private TestNpc requireOwned(Player player) {
        return ownedBy(player);
    }

    private NPCRegistry registry() {
        return CitizensAPI.getTemporaryNPCRegistry();
    }

    private void destroy(NPC npc) {
        if (npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
        npc.getDefaultGoalController().clear();
        if (npc.isSpawned()) {
            npc.despawn();
        }
        npc.destroy();
    }

    private void startDebugTask() {
        if (debugTask != null) {
            return;
        }
        debugTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::showDebug, 10L, 10L);
    }

    private void stopDebugTask() {
        if (debugTask != null) {
            debugTask.cancel();
            debugTask = null;
        }
    }

    private void showDebug() {
        byOwner.values().forEach(testNpc -> {
            NPC npc = testNpc.npc();
            if (!npc.isSpawned() || npc.getEntity() == null) {
                return;
            }
            String text = ChatColor.GOLD + npc.getName() + ChatColor.GRAY + " · "
                    + ChatColor.YELLOW + testNpc.activeNode()
                    + ChatColor.GRAY + " · nav="
                    + (npc.getNavigator().isNavigating() ? ChatColor.GREEN + "yes" : ChatColor.RED + "no");
            npc.getEntity().getWorld().getPlayers().stream()
                    .filter(player -> player.getLocation().distanceSquared(npc.getEntity().getLocation()) <= 32 * 32)
                    .forEach(player -> player.spigot().sendMessage(
                            ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(text)));
        });
    }
}
