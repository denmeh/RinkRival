package com.github.denmeh.rinkrival.arena;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** Chest GUI opened by {@code /rink arena} when no difficulty argument is given. */
public final class DifficultyMenu implements InventoryHolder {

    static final String TITLE = ChatColor.DARK_GRAY + "Rival difficulty";

    private final Inventory inventory;

    private DifficultyMenu() {
        this.inventory = Bukkit.createInventory(this, 9, TITLE);
        inventory.setItem(2, icon(Material.LIME_CONCRETE, ChatColor.GREEN + "Easy",
                ChatColor.GRAY + "Slower, leaky goalie, misses more."));
        inventory.setItem(4, icon(Material.YELLOW_CONCRETE, ChatColor.YELLOW + "Normal",
                ChatColor.GRAY + "Keeps up with you. A fair 1v1."));
        inventory.setItem(6, icon(Material.RED_CONCRETE, ChatColor.RED + "Hard",
                ChatColor.GRAY + "Fast, accurate, little room to breathe."));
    }

    public static void open(Player player) {
        player.openInventory(new DifficultyMenu().inventory);
    }

    static RivalDifficulty fromSlot(int slot) {
        return switch (slot) {
            case 2 -> RivalDifficulty.EASY;
            case 4 -> RivalDifficulty.NORMAL;
            case 6 -> RivalDifficulty.HARD;
            default -> null;
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static ItemStack icon(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore, ChatColor.DARK_GRAY + "Click to start"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
