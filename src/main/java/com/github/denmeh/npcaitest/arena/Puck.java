package com.github.denmeh.npcaitest.arena;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Turtle;
import org.bukkit.util.Vector;

public final class Puck {

    static final String NAME = "Puck";
    private static final double HEALTH = 40.0;

    private Puck() {
    }

    public static void protect(Turtle turtle) {
        turtle.setAware(false);
        turtle.setAgeLock(true);
        turtle.setBreed(false);
        turtle.setSilent(true);
        turtle.setInvulnerable(false);
        turtle.setCollidable(true);
        turtle.setRemoveWhenFarAway(false);
        turtle.setPersistent(true);
        turtle.setCanPickupItems(false);
        turtle.setFireTicks(0);
        AttributeInstance health = turtle.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null && health.getBaseValue() != HEALTH) {
            health.setBaseValue(HEALTH);
        }
        if (turtle.getHealth() < HEALTH) {
            turtle.setHealth(Math.min(HEALTH, turtle.getMaxHealth()));
        }
    }

    public static void style(Turtle turtle) {
        protect(turtle);
        turtle.setCustomName(ChatColor.AQUA + NAME);
        turtle.setCustomNameVisible(true);
        turtle.setVelocity(new Vector());
    }
}
