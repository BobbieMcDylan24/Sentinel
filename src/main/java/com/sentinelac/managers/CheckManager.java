package com.sentinelac.managers;

import com.sentinelac.SentinelAC;
import com.sentinelac.checks.Check;
import com.sentinelac.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public class CheckManager {

    private final SentinelAC plugin;
    private final CopyOnWriteArrayList<Check> registeredChecks = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Check> checksByName = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    public CheckManager(SentinelAC plugin) {
        this.plugin = plugin;
        this.executorService = plugin.getExecutorService();
        initialize();
    }
    
    private void initialize() {
        plugin.getLogger().info("Initializing check manager...");
        
        // Register all checks here
        registerChecks();
        
        plugin.getLogger().info("Check manager initialized with " + registeredChecks.size() + " checks");
    }
    
    private void registerChecks() {
        // Register movement checks
        registerCheck(new com.sentinelac.checks.movement.SpeedCheck(plugin));
        registerCheck(new com.sentinelac.checks.movement.FlightCheck(plugin));
        registerCheck(new com.sentinelac.checks.movement.NoFallCheck(plugin));
        registerCheck(new com.sentinelac.checks.movement.StepCheck(plugin));
        registerCheck(new com.sentinelac.checks.movement.MotionCheck(plugin));
        
        // TODO: Register combat checks
        // registerCheck(new KillAuraCheck(plugin));
        // registerCheck(new ReachCheck(plugin));
        // registerCheck(new AutoClickerCheck(plugin));
        
        // TODO: Register interaction checks
        // registerCheck(new FastBreakCheck(plugin));
        // registerCheck(new FastPlaceCheck(plugin));
        // registerCheck(new ScaffoldCheck(plugin));
        
        plugin.getLogger().info("Registered " + registeredChecks.size() + " checks");
    }
    
    public void registerCheck(Check check) {
        if (check == null) {
            plugin.getLogger().warning("Attempted to register null check");
            return;
        }
        
        String checkName = check.getName().toLowerCase();
        
        if (checksByName.containsKey(checkName)) {
            plugin.getLogger().warning("Check with name '" + checkName + "' is already registered");
            return;
        }
        
        registeredChecks.add(check);
        checksByName.put(checkName, check);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Registered check: " + check.getName());
        }
    }
    
    public void unregisterCheck(String checkName) {
        if (checkName == null) return;
        
        Check check = checksByName.remove(checkName.toLowerCase());
        if (check != null) {
            registeredChecks.remove(check);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Unregistered check: " + check.getName());
            }
        }
    }
    
    public Check getCheck(String checkName) {
        if (checkName == null) return null;
        return checksByName.get(checkName.toLowerCase());
    }
    
    public boolean isCheckRegistered(String checkName) {
        if (checkName == null) return false;
        return checksByName.containsKey(checkName.toLowerCase());
    }
    
    public void runMovementChecks(Player player, PlayerData data) {
        if (shouldSkipChecks(player, data)) {
            return;
        }
        
        for (Check check : registeredChecks) {
            if (!check.isMovementCheck() || !isCheckEnabled(check)) {
                continue;
            }
            
            if (plugin.getConfigManager().isAsyncChecksEnabled()) {
                runCheckAsync(check, player, data, "movement");
            } else {
                runCheckSync(check, player, data, "movement");
            }
        }
    }
    
    public void runCombatChecks(Player player, PlayerData data, Object event) {
        if (shouldSkipChecks(player, data)) {
            return;
        }
        
        for (Check check : registeredChecks) {
            if (!check.isCombatCheck() || !isCheckEnabled(check)) {
                continue;
            }
            
            if (plugin.getConfigManager().isAsyncChecksEnabled()) {
                runCheckAsync(check, player, data, "combat", event);
            } else {
                runCheckSync(check, player, data, "combat", event);
            }
        }
    }
    
    public void runInteractionChecks(Player player, PlayerData data, Object event) {
        if (shouldSkipChecks(player, data)) {
            return;
        }
        
        for (Check check : registeredChecks) {
            if (!check.isInteractionCheck() || !isCheckEnabled(check)) {
                continue;
            }
            
            if (plugin.getConfigManager().isAsyncChecksEnabled()) {
                runCheckAsync(check, player, data, "interaction", event);
            } else {
                runCheckSync(check, player, data, "interaction", event);
            }
        }
    }
    
    public void runPacketChecks(Player player, PlayerData data, Object packet) {
        if (shouldSkipChecks(player, data)) {
            return;
        }
        
        for (Check check : registeredChecks) {
            if (!check.isPacketCheck() || !isCheckEnabled(check)) {
                continue;
            }
            
            if (plugin.getConfigManager().isAsyncChecksEnabled()) {
                runCheckAsync(check, player, data, "packet", packet);
            } else {
                runCheckSync(check, player, data, "packet", packet);
            }
        }
    }
    
    private void runCheckSync(Check check, Player player, PlayerData data, String type, Object... args) {
        try {
            switch (type) {
                case "movement":
                    if (check.isMovementCheck()) {
                        check.checkMovement(player, data);
                    }
                    break;
                case "combat":
                    if (check.isCombatCheck() && args.length > 0) {
                        check.checkCombat(player, data, args[0]);
                    }
                    break;
                case "interaction":
                    if (check.isInteractionCheck() && args.length > 0) {
                        check.checkInteraction(player, data, args[0]);
                    }
                    break;
                case "packet":
                    if (check.isPacketCheck() && args.length > 0) {
                        check.checkPacket(player, data, args[0]);
                    }
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error running check " + check.getName() + " for player " + player.getName(), e);
        }
    }
    
    private void runCheckAsync(Check check, Player player, PlayerData data, String type, Object... args) {
        executorService.submit(() -> runCheckSync(check, player, data, type, args));
    }
    
    private boolean shouldSkipChecks(Player player, PlayerData data) {
        // Skip if player is null or offline
        if (player == null || !player.isOnline()) {
            return true;
        }
        
        // Skip if player data is null
        if (data == null) {
            return true;
        }
        
        // Skip if player is exempt
        if (data.isExempt()) {
            return true;
        }
        
        // Skip if player has high ping
        if (data.getPing() > plugin.getConfigManager().getMaxPing()) {
            return true;
        }
        
        // Skip if server is lagging
        if (data.getServerTPS() < 18.0) {
            return true;
        }
        
        return false;
    }
    
    private boolean isCheckEnabled(Check check) {
        return plugin.getConfigManager().isCheckEnabled(check.getName());
    }
    
    public void reloadChecks() {
        plugin.getLogger().info("Reloading check configurations...");
        
        for (Check check : registeredChecks) {
            try {
                check.reload();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error reloading check " + check.getName(), e);
            }
        }
        
        plugin.getLogger().info("Check configurations reloaded");
    }
    
    public void printCheckStats() {
        plugin.getLogger().info("=== Check Statistics ===");
        plugin.getLogger().info("Total registered checks: " + registeredChecks.size());
        
        int enabledChecks = 0;
        for (Check check : registeredChecks) {
            if (isCheckEnabled(check)) {
                enabledChecks++;
            }
        }
        
        plugin.getLogger().info("Enabled checks: " + enabledChecks);
        plugin.getLogger().info("Disabled checks: " + (registeredChecks.size() - enabledChecks));
        plugin.getLogger().info("======================");
    }
    
    public void shutdown() {
        plugin.getLogger().info("Shutting down check manager...");
        
        // Clear all checks
        registeredChecks.clear();
        checksByName.clear();
        
        plugin.getLogger().info("Check manager shutdown complete");
    }
    
    // Getters
    public int getRegisteredCheckCount() {
        return registeredChecks.size();
    }
    
    public CopyOnWriteArrayList<Check> getRegisteredChecks() {
        return new CopyOnWriteArrayList<>(registeredChecks);
    }
    
    public boolean hasMovementChecks() {
        return registeredChecks.stream().anyMatch(Check::isMovementCheck);
    }
    
    public boolean hasCombatChecks() {
        return registeredChecks.stream().anyMatch(Check::isCombatCheck);
    }
    
    public boolean hasInteractionChecks() {
        return registeredChecks.stream().anyMatch(Check::isInteractionCheck);
    }
    
    public boolean hasPacketChecks() {
        return registeredChecks.stream().anyMatch(Check::isPacketCheck);
    }
}
