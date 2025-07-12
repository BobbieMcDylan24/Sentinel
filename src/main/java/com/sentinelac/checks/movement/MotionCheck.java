package com.sentinelac.checks.movement;

import com.sentinelac.SentinelAC;
import com.sentinelac.checks.Check;
import com.sentinelac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class MotionCheck extends Check {

    private static final double MAX_MOTION_CHANGE = 2.0;
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;
    private static final double MIN_MOTION_THRESHOLD = 0.005;
    private static final int MOTION_BUFFER_SIZE = 20;
    
    public MotionCheck(SentinelAC plugin) {
        super(plugin, "Motion", "Detects impossible motion changes and physics violations");
        this.movementCheck = true;
    }

    @Override
    public void checkMovement(Player player, PlayerData data) {
        if (isExempt(player, data) || shouldIgnorePlayer(player, data)) {
            return;
        }

        Location current = data.getCurrentLocation();
        Location last = data.getLastLocation();
        
        if (current == null || last == null) {
            return;
        }

        // Calculate current motion
        Vector currentMotion = calculateMotion(current, last);
        
        // Predict motion based on physics
        Vector predictedMotion = predictMotion(player, data, currentMotion);
        
        // Check for motion violations
        checkMotionChange(player, data, currentMotion, predictedMotion);
        checkPhysicsViolation(player, data, currentMotion);
        checkMotionPattern(player, data, currentMotion);
        
        // Update motion history
        updateMotionHistory(data, currentMotion);
    }

    private Vector calculateMotion(Location current, Location last) {
        double deltaX = current.getX() - last.getX();
        double deltaY = current.getY() - last.getY();
        double deltaZ = current.getZ() - last.getZ();
        
        return new Vector(deltaX, deltaY, deltaZ);
    }

    private Vector predictMotion(Player player, PlayerData data, Vector currentMotion) {
        Vector lastMotion = data.getCheckData("motion.lastMotion", Vector.class);
        if (lastMotion == null) {
            data.setCheckData("motion.lastMotion", currentMotion.clone());
            return currentMotion.clone();
        }
        
        // Start with last motion
        Vector predicted = lastMotion.clone();
        
        // Apply gravity if player is airborne
        if (!isPlayerGrounded(player, player.getLocation())) {
            predicted.setY(predicted.getY() - GRAVITY);
        }
        
        // Apply drag
        predicted.multiply(DRAG);
        
        // Account for external forces
        applyExternalForces(player, predicted);
        
        // Store for next prediction
        data.setCheckData("motion.lastMotion", currentMotion.clone());
        
        return predicted;
    }

    private void applyExternalForces(Player player, Vector motion) {
        // Apply knockback resistance if available
        if (player.getInventory().getChestplate() != null) {
            int knockbackResistance = player.getInventory().getChestplate().getEnchantmentLevel(
                org.bukkit.enchantments.Enchantment.KNOCKBACK_RESISTANCE);
            if (knockbackResistance > 0) {
                double resistance = knockbackResistance * 0.1;
                motion.multiply(1.0 - resistance);
            }
        }
        
        // Apply velocity effects
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            motion.setX(motion.getX() * (1.0 + (amplifier + 1) * 0.2));
            motion.setZ(motion.getZ() * (1.0 + (amplifier + 1) * 0.2));
        }
        
        if (player.hasPotionEffect(PotionEffectType.SLOW)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SLOW).getAmplifier();
            motion.setX(motion.getX() * (1.0 - (amplifier + 1) * 0.15));
            motion.setZ(motion.getZ() * (1.0 - (amplifier + 1) * 0.15));
        }
        
        if (player.hasPotionEffect(PotionEffectType.JUMP)) {
            int amplifier = player.getPotionEffect(PotionEffectType.JUMP).getAmplifier();
            if (motion.getY() > 0) {
                motion.setY(motion.getY() + (amplifier + 1) * 0.1);
            }
        }
    }

    private void checkMotionChange(Player player, PlayerData data, Vector currentMotion, Vector predictedMotion) {
        double motionDifference = currentMotion.distance(predictedMotion);
        
        // Get motion buffer
        Double motionBuffer = data.getCheckData("motion.buffer", Double.class);
        if (motionBuffer == null) motionBuffer = 0.0;
        
        if (motionDifference > MAX_MOTION_CHANGE) {
            motionBuffer += motionDifference - MAX_MOTION_CHANGE;
            
            if (motionBuffer > 1.0) {
                flag(player, data, String.format("Impossible motion change (diff: %.3f, buffer: %.3f)", 
                    motionDifference, motionBuffer));
                motionBuffer *= 0.8;
            }
        } else {
            motionBuffer = Math.max(0, motionBuffer - 0.1);
        }
        
        data.setCheckData("motion.buffer", motionBuffer);
    }

    private void checkPhysicsViolation(Player player, PlayerData data, Vector motion) {
        // Check for zero motion when it shouldn't be zero
        if (isMovingHorizontally(motion) && motion.getY() == 0 && !isPlayerGrounded(player, player.getLocation())) {
            Integer zeroMotionCount = data.getCheckData("motion.zeroMotionCount", Integer.class);
            if (zeroMotionCount == null) zeroMotionCount = 0;
            
            zeroMotionCount++;
            data.setCheckData("motion.zeroMotionCount", zeroMotionCount);
            
            if (zeroMotionCount > 5) {
                flag(player, data, String.format("Zero Y motion while airborne (count: %d)", zeroMotionCount));
                zeroMotionCount = 0;
            }
        } else {
            data.setCheckData("motion.zeroMotionCount", 0);
        }
        
        // Check for impossible horizontal motion patterns
        checkHorizontalMotion(player, data, motion);
        
        // Check for impossible vertical motion patterns
        checkVerticalMotion(player, data, motion);
    }

    private void checkHorizontalMotion(Player player, PlayerData data, Vector motion) {
        double horizontalSpeed = Math.sqrt(motion.getX() * motion.getX() + motion.getZ() * motion.getZ());
        
        // Check for sudden horizontal motion changes
        Double lastHorizontalSpeed = data.getCheckData("motion.lastHorizontalSpeed", Double.class);
        if (lastHorizontalSpeed != null) {
            double speedChange = Math.abs(horizontalSpeed - lastHorizontalSpeed);
            
            if (speedChange > 1.5 && !isPlayerGrounded(player, player.getLocation())) {
                Integer suddenChangeCount = data.getCheckData("motion.suddenChangeCount", Integer.class);
                if (suddenChangeCount == null) suddenChangeCount = 0;
                
                suddenChangeCount++;
                data.setCheckData("motion.suddenChangeCount", suddenChangeCount);
                
                if (suddenChangeCount > 3) {
                    flag(player, data, String.format("Sudden horizontal motion change (change: %.3f, count: %d)", 
                        speedChange, suddenChangeCount));
                    suddenChangeCount = 0;
                }
            } else {
                data.setCheckData("motion.suddenChangeCount", 0);
            }
        }
        
        data.setCheckData("motion.lastHorizontalSpeed", horizontalSpeed);
    }

    private void checkVerticalMotion(Player player, PlayerData data, Vector motion) {
        double verticalMotion = motion.getY();
        
        // Check for impossible upward motion
        if (verticalMotion > 0.8 && !isPlayerGrounded(player, player.getLocation())) {
            Double lastVerticalMotion = data.getCheckData("motion.lastVerticalMotion", Double.class);
            
            if (lastVerticalMotion != null && lastVerticalMotion < 0) {
                // Player was falling and suddenly moved up significantly
                flag(player, data, String.format("Impossible upward motion (from: %.3f to: %.3f)", 
                    lastVerticalMotion, verticalMotion));
            }
        }
        
        data.setCheckData("motion.lastVerticalMotion", verticalMotion);
    }

    private void checkMotionPattern(Player player, PlayerData data, Vector motion) {
        // Add motion to history buffer
        @SuppressWarnings("unchecked")
        java.util.List<Vector> motionHistory = data.getCheckData("motion.history", java.util.List.class);
        if (motionHistory == null) {
            motionHistory = new java.util.ArrayList<>();
        }
        
        motionHistory.add(motion.clone());
        
        // Keep only recent motion data
        if (motionHistory.size() > MOTION_BUFFER_SIZE) {
            motionHistory.remove(0);
        }
        
        data.setCheckData("motion.history", motionHistory);
        
        // Check for repeating motion patterns (bot-like behavior)
        if (motionHistory.size() >= 10) {
            checkRepeatingPattern(player, data, motionHistory);
        }
    }

    private void checkRepeatingPattern(Player player, PlayerData data, java.util.List<Vector> motionHistory) {
        int patternLength = 3; // Check for patterns of length 3
        int matches = 0;
        
        for (int i = 0; i < motionHistory.size() - (patternLength * 2); i++) {
            boolean patternMatch = true;
            
            for (int j = 0; j < patternLength; j++) {
                Vector motion1 = motionHistory.get(i + j);
                Vector motion2 = motionHistory.get(i + j + patternLength);
                
                if (motion1.distance(motion2) > 0.01) {
                    patternMatch = false;
                    break;
                }
            }
            
            if (patternMatch) {
                matches++;
            }
        }
        
        if (matches > 2) {
            flag(player, data, String.format("Repeating motion pattern detected (matches: %d)", matches));
        }
    }

    private boolean isMovingHorizontally(Vector motion) {
        double horizontalSpeed = Math.sqrt(motion.getX() * motion.getX() + motion.getZ() * motion.getZ());
        return horizontalSpeed > MIN_MOTION_THRESHOLD;
    }

    private boolean isPlayerGrounded(Player player, Location location) {
        // Check multiple points around the player's feet
        double[][] checkPoints = {
            {-0.3, 0, -0.3}, {0.3, 0, -0.3},
            {-0.3, 0, 0.3}, {0.3, 0, 0.3},
            {0, 0, 0} // Center point
        };
        
        for (double[] point : checkPoints) {
            Location checkLoc = location.clone().add(point[0], -0.1, point[2]);
            Material material = checkLoc.getBlock().getType();
            
            if (material.isSolid() && !material.isTransparent()) {
                return true;
            }
            
            // Check for partial blocks
            if (isPartialBlock(material)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean isPartialBlock(Material material) {
        return material.name().contains("SLAB") ||
               material.name().contains("STAIRS") ||
               material.name().contains("CARPET") ||
               material == Material.SOUL_SAND ||
               material == Material.FARMLAND ||
               material == Material.GRASS_PATH ||
               material.name().contains("FENCE") ||
               material.name().contains("WALL");
    }

    private boolean shouldIgnorePlayer(Player player, PlayerData data) {
        // Ignore lagging players
        if (isPlayerLagging(data) || isServerLagging(data)) {
            return true;
        }
        
        // Ignore creative/spectator players
        if (player.isFlying() || player.getAllowFlight()) {
            return true;
        }
        
        // Ignore players with elytra
        if (player.isGliding()) {
            return true;
        }
        
        // Ignore players in vehicles
        if (player.isInsideVehicle()) {
            return true;
        }
        
        // Ignore players with certain effects that alter motion
        if (player.hasPotionEffect(PotionEffectType.LEVITATION) ||
            player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return true;
        }
        
        // Ignore players using riptide
        if (player.isRiptiding()) {
            return true;
        }
        
        // Ignore if player teleported recently (large distance moved)
        if (data.getDistanceMoved() > 20) {
            return true;
        }
        
        // Ignore players in water or lava (different physics)
        Location loc = player.getLocation();
        Material blockType = loc.getBlock().getType();
        if (blockType == Material.WATER || blockType == Material.LAVA) {
            return true;
        }
        
        return false;
    }

    private void updateMotionHistory(PlayerData data, Vector motion) {
        // Store motion data for future predictions
        data.setCheckData("motion.lastMotion", motion.clone());
        
        // Update time stamps for motion tracking
        data.setCheckData("motion.lastUpdateTime", System.currentTimeMillis());
    }
}
