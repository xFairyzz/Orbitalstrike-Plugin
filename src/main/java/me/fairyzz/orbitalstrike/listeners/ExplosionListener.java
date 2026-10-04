package me.fairyzz.orbitalstrike.listeners;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ExplosionListener implements Listener {

    private final OrbitalStrikePlugin plugin;

    public ExplosionListener(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;

        String type = plugin.getStrikeType(tnt);
        if (type == null) return;

        boolean breakBlocks = switch (type) {
            case "nuke" -> plugin.getPluginConfig().getBoolean("nuke.break-blocks", true);
            case "stab" -> plugin.getPluginConfig().getBoolean("stab.break-blocks", true);
            default -> true;
        };

        if (!breakBlocks) {
            event.blockList().clear();
        }
    }
}
