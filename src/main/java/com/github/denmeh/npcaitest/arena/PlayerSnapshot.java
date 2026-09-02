package com.github.denmeh.npcaitest.arena;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class PlayerSnapshot {

    private final Location location;
    private final GameMode gameMode;
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack[] extra;
    private final ItemStack offHand;
    private final boolean flying;
    private final boolean allowFlight;

    private PlayerSnapshot(Location location, GameMode gameMode, ItemStack[] contents, ItemStack[] armor,
            ItemStack[] extra, ItemStack offHand, boolean flying, boolean allowFlight) {
        this.location = location;
        this.gameMode = gameMode;
        this.contents = contents;
        this.armor = armor;
        this.extra = extra;
        this.offHand = offHand;
        this.flying = flying;
        this.allowFlight = allowFlight;
    }

    public static PlayerSnapshot capture(Player player) {
        PlayerInventory inventory = player.getInventory();
        return new PlayerSnapshot(
                player.getLocation().clone(),
                player.getGameMode(),
                cloneItems(inventory.getContents()),
                cloneItems(inventory.getArmorContents()),
                cloneItems(inventory.getExtraContents()),
                cloneItem(inventory.getItemInOffHand()),
                player.isFlying(),
                player.getAllowFlight());
    }

    public void restore(Player player) {
        player.closeInventory();
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setContents(cloneItems(contents));
        inventory.setArmorContents(cloneItems(armor));
        inventory.setExtraContents(cloneItems(extra));
        inventory.setItemInOffHand(cloneItem(offHand));
        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight && flying);
        player.teleport(location);
        player.updateInventory();
    }

    private static ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] copy = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            copy[i] = cloneItem(items[i]);
        }
        return copy;
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
