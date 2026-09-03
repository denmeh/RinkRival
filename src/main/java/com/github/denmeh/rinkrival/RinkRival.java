package com.github.denmeh.rinkrival;

import com.github.denmeh.rinkrival.arena.ArenaListener;
import com.github.denmeh.rinkrival.arena.ArenaService;
import com.github.denmeh.rinkrival.arena.PuckListener;
import com.github.denmeh.rinkrival.command.RinkCommand;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class RinkRival extends JavaPlugin implements Listener {

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

        arenas = new ArenaService(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PuckListener(arenas), this);
        getServer().getPluginManager().registerEvents(new ArenaListener(arenas), this);

        PluginCommand command = getCommand("rink");
        if (command != null) {
            RinkCommand executor = new RinkCommand(this);
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
            arenas.leaveAll();
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
        getLogger().info("Citizens API ready. Use /rink arena");
    }

    public boolean isCitizensReady() {
        return citizensReady && CitizensAPI.hasImplementation();
    }

    public ArenaService arenas() {
        return arenas;
    }
}
