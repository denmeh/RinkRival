package com.github.denmeh.rinkrival.ai;

import com.github.denmeh.rinkrival.npc.TestNpc;
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import net.citizensnpcs.api.npc.NPC;

public final class IdleBehavior extends BehaviorGoalAdapter {

    private final TestNpc testNpc;

    public IdleBehavior(TestNpc testNpc) {
        this.testNpc = testNpc;
    }

    @Override
    public void reset() {
    }

    @Override
    public BehaviorStatus run() {
        testNpc.setActiveNode("IDLE");
        NPC npc = testNpc.npc();
        if (npc.isSpawned() && npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
        return BehaviorStatus.RUNNING;
    }

    @Override
    public boolean shouldExecute() {
        return testNpc.npc().isSpawned();
    }
}
