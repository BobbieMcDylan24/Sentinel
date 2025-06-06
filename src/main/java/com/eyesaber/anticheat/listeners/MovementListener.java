package com.eyesaber.anticheat.listeners;

import com.eyesaber.anticheat.AntiCheatPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementListener implements Listener {
    
    private final AntiCheatPlugin plugin;
    
    public MovementListener(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Skip if player has bypass permission
        if (player.hasPermission("anticheat.bypass")) {
            return;
        }
        
        // Check for fly
        if (!player.hasPermission("anticheat.bypass.fly")) {
            plugin.getCheckManager().getFlyCheck().check(player, event);
        }
        
        // Check for speed
        if (!player.hasPermission("anticheat.bypass.speed")) {
            plugin.getCheckManager().getSpeedCheck().check(player, event);
        }
        
        // Check for nofall
        if (!player.hasPermission("anticheat.bypass.nofall")) {
            plugin.getCheckManager().getNoFallCheck().check(player, event);
        }
    }
}