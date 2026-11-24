package com.yid.app.feature.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The icons Settings tiles need on top of YidIcons. Ported from LinkedOut's SettingsIcons
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/settings/SettingsIcons.kt#SettingsIcons
 * Google's Material Icons, filled style, 24 dp grid, Apache License 2.0 (https://github.com/google/material-design-icons),
 * with the paths as LinkedOut keeps them. Kept beside the screen because YidIcons is shared by the whole app and the
 * tiles are the only users.
 * not ported: Brightness (Theme takes Palette, as the validated picture shows), Dns (the Reads tile is for X's sources),
 * symbol (the builder for the 960 grid, only Warning uses that grid and it is written out below), LinkedOut's Warning
 * stays LinkedOutIcons' own, Yiḍ's is the same Material Symbols Rounded path
 */
internal object SettingsIcons {

    val Palette: ImageVector by lazy {
        build(
            "Palette",
            "M12,2C6.49,2,2,6.49,2,12s4.49,10,10,10c1.38,0,2.5-1.12,2.5-2.5c0-0.61-0.23-1.2-0.64-1.67c-0.08-0.1-0.13-0.21-0.13-0.33 c0-0.28,0.22-0.5,0.5-0.5H16c3.31,0,6-2.69,6-6C22,6.04,17.51,2,12,2z M17.5,13c-0.83,0-1.5-0.67-1.5-1.5c0-0.83,0.67-1.5,1.5-1.5 s1.5,0.67,1.5,1.5C19,12.33,18.33,13,17.5,13z M14.5,9C13.67,9,13,8.33,13,7.5C13,6.67,13.67,6,14.5,6S16,6.67,16,7.5 C16,8.33,15.33,9,14.5,9z M5,11.5C5,10.67,5.67,10,6.5,10S8,10.67,8,11.5C8,12.33,7.33,13,6.5,13S5,12.33,5,11.5z M11,7.5 C11,8.33,10.33,9,9.5,9S8,8.33,8,7.5C8,6.67,8.67,6,9.5,6S11,6.67,11,7.5z"
        )
    }

    val Image: ImageVector by lazy {
        build("Image", "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z")
    }

    val FormatSize: ImageVector by lazy { build("FormatSize", "M9 4v3h5v12h3V7h5V4H9zm-6 8h3v7h3v-7h3V9H3v3z") }

    val Night: ImageVector by lazy {
        build(
            "Night",
            "M12,3c-4.97,0-9,4.03-9,9s4.03,9,9,9s9-4.03,9-9c0-0.46-0.04-0.92-0.1-1.36c-0.98,1.37-2.58,2.26-4.4,2.26 c-2.98,0-5.4-2.42-5.4-5.4c0-1.81,0.89-3.42,2.26-4.4C12.92,3.04,12.46,3,12,3L12,3z"
        )
    }

    val AutoAwesome: ImageVector by lazy {
        build(
            "AutoAwesome",
            "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z"
        )
    }

    val GridView: ImageVector by lazy {
        build("GridView", "M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 4v8h8v-8H3zm6 6H5v-4h4v4zm4-16v8h8V3h-8zm6 6h-4V5h4v4zm-6 4v8h8v-8h-8zm6 6h-4v-4h4v4z")
    }

    val Wifi: ImageVector by lazy {
        build("Wifi", "M1 9l2 2c4.97-4.97 13.03-4.97 18 0l2-2C16.93 2.93 7.08 2.93 1 9zm8 8l3 3 3-3c-1.65-1.66-4.34-1.66-6 0zm-4-4l2 2c2.76-2.76 7.24-2.76 10 0l2-2C15.14 9.14 8.87 9.14 5 13z")
    }

    val Link: ImageVector by lazy {
        build(
            "Link",
            "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z"
        )
    }

    val Code: ImageVector by lazy {
        build("Code", "M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z")
    }

    val Update: ImageVector by lazy {
        build(
            "Update",
            "M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14zm-1-6h-3V8h-2v5H8l4 4 4-4z"
        )
    }

    val Timer: ImageVector by lazy {
        build(
            "Timer",
            "M19.03,7.39l1.42-1.42c-0.43-0.51-0.9-0.99-1.41-1.41l-1.42,1.42C16.07,4.74,14.12,4,12,4c-4." +
                "97,0-9,4.03-9,9 c0,4.97,4.02,9,9,9s9-4.03,9-9C21,10.88,20.26,8.93,19.03,7.39z M13,14h-2V8h" +
                "2V14zM9 1h6v2H9z"
        )
    }

    val Upload: ImageVector by lazy { build("Upload", "M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z") }

    // Troubleshooting's section icon. Material Symbols Rounded, fill 1 (960 grid), as LinkedOutIcons' Warning.
    // https://github.com/google/material-design-icons/blob/master/symbols/android/warning/materialsymbolsrounded/warning_fill1_24px.xml
    val Warning: ImageVector by lazy {
        val pathData = "M109,840Q98,840 89,834.5Q80,829 75,820Q70,811 69.5,800.5Q69,790 75,780L445,140Q451,130 460.5,125" +
            "Q470,120 480,120Q490,120 499.5,125Q509,130 515,140L885,780Q891,790 890.5,800.5Q890,811 885,820" +
            "Q880,829 871,834.5Q862,840 851,840L109,840ZM480,720Q497,720 508.5,708.5Q520,697 520,680Q520,663 508.5,651.5" +
            "Q497,640 480,640Q463,640 451.5,651.5Q440,663 440,680Q440,697 451.5,708.5Q463,720 480,720Z" +
            "M480,600Q497,600 508.5,588.5Q520,577 520,560L520,440Q520,423 508.5,411.5Q497,400 480,400Q463,400 451.5,411.5" +
            "Q440,423 440,440L440,560Q440,577 451.5,588.5Q463,600 480,600Z"
        ImageVector.Builder("Warning", 24.dp, 24.dp, viewportWidth = 960f, viewportHeight = 960f)
            .addPath(pathData = PathParser().parsePathString(pathData).toNodes(), fill = SolidColor(Color.White))
            .build()
    }

    private fun build(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                fill = SolidColor(Color.Black)
            )
        }.build()
}
