// Modified for Kotlin Reforged by SvyatON, 2026-10-11.
package thedarkcolour.kotlinforforge

import net.minecraftforge.eventbus.api.bus.BusGroup
import net.minecraftforge.fml.ModLoadingContext

/**
 * Mod loading context for mods made with Kotlin for Forge.
 */
public class KotlinModLoadingContext(public val container: KotlinModContainer) {
    /** Mods should access through [MOD_BUS] */
    public fun getKEventBus(): BusGroup {
        return container.eventBus
    }

    public fun getModBusGroup(): BusGroup = container.eventBus

    public companion object {
        /** Mods should access through [MOD_CONTEXT] */
        public fun get(): KotlinModLoadingContext {
            return ModLoadingContext.get().extension()
        }
    }
}
