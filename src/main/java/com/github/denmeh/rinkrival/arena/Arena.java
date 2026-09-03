package com.github.denmeh.rinkrival.arena;

import com.github.denmeh.rinkrival.arena.ai.RivalContext;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Turtle;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Arena {

    public static final int WIN_SCORE = 3;

    private final UUID ownerId;
    private final ArenaLayout layout;
    private final PlayerSnapshot ownerSnapshot;
    private final RivalDifficulty difficulty;
    private final ArenaHud hud = new ArenaHud();
    private final List<ArenaBuilder.SavedBlock> originalBlocks = new ArrayList<>();
    private NPC rival;
    private RivalContext rivalContext;
    private Turtle puck;
    private BukkitTask worldTask;
    private boolean ready;
    private int playerScore;
    private int enemyScore;
    private Phase phase = Phase.BUILDING;
    private int phaseTicks;
    private boolean lastGoalByPlayer;
    private long stunnedUntil;
    private long nextPlayerCheckAt;

    Arena(UUID ownerId, ArenaLayout layout, PlayerSnapshot ownerSnapshot, RivalDifficulty difficulty) {
        this.ownerId = ownerId;
        this.layout = layout;
        this.ownerSnapshot = ownerSnapshot;
        this.difficulty = difficulty;
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

    public RivalDifficulty difficulty() {
        return difficulty;
    }

    RivalContext rivalContext() {
        return rivalContext;
    }

    void setRivalContext(RivalContext rivalContext) {
        this.rivalContext = rivalContext;
    }

    public boolean stunned() {
        return System.currentTimeMillis() < stunnedUntil;
    }

    boolean playerCheckReady() {
        return System.currentTimeMillis() >= nextPlayerCheckAt;
    }

    public void takeCheck(long stunMs, long cooldownMs) {
        long now = System.currentTimeMillis();
        stunnedUntil = now + stunMs;
        nextPlayerCheckAt = now + cooldownMs;
    }

    void clearStun() {
        stunnedUntil = 0;
    }

    public String rivalName() {
        return rival == null ? RivalNpc.NAME : rival.getName();
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

    public ArenaHud hud() {
        return hud;
    }

    public Phase phase() {
        return phase;
    }

    /**
     * True only during live play. Goals count and the rival skates; during a celebration or a faceoff
     * countdown everything holds still.
     */
    public boolean playing() {
        return ready && phase == Phase.PLAYING;
    }

    void enterPhase(Phase next, int ticks) {
        this.phase = next;
        this.phaseTicks = ticks;
    }

    /** Counts the current phase down by one tick and reports whether it just ran out. */
    boolean phaseElapsed() {
        if (phaseTicks > 0) {
            phaseTicks--;
        }
        return phaseTicks <= 0;
    }

    int phaseTicks() {
        return phaseTicks;
    }

    void playerScored() {
        playerScore++;
        lastGoalByPlayer = true;
    }

    void enemyScored() {
        enemyScore++;
        lastGoalByPlayer = false;
    }

    boolean lastGoalByPlayer() {
        return lastGoalByPlayer;
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

    /** Pins the puck on the faceoff dot so a stray bump cannot start the play early. */
    void holdPuck() {
        if (puck == null || !puck.isValid()) {
            return;
        }
        Location dot = layout.puckSpawn();
        if (puck.getLocation().distanceSquared(dot) > 0.25) {
            puck.teleport(dot);
        }
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

    void finishBuild(List<ArenaBuilder.SavedBlock> original, NPC rival, Turtle puck) {
        originalBlocks.clear();
        originalBlocks.addAll(original);
        this.rival = rival;
        this.puck = puck;
        this.ready = true;
        this.worldTask = null;
    }

    public enum Phase {
        BUILDING,
        FACEOFF,
        PLAYING,
        CELEBRATION
    }

    void setOriginalBlocks(List<ArenaBuilder.SavedBlock> original) {
        originalBlocks.clear();
        originalBlocks.addAll(original);
    }

    List<ArenaBuilder.SavedBlock> originalBlocks() {
        return originalBlocks;
    }

    NPC npc() {
        return rival;
    }
}
