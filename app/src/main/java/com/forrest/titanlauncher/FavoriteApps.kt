package com.forrest.titanlauncher

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap


/*
 * FAVORITE APPS
 *
 * Six assignable slots reached from the chevron button in the home
 * header. Assignments are stored by package name, so an app that gets
 * uninstalled simply shows its slot as empty again rather than
 * crashing on launch.
 */


internal const val FavoriteAppSlotCount =
    6


class FavoriteAppsStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    /*
     * Returns exactly FavoriteAppSlotCount entries. A null entry is an
     * unassigned slot.
     */
    fun load(): List<String?> {

        return (0 until FavoriteAppSlotCount)
            .map { slotIndex ->

                preferences
                    .getString(
                        keyForSlot(
                            slotIndex
                        ),
                        null
                    )
                    ?.takeIf {
                        it.isNotBlank()
                    }
            }
    }

    fun assign(
        slotIndex: Int,
        packageName: String
    ): List<String?> {

        preferences
            .edit()
            .putString(
                keyForSlot(
                    slotIndex
                ),
                packageName
            )
            .apply()

        return load()
    }

    fun clear(
        slotIndex: Int
    ): List<String?> {

        preferences
            .edit()
            .remove(
                keyForSlot(
                    slotIndex
                )
            )
            .apply()

        return load()
    }

    private fun keyForSlot(
        slotIndex: Int
    ): String {

        return "favorite_slot_$slotIndex"
    }

    companion object {

        private const val PREFS_NAME =
            "prompt_launcher_favorite_apps"
    }
}


/*
 * A small round icon button for the home header.
 */
@Composable
internal fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = PrimaryText,
    onClick: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .size(
                    30.dp
                )
                .background(
                    SurfaceBlack,
                    CircleShape
                )
                .border(
                    0.8.dp,
                    BorderGray,
                    CircleShape
                )
                .clickable {
                    onClick()
                },
        contentAlignment =
            Alignment.Center
    ) {

        Icon(
            imageVector =
                icon,
            contentDescription =
                contentDescription,
            tint =
                tint,
            modifier =
                Modifier
                    .size(
                        16.dp
                    )
        )
    }
}


/*
 * Shared by the favorites grid and the app search list.
 */
@Composable
internal fun rememberAppIconBitmap(
    packageName: String?
): ImageBitmap? {

    val context =
        LocalContext.current

    return remember(
        packageName
    ) {

        if (
            packageName == null
        ) {
            null
        } else {
            runCatching {
                context
                    .packageManager
                    .getApplicationIcon(
                        packageName
                    )
                    .toBitmap(
                        96,
                        96
                    )
                    .asImageBitmap()
            }
                .getOrNull()
        }
    }
}


@Composable
private fun rememberAppLabel(
    packageName: String?
): String? {

    val context =
        LocalContext.current

    return remember(
        packageName
    ) {

        if (
            packageName == null
        ) {
            null
        } else {
            runCatching {
                val packageManager =
                    context.packageManager

                packageManager
                    .getApplicationLabel(
                        packageManager
                            .getApplicationInfo(
                                packageName,
                                0
                            )
                    )
                    .toString()
            }
                .getOrNull()
        }
    }
}


@Composable
internal fun FavoriteAppsOverlayCard(
    favorites: List<String?>,
    onLaunch: (String) -> Unit,
    onAssignRequest: (Int) -> Unit,
    onClear: (Int) -> Unit,
    onDismiss: () -> Unit
) {

    var editing by remember {
        mutableStateOf(
            false
        )
    }

    fun activateSlot(
        slotIndex: Int
    ) {

        val packageName =
            favorites.getOrNull(
                slotIndex
            )

        if (
            editing ||
            packageName == null
        ) {
            onAssignRequest(
                slotIndex
            )
        } else {
            onLaunch(
                packageName
            )
        }
    }

    BackHandler {
        onDismiss()
    }

    Column(
        modifier =
            Modifier
                .widthIn(
                    max = 215.dp
                )
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .border(
                    0.8.dp,
                    BorderGray,
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures {
                        // Consume taps inside the card.
                    }
                }
                .onPreviewKeyEvent { event ->

                    if (
                        event.type !=
                        KeyEventType.KeyDown
                    ) {
                        false
                    } else {
                        when (
                            event.key
                        ) {
                            Key.Escape -> {
                                onDismiss()
                                true
                            }

                            Key.One -> {
                                activateSlot(0)
                                true
                            }

                            Key.Two -> {
                                activateSlot(1)
                                true
                            }

                            Key.Three -> {
                                activateSlot(2)
                                true
                            }

                            Key.Four -> {
                                activateSlot(3)
                                true
                            }

                            Key.Five -> {
                                activateSlot(4)
                                true
                            }

                            Key.Six -> {
                                activateSlot(5)
                                true
                            }

                            else ->
                                false
                        }
                    }
                }
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "favorites",
                color =
                    PrimaryText,
                fontSize =
                    14.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium
            )

            Spacer(
                modifier =
                    Modifier
                        .weight(1f)
            )

            Text(
                text =
                    if (
                        editing
                    ) {
                        "done"
                    } else {
                        "edit"
                    },
                color =
                    AccentOrange,
                fontSize =
                    11.sp,
                fontFamily =
                    InterfaceFont,
                modifier =
                    Modifier
                        .clickable {
                            editing =
                                !editing
                        }
                        .padding(
                            horizontal = 4.dp,
                            vertical = 2.dp
                        )
            )
        }

        Spacer(
            modifier =
                Modifier
                    .height(
                        8.dp
                    )
        )

        /*
         * Six slots laid out as three rows of two.
         */
        (0 until FavoriteAppSlotCount)
            .chunked(
                2
            )
            .forEach { rowSlots ->

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            7.dp
                        )
                ) {

                    rowSlots.forEach { slotIndex ->

                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                        ) {

                            FavoriteSlotTile(
                                slotIndex =
                                    slotIndex,
                                packageName =
                                    favorites.getOrNull(
                                        slotIndex
                                    ),
                                editing =
                                    editing,
                                onActivate = {
                                    activateSlot(
                                        slotIndex
                                    )
                                },
                                onClear = {
                                    onClear(
                                        slotIndex
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier
                            .height(
                                7.dp
                            )
                )
            }
    }
}


@Composable
private fun FavoriteSlotTile(
    slotIndex: Int,
    packageName: String?,
    editing: Boolean,
    onActivate: () -> Unit,
    onClear: () -> Unit
) {

    val icon =
        rememberAppIconBitmap(
            packageName
        )

    val label =
        rememberAppLabel(
            packageName
        )

    Box {

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        62.dp
                    )
                    .background(
                        InputSurface,
                        RoundedCornerShape(
                            10.dp
                        )
                    )
                    .border(
                        0.75.dp,
                        if (
                            editing
                        ) {
                            AccentOrange
                        } else {
                            BorderGray
                        },
                        RoundedCornerShape(
                            10.dp
                        )
                    )
                    .clickable {
                        onActivate()
                    }
                    .padding(
                        4.dp
                    )
        ) {

            /*
             * Icon only, centred and sized to fill the tile. The
             * label is gone: the icon is the recognisable part, and
             * dropping the text frees the whole square for it.
             */
            Box(
                modifier =
                    Modifier
                        .align(
                            Alignment.Center
                        )
            ) {

                if (
                    packageName == null
                ) {

                    Text(
                        text =
                            "+",
                        color =
                            SecondaryText,
                        fontSize =
                            24.sp,
                        fontFamily =
                            InterfaceFont
                    )
                } else if (
                    icon != null
                ) {

                    androidx.compose.foundation.Image(
                        bitmap =
                            icon,
                        contentDescription =
                            label,
                        modifier =
                            Modifier
                                .size(
                                    38.dp
                                )
                    )
                } else {

                    Text(
                        text =
                            "?",
                        color =
                            SecondaryText,
                        fontSize =
                            24.sp,
                        fontFamily =
                            InterfaceFont
                    )
                }
            }

        }

        /*
         * The clear badge only appears in edit mode on a filled slot.
         */
        if (
            editing &&
            packageName != null
        ) {

            Box(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .size(
                            16.dp
                        )
                        .background(
                            BackgroundBlack,
                            CircleShape
                        )
                        .border(
                            0.75.dp,
                            BorderGray,
                            CircleShape
                        )
                        .clickable {
                            onClear()
                        },
                contentAlignment =
                    Alignment.Center
            ) {

                /*
                 * A vector glyph rather than a "×" character: text is
                 * centred by its line box, which leaves the
                 * multiplication sign sitting high in the circle.
                 */
                Icon(
                    imageVector =
                        Icons.Outlined.Close,
                    contentDescription =
                        "clear slot",
                    tint =
                        SecondaryText,
                    modifier =
                        Modifier
                            .size(
                                9.dp
                            )
                )
            }
        }
    }
}


/*
 * FAVORITES OVERLAY
 *
 * Owns the dimmed backdrop and the slot grid. While a slot is being
 * assigned the app picker takes the grid's place.
 */
@Composable
internal fun FavoritesOverlay(
    favorites: List<String?>,
    assigningSlot: Int?,
    onLaunch: (String) -> Unit,
    onAssignRequest: (Int) -> Unit,
    onAssignApp: (Int, LauncherAppEntry) -> Unit,
    onCancelAssign: () -> Unit,
    onClear: (Int) -> Unit,
    onDismiss: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.18f
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures {
                        onDismiss()
                    }
                },
        contentAlignment =
            Alignment.Center
    ) {

        if (
            assigningSlot == null
        ) {

            FavoriteAppsOverlayCard(
                favorites =
                    favorites,
                onLaunch =
                    onLaunch,
                onAssignRequest =
                    onAssignRequest,
                onClear =
                    onClear,
                onDismiss =
                    onDismiss
            )
        } else {

            AppSearchOverlayCard(
                title =
                    "slot ${assigningSlot + 1}",
                onLaunchApp = { app ->
                    onAssignApp(
                        assigningSlot,
                        app
                    )
                },
                onDismiss =
                    onCancelAssign
            )
        }
    }
}