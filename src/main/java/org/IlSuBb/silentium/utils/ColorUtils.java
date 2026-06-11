package org.IlSuBb.silentium.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class ColorUtils {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ColorUtils() {}

    public static Component parse(String text) {
        return MM.deserialize(text);
    }

    /**
     * Parse MiniMessage text after substituting {key} placeholders.
     * @param text    source string with {placeholder} tokens
     * @param entries alternating key, value pairs: "player", "Steve", "vl", "15", …
     */
    public static Component parse(String text, String... entries) {
        text = replace(text, entries);
        return MM.deserialize(text);
    }

    public static String replace(String text, String... entries) {
        for (int i = 0; i + 1 < entries.length; i += 2) {
            text = text.replace("{" + entries[i] + "}", entries[i + 1]);
        }
        return text;
    }

    public static String stripTags(String text) {
        return MM.stripTags(text);
    }
}
