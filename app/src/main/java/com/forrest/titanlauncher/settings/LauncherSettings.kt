package com.forrest.titanlauncher.settings

val DefaultDistractingApps =
    setOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.facebook.katana",
        "com.reddit.frontpage",
        "com.twitter.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.snapchat.android",
        "tv.twitch.android.app",
        "com.netflix.mediaclient",
        "com.hulu.plus",
        "com.disney.disneyplus",
        "com.amazon.avod.thirdpartyclient",
        "com.wbd.stream",
        "com.peacocktv.peacockandroid"
    )

enum class LauncherThemeMode {
    DARK,
    LIGHT,
    NAVY,
    SLATE,
    TAN,
    FOREST
}

/*
 * Overrides the palette's own text colours. DEFAULT leaves each theme
 * to supply its own.
 */
enum class LauncherTextColor {
    DEFAULT,
    WHITE,
    BLACK,
    WARM,
    COOL,
    ACCENT
}

/*
 * The muted text: labels, previews, timestamps, hints. Separate from
 * LauncherTextColor because the useful choices are different — steps
 * of grey rather than a foreground colour.
 */
enum class LauncherSecondaryTextColor {
    DEFAULT,
    LIGHT,
    MEDIUM,
    DARK,
    WARM,
    COOL,
    ACCENT
}

enum class LauncherAccent {
    ORANGE,
    RED,
    YELLOW,
    GREEN,
    BLUE,
    PURPLE,
    PINK
}

enum class LauncherInterfaceFont {
    MONO,
    SANS,
    SERIF,
    POPPINS
}

enum class ReadabilityTextSize {
    SMALL,
    NORMAL,
    LARGE,
    EXTRA_LARGE
}

enum class ReadabilityTextWeight {
    NORMAL,
    MEDIUM,
    BOLD
}

enum class ReadabilityContrast {
    STANDARD,
    HIGH,
    MAXIMUM
}

enum class ReadabilityMotion {
    NORMAL,
    REDUCED
}

enum class HomeSurfaceTone {
    BLACK,
    CHARCOAL,
    GRAY
}

enum class CommandSurfaceTone {
    DARK,
    MEDIUM,
    LIGHT
}

enum class HomeElementSize {
    SMALL,
    NORMAL,
    LARGE
}

enum class HomeCornerStyle {
    SQUARE,
    SOFT,
    ROUND
}

data class LauncherSettings(
    val themeMode: LauncherThemeMode = LauncherThemeMode.DARK,
    val textColor: LauncherTextColor = LauncherTextColor.DEFAULT,
    val secondaryTextColor: LauncherSecondaryTextColor =
        LauncherSecondaryTextColor.DEFAULT,
    val accent: LauncherAccent = LauncherAccent.ORANGE,
    val interfaceFont: LauncherInterfaceFont = LauncherInterfaceFont.MONO,
    val appButtonSurface: HomeSurfaceTone = HomeSurfaceTone.BLACK,
    val infoCardSurface: HomeSurfaceTone = HomeSurfaceTone.BLACK,
    val commandSurface: CommandSurfaceTone = CommandSurfaceTone.DARK,
    val buttonIconSize: HomeElementSize = HomeElementSize.NORMAL,
    val buttonLabelSize: HomeElementSize = HomeElementSize.NORMAL,
    val buttonCorners: HomeCornerStyle = HomeCornerStyle.SOFT,
    val showWeather: Boolean = true,
    /*
     * Show temperatures in Celsius instead of Fahrenheit.
     */
    val weatherCelsius: Boolean = false,

    /*
     * Offers chrome or claude above the prompt bar when free text is
     * typed. Off means free text goes straight to the browser, the
     * way it did before the picker existed.
     */
    val showSearchTargetPicker: Boolean = true,
    /*
     * Which chips the search picker offers. With every one switched
     * off the picker behaves as if it were off.
     */
    val searchChipChrome: Boolean = true,
    val searchChipClaude: Boolean = true,
    val searchChipApps: Boolean = true,
    /*
     * Chips above the prompt while typing "+": which task app the
     * new task goes to.
     */
    val showTaskPicker: Boolean = true,
    val taskChipGoogle: Boolean = true,
    val taskChipTodoist: Boolean = true,
    /*
     * Package of the mail app new emails are written in. Blank means
     * Prompt Launcher's own Gmail compose.
     */
    val mailComposeApp: String = "",
    /*
     * Email apps picked in onboarding or settings, in order. Their
     * notifications fill the hub's email tab and the first one opens
     * for "mail". Empty means every installed email app.
     */
    val mailApps: List<String> = emptyList(),
    val showProductivityDots: Boolean = true,
    val showCalendarCard: Boolean = true,
    val showAttentionCard: Boolean = true,
    val readabilityTextSize: ReadabilityTextSize = ReadabilityTextSize.NORMAL,
    val readabilityTextWeight: ReadabilityTextWeight = ReadabilityTextWeight.NORMAL,
    val readabilityContrast: ReadabilityContrast = ReadabilityContrast.STANDARD,
    val readabilityMotion: ReadabilityMotion = ReadabilityMotion.NORMAL,
    val dimSecondaryText: Boolean = true,
    val distractingApps: Set<String> = DefaultDistractingApps,
    val mutedNotificationApps: Set<String> = emptySet()
)