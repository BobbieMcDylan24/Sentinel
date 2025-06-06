package com.eyesaber.anticheat.listeners;

import com.eyesaber.anticheat.AntiCheatPlugin;
import com.eyesaber.anticheat.checks.block.FastBreakCheck;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockListener implements Listener {
    
    private final AntiCheatPlugin plugin;
    private final FastBreakCheck fastBreakCheck;
    
    public BlockListener(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.fastBreakCheck = new FastBreakCheck(plugin);
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (player.hasPermission("anticheat.bypass")) {
            return;
        }
        
        fastBreakCheck.check(player, event);
    }
}