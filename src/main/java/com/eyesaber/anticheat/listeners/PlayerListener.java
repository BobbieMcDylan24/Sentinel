package com.eyesaber.anticheat.listeners;

import com.eyesaber.anticheat.AntiCheatPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    
    private final AntiCheatPlugin plugin;
    
    public PlayerListener(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Clear any existing violations for rejoining players
        plugin.getViolationManager().clearViolations(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up player data to prevent memory leaks
        plugin.getViolationManager().clearViolations(event.getPlayer());
    }
}