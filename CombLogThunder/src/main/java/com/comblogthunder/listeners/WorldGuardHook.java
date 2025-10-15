package com.comblogthunder.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class WorldGuardHook {

    private WorldGuardHook() {}

    public static boolean isEnteringDeniedPvP(Player player, PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getWorld() != event.getTo().getWorld()) {
            return false;
        }
        // If not changing block, ignore for performance
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return false;
        }
        return isDeniedAt(player, event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
    }

    public static boolean isEnteringDeniedPvP(Player player, PlayerTeleportEvent event) {
        if (event.getTo() == null) return false;
        return isDeniedAt(player, event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
    }

    private static boolean isDeniedAt(Player player, double x, double y, double z) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        Location bukkitLocation = new Location(player.getWorld(), x, y, z);
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(bukkitLocation));
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        StateFlag.State state = set.queryValue(localPlayer, Flags.PVP);
        // Denied only when flag explicitly DENY at target location
        return state == StateFlag.State.DENY;
    }
}
