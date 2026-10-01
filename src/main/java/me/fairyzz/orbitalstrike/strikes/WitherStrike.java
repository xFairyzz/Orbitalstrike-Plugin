package me.fairyzz.orbitalstrike.strikes;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public class WitherStrike {

    private static final int DURATION = 600;
    private static final int INTERVAL = 8;
    private static final double HEIGHT = 28;
    private static final double TRACK_RANGE = 48;

    private final OrbitalStrikePlugin plugin;
    private final PluginConfig cfg;

    public WitherStrike(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getPluginConfig();
    }

    public void spawn(World world, Location center, Player caster) {
        UUID casterId = caster.getUniqueId();
        boolean charged = cfg.getBoolean("wither.charged", false);
        double range = Math.max(cfg.getDouble("wither.range", 8), 1);
        double width = Math.max(cfg.getDouble("wither.width", 8), 1);
        double speed = Math.max(cfg.getDouble("wither.speed", 1.2), 0.1);
        int skulls = Math.max(cfg.getInt("wither.skulls", 3), 1);

        Vector forward = caster.getLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 0.01) forward = new Vector(0, 0, 1);
        else forward.normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 0.01) right = new Vector(1, 0, 0);
        else right.normalize();

        Vector alongAxis = forward.clone();
        Vector sideAxis = right.clone();

        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= DURATION) {
                    cancel();
                    return;
                }

                Player prey = nearestPlayer(world, center, casterId);
                Location aim = prey != null ? prey.getLocation() : center;

                for (int i = 0; i < skulls; i++) {
                    double along = (Math.random() - 0.5) * range;
                    double side = (Math.random() - 0.5) * width;
                    Location spawn = new Location(
                            world,
                            aim.getX() + alongAxis.getX() * along + sideAxis.getX() * side,
                            aim.getY() + HEIGHT,
                            aim.getZ() + alongAxis.getZ() * along + sideAxis.getZ() * side
                    );

                    Vector dir = new Vector(0, -1, 0);
                    if (prey != null) {
                        Vector toPrey = prey.getLocation().add(0, 1, 0).toVector().subtract(spawn.toVector());
                        if (toPrey.lengthSquared() > 0.01) dir = toPrey.normalize();
                    }

                    WitherSkull skull = world.spawn(spawn, WitherSkull.class);
                    skull.setCharged(charged);
                    skull.setYield(1.0f);
                    skull.setBounce(false);
                    skull.setInvulnerable(true);
                    skull.setDirection(dir);
                    skull.setVelocity(dir.multiply(speed));
                }

                elapsed += INTERVAL;
            }
        }.runTaskTimer(plugin, 0L, INTERVAL);
    }

    private Player nearestPlayer(World world, Location center, UUID casterId) {
        Player best = null;
        double bestDist = TRACK_RANGE * TRACK_RANGE;
        for (Player p : world.getPlayers()) {
            if (p.getUniqueId().equals(casterId) || p.isDead() || !p.isValid()) continue;
            double d = p.getLocation().distanceSquared(center);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }
}
