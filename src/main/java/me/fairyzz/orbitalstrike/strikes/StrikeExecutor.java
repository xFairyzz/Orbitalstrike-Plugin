package me.fairyzz.orbitalstrike.strikes;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.config.PluginConfig;
import me.fairyzz.orbitalstrike.items.StrikeRodFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class StrikeExecutor {

    private final OrbitalStrikePlugin plugin;
    private final PluginConfig cfg;
    private final StrikeRodFactory rodFactory;

    private final NukeStrike nukeStrike;
    private final StabStrike stabStrike;
    private final DogsStrike dogsStrike;
    private final ChunkEaterStrike chunkEaterStrike;
    private final StasisStrike stasisStrike;

    public StrikeExecutor(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getPluginConfig();
        this.rodFactory = new StrikeRodFactory(plugin);

        this.nukeStrike = new NukeStrike(plugin);
        this.stabStrike = new StabStrike(plugin);
        this.dogsStrike = new DogsStrike(plugin);
        this.chunkEaterStrike = new ChunkEaterStrike(plugin);
        this.stasisStrike = new StasisStrike(plugin);
    }

    private boolean checkCooldown(Player player, String type) {
        if (!cfg.isCooldownsEnabled()) return false;

        int seconds = cfg.getCooldown(type);
        if (seconds <= 0) return false;

        Long lastUse = plugin.getCooldowns().get(player.getUniqueId(), type);
        if (lastUse == null) return false;

        long remaining = lastUse + (seconds * 1000L) - System.currentTimeMillis();
        if (remaining > 0) {
            player.sendMessage(ChatColor.RED + "§lCooldown: §f" + (remaining / 1000) + "s §cremaining before using " + type + " again.");
            return true;
        }
        return false;
    }

    public boolean checkCooldownPublic(Player player, String type) {
        return checkCooldown(player, type);
    }

    private void setCooldown(Player player, String type) {
        int seconds = cfg.getCooldown(type);
        if (cfg.isCooldownsEnabled() && seconds > 0) {
            plugin.getCooldowns().put(player.getUniqueId(), type, System.currentTimeMillis());
        }
    }

    public void execute(Player player, ItemStack item, String type, Location target) {
        if (!cfg.isStrikeEnabled(type)) {
            player.sendMessage("§cThis orbital strike is disabled.");
            return;
        }
        if (cfg.isWorldDisabled(target.getWorld())) {
            player.sendMessage("§cOrbitals are disabled in this world");
            return;
        }

        if (checkCooldown(player, type)) {
            return;
        }

        setCooldown(player, type);

        if (!type.equals("chunkeater") && !cfg.getBoolean("rod.throw-rod", true)) {
            consumeRodImmediately(player, item);
        }

        UUID strikeId = UUID.randomUUID();
        Set<UUID> tntList = new HashSet<>();
        plugin.getStrikeTNT().put(strikeId, tntList);

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (type) {
                case "nuke" -> nukeStrike.spawn(target.getWorld(), target, strikeId, tntList);
                case "stab" -> stabStrike.spawn(target.getWorld(), target);
                case "dogs" -> dogsStrike.spawn(target.getWorld(), target, player);
                case "chunkeater" -> chunkEaterStrike.spawn(target.getWorld(), target);
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getStrikeTNT().remove(strikeId), 200L);
        });
    }

    private void consumeRodImmediately(Player player, ItemStack originalItem) {
        if (originalItem == null) return;

        String displayName = Objects.requireNonNull(originalItem.getItemMeta()).getDisplayName();

        ItemStack main = player.getInventory().getItemInMainHand();
        if (isMatchingRod(main, displayName)) {
            main.setAmount(main.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            return;
        }

        ItemStack off = player.getInventory().getItemInOffHand();
        if (isMatchingRod(off, displayName)) {
            off.setAmount(off.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        }
    }

    private boolean isMatchingRod(ItemStack hand, String displayName) {
        return hand != null
                && hand.getType() == Material.FISHING_ROD
                && hand.hasItemMeta()
                && Objects.requireNonNull(hand.getItemMeta()).getDisplayName().equals(displayName);
    }

    public void executeChunkEater(Player player, Location target) {
        if (!cfg.isStrikeEnabled("chunkeater")) {
            player.sendMessage("§cThis orbital strike is disabled.");
            return;
        }
        if (cfg.isWorldDisabled(target.getWorld())) {
            player.sendMessage("§cOrbitals are disabled in this world");
            return;
        }

        if (checkCooldown(player, "chunkeater")) return;
        setCooldown(player, "chunkeater");

        Bukkit.getScheduler().runTask(plugin, () -> chunkEaterStrike.spawn(target.getWorld(), target));
    }

    public void executeStasis(Player player, ItemStack item) {
        if (!cfg.isStrikeEnabled("stasis")) {
            player.sendMessage("§cThis orbital strike is disabled.");
            return;
        }
        if (cfg.isWorldDisabled(player.getWorld())) {
            player.sendMessage("§cOrbitals are disabled in this world");
            return;
        }

        if (checkCooldown(player, "stasis")) return;
        setCooldown(player, "stasis");

        stasisStrike.execute(player, item);
    }
}