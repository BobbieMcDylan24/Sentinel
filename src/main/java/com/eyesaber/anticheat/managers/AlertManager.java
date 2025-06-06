package com.eyesaber.anticheat.managers;

import com.eyesaber.anticheat.AntiCheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class AlertManager {
    
    private final AntiCheatPlugin plugin;
    
    public AlertManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void sendAlert(Player player, String checkName, String details) {
        if (!plugin.getConfigManager().getConfig().getBoolean("anticheat.alerts.enabled", true)) {
            return;
        }
        
        // Get violation count
        int violations = plugin.getViolationManager().getViolations(player, checkName);
        
        // Get ping (with fallback for older versions)
        int ping = getPing(player);
        
        // Format the alert message - using SentinelAC
        String format = plugin.getConfigManager().getConfig()
            .getString("anticheat.alerts.format", "&8[&cSentinelAC&8] &f%player% &7failed &c%check% &8(&7x%violations%&8) &8[&7%ping%ms&8]");
        
        String alertMessage = ChatColor.translateAlternateColorCodes('&', format
            .replace("%player%", player.getName())
            .replace("%check%", checkName)
            .replace("%violations%", String.valueOf(violations))
            .replace("%ping%", String.valueOf(ping))
            .replace("%details%", details != null ? details : ""));
        
        // Send to console
        if (plugin.getConfigManager().getConfig().getBoolean("anticheat.alerts.console", true)) {
            Bukkit.getConsoleSender().sendMessage(alertMessage);
        }
        
        // Send to staff
        if (plugin.getConfigManager().getConfig().getBoolean("anticheat.alerts.staff", true)) {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("anticheat.alerts")) {
                    staff.sendMessage(alertMessage);
                }
            }
        }
    }
    
    private int getPing(Player player) {
        try {
            // Try modern method first (1.17+)
            return player.getPing();
        } catch (Exception e) {
            try {
                // Fallback for older versions using reflection
                Object handle = player.getClass().getMethod("getHandle").invoke(player);
                Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
                return (Integer) playerConnection.getClass().getField("ping").get(playerConnection);
            } catch (Exception ex) {
                return 0; // Default if all methods fail
            }
        }
    }
}