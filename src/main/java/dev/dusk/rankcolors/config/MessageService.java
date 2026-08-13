package dev.dusk.rankcolors.config;

import dev.dusk.rankcolors.util.Text;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;

public final class MessageService {
    private final ConfigManager configs;

    public MessageService(ConfigManager configs) {
        this.configs = configs;
    }

    public Component component(String path) {
        return component(path, Map.of());
    }

    public Component component(String path, Map<String, String> replacements) {
        String raw = configs.messages().getString(path, "&cMissing message: " + path);
        Map<String, String> values = new HashMap<>(replacements);
        values.putIfAbsent("prefix", configs.messages().getString("prefix", "&8[&6DuskRankColors&8] "));
        return Text.legacy(Text.replace(raw, values));
    }

    public void send(Audience audience, String path) {
        audience.sendMessage(component(path));
    }

    public void send(Audience audience, String path, Map<String, String> replacements) {
        audience.sendMessage(component(path, replacements));
    }
}
