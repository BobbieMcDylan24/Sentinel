package com.sentinelac.checks;

import com.sentinelac.SentinelAC;
import com.sentinelac.data.PlayerData;
import org.bukkit.entity.Player;

public abstract class Check {

    protected final SentinelAC plugin;
    protected final String name;
    protected final String description;
    
    // Check type flags
    protected boolean movementCheck = false;
    protected boolean combatCheck = false;
    protected boolean interactionCheck = false;
    protected boolean packetCheck = false;
    
    // Configuration
    protected int maxViolations = 5;
    protected boolean enabled = true;

    public Check(SentinelAC plugin, String name, String description) {
        this.plugin = plugin;
        this.name = name;
        this.description = description;
        loadConfig();
    }
    
    /**
     * Load configuration for this check
     */
    protected void loadConfig() {
        this.enabled = plugin.getConfigManager().isCheckEnabled(name);
        this.maxViolations = plugin.getConfigManager().getCheckMaxViolations(name);
    }
    
    /**
     * Reload configuration for this check
     */
    public void reload() {
        loadConfig();
    }
    
    /**
     * Check movement-related violations
     * Override this method in movement checks
     */
    public void checkMovement(Player player, PlayerData data) {
        // Override in subclasses
    }
    
    /**
     * Check combat-related violations
     * Override this method in combat checks
     */
    public void checkCombat(Player player, PlayerData data, Object event) {
        // Override in subclasses
    }
    
    /**
     * Check interaction-related violations
     * Override this method in interaction checks
     */
    public void checkInteraction(Player player, PlayerData data, Object event) {
        // Override in subclasses
    }
    
    /**
     * Check packet-related violations
     * Override this method in packet checks
     */
    public void checkPacket(Player player, PlayerData data, Object packet) {
        // Override in subclasses
    }
    
    /**
     * Called when this check detects a violation
     */
    protected void flag(Player player, PlayerData data, String details) {
        if (!enabled) return;
        
        int violations = data.addViolation(name);
        
        // Send to violation manager for processing
        plugin.getViolationManager().handleViolation(player, data, this, violations, details);
        
        // Debug logging
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(String.format("Check %s flagged %s: %s (violations: %d)", 
                name, player.getName(), details, violations));
        }
    }
    
    /**
     * Called when this check detects a violation with custom violation count
     */
    protected void flag(Player player, PlayerData data, String details, int customViolations) {
        if (!enabled) return;
        
        // Add custom violations
        for (int i = 0; i < customViolations; i++) {
            data.addViolation(name);
        }
        
        int totalViolations = data.getViolations(name);
        
        // Send to violation manager for processing
        plugin.getViolationManager().handleViolation(player, data, this, totalViolations, details);
        
        // Debug logging
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(String.format("Check %s flagged %s: %s (violations: %d, added: %d)", 
                name, player.getName(), details, totalViolations, customViolations));
        }
    }
    
    /**
     * Reset violations for a player
     */
    protected void resetViolations(Player player, PlayerData data) {
        data.resetViolations(name);
    }
    
    /**
     * Check if the player should be exempt from this check
     */
    protected boolean isExempt(Player player, PlayerData data) {
        return data.isExempt() || !enabled;
    }
    
    /**
     * Utility method to check if player is lagging
     */
    protected boolean isPlayerLagging(PlayerData data) {
        return data.isLagging() || data.getPing() > plugin.getConfigManager().getMaxPing();
    }
    
    /**
     * Utility method to check if server is lagging
     */
    protected boolean isServerLagging(PlayerData data) {
        return data.getServerTPS() < 18.0;
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getMaxViolations() {
        return maxViolations;
    }
    
    public boolean isMovementCheck() {
        return movementCheck;
    }
    
    public boolean isCombatCheck() {
        return combatCheck;
    }
    
    public boolean isInteractionCheck() {
        return interactionCheck;
    }
    
    public boolean isPacketCheck() {
        return packetCheck;
    }
    
    // Setters (for runtime configuration)
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setMaxViolations(int maxViolations) {
        this.maxViolations = maxViolations;
    }
    
    @Override
    public String toString() {
        return "Check{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                ", maxViolations=" + maxViolations +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Check)) return false;
        Check check = (Check) obj;
        return name.equals(check.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
