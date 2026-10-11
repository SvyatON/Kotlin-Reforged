// Modified for Kotlin Reforged by SvyatON, 2026-10-11.
package thedarkcolour.kotlinforforge.forge

import com.mojang.blaze3d.vertex.PoseStack
import org.joml.Quaternionf
import org.joml.Matrix4f

public fun PoseStack.use(toRun: () -> Unit) {
    pushPose()
    try {
        toRun()
    } finally {
        popPose()
    }
}

public operator fun PoseStack.timesAssign(matrix: Quaternionf): Unit = mulPose(Matrix4f().rotation(matrix))
