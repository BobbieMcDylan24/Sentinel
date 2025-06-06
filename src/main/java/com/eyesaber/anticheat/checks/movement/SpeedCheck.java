package com.eyesaber.anticheat.checks.movement;

import com.eyesaber.anticheat.AntiCheatPlugin;
import com.eyesaber.anticheat.checks.Check;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeedCheck extends Check {
    
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();
    private final Map<UUID, Vector> lastLocation = new HashMap<>();
    private final Map<UUID, Integer> speedViolations = new HashMap<>();
    
    public SpeedCheck(AntiCheatPlugin plugin) {
        super(plugin, "speed");
    }
    
    public void check(Player player, PlayerMoveEvent event) {
        if (!isEnabled() || player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        
        // Skip if player has bypass permission
        if (player.hasPermission("anticheat.bypass") || player.hasPermission("anticheat.bypass.speed")) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Vector currentPos = event.getTo().toVector();
        Vector fromPos = event.getFrom().toVector();
        
        // Skip if teleporting or first move
        if (!lastMoveTime.containsKey(uuid) || !lastLocation.containsKey(uuid)) {
            lastMoveTime.put(uuid, currentTime);
            lastLocation.put(uuid, currentPos);
            return;
        }
        
        long timeDiff = currentTime - lastMoveTime.get(uuid);
        
        // Skip if time difference is too small or too large (lag)
        if (timeDiff < 50 || timeDiff > 500) {
            lastMoveTime.put(uuid, currentTime);
            lastLocation.put(uuid, currentPos);
            return;
        }
        
        // Calculate horizontal distance only
        Vector lastPos = lastLocation.get(uuid);
        double deltaX = currentPos.getX() - lastPos.getX();
        double deltaZ = currentPos.getZ() - lastPos.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        
        // Calculate speed (blocks per second)
        double speed = (horizontalDistance * 1000.0) / timeDiff;
        
        // Get maximum allowed speed
        double maxSpeed = getMaxAllowedSpeed(player);
        
        // Vulcan-style threshold
        double threshold = maxSpeed * 1.2; // 20% tolerance like Vulcan
        
        if (speed > threshold) {
            int violations = speedViolations.getOrDefault(uuid, 0) + 1;
            speedViolations.put(uuid, violations);
            
            // Vulcan kicks at 15 violations for speed
            int maxViolations = plugin.getCheckManager().getMaxViolations("speed");
            
            if (violations >= maxViolations) {
                flag(player, String.format("Speed (%.2f > %.2f)", speed, maxSpeed));
                speedViolations.put(uuid, 0); // Reset after punishment
            } else if (violations % 5 == 0) {
                // Send alert every 5 violations
                flag(player, String.format("Speed (%.2f > %.2f)", speed, maxSpeed));
            }
        } else {
            // Reduce violations if player is moving normally
            int violations = speedViolations.getOrDefault(uuid, 0);
            if (violations > 0) {
                speedViolations.put(uuid, Math.max(0, violations - 1));
            }
        }
        
        lastMoveTime.put(uuid, currentTime);
        lastLocation.put(uuid, currentPos);
    }
    
    private double getMaxAllowedSpeed(Player player) {
        double baseSpeed = 4.3; // Base walking speed in blocks/second
        
        // Check for speed effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.SPEED)) {
                baseSpeed += (effect.getAmplifier() + 1) * 1.2;
            }
        }
        
        // Check if player is sprinting
        if (player.isSprinting()) {
            baseSpeed *= 1.3;
        }
        
        // Check ground material for ice, etc.
        Block groundBlock = player.getLocation().subtract(0, 1, 0).getBlock();
        Material groundMaterial = groundBlock.getType();
        
        if (groundMaterial.name().contains("ICE")) {
            baseSpeed *= 2.5; // Ice multiplier like Vulcan
        }
        
        return baseSpeed;
    }
    
    public void clearViolations(Player player) {
        UUID uuid = player.getUniqueId();
        speedViolations.remove(uuid);
        lastMoveTime.remove(uuid);
        lastLocation.remove(uuid);
    }
}