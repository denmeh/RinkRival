package com.github.denmeh.rinkrival.arena;

import com.github.denmeh.rinkrival.ai.BehaviorTreeGoal;
import com.github.denmeh.rinkrival.arena.ai.RivalContext;
import com.github.denmeh.rinkrival.arena.ai.RivalTree;
import com.github.denmeh.rinkrival.npc.TestNpc;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public final class RivalNpc {

    /** Fallback label when no roster name is available. */
    static final String NAME = "Rival";
    private static final float SPEED_MODIFIER = 2.05f;

    private RivalNpc() {
    }

    public static TestNpc spawn(Arena arena, ArenaKit kit) {
        return spawn(arena, kit, arena.difficulty());
    }

    public static TestNpc spawn(Arena arena, ArenaKit kit, RivalDifficulty difficulty) {
        RivalRoster.Pick pick = RivalRoster.pick(new Random());
        NPC npc = CitizensAPI.getTemporaryNPCRegistry().createNPC(EntityType.PLAYER, pick.name());
        npc.data().setPersistent(NPC.Metadata.SHOULD_SAVE, false);
        npc.data().setPersistent(NPC.Metadata.REMOVE_FROM_TABLIST, true);
        npc.data().set(NPC.Metadata.DAMAGE_OTHERS, true);
        npc.data().set(NPC.Metadata.COLLIDABLE, true);
        // Unprotected so a stick swing keeps vanilla knockback. Damage is zeroed in ArenaListener.
        npc.setProtected(false);
        npc.spawn(arena.layout().npcSpawn());

        ItemStack lightStick = kit.knockbackStick(1);
        ItemStack heavyStick = kit.knockbackStick(2);
        if (npc.getEntity() instanceof Player player) {
            player.getInventory().setItem(0, lightStick.clone());
            player.getInventory().setItem(1, heavyStick.clone());
            player.getInventory().setItemInMainHand(lightStick.clone());
            player.getInventory().setChestplate(pick.jersey());
            player.setSprinting(true);
        }

        TestNpc rival = new TestNpc(arena.ownerId(), npc, pick.name());
        rival.setActiveNode("IDLE");
        RivalContext ctx = new RivalContext(arena, rival, lightStick, heavyStick, difficulty);
        arena.setRivalContext(ctx);
        configureNavigator(npc, ctx);

        GoalController controller = npc.getDefaultGoalController();
        controller.clear();
        controller.addBehavior(new BehaviorTreeGoal(RivalTree.build(ctx, difficulty), rival::setActiveNode), 1);
        return rival;
    }

    public static void toFaceoff(Arena arena) {
        NPC npc = arena.npc();
        if (npc == null || !npc.isSpawned()) {
            return;
        }
        if (npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
        npc.teleport(arena.layout().npcSpawn(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        arena.clearStun();
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
