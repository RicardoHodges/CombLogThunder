package com.comblogthunder.listeners;

import com.comblogthunder.CombLogThunderPlugin;
import com.comblogthunder.combat.CombatManager;
import com.comblogthunder.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.entity.Projectile;

public class CombatListeners implements Listener {

    private final CombLogThunderPlugin plugin;
    private final CombatManager combatManager;
    private final boolean commandBlockEnabled;
    private final boolean wgRestrictionEnabled;

    public CombatListeners(CombLogThunderPlugin plugin, CombatManager combatManager, boolean commandBlockEnabled, boolean wgRestrictionEnabled) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.commandBlockEnabled = commandBlockEnabled;
        this.wgRestrictionEnabled = wgRestrictionEnabled;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player damagerPlayer = null;
        if (event.getDamager() instanceof Player p) {
            damagerPlayer = p;
        } else if (event.getDamager() instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player sp) {
                damagerPlayer = sp;
            }
        }
        if (damagerPlayer == null) return;

        // Only tag if both are players and PvP
        if (damagerPlayer.equals(victim)) return;

        combatManager.tagPlayers(damagerPlayer, victim);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        combatManager.handleQuit(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!commandBlockEnabled) return;
        Player player = event.getPlayer();
        if (combatManager.isInCombat(player.getUniqueId())) {
            String msg = plugin.getConfig().getString("messages.command-blocked", "&cVocê não pode usar comandos enquanto estiver em combate.");
            event.setCancelled(true);
            player.sendMessage(MessageUtil.prefix(msg,
                    plugin.getConfig().getString("messages.prefix", "&7[&6CombLogThunder&7] ")));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!wgRestrictionEnabled) return;
        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player.getUniqueId())) return;
        if (WorldGuardHook.isEnteringDeniedPvP(player, event)) {
            event.setTo(event.getFrom());
            String msg = plugin.getConfig().getString("messages.region-blocked", "&cVocê não pode entrar nessa área em combate.");
            player.sendMessage(MessageUtil.prefix(msg,
                    plugin.getConfig().getString("messages.prefix", "&7[&6CombLogThunder&7] ")));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!wgRestrictionEnabled) return;
        Player player = event.getPlayer();
        if (!combatManager.isInCombat(player.getUniqueId())) return;
        if (WorldGuardHook.isEnteringDeniedPvP(player, event)) {
            event.setCancelled(true);
            String msg = plugin.getConfig().getString("messages.region-blocked", "&cVocê não pode entrar nessa área em combate.");
            player.sendMessage(MessageUtil.prefix(msg,
                    plugin.getConfig().getString("messages.prefix", "&7[&6CombLogThunder&7] ")));
        }
    }
}
