package com.github.denmeh.npcaitest.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Places {@link ArenaSchematic} in front of the player, yaw snapped to a cardinal.
 * Local col 0 is the left side, row 0 is the near (player/blue) end.
 */
public final class ArenaLayout {

    private static final double GOAL_EXPAND = 0.35;

    private final ArenaSchematic schematic;
    private final World world;
    private final BlockFace facing;
    private final int originX;
    private final int originZ;
    private final int floorY;
    private final BoundingBox playerGoalBox;
    private final BoundingBox enemyGoalBox;
    private final BoundingBox rinkBox;

    private ArenaLayout(ArenaSchematic schematic, World world, BlockFace facing, int originX, int originZ, int floorY) {
        this.schematic = schematic;
        this.world = world;
        this.facing = facing;
        this.originX = originX;
        this.originZ = originZ;
        this.floorY = floorY;
        this.playerGoalBox = unionCells(schematic.playerGoal()).expand(GOAL_EXPAND);
        this.enemyGoalBox = unionCells(schematic.enemyGoal()).expand(GOAL_EXPAND);
        this.rinkBox = unionVoxels().expand(0.25);
    }

    public static ArenaLayout place(ArenaSchematic schematic, Player player) {
        Location location = player.getLocation();
        BlockFace facing = cardinal(location.getYaw());
        int originX = location.getBlockX() + facing.getModX();
        int originZ = location.getBlockZ() + facing.getModZ();
        return new ArenaLayout(schematic, location.getWorld(), facing, originX, originZ, location.getBlockY() - 1);
    }

    public World world() {
        return world;
    }

    public Location playerSpawn() {
        return standing(schematic.playerSpawn(), yaw());
    }

    public Location npcSpawn() {
        return standing(schematic.npcSpawn(), oppositeYaw());
    }

    public Location puckSpawn() {
        return standing(schematic.puckSpawn(), yaw());
    }

    public BoundingBox playerGoalBox() {
        return playerGoalBox;
    }

    public BoundingBox enemyGoalBox() {
        return enemyGoalBox;
    }

    public BoundingBox rinkBox() {
        return rinkBox;
    }

    public boolean overlaps(ArenaLayout other) {
        if (other == null || world != other.world) {
            return false;
        }
        return rinkBox.overlaps(other.rinkBox);
    }

    public void forEachVoxel(Consumer<BlockCell> consumer) {
        for (ArenaSchematic.Voxel voxel : schematic.voxels()) {
            consumer.accept(new BlockCell(
                    worldX(localWidth(voxel.col()), voxel.row()),
                    floorY + voxel.layer(),
                    worldZ(localWidth(voxel.col()), voxel.row()),
                    voxel.material()));
        }
    }

    private BoundingBox unionCells(List<ArenaSchematic.Cell> cells) {
        BoundingBox box = null;
        for (ArenaSchematic.Cell cell : cells) {
            BoundingBox part = blockBox(localWidth(cell.col()), cell.layer(), cell.row());
            box = box == null ? part : box.union(part);
        }
        if (box == null) {
            throw new IllegalStateException("goal has no cells");
        }
        return box;
    }

    private BoundingBox unionVoxels() {
        BoundingBox box = null;
        for (ArenaSchematic.Voxel voxel : schematic.voxels()) {
            BoundingBox part = blockBox(localWidth(voxel.col()), voxel.layer(), voxel.row());
            box = box == null ? part : box.union(part);
        }
        return box;
    }

    private BoundingBox blockBox(int width, int layer, int row) {
        int x = worldX(width, row);
        int y = floorY + layer;
        int z = worldZ(width, row);
        return new BoundingBox(x, y, z, x + 1.0, y + 1.0, z + 1.0);
    }

    private Location standing(ArenaSchematic.Cell cell, float yaw) {
        int width = localWidth(cell.col());
        return new Location(world, worldX(width, cell.row()) + 0.5, floorY + cell.layer(),
                worldZ(width, cell.row()) + 0.5, yaw, 0);
    }

    private int localWidth(int col) {
        return col - schematic.centerCol();
    }

    private int worldX(int width, int length) {
        return (int) Math.floor(worldX((double) width, length));
    }

    private int worldZ(int width, int length) {
        return (int) Math.floor(worldZ((double) width, length));
    }

    private double worldX(double width, double length) {
        return switch (facing) {
            case SOUTH -> originX - width;
            case NORTH -> originX + width;
            case EAST -> originX + length;
            case WEST -> originX - length;
            default -> originX;
        };
    }

    private double worldZ(double width, double length) {
        return switch (facing) {
            case SOUTH -> originZ + length;
            case NORTH -> originZ - length;
            case EAST -> originZ + width;
            case WEST -> originZ - width;
            default -> originZ;
        };
    }

    private float yaw() {
        return switch (facing) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> -90f;
            default -> 0f;
        };
    }

    private float oppositeYaw() {
        return switch (facing) {
            case SOUTH -> 180f;
            case WEST -> -90f;
            case NORTH -> 0f;
            case EAST -> 90f;
            default -> 180f;
        };
    }

    static BlockFace cardinal(float yaw) {
        float wrapped = yaw % 360f;
        if (wrapped < 0) {
            wrapped += 360f;
        }
        if (wrapped >= 315 || wrapped < 45) {
            return BlockFace.SOUTH;
        }
        if (wrapped < 135) {
            return BlockFace.WEST;
        }
        if (wrapped < 225) {
            return BlockFace.NORTH;
        }
        return BlockFace.EAST;
    }

    public record BlockCell(int x, int y, int z, Material material) {
    }
}
