package dev.frozenworld.manager;

import dev.frozenworld.FrozenWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.ServerTickManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Climate work is deliberately bounded: each pulse processes a limited number
 * of columns from already-loaded chunks. All Bukkit world access stays on the
 * primary server thread.
 */
public final class WinterManager {
    private final FrozenWorldPlugin plugin;
    private final Map<UUID, Boolean> previousDaylightRules = new HashMap<>();
    private final Map<ColumnKey, BlockData> frozenWater = new HashMap<>();
    private BukkitTask climateTask;
    private boolean frozen;
    private boolean snowEnabled;
    private boolean waterFreezeEnabled;
    private boolean timeStopped;
    private int snowLayers;
    private int cursorChunk;
    private int cursorColumn;

    public WinterManager(FrozenWorldPlugin plugin) {
        this.plugin = plugin;
        this.frozen = plugin.getConfig().getBoolean("winter.start-frozen", false);
        this.snowEnabled = plugin.getConfig().getBoolean("snow.enabled", true);
        this.waterFreezeEnabled = plugin.getConfig().getBoolean("water.enabled", true);
        this.snowLayers = clamp(plugin.getConfig().getInt("snow.layers-per-cycle", 1), 1, 8);
        this.timeStopped = plugin.getConfig().getBoolean("time.stop-on-winter", false);
    }

    public void start() {
        if (frozen) applyWinter();
        if (timeStopped) stopTime();
        climateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::climatePulse, 40L, 20L);
    }

    public void stop() {
        if (climateTask != null) climateTask.cancel();
        climateTask = null;
        restoreTime();
        restoreDaylightRules();
    }

    public void toggleWinter() {
        if (frozen) unfreeze(); else freeze();
    }

    public void freeze() {
        if (frozen) return;
        frozen = true;
        applyWinter();
        if (plugin.getConfig().getBoolean("winter.stop-time-on-enable", false)) stopTime();
        saveState();
    }

    public void unfreeze() {
        if (!frozen) return;
        frozen = false;
        restoreDaylightRules();
        restoreFrozenWater();
        if (plugin.getConfig().getBoolean("time.resume-on-winter-disable", true)) restoreTime();
        saveState();
    }

    private void applyWinter() {
        for (World world : targetWorlds()) {
            Boolean old = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
            previousDaylightRules.put(world.getUID(), old == null || old);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            // Do not enable vanilla rain: it renders as rain in temperate biomes.
            world.setStorm(false);
            world.setThundering(false);
        }
    }

    private void restoreDaylightRules() {
        for (World world : targetWorlds()) {
            Boolean old = previousDaylightRules.remove(world.getUID());
            if (old != null) world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, old);
        }
    }

    public void toggleTime() {
        if (isTimeStopped()) restoreTime(); else stopTime();
        plugin.getConfig().set("time.stopped", isTimeStopped());
        plugin.saveConfig();
    }

    public void stopTime() {
        ServerTickManager manager = Bukkit.getServer().getServerTickManager();
        manager.setFrozen(true);
    }

    public void restoreTime() {
        ServerTickManager manager = Bukkit.getServer().getServerTickManager();
        if (manager.isFrozen()) manager.setFrozen(false);
    }

    public boolean isTimeStopped() {
        return Bukkit.getServer().getServerTickManager().isFrozen();
    }

    public void toggleSnow() {
        snowEnabled = !snowEnabled;
        plugin.getConfig().set("snow.enabled", snowEnabled);
        plugin.saveConfig();
    }

    public void toggleWaterFreeze() {
        waterFreezeEnabled = !waterFreezeEnabled;
        if (!waterFreezeEnabled) restoreFrozenWater();
        plugin.getConfig().set("water.enabled", waterFreezeEnabled);
        plugin.saveConfig();
    }

    public void changeSnowLayers(int delta) {
        snowLayers = clamp(snowLayers + delta, 1, 8);
        plugin.getConfig().set("snow.layers-per-cycle", snowLayers);
        plugin.saveConfig();
    }

    private void climatePulse() {
        if (!frozen) return;
        List<org.bukkit.Chunk> chunks = new ArrayList<>();
        for (World world : targetWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) continue;
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) chunks.add(chunk);
        }
        if (chunks.isEmpty()) return;

        int budget = clamp(plugin.getConfig().getInt("performance.columns-per-pulse", 96), 16, 512);
        for (int i = 0; i < budget; i++) {
            if (cursorChunk >= chunks.size()) cursorChunk = 0;
            org.bukkit.Chunk chunk = chunks.get(cursorChunk);
            int localX = cursorColumn & 15;
            int localZ = (cursorColumn >>> 4) & 15;
            cursorColumn++;
            if (cursorColumn >= 256) {
                cursorColumn = 0;
                cursorChunk++;
            }
            int x = chunk.getX() * 16 + localX;
            int z = chunk.getZ() * 16 + localZ;
            if (snowEnabled) accumulateSnow(chunk.getWorld(), x, z);
            if (waterFreezeEnabled) freezeSurfaceWater(chunk.getWorld(), x, z);
        }
    }

    private void accumulateSnow(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) return;
        Block ground = world.getBlockAt(x, y, z);
        if (ground.getType() == Material.SNOW) {
            Snow existing = (Snow) ground.getBlockData();
            if (existing.getLayers() < 8 && !isWarmNearby(ground)) {
                existing.setLayers(Math.min(8, existing.getLayers() + snowLayers));
                ground.setBlockData(existing, false);
            }
            return;
        }
        if (!ground.getType().isSolid() || ground.isLiquid()) return;
        if (ground.getBiome() == Biome.DESERT || ground.getBiome() == Biome.BADLANDS
                || ground.getBiome() == Biome.ERODED_BADLANDS || ground.getBiome() == Biome.WOODED_BADLANDS) return;
        if (isWarmNearby(ground)) return;

        Block above = ground.getRelative(BlockFace.UP);
        if (above.getType() == Material.AIR) {
            if (!above.canPlace(Material.SNOW.createBlockData())) return;
            above.setType(Material.SNOW, false);
            Snow data = (Snow) above.getBlockData();
            data.setLayers(snowLayers);
            above.setBlockData(data, false);
        } else if (above.getType() == Material.SNOW) {
            Snow data = (Snow) above.getBlockData();
            if (data.getLayers() < 8) {
                data.setLayers(Math.min(8, data.getLayers() + snowLayers));
                above.setBlockData(data, false);
            }
        }
    }

    private void freezeSurfaceWater(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        for (int scan = y; scan >= Math.max(world.getMinHeight(), y - 4); scan--) {
            Block block = world.getBlockAt(x, scan, z);
            if (block.getType() == Material.WATER && !isWarmNearby(block)) {
                ColumnKey key = new ColumnKey(world.getUID(), x, scan, z);
                frozenWater.putIfAbsent(key, block.getBlockData().clone());
                block.setType(Material.ICE, false);
                return;
            }
            if (!block.isPassable() && !block.isLiquid()) return;
        }
    }

    private boolean isWarmNearby(Block block) {
        int radius = clamp(plugin.getConfig().getInt("heat.radius", 3), 0, 8);
        if (radius == 0) return false;
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    Material type = block.getRelative(dx, dy, dz).getType();
                    if (type == Material.TORCH || type == Material.WALL_TORCH
                            || type == Material.LANTERN || type == Material.SOUL_LANTERN
                            || type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE
                            || type == Material.FIRE || type == Material.SOUL_FIRE
                            || type == Material.LAVA || type == Material.MAGMA_BLOCK
                            || type == Material.FURNACE || type == Material.BLAST_FURNACE
                            || type == Material.SMOKER) return true;
                }
            }
        }
        return false;
    }

    private void restoreFrozenWater() {
        for (Map.Entry<ColumnKey, BlockData> entry : frozenWater.entrySet()) {
            ColumnKey key = entry.getKey();
            World world = Bukkit.getWorld(key.world());
            if (world == null || !world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) continue;
            Block block = world.getBlockAt(key.x(), key.y(), key.z());
            if (block.getType() == Material.ICE) block.setBlockData(entry.getValue(), false);
        }
        frozenWater.clear();
    }

    private List<World> targetWorlds() {
        List<String> configured = plugin.getConfig().getStringList("worlds.enabled");
        if (configured.isEmpty()) return Bukkit.getWorlds();
        return configured.stream().map(Bukkit::getWorld).filter(java.util.Objects::nonNull).toList();
    }

    private void saveState() {
        plugin.getConfig().set("winter.start-frozen", frozen);
        plugin.saveConfig();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public boolean isFrozen() { return frozen; }
    public boolean isSnowEnabled() { return snowEnabled; }
    public boolean isWaterFreezeEnabled() { return waterFreezeEnabled; }
    public int getSnowLayers() { return snowLayers; }

    private record ColumnKey(UUID world, int x, int y, int z) {}
}
