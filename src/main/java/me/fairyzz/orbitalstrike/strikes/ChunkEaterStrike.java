package me.fairyzz.orbitalstrike.strikes;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;

import java.util.Random;

@SuppressWarnings("ClassCanBeRecord")
public class ChunkEaterStrike {

    private final OrbitalStrikePlugin plugin;

    public ChunkEaterStrike(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
    }

    public void spawn(World world, Location center) {
        Location ground  = findGround(world, center);
        Chunk    chunk   = ground.getChunk();
        int      chunkX  = chunk.getX() << 4;
        int      chunkZ  = chunk.getZ() << 4;
        int      minY    = world.getMinHeight();
        int      maxY    = world.getMaxHeight();
        Random   random  = new Random();

        for (int i = 0; i < 250; i++) {
            double x = chunkX + random.nextDouble() * 16;
            double z = chunkZ + random.nextDouble() * 16;
            double y = center.getY() + 35;

            TNTPrimed tnt = (TNTPrimed) world.spawnEntity(new Location(world, x, y, z), EntityType.TNT);
            tnt.setFuseTicks(90);
            tnt.setYield(2.0f);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!chunk.isLoaded()) chunk.load();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minY + 1; y < maxY; y++) {
                        if (chunk.getBlock(x, y, z).getType() != Material.BEDROCK) {
                            chunk.getBlock(x, y, z).setType(Material.AIR, false);
                        }
                    }
                }
            }
        }, 100L);
    }

    private Location findGround(World world, Location start) {
        Location loc = start.clone();
        while (loc.getY() > world.getMinHeight() && loc.getBlock().getType().isAir()) {
            loc.subtract(0, 1, 0);
        }
        return loc.add(0, 1, 0);
    }
}
