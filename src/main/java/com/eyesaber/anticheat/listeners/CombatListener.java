package com.eyesaber.anticheat.listeners;

import com.eyesaber.anticheat.AntiCheatPlugin;
import com.eyesaber.anticheat.checks.combat.KillAuraCheck;
import com.eyesaber.anticheat.checks.combat.ReachCheck;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {
    
    private final AntiCheatPlugin plugin;
    private final KillAuraCheck killAuraCheck;
    private final ReachCheck reachCheck;
    
    public CombatListener(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.killAuraCheck = new KillAuraCheck(plugin);
        this.reachCheck = new ReachCheck(plugin);
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        
        if (player.hasPermission("anticheat.bypass")) {
            return;
        }
        
        killAuraCheck.check(player, event);
        reachCheck.check(player, event);
    }
}