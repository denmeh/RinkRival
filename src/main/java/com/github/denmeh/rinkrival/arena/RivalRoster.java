package com.github.denmeh.rinkrival.arena;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Random;

/** Offline-safe rival identity: a hockey name and a dyed jersey, no skin downloads. */
public final class RivalRoster {

    private static final String[] NAMES = {
            "Gretzky", "Orr", "Brodeur", "Lemieux", "Howe",
            "Crosby", "McDavid", "Kane", "Matthews", "Jagr",
            "Fleury", "Price", "Kucherov", "Malkin"
    };

    private static final DyeColor[] JERSEY_COLORS = {
            DyeColor.RED, DyeColor.BLUE, DyeColor.BLACK, DyeColor.WHITE,
            DyeColor.ORANGE, DyeColor.PURPLE, DyeColor.CYAN, DyeColor.GREEN
    };

    private RivalRoster() {
    }

    public record Pick(String name, ItemStack jersey) {
    }

    public static Pick pick(Random random) {
        String name = NAMES[random.nextInt(NAMES.length)];
        DyeColor dye = JERSEY_COLORS[random.nextInt(JERSEY_COLORS.length)];
        ItemStack jersey = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) jersey.getItemMeta();
        if (meta != null) {
            meta.setColor(dye.getColor());
            meta.setDisplayName(ChatColor.RED + name);
            jersey.setItemMeta(meta);
        }
        return new Pick(name, jersey);
    }
}
