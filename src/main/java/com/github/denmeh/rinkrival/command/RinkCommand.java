package com.github.denmeh.rinkrival.command;

import com.github.denmeh.rinkrival.RinkRival;
import com.github.denmeh.rinkrival.arena.ArenaListener;
import com.github.denmeh.rinkrival.arena.DifficultyMenu;
import com.github.denmeh.rinkrival.arena.RivalDifficulty;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class RinkCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("arena", "leave");

    private final RinkRival plugin;

    public RinkCommand(RinkRival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /rink.");
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

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "arena" -> {
                if (args.length < 2) {
                    DifficultyMenu.open(player);
                    return true;
                }
                RivalDifficulty difficulty = RivalDifficulty.parse(args[1]);
                if (difficulty == null) {
                    player.sendMessage(ChatColor.RED + "Use easy, normal, or hard. Or /rink arena to pick in a menu.");
                    return true;
                }
                ArenaListener.announceCreate(player, plugin.arenas().create(player, difficulty));
            }
            case "leave" -> {
                if (!plugin.arenas().leave(player)) {
                    player.sendMessage(ChatColor.RED + "No rink to leave. /rink arena first.");
                    return true;
                }
                player.sendMessage(ChatColor.GREEN + "Left the rink. Inventory, gamemode and location restored.");
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
        if (args.length == 2 && args[0].equalsIgnoreCase("arena")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Stream.of("easy", "normal", "hard").filter(name -> name.startsWith(prefix)).toList();
        }
        return List.of();
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "/rink arena" + ChatColor.GRAY + " — pick difficulty, then paste a rink");
        player.sendMessage(ChatColor.GOLD + "/rink arena <easy|normal|hard>" + ChatColor.GRAY + " — skip the menu");
        player.sendMessage(ChatColor.GOLD + "/rink leave" + ChatColor.GRAY + " — restore you and the world (or use the barrier)");
    }
}
