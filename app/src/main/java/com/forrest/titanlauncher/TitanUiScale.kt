package com.forrest.titanlauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.max


private const val DESIGN_WIDTH_DP =
    360f


@Composable
internal fun TitanUiScale(
    content: @Composable () -> Unit
) {

    val configuration =
        LocalConfiguration.current

    val baseDensity =
        LocalDensity.current

    /*
     * Prompt Launcher was designed and approved against a ~360dp-wide
     * Titan-shaped canvas. The physical Titan 2 Elite ships at roughly
     * 573dp smallest width, which makes ordinary dp/sp values appear
     * dramatically smaller.
     *
     * Increase only Prompt Launcher's Compose density so the app keeps
     * the original visual proportions while leaving Android's system
     * display size at the stock Titan setting.
     *
     * On a 573dp-wide Titan:
     * 573 / 360 = ~1.59x
     *
     * On the original 360dp emulator:
     * 360 / 360 = 1.0x
     */
    val widthScale =
        max(
            1f,
            configuration.screenWidthDp /
                    DESIGN_WIDTH_DP
        )

    val titanDensity =
        Density(
            density =
                baseDensity.density *
                        widthScale,
            fontScale =
                baseDensity.fontScale
        )

    CompositionLocalProvider(
        LocalDensity provides titanDensity
    ) {

        content()
    }
}
