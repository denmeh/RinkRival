package com.github.denmeh.npcaitest.arena;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ArenaBuilder {

    static final int BLOCKS_PER_TICK = 192;

    private ArenaBuilder() {
    }

    public static BukkitTask paste(Plugin plugin, ArenaLayout layout, Consumer<List<SavedBlock>> onSnapshot,
            Consumer<List<SavedBlock>> onComplete) {
        List<ArenaLayout.BlockCell> cells = new ArrayList<>();
        layout.forEachVoxel(cells::add);
        World world = layout.world();
        List<SavedBlock> original = new ArrayList<>(cells.size());
        int[] index = {0};
        boolean[] snapshotting = {true};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int budget = BLOCKS_PER_TICK;
            while (budget-- > 0 && index[0] < cells.size()) {
                ArenaLayout.BlockCell cell = cells.get(index[0]++);
                Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
                if (snapshotting[0]) {
                    original.add(new SavedBlock(cell.x(), cell.y(), cell.z(), block.getBlockData().clone()));
                } else {
                    BlockData next = cell.material().createBlockData();
                    if (!block.getBlockData().equals(next)) {
                        block.setBlockData(next, false);
                    }
                }
            }
            if (index[0] < cells.size()) {
                return;
            }
            if (snapshotting[0]) {
                snapshotting[0] = false;
                index[0] = 0;
                onSnapshot.accept(List.copyOf(original));
                return;
            }
            task[0].cancel();
            onComplete.accept(original);
        }, 1L, 1L);
        return task[0];
    }

    public static BukkitTask restoreLater(Plugin plugin, World world, List<SavedBlock> original) {
        if (world == null || original == null || original.isEmpty()) {
            return null;
        }
        int[] index = {original.size() - 1};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int budget = BLOCKS_PER_TICK;
            while (budget-- > 0 && index[0] >= 0) {
                SavedBlock saved = original.get(index[0]--);
                world.getBlockAt(saved.x(), saved.y(), saved.z()).setBlockData(saved.data(), false);
            }
            if (index[0] < 0) {
                task[0].cancel();
            }
        }, 1L, 1L);
        return task[0];
    }

    public static void restoreNow(World world, List<SavedBlock> original) {
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
