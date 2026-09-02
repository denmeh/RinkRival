package com.github.denmeh.npcaitest.arena;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ArenaKit {

    public static final int LEAVE_SLOT = 8;

    private final NamespacedKey stickKey;
    private final NamespacedKey leaveKey;

    public ArenaKit(JavaPlugin plugin) {
        this.stickKey = new NamespacedKey(plugin, "puck_stick");
        this.leaveKey = new NamespacedKey(plugin, "leave_arena");
    }

    public void equip(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setGameMode(GameMode.ADVENTURE);
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(0, knockbackStick(1));
        inventory.setItem(1, knockbackStick(2));
        inventory.setItem(LEAVE_SLOT, leaveItem());
        player.getInventory().setHeldItemSlot(0);
        player.updateInventory();
    }

    public boolean isLeaveItem(ItemStack item) {
        return hasKey(item, leaveKey);
    }

    public boolean isKitItem(ItemStack item) {
        return hasKey(item, stickKey) || hasKey(item, leaveKey);
    }

    private ItemStack knockbackStick(int level) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Puck Stick " + ChatColor.GRAY + "KB " + level);
            meta.setLore(List.of(ChatColor.DARK_GRAY + "Left-click the puck"));
            meta.addEnchant(Enchantment.KNOCKBACK, level, true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(stickKey, PersistentDataType.INTEGER, level);
            stick.setItemMeta(meta);
        }
        return stick;
    }

    private ItemStack leaveItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Leave Arena");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Restores your inventory,",
                    ChatColor.GRAY + "gamemode and location"));
            meta.getPersistentDataContainer().set(leaveKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static boolean hasKey(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(key, PersistentDataType.INTEGER) || pdc.has(key, PersistentDataType.BYTE);
    }
}
