package dev.dusk.rankcolors.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;

/**
 * A deliberately small reflection boundary. Compiling against the 1.20.1 API keeps the JAR loadable
 * on the minimum target while this adapter invokes Folia's schedulers when they are present.
 */
public final class FoliaSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;
    private final Method getGlobalScheduler;
    private final Method getAsyncScheduler;
    private final Method getEntityScheduler;

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        try {
            getGlobalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
            getAsyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");
            getEntityScheduler = Player.class.getMethod("getScheduler");
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Folia scheduler API was not found", exception);
        }
    }

    public static boolean isAvailable() {
        try {
            Class<?> buildInfoType = Class.forName("io.papermc.paper.ServerBuildInfo");
            Object buildInfo = buildInfoType.getMethod("buildInfo").invoke(null);
            Object result = buildInfoType.getMethod("isBrandCompatible", Key.class)
                .invoke(buildInfo, Key.key("papermc", "folia"));
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // ServerBuildInfo is newer than the minimum target; use the established legacy probe below.
        }
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            Bukkit.class.getMethod("getGlobalRegionScheduler");
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return false;
        }
    }

    @Override
    public void runGlobal(Runnable task) {
        Object scheduler = invoke(getGlobalScheduler, null);
        invokeNamed(getGlobalScheduler, scheduler, "execute", new Class<?>[]{Plugin.class, Runnable.class}, plugin, task);
    }

    @Override
    public void runAsync(Runnable task) {
        Object scheduler = invoke(getAsyncScheduler, null);
        Consumer<Object> consumer = ignored -> task.run();
        invokeNamed(getAsyncScheduler, scheduler, "runNow", new Class<?>[]{Plugin.class, Consumer.class}, plugin, consumer);
    }

    @Override
    public void runForPlayer(Player player, Runnable task) {
        Object scheduler = invoke(getEntityScheduler, player);
        invokeNamed(getEntityScheduler, scheduler, "execute", new Class<?>[]{Plugin.class, Runnable.class, Runnable.class, long.class},
            plugin, task, null, 1L);
    }

    @Override
    public void runForPlayerLater(Player player, long delayTicks, Runnable task) {
        Object scheduler = invoke(getEntityScheduler, player);
        invokeNamed(getEntityScheduler, scheduler, "execute", new Class<?>[]{Plugin.class, Runnable.class, Runnable.class, long.class},
            plugin, task, null, Math.max(1, delayTicks));
    }

    @Override
    public void shutdown() {
        Object global = invoke(getGlobalScheduler, null);
        Object async = invoke(getAsyncScheduler, null);
        invokeNamed(getGlobalScheduler, global, "cancelTasks", new Class<?>[]{Plugin.class}, plugin);
        invokeNamed(getAsyncScheduler, async, "cancelTasks", new Class<?>[]{Plugin.class}, plugin);
        // Entity scheduler work is also cancelled by Folia when its owning plugin disables.
    }

    @Override
    public String platformName() {
        return "Folia";
    }

    private Object invoke(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not invoke Folia scheduler", exception);
        }
    }

    private Object invokeNamed(Method accessor, Object target, String name, Class<?>[] types, Object... arguments) {
        try {
            Method method = accessor.getReturnType().getMethod(name, types);
            return method.invoke(target, arguments);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not invoke Folia scheduler method " + name, exception);
        }
    }
}
