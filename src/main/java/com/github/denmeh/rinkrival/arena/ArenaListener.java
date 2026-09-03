package com.github.denmeh.rinkrival.arena;

import net.citizensnpcs.api.event.NPCLeftClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onRivalLeftClick(NPCLeftClickEvent event) {
        arenas.tryPlayerCheck(event.getClicker(), event.getNPC());
    }

    /**
     * Keep the stick's knockback. A token of damage is required for vanilla knockback to apply;
     * health is restored so the rival cannot die. Do not cancel — that eats the hit.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onRivalMelee(EntityDamageByEntityEvent event) {
        Arena arena = arenas.arenaOfRival(event.getEntity());
        if (arena == null) {
            return;
        }
        if (!arena.playing()) {
            event.setCancelled(true);
            event.setDamage(0);
            return;
        }
        event.setCancelled(false);
        event.setDamage(0.01);
        if (event.getDamager() instanceof Player player && player.getUniqueId().equals(arena.ownerId())) {
            arenas.tryPlayerCheck(player, arena.npc());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterRivalMelee(EntityDamageByEntityEvent event) {
        Arena arena = arenas.arenaOfRival(event.getEntity());
        if (arena == null || !(event.getEntity() instanceof org.bukkit.entity.LivingEntity living)) {
            return;
        }
        double max = living.getMaxHealth();
        if (living.getHealth() < max) {
            living.setHealth(max);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onRivalHurt(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }
        if (arenas.arenaOfRival(event.getEntity()) == null) {
            return;
        }
        event.setDamage(0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDifficultyPick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DifficultyMenu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() == null
                || !(event.getClickedInventory().getHolder() instanceof DifficultyMenu)) {
            return;
        }
        RivalDifficulty difficulty = DifficultyMenu.fromSlot(event.getSlot());
        if (difficulty == null) {
            return;
        }
        player.closeInventory();
        announceCreate(player, arenas.create(player, difficulty));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDifficultyDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DifficultyMenu) {
            event.setCancelled(true);
        }
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

    public static void announceCreate(Player player, ArenaService.CreateResult result) {
        switch (result) {
            case ALREADY_EXISTS -> player.sendMessage(ChatColor.RED
                    + "You already have a rink. Use the Leave item or /rink leave.");
            case NO_SCHEMATIC -> player.sendMessage(ChatColor.RED
                    + "Could not load plugins/RinkRival/arena/rink.txt — check the server log.");
            case CREATED_OVERLAP -> {
                player.sendMessage(ChatColor.GRAY + "Building rink in the background (no lag spike)...");
                player.sendMessage(ChatColor.YELLOW
                        + "Warning: this rink overlaps another player's arena.");
            }
            case CREATED -> player.sendMessage(ChatColor.GRAY
                    + "Building rink in the background. You will teleport in when it is ready.");
        }
    }
}
