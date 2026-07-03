package me.fairyzz.orbitalstrike.items;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@SuppressWarnings("ClassCanBeRecord")
public class StrikeRodFactory {

    public static final int CUSTOM_MODEL_DATA = 12345;

    private final OrbitalStrikePlugin plugin;

    public StrikeRodFactory(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack create(String type) {
        return create(type, 0, 0, 0);
    }

    public ItemStack create(String type, double x, double y, double z) {
        Material material = type.equals("chunkeater") ? Material.ARMOR_STAND : Material.FISHING_ROD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        assert meta != null;
        meta.setDisplayName(displayNameFor(type));
        meta.setCustomModelData(CUSTOM_MODEL_DATA);

        item.setItemMeta(meta);
        if (material == Material.FISHING_ROD) item.setDurability((short) 61);
        return item;
    }

    public boolean isStrikeRod(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (!meta.hasDisplayName() || !meta.hasCustomModelData()) return false;
        if (meta.getCustomModelData() != CUSTOM_MODEL_DATA) return false;
        Material t = item.getType();
        return t == Material.FISHING_ROD || t == Material.ARMOR_STAND;
    }

    public String getStrikeType(ItemStack item) {
        if (!isStrikeRod(item)) return null;
        String name = item.getItemMeta().getDisplayName();
        return switch (name) {
            case "Nuke shot"    -> "nuke";
            case "Stab shot"    -> "stab";
            case "Dogs"         -> "dogs";
            case "Chunk Eater"  -> "chunkeater";
            case "Stasis"       -> "stasis";
            default             -> null;
        };
    }

    private static String displayNameFor(String type) {
        return switch (type) {
            case "nuke"       -> "Nuke shot";
            case "stab"       -> "Stab shot";
            case "dogs"       -> "Dogs";
            case "chunkeater" -> "Chunk Eater";
            case "stasis"     -> "Stasis";
            default           -> "Orbital Strike Rod";
        };
    }
}