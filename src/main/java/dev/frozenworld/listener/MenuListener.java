package dev.frozenworld.listener;

import dev.frozenworld.menu.MenuHolder;
import dev.frozenworld.menu.WinterMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class MenuListener implements Listener {
    private final WinterMenu menu;

    public MenuListener(WinterMenu menu) {
        this.menu = menu;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        switch (event.getSlot()) {
            case 10 -> {
                menu.manager().toggleWinter();
                player.sendMessage("§b[FrozenWorld] §fРежим зимы: "
                        + (menu.manager().isFrozen() ? "§aвключён" : "§eвыключен"));
            }
            case 12 -> {
                menu.manager().toggleSnow();
                player.sendMessage("§b[FrozenWorld] §fНакопление снега: "
                        + (menu.manager().isSnowEnabled() ? "§aвключено" : "§eвыключено"));
            }
            case 14 -> {
                int delta = event.isRightClick() ? -1 : 1;
                menu.manager().changeSnowLayers(delta);
                player.sendMessage("§b[FrozenWorld] §fСлоёв снега: "
                        + menu.manager().getSnowLayers());
            }
            default -> { return; }
        }
        menu.open(player);
    }
}
