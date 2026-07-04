package me.fairyzz.orbitalstrike.updater;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker implements Listener {

    private static final String GITHUB_REPO     = "xFairyzz/Orbitalstrike-Plugin";
    private static final String CURRENT_VERSION = "v1.6.8";

    private final OrbitalStrikePlugin plugin;
    private boolean hasUpdate     = false;
    private String  latestVersion = "";

    public UpdateChecker(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
    }

    public void checkAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String apiUrl = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) {
                    plugin.getLogger().warning("[OrbitalStrike] GitHub API ERROR: HTTP " + conn.getResponseCode());
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }

                String tagName = extractTagName(sb.toString());
                if (tagName == null) return;

                String normalizedTag     = tagName.toLowerCase().startsWith("v")         ? tagName.substring(1)         : tagName;
                String normalizedCurrent = CURRENT_VERSION.toLowerCase().startsWith("v") ? CURRENT_VERSION.substring(1) : CURRENT_VERSION;

                if (!normalizedTag.equalsIgnoreCase(normalizedCurrent)) {
                    hasUpdate     = true;
                    latestVersion = tagName;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[OrbitalStrike] Update-Check Failed: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> notifyPlayer(event.getPlayer()), 20L);
    }

    public void notifyPlayer(Player player) {
        if (hasUpdate && player.isOp()) {
            player.sendMessage("§c[OrbitalStrike] §fUpdate available! §cCurrent: "
                    + CURRENT_VERSION + " -> §aLatest: " + latestVersion);
            player.sendMessage("§cDownload: §fhttps://modrinth.com/plugin/orbitalstrike-plugin");
        }
    }

    public void sendConsoleUpdate() {
        if (hasUpdate) {
            plugin.getLogger().warning("\u001B[31mUPDATE AVAILABLE! Current: "
                    + CURRENT_VERSION + " -> Latest: " + latestVersion + "\u001B[0m");
            plugin.getLogger().warning("\u001B[31mDownload: https://modrinth.com/plugin/orbitalstrike-plugin\u001B[0m");
        }
    }

    private String extractTagName(String json) {
        int idx = json.indexOf("\"tag_name\":\"");
        if (idx == -1) return null;
        int start = idx + 12;
        int end   = json.indexOf("\"", start);
        return (end == -1) ? null : json.substring(start, end);
    }
}