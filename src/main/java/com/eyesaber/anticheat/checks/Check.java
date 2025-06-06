package com.eyesaber.anticheat.checks;

import com.eyesaber.anticheat.AntiCheatPlugin;
import org.bukkit.entity.Player;

public abstract class Check {
    
    protected final AntiCheatPlugin plugin;
    protected final String checkName;
    
    public Check(AntiCheatPlugin plugin, String checkName) {
        this.plugin = plugin;
        this.checkName = checkName;
    }
    
    protected boolean isEnabled() {
        return plugin.getConfigManager().getConfig()
            .getBoolean("anticheat.checks." + checkName + ".enabled", true);
    }
    
    protected int getMaxViolations() {
        return plugin.getConfigManager().getConfig()
            .getInt("anticheat.checks." + checkName + ".max-violations", 10);
    }
    
    protected void flag(Player player, String details) {
        // Debug logging
        plugin.getLogger().info("FLAG CALLED: Player=" + player.getName() + ", Check=" + checkName + ", Details=" + details);
        
        // Add violation
        plugin.getViolationManager().addViolation(player, checkName);
        
        // Send alert
        plugin.getAlertManager().sendAlert(player, checkName, details);
        
        // Check if punishment should be executed
        int violations = plugin.getViolationManager().getViolations(player, checkName);
        int maxViolations = getMaxViolations();
        
        plugin.getLogger().info("Violations: " + violations + "/" + maxViolations);
        
        if (violations >= maxViolations) {
            plugin.getLogger().info("Max violations reached, executing punishment for " + player.getName());
            plugin.getCheckManager().executePunishment(player, checkName);
        }
    }
    
    public String getName() {
        return checkName;
    }
}