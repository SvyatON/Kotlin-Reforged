// Modified for Kotlin Reforged by SvyatON, 2026-10-11.
package thedarkcolour.kotlinforforge.forge

import net.minecraft.util.profiling.ProfilerFiller

public fun ProfilerFiller.use(name: String, toProfile: () -> Unit) {
    push(name)
    try {
        toProfile()
    } finally {
        pop()
    }
}

public fun ProfilerFiller.use(supplier: () -> String, toProfile: () -> Unit) {
    push(supplier)
    try {
        toProfile()
    } finally {
        pop()
    }
}
