// Modified for Kotlin Reforged by SvyatON, 2026-10-11.
package thedarkcolour.kotlinforforge.test

import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.LogManager

@Mod("kotlin_reforged")
public object KotlinForForge {
    private val LOGGER = LogManager.getLogger()
    init {
        LOGGER.info("Kotlin Reforged Enabled!")
    }
}
