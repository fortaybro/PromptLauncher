package com.forrest.titanlauncher.settings

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LauncherSettingsStore(
    context: Context
) {
    private val preferences =
        context.getSharedPreferences(
            "titan_launcher_settings",
            Context.MODE_PRIVATE
        )

    fun load(): LauncherSettings {
        val themeMode =
            enumValueOrDefault(
                preferences.getString("theme_mode", null),
                LauncherThemeMode.DARK
            )

        val accent =
            enumValueOrDefault(
                preferences.getString("accent", null),
                LauncherAccent.ORANGE
            )

        val textColor =
            enumValueOrDefault(
                preferences.getString("text_color", null),
                LauncherTextColor.DEFAULT
            )

        val secondaryTextColor =
            enumValueOrDefault(
                preferences.getString("secondary_text_color", null),
                LauncherSecondaryTextColor.DEFAULT
            )

        val showSearchTargetPicker =
            preferences.getBoolean(
                "show_search_target_picker",
                true
            )

        val interfaceFont =
            enumValueOrDefault(
                preferences.getString("interface_font", null),
                LauncherInterfaceFont.MONO
            )

        val appButtonSurface =
            enumValueOrDefault(
                preferences.getString("app_button_surface", null),
                HomeSurfaceTone.BLACK
            )

        val infoCardSurface =
            enumValueOrDefault(
                preferences.getString("info_card_surface", null),
                HomeSurfaceTone.BLACK
            )

        val commandSurface =
            enumValueOrDefault(
                preferences.getString("command_surface", null),
                CommandSurfaceTone.DARK
            )

        val buttonIconSize =
            enumValueOrDefault(
                preferences.getString("button_icon_size", null),
                HomeElementSize.NORMAL
            )

        val buttonLabelSize =
            enumValueOrDefault(
                preferences.getString("button_label_size", null),
                HomeElementSize.NORMAL
            )

        val buttonCorners =
            enumValueOrDefault(
                preferences.getString("button_corners", null),
                HomeCornerStyle.SOFT
            )

        val readabilityTextSize =
            enumValueOrDefault(
                preferences.getString("readability_text_size", null),
                ReadabilityTextSize.NORMAL
            )

        val readabilityTextWeight =
            enumValueOrDefault(
                preferences.getString("readability_text_weight", null),
                ReadabilityTextWeight.NORMAL
            )

        val readabilityContrast =
            enumValueOrDefault(
                preferences.getString("readability_contrast", null),
                ReadabilityContrast.STANDARD
            )

        val readabilityMotion =
            enumValueOrDefault(
                preferences.getString("readability_motion", null),
                ReadabilityMotion.NORMAL
            )

        return LauncherSettings(
            themeMode = themeMode,
            accent = accent,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            showSearchTargetPicker = showSearchTargetPicker,
            searchChipChrome = preferences.getBoolean("search_chip_chrome", true),
            searchChipClaude = preferences.getBoolean("search_chip_claude", true),
            searchChipApps = preferences.getBoolean("search_chip_apps", true),
            mailComposeApp = preferences.getString("mail_compose_app", "").orEmpty(),
            mailApps = preferences.getString("mail_apps", "")
                .orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() },
            interfaceFont = interfaceFont,
            appButtonSurface = appButtonSurface,
            infoCardSurface = infoCardSurface,
            commandSurface = commandSurface,
            buttonIconSize = buttonIconSize,
            buttonLabelSize = buttonLabelSize,
            buttonCorners = buttonCorners,
            showWeather = preferences.getBoolean("show_weather", true),
            showProductivityDots = preferences.getBoolean("show_productivity_dots", true),
            showCalendarCard = preferences.getBoolean("show_calendar_card", true),
            showAttentionCard = preferences.getBoolean("show_attention_card", true),
            readabilityTextSize = readabilityTextSize,
            readabilityTextWeight = readabilityTextWeight,
            readabilityContrast = readabilityContrast,
            readabilityMotion = readabilityMotion,
            dimSecondaryText = preferences.getBoolean("dim_secondary_text", true),
            distractingApps =
                preferences
                    .getStringSet(
                        "distracting_apps",
                        DefaultDistractingApps
                    )
                    ?.toSet()
                    ?: DefaultDistractingApps,
            mutedNotificationApps =
                preferences
                    .getStringSet(
                        "muted_notification_apps",
                        emptySet()
                    )
                    ?.toSet()
                    ?: emptySet()
        )
    }

    fun save(settings: LauncherSettings) {
        preferences
            .edit()
            .putString("theme_mode", settings.themeMode.name)
            .putString("accent", settings.accent.name)
            .putString("text_color", settings.textColor.name)
            .putString("secondary_text_color", settings.secondaryTextColor.name)
            .putBoolean("show_search_target_picker", settings.showSearchTargetPicker)
            .putBoolean("search_chip_chrome", settings.searchChipChrome)
            .putBoolean("search_chip_claude", settings.searchChipClaude)
            .putBoolean("search_chip_apps", settings.searchChipApps)
            .putString("mail_compose_app", settings.mailComposeApp)
            .putString("mail_apps", settings.mailApps.joinToString(","))
            .putString("interface_font", settings.interfaceFont.name)
            .putString("app_button_surface", settings.appButtonSurface.name)
            .putString("info_card_surface", settings.infoCardSurface.name)
            .putString("command_surface", settings.commandSurface.name)
            .putString("button_icon_size", settings.buttonIconSize.name)
            .putString("button_label_size", settings.buttonLabelSize.name)
            .putString("button_corners", settings.buttonCorners.name)
            .putBoolean("show_weather", settings.showWeather)
            .putBoolean("show_productivity_dots", settings.showProductivityDots)
            .putBoolean("show_calendar_card", settings.showCalendarCard)
            .putBoolean("show_attention_card", settings.showAttentionCard)
            .putString("readability_text_size", settings.readabilityTextSize.name)
            .putString("readability_text_weight", settings.readabilityTextWeight.name)
            .putString("readability_contrast", settings.readabilityContrast.name)
            .putString("readability_motion", settings.readabilityMotion.name)
            .putBoolean("dim_secondary_text", settings.dimSecondaryText)
            .putStringSet(
                "distracting_apps",
                settings.distractingApps
            )
            .putStringSet(
                "muted_notification_apps",
                settings.mutedNotificationApps
            )
            .apply()
    }

    fun exportBackup(
        settings: LauncherSettings
    ): String {
        val settingsJson =
            JSONObject()
                .put(
                    "theme_mode",
                    settings.themeMode.name
                )
                .put(
                    "accent",
                    settings.accent.name
                )
                .put(
                    "interface_font",
                    settings.interfaceFont.name
                )
                .put(
                    "app_button_surface",
                    settings.appButtonSurface.name
                )
                .put(
                    "info_card_surface",
                    settings.infoCardSurface.name
                )
                .put(
                    "command_surface",
                    settings.commandSurface.name
                )
                .put(
                    "button_icon_size",
                    settings.buttonIconSize.name
                )
                .put(
                    "button_label_size",
                    settings.buttonLabelSize.name
                )
                .put(
                    "button_corners",
                    settings.buttonCorners.name
                )
                .put(
                    "show_weather",
                    settings.showWeather
                )
                .put(
                    "show_productivity_dots",
                    settings.showProductivityDots
                )
                .put(
                    "show_calendar_card",
                    settings.showCalendarCard
                )
                .put(
                    "show_attention_card",
                    settings.showAttentionCard
                )
                .put(
                    "readability_text_size",
                    settings.readabilityTextSize.name
                )
                .put(
                    "readability_text_weight",
                    settings.readabilityTextWeight.name
                )
                .put(
                    "readability_contrast",
                    settings.readabilityContrast.name
                )
                .put(
                    "readability_motion",
                    settings.readabilityMotion.name
                )
                .put(
                    "dim_secondary_text",
                    settings.dimSecondaryText
                )
                .put(
                    "distracting_apps",
                    JSONArray(
                        settings
                            .distractingApps
                            .sorted()
                    )
                )
                .put(
                    "muted_notification_apps",
                    JSONArray(
                        settings
                            .mutedNotificationApps
                            .sorted()
                    )
                )

        return JSONObject()
            .put(
                "format",
                "prompt_launcher_settings_backup"
            )
            .put(
                "version",
                1
            )
            .put(
                "settings",
                settingsJson
            )
            .toString(
                2
            )
    }

    fun importBackup(
        backupJson: String
    ): LauncherSettings {
        val root =
            JSONObject(
                backupJson
            )

        val backupFormat =
            root.optString(
                "format"
            )

        require(
            backupFormat ==
                    "prompt_launcher_settings_backup" ||
                    backupFormat ==
                    "titan_launcher_settings_backup"
        ) {
            "Not a Prompt Launcher settings backup"
        }

        require(
            root.optInt(
                "version",
                -1
            ) == 1
        ) {
            "Unsupported backup version"
        }

        val json =
            root.getJSONObject(
                "settings"
            )

        val defaults =
            LauncherSettings()

        return LauncherSettings(
            themeMode =
                enumValueOrDefault(
                    json.optString(
                        "theme_mode",
                        defaults.themeMode.name
                    ),
                    defaults.themeMode
                ),
            accent =
                enumValueOrDefault(
                    json.optString(
                        "accent",
                        defaults.accent.name
                    ),
                    defaults.accent
                ),
            interfaceFont =
                enumValueOrDefault(
                    json.optString(
                        "interface_font",
                        defaults.interfaceFont.name
                    ),
                    defaults.interfaceFont
                ),
            appButtonSurface =
                enumValueOrDefault(
                    json.optString(
                        "app_button_surface",
                        defaults.appButtonSurface.name
                    ),
                    defaults.appButtonSurface
                ),
            infoCardSurface =
                enumValueOrDefault(
                    json.optString(
                        "info_card_surface",
                        defaults.infoCardSurface.name
                    ),
                    defaults.infoCardSurface
                ),
            commandSurface =
                enumValueOrDefault(
                    json.optString(
                        "command_surface",
                        defaults.commandSurface.name
                    ),
                    defaults.commandSurface
                ),
            buttonIconSize =
                enumValueOrDefault(
                    json.optString(
                        "button_icon_size",
                        defaults.buttonIconSize.name
                    ),
                    defaults.buttonIconSize
                ),
            buttonLabelSize =
                enumValueOrDefault(
                    json.optString(
                        "button_label_size",
                        defaults.buttonLabelSize.name
                    ),
                    defaults.buttonLabelSize
                ),
            buttonCorners =
                enumValueOrDefault(
                    json.optString(
                        "button_corners",
                        defaults.buttonCorners.name
                    ),
                    defaults.buttonCorners
                ),
            showWeather =
                json.optBoolean(
                    "show_weather",
                    defaults.showWeather
                ),
            showProductivityDots =
                json.optBoolean(
                    "show_productivity_dots",
                    defaults.showProductivityDots
                ),
            showCalendarCard =
                json.optBoolean(
                    "show_calendar_card",
                    defaults.showCalendarCard
                ),
            showAttentionCard =
                json.optBoolean(
                    "show_attention_card",
                    defaults.showAttentionCard
                ),
            readabilityTextSize =
                enumValueOrDefault(
                    json.optString(
                        "readability_text_size",
                        defaults.readabilityTextSize.name
                    ),
                    defaults.readabilityTextSize
                ),
            readabilityTextWeight =
                enumValueOrDefault(
                    json.optString(
                        "readability_text_weight",
                        defaults.readabilityTextWeight.name
                    ),
                    defaults.readabilityTextWeight
                ),
            readabilityContrast =
                enumValueOrDefault(
                    json.optString(
                        "readability_contrast",
                        defaults.readabilityContrast.name
                    ),
                    defaults.readabilityContrast
                ),
            readabilityMotion =
                enumValueOrDefault(
                    json.optString(
                        "readability_motion",
                        defaults.readabilityMotion.name
                    ),
                    defaults.readabilityMotion
                ),
            dimSecondaryText =
                json.optBoolean(
                    "dim_secondary_text",
                    defaults.dimSecondaryText
                ),
            distractingApps =
                jsonStringSet(
                    json.optJSONArray(
                        "distracting_apps"
                    )
                )
                    ?: defaults.distractingApps,
            mutedNotificationApps =
                jsonStringSet(
                    json.optJSONArray(
                        "muted_notification_apps"
                    )
                )
                    ?: defaults.mutedNotificationApps
        )
    }

    private fun jsonStringSet(
        array: JSONArray?
    ): Set<String>? {
        if (
            array == null
        ) {
            return null
        }

        return buildSet {
            for (
            index in 0 until array.length()
            ) {
                val value =
                    array.optString(
                        index
                    )

                if (
                    value.isNotBlank()
                ) {
                    add(
                        value
                    )
                }
            }
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String?,
        defaultValue: T
    ): T {
        if (value.isNullOrBlank()) {
            return defaultValue
        }

        return enumValues<T>()
            .firstOrNull {
                it.name == value
            }
            ?: defaultValue
    }
}