package me.fairyzz.orbitalstrike.strikes;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
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
    private static final int PER_BURST = 3;
    private static final double HEIGHT = 28;
    private static final double SPREAD = 4;
    private static final double RANGE = 48;
    private static final double SPEED = 1.2;

    private final OrbitalStrikePlugin plugin;

    public WitherStrike(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
    }

    public void spawn(World world, Location center, Player caster) {
        UUID casterId = caster.getUniqueId();

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

                for (int i = 0; i < PER_BURST; i++) {
                    double ox = (Math.random() - 0.5) * 2 * SPREAD;
                    double oz = (Math.random() - 0.5) * 2 * SPREAD;
                    Location spawn = new Location(world, aim.getX() + ox, aim.getY() + HEIGHT, aim.getZ() + oz);

                    Vector dir = new Vector(0, -1, 0);
                    if (prey != null) {
                        Vector toPrey = prey.getLocation().add(0, 1, 0).toVector().subtract(spawn.toVector());
                        if (toPrey.lengthSquared() > 0.01) dir = toPrey.normalize();
                    }

                    WitherSkull skull = world.spawn(spawn, WitherSkull.class);
                    skull.setCharged(false);
                    skull.setYield(1.0f);
                    skull.setBounce(false);
                    skull.setInvulnerable(true);
                    skull.setDirection(dir);
                    skull.setVelocity(dir.multiply(SPEED));
                }

                elapsed += INTERVAL;
            }
        }.runTaskTimer(plugin, 0L, INTERVAL);
    }

    private Player nearestPlayer(World world, Location center, UUID casterId) {
        Player best = null;
        double bestDist = RANGE * RANGE;
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
