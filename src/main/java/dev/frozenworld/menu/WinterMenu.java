package dev.frozenworld.menu;

import dev.frozenworld.FrozenWorldPlugin;
import dev.frozenworld.manager.WinterManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class WinterMenu {
    private final FrozenWorldPlugin plugin;
    private final WinterManager manager;

    public WinterMenu(FrozenWorldPlugin plugin, WinterManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(org.bukkit.entity.Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(), 27,
                Component.text("FrozenWorld — Вечная зима"));

        inventory.setItem(10, item(manager.isFrozen() ? Material.BLUE_ICE : Material.SNOW_BLOCK,
                manager.isFrozen() ? "§bЗима включена" : "§eЗима выключена",
                "§7Нажмите, чтобы переключить режим."));
        inventory.setItem(12, item(manager.isSnowEnabled() ? Material.SNOW : Material.POWDER_SNOW,
                manager.isSnowEnabled() ? "§bНакопление снега: ВКЛ" : "§eНакопление снега: ВЫКЛ",
                "§7Нажмите, чтобы переключить накопление."));
        inventory.setItem(14, item(Material.SNOW_BLOCK, "§fСлои снега: " + manager.getSnowLayers(),
                "§7ЛКМ: +1 слой", "§7ПКМ: -1 слой"));
        inventory.setItem(16, item(Material.PACKED_ICE, "§bСтатус FrozenWorld",
                "§7Зима: " + (manager.isFrozen() ? "§aактивна" : "§cнеактивна"),
                "§7Снег: " + (manager.isSnowEnabled() ? "§aвключён" : "§cвыключен")));
        player.openInventory(inventory);
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name.replaceAll("§[0-9a-fk-or]", "")));
        meta.lore(List.of(lore).stream()
                .map(line -> Component.text(line.replaceAll("§[0-9a-fk-or]", ""))).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    public WinterManager manager() {
        return manager;
    }
}
