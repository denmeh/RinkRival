package com.github.denmeh.npcaitest.arena;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Slime;
import org.bukkit.util.Vector;

public final class Puck {

    static final String NAME = "Puck";
    private static final double HEALTH = 40.0;

    private Puck() {
    }

    public static void protect(Slime slime) {
        // ai dont override
        // slime.setAI(false);
        slime.setAware(false);
        slime.setSilent(true);
        slime.setInvulnerable(false);
        slime.setCollidable(true);
        slime.setRemoveWhenFarAway(false);
        slime.setPersistent(true);
        slime.setCanPickupItems(false);
        slime.setFireTicks(0);
        AttributeInstance health = slime.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null && health.getBaseValue() != HEALTH) {
            health.setBaseValue(HEALTH);
        }
        if (slime.getHealth() < HEALTH) {
            slime.setHealth(Math.min(HEALTH, slime.getMaxHealth()));
        }
    }

    public static void style(Slime slime) {
        protect(slime);
        slime.setSize(1);
        slime.setCustomName(ChatColor.AQUA + NAME);
        slime.setCustomNameVisible(true);
        slime.setVelocity(new Vector());
    }
}
