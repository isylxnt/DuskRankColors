package dev.dusk.rankcolors;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import dev.dusk.rankcolors.api.DuskRankColorsAPI;
import dev.dusk.rankcolors.api.SelectionResult;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.event.PlayerColorChangeEvent;
import dev.dusk.rankcolors.menu.MenuHolder;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuskRankColorsIntegrationTest {
    private ServerMock server;
    private DuskRankColorsPlugin plugin;
    private DuskRankColorsAPI api;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DuskRankColorsPlugin.class);
        api = server.getServicesManager().load(DuskRankColorsAPI.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginLoadsCommandsConfigurationAndApiService() {
        assertTrue(plugin.isEnabled());
        assertNotNull(plugin.getCommand("rankcolors"));
        assertNotNull(api);
        assertTrue(plugin.getDataFolder().toPath().resolve("config.yml").toFile().isFile());
        assertTrue(plugin.getDataFolder().toPath().resolve("menus.yml").toFile().isFile());
    }

    @Test
    void bundledConfigurationPassesTheAdminValidator() {
        PlayerMock administrator = server.addPlayer("Administrator");
        administrator.setOp(true);

        assertTrue(server.dispatchCommand(administrator, "drc admin validate"));
        assertTrue(administrator.nextMessage().contains("0 errors"));
        assertTrue(administrator.nextMessage().contains("No configuration problems"));
    }

    @Test
    void playerWithoutRankSeesOnlyApplicableMainMenuButtons() {
        PlayerMock player = server.addPlayer("NoRank");
        player.addAttachment(plugin, "duskrankcolors.use", true);

        assertTrue(api.isLoaded(player.getUniqueId()));
        assertTrue(server.dispatchCommand(player, "rankcolors"));
        MenuHolder holder = assertInstanceOf(MenuHolder.class,
            player.getOpenInventory().getTopInventory().getHolder());
        assertEquals(MenuHolder.MenuType.MAIN, holder.type());
        assertNull(player.getOpenInventory().getTopInventory().getItem(10));
        assertNull(player.getOpenInventory().getTopInventory().getItem(12));
        assertNotNull(player.getOpenInventory().getTopInventory().getItem(14));
    }

    @SuppressWarnings("deprecation")
    @Test
    void cancelledLegacyChatStillSuppliesPendingHexInput() {
        PlayerMock player = server.addPlayer("CustomChatUser");
        player.addAttachment(plugin, "duskrankcolors.use", true);
        player.addAttachment(plugin, "duskrankcolors.name.rgb", true);
        assertTrue(server.dispatchCommand(player, "rankcolors"));

        player.simulateInventoryClick(14);
        assertEquals(MenuHolder.MenuType.TYPE, ((MenuHolder) player.getOpenInventory()
            .getTopInventory().getHolder()).type());
        player.simulateInventoryClick(13);

        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(false, player, "#123456",
            new HashSet<>(server.getOnlinePlayers()));
        event.setCancelled(true); // Simulates a custom chat plugin consuming the message first.
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        MenuHolder holder = assertInstanceOf(MenuHolder.class,
            player.getOpenInventory().getTopInventory().getHolder());
        assertEquals(MenuHolder.MenuType.RGB_CONFIRMATION, holder.type());
        assertEquals("#123456", holder.value());
    }

    @Test
    void automaticWatcherRefreshesPermissionRank() {
        PlayerMock player = server.addPlayer("Ranked");
        assertTrue(api.getRank(player).isEmpty());
        player.addAttachment(plugin, "duskrankcolors.ranks.vip+", true);

        server.getScheduler().performTicks(21);

        assertEquals("vip+", api.getRank(player).orElseThrow().id());
        assertEquals(20, api.getRank(player).orElseThrow().priority());
        assertTrue(api.getRank(player).orElseThrow().plus());
    }

    @Test
    void asyncApiReportsResultAndPersistsSelection() {
        PlayerMock player = server.addPlayer("ApiUser");
        player.addAttachment(plugin, "duskrankcolors.name", true);
        player.addAttachment(plugin, "duskrankcolors.name.rgb", true);

        CompletionStage<SelectionResult> operation = api.setNameSelectionAsync(player,
            PlayerColorSelection.rgb("#123456"));

        assertEquals(SelectionResult.SUCCESS, operation.toCompletableFuture().join());
        assertEquals("#123456", api.getNameSelection(player.getUniqueId()).rgbHex());
        assertEquals("RGB", player.getPersistentDataContainer().get(
            new NamespacedKey(plugin, "name_mode"), PersistentDataType.STRING));
        assertEquals("#123456", player.getPersistentDataContainer().get(
            new NamespacedKey(plugin, "name_value"), PersistentDataType.STRING));
    }

    @Test
    void cancelledChangeCompletesAsyncOperationsWithoutMutatingState() {
        PlayerMock player = server.addPlayer("CancelledApiUser");
        player.addAttachment(plugin, "duskrankcolors.name", true);
        player.addAttachment(plugin, "duskrankcolors.name.rgb", true);
        assertEquals(SelectionResult.SUCCESS, api.setNameSelectionAsync(player,
            PlayerColorSelection.rgb("#123456")).toCompletableFuture().join());

        server.getPluginManager().registerEvent(PlayerColorChangeEvent.class, new Listener() { },
            EventPriority.NORMAL, (listener, event) -> ((PlayerColorChangeEvent) event).setCancelled(true), plugin);

        assertEquals(SelectionResult.CANCELLED, api.setNameSelectionAsync(player,
            PlayerColorSelection.rgb("#654321")).toCompletableFuture().join());
        assertFalse(api.resetAllAsync(player).toCompletableFuture().join());
        assertEquals("#123456", api.getNameSelection(player.getUniqueId()).rgbHex());
    }
}
