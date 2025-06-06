package com.eyesaber.anticheat.listeners;

import com.eyesaber.anticheat.AntiCheatPlugin;
import com.eyesaber.anticheat.checks.inventory.InventoryCheck;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryListener implements Listener {
    
    private final AntiCheatPlugin plugin;
    private final InventoryCheck inventoryCheck;
    
    public InventoryListener(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.inventoryCheck = new InventoryCheck(plugin);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        if (player.hasPermission("anticheat.bypass")) {
            return;
        }
        
        inventoryCheck.check(player, event);
    }
}