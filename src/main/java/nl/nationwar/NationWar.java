package nl.nationwar;

import nl.nationwar.commands.ClaimCommand;
import nl.nationwar.commands.NationCommand;
import nl.nationwar.commands.WarCommand;
import nl.nationwar.listeners.ProtectionListener;
import nl.nationwar.managers.NationManager;
import nl.nationwar.managers.WarManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NationWar extends JavaPlugin {

    private NationManager nationManager;
    private WarManager warManager;

    @Override
    public void onEnable() {
        this.nationManager = new NationManager(this);
        this.warManager = new WarManager(this, nationManager);
        nationManager.load();
        warManager.load();
        getCommand("claim").setExecutor(new ClaimCommand(this, nationManager));
        getCommand("unclaim").setExecutor(new ClaimCommand(this, nationManager));
        getCommand("nation").setExecutor(new NationCommand(this, nationManager));
        getCommand("war").setExecutor(new WarCommand(this, nationManager, warManager));
        getServer().getPluginManager().registerEvents(new ProtectionListener(nationManager, warManager), this);
        warManager.startWarTimer();
        getLogger().info("NationWar enabled!");
    }

    @Override
    public void onDisable() {
        if (nationManager != null) nationManager.save();
        if (warManager != null) warManager.save();
        getLogger().info("NationWar disabled!");
    }

    public NationManager getNationManager() { return nationManager; }
    public WarManager getWarManager() { return warManager; }
}
