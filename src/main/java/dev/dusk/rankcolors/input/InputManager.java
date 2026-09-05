package dev.dusk.rankcolors.input;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.config.MessageService;
import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.config.SoundService;
import dev.dusk.rankcolors.menu.MenuManager;
import dev.dusk.rankcolors.scheduler.SchedulerAdapter;
import dev.dusk.rankcolors.service.PlayerColorService;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InputManager implements Listener {
    private final PluginConfiguration configuration;
    private final MessageService messages;
    private final SoundService sounds;
    private final SchedulerAdapter scheduler;
    private final PlayerColorService colors;
    private final Map<UUID, PendingColorInput> pending = new ConcurrentHashMap<>();
    private final Map<UUID, GradientDraft> drafts = new ConcurrentHashMap<>();
    private MenuManager menus;

    public InputManager(PluginConfiguration configuration, MessageService messages, SoundService sounds, SchedulerAdapter scheduler,
                        PlayerColorService colors) {
        this.configuration = configuration;
        this.messages = messages;
        this.sounds = sounds;
        this.scheduler = scheduler;
        this.colors = colors;
    }

    public void bindMenus(MenuManager menus) {
        this.menus = menus;
    }

    public void requestRgb(Player player, ColorCategory category) {
        createPending(player, new PendingColorInput(category, PendingColorInput.InputType.RGB, -1,
            PendingColorInput.MenuReturn.COLOR_TYPE, expiration()));
        player.closeInventory();
        messages.send(player, "input.rgb");
        sounds.play(player, "input");
    }

    public void requestGradientStop(Player player, GradientDraft draft, int index) {
        drafts.put(player.getUniqueId(), draft);
        createPending(player, new PendingColorInput(draft.category(), PendingColorInput.InputType.GRADIENT_STOP, index,
            PendingColorInput.MenuReturn.GRADIENT_EDITOR, expiration()));
        player.closeInventory();
        messages.send(player, "input.gradient");
        sounds.play(player, "input");
    }

    private void createPending(Player player, PendingColorInput value) {
        UUID uuid = player.getUniqueId();
        pending.put(uuid, value);
        scheduler.runForPlayerLater(player, configuration.inputTimeoutSeconds() * 20L, () -> {
            if (pending.remove(uuid, value)) {
                messages.send(player, "input.expired");
                sounds.play(player, "cancel");
                if (value.type() == PendingColorInput.InputType.GRADIENT_STOP) menus.openGradient(player, value.category());
                else menus.openType(player, value.category());
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PendingColorInput input = pending.get(player.getUniqueId());
        if (input == null) return;
        event.setCancelled(true);
        String value = dev.dusk.rankcolors.util.Text.plain(event.message()).trim();
        capture(player, input, value);
    }

    /** Compatibility path for chat plugins that still emit or bridge Bukkit's legacy chat event. */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingColorInput input = pending.get(player.getUniqueId());
        if (input == null) return;
        event.setCancelled(true);
        capture(player, input, event.getMessage().trim());
    }

    private void capture(Player player, PendingColorInput input, String value) {
        scheduler.runForPlayer(player, () -> handle(player, input, value));
    }

    private void handle(Player player, PendingColorInput expected, String value) {
        UUID uuid = player.getUniqueId();
        if (!pending.remove(uuid, expected)) return;
        if (Instant.now().isAfter(expected.expiresAt())) {
            messages.send(player, "input.expired");
            sounds.play(player, "cancel");
            reopen(player, expected);
            return;
        }
        if (value.equalsIgnoreCase("cancel") || value.equalsIgnoreCase("cancelar")) {
            messages.send(player, "input.cancelled");
            sounds.play(player, "cancel");
            reopen(player, expected);
            return;
        }
        java.util.Optional<String> normalized = dev.dusk.rankcolors.color.HexColor.normalize(value);
        if (normalized.isEmpty()) {
            messages.send(player, "invalid-hex");
            sounds.play(player, "denied");
            createPending(player, new PendingColorInput(expected.category(), expected.type(), expected.gradientIndex(),
                expected.returnTo(), expiration()));
            return;
        }
        if (expected.type() == PendingColorInput.InputType.RGB) {
            menus.openRgbConfirmation(player, expected.category(), normalized.get());
            return;
        }
        GradientDraft draft = drafts.get(uuid);
        if (draft == null || draft.category() != expected.category()) {
            draft = draftFor(player, expected.category());
        }
        if (expected.gradientIndex() < 0) draft.add(normalized.get());
        else if (expected.gradientIndex() < draft.stops().size()) draft.set(expected.gradientIndex(), normalized.get());
        draft.touch(expiration());
        menus.openGradient(player, expected.category());
    }

    private void reopen(Player player, PendingColorInput input) {
        if (input.returnTo() == PendingColorInput.MenuReturn.GRADIENT_EDITOR) menus.openGradient(player, input.category());
        else menus.openType(player, input.category());
    }

    public GradientDraft draftFor(Player player, ColorCategory category) {
        GradientDraft existing = drafts.get(player.getUniqueId());
        if (existing != null && existing.category() == category && Instant.now().isBefore(existing.expiresAt())) return existing;
        PlayerColorSelection current = colors.settings(player.getUniqueId()).selection(category);
        List<String> stops = current.mode() == dev.dusk.rankcolors.color.ColorMode.GRADIENT
            ? current.gradient().hexColors() : List.of("#FF55FF", "#55FFFF");
        GradientDraft created = new GradientDraft(category, stops, expiration());
        drafts.put(player.getUniqueId(), created);
        return created;
    }

    public void discardDraft(UUID uuid) { drafts.remove(uuid); pending.remove(uuid); }
    public void finishDraft(UUID uuid) { drafts.remove(uuid); pending.remove(uuid); }
    public boolean isPending(UUID uuid) { return pending.containsKey(uuid); }
    public int pendingCount() { return pending.size(); }
    public void clear() { pending.clear(); drafts.clear(); }

    private Instant expiration() {
        return Instant.now().plusSeconds(configuration.inputTimeoutSeconds());
    }
}
