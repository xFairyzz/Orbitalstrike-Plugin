package me.fairyzz.orbitalstrike.strikes;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.items.StrikeRodFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StasisStrike {

    private final OrbitalStrikePlugin plugin;
    private static final Map<UUID, Location> playerLocations = new HashMap<>();

    public StasisStrike(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack create(double x, double y, double z) {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName("Stasis");
        meta.setCustomModelData(StrikeRodFactory.CUSTOM_MODEL_DATA);

        item.setItemMeta(meta);
        item.setDurability((short) 61);

        return item;
    }

    public void storeLocation(UUID playerId, Location loc) {
        playerLocations.put(playerId, loc);
    }

    public Location retrieveLocation(UUID playerId) {
        return playerLocations.remove(playerId);
    }

    public void execute(Player player, ItemStack item) {
        Location dest = retrieveLocation(player.getUniqueId());
        if (dest == null) {
            return;
        }

        dest.setWorld(player.getWorld());
        player.teleport(dest);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_TELEPORT, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        item.setAmount(item.getAmount() - 1);
    }
}