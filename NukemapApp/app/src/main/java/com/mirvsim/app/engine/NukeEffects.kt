package com.mirvsim.app.engine

import com.mirvsim.app.model.DamageEffects
import com.mirvsim.app.model.RingType
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

object NukeEffects {

    fun calculate(yieldKt: Double, hobMode: String): DamageEffects {
        val y = yieldKt
        val isSurface = hobMode == "surface"

        return DamageEffects(
            fireball = if (isSurface) 0.142 * y.pow(0.4)
            else 0.17 * y.pow(0.4),
            psi20 = 0.45 * y.pow(1.0 / 3),
            psi10 = 0.63 * y.pow(1.0 / 3),
            psi5 = 0.87 * y.pow(1.0 / 3),
            psi3 = 1.2 * y.pow(1.0 / 3),
            psi1 = 2.4 * y.pow(1.0 / 3),
            thermal = if (isSurface) 0.57 * y.pow(0.42)
            else 0.67 * y.pow(0.41),
            radiation = 0.24 * y.pow(0.19)
        )
    }

    data class RingStyle(
        val color: Color,
        val fillColor: Color,
        val fillOpacity: Float,
        val weight: Float,
        val dashArray: FloatArray?
    )

    fun getRingStyle(ringType: RingType): RingStyle {
        return when (ringType) {
            RingType.fireball -> RingStyle(
                Color(0xFFFFD700), Color(0xFFFFD700), 0.04f, 3f, null
            )
            RingType.psi20 -> RingStyle(
                Color(0xFFE53935), Color(0xFFE53935), 0.03f, 3f, null
            )
            RingType.psi10 -> RingStyle(
                Color(0xFFF4511E), Color(0xFFF4511E), 0.025f, 2.5f, null
            )
            RingType.psi5 -> RingStyle(
                Color(0xFFFF8F00), Color(0xFFFF8F00), 0.02f, 2f,
                floatArrayOf(8f, 6f)
            )
            RingType.psi3 -> RingStyle(
                Color(0xFF00BCD4), Color(0xFF00BCD4), 0.015f, 2f,
                floatArrayOf(4f, 4f)
            )
            RingType.psi1 -> RingStyle(
                Color(0xFF7CB342), Color(0xFF7CB342), 0.01f, 1.5f,
                floatArrayOf(2f, 4f)
            )
            RingType.thermal -> RingStyle(
                Color(0xFFE040FB), Color(0xFFE040FB), 0.025f, 2.5f,
                floatArrayOf(10f, 4f, 2f, 4f)
            )
        }
    }

}
