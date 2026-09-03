package com.github.denmeh.npcaitest.arena;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Everything the player hears and sees that is not a block: the score bar, the goal horn, titles and
 * particles. Kept apart from {@link ArenaService} so the game rules stay readable.
 */
public final class ArenaHud {

    private static final int GOAL_PARTICLES = 90;
    private static final int BURST_PARTICLES = 4;

    private final BossBar bar = Bukkit.createBossBar(
            ChatColor.WHITE + "Faceoff", BarColor.WHITE, BarStyle.SOLID);

    void attach(Player owner) {
        bar.addPlayer(owner);
        bar.setVisible(true);
    }

    void score(int playerScore, int enemyScore) {
        bar.setTitle(ChatColor.AQUA + "You " + ChatColor.BOLD + playerScore
                + ChatColor.GRAY + "   -   "
                + ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + enemyScore
                + ChatColor.LIGHT_PURPLE + " " + RivalNpc.NAME);
        if (playerScore > enemyScore) {
            bar.setColor(BarColor.BLUE);
        } else if (enemyScore > playerScore) {
            bar.setColor(BarColor.PINK);
        } else {
            bar.setColor(BarColor.WHITE);
        }
        int leader = Math.max(playerScore, enemyScore);
        bar.setProgress(Math.min(1.0, (double) leader / Arena.WIN_SCORE));
    }

    /** The moment itself: horn, title, and a shower of sparks out of the net that was scored on. */
    void goal(Arena arena, boolean playerScored, Player owner) {
        Location net = goalCenter(arena, playerScored);
        burst(arena.layout().world(), net);
        if (owner == null) {
            return;
        }
        Location at = owner.getLocation();
        owner.playSound(at, Sound.EVENT_RAID_HORN, 1.0f, playerScored ? 0.9f : 0.7f);
        owner.playSound(at, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.2f);
        String title = playerScored
                ? ChatColor.AQUA + "" + ChatColor.BOLD + "GOAL!"
                : ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + RivalNpc.NAME + " SCORES";
        owner.sendTitle(title, ChatColor.GRAY + "" + arena.playerScore() + " - " + arena.enemyScore(),
                3, 40, 12);
    }

    void celebrate(Arena arena, boolean playerScored) {
        World world = arena.layout().world();
        Location net = goalCenter(arena, playerScored);
        world.spawnParticle(Particle.FIREWORK, net, 12, 1.4, 1.0, 1.4, 0.08);
    }

    void win(Player owner, boolean playerWon) {
        if (owner == null) {
            return;
        }
        Location at = owner.getLocation();
        if (playerWon) {
            owner.playSound(at, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
            owner.playSound(at, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
            owner.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "YOU WIN",
                    ChatColor.GRAY + "First to " + Arena.WIN_SCORE + " — scores reset", 5, 50, 15);
        } else {
            owner.playSound(at, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.6f);
            owner.sendTitle(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + RivalNpc.NAME + " WINS",
                    ChatColor.GRAY + "First to " + Arena.WIN_SCORE + " — scores reset", 5, 50, 15);
        }
    }

    void faceoffCount(Player owner, int count) {
        if (owner == null) {
            return;
        }
        owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f + 0.2f * (3 - count));
        owner.sendTitle(ChatColor.YELLOW + "" + ChatColor.BOLD + count,
                ChatColor.GRAY + "Faceoff", 0, 18, 4);
    }

    void faceoffGo(Arena arena, Player owner) {
        arena.layout().world().spawnParticle(Particle.SNOWFLAKE, arena.layout().puckSpawn(),
                24, 0.6, 0.3, 0.6, 0.04);
        if (owner == null) {
            return;
        }
        owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.6f);
        owner.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "GO!", "", 0, 12, 6);
    }

    void dispose() {
        bar.removeAll();
        bar.setVisible(false);
    }

    private void burst(World world, Location net) {
        world.spawnParticle(Particle.FIREWORK, net, GOAL_PARTICLES, 1.6, 1.2, 1.6, 0.16);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, net, BURST_PARTICLES, 0.8, 0.4, 0.8, 0.0);
        world.playSound(net, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
    }

    private Location goalCenter(Arena arena, boolean playerScored) {
        Vector center = playerScored
                ? arena.layout().enemyGoalBox().getCenter()
                : arena.layout().playerGoalBox().getCenter();
        return center.toLocation(arena.layout().world()).add(0, 1.0, 0);
    }
}
