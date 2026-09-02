package com.github.denmeh.npcaitest;

import com.github.denmeh.npcaitest.arena.ArenaService;
import com.github.denmeh.npcaitest.arena.PuckListener;
import com.github.denmeh.npcaitest.command.NpcTestCommand;
import com.github.denmeh.npcaitest.npc.TestNpcService;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class NpcAiTest extends JavaPlugin implements Listener {

    private TestNpcService npcs;
    private ArenaService arenas;
    private boolean citizensReady;

    @Override
    public void onEnable() {

        Plugin citizens = getServer().getPluginManager().getPlugin("Citizens");
        if (citizens == null || !citizens.isEnabled()) {
            getLogger().log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        npcs = new TestNpcService(this);
        arenas = new ArenaService(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PuckListener(arenas), this);

        PluginCommand command = getCommand("npctest");
        if (command != null) {
            NpcTestCommand executor = new NpcTestCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        if (CitizensAPI.hasImplementation()) {
            onCitizensReady();
        }
    }

    @Override
    public void onDisable() {
        if (arenas != null) {
            arenas.deleteAll();
        }
        if (npcs != null) {
            npcs.removeAll();
        }
        citizensReady = false;
    }

    @EventHandler
    public void onCitizensEnable(CitizensEnableEvent event) {
        onCitizensReady();
    }

    private void onCitizensReady() {
        if (citizensReady) {
            return;
        }
        citizensReady = true;
        getLogger().info("Citizens API ready. Use /npctest spawn");
    }

    public boolean isCitizensReady() {
        return citizensReady && CitizensAPI.hasImplementation();
    }

    public TestNpcService npcs() {
        return npcs;
    }

    public ArenaService arenas() {
        return arenas;
    }
}
