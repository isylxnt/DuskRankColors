package dev.dusk.rankcolors.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledConfigurationTest {
    @Test
    void menuAccessIsDeniedByDefaultAndUseIncludesGuiAccess() throws IOException {
        String descriptor = resource("plugin.yml");
        assertTrue(descriptor.contains("duskrankcolors.use:\n    default: false"));
        assertTrue(descriptor.contains("duskrankcolors.gui: true"));
        assertTrue(descriptor.contains("duskrankcolors.gui:\n    default: false"));
    }

    @Test
    void everyUiFeedbackSoundHasABundledProfile() throws IOException {
        String menus = resource("menus.yml");
        for (String type : new String[]{"open", "navigation", "input", "select", "confirm", "cancel", "reset", "denied"}) {
            assertTrue(menus.contains("  " + type + ": { enabled: true"), "Missing sound profile: " + type);
        }
    }

    @Test
    void deniedCommandMessageExplainsTheFailure() throws IOException {
        assertTrue(resource("messages.yml").contains("You do not have permission to use this command."));
    }

    @Test
    void ranksAreInternalAndPermissionBased() throws IOException {
        String config = resource("config.yml");
        assertTrue(config.contains("ranks:"));
        assertTrue(!config.contains("  default:"));
        assertTrue(config.contains("  vip+:"));
        assertTrue(config.contains("    priority: 20"));
        assertTrue(config.contains("    plus: true"));
        assertTrue(config.contains("    plus-color: \"&6\""));
        assertTrue(config.contains("    bold: true"));
        assertTrue(!resource("plugin.yml").contains("LuckPerms"));
    }

    @Test
    void advancedRankAndPreviewFeaturesAreBundled() throws IOException {
        String config = resource("config.yml");
        String menus = resource("menus.yml");
        String descriptor = resource("plugin.yml");
        assertTrue(config.contains("rank-refresh-ticks: 20"));
        assertTrue(config.contains("allowed-modes:"));
        assertTrue(config.contains("contexts:"));
        assertTrue(menus.contains("RESET COLOR"));
        assertTrue(menus.contains("{tab_preview}"));
        assertTrue(descriptor.contains("duskrankcolors.admin.inspect"));
        assertTrue(descriptor.contains("duskrankcolors.admin.validate"));
    }

    private String resource(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(input, "Missing bundled resource: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
