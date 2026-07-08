package me.fairyzz.orbitalstrike.config;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConfig {

    private final OrbitalStrikePlugin plugin;
    private FileConfiguration cfg;

    public PluginConfig(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
        registerDefaults();
        plugin.saveConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        cfg = plugin.getConfig();
    }

    public boolean getBoolean(String path, boolean def) { return cfg.getBoolean(path, def); }
    public int getInt(String path, int def)             { return cfg.getInt(path, def); }
    public double getDouble(String path, double def)    { return cfg.getDouble(path, def); }
    public List<String> getStringList(String path)      { return cfg.getStringList(path); }

    public String getPermission() {
        return cfg.getString("permission", "orbital.use");
    }

    public boolean isCooldownsEnabled() {
        return cfg.getBoolean("cooldowns.enabled", true);
    }

    public int getCooldown(String strikeType) {
        return cfg.getInt("cooldowns." + strikeType, 0);
    }

    public boolean isWorldDisabled(World world) {
        List<String> disabled = cfg.getStringList("disabled-worlds");
        return disabled.contains("*") || disabled.contains(world.getName());
    }

    public boolean isStrikeEnabled(String type) {
        return cfg.getBoolean("strikes." + type.toLowerCase() + ".enabled", true);
    }

    private void registerDefaults() {
        cfg.addDefault("permission", "orbital.use");
        addWorldDefaults();
        addCooldownDefaults();
        addRodDefaults();
        addStrikeDefaults();
        addNukeDefaults();
        addStabDefaults();
        addDogsDefaults();
        cfg.options().copyDefaults(true);
    }

    private void addWorldDefaults() {
        List<String> disabled = new ArrayList<>();
        cfg.addDefault("disabled-worlds", disabled);
    }

    private void addCooldownDefaults() {
        Map<String, Object> cooldowns = new HashMap<>();
        cooldowns.put("enabled", false);
        cooldowns.put("nuke", 300);
        cooldowns.put("stab", 120);
        cooldowns.put("dogs", 180);
        cooldowns.put("chunkeater", 600);
        cooldowns.put("stasis", 240);
        cfg.addDefault("cooldowns", cooldowns);
    }

    private void addRodDefaults() {
        Map<String, Object> rod = new HashMap<>();
        rod.put("distance", 100);
        rod.put("throw-rod", true);
        cfg.addDefault("rod", rod);
    }

    private void addStrikeDefaults() {
        Map<String, Boolean> strikes = new HashMap<>();
        strikes.put("nuke.enabled", true);
        strikes.put("stab.enabled", true);
        strikes.put("dogs.enabled", true);
        strikes.put("chunkeater.enabled", true);
        strikes.put("stasis.enabled", true);
        for (Map.Entry<String, Boolean> entry : strikes.entrySet()) {
            cfg.addDefault("strikes." + entry.getKey(), entry.getValue());
        }
    }

    private void addNukeDefaults() {
        Map<String, Object> nuke = new HashMap<>();
        nuke.put("rings", 10);
        nuke.put("height", 80);
        nuke.put("yield", 6.0);
        nuke.put("tnt-per-ring-base", 40);
        nuke.put("tnt-per-ring-increase", 2);
        nuke.put("fuse-ticks", 160);
        nuke.put("center-tnt", true);
        nuke.put("Animated-rings", true);
        nuke.put("damaged-rings", true);
        cfg.addDefault("nuke", nuke);
    }

    private void addStabDefaults() {
        Map<String, Object> stab = new HashMap<>();
        stab.put("yield", 4.0);
        cfg.addDefault("stab", stab);
    }

    private void addDogsDefaults() {
        Map<String, Object> dogs = new HashMap<>();
        dogs.put("count", 50);
        dogs.put("radius", 5.0);
        dogs.put("effect-duration", 2400);
        List<String> effects = new ArrayList<>();
        effects.add("SPEED:1");
        effects.add("STRENGTH:2");
        dogs.put("effects", effects);
        cfg.addDefault("dogs", dogs);
    }
}