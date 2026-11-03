package dev.frozenworld;

import dev.frozenworld.command.FrozenWorldCommand;
import dev.frozenworld.listener.MenuListener;
import dev.frozenworld.manager.WinterManager;
import dev.frozenworld.menu.WinterMenu;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrozenWorldPlugin extends JavaPlugin {
    private WinterManager winterManager;
    private WinterMenu winterMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        winterManager = new WinterManager(this);
        winterMenu = new WinterMenu(this, winterManager);

        FrozenWorldCommand command = new FrozenWorldCommand(this, winterMenu);
        getCommand("frozenworld").setExecutor(command);
        getCommand("frozenworld").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new MenuListener(winterMenu), this);

        winterManager.start();
        getLogger().info("FrozenWorld включён. Да начнётся вечная зима!");
    }

    @Override
    public void onDisable() {
        if (winterManager != null) {
            winterManager.stop();
        }
        getLogger().info("FrozenWorld выключен.");
    }

    public WinterManager getWinterManager() {
        return winterManager;
    }
}
