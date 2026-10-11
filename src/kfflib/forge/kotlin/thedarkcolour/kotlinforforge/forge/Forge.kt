// Modified for Kotlin Reforged by SvyatON, 2026-10-11.
package thedarkcolour.kotlinforforge.forge

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.listener.Priority
import net.minecraftforge.eventbus.api.bus.EventBus
import net.minecraftforge.eventbus.internal.Event
import net.minecraftforge.eventbus.api.bus.BusGroup
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.loading.FMLEnvironment
import thedarkcolour.kotlinforforge.KotlinModLoadingContext
import java.util.function.Consumer
import java.util.function.Predicate
import net.minecraftforge.eventbus.api.bus.CancellableEventBus
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable
import net.minecraftforge.eventbus.api.listener.EventListener
import net.minecraftforge.eventbus.api.listener.ObjBooleanBiConsumer
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Forge event bus.
 * Game events are fired on this bus.
 *
 * Examples:
 *   @see net.minecraftforge.event.entity.player.PlayerEvent
 *   @see net.minecraftforge.event.entity.living.LivingEvent
 *   @see net.minecraftforge.event.world.BlockEvent
 */
public inline val FORGE_BUS: BusGroup
    get() = BusGroup.DEFAULT

/**
 * Mod-specific event bus.
 * Mod lifecycle events are fired on this bus.
 *
 * Examples:
 *   @see net.minecraftforge.fml.event.lifecycle.InterModProcessEvent
 *   @see net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
 *   @see net.minecraftforge.registries.NewRegistryEvent
 */
public inline val MOD_BUS: BusGroup
    get() = KotlinModLoadingContext.get().getKEventBus()

/**
 * Used in place of [net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext]
 */
public inline val MOD_CONTEXT: KotlinModLoadingContext
    get() = KotlinModLoadingContext.get()

public inline val LOADING_CONTEXT: ModLoadingContext
    get() = ModLoadingContext.get()

/** Current dist */
public inline val DIST: Dist
    get() = FMLEnvironment.dist

/**
 * An alternative to using [net.minecraftforge.fml.DistExecutor]
 */
public fun <T> callWhenOn(dist: Dist, toRun: () -> T): T? {
    return if (DIST == dist) {
        try {
            toRun()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    } else null
}

/**
 * An alternative to using [net.minecraftforge.fml.DistExecutor]
 */
public inline fun runWhenOn(dist: Dist, toRun: () -> Unit) {
    if (DIST == dist) {
        toRun()
    }
}

/**
 * An alternative to using [net.minecraftforge.fml.DistExecutor]
 */
public inline fun <T> runForDist(clientTarget: () -> T, serverTarget: () -> T): T {
    return if (DIST == Dist.CLIENT) {
        clientTarget()
    } else {
        serverTarget()
    }
}

/**
 * Register a config
 */
public inline fun registerConfig(type: ModConfig.Type, spec: ForgeConfigSpec, fileName: String) {
    LOADING_CONTEXT.registerConfig(type, spec, fileName)
}

public inline fun registerConfig(type: ModConfig.Type, spec: ForgeConfigSpec) {
    LOADING_CONTEXT.registerConfig(type, spec)
}

public inline fun <reified T : Event> BusGroup.addListener(listener: Consumer<T>, priority: Byte = Priority.NORMAL): EventListener =
    EventBus.create(this, T::class.java).addListener(priority, listener)

public inline fun <reified T : Event, reified F> BusGroup.addGenericListener(
    listener: Consumer<T>,
    crossinline genericType: (T) -> Class<*>,
    priority: Byte = Priority.NORMAL,
): EventListener = EventBus.create(this, T::class.java).addListener(priority) { event ->
    if (genericType(event) == F::class.java) listener.accept(event)
}

public inline fun <reified T> BusGroup.addCancellingListener(listener: Predicate<T>, priority: Byte = Priority.NORMAL): EventListener
    where T : Event, T : Cancellable = CancellableEventBus.create(this, T::class.java).addListener(priority, listener)

public inline fun <reified T> BusGroup.addCancellationMonitor(listener: ObjBooleanBiConsumer<T>): EventListener
    where T : Event, T : Cancellable = CancellableEventBus.create(this, T::class.java).addListener(listener)

/**
 * Sided delegate with lazy values. This works well with proxies, if you still use those.
 * It is safe to use side-specific values in [clientValue] and [serverValue].
 *
 * @param clientValue the value of this property on the client side.
 * @param serverValue the value of this property on the server side.
 * @param T the common type of both values. It is recommended to not use [Any] when possible.
 *
 * @see sidedDelegate if you'd like a sided value that is computed each time it is accessed
 */
public fun <T> lazySidedDelegate(clientValue: () -> T, serverValue: () -> T): ReadOnlyProperty<Any?, T> {
    return LazySidedDelegate(clientValue, serverValue)
}

/**
 * Sided delegate with values that are computed per access.
 * It is safe to use client-only types for [clientValue]
 * and server-only types for [serverValue].
 *
 * @param clientValue the value of this property on the client side.
 * @param serverValue the value of this property on the server side.
 * @param T the common type of both values. It is recommended to not use [Any] when possible.
 */
public fun <T> sidedDelegate(clientValue: () -> T, serverValue: () -> T): ReadOnlyProperty<Any?, T> {
    return SidedGetterDelegate(clientValue, serverValue)
}

/**
 * Lazy sided delegate whose values are initialized lazily.
 */
private class LazySidedDelegate<T>(clientValue: () -> T, serverValue: () -> T) : ReadOnlyProperty<Any?, T> {
    private val clientValue by lazy(clientValue)
    private val serverValue by lazy(serverValue)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return when (DIST) {
            Dist.CLIENT -> clientValue
            Dist.DEDICATED_SERVER -> serverValue
        }
    }
}

/** @since 1.2.2
 * Sided delegate for things like proxies,
 * or just a null checker for values that only exist on one side.
 * Values are computed each time they are accessed.
 */
private class SidedGetterDelegate<T>(private val clientValue: () -> T, private val serverValue: () -> T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return runForDist(clientValue, serverValue)
    }
}
