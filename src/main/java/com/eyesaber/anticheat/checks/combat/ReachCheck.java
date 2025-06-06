package com.eyesaber.anticheat.checks.combat;

import com.eyesaber.anticheat.AntiCheatPlugin;
import com.eyesaber.anticheat.checks.Check;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ReachCheck extends Check {
    
    public ReachCheck(AntiCheatPlugin plugin) {
        super(plugin, "reach");
    }
    
    public void check(Player player, EntityDamageByEntityEvent event) {
        if (!isEnabled()) {
            return;
        }
        
        Entity target = event.getEntity();
        double distance = player.getLocation().distance(target.getLocation());
        
        double maxDistance = plugin.getConfigManager().getConfig()
            .getDouble("anticheat.checks.reach.max-distance", 4.2);
        
        if (distance > maxDistance) {
            flag(player, "Reach detected (Distance: " + String.format("%.2f", distance) + 
                 ", Max: " + String.format("%.2f", maxDistance) + ")");
        }
    }
}