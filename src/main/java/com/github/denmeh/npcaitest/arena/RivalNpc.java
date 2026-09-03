package com.github.denmeh.npcaitest.arena;

import com.github.denmeh.npcaitest.ai.IdleBehavior;
import com.github.denmeh.npcaitest.arena.ai.BodyCheck;
import com.github.denmeh.npcaitest.arena.ai.ChaseToIntercept;
import com.github.denmeh.npcaitest.arena.ai.GuardNet;
import com.github.denmeh.npcaitest.arena.ai.RivalContext;
import com.github.denmeh.npcaitest.arena.ai.StrikeTowardGoal;
import com.github.denmeh.npcaitest.npc.TestNpc;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.ai.tree.Precondition;
import net.citizensnpcs.api.ai.tree.Sequence;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public final class RivalNpc {

    static final String NAME = "Rival";
    private static final float SPEED_MODIFIER = 1.75f;

    /**
     * Role selection lives here rather than in a {@link net.citizensnpcs.api.ai.tree.Selector}: the goal
     * controller re-checks priorities every tick and preempts what is running, so a check or a defensive
     * drop-back can interrupt a chase mid-stride.
     */
    private static final int CHECK_PRIORITY = 4;
    private static final int GUARD_PRIORITY = 3;
    private static final int ATTACK_PRIORITY = 2;
    private static final int IDLE_PRIORITY = 1;

    private RivalNpc() {
    }

    public static TestNpc spawn(Arena arena, ArenaKit kit) {
        NPC npc = CitizensAPI.getTemporaryNPCRegistry().createNPC(EntityType.PLAYER, NAME);
        npc.data().setPersistent(NPC.Metadata.SHOULD_SAVE, false);
        npc.data().setPersistent(NPC.Metadata.REMOVE_FROM_TABLIST, true);
        npc.data().set(NPC.Metadata.DAMAGE_OTHERS, true);
        npc.setProtected(true);
        npc.spawn(arena.layout().npcSpawn());

        ItemStack lightStick = kit.knockbackStick(1);
        ItemStack heavyStick = kit.knockbackStick(2);
        if (npc.getEntity() instanceof Player player) {
            player.getInventory().setItem(0, lightStick.clone());
            player.getInventory().setItem(1, heavyStick.clone());
            player.getInventory().setItemInMainHand(lightStick.clone());
            player.setSprinting(true);
        }

        TestNpc rival = new TestNpc(arena.ownerId(), npc);
        rival.setActiveNode("IDLE");
        RivalContext ctx = new RivalContext(arena, rival, lightStick, heavyStick);
        configureNavigator(npc, ctx);

        GoalController controller = npc.getDefaultGoalController();
        controller.clear();
        controller.addBehavior(new BodyCheck(ctx), CHECK_PRIORITY);
        controller.addBehavior(new GuardNet(ctx), GUARD_PRIORITY);
        controller.addBehavior(Precondition.wrappingPrecondition(
                Sequence.createSequence(new ChaseToIntercept(ctx), new StrikeTowardGoal(ctx)),
                () -> ctx.playable() && ctx.puckAlive()), ATTACK_PRIORITY);
        controller.addBehavior(new IdleBehavior(rival), IDLE_PRIORITY);
        return rival;
    }

    /** Parks the rival on its faceoff dot and drops whatever it was doing. */
    public static void toFaceoff(Arena arena) {
        NPC npc = arena.npc();
        if (npc == null || !npc.isSpawned()) {
            return;
        }
        if (npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
        npc.teleport(arena.layout().npcSpawn(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        if (arena.rival() != null) {
            arena.rival().setActiveNode("FACEOFF");
        }
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

    private static void configureNavigator(NPC npc, RivalContext ctx) {
        NavigatorParameters params = npc.getNavigator().getDefaultParameters();
        params.speedModifier(SPEED_MODIFIER);
        params.distanceMargin(0.75);
        params.pathDistanceMargin(0.5);
        params.straightLineTargetingDistance(8f);
        params.range(48f);
        params.lookAtFunction(navigator -> ctx.lookTarget());
    }
}
