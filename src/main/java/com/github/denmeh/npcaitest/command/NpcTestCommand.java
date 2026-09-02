package com.github.denmeh.npcaitest.command;

import com.github.denmeh.npcaitest.NpcAiTest;
import com.github.denmeh.npcaitest.arena.ArenaService;
import com.github.denmeh.npcaitest.npc.TestNpc;
import com.github.denmeh.npcaitest.npc.TestNpcService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class NpcTestCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of(
            "spawn", "come", "tree", "status", "remove", "arena", "unarena");

    private final NpcAiTest plugin;

    public NpcTestCommand(NpcAiTest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /npctest.");
            return true;
        }
        if (!plugin.isCitizensReady()) {
            player.sendMessage(ChatColor.RED + "Citizens is not ready yet.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        TestNpcService npcs = plugin.npcs();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "spawn" -> {
                String name = args.length >= 2 ? String.join(" ", Stream.of(args).skip(1).toList()) : "Trainee";
                TestNpc spawned = npcs.spawn(player, name);
                player.sendMessage(ChatColor.GREEN + "Spawned " + spawned.npc().getName()
                        + ChatColor.GRAY + ". Next: /npctest come  then  /npctest tree");
            }
            case "come" -> {
                if (!npcs.come(player)) {
                    player.sendMessage(ChatColor.RED + "Spawn an NPC first with /npctest spawn");
                    return true;
                }
                player.sendMessage(ChatColor.GREEN + "MOVE_TO your location (Citizens Navigator / MoveToGoal).");
            }
            case "tree" -> {
                if (!npcs.attachFollowTree(player)) {
                    player.sendMessage(ChatColor.RED + "Spawn an NPC first with /npctest spawn");
                    return true;
                }
                player.sendMessage(ChatColor.GREEN + "Tree attached: FOLLOW (prio 2) if a player is within "
                        + (int) TestNpcService.FOLLOW_RANGE + " blocks, else IDLE (prio 1).");
                player.sendMessage(ChatColor.GRAY + "Walk away past " + (int) TestNpcService.FOLLOW_RANGE
                        + " blocks to see it drop back to IDLE.");
            }
            case "status" -> {
                TestNpc testNpc = npcs.ownedBy(player);
                if (testNpc == null) {
                    player.sendMessage(ChatColor.RED + "No test NPC. /npctest spawn");
                    return true;
                }
                player.sendMessage(ChatColor.GOLD + testNpc.npc().getName()
                        + ChatColor.GRAY + " node=" + ChatColor.YELLOW + testNpc.activeNode()
                        + ChatColor.GRAY + " navigating=" + testNpc.npc().getNavigator().isNavigating());
            }
            case "remove" -> {
                if (npcs.ownedBy(player) == null) {
                    player.sendMessage(ChatColor.RED + "No test NPC to remove.");
                    return true;
                }
                npcs.remove(player);
                player.sendMessage(ChatColor.GREEN + "Removed your test NPC.");
            }
            case "arena" -> {
                ArenaService.CreateResult result = plugin.arenas().create(player);
                switch (result) {
                    case ALREADY_EXISTS -> player.sendMessage(ChatColor.RED
                            + "You already have a rink. /npctest unarena first.");
                    case NO_SCHEMATIC -> player.sendMessage(ChatColor.RED
                            + "Could not load plugins/NpcAiTest/arena/rink.txt — check the server log.");
                    case CREATED_OVERLAP -> {
                        player.sendMessage(ChatColor.GREEN + "Rink built. Knock the slime into the "
                                + ChatColor.RED + "red" + ChatColor.GREEN + " goal. First to 3.");
                        player.sendMessage(ChatColor.YELLOW
                                + "Warning: this rink overlaps another player's arena.");
                    }
                    case CREATED -> {
                        player.sendMessage(ChatColor.GREEN + "Rink built. Knock the slime into the "
                                + ChatColor.RED + "red" + ChatColor.GREEN + " goal. First to 3.");
                        player.sendMessage(ChatColor.GRAY + "Rival is idle. /npctest unarena restores the blocks.");
                    }
                }
            }
            case "unarena" -> {
                if (!plugin.arenas().delete(player)) {
                    player.sendMessage(ChatColor.RED + "No rink to remove. /npctest arena first.");
                    return true;
                }
                player.sendMessage(ChatColor.GREEN + "Rink removed and blocks restored.");
            }
            default -> sendUsage(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(name -> name.startsWith(prefix)).toList();
        }
        return List.of();
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "/npctest spawn [name]" + ChatColor.GRAY + " — create a temporary NPC");
        player.sendMessage(ChatColor.GOLD + "/npctest come" + ChatColor.GRAY + " — walk to you once");
        player.sendMessage(ChatColor.GOLD + "/npctest tree" + ChatColor.GRAY + " — FOLLOW vs IDLE behavior tree");
        player.sendMessage(ChatColor.GOLD + "/npctest status" + ChatColor.GRAY + " — print the active node");
        player.sendMessage(ChatColor.GOLD + "/npctest remove" + ChatColor.GRAY + " — despawn it");
        player.sendMessage(ChatColor.GOLD + "/npctest arena" + ChatColor.GRAY + " — paste a small ice rink vs idle Rival");
        player.sendMessage(ChatColor.GOLD + "/npctest unarena" + ChatColor.GRAY + " — restore blocks and despawn the rink");
    }
}
