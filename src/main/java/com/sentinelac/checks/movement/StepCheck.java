package com.sentinelac.checks.movement;

import com.sentinelac.SentinelAC;
import com.sentinelac.checks.Check;
import com.sentinelac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class StepCheck extends Check {

    private static final double MAX_STEP_HEIGHT = 0.6;
    private static final double MAX_JUMP_HEIGHT = 1.25;
    private static final double STEP_BUFFER_THRESHOLD = 1.0;
    private static final int STEP_VIOLATIONS_THRESHOLD = 5;
    
    public StepCheck(SentinelAC plugin) {
        super(plugin, "Step", "Detects step and jump hacks");
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

        // Check for step hacks
        checkStepHeight(player, data, current, last);
        
        // Check for jump hacks
        checkJumpHeight(player, data, current, last);
        
        // Check for impossible step patterns
        checkStepPattern(player, data, current, last);
        
        // Decay step buffer over time
        decayStepBuffer(data);
    }

    private void checkStepHeight(Player player, PlayerData data, Location current, Location last) {
        double verticalDistance = current.getY() - last.getY();
        
        // Only check upward movement
        if (verticalDistance <= 0) {
            return;
        }
        
        // Check if player is on ground (step requires ground contact)
        if (!isPlayerGrounded(player, last)) {
            return;
        }
        
        // Calculate maximum allowed step height
        double maxStepHeight = calculateMaxStepHeight(player, current);
        
        // Get step buffer
        Double stepBuffer = data.getCheckData("step.buffer", Double.class);
        if (stepBuffer == null) stepBuffer = 0.0;
        
        if (verticalDistance > maxStepHeight && verticalDistance < MAX_JUMP_HEIGHT) {
            // This looks like a step hack
            double excess = verticalDistance - maxStepHeight;
            stepBuffer += excess;
            
            if (stepBuffer > STEP_BUFFER_THRESHOLD) {
                flag(player, data, String.format("Step hack detected (height: %.3f, max: %.3f, buffer: %.3f)", 
                    verticalDistance, maxStepHeight, stepBuffer));
                stepBuffer *= 0.7; // Reduce buffer on flag
            }
        } else {
            // Reduce buffer when player moves legitimately
            stepBuffer = Math.max(0, stepBuffer - 0.02);
        }
        
        data.setCheckData("step.buffer", stepBuffer);
    }

    private void checkJumpHeight(Player player, PlayerData data, Location current, Location last) {
        double verticalDistance = current.getY() - last.getY();
        
        // Only check significant upward movement
        if (verticalDistance < 0.5) {
            return;
        }
        
        // Calculate maximum allowed jump height
        double maxJumpHeight = calculateMaxJumpHeight(player, current);
        
        if (verticalDistance > maxJumpHeight) {
            Integer jumpViolations = data.getCheckData("step.jumpViolations", Integer.class);
            if (jumpViolations == null) jumpViolations = 0;
            
            jumpViolations++;
            data.setCheckData("step.jumpViolations", jumpViolations);
            
            if (jumpViolations > STEP_VIOLATIONS_THRESHOLD) {
                flag(player, data, String.format("Jump hack detected (height: %.3f, max: %.3f, violations: %d)", 
                    verticalDistance, maxJumpHeight, jumpViolations));
                jumpViolations = 0; // Reset to prevent spam
            }
            
            data.setCheckData("step.jumpViolations", jumpViolations);
        } else {
            // Reset violations on legitimate jump
            data.setCheckData("step.jumpViolations", 0);
        }
    }

    private void checkStepPattern(Player player, PlayerData data, Location current, Location last) {
        double verticalDistance = current.getY() - last.getY();
        
        if (verticalDistance > 0.1 && verticalDistance < 2.0) {
            // Track recent upward movements
            Long lastStepTime = data.getCheckData("step.lastStepTime", Long.class);
            if (lastStepTime == null) lastStepTime = 0L;
            
            long currentTime = System.currentTimeMillis();
            long timeSinceLastStep = currentTime - lastStepTime;
            
            // Check for rapid stepping
            if (timeSinceLastStep < 200) { // Less than 200ms between steps
                Integer rapidSteps = data.getCheckData("step.rapidSteps", Integer.class);
                if (rapidSteps == null) rapidSteps = 0;
                
                rapidSteps++;
                data.setCheckData("step.rapidSteps", rapidSteps);
                
                if (rapidSteps > 3) {
                    flag(player, data, String.format("Rapid step pattern (steps: %d, interval: %dms)", 
                        rapidSteps, timeSinceLastStep));
                    rapidSteps = 0;
                }
                
                data.setCheckData("step.rapidSteps", rapidSteps);
            } else {
                data.setCheckData("step.rapidSteps", 0);
            }
            
            data.setCheckData("step.lastStepTime", currentTime);
        }
    }

    private double calculateMaxStepHeight(Player player, Location location) {
        double baseStepHeight = MAX_STEP_HEIGHT;
        
        // Apply jump boost effect
        if (player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
            int amplifier = player.getPotionEffect(PotionEffectType.JUMP_BOOST).getAmplifier();
            baseStepHeight += (amplifier + 1) * 0.1; // Small increase for step height
        }
        
        // Check for blocks that allow higher stepping
        Material blockBelow = location.clone().subtract(0, 1, 0).getBlock().getType();
        
        // Slime blocks allow slightly higher steps
        if (blockBelow == Material.SLIME_BLOCK) {
            baseStepHeight += 0.2;
        }
        
        // Soul sand is lower, so effective step height is higher
        if (blockBelow == Material.SOUL_SAND || blockBelow == Material.SOUL_SOIL) {
            baseStepHeight += 0.125; // Soul sand is 0.125 blocks lower
        }
        
        return baseStepHeight;
    }

    private double calculateMaxJumpHeight(Player player, Location location) {
        double baseJumpHeight = MAX_JUMP_HEIGHT;
        
        // Apply jump boost effect
        if (player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
            int amplifier = player.getPotionEffect(PotionEffectType.JUMP_BOOST).getAmplifier();
            baseJumpHeight += (amplifier + 1) * 0.5;
        }
        
        // Check for blocks that affect jump height
        Material blockBelow = location.clone().subtract(0, 1, 0).getBlock().getType();
        
        // Slime blocks increase jump height significantly
        if (blockBelow == Material.SLIME_BLOCK) {
            baseJumpHeight *= 2.0;
        }
        
        // Soul sand reduces jump height slightly
        if (blockBelow == Material.SOUL_SAND || blockBelow == Material.SOUL_SOIL) {
            baseJumpHeight *= 0.9;
        }
        
        // Honey blocks reduce jump height
        if (blockBelow == Material.HONEY_BLOCK) {
            baseJumpHeight *= 0.8;
        }
        
        return baseJumpHeight;
    }

    private boolean isPlayerGrounded(Player player, Location location) {
        // Check multiple points around the player's feet for ground contact
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
            
            // Check for climbable blocks
            if (material == Material.LADDER || material == Material.VINE) {
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
               material == Material.LEGACY_GRASS_PATH ||
               material.name().contains("FENCE") ||
               material.name().contains("WALL") ||
               material == Material.COBBLESTONE_WALL;
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
        
        // Ignore players with levitation effect
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            return true;
        }
        
        // Ignore players in water or lava
        Location loc = player.getLocation();
        Material blockType = loc.getBlock().getType();
        if (blockType == Material.WATER || blockType == Material.LAVA) {
            return true;
        }
        
        // Ignore players near climbable blocks
        if (isNearClimbable(loc)) {
            return true;
        }
        
        // Ignore if player teleported recently
        if (data.getDistanceMoved() > 10) {
            return true;
        }
        
        return false;
    }

    private boolean isNearClimbable(Location location) {
        // Check surrounding blocks for climbable materials
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 2; y++) { // Check up to 2 blocks above
                for (int z = -1; z <= 1; z++) {
                    Location check = location.clone().add(x, y, z);
                    Material material = check.getBlock().getType();
                    
                    if (material == Material.LADDER || 
                        material == Material.VINE ||
                        material.name().contains("SCAFFOLDING")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void decayStepBuffer(PlayerData data) {
        Long lastBufferDecay = data.getCheckData("step.lastBufferDecay", Long.class);
        if (lastBufferDecay == null) {
            lastBufferDecay = System.currentTimeMillis();
            data.setCheckData("step.lastBufferDecay", lastBufferDecay);
            return;
        }
        
        long timeSinceDecay = System.currentTimeMillis() - lastBufferDecay;
        
        // Decay buffer every 500ms
        if (timeSinceDecay >= 500) {
            Double stepBuffer = data.getCheckData("step.buffer", Double.class);
            
            if (stepBuffer != null && stepBuffer > 0) {
                stepBuffer = Math.max(0, stepBuffer - 0.05);
                data.setCheckData("step.buffer", stepBuffer);
            }
            
            data.setCheckData("step.lastBufferDecay", System.currentTimeMillis());
        }
    }
}
