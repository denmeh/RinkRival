package com.github.denmeh.npcaitest.arena;

import org.bukkit.entity.Player;
import org.bukkit.entity.Turtle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public final class PuckListener implements Listener {

    private final ArenaService arenas;

    public PuckListener(ArenaService arenas) {
        this.arenas = arenas;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPuckDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Turtle turtle) || !arenas.isPuck(turtle)) {
            return;
        }
        if (isPlayerMelee(event)) {
            if (event.getDamage() <= 0) {
                event.setDamage(0.01);
            }
            return;
        }
        event.setCancelled(true);
        event.setDamage(0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Turtle turtle) || !arenas.isPuck(turtle)) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        turtle.setHealth(turtle.getMaxHealth());
        Puck.protect(turtle);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPuckDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Turtle turtle) || !arenas.isPuck(turtle)) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPuckDig(EntityChangeBlockEvent event) {
        if (arenas.isPuck(event.getEntity())) {
            event.setCancelled(true);
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

    private static boolean isPlayerMelee(EntityDamageEvent event) {
        return event instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Player;
    }
}
