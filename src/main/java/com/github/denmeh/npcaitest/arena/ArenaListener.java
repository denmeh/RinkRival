package com.github.denmeh.npcaitest.arena;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ArenaListener implements Listener {

    private final ArenaService arenas;

    public ArenaListener(ArenaService arenas) {
        this.arenas = arenas;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLeaveClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!arenas.isPlaying(player) || !arenas.kit().isLeaveItem(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        leave(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeaveDrop(PlayerDropItemEvent event) {
        if (!arenas.isPlaying(event.getPlayer())) {
            return;
        }
        if (!arenas.kit().isLeaveItem(event.getItemDrop().getItemStack())) {
            return;
        }
        event.setCancelled(true);
        leave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKitClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !arenas.isPlaying(player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (arenas.kit().isKitItem(current) || arenas.kit().isKitItem(cursor)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!arenas.isPlaying(event.getPlayer())) {
            return;
        }
        if (arenas.kit().isKitItem(event.getOffHandItem()) || arenas.kit().isKitItem(event.getMainHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        arenas.leave(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!arenas.isPlaying(event.getEntity())) {
            return;
        }
        event.getDrops().clear();
        event.setKeepInventory(true);
        event.setKeepLevel(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Arena arena = arenas.arenaOf(event.getPlayer());
        if (arena != null) {
            event.setRespawnLocation(arena.layout().playerSpawn());
        }
    }

    private void leave(Player player) {
        if (!arenas.leave(player)) {
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Left the rink. Inventory, gamemode and location restored.");
    }
}
