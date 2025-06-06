package com.eyesaber.anticheat.checks.inventory;

import com.eyesaber.anticheat.AntiCheatPlugin;
import com.eyesaber.anticheat.checks.Check;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryCheck extends Check {
    
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Integer> clickCount = new HashMap<>();
    
    public InventoryCheck(AntiCheatPlugin plugin) {
        super(plugin, "inventory");
    }
    
    public void check(Player player, InventoryClickEvent event) {
        if (!isEnabled()) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        Long lastTime = lastClickTime.get(uuid);
        if (lastTime != null) {
            long timeDiff = currentTime - lastTime;
            
            if (timeDiff < 1000) { // Within 1 second
                int count = clickCount.getOrDefault(uuid, 0) + 1;
                clickCount.put(uuid, count);
                
                int maxCps = plugin.getConfigManager().getConfig()
                    .getInt("anticheat.checks.inventory.max-cps", 15);
                
                if (count > maxCps) {
                    flag(player, "Inventory manipulation detected (CPS: " + count + ")");
                }
            } else {
                clickCount.put(uuid, 1);
            }
        } else {
            clickCount.put(uuid, 1);
        }
        
        lastClickTime.put(uuid, currentTime);
        
        // Clean up old data
        if (lastTime != null && currentTime - lastTime > 5000) {
            clickCount.remove(uuid);
        }
    }
}