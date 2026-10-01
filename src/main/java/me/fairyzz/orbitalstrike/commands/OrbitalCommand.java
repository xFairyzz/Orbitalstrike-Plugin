package me.fairyzz.orbitalstrike.commands;

import me.fairyzz.orbitalstrike.OrbitalStrikePlugin;
import me.fairyzz.orbitalstrike.items.StrikeRodFactory;
import me.fairyzz.orbitalstrike.strikes.StasisStrike;
import me.fairyzz.orbitalstrike.strikes.TotemStasis;
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

    private static final String[] STRIKE_TYPES = {"nuke", "stab", "dogs", "chunkeater", "stasis", "wither", "totem"};
    private static final List<String> COORD_TYPES = List.of("stasis", "totem");

    private final OrbitalStrikePlugin plugin;
    private final StrikeRodFactory rodFactory;
    private final StasisStrike stasisStrike;
    private final TotemStasis totemStasis;

    public OrbitalCommand(OrbitalStrikePlugin plugin) {
        this.plugin = plugin;
        this.rodFactory = new StrikeRodFactory(plugin);
        this.stasisStrike = new StasisStrike(plugin);
        this.totemStasis = new TotemStasis(plugin);
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

        if (COORD_TYPES.contains(type)) {
            return giveCoordItem(sender, player, type, args, 1);
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /orbital " + type);
            return true;
        }
        player.getInventory().addItem(rodFactory.create(type));
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

        if (COORD_TYPES.contains(type)) {
            return giveCoordItem(sender, target, type, args, 3);
        }

        if (args.length != 3) {
            sender.sendMessage("§cUsage: /orbital give <player> " + type);
            return true;
        }
        target.getInventory().addItem(rodFactory.create(type));
        sender.sendMessage("§aGave " + type + " rod to " + target.getName());
        return true;
    }

    private boolean giveCoordItem(CommandSender sender, Player target, String type, String[] args, int coordIndex) {
        if (args.length != coordIndex + 3) {
            String prefix = coordIndex == 1 ? "/orbital " + type : "/orbital give <player> " + type;
            sender.sendMessage("§cUsage: " + prefix + " <x> <y> <z>");
            return true;
        }
        try {
            double x = Double.parseDouble(args[coordIndex]);
            double y = Double.parseDouble(args[coordIndex + 1]);
            double z = Double.parseDouble(args[coordIndex + 2]);
            if (type.equals("totem")) {
                totemStasis.give(target, x, y, z);
            } else {
                stasisStrike.storeLocation(target.getUniqueId(), new Location(target.getWorld(), x, y, z));
                target.getInventory().addItem(stasisStrike.create(x, y, z));
            }
            if (sender != target) {
                sender.sendMessage("§aGave " + type + " to " + target.getName());
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid coordinates!");
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
            if (args.length >= 4 && COORD_TYPES.contains(args[2].toLowerCase())) {
                return coordHint(args.length - 3);
            }
            return Collections.emptyList();
        }

        if (args.length > 1 && COORD_TYPES.contains(args[0].toLowerCase())) {
            return coordHint(args.length - 1);
        }

        return Collections.emptyList();
    }

    private List<String> coordHint(int coordArg) {
        if (coordArg >= 4) return Collections.emptyList();
        if (coordArg == 1) return List.of("<x>");
        if (coordArg == 2) return List.of("<y>");
        if (coordArg == 3) return List.of("<z>");
        return Collections.emptyList();
    }

    private boolean isValidType(String type) {
        if (!Arrays.asList(STRIKE_TYPES).contains(type)) return true;
        return !plugin.getPluginConfig().isStrikeEnabled(type);
    }
}
