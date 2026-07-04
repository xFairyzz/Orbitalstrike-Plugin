package me.fairyzz.orbitalstrike.listeners;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.items.StrikeRodFactory;
import me.fairyzz.orbitalstrike.strikes.StrikeExecutor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class FishingRodListener implements Listener {

    private static final Set<PlayerFishEvent.State> TRIGGER_STATES = EnumSet.of(
            PlayerFishEvent.State.REEL_IN,
            PlayerFishEvent.State.FAILED_ATTEMPT,
            PlayerFishEvent.State.CAUGHT_ENTITY,
            PlayerFishEvent.State.IN_GROUND,
            PlayerFishEvent.State.BITE
    );

    private final OrbitalStrikePlugin plugin;
    private final StrikeRodFactory rodFactory;
    private final StrikeExecutor executor;

    public FishingRodListener(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.rodFactory = new StrikeRodFactory(plugin);
        this.executor = new StrikeExecutor(plugin);
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!plugin.getPluginConfig().getBoolean("rod.throw-rod", true)) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!plugin.getPendingStrikes().containsKey(playerId)) return;
        if (!TRIGGER_STATES.contains(event.getState())) return;

        String type = plugin.getPendingStrikes().remove(playerId);

        if (executor.checkCooldownPublic(player, type)) {
            return;
        }

        if (type.equals("stasis")) {
            ItemStack main = player.getInventory().getItemInMainHand();
            ItemStack off = player.getInventory().getItemInOffHand();

            if (main.getType() == Material.FISHING_ROD && rodFactory.isStrikeRod(main)) {
                executor.executeStasis(player, main);
            } else if (off.getType() == Material.FISHING_ROD && rodFactory.isStrikeRod(off)) {
                executor.executeStasis(player, off);
            }
            return;
        }

        boolean consumed = consumeStrikeRod(player);
        if (consumed) player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);

        Location target = rayTrace(player);
        if (target == null) return;

        executor.execute(player, new ItemStack(Material.FISHING_ROD), type, target);
    }

    private boolean consumeStrikeRod(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (main.getType() == Material.FISHING_ROD && rodFactory.isStrikeRod(main)) {
            main.setAmount(main.getAmount() - 1);
            return true;
        }
        if (off.getType() == Material.FISHING_ROD && rodFactory.isStrikeRod(off)) {
            off.setAmount(off.getAmount() - 1);
            return true;
        }
        return false;
    }

    private Location rayTrace(Player player) {
        int range = plugin.getPluginConfig().getInt("rod.distance", 100);
        RayTraceResult result = player.rayTraceBlocks(range);
        if (result == null || result.getHitBlock() == null) return null;
        return result.getHitBlock().getLocation().add(0, 60, 0);
    }

    @EventHandler
    public void onTNTLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;

        UUID tntId = tnt.getUniqueId();
        if (!plugin.getTrackedTNT().contains(tntId)) return;

        event.setCancelled(true);
        if (event.getBlock().getType().isSolid()) {
            plugin.getTrackedTNT().remove(tntId);
            if (!tnt.isDead()) tnt.setFuseTicks(1);
        }
    }
}