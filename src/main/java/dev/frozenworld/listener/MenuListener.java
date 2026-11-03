package dev.frozenworld.listener;

import dev.frozenworld.menu.MenuHolder;
import dev.frozenworld.menu.WinterMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class MenuListener implements Listener {
    private final WinterMenu menu;
    public MenuListener(WinterMenu menu) { this.menu = menu; }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        switch (event.getSlot()) {
            case 10 -> menu.manager().toggleWinter();
            case 11 -> menu.manager().toggleTime();
            case 12 -> menu.manager().toggleSnow();
            case 13 -> menu.manager().toggleWaterFreeze();
            case 14 -> menu.manager().changeSnowLayers(event.isRightClick() ? -1 : 1);
            default -> { return; }
        }
        menu.open(player);
    }
}
