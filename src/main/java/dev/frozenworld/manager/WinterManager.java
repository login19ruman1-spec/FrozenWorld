package dev.frozenworld.manager;

import dev.frozenworld.FrozenWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public final class WinterManager {
    private final FrozenWorldPlugin plugin;
    private final Map<World, Boolean> previousDaylightRules = new HashMap<>();
    private BukkitTask snowTask;
    private boolean frozen;
    private boolean snowEnabled;
    private int snowLayers;

    public WinterManager(FrozenWorldPlugin plugin) {
        this.plugin = plugin;
        this.frozen = plugin.getConfig().getBoolean("winter.start-frozen", false);
        this.snowEnabled = plugin.getConfig().getBoolean("snow.enabled", true);
        this.snowLayers = Math.max(1, Math.min(8,
                plugin.getConfig().getInt("snow.layers-per-cycle", 1)));
    }

    public void start() {
        if (frozen) {
            freeze();
        }
        restartSnowTask();
    }

    public void stop() {
        if (snowTask != null) {
            snowTask.cancel();
            snowTask = null;
        }
        unfreeze();
    }

    public void toggleWinter() {
        if (frozen) {
            unfreeze();
        } else {
            freeze();
        }
    }

    public void freeze() {
        if (frozen) return;
        frozen = true;

        for (World world : getTargetWorlds()) {
            Boolean rule = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
            previousDaylightRules.put(world, rule != null && rule);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);

            if (plugin.getConfig().getBoolean("winter.snowfall-on-freeze", true)) {
                world.setStorm(true);
                world.setWeatherDuration(plugin.getConfig().getInt("winter.weather-duration", 12000));
            }
        }
    }

    public void unfreeze() {
        if (!frozen && previousDaylightRules.isEmpty()) return;
        frozen = false;

        for (Map.Entry<World, Boolean> entry : previousDaylightRules.entrySet()) {
            World world = entry.getKey();
            if (world.isChunkLoaded(0, 0) || Bukkit.getWorld(world.getUID()) != null) {
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, entry.getValue());
            }
        }
        previousDaylightRules.clear();
    }

    public void toggleSnow() {
        setSnowEnabled(!snowEnabled);
    }

    public void setSnowEnabled(boolean enabled) {
        snowEnabled = enabled;
        plugin.getConfig().set("snow.enabled", enabled);
        plugin.saveConfig();
        restartSnowTask();
    }

    public void changeSnowLayers(int delta) {
        snowLayers = Math.max(1, Math.min(8, snowLayers + delta));
        plugin.getConfig().set("snow.layers-per-cycle", snowLayers);
        plugin.saveConfig();
    }

    private void restartSnowTask() {
        if (snowTask != null) {
            snowTask.cancel();
            snowTask = null;
        }
        if (!snowEnabled) return;

        long seconds = Math.max(5,
                plugin.getConfig().getLong("snow.accumulation-interval-seconds", 30));
        snowTask = Bukkit.getScheduler().runTaskTimer(plugin, this::accumulateSnow,
                20L * seconds, 20L * seconds);
    }

    private void accumulateSnow() {
        if (!frozen || !snowEnabled) return;

        // Snow layers are added only to safe, exposed locations. Chunk/world access stays on the server thread.
        for (World world : getTargetWorlds()) {
            if (!world.hasStorm()) continue;
            for (var player : world.getPlayers()) {
                var center = player.getLocation();
                int radius = 8;
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if ((x * x) + (z * z) > radius * radius) continue;
                        var ground = world.getHighestBlockAt(center.getBlockX() + x,
                                center.getBlockZ() + z);
                        var above = ground.getRelative(0, 1, 0);
                        if (!above.getType().isAir()) continue;
                        if (ground.getLightFromSky() < 9) continue;
                        if (ground.getType().isSolid() && ground.getType().isOccluding()) {
                            above.setType(org.bukkit.Material.SNOW, false);
                            if (snowLayers > 1) {
                                var data = (org.bukkit.block.data.type.Snow) above.getBlockData();
                                data.setLayers(snowLayers);
                                above.setBlockData(data, false);
                            }
                        }
                    }
                }
            }
        }
    }

    private java.util.List<World> getTargetWorlds() {
        var configured = plugin.getConfig().getStringList("worlds.enabled");
        if (configured.isEmpty()) return Bukkit.getWorlds();
        return configured.stream().map(Bukkit::getWorld).filter(java.util.Objects::nonNull).toList();
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isSnowEnabled() {
        return snowEnabled;
    }

    public int getSnowLayers() {
        return snowLayers;
    }
}
