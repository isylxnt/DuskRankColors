package dev.dusk.rankcolors.util;

import net.kyori.adventure.text.Component;

import java.util.Map;

/** Composes Adventure components without flattening RGB or gradient styling into plain text. */
public final class ComponentTemplate {
    private ComponentTemplate() {
    }

    public static Component compose(String template, Map<String, Component> replacements) {
        Component result = Component.empty();
        int cursor = 0;
        while (cursor < template.length()) {
            int next = template.indexOf('{', cursor);
            if (next < 0) return result.append(Component.text(template.substring(cursor)));
            if (next > cursor) result = result.append(Component.text(template.substring(cursor, next)));

            String matched = null;
            for (String key : replacements.keySet()) {
                if (template.startsWith("{" + key + "}", next)) {
                    matched = key;
                    break;
                }
            }
            if (matched == null) {
                result = result.append(Component.text("{"));
                cursor = next + 1;
            } else {
                result = result.append(replacements.get(matched));
                cursor = next + matched.length() + 2;
            }
        }
        return result;
    }
}
