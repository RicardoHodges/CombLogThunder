package com.comblogthunder;

import com.comblogthunder.combat.CombatManager;
import com.comblogthunder.listeners.CombatListeners;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class CombLogThunderPlugin extends JavaPlugin {

    private static CombLogThunderPlugin instance;
    private CombatManager combatManager;

    public static CombLogThunderPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.combatManager = new CombatManager(this);

        boolean commandBlockingEnabled = cfg().getBoolean("command-blocking.enabled", true);
        boolean worldGuardConfigured = cfg().getBoolean("worldguard-restriction.enabled", true);
        boolean worldGuardPresent = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        boolean worldGuardRestrictionEnabled = worldGuardConfigured && worldGuardPresent;

        Bukkit.getPluginManager().registerEvents(
                new CombatListeners(this, combatManager, commandBlockingEnabled, worldGuardRestrictionEnabled), this);
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.close();
        }
    }

    public FileConfiguration cfg() {
        return getConfig();
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }
}
