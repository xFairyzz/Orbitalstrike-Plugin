package me.fairyzz.orbitalstrike.strikes;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.config.PluginConfig;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("ClassCanBeRecord")
public class DogsStrike {

    private final PluginConfig cfg;

    public DogsStrike(OrbitalStrikePlugin plugin) {
        this.cfg = plugin.getPluginConfig();
    }

    public void spawn(World world, Location center, Player owner) {
        int    count         = cfg.getInt("dogs.count", 50);
        double radius        = cfg.getDouble("dogs.radius", 5.0);
        int    durationTicks = cfg.getInt("dogs.effect-duration", 2400);

        List<PotionEffect> effects = buildEffects(durationTicks);
        Location ground = findGround(world, center);

        for (int i = 0; i < count; i++) {
            double angle    = Math.random() * 360;
            double dist     = Math.random() * radius;
            double x        = ground.getX() + dist * Math.cos(Math.toRadians(angle));
            double z        = ground.getZ() + dist * Math.sin(Math.toRadians(angle));
            Location spawn  = findGround(world, new Location(world, x, ground.getY(), z));
            if (spawn.getBlock().isLiquid()) continue;

            Wolf wolf = (Wolf) world.spawnEntity(spawn, EntityType.WOLF);
            wolf.setTamed(true);
            wolf.setOwner(owner);
            wolf.setSitting(false);
            wolf.setCollarColor(DyeColor.RED);
            Objects.requireNonNull(wolf.getEquipment()).setItem(EquipmentSlot.BODY, new ItemStack(Material.WOLF_ARMOR));
            effects.forEach(wolf::addPotionEffect);
        }
    }

    private List<PotionEffect> buildEffects(int duration) {
        List<String>       raw     = cfg.getStringList("dogs.effects");
        List<PotionEffect> effects = new ArrayList<>();

        for (int i = 0; i < Math.min(raw.size(), 3); i++) {
            String[] parts = raw.get(i).trim().toUpperCase().split(":");
            if (parts.length != 2) continue;

            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            if (type == null) continue;

            try {
                int amp = Math.max(0, Integer.parseInt(parts[1]) - 1);
                effects.add(new PotionEffect(type, duration, amp));
            } catch (NumberFormatException ignored) { }
        }

        if (effects.isEmpty()) {
            effects.add(new PotionEffect(PotionEffectType.SPEED, duration, 1));
        }
        return effects;
    }

    private Location findGround(World world, Location start) {
        Location loc = start.clone();
        while (loc.getY() > world.getMinHeight() && loc.getBlock().getType().isAir()) {
            loc.subtract(0, 1, 0);
        }
        return loc.add(0, 1, 0);
    }
}
