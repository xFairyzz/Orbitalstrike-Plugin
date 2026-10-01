package me.fairyzz.orbitalstrike.strikes;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.WitherSkull;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WitherStrike {

    private final OrbitalStrikePlugin plugin;
    private final PluginConfig cfg;

    public WitherStrike(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getPluginConfig();
    }

    public void spawn(World world, Location center) {
        int duration = Math.max(cfg.getInt("wither.duration-ticks", 600), 600);
        int interval = Math.max(cfg.getInt("wither.interval-ticks", 8), 1);
        int perBurst = Math.max(cfg.getInt("wither.per-burst", 3), 1);
        double height = cfg.getDouble("wither.height", 28);
        double radius = cfg.getDouble("wither.radius", 8);
        boolean charged = cfg.getBoolean("wither.charged", false);
        float yield = (float) cfg.getDouble("wither.yield", 1.0);
        double speed = cfg.getDouble("wither.speed", 1.2);

        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= duration) {
                    cancel();
                    return;
                }

                for (int i = 0; i < perBurst; i++) {
                    double ox = (Math.random() - 0.5) * 2 * radius;
                    double oz = (Math.random() - 0.5) * 2 * radius;
                    Location spawn = new Location(world, center.getX() + ox, center.getY() + height, center.getZ() + oz);
                    WitherSkull skull = world.spawn(spawn, WitherSkull.class);
                    skull.setCharged(charged);
                    skull.setYield(yield);
                    skull.setBounce(false);
                    skull.setInvulnerable(true);
                    skull.setDirection(new Vector(0, -1, 0));
                    skull.setVelocity(new Vector(0, -speed, 0));
                }

                elapsed += interval;
            }
        }.runTaskTimer(plugin, 0L, interval);
    }
}
