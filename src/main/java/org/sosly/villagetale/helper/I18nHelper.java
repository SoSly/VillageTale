package org.sosly.villagetale.helper;

import com.ibm.icu.text.MessageFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class I18nHelper {

    public static Component translate(String key, Object... args) {
        String pattern = I18n.get(key);

        if (usesIcuPatterns(pattern)) {
            return formatWithIcu(pattern, args);
        }

        return Component.translatable(key, args);
    }

    private static boolean usesIcuPatterns(String pattern) {
        return pattern.contains("{0, plural") ||
               pattern.contains("{0, number") ||
               pattern.contains("{1, plural") ||
               pattern.contains("{1, number");
    }

    private static Component formatWithIcu(String pattern, Object... args) {
        Locale locale = getMinecraftLocale();
        MessageFormat messageFormat = new MessageFormat(pattern, locale);
        String formatted = messageFormat.format(args);
        return Component.literal(formatted);
    }

    private static Locale getMinecraftLocale() {
        String languageCode = Minecraft.getInstance().getLanguageManager().getSelected();
        String[] parts = languageCode.split("_");

        if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        }

        return new Locale(parts[0]);
    }
}
