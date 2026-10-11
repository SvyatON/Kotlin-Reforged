// Modified for Kotlin Reforged by SvyatON, 2026-10-11.
package thedarkcolour.kotlinforforge

import net.minecraftforge.eventbus.api.bus.BusGroup
import net.minecraftforge.eventbus.api.bus.EventBus
import net.minecraftforge.eventbus.api.event.InheritableEvent
import net.minecraftforge.fml.config.IConfigEvent
import net.minecraftforge.fml.Logging
import net.minecraftforge.fml.ModContainer
import net.minecraftforge.fml.ModLoadingException
import net.minecraftforge.fml.ModLoadingStage
import net.minecraftforge.fml.event.IModBusEvent
import net.minecraftforge.forgespi.language.IModInfo
import net.minecraftforge.forgespi.language.ModFileScanData
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import java.lang.reflect.InvocationTargetException
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.loading.FMLEnvironment

/**
 * Kotlin mod container
 */
public class KotlinModContainer(
    info: IModInfo,
    className: String,
    private val scanResults: ModFileScanData,
    gameLayer: ModuleLayer,
) : ModContainer(info) {
    private var modInstance: Any? = null
    internal val eventBus: BusGroup
    private val modClass: Class<*>
    private val context = KotlinModLoadingContext(this)

    init {
        LOGGER.debug(Logging.LOADING, "Creating KotlinModContainer instance for $className")

        activityMap[ModLoadingStage.CONSTRUCT] = Runnable(::constructMod)

        eventBus = BusGroup.create("modBusFor ${info.modId}")

        contextExtension = Supplier { context }

        try {
            val layer = gameLayer.findModule(info.owningFile.moduleName()).orElseThrow()
            modClass = Class.forName(layer, className)
            LOGGER.trace(Logging.LOADING, "Loaded modclass {} with {}", modClass.name, modClass.classLoader)
        } catch (t: Throwable) {
            LOGGER.error(Logging.LOADING, "Failed to load class $className", t)
            throw ModLoadingException(info, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmodclass", t)
        }
    }

    // Sets modInstance to a new instance of the mod class or the object instance
    private fun constructMod() {
        try {
            LOGGER.trace(Logging.LOADING, "Loading mod instance ${getModId()} of type ${modClass.name}")
            modInstance = modClass.kotlin.objectInstance ?: createModInstance()
            LOGGER.trace(Logging.LOADING, "Loaded mod instance ${getModId()} of type ${modClass.name}")
        } catch (throwable: Throwable) {
            LOGGER.error(Logging.LOADING, "Failed to create mod instance. ModID: ${getModId()}, class ${modClass.name}", throwable)
            val cause = if (throwable is InvocationTargetException) throwable.targetException else throwable
            throw ModLoadingException(modInfo, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmod", cause, modClass)
        }

        try {
            LOGGER.trace(Logging.LOADING, "Injecting Automatic Kotlin event subscribers for ${getModId()}")
            // Inject into object EventBusSubscribers
            AutoKotlinEventBusSubscriber.inject(this, scanResults, modClass.classLoader)
            LOGGER.trace(Logging.LOADING, "Completed Automatic Kotlin event subscribers for ${getModId()}")
        } catch (throwable: Throwable) {
            LOGGER.error(Logging.LOADING, "Failed to register Automatic Kotlin subscribers. ModID: ${getModId()}, class ${modClass.name}", throwable)
            throw ModLoadingException(modInfo, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmod", throwable, modClass)
        }
    }

    public fun getModBusGroup(): BusGroup = eventBus

    private fun createModInstance(): Any {
        val constructors = modClass.constructors.filterNot { it.isSynthetic }
        val constructor = constructors.singleOrNull { it.parameterTypes.contentEquals(arrayOf(KotlinModLoadingContext::class.java)) }
            ?: constructors.singleOrNull { it.parameterCount == 0 }
            ?: constructors.singleOrNull()
            ?: throw IllegalArgumentException("Mod class ${modClass.name} must have an unambiguous public constructor")
        val supported = mapOf(
            KotlinModLoadingContext::class.java to context,
            BusGroup::class.java to eventBus,
            ModContainer::class.java to this,
            KotlinModContainer::class.java to this,
            Dist::class.java to FMLEnvironment.dist,
        )
        require(constructor.parameterTypes.toSet().size == constructor.parameterCount) { "Duplicate mod constructor parameters: $constructor" }
        val arguments = constructor.parameterTypes.map { type ->
            supported[type] ?: throw IllegalArgumentException("Unsupported mod constructor parameter ${type.name} in $constructor")
        }
        return constructor.newInstance(*arguments.toTypedArray())
    }

    override fun dispatchConfigEvent(event: IConfigEvent) {
        val self: net.minecraftforge.eventbus.internal.Event = event.self()
        EventBus.create(eventBus, self.javaClass).post(self)
    }

    override fun matches(mod: Any?): Boolean {
        return mod === modInstance
    }

    override fun getMod(): Any? = modInstance

    public override fun <T : IModBusEvent> acceptEvent(e: T) {
        try {
            LOGGER.trace("Firing event for modid $modId : $e")
            IModBusEvent.getBus(eventBus, e.javaClass).post(e)
            LOGGER.trace("Fired event for modid $modId : $e")
        } catch (t: Throwable) {
            LOGGER.error("Caught exception during event $e dispatch for modid $modId", t)
            throw ModLoadingException(modInfo, modLoadingStage, "fml.modloading.errorduringevent", t)
        }
    }
}
