package com.comblogthunder.util;

import org.bukkit.ChatColor;

public final class MessageUtil {

    private MessageUtil() {}

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public static String prefix(String message, String prefix) {
        return colorize(prefix) + colorize(message);
    }
}
