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
    private final WinterManager manager;

    public WinterMenu(FrozenWorldPlugin plugin, WinterManager manager) {
        this.manager = manager;
    }

    public void open(org.bukkit.entity.Player player) {
        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("FrozenWorld — Вечная зима"));
        holder.setInventory(inventory);

        inventory.setItem(10, item(manager.isFrozen() ? Material.BLUE_ICE : Material.SNOW_BLOCK,
                manager.isFrozen() ? "Зима включена" : "Зима выключена",
                "Нажмите, чтобы переключить режим."));
        inventory.setItem(12, item(manager.isSnowEnabled() ? Material.SNOW : Material.POWDER_SNOW,
                manager.isSnowEnabled() ? "Накопление снега: ВКЛ" : "Накопление снега: ВЫКЛ",
                "Нажмите, чтобы переключить накопление."));
        inventory.setItem(14, item(Material.SNOW_BLOCK, "Слои снега: " + manager.getSnowLayers(),
                "ЛКМ: +1 слой", "ПКМ: -1 слой"));
        inventory.setItem(16, item(Material.PACKED_ICE, "Статус FrozenWorld",
                "Зима: " + (manager.isFrozen() ? "активна" : "неактивна"),
                "Снег: " + (manager.isSnowEnabled() ? "включён" : "выключен")));
        player.openInventory(inventory);
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(List.of(lore).stream().map(Component::text).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    public WinterManager manager() {
        return manager;
    }
}
