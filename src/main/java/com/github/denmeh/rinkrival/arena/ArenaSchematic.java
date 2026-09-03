package com.github.denmeh.rinkrival.arena;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class ArenaSchematic {

    private static final String RESOURCE = "arena/rink.txt";

    private final int width;
    private final int height;
    private final int length;
    private final List<Voxel> voxels;
    private final Cell playerSpawn;
    private final Cell npcSpawn;
    private final Cell puckSpawn;
    private final List<Cell> playerGoal;
    private final List<Cell> enemyGoal;

    private ArenaSchematic(int width, int height, int length, List<Voxel> voxels, Cell playerSpawn,
            Cell npcSpawn, Cell puckSpawn, List<Cell> playerGoal, List<Cell> enemyGoal) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.voxels = List.copyOf(voxels);
        this.playerSpawn = playerSpawn;
        this.npcSpawn = npcSpawn;
        this.puckSpawn = puckSpawn;
        this.playerGoal = List.copyOf(playerGoal);
        this.enemyGoal = List.copyOf(enemyGoal);
    }

    public static ArenaSchematic load(JavaPlugin plugin) {
        plugin.getDataFolder().mkdirs();
        Path file = plugin.getDataFolder().toPath().resolve(RESOURCE);
        if (!Files.exists(file)) {
            plugin.saveResource(RESOURCE, false);
        } else {
            try {
                ArenaSchematic existing = parse(Files.readString(file, StandardCharsets.UTF_8));
                if (existing.width() > 22 || existing.length() > 36) {
                    plugin.getLogger().info("Replacing oversized rink schematic with the bundled 19x33 1v1 layout");
                    plugin.saveResource(RESOURCE, true);
                }
            } catch (IOException | IllegalArgumentException ignored) {
                plugin.saveResource(RESOURCE, true);
            }
        }
        try {
            ArenaSchematic parsed = parse(Files.readString(file, StandardCharsets.UTF_8));
            plugin.getLogger().info("Loaded rink " + parsed.width() + "x" + parsed.length() + "x"
                    + parsed.height() + " from " + file.toAbsolutePath());
            return parsed;
        } catch (IOException | IllegalArgumentException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load " + RESOURCE, exception);
            return null;
        }
    }

    static ArenaSchematic parse(String text) {
        List<List<String>> layers = new ArrayList<>();
        List<String> current = new ArrayList<>();
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                if (line.isEmpty() && !current.isEmpty()) {
                    layers.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            if (line.toUpperCase(Locale.ROOT).startsWith("LAYER")) {
                if (!current.isEmpty()) {
                    layers.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            current.add(line);
        }
        if (!current.isEmpty()) {
            layers.add(current);
        }
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("schematic has no layers");
        }
        int width = layers.getFirst().getFirst().length();
        int length = layers.getFirst().size();
        int height = layers.size();
        List<Voxel> voxels = new ArrayList<>();
        Cell playerSpawn = null;
        Cell npcSpawn = null;
        Cell puckSpawn = null;
        List<Cell> playerGoal = new ArrayList<>();
        List<Cell> enemyGoal = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            List<String> rows = layers.get(y);
            if (rows.size() != length) {
                throw new IllegalArgumentException("layer " + y + " has " + rows.size() + " rows, expected " + length);
            }
            for (int z = 0; z < length; z++) {
                String row = rows.get(z);
                if (row.length() != width) {
                    throw new IllegalArgumentException("layer " + y + " row " + z + " width " + row.length()
                            + ", expected " + width);
                }
                for (int x = 0; x < width; x++) {
                    char glyph = row.charAt(x);
                    Cell cell = new Cell(x, y, z);
                    Material material = materialFor(glyph, y, z, x);
                    voxels.add(new Voxel(x, y, z, material));
                    switch (glyph) {
                        case 'P' -> playerSpawn = requireUnique(playerSpawn, cell, "P");
                        case 'N' -> npcSpawn = requireUnique(npcSpawn, cell, "N");
                        case 'O' -> puckSpawn = requireUnique(puckSpawn, cell, "O");
                        case 'g' -> playerGoal.add(cell);
                        case 'e' -> enemyGoal.add(cell);
                        default -> {
                        }
                    }
                }
            }
        }
        if (playerSpawn == null || npcSpawn == null || puckSpawn == null) {
            throw new IllegalArgumentException("schematic must mark P, N and O spawns");
        }
        if (playerGoal.isEmpty() || enemyGoal.isEmpty()) {
            throw new IllegalArgumentException("schematic must mark g (player net) and e (enemy net)");
        }
        return new ArenaSchematic(width, height, length, voxels, playerSpawn, npcSpawn, puckSpawn, playerGoal, enemyGoal);
    }

    private static Material materialFor(char glyph, int y, int z, int x) {
        return switch (glyph) {
            case 'f', 'i' -> Material.PACKED_ICE;
            case 'z' -> Material.BLUE_ICE;
            case 'w' -> Material.WHITE_CONCRETE;
            case 'q' -> Material.QUARTZ_BLOCK;
            case 'n' -> Material.LIGHT_BLUE_CONCRETE;
            case 'k' -> Material.RED_TERRACOTTA;
            case 'b' -> Material.BLUE_CONCRETE;
            case 'r' -> Material.RED_CONCRETE;
            case 'm' -> Material.SEA_LANTERN;
            case 'y' -> Material.LIGHT_BLUE_STAINED_GLASS;
            case 'h' -> Material.BLUE_STAINED_GLASS;
            case 'j' -> Material.RED_STAINED_GLASS;
            case '.', 'P', 'N', 'O', 'g', 'e' -> Material.AIR;
            default -> throw new IllegalArgumentException(
                    "unknown schematic char '" + glyph + "' at layer " + y + " row " + z + " col " + x);
        };
    }

    private static Cell requireUnique(Cell existing, Cell next, String name) {
        if (existing != null) {
            throw new IllegalArgumentException("duplicate " + name + " marker");
        }
        return next;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int length() {
        return length;
    }

    public int centerCol() {
        return width / 2;
    }

    public List<Voxel> voxels() {
        return voxels;
    }

    public Cell playerSpawn() {
        return playerSpawn;
    }

    public Cell npcSpawn() {
        return npcSpawn;
    }

    public Cell puckSpawn() {
        return puckSpawn;
    }

    public List<Cell> playerGoal() {
        return playerGoal;
    }

    public List<Cell> enemyGoal() {
        return enemyGoal;
    }

    public record Voxel(int col, int layer, int row, Material material) {
    }

    public record Cell(int col, int layer, int row) {
    }
}
