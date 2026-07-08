package me.fairyzz.orbitalstrike.commands;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.items.StrikeRodFactory;
import me.fairyzz.orbitalstrike.strikes.StasisStrike;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrbitalCommand implements CommandExecutor, TabCompleter {

    private static final String[] STRIKE_TYPES = {"nuke", "stab", "dogs", "chunkeater", "stasis"};

    private final OrbitalStrikePlugin plugin;
    private final StrikeRodFactory rodFactory;
    private final StasisStrike stasisStrike;

    public OrbitalCommand(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.rodFactory = new StrikeRodFactory(plugin);
        this.stasisStrike = new StasisStrike(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /orbital <type> or /orbital give <player> <type> [x y z]");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            return handleGive(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission(plugin.getPluginConfig().getPermission())) {
            player.sendMessage("§cYou don't have permission!");
            return true;
        }

        String type = args[0].toLowerCase();
        if (isValidType(type)) {
            player.sendMessage("§cInvalid strike type.");
            return true;
        }

        if (type.equals("stasis")) {
            if (args.length != 4) {
                player.sendMessage("§cUsage: /orbital stasis <x> <y> <z>");
                return true;
            }
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                stasisStrike.storeLocation(player.getUniqueId(), new Location(player.getWorld(), x, y, z));
                player.getInventory().addItem(stasisStrike.create(x, y, z));
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid coordinates!");
            }
        } else {
            if (args.length != 1) {
                player.sendMessage("§cUsage: /orbital " + type);
                return true;
            }
            player.getInventory().addItem(rodFactory.create(type));
        }

        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orbital.give")) {
            sender.sendMessage("§cYou don't have permission to give rods!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /orbital give <player> <type> [x y z]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return true;
        }

        String type = args[2].toLowerCase();
        if (isValidType(type)) {
            sender.sendMessage("§cInvalid strike type.");
            return true;
        }

        if (type.equals("stasis")) {
            if (args.length != 6) {
                sender.sendMessage("§cUsage: /orbital give <player> stasis <x> <y> <z>");
                return true;
            }
            try {
                double x = Double.parseDouble(args[3]);
                double y = Double.parseDouble(args[4]);
                double z = Double.parseDouble(args[5]);
                stasisStrike.storeLocation(target.getUniqueId(), new Location(target.getWorld(), x, y, z));
                target.getInventory().addItem(stasisStrike.create(x, y, z));
                sender.sendMessage("§aGave Stasis rod to " + target.getName());
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid coordinates!");
            }
        } else {
            if (args.length != 3) {
                sender.sendMessage("§cUsage: /orbital give <player> " + type);
                return true;
            }
            target.getInventory().addItem(rodFactory.create(type));
            sender.sendMessage("§aGave " + type + " rod to " + target.getName());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("orbital")) return null;

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> opts = new ArrayList<>();
            for (String t : STRIKE_TYPES) {
                if (plugin.getPluginConfig().isStrikeEnabled(t)) {
                    opts.add(t);
                }
            }
            opts.add("give");
            return opts.stream().filter(t -> t.startsWith(input)).sorted().collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                String input = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(input))
                        .sorted()
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                String input = args[2].toLowerCase();
                List<String> enabled = new ArrayList<>();
                for (String t : STRIKE_TYPES) {
                    if (plugin.getPluginConfig().isStrikeEnabled(t)) {
                        enabled.add(t);
                    }
                }
                return enabled.stream()
                        .filter(t -> t.startsWith(input))
                        .sorted()
                        .collect(Collectors.toList());
            }
            if (args.length >= 4 && args[2].equalsIgnoreCase("stasis")) {
                if (args.length >= 6) return Collections.emptyList();
                List<String> hints = new ArrayList<>();
                if (args.length == 4) hints.add("<x>");
                if (args.length == 5) hints.add("<y>");
                if (args.length == 6) hints.add("<z>");
                return hints;
            }
            return Collections.emptyList();
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("stasis")) {
            if (args.length >= 5) return Collections.emptyList();
            List<String> hints = new ArrayList<>();
            if (args.length == 2) hints.add("<x>");
            if (args.length == 3) hints.add("<y>");
            if (args.length == 4) hints.add("<z>");
            return hints;
        }

        return Collections.emptyList();
    }

    private boolean isValidType(String type) {
        if (!Arrays.asList(STRIKE_TYPES).contains(type)) return true;
        return !plugin.getPluginConfig().isStrikeEnabled(type);
    }
}