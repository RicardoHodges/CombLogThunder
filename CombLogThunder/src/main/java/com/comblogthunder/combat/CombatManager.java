package com.comblogthunder.combat;

import com.comblogthunder.CombLogThunderPlugin;
import com.comblogthunder.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CombatManager {

    private final CombLogThunderPlugin plugin;
    private final Map<UUID, CombatTag> playerIdToCombatTag = new HashMap<>();
    private final Map<UUID, BossBar> playerIdToBossBar = new HashMap<>();

    private final long combatDurationSeconds;
    private final boolean bossbarEnabled;
    private final String bossbarText;
    private final BarColor bossbarColor;
    private final BarStyle bossbarStyle;

    private final boolean antiLogEnabled;

    private BukkitTask tickTask;

    public CombatManager(CombLogThunderPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration cfg = plugin.cfg();
        this.combatDurationSeconds = cfg.getLong("config-geral.combat-tag-duration", 10L);
        this.antiLogEnabled = cfg.getBoolean("config-geral.enable-anti-log-punishment", true);

        this.bossbarEnabled = cfg.getBoolean("bossbar-settings.enabled", true);
        this.bossbarText = cfg.getString("bossbar-settings.text", "&cNão deslogue em combate caso contrário será punido (Timer de &l{time}&cs)");
        this.bossbarColor = parseBarColor(cfg.getString("bossbar-settings.color", "RED"));
        this.bossbarStyle = parseBarStyle(cfg.getString("bossbar-settings.style", "SEGMENTED_20"));

        startTicker();
    }

    private BarColor parseBarColor(String color) {
        try {
            return BarColor.valueOf(color.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return BarColor.RED;
        }
    }

    private BarStyle parseBarStyle(String style) {
        try {
            return BarStyle.valueOf(style.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return BarStyle.SEGMENTED_20;
        }
    }

    private void startTicker() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, CombatTag>> it = playerIdToCombatTag.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, CombatTag> entry = it.next();
                UUID playerId = entry.getKey();
                CombatTag tag = entry.getValue();
                if (tag.isExpired()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        sendMessage(player, plugin.cfg().getString("messages.combat-end", "&aVocê saiu de combate."));
                    }
                    removeBossBar(playerId);
                    it.remove();
                    continue;
                }
                updateBossBar(playerId, tag.getRemainingSeconds());
            }
        }, 20L, 20L); // every second
    }

    public boolean isInCombat(UUID playerId) {
        CombatTag tag = playerIdToCombatTag.get(playerId);
        return tag != null && !tag.isExpired();
    }

    public long getRemainingSeconds(UUID playerId) {
        CombatTag tag = playerIdToCombatTag.get(playerId);
        return tag == null ? 0 : tag.getRemainingSeconds();
    }

    public void tagPlayers(Player damager, Player victim) {
        long newEnd = System.currentTimeMillis() + combatDurationSeconds * 1000L;

        tagPlayer(damager, victim, newEnd);
        tagPlayer(victim, damager, newEnd);
    }

    private void tagPlayer(Player player, Player opponent, long newEnd) {
        UUID pid = player.getUniqueId();
        CombatTag tag = playerIdToCombatTag.get(pid);
        boolean isNew = tag == null || tag.isExpired();
        if (tag == null) {
            tag = new CombatTag(pid, opponent.getUniqueId(), newEnd);
            playerIdToCombatTag.put(pid, tag);
        } else {
            tag.setOpponentId(opponent.getUniqueId());
            tag.setEndTimestampMs(newEnd);
        }

        if (isNew) {
            sendMessage(player, plugin.cfg().getString("messages.combat-start", "&cVocê entrou em combate com &l{player}.")
                .replace("{player}", opponent.getName()));
        }
        // Always refresh bossbar on tag/retag
        updateBossBar(pid, tag.getRemainingSeconds());
    }

    public void clearCombat(Player player) {
        UUID pid = player.getUniqueId();
        if (playerIdToCombatTag.remove(pid) != null) {
            removeBossBar(pid);
            sendMessage(player, plugin.cfg().getString("messages.combat-end", "&aVocê saiu de combate."));
        }
    }

    private void sendMessage(Player player, String raw) {
        String prefix = plugin.cfg().getString("messages.prefix", "&7[&6CombLogThunder&7] ");
        player.sendMessage(MessageUtil.prefix(raw, prefix));
    }

    private void updateBossBar(UUID playerId, long remainingSeconds) {
        if (!bossbarEnabled) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;
        BossBar bar = playerIdToBossBar.computeIfAbsent(playerId, id -> {
            BossBar b = Bukkit.createBossBar("", bossbarColor, bossbarStyle);
            b.addPlayer(player);
            b.setVisible(true);
            return b;
        });
        long total = Math.max(1, combatDurationSeconds);
        double progress = Math.max(0.0, Math.min(1.0, remainingSeconds / (double) total));
        String text = bossbarText.replace("{time}", String.valueOf(remainingSeconds));
        bar.setTitle(MessageUtil.colorize(text));
        bar.setProgress(progress);
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
    }

    private void removeBossBar(UUID playerId) {
        BossBar bar = playerIdToBossBar.remove(playerId);
        if (bar != null) {
            for (Player p : new ArrayList<>(bar.getPlayers())) {
                bar.removePlayer(p);
            }
            bar.setVisible(false);
        }
    }

    public void handleQuit(Player player) {
        if (!antiLogEnabled) return;
        UUID pid = player.getUniqueId();
        CombatTag tag = playerIdToCombatTag.get(pid);
        if (tag == null || tag.isExpired()) return;

        UUID opponentId = tag.getOpponentId();
        Player opponent = opponentId == null ? null : Bukkit.getPlayer(opponentId);
        if (opponent == null) {
            // No online opponent to award loot to; simply drop on ground at player's location
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null) player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            player.getInventory().clear();
            return;
        }

        // Transfer items to opponent; drop excess at opponent's feet
        Map<Integer, ItemStack> leftovers = opponent.getInventory().addItem(Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .toArray(ItemStack[]::new));
        leftovers.values().forEach(item -> opponent.getWorld().dropItemNaturally(opponent.getLocation(), item));
        player.getInventory().clear();

        String msg = plugin.cfg().getString("messages.punishment-message-killer",
                "&6O jogador &l{fugitive} &6fugiu do combate, e seus itens foram transferidos para o seu inventário.");
        opponent.sendMessage(MessageUtil.prefix(msg.replace("{fugitive}", player.getName()),
                plugin.cfg().getString("messages.prefix", "&7[&6CombLogThunder&7] ")));

        // Ensure player's tag is cleared post-handling
        playerIdToCombatTag.remove(pid);
        removeBossBar(pid);
    }

    public void close() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        for (UUID pid : new ArrayList<>(playerIdToBossBar.keySet())) {
            removeBossBar(pid);
        }
        playerIdToCombatTag.clear();
        playerIdToBossBar.clear();
    }
}
