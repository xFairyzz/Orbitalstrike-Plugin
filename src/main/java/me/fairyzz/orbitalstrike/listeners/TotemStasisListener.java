package me.fairyzz.orbitalstrike.listeners;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.strikes.TotemStasis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class TotemStasisListener implements Listener {

    private final OrbitalStrikePlugin plugin;
    private final TotemStasis totemStasis;

    public TotemStasisListener(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.totemStasis = new TotemStasis(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getHealth() + player.getAbsorptionAmount() - event.getFinalDamage() > 0) return;

        ItemStack totem = findPoppingTotem(player);
        if (totem == null) return;

        Location dest = totemStasis.readLocation(totem, player.getWorld());
        if (dest == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.teleport(dest);
            player.playSound(dest, Sound.ENTITY_PLAYER_TELEPORT, 1f, 1f);
        }, 1L);
    }

    private ItemStack findPoppingTotem(Player player) {
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.TOTEM_OF_UNDYING) {
            return totemStasis.isStasisTotem(off) ? off : null;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.TOTEM_OF_UNDYING && totemStasis.isStasisTotem(main)) {
            return main;
        }
        return null;
    }
}
