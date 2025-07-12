package com.sentinelac.managers;

import com.sentinelac.SentinelAC;
import com.sentinelac.checks.Check;
import com.sentinelac.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ViolationManager {

    private final SentinelAC plugin;
    private final ScheduledExecutorService violationResetService;
    private final ConcurrentHashMap<String, Long> lastViolationAlert = new ConcurrentHashMap<>();
    
    // Configuration
    private int maxViolationsBeforeAction;
    private int autoBanThreshold;
    private long violationResetTime;
    private long alertCooldown = 5000; // 5 seconds between alerts for same check

    public ViolationManager(SentinelAC plugin) {
        this.plugin = plugin;
        this.violationResetService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "SentinelAC-ViolationReset");
            thread.setDaemon(true);
            return thread;
        });
        
        initialize();
    }
    
    private void initialize() {
        plugin.getLogger().info("Initializing violation manager...");
        
        loadConfig();
        
        // Start violation reset task - runs every minute
        violationResetService.scheduleAtFixedRate(this::processViolationResets, 60, 60, TimeUnit.SECONDS);
        
        plugin.getLogger().info("Violation manager initialized");
    }
    
    private void loadConfig() {
        this.maxViolationsBeforeAction = plugin.getConfigManager().getMaxViolations();
        this.autoBanThreshold = plugin.getConfigManager().getAutoBanThreshold();
        this.violationResetTime = plugin.getConfigManager().getInt("violations.violation-reset-time") * 1000L; // Convert to ms
    }
    
    public void handleViolation(Player player, PlayerData data, Check check, int violations, String details) {
        if (player == null || data == null || check == null) {
            return;
        }
        
        try {
            // Check if we should alert
            if (shouldAlert(check.getName(), violations)) {
                plugin.getAlertManager().sendAlert(player, check, violations, details);
                updateLastAlert(check.getName());
            }
            
            // Check if we should take action
            if (shouldTakeAction(data, check, violations)) {
                takeAction(player, data, check, violations);
            }
            
            // Check for auto-ban
            if (shouldAutoBan(data)) {
                autoBan(player, data);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling violation for " + player.getName(), e);
        }
    }
    
    private boolean shouldAlert(String checkName, int violations) {
        String alertKey = checkName;
        Long lastAlert = lastViolationAlert.get(alertKey);
        long currentTime = System.currentTimeMillis();
        
        // Always alert on first violation, then respect cooldown
        if (lastAlert == null) {
            return violations == 1;
        }
        
        // Alert if cooldown has passed and violations are significant
        return (currentTime - lastAlert) > alertCooldown && violations % 3 == 0;
    }
    
    private void updateLastAlert(String checkName) {
        lastViolationAlert.put(checkName, System.currentTimeMillis());
    }
    
    private boolean shouldTakeAction(PlayerData data, Check check, int violations) {
        // Take action if violations exceed check-specific threshold
        if (violations >= check.getMaxViolations()) {
            return true;
        }
        
        // Take action if total violations exceed global threshold
        return data.getTotalViolations() >= maxViolationsBeforeAction;
    }
    
    private void takeAction(Player player, PlayerData data, Check check, int violations) {
        String actionType = determineActionType(violations, data.getTotalViolations());
        
        switch (actionType) {
            case "kick":
                kickPlayer(player, check, violations);
                break;
            case "tempban":
                tempBanPlayer(player, check, violations);
                break;
            case "setback":
                setbackPlayer(player, data, check);
                break;
            case "cancel":
                // Action is handled by the check itself (e.g., canceling movement)
                break;
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(String.format("Took action '%s' against %s for check %s (violations: %d)", 
                actionType, player.getName(), check.getName(), violations));
        }
    }
    
    private String determineActionType(int checkViolations, int totalViolations) {
        // Determine action based on violation count
        if (totalViolations >= 20) {
            return "tempban";
        } else if (checkViolations >= 8) {
            return "kick";
        } else if (checkViolations >= 5) {
            return "setback";
        } else {
            return "cancel";
        }
    }
    
    private void kickPlayer(Player player, Check check, int violations) {
        String reason = String.format("§cSentinelAC §7» §fSuspicious activity detected\n§7Check: §f%s\n§7Violations: §f%d", 
            check.getName(), violations);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.kickPlayer(reason);
                plugin.getLogger().info("Kicked " + player.getName() + " for " + check.getName() + " violations (" + violations + ")");
            }
        });
    }
    
    private void tempBanPlayer(Player player, Check check, int violations) {
        // For now, just kick with a stronger message
        // In a full implementation, you'd integrate with a ban plugin
        String reason = String.format("§cSentinelAC §7» §fTemporary ban\n§7Check: §f%s\n§7Violations: §f%d\n§7Duration: §f10 minutes", 
            check.getName(), violations);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.kickPlayer(reason);
                plugin.getLogger().warning("Temp-banned " + player.getName() + " for " + check.getName() + " violations (" + violations + ")");
            }
        });
    }
    
    private void setbackPlayer(Player player, PlayerData data, Check check) {
        // Setback player to their last valid location
        if (data.getLastLocation() != null && check.isMovementCheck()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.teleport(data.getLastLocation());
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Set back " + player.getName() + " for " + check.getName());
                    }
                }
            });
        }
    }
    
    private boolean shouldAutoBan(PlayerData data) {
        return data.getTotalViolations() >= autoBanThreshold;
    }
    
    private void autoBan(Player player, PlayerData data) {
        String reason = String.format("§cSentinelAC §7» §fAutomatic ban\n§7Total violations: §f%d\n§7Threshold: §f%d", 
            data.getTotalViolations(), autoBanThreshold);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.kickPlayer(reason);
                plugin.getLogger().warning("Auto-banned " + player.getName() + " for excessive violations (" + data.getTotalViolations() + ")");
                
                // Send alert to staff about auto-ban
                plugin.getAlertManager().sendStaffMessage("§cSentinelAC §7» §fAuto-banned §c" + player.getName() + 
                    " §7(§f" + data.getTotalViolations() + " §7violations)");
            }
        });
    }
    
    private void processViolationResets() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // Get all online players and check for violation resets
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
                if (data == null) continue;
                
                // Reset violations that are older than the reset time
                // This is a simplified implementation - a full version would track per-check reset times
                if (data.getTotalViolations() > 0) {
                    long timeSinceLastViolation = getTimeSinceLastViolation(data);
                    if (timeSinceLastViolation > violationResetTime) {
                        data.resetAllViolations();
                        
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Reset violations for " + player.getName() + " (inactive for " + 
                                (timeSinceLastViolation / 1000) + " seconds)");
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during violation reset process", e);
        }
    }
    
    private long getTimeSinceLastViolation(PlayerData data) {
        // Get the most recent violation time across all checks
        // This is simplified - in a full implementation you'd track this properly
        return System.currentTimeMillis() - data.getLastSeen();
    }
    
    public void resetPlayerViolations(Player player, String checkName) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            if (checkName == null || checkName.equalsIgnoreCase("all")) {
                data.resetAllViolations();
                plugin.getLogger().info("Reset all violations for " + player.getName());
            } else {
                data.resetViolations(checkName);
                plugin.getLogger().info("Reset " + checkName + " violations for " + player.getName());
            }
        }
    }
    
    public void reload() {
        loadConfig();
        plugin.getLogger().info("Violation manager configuration reloaded");
    }
    
    public void shutdown() {
        plugin.getLogger().info("Shutting down violation manager...");
        
        violationResetService.shutdown();
        try {
            if (!violationResetService.awaitTermination(5, TimeUnit.SECONDS)) {
                violationResetService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            violationResetService.shutdownNow();
        }
        
        lastViolationAlert.clear();
        
        plugin.getLogger().info("Violation manager shutdown complete");
    }
    
    // Getters
    public int getMaxViolationsBeforeAction() {
        return maxViolationsBeforeAction;
    }
    
    public int getAutoBanThreshold() {
        return autoBanThreshold;
    }
    
    public long getViolationResetTime() {
        return violationResetTime;
    }
}
