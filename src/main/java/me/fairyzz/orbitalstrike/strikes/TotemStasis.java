package me.fairyzz.orbitalstrike.strikes;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.items.StrikeRodFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

public class TotemStasis {

    private final NamespacedKey markerKey;
    private final NamespacedKey worldKey;
    private final NamespacedKey xKey;
    private final NamespacedKey yKey;
    private final NamespacedKey zKey;

    public TotemStasis(OrbitalStrikePlugin plugin) {
        this.markerKey = new NamespacedKey(plugin, "totem_stasis");
        this.worldKey = new NamespacedKey(plugin, "totem_world");
        this.xKey = new NamespacedKey(plugin, "totem_x");
        this.yKey = new NamespacedKey(plugin, "totem_y");
        this.zKey = new NamespacedKey(plugin, "totem_z");
    }

    public ItemStack create(String worldName, double x, double y, double z) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName("§6Stasis Totem");
        meta.setLore(List.of(
                "§7Pop this totem to teleport",
                "§8" + (int) x + " " + (int) y + " " + (int) z
        ));
        meta.setCustomModelData(StrikeRodFactory.CUSTOM_MODEL_DATA);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(worldKey, PersistentDataType.STRING, worldName);
        pdc.set(xKey, PersistentDataType.DOUBLE, x);
        pdc.set(yKey, PersistentDataType.DOUBLE, y);
        pdc.set(zKey, PersistentDataType.DOUBLE, z);

        item.setItemMeta(meta);
        return item;
    }

    public void give(Player player, double x, double y, double z) {
        ItemStack totem = create(player.getWorld().getName(), x, y, z);
        if (player.getInventory().getItemInOffHand().getType().isAir()) {
            player.getInventory().setItemInOffHand(totem);
        } else {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(totem);
            leftover.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
        }
        player.sendMessage("§aStasis Totem bound to §f" + (int) x + " " + (int) y + " " + (int) z);
    }

    public boolean isStasisTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public Location readLocation(ItemStack item, World fallback) {
        if (!isStasisTotem(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Double x = pdc.get(xKey, PersistentDataType.DOUBLE);
        Double y = pdc.get(yKey, PersistentDataType.DOUBLE);
        Double z = pdc.get(zKey, PersistentDataType.DOUBLE);
        if (x == null || y == null || z == null) return null;

        String worldName = pdc.get(worldKey, PersistentDataType.STRING);
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        if (world == null) world = fallback;
        if (world == null) return null;

        return new Location(world, x, y, z);
    }
}
