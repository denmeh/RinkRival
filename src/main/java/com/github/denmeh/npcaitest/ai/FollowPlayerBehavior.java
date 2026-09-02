package com.github.denmeh.npcaitest.ai;

import com.github.denmeh.npcaitest.npc.TestNpc;
import net.citizensnpcs.api.ai.EntityTarget;
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class FollowPlayerBehavior extends BehaviorGoalAdapter {

    private final TestNpc testNpc;
    private final double range;
    private Player target;

    public FollowPlayerBehavior(TestNpc testNpc, double range) {
        this.testNpc = testNpc;
        this.range = range;
    }

    @Override
    public void reset() {
        target = null;
        NPC npc = testNpc.npc();
        if (npc.isSpawned() && npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
    }

    @Override
    public BehaviorStatus run() {
        testNpc.setActiveNode("FOLLOW");
        NPC npc = testNpc.npc();
        if (!npc.isSpawned() || target == null || !target.isValid() || isOutOfRange(npc, target)) {
            return BehaviorStatus.FAILURE;
        }

        EntityTarget current = npc.getNavigator().getEntityTarget();
        Entity currentEntity = current == null ? null : current.getTarget();
        if (currentEntity == null || !currentEntity.equals(target)) {
            npc.getNavigator().setTarget(target, false);
        }
        return BehaviorStatus.RUNNING;
    }

    @Override
    public boolean shouldExecute() {
        NPC npc = testNpc.npc();
        if (!npc.isSpawned()) {
            return false;
        }
        target = nearestPlayer(npc);
        return target != null;
    }

    private Player nearestPlayer(NPC npc) {
        Entity entity = npc.getEntity();
        if (entity == null) {
            return null;
        }
        Player nearest = null;
        double nearestDistance = range;
        for (Player player : entity.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(entity.getLocation());
            if (distance <= nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean isOutOfRange(NPC npc, Player player) {
        Entity entity = npc.getEntity();
        return entity == null
                || !entity.getWorld().equals(player.getWorld())
                || entity.getLocation().distanceSquared(player.getLocation()) > range * range;
    }
}
