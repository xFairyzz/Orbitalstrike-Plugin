package me.fairyzz.orbitalstrike.strikes;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;

@SuppressWarnings("ClassCanBeRecord")
public class StabStrike {

    private final PluginConfig cfg;

    public StabStrike(OrbitalStrikePlugin plugin) {
        this.cfg = plugin.getPluginConfig();
    }

    public void spawn(World world, Location center) {
        Location ground = findGround(world, center);
        float    yield  = (float) cfg.getDouble("stab.yield", 8.0);
        double   offset = cfg.getDouble("stab.tnt-offset", 0.3);

        int y    = (int) ground.getY();
        int minY = world.getMinHeight();

        while (y >= minY) {
            Location base = new Location(world, ground.getX(), y, ground.getZ());
            spawnTNT(world, base.clone().add(offset, 0, offset),  yield);
            spawnTNT(world, base.clone().subtract(offset, 0, offset), yield);
            y -= 2;
        }
    }

    private void spawnTNT(World world, Location loc, float yield) {
        if (loc.getBlock().isLiquid()) return;
        TNTPrimed tnt = (TNTPrimed) world.spawnEntity(loc, EntityType.TNT);
        tnt.setFuseTicks(0);
        tnt.setYield(yield);
    }

    private Location findGround(World world, Location start) {
        Location loc = start.clone();
        while (loc.getY() > world.getMinHeight() && loc.getBlock().getType().isAir()) {
            loc.subtract(0, 1, 0);
        }
        return loc.add(0, 1, 0);
    }
}
