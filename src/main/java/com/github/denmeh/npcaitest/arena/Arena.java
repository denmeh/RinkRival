package com.github.denmeh.npcaitest.arena;

import com.github.denmeh.npcaitest.npc.TestNpc;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Slime;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public final class Arena {

    static final int WIN_SCORE = 3;
    static final int SCORE_COOLDOWN_TICKS = 30;

    private final UUID ownerId;
    private final ArenaLayout layout;
    private final List<ArenaBuilder.SavedBlock> original;
    private final TestNpc rival;
    private Slime puck;
    private int playerScore;
    private int enemyScore;
    private int scoreCooldown;

    Arena(UUID ownerId, ArenaLayout layout, List<ArenaBuilder.SavedBlock> original, TestNpc rival, Slime puck) {
        this.ownerId = ownerId;
        this.layout = layout;
        this.original = original;
        this.rival = rival;
        this.puck = puck;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public ArenaLayout layout() {
        return layout;
    }

    public TestNpc rival() {
        return rival;
    }

    public Slime puck() {
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
        return scoreCooldown <= 0;
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

    void replacePuck(Slime next) {
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

    List<ArenaBuilder.SavedBlock> original() {
        return original;
    }

    NPC npc() {
        return rival.npc();
    }
}
