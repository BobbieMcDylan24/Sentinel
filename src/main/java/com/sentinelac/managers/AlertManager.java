package com.sentinelac.managers;

import com.sentinelac.SentinelAC;
import com.sentinelac.checks.Check;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class AlertManager {

    private final SentinelAC plugin;
    private final HttpClient httpClient;
    
    // Configuration
    private boolean alertsEnabled;
    private boolean consoleAlertsEnabled;
    private boolean webhookEnabled;
    private String webhookUrl;
    private String staffPermission;

    public AlertManager(SentinelAC plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        initialize();
    }
    
    private void initialize() {
        plugin.getLogger().info("Initializing alert manager...");
        
        loadConfig();
        
        plugin.getLogger().info("Alert manager initialized");
    }
    
    private void loadConfig() {
        this.alertsEnabled = plugin.getConfigManager().areAlertsEnabled();
        this.consoleAlertsEnabled = plugin.getConfigManager().areConsoleAlertsEnabled();
        this.webhookEnabled = plugin.getConfigManager().getBoolean("alerts.webhook.enabled");
        this.webhookUrl = plugin.getConfigManager().getString("alerts.webhook.url");
        this.staffPermission = plugin.getConfigManager().getString("alerts.staff-permission");
        
        // Validate webhook URL if enabled
        if (webhookEnabled && (webhookUrl == null || webhookUrl.trim().isEmpty())) {
            plugin.getLogger().warning("Webhook alerts are enabled but no URL is configured!");
            webhookEnabled = false;
        }
    }
    
    public void sendAlert(Player player, Check check, int violations, String details) {
        if (!alertsEnabled) {
            return;
        }
        
        try {
            String alertMessage = formatAlert(player, check, violations, details);
            
            // Send to console
            if (consoleAlertsEnabled) {
                sendConsoleAlert(alertMessage);
            }
            
            // Send to staff members
            sendStaffAlert(alertMessage);
            
            // Send webhook if enabled
            if (webhookEnabled) {
                sendWebhookAlert(player, check, violations, details);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error sending alert for " + player.getName(), e);
        }
    }
    
    private String formatAlert(Player player, Check check, int violations, String details) {
        String format = plugin.getConfigManager().getAlertFormat("default");
        
        return format
            .replace("{player}", player.getName())
            .replace("{check}", check.getName())
            .replace("{violations}", String.valueOf(violations))
            .replace("{details}", details)
            .replace("{ping}", String.valueOf(plugin.getPlayerDataManager().getPlayerData(player).getPing()))
            .replace("{tps}", String.format("%.1f", plugin.getPlayerDataManager().getPlayerData(player).getServerTPS()));
    }
    
    private void sendConsoleAlert(String message) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    private void sendStaffAlert(String message) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission(staffPermission)) {
                    staff.sendMessage(coloredMessage);
                }
            }
        });
    }
    
    public void sendStaffMessage(String message) {
        if (!alertsEnabled) {
            return;
        }
        
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        // Send to console
        if (consoleAlertsEnabled) {
            sendConsoleAlert(message);
        }
        
        // Send to staff
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission(staffPermission)) {
                    staff.sendMessage(coloredMessage);
                }
            }
        });
    }
    
    private void sendWebhookAlert(Player player, Check check, int violations, String details) {
        if (!webhookEnabled || webhookUrl == null) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                String jsonPayload = buildWebhookPayload(player, check, violations, details);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(10))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    plugin.getLogger().warning("Webhook alert failed with status code: " + response.statusCode());
                }
                
            } catch (IOException | InterruptedException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send webhook alert", e);
            }
        });
    }
    
    private String buildWebhookPayload(Player player, Check check, int violations, String details) {
        // Build Discord webhook payload format
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"content\": null,");
        json.append("\"embeds\": [{");
        json.append("\"title\": \"SentinelAC Alert\",");
        json.append("\"color\": 15158332,"); // Red color
        json.append("\"fields\": [");
        json.append("{\"name\": \"Player\", \"value\": \"").append(escapeJson(player.getName())).append("\", \"inline\": true},");
        json.append("{\"name\": \"Check\", \"value\": \"").append(escapeJson(check.getName())).append("\", \"inline\": true},");
        json.append("{\"name\": \"Violations\", \"value\": \"").append(violations).append("\", \"inline\": true},");
        json.append("{\"name\": \"Details\", \"value\": \"").append(escapeJson(details)).append("\", \"inline\": false},");
        json.append("{\"name\": \"Ping\", \"value\": \"").append(plugin.getPlayerDataManager().getPlayerData(player).getPing()).append("ms\", \"inline\": true},");
        json.append("{\"name\": \"TPS\", \"value\": \"").append(String.format("%.1f", plugin.getPlayerDataManager().getPlayerData(player).getServerTPS())).append("\", \"inline\": true}");
        json.append("],");
        json.append("\"timestamp\": \"").append(java.time.Instant.now().toString()).append("\"");
        json.append("}]");
        json.append("}");
        
        return json.toString();
    }
    
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    public void sendStartupMessage() {
        if (alertsEnabled) {
            String message = "&aSentinelAC &7» &fPlugin enabled successfully";
            sendStaffMessage(message);
        }
    }
    
    public void sendShutdownMessage() {
        if (alertsEnabled) {
            String message = "&cSentinelAC &7» &fPlugin disabled";
            sendStaffMessage(message);
        }
    }
    
    public void testWebhook() {
        if (!webhookEnabled) {
            plugin.getLogger().info("Webhook is not enabled");
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                String testPayload = "{\"content\": \"SentinelAC webhook test - connection successful!\"}";
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(testPayload))
                    .timeout(Duration.ofSeconds(10))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    plugin.getLogger().info("Webhook test successful!");
                } else {
                    plugin.getLogger().warning("Webhook test failed with status code: " + response.statusCode());
                }
                
            } catch (IOException | InterruptedException e) {
                plugin.getLogger().log(Level.WARNING, "Webhook test failed", e);
            }
        });
    }
    
    public void reload() {
        loadConfig();
        plugin.getLogger().info("Alert manager configuration reloaded");
    }
    
    public void shutdown() {
        plugin.getLogger().info("Shutting down alert manager...");
        
        // Send shutdown message if enabled
        sendShutdownMessage();
        
        plugin.getLogger().info("Alert manager shutdown complete");
    }
    
    // Getters
    public boolean areAlertsEnabled() {
        return alertsEnabled;
    }
    
    public boolean areConsoleAlertsEnabled() {
        return consoleAlertsEnabled;
    }
    
    public boolean isWebhookEnabled() {
        return webhookEnabled;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public String getStaffPermission() {
        return staffPermission;
    }
    
    // Setters (for runtime configuration)
    public void setAlertsEnabled(boolean enabled) {
        this.alertsEnabled = enabled;
    }
    
    public void setWebhookEnabled(boolean enabled) {
        this.webhookEnabled = enabled;
    }
    
    public void setWebhookUrl(String url) {
        this.webhookUrl = url;
        this.webhookEnabled = url != null && !url.trim().isEmpty();
    }
}
