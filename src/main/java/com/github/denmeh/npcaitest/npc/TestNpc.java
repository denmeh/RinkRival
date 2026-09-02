package com.github.denmeh.npcaitest.npc;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class TestNpc {

    private final UUID ownerId;
    private final NPC npc;
    private String activeNode = "NONE";

    public TestNpc(UUID ownerId, NPC npc) {
        this.ownerId = ownerId;
        this.npc = npc;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public NPC npc() {
        return npc;
    }

    public String activeNode() {
        return activeNode;
    }

    public void setActiveNode(String activeNode) {
        this.activeNode = activeNode;
    }

    public boolean isOwnedBy(Player player) {
        return ownerId.equals(player.getUniqueId());
    }
}
