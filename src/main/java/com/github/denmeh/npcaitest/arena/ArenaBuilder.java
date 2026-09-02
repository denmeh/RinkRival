package com.github.denmeh.npcaitest.arena;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;

public final class ArenaBuilder {

    private ArenaBuilder() {
    }

    public static List<SavedBlock> snapshotAndBuild(ArenaLayout layout) {
        World world = layout.world();
        List<SavedBlock> original = new ArrayList<>();
        layout.forEachVoxel(cell -> {
            Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
            original.add(new SavedBlock(cell.x(), cell.y(), cell.z(), block.getBlockData().clone()));
        });
        layout.forEachVoxel(cell -> world.getBlockAt(cell.x(), cell.y(), cell.z())
                .setBlockData(cell.material().createBlockData(), false));
        return original;
    }

    public static void restore(World world, List<SavedBlock> original) {
        if (world == null || original == null) {
            return;
        }
        for (int i = original.size() - 1; i >= 0; i--) {
            SavedBlock saved = original.get(i);
            world.getBlockAt(saved.x(), saved.y(), saved.z()).setBlockData(saved.data(), false);
        }
    }

    public record SavedBlock(int x, int y, int z, BlockData data) {
    }
}
