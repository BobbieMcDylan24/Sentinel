package com.eyesaber.anticheat.managers;

import com.eyesaber.anticheat.AntiCheatPlugin;
import com.eyesaber.anticheat.checks.combat.KillAuraCheck;
import com.eyesaber.anticheat.checks.combat.ReachCheck;
import com.eyesaber.anticheat.checks.movement.FlyCheck;
import com.eyesaber.anticheat.checks.movement.NoFallCheck;
import com.eyesaber.anticheat.checks.movement.SpeedCheck;
import com.eyesaber.anticheat.checks.block.FastBreakCheck;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class CheckManager {
    
    private final AntiCheatPlugin plugin;
    
    // Check instances
    private final FlyCheck flyCheck;
    private final SpeedCheck speedCheck;
    private final NoFallCheck noFallCheck;
    private final KillAuraCheck killAuraCheck;
    private final ReachCheck reachCheck;
    private final FastBreakCheck fastBreakCheck;
    
    public CheckManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        
        // Initialize checks
        this.flyCheck = new FlyCheck(plugin);
        this.speedCheck = new SpeedCheck(plugin);
        this.noFallCheck = new NoFallCheck(plugin);
        this.killAuraCheck = new KillAuraCheck(plugin);
        this.reachCheck = new ReachCheck(plugin);
        this.fastBreakCheck = new FastBreakCheck(plugin);
    }
    
    // Getters for checks
    public FlyCheck getFlyCheck() { return flyCheck; }
    public SpeedCheck getSpeedCheck() { return speedCheck; }
    public NoFallCheck getNoFallCheck() { return noFallCheck; }
    public KillAuraCheck getKillAuraCheck() { return killAuraCheck; }
    public ReachCheck getReachCheck() { return reachCheck; }
    public FastBreakCheck getFastBreakCheck() { return fastBreakCheck; }
    
    public void executePunishment(Player player, String checkName) {
        plugin.getLogger().info("Executing punishment for " + player.getName() + " (check: " + checkName + ")");
        
        List<String> punishments = plugin.getConfigManager().getConfig()
            .getStringList("punishments." + checkName);
        
        if (punishments.isEmpty()) {
            plugin.getLogger().warning("No punishments configured for check: " + checkName);
            return;
        }
        
        for (String punishment : punishments) {
            String processedPunishment = punishment
                .replace("%player%", player.getName())
                .replace("%ping%", String.valueOf(getPing(player)));
            
            plugin.getLogger().info("Executing punishment command: " + processedPunishment);
            
            // Execute the punishment command
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedPunishment);
            });
        }
        
        // Reset violations after punishment
        plugin.getViolationManager().clearViolations(player, checkName);
    }
    
    private int getPing(Player player) {
        try {
            return player.getPing();
        } catch (Exception e) {
            return 0;
        }
    }
    
    public void clearPlayerViolations(Player player) {
        plugin.getViolationManager().clearViolations(player);
        plugin.getLogger().info("Cleared all violations for " + player.getName());
    }
    
    public int getMaxViolations(String checkName) {
        return plugin.getConfigManager().getConfig()
            .getInt("anticheat.checks." + checkName + ".max-violations", 10);
    }
    
    public String getCheckStatistics() {
        return "Check statistics feature coming soon!";
    }
}