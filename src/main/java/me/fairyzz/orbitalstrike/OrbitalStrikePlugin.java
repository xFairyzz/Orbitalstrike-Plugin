package me.fairyzz.orbitalstrike;

import me.fairyzz.orbitalstrike.commands.OrbitalCommand;
import me.fairyzz.orbitalstrike.config.PluginConfig;
import me.fairyzz.orbitalstrike.listeners.ChunkEaterPlaceListener;
import me.fairyzz.orbitalstrike.listeners.FishingRodListener;
import me.fairyzz.orbitalstrike.listeners.InteractListener;
import me.fairyzz.orbitalstrike.updater.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class OrbitalStrikePlugin extends JavaPlugin {

    private final Map<UUID, Set<UUID>> strikeTNT = new HashMap<>();
    private final Map<UUID, String> pendingStrikes = new HashMap<>();
    private final Set<UUID> trackedTNT = new HashSet<>();
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    private PluginConfig pluginConfig;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);

        updateChecker = new UpdateChecker(this);
        updateChecker.checkAsync();

        OrbitalCommand commandHandler = new OrbitalCommand(this);
        Objects.requireNonNull(getCommand("orbital")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("orbital")).setTabCompleter(commandHandler);

        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingRodListener(this), this);
        getServer().getPluginManager().registerEvents(updateChecker, this);
        getServer().getPluginManager().registerEvents(new ChunkEaterPlaceListener(this), this);

        getServer().getScheduler().runTaskLater(this, () -> updateChecker.sendConsoleUpdate(), 40L);
    }

    @Override
    public void onDisable() {
        strikeTNT.clear();
        pendingStrikes.clear();
        trackedTNT.clear();
        playerCooldowns.clear();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public Map<UUID, Set<UUID>> getStrikeTNT() {
        return strikeTNT;
    }

    public Map<UUID, String> getPendingStrikes() {
        return pendingStrikes;
    }

    public Set<UUID> getTrackedTNT() {
        return trackedTNT;
    }

    public CooldownManager getCooldowns() {
        return new CooldownManager(playerCooldowns);
    }

    public static class CooldownManager {
        private final Map<UUID, Map<String, Long>> cooldowns;

        public CooldownManager(Map<UUID, Map<String, Long>> cooldowns) {
            this.cooldowns = cooldowns;
        }

        public void put(UUID uuid, String strikeType, long timestamp) {
            cooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(strikeType.toLowerCase(), timestamp);
        }

        public Long get(UUID uuid, String strikeType) {
            Map<String, Long> playerMap = cooldowns.get(uuid);
            return playerMap != null ? playerMap.get(strikeType.toLowerCase()) : null;
        }

        public void clearPlayer(UUID uuid) {
            cooldowns.remove(uuid);
        }
    }
}