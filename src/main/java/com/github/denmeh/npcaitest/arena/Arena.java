package com.github.denmeh.npcaitest.arena;

import com.github.denmeh.npcaitest.npc.TestNpc;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Turtle;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Arena {

    static final int WIN_SCORE = 3;
    static final int SCORE_COOLDOWN_TICKS = 30;

    private final UUID ownerId;
    private final ArenaLayout layout;
    private final PlayerSnapshot ownerSnapshot;
    private final List<ArenaBuilder.SavedBlock> originalBlocks = new ArrayList<>();
    private TestNpc rival;
    private Turtle puck;
    private BukkitTask worldTask;
    private boolean ready;
    private int playerScore;
    private int enemyScore;
    private int scoreCooldown;

    Arena(UUID ownerId, ArenaLayout layout, PlayerSnapshot ownerSnapshot) {
        this.ownerId = ownerId;
        this.layout = layout;
        this.ownerSnapshot = ownerSnapshot;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public ArenaLayout layout() {
        return layout;
    }

    public PlayerSnapshot ownerSnapshot() {
        return ownerSnapshot;
    }

    public boolean ready() {
        return ready;
    }

    public TestNpc rival() {
        return rival;
    }

    public Turtle puck() {
        return puck;
    }

    public int playerScore() {
        return playerScore;
    }

    public int enemyScore() {
        return enemyScore;
    }

    public boolean isPuck(UUID entityId) {
        return puck != null && puck.getUniqueId().equals(entityId);
    }

    void tickCooldown(int ticks) {
        if (scoreCooldown > 0) {
            scoreCooldown = Math.max(0, scoreCooldown - ticks);
        }
    }

    boolean canScore() {
        return ready && scoreCooldown <= 0;
    }

    void playerScored() {
        playerScore++;
        scoreCooldown = SCORE_COOLDOWN_TICKS;
    }

    void enemyScored() {
        enemyScore++;
        scoreCooldown = SCORE_COOLDOWN_TICKS;
    }

    boolean playerWon() {
        return playerScore >= WIN_SCORE;
    }

    boolean enemyWon() {
        return enemyScore >= WIN_SCORE;
    }

    void resetScores() {
        playerScore = 0;
        enemyScore = 0;
    }

    void replacePuck(Turtle next) {
        this.puck = next;
    }

    void resetPuck() {
        if (puck == null || !puck.isValid()) {
            return;
        }
        puck.teleport(layout.puckSpawn());
        puck.setVelocity(new Vector());
        puck.setFallDistance(0);
    }

    void setWorldTask(BukkitTask worldTask) {
        this.worldTask = worldTask;
    }

    void cancelWorldTask() {
        if (worldTask != null) {
            worldTask.cancel();
            worldTask = null;
        }
    }

    void finishBuild(List<ArenaBuilder.SavedBlock> original, TestNpc rival, Turtle puck) {
        originalBlocks.clear();
        originalBlocks.addAll(original);
        this.rival = rival;
        this.puck = puck;
        this.ready = true;
        this.worldTask = null;
    }

    void setOriginalBlocks(List<ArenaBuilder.SavedBlock> original) {
        originalBlocks.clear();
        originalBlocks.addAll(original);
    }

    List<ArenaBuilder.SavedBlock> originalBlocks() {
        return originalBlocks;
    }

    NPC npc() {
        return rival == null ? null : rival.npc();
    }
}
