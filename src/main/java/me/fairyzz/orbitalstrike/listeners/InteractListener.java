package me.fairyzz.orbitalstrike.listeners;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.items.StrikeRodFactory;
import me.fairyzz.orbitalstrike.strikes.StrikeExecutor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

public class InteractListener implements Listener {

    private final OrbitalStrikePlugin plugin;
    private final StrikeRodFactory rodFactory;
    private final StrikeExecutor executor;

    public InteractListener(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.rodFactory = new StrikeRodFactory(plugin);
        this.executor = new StrikeExecutor(plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT") || !event.hasItem()) return;

        ItemStack item = event.getItem();
        if (!rodFactory.isStrikeRod(item)) return;

        String type = rodFactory.getStrikeType(item);
        if (type == null) return;

        Player player = event.getPlayer();
        boolean throwRod = plugin.getPluginConfig().getBoolean("rod.throw-rod", true);

        if (type.equals("chunkeater")) return;

        if (type.equals("stasis")) {
            if (throwRod) {
                plugin.getPendingStrikes().put(player.getUniqueId(), type);
            } else {
                executor.executeStasis(player, item);
                event.setCancelled(true);
            }
            return;
        }

        Location target = rayTrace(player);
        if (target == null) {
            return;
        }

        if (throwRod) {
            plugin.getPendingStrikes().put(player.getUniqueId(), type);
        } else {
            executor.execute(player, item, type, target);
            event.setCancelled(true);
        }
    }

    private Location rayTrace(Player player) {
        int range = plugin.getPluginConfig().getInt("rod.distance", 100);
        RayTraceResult result = player.rayTraceBlocks(range);
        if (result == null || result.getHitBlock() == null) return null;
        return result.getHitBlock().getLocation().add(0, 60, 0);
    }

}