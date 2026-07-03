package me.fairyzz.orbitalstrike.listeners;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.items.StrikeRodFactory;
import me.fairyzz.orbitalstrike.strikes.StrikeExecutor;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class ChunkEaterPlaceListener implements Listener {

    private final StrikeRodFactory    rodFactory;
    private final StrikeExecutor      executor;

    public ChunkEaterPlaceListener(OrbitalStrikePlugin plugin) {
        this.rodFactory = new StrikeRodFactory(plugin);
        this.executor   = new StrikeExecutor(plugin);
    }

    @EventHandler
    public void onArmorStandPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;

        Player    player = event.getPlayer();
        if (player == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.ARMOR_STAND) {
            item = player.getInventory().getItemInOffHand();
        }

        if (!rodFactory.isStrikeRod(item) || !Objects.equals(rodFactory.getStrikeType(item), "chunkeater")) return;

        stand.remove();
        executor.executeChunkEater(player, stand.getLocation());
    }
}
