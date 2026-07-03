package me.fairyzz.orbitalstrike.strikes;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NukeStrike {

    private final OrbitalStrikePlugin plugin;
    private final PluginConfig cfg;

    private final Map<UUID, TNTPrimed> tntCache = new HashMap<>();

    public NukeStrike(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.cfg    = plugin.getPluginConfig();
    }

    public void spawn(World world, Location center, UUID strikeId, Set<UUID> tntList) {
        int     rings         = cfg.getInt("nuke.rings", 10);
        double  height        = center.getY() + cfg.getInt("nuke.height", 80);
        float   yield         = (float) cfg.getDouble("nuke.yield", 6.0);
        int     baseTnt       = cfg.getInt("nuke.tnt-per-ring-base", 40);
        int     increase      = cfg.getInt("nuke.tnt-per-ring-increase", 2);
        boolean centerTnt     = cfg.getBoolean("nuke.center-tnt", true);
        boolean animatedRings = cfg.getBoolean("nuke.Animated-rings", true);

        Location centerLoc = new Location(world, center.getX() + 0.5, height, center.getZ() + 0.5);

        if (animatedRings) {
            spawnAnimated(world, center, centerLoc, yield, rings, baseTnt, increase, centerTnt, tntList);
        } else {
            spawnInstant(world, center, centerLoc, yield, rings, baseTnt, increase, centerTnt, strikeId, tntList);
        }
    }

    private void spawnAnimated(World world, Location center, Location centerLoc,
                               float yield, int rings, int baseTnt, int increase,
                               boolean centerTnt, Set<UUID> tntList) {
        if (centerTnt) {
            TNTPrimed ct = spawnHovering(world, centerLoc.clone(), yield, tntList);
            UUID centerId = ct.getUniqueId();
            cacheTNT(ct);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                TNTPrimed t = getCachedTNT(centerId);
                if (t != null && !t.isDead()) t.setGravity(true);
            }, 30L);
        }

        boolean splitRings   = cfg.getBoolean("nuke.damaged-rings", false);
        double  minRemove    = cfg.getDouble("nuke.full-rings-min-remove", 0.20);
        double  maxRemove    = cfg.getDouble("nuke.full-rings-max-remove", 0.35);
        int     fuseFallback = cfg.getInt("nuke.fuse-ticks", 160);

        for (int ring = 1; ring <= rings; ring++) {
            double radius        = ring * 4.0;
            int    originalCount = baseTnt + ring * increase;
            int    tntCount      = originalCount;

            if (splitRings && originalCount > 15) {
                double pct    = minRemove + (Math.random() * (maxRemove - minRemove));
                int    remove = (int) (originalCount * pct);
                remove   = Math.min(remove, originalCount - 10);
                tntCount = originalCount - remove;
            }

            double step = 360.0 / originalCount;
            List<Integer> indices = new ArrayList<>();
            for (int j = 0; j < originalCount; j++) indices.add(j);

            if (splitRings && tntCount < originalCount) {
                Collections.shuffle(indices);
                indices = new ArrayList<>(indices.subList(0, tntCount));
                Collections.sort(indices);
            }

            for (int idx : indices) {
                double angle   = idx * step + (ring * 10);
                double targetX = Math.round((center.getX() + radius * Math.cos(Math.toRadians(angle))) * 10) / 10.0 + 0.5;
                double targetZ = Math.round((center.getZ() + radius * Math.sin(Math.toRadians(angle))) * 10) / 10.0 + 0.5;

                TNTPrimed tnt    = spawnHovering(world, centerLoc.clone(), yield, tntList);
                UUID      tntUid = tnt.getUniqueId();
                cacheTNT(tnt);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    TNTPrimed t = getCachedTNT(tntUid);
                    if (t != null && !t.isDead()) {
                        t.setVelocity(calcVelocity(targetX, centerLoc, targetZ));
                        t.setGravity(true);
                    }
                }, 30L);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID id : tntList) {
                TNTPrimed t = getCachedTNT(id);
                if (t != null && !t.isDead()) {
                    t.setFuseTicks(1);
                }
                removeCachedTNT(id);
            }
            tntList.clear();
        }, fuseFallback);
    }

    private TNTPrimed spawnHovering(World world, Location loc, float yield, Set<UUID> tntList) {
        TNTPrimed tnt = (TNTPrimed) world.spawnEntity(loc, EntityType.TNT);
        tnt.setFuseTicks(10000);
        tnt.setVelocity(new Vector(0, 0, 0));
        tnt.setGravity(false);
        tnt.setYield(yield);
        tnt.setInvulnerable(true);
        tntList.add(tnt.getUniqueId());
        plugin.getTrackedTNT().add(tnt.getUniqueId());
        cacheTNT(tnt);
        return tnt;
    }

    private void spawnInstant(World world, Location center, Location centerLoc,
                              float yield, int rings, int baseTnt, int increase,
                              boolean centerTnt, UUID strikeId, Set<UUID> tntList) {
        if (centerTnt) spawnFalling(world, centerLoc.clone(), yield, strikeId, tntList);

        for (int ring = 1; ring <= rings; ring++) {
            double radius   = ring * 4.0;
            int    tntCount = baseTnt + ring * increase;
            double step     = 360.0 / tntCount;
            double startAng = ring * 13.0;

            for (int i = 0; i < tntCount; i++) {
                double angle = startAng + i * step;
                double x     = Math.round((center.getX() + radius * Math.cos(Math.toRadians(angle))) * 10) / 10.0 + 0.5;
                double z     = Math.round((center.getZ() + radius * Math.sin(Math.toRadians(angle))) * 10) / 10.0 + 0.5;
                spawnFalling(world, new Location(world, x, centerLoc.getY(), z), yield, strikeId, tntList);
            }
        }
    }

    private void spawnFalling(World world, Location loc, float yield, UUID strikeId, Set<UUID> tntList) {
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        if (!world.isChunkLoaded(cx, cz)) return;

        TNTPrimed tnt = (TNTPrimed) world.spawnEntity(loc, EntityType.TNT);
        tnt.setFuseTicks(10000);
        tnt.setVelocity(new Vector(0, -0.8, 0));
        tnt.setGravity(true);
        tnt.setYield(yield);
        tnt.setInvulnerable(true);

        UUID tntId       = tnt.getUniqueId();
        int  fallbackTick = cfg.getInt("nuke.fuse-ticks", 160);
        tntList.add(tntId);
        plugin.getTrackedTNT().add(tntId);
        cacheTNT(tnt);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (tntList.contains(tntId) && !tnt.isDead()) {
                tnt.setFuseTicks(1);
                tntList.remove(tntId);
            }
            removeCachedTNT(tntId);
        }, fallbackTick);
    }

    private void cacheTNT(TNTPrimed tnt) {
        tntCache.put(tnt.getUniqueId(), tnt);
    }

    private TNTPrimed getCachedTNT(UUID id) {
        TNTPrimed tnt = tntCache.get(id);
        if (tnt != null && !tnt.isDead()) return tnt;
        return null;
    }

    private void removeCachedTNT(UUID id) {
        tntCache.remove(id);
    }

    private static Vector calcVelocity(double targetX, Location origin, double targetZ) {
        double dx       = targetX - origin.getX();
        double dz       = targetZ - origin.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double speed    = distance / 30.0;
        return new Vector(dx / distance * speed, 0, dz / distance * speed);
    }
}