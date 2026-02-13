package me.fairyzz.orbitalstrike;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityPlaceEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class OrbitalStrikePlugin extends JavaPlugin implements CommandExecutor, Listener, TabCompleter {

    private static final int CUSTOM_MODEL_DATA = 12345;
    private static final String[] STRIKE_TYPES = {"nuke", "stab", "dogs", "chunkeater", "stasis"};

    private final Map<UUID, Set<UUID>> strikeTNT = new HashMap<>();
    private final Map<UUID, String> pendingStrikes = new HashMap<>();
    private FileConfiguration config;

    private static final String GITHUB_REPO = "xFairyzz/Orbitalstrike-Plugin";
    private static final String CURRENT_VERSION = "v1.6.1";
    private boolean hasUpdate = false;
    private String latestVersion = "";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        setDefaults();
        saveConfig();

        Objects.requireNonNull(getCommand("orbital")).setExecutor(this);
        Objects.requireNonNull(getCommand("orbital")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);

        checkForUpdate();
        Bukkit.getScheduler().runTaskLater(this, this::sendConsoleUpdate, 40L);
    }

    private void checkForUpdate() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String releasesUrl = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
                URL url = new URL(releasesUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    getLogger().warning("§e[OrbitalStrike] GitHub API ERROR: HTTP " + responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                String tagName = extractTagName(json);

                assert tagName != null;
                String normalizedTag = tagName.toLowerCase().startsWith("v") ? tagName.substring(1) : tagName;
                String normalizedCurrent = CURRENT_VERSION.toLowerCase().startsWith("v") ? CURRENT_VERSION.substring(1) : CURRENT_VERSION;

                if (!normalizedTag.equalsIgnoreCase(normalizedCurrent)) {
                    hasUpdate = true;
                    latestVersion = tagName;
                }
            } catch (Exception e) {
                getLogger().warning("§e[OrbitalStrike] Update-Check Failed: " + e.getMessage());
            }
        });
    }

    private String extractTagName(String json) {
        try {
            int tagIndex = json.indexOf("\"tag_name\":\"");
            if (tagIndex == -1) return null;

            int start = tagIndex + 12;
            int end = json.indexOf("\"", start);
            if (end == -1) return null;

            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendUpdateNotification(Player player) {
        if (hasUpdate && player.isOp()) {
            player.sendMessage("§c[OrbitalStrike] §fUpdate available! §cCurrent: " + CURRENT_VERSION + " → §aLatest: " + latestVersion);
            player.sendMessage("§cDownload: §Fhttps://modrinth.com/plugin/orbitalstrike-plugin");
        }
    }

    private void sendConsoleUpdate() {
        if (hasUpdate) {
            getLogger().warning("\u001B[31mUPDATE AVAILABLE! Current: " + CURRENT_VERSION + " → Latest: " + latestVersion + "\u001B[0m");
            getLogger().warning("\u001B[31mDownload: https://modrinth.com/plugin/orbitalstrike-plugin\u001B[0m");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> sendUpdateNotification(event.getPlayer()), 20L);
    }

    @Override
    public void onDisable() {
        strikeTNT.clear();
        pendingStrikes.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!hasPermission(player)) {
            sendMessage(player, "no-permission", Map.of());
            return true;
        }

        if (args.length == 0) {
            sendMessage(player, "usage", Map.of("{CMD}", "/orbital <nuke|stab|dogs|stasis>"));
            return true;
        }

        String type = args[0].toLowerCase();
        if (!isValidStrikeType(type)) {
            sendMessage(player, "invalid-type", Map.of());
            return true;
        }

        if (type.equals("stasis")) {
            if (args.length != 4) {
                sendMessage(player, "usage", Map.of("{CMD}", "/orbital stasis <x> <y> <z>"));
                return true;
            }
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                giveStrikeRod(player, type, x, y, z);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid coordinates!");
                return true;
            }
        } else {
            if (args.length != 1) {
                sendMessage(player, "usage", Map.of("{CMD}", "/orbital <nuke|stab|dogs>"));
                return true;
            }
            giveStrikeRod(player, type);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("orbital")) {
            return null;
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Arrays.stream(STRIKE_TYPES)
                    .filter(type -> type.startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length > 1) {
            String type = args[0].toLowerCase();
            if (type.startsWith("stasis")) {
                if (args.length >= 5) {
                    return Collections.emptyList();
                }

                List<String> completions = new ArrayList<>();
                if (args.length == 2) completions.add("<x>");
                if (args.length == 3) completions.add("<y>");
                if (args.length == 4) completions.add("<z>");
                return completions;
            }
        }

        return Collections.emptyList();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT") || !event.hasItem()) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isStrikeRod(item)) {
            return;
        }

        String type = getStrikeType(item);
        if (type == null) return;

        Player player = event.getPlayer();

        if (type.equals("chunkeater")) {
            return;
        }

        if (type.equals("stasis")) {
            boolean throwRod = config.getBoolean("rod.throw-rod", true);

            if (throwRod) {
                pendingStrikes.put(player.getUniqueId(), type);
            } else {
                executeStasisStrike(player, item);
                event.setCancelled(true);
            }
            return;
        }

        Location target = getTargetLocation(player);

        if (target == null) {
            sendMessage(player, "no-target", Map.of());
            return;
        }

        boolean throwRod = config.getBoolean("rod.throw-rod", true);

        if (throwRod) {
            pendingStrikes.put(player.getUniqueId(), type);
        } else {
            executeStrike(player, item, type, target);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!config.getBoolean("rod.throw-rod", true)) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!pendingStrikes.containsKey(playerId)) {
            return;
        }

        if (event.getState() != PlayerFishEvent.State.REEL_IN &&
                event.getState() != PlayerFishEvent.State.FAILED_ATTEMPT &&
                event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY &&
                event.getState() != PlayerFishEvent.State.IN_GROUND &&
                event.getState() != PlayerFishEvent.State.BITE) {
            return;
        }

        String type = pendingStrikes.remove(playerId);

        if (type.equals("stasis")) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();

            boolean consumed = false;
            if (mainHand.getType() == Material.FISHING_ROD && isStrikeRod(mainHand)) {
                executeStasisStrike(player, mainHand);
                mainHand.setAmount(mainHand.getAmount() - 1);
                consumed = true;
            } else if (offHand.getType() == Material.FISHING_ROD && isStrikeRod(offHand)) {
                executeStasisStrike(player, offHand);
                offHand.setAmount(offHand.getAmount() - 1);
                consumed = true;
            }

            if (consumed) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_TELEPORT, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            }
            return;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean consumed = false;
        if (mainHand.getType() == Material.FISHING_ROD && isStrikeRod(mainHand)) {
            mainHand.setAmount(mainHand.getAmount() - 1);
            consumed = true;
        } else if (offHand.getType() == Material.FISHING_ROD && isStrikeRod(offHand)) {
            offHand.setAmount(offHand.getAmount() - 1);
            consumed = true;
        }

        if (consumed) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }

        Location target = getTargetLocation(player);
        if (target == null) return;

        executeStrike(player, new ItemStack(Material.FISHING_ROD), type, target);
    }

    @EventHandler
    public void onTNTLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }

        UUID tntId = tnt.getUniqueId();
        if (!isTrackedTNT(tntId)) {
            return;
        }

        event.setCancelled(true);

        if (event.getBlock().getType().isSolid()) {
            removeFromTracking(tntId);
            scheduleExplosion(tnt);
        }
    }

    @EventHandler
    public void onArmorStandPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand)) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.ARMOR_STAND) {
            item = player.getInventory().getItemInOffHand();
        }

        if (!isStrikeRod(item) || !Objects.equals(getStrikeType(item), "chunkeater")) {
            return;
        }

        Location target = armorStand.getLocation();
        armorStand.remove();
        sendMessage(player, "incoming", Map.of("{TYPE}", "CHUNKEATER"));
        executeChunkEaterStrike(player, target);
    }

    private void executeChunkEaterStrike(Player player, Location target) {
        Bukkit.getScheduler().runTask(this, () -> {
            spawnChunkEater(target.getWorld(), target);
        });
    }

    private void executeStasisStrike(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(new NamespacedKey(this, "stasis_x"), PersistentDataType.DOUBLE) ||
                !meta.getPersistentDataContainer().has(new NamespacedKey(this, "stasis_y"), PersistentDataType.DOUBLE) ||
                !meta.getPersistentDataContainer().has(new NamespacedKey(this, "stasis_z"), PersistentDataType.DOUBLE)) {
            return;
        }

        double x = meta.getPersistentDataContainer().get(new NamespacedKey(this, "stasis_x"), PersistentDataType.DOUBLE);
        double y = meta.getPersistentDataContainer().get(new NamespacedKey(this, "stasis_y"), PersistentDataType.DOUBLE);
        double z = meta.getPersistentDataContainer().get(new NamespacedKey(this, "stasis_z"), PersistentDataType.DOUBLE);

        Location teleportLoc = new Location(player.getWorld(), x, y, z);
        player.teleport(teleportLoc);
        player.playSound(teleportLoc, Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.0f);

        item.setAmount(item.getAmount() - 1);
    }

    private void executeStrike(Player player, ItemStack item, String type, Location target) {
        sendMessage(player, "incoming", Map.of("{TYPE}", type.toUpperCase()));

        if (!type.equals("chunkeater") && !config.getBoolean("rod.throw-rod", true)) {
            consumeRodDelayed(player, item);
        }

        UUID strikeId = UUID.randomUUID();
        Set<UUID> tntList = new HashSet<>();
        strikeTNT.put(strikeId, tntList);
        Bukkit.getScheduler().runTask(this, () -> {
            switch (type) {
                case "nuke" -> spawnNuke(target.getWorld(), target, strikeId, tntList);
                case "stab" -> spawnStab(target.getWorld(), target);
                case "dogs" -> spawnDogs(target.getWorld(), target, player);
                case "chunkeater" -> spawnChunkEater(target.getWorld(), target);
            }

            Bukkit.getScheduler().runTaskLater(this, () -> strikeTNT.remove(strikeId), 200L);
        });
    }

    private void consumeRodDelayed(Player player, ItemStack originalItem) {
        String displayName = Objects.requireNonNull(originalItem.getItemMeta()).getDisplayName();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (consumeFromHand(player.getInventory().getItemInMainHand(), displayName, player)) {
                return;
            }
            consumeFromHand(player.getInventory().getItemInOffHand(), displayName, player);
        }, 1L);
    }

    private boolean consumeFromHand(ItemStack hand, String displayName, Player player) {
        if (hand.getType() == Material.FISHING_ROD &&
                hand.hasItemMeta() &&
                Objects.requireNonNull(hand.getItemMeta()).getDisplayName().equals(displayName)) {

            hand.setAmount(hand.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return true;
        }
        return false;
    }

    private void spawnNuke(World world, Location center, UUID strikeId, Set<UUID> tntList) {
        int rings = config.getInt("nuke.rings", 10);
        double height = center.getY() + config.getInt("nuke.height", 80);
        float yield = (float) config.getDouble("nuke.yield", 6.0);
        int baseTnt = config.getInt("nuke.tnt-per-ring-base", 40);
        int increase = config.getInt("nuke.tnt-per-ring-increase", 2);
        boolean centerTnt = config.getBoolean("nuke.center-tnt", true);
        boolean animatedRings = config.getBoolean("nuke.Animated-rings", true);

        Location centerLoc = new Location(world, center.getX() + 0.5, height, center.getZ() + 0.5);

        if (animatedRings) {
            UUID centerId;
            if (centerTnt) {
                TNTPrimed ct = (TNTPrimed) world.spawnEntity(centerLoc.clone(), EntityType.TNT);
                ct.setFuseTicks(10000);
                ct.setVelocity(new Vector(0, 0, 0));
                ct.setGravity(false);
                ct.setYield(yield);
                ct.setInvulnerable(true);
                centerId = ct.getUniqueId();
                tntList.add(centerId);

                // Gravity-Drop für Center
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    for (Entity e : world.getNearbyEntities(centerLoc, 100, 100, 100)) {
                        if (e instanceof TNTPrimed tnt && tnt.getUniqueId().equals(centerId) && !tnt.isDead()) {
                            tnt.setGravity(true);
                            break;
                        }
                    }
                }, 30L);
            } else {
                centerId = null;
            }

            for (int ring = 1; ring <= rings; ring++) {
                double radius = ring * 4.0;
                int originalCount = baseTnt + ring * increase;
                int tntCount = originalCount;

                boolean splitRings = config.getBoolean("nuke.full-rings", false);
                if (splitRings && originalCount > 15) {
                    double minRemove = config.getDouble("nuke.full-rings-min-remove", 0.20);
                    double maxRemove = config.getDouble("nuke.full-rings-max-remove", 0.35);
                    double removePercent = minRemove + (Math.random() * (maxRemove - minRemove));
                    int remove = (int) (originalCount * removePercent);
                    remove = Math.min(remove, originalCount - 10);
                    tntCount = originalCount - remove;
                }

                double step = 360.0 / originalCount;

                List<Integer> indices = new ArrayList<>();
                for (int j = 0; j < originalCount; j++) indices.add(j);

                if (splitRings && tntCount < originalCount) {
                    Collections.shuffle(indices);
                    indices = indices.subList(0, tntCount);
                    Collections.sort(indices); // Für optisch schöneren Ring
                }

                // TNT pro Ring spawnen
                for (int idx : indices) {
                    double angle = idx * step + (ring * 10);
                    double tx = center.getX() + radius * Math.cos(Math.toRadians(angle));
                    double tz = center.getZ() + radius * Math.sin(Math.toRadians(angle));
                    double targetX = Math.round(tx * 10) / 10.0 + 0.5;
                    double targetZ = Math.round(tz * 10) / 10.0 + 0.5;

                    TNTPrimed tnt = (TNTPrimed) world.spawnEntity(centerLoc.clone(), EntityType.TNT);
                    tnt.setFuseTicks(10000);
                    tnt.setVelocity(new Vector(0, 0, 0));
                    tnt.setGravity(false);
                    tnt.setYield(yield);
                    tnt.setInvulnerable(true);
                    tntList.add(tnt.getUniqueId());

                    final double fx = targetX, fz = targetZ;
                    final UUID tntUuid = tnt.getUniqueId();
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        for (Entity e : world.getNearbyEntities(centerLoc, 200, 200, 200)) {
                            if (e instanceof TNTPrimed rtnt && rtnt.getUniqueId().equals(tntUuid) && !rtnt.isDead()) {
                                rtnt.setVelocity(getVector(fx, centerLoc, fz));
                                rtnt.setGravity(true);
                                break;
                            }
                        }
                    }, 30L);
                }
            }

            // Fallback-Explosion (als normale Schleife, keine Lambda-Probleme)
            int fuseTicks = config.getInt("nuke.fuse-fallback-ticks", 160);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                List<UUID> toRemove = new ArrayList<>();
                for (UUID id : tntList) {
                    boolean exploded = false;
                    for (Entity e : world.getNearbyEntities(centerLoc, 200, 200, 200)) {
                        if (e instanceof TNTPrimed tnt && tnt.getUniqueId().equals(id) && !tnt.isDead()) {
                            tnt.setFuseTicks(1);
                            exploded = true;
                            break;
                        }
                    }
                    if (exploded) toRemove.add(id);
                }
                toRemove.forEach(tntList::remove);
            }, fuseTicks);

        } else {
            // Nicht-animierte Version (direkt spawnen, wie alt)
            if (centerTnt) {
                spawnNukeTNT(world, centerLoc.clone(), yield, strikeId, tntList);
            }
            for (int ring = 1; ring <= rings; ring++) {
                double radius = ring * 4.0;
                int tntCount = baseTnt + ring * increase;
                double step = 360.0 / tntCount;
                double startAngle = ring * 13.0;
                for (int i = 0; i < tntCount; i++) {
                    double angle = startAngle + i * step;
                    double x = center.getX() + radius * Math.cos(Math.toRadians(angle));
                    double z = center.getZ() + radius * Math.sin(Math.toRadians(angle));
                    double roundedX = Math.round(x * 10) / 10.0 + 0.5;
                    double roundedZ = Math.round(z * 10) / 10.0 + 0.5;
                    Location loc = new Location(world, roundedX, height, roundedZ);
                    spawnNukeTNT(world, loc, yield, strikeId, tntList);
                }
            }
        }
    }

    private static Vector getVector(double finalTargetX, Location centerLoc, double finalTargetZ) {
        double deltaX = finalTargetX - centerLoc.getX();
        double deltaZ = finalTargetZ - centerLoc.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double speed = distance / 30.0;

        return new Vector(deltaX / distance * speed, 0, deltaZ / distance * speed);
    }

    private void spawnNukeTNT(World world, Location loc, float yield, UUID strikeId, Set<UUID> tntList) {
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        if (!world.isChunkLoaded(cx, cz)) return;

        TNTPrimed tnt = (TNTPrimed) world.spawnEntity(loc, EntityType.TNT);
        tnt.setFuseTicks(10000);
        tnt.setVelocity(new Vector(0, -0.8, 0));
        tnt.setGravity(true);
        tnt.setYield(yield);
        tnt.setInvulnerable(true);

        UUID tntId = tnt.getUniqueId();
        tntList.add(tntId);

        int fuseFallbackTicks = config.getInt("nuke.fuse-fallback-ticks", 160);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (tntList.contains(tntId) && !tnt.isDead()) {
                tnt.setFuseTicks(1);
                tntList.remove(tntId);
            }
        }, fuseFallbackTicks);
    }

    private void spawnStab(World world, Location center) {
        Location ground = findGroundLevel(world, center);
        float yield = (float) config.getDouble("stab.yield", 8.0);
        double offset = config.getDouble("stab.tnt-offset", 0.3);

        int y = (int) ground.getY();
        int minY = world.getMinHeight();

        while (y >= minY) {
            Location loc = new Location(world, ground.getX(), y, ground.getZ());
            spawnTNTAt(world, loc.clone().add(offset, 0, offset), yield);
            spawnTNTAt(world, loc.clone().subtract(offset, 0, offset), yield);
            y -= 2;
        }
    }

    private void spawnTNTAt(World world, Location loc, float yield) {
        if (loc.getBlock().isLiquid()) return;

        TNTPrimed tnt = (TNTPrimed) world.spawnEntity(loc, EntityType.TNT);
        tnt.setFuseTicks(0);
        tnt.setYield(yield);
    }

    private void spawnDogs(World world, Location center, Player owner) {
        int count = config.getInt("dogs.count", 50);
        double radius = config.getDouble("dogs.radius", 5.0);
        int durationTicks = config.getInt("dogs.effect-duration", 2400);

        List<String> effectStrings = config.getStringList("dogs.effects");
        List<PotionEffect> effects = new ArrayList<>();

        for (int i = 0; i < Math.min(effectStrings.size(), 3); i++) {
            String entry = effectStrings.get(i).trim().toUpperCase();
            String[] parts = entry.split(":");
            if (parts.length != 2) continue;

            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            if (type == null) continue;

            int amplifier;
            try {
                amplifier = Integer.parseInt(parts[1]) - 1;
            } catch (NumberFormatException e) {
                continue;
            }

            if (amplifier < 0) amplifier = 0;
            effects.add(new PotionEffect(type, durationTicks, amplifier));
        }

        if (effects.isEmpty()) {
            effects.add(new PotionEffect(PotionEffectType.SPEED, durationTicks, 1));
        }

        Location ground = findGroundLevel(world, center);

        for (int i = 0; i < count; i++) {
            double angle = Math.random() * 360;
            double dist = Math.random() * radius;
            double x = ground.getX() + dist * Math.cos(Math.toRadians(angle));
            double z = ground.getZ() + dist * Math.sin(Math.toRadians(angle));

            Location spawnLoc = findGroundLevel(world, new Location(world, x, ground.getY(), z));
            if (spawnLoc.getBlock().isLiquid()) continue;

            Wolf wolf = (Wolf) world.spawnEntity(spawnLoc, EntityType.WOLF);
            wolf.setTamed(true);
            wolf.setOwner(owner);
            wolf.setSitting(false);
            wolf.setCollarColor(DyeColor.RED);

            ItemStack wolfArmor = new ItemStack(Material.WOLF_ARMOR);
            Objects.requireNonNull(wolf.getEquipment()).setItem(EquipmentSlot.BODY, wolfArmor);

            for (PotionEffect effect : effects) {
                wolf.addPotionEffect(effect);
            }
        }
    }

    private void spawnChunkEater(World world, Location center) {
        Location ground = findGroundLevel(world, center);
        Chunk chunk = ground.getChunk();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        int chunkX = chunk.getX() << 4;
        int chunkZ = chunk.getZ() << 4;

        int tntAmount = (250);
        Random random = new Random();

        for (int i = 0; i < tntAmount; i++) {
            double x = chunkX + random.nextDouble() * 16;
            double z = chunkZ + random.nextDouble() * 16;
            double y = center.getY() + 35;

            Location tntLoc = new Location(world, x, y, z);
            TNTPrimed tnt = (TNTPrimed) world.spawnEntity(tntLoc, EntityType.TNT);
            tnt.setFuseTicks(90);
            tnt.setYield(2.0f);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!chunk.isLoaded()) chunk.load();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minY + 1; y < maxY; y++) {

                        Material blockType = chunk.getBlock(x, y, z).getType();
                        if (blockType != Material.BEDROCK) {
                            chunk.getBlock(x, y, z).setType(Material.AIR, false);
                        }
                    }
                }
            }
        }, 100L);
    }

    private boolean hasPermission(Player player) {
        return player.hasPermission(config.getString("permission", "orbital.use"));
    }

    private boolean isValidStrikeType(String type) {
        return Arrays.asList(STRIKE_TYPES).contains(type);
    }

    private void giveStrikeRod(Player player, String type) {
        ItemStack rod = createStrikeRod(type);
        player.getInventory().addItem(rod);
        sendMessage(player, "received", Map.of("{TYPE}", type.toUpperCase()));
    }

    private void giveStrikeRod(Player player, String type, double x, double y, double z) {
        ItemStack rod = createStrikeRod(type, x, y, z);
        player.getInventory().addItem(rod);
        sendMessage(player, "received", Map.of("{TYPE}", type.toUpperCase()));
    }

    private ItemStack createStrikeRod(String type) {
        return createStrikeRod(type, 0, 0, 0);
    }

    private ItemStack createStrikeRod(String type, double x, double y, double z) {
        Material material = type.equals("chunkeater") ? Material.ARMOR_STAND : Material.FISHING_ROD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = switch (type) {
            case "nuke" -> "Nuke shot";
            case "stab" -> "Stab shot";
            case "dogs" -> "Dogs";
            case "chunkeater" -> "Chunk Eater";
            case "stasis" -> "Stasis";
            default -> "Orbital Strike Rod";
        };

        assert meta != null;
        meta.setDisplayName(displayName);
        meta.setCustomModelData(CUSTOM_MODEL_DATA);
        if (type.equals("stasis")) {
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "stasis_x"), PersistentDataType.DOUBLE, x);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "stasis_y"), PersistentDataType.DOUBLE, y);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "stasis_z"), PersistentDataType.DOUBLE, z);
        }
        item.setItemMeta(meta);

        if (material == Material.FISHING_ROD) {
            item.setDurability((short) 63);
        }

        return item;
    }

    private boolean isStrikeRod(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (!meta.hasDisplayName() || !meta.hasCustomModelData() || meta.getCustomModelData() != CUSTOM_MODEL_DATA)
            return false;
        Material type = item.getType();
        return type == Material.FISHING_ROD || type == Material.ARMOR_STAND;
    }

    private String getStrikeType(ItemStack item) {
        String displayName = Objects.requireNonNull(item.getItemMeta()).getDisplayName();
        return switch (displayName) {
            case "Nuke shot" -> "nuke";
            case "Stab shot" -> "stab";
            case "Dogs" -> "dogs";
            case "Chunk Eater" -> "chunkeater";
            case "Stasis" -> "stasis";
            default -> null;
        };
    }

    private Location getTargetLocation(Player player) {
        int distance = config.getInt("rod.distance", 100);
        RayTraceResult result = player.rayTraceBlocks(distance);
        if (result == null || result.getHitBlock() == null) return null;
        return result.getHitBlock().getLocation().add(0, 60, 0);
    }

    private Location findGroundLevel(World world, Location start) {
        Location ground = start.clone();
        while (ground.getY() > world.getMinHeight() && ground.getBlock().getType().isAir()) {
            ground.subtract(0, 1, 0);
        }
        return ground.add(0, 1, 0);
    }

    private void scheduleExplosion(TNTPrimed tnt) {
        if (!tnt.isDead()) {
            tnt.setFuseTicks(1);
        }
    }

    private boolean isTrackedTNT(UUID tntId) {
        return strikeTNT.values().stream().anyMatch(set -> set.contains(tntId));
    }

    private void removeFromTracking(UUID tntId) {
        strikeTNT.values().forEach(set -> set.remove(tntId));
    }

    private void sendMessage(Player player, String key, Map<String, String> placeholders) {
        if (!config.getBoolean("messages-enabled", true)) return;
        String path = "messages." + key;
        String message = config.getString(path);
        if (message == null || message.isEmpty()) return;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        player.sendMessage(message);
    }

    private void setDefaults() {
        config.addDefault("messages-enabled", true);
        config.addDefault("permission", "orbital.use");

        setRodDefaults();
        setNukeDefaults();
        setStabDefaults();
        setDogsDefaults();

        config.options().copyDefaults(true);
    }

    private void setRodDefaults() {
        Map<String, Object> rod = new HashMap<>();
        rod.put("distance", 100);
        rod.put("throw-rod", true);
        config.addDefault("rod", rod);
    }

    private void setNukeDefaults() {
        Map<String, Object> nuke = new HashMap<>();
        nuke.put("rings", 10);
        nuke.put("height", 80);
        nuke.put("yield", 6.0);
        nuke.put("tnt-per-ring-base", 40);
        nuke.put("tnt-per-ring-increase", 2);
        nuke.put("center-tnt", true);
        nuke.put("fuse-fallback-ticks", 160);
        nuke.put("Animated-rings", true);
        nuke.put("full-rings", false);
        config.addDefault("nuke", nuke);
    }

    private void setStabDefaults() {
        Map<String, Object> stab = new HashMap<>();
        stab.put("yield", 8.0);
        stab.put("tnt-offset", 0.3);
        config.addDefault("stab", stab);
    }

    private void setDogsDefaults() {
        Map<String, Object> dogs = new HashMap<>();
        dogs.put("count", 50);
        dogs.put("radius", 5.0);
        dogs.put("effect-duration", 2400);

        List<String> effects = new ArrayList<>();
        effects.add("SPEED:1");
        effects.add("STRENGTH:2");
        dogs.put("effects", effects);

        config.addDefault("dogs", dogs);
    }
}
