package com.github.denmeh.npcaitest.arena;

import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

public final class PuckListener implements Listener {

    private static final double KNOCKBACK = 0.75;
    private static final double KNOCKBACK_Y = 0.12;

    private final ArenaService arenas;

    public PuckListener(ArenaService arenas) {
        this.arenas = arenas;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPuckDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Slime slime) || !arenas.isPuck(slime)) {
            return;
        }
        event.setCancelled(true);
        event.setDamage(0);
        ArenaService.protectPuck(slime);
        if (event instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Player player) {
            knock(slime, player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPuckClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Slime slime) || !arenas.isPuck(slime)) {
            return;
        }
        event.setCancelled(true);
        knock(slime, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPuckDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Slime slime) || !arenas.isPuck(slime)) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPuckSplit(SlimeSplitEvent event) {
        if (arenas.isPuck(event.getEntity())) {
            event.setCancelled(true);
            event.setCount(0);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPuckCombust(EntityCombustEvent event) {
        if (arenas.isPuck(event.getEntity())) {
            event.setCancelled(true);
            event.getEntity().setFireTicks(0);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPuckTarget(EntityTargetEvent event) {
        if (arenas.isPuck(event.getEntity()) || arenas.isPuck(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    private static void knock(Slime slime, Player player) {
        Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        if (direction.lengthSquared() < 1.0E-4) {
            return;
        }
        slime.setVelocity(direction.normalize().multiply(KNOCKBACK).setY(KNOCKBACK_Y));
    }
}
