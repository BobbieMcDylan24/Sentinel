package com.eyesaber.anticheat.checks.combat;

import com.eyesaber.anticheat.AntiCheatPlugin;
import com.eyesaber.anticheat.checks.Check;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillAuraCheck extends Check {
    
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private final Map<UUID, Integer> attackCount = new HashMap<>();
    private final Map<UUID, Float> lastYaw = new HashMap<>();
    private final Map<UUID, Float> lastPitch = new HashMap<>();
    private final Map<UUID, Integer> killAuraViolations = new HashMap<>();
    private final Map<UUID, Long> consistentAttackStart = new HashMap<>();
    
    public KillAuraCheck(AntiCheatPlugin plugin) {
        super(plugin, "killaura");
    }
    
    public void check(Player player, EntityDamageByEntityEvent event) {
        if (!isEnabled()) {
            return;
        }
        
        // Skip if player has bypass permission
        if (player.hasPermission("anticheat.bypass") || player.hasPermission("anticheat.bypass.killaura")) {
            return;
        }
        
        // Only check attacks on living entities
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Entity target = event.getEntity();
        
        // Check 1: Attack speed (CPS check)
        checkAttackSpeed(player, uuid, currentTime);
        
        // Check 2: Head movement patterns
        checkHeadMovement(player, uuid, target);
        
        // Check 3: Attack angle consistency
        checkAttackAngle(player, uuid, target);
        
        // Check 4: Multi-target detection
        checkMultiTarget(player, uuid, currentTime);
        
        lastAttackTime.put(uuid, currentTime);
    }
    
    private void checkAttackSpeed(Player player, UUID uuid, long currentTime) {
        Long lastTime = lastAttackTime.get(uuid);
        if (lastTime != null) {
            long timeDiff = currentTime - lastTime;
            
            // Minecraft has a 0.5 second attack cooldown (500ms)
            // But allow some tolerance for lag
            if (timeDiff < 400) { // Less than 400ms = too fast
                int violations = killAuraViolations.getOrDefault(uuid, 0) + 1;
                killAuraViolations.put(uuid, violations);
                
                if (violations >= 3) {
                    flag(player, String.format("KillAura (Attack Speed: %dms)", timeDiff));
                }
            }
        }
    }
    
    private void checkHeadMovement(Player player, UUID uuid, Entity target) {
        float currentYaw = player.getLocation().getYaw();
        float currentPitch = player.getLocation().getPitch();
        
        Float lastYawValue = lastYaw.get(uuid);
        Float lastPitchValue = lastPitch.get(uuid);
        
        if (lastYawValue != null && lastPitchValue != null) {
            float yawDiff = Math.abs(currentYaw - lastYawValue);
            float pitchDiff = Math.abs(currentPitch - lastPitchValue);
            
            // Normalize yaw difference (handle 360° wrap-around)
            if (yawDiff > 180) {
                yawDiff = 360 - yawDiff;
            }
            
            // Check for robotic/perfect movements
            if (yawDiff < 0.1f && pitchDiff < 0.1f) {
                // Very small head movement while attacking = suspicious
                int violations = killAuraViolations.getOrDefault(uuid, 0) + 1;
                killAuraViolations.put(uuid, violations);
                
                if (violations >= 4) {
                    flag(player, String.format("KillAura (Robotic Movement: Y%.2f P%.2f)", yawDiff, pitchDiff));
                }
            }
            
            // Check for impossible head snaps
            if (yawDiff > 90 || pitchDiff > 45) {
                int violations = killAuraViolations.getOrDefault(uuid, 0) + 2; // More severe
                killAuraViolations.put(uuid, violations);
                
                if (violations >= 3) {
                    flag(player, String.format("KillAura (Head Snap: Y%.2f P%.2f)", yawDiff, pitchDiff));
                }
            }
        }
        
        lastYaw.put(uuid, currentYaw);
        lastPitch.put(uuid, currentPitch);
    }
    
    private void checkAttackAngle(Player player, UUID uuid, Entity target) {
        Location playerLoc = player.getEyeLocation();
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        
        // Calculate where player should be looking to hit target
        Vector toTarget = targetLoc.subtract(playerLoc).toVector().normalize();
        Vector playerDirection = playerLoc.getDirection().normalize();
        
        // Calculate angle between where player is looking and where target is
        double dotProduct = playerDirection.dot(toTarget);
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dotProduct))));
        
        // Get max allowed angle from config
        double maxAngle = plugin.getConfigManager().getConfig()
            .getDouble("anticheat.checks.killaura.max-angle", 90.0);
        
        if (angle > maxAngle) {
            int violations = killAuraViolations.getOrDefault(uuid, 0) + 1;
            killAuraViolations.put(uuid, violations);
            
            if (violations >= 2) {
                flag(player, String.format("KillAura (Attack Angle: %.1f° > %.1f°)", angle, maxAngle));
            }
        }
    }
    
    private void checkMultiTarget(Player player, UUID uuid, long currentTime) {
        int count = attackCount.getOrDefault(uuid, 0) + 1;
        attackCount.put(uuid, count);
        
        Long startTime = consistentAttackStart.get(uuid);
        if (startTime == null) {
            consistentAttackStart.put(uuid, currentTime);
            return;
        }
        
        long duration = currentTime - startTime;
        
        // Reset counter every 3 seconds
        if (duration > 3000) {
            attackCount.put(uuid, 1);
            consistentAttackStart.put(uuid, currentTime);
            return;
        }
        
        // If attacking very frequently (more than 8 times in 3 seconds)
        if (count > 8 && duration < 3000) {
            int violations = killAuraViolations.getOrDefault(uuid, 0) + 1;
            killAuraViolations.put(uuid, violations);
            
            if (violations >= 3) {
                flag(player, String.format("KillAura (Multi-Target: %d attacks in %dms)", count, duration));
            }
        }
    }
    
    public void clearViolations(Player player) {
        UUID uuid = player.getUniqueId();
        killAuraViolations.remove(uuid);
        lastAttackTime.remove(uuid);
        attackCount.remove(uuid);
        lastYaw.remove(uuid);
        lastPitch.remove(uuid);
        consistentAttackStart.remove(uuid);
    }
}