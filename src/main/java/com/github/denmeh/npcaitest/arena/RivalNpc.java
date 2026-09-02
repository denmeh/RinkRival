package com.github.denmeh.npcaitest.arena;

import com.github.denmeh.npcaitest.ai.IdleBehavior;
import com.github.denmeh.npcaitest.npc.TestNpc;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.entity.EntityType;

import java.util.UUID;

public final class RivalNpc {

    static final String NAME = "Rival";

    private RivalNpc() {
    }

    public static TestNpc spawn(UUID ownerId, ArenaLayout layout) {
        NPC npc = CitizensAPI.getTemporaryNPCRegistry().createNPC(EntityType.PLAYER, NAME);
        npc.data().setPersistent(NPC.Metadata.SHOULD_SAVE, false);
        npc.data().setPersistent(NPC.Metadata.REMOVE_FROM_TABLIST, true);
        npc.setProtected(true);
        npc.spawn(layout.npcSpawn());

        LookClose lookClose = npc.getOrAddTrait(LookClose.class);
        lookClose.lookClose(true);
        lookClose.setRange(32);

        TestNpc rival = new TestNpc(ownerId, npc);
        rival.setActiveNode("IDLE");
        npc.getDefaultGoalController().clear();
        npc.getDefaultGoalController().addBehavior(new IdleBehavior(rival), 1);
        return rival;
    }

    public static void destroy(NPC npc) {
        if (npc == null) {
            return;
        }
        if (npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
        npc.getDefaultGoalController().clear();
        if (npc.isSpawned()) {
            npc.despawn();
        }
        npc.destroy();
    }
}
