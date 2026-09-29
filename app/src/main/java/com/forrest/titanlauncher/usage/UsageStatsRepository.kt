package com.forrest.titanlauncher.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.forrest.titanlauncher.settings.LauncherSettingsStore
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

data class CurrentHourUsageDiagnostics(
    val totalActiveMs: Long,
    val distractingMs: Long,
    val distractingSharePercent: Int,
    val status: HourProductivityStatus
)

class UsageStatsRepository(
    context: Context
) {

    companion object {
        private const val HOUR_MS =
            60L * 60L * 1000L

    }

    private val appContext =
        context.applicationContext

    private val usageStatsManager =
        appContext.getSystemService(
            Context.USAGE_STATS_SERVICE
        ) as UsageStatsManager

    private val classifier =
        ProductivityClassifier()

    private val launcherSettingsStore =
        LauncherSettingsStore(
            appContext
        )

    private var latestHourlyTotals =
        List(24) {
            HourUsageTotals()
        }

    fun hasUsageAccess(): Boolean {
        val appOpsManager =
            appContext.getSystemService(
                Context.APP_OPS_SERVICE
            ) as AppOpsManager

        val mode =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                appOpsManager
                    .unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        appContext.packageName
                    )
            } else {
                @Suppress("DEPRECATION")
                appOpsManager
                    .checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        appContext.packageName
                    )
            }

        return mode ==
                AppOpsManager.MODE_ALLOWED
    }

    fun loadTodayHourlyStatuses(): List<HourProductivityStatus> {
        if (
            !hasUsageAccess()
        ) {
            return List(24) {
                HourProductivityStatus.GRAY
            }
        }

        val distractingPackages =
            launcherSettingsStore
                .load()
                .distractingApps

        val now =
            System.currentTimeMillis()

        val calendar =
            Calendar.getInstance()

        val currentHour =
            calendar.get(
                Calendar.HOUR_OF_DAY
            )

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )
        calendar.set(
            Calendar.MINUTE,
            0
        )
        calendar.set(
            Calendar.SECOND,
            0
        )
        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        val dayStart =
            calendar.timeInMillis

        val dayEnd =
            dayStart +
                    24L * HOUR_MS

        val totals =
            MutableList(24) {
                MutableHourUsageTotals()
            }

        val events =
            usageStatsManager
                .queryEvents(
                    dayStart,
                    now
                )

        val event =
            UsageEvents.Event()

        var activePackage: String? =
            null

        var activeSince =
            0L

        while (
            events.hasNextEvent()
        ) {
            events.getNextEvent(
                event
            )

            if (
                event.timeStamp <
                dayStart
            ) {
                continue
            }

            val packageName =
                event.packageName
                    .orEmpty()

            when (
                event.eventType
            ) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (
                        packageName.isBlank()
                    ) {
                        continue
                    }

                    if (
                        activePackage == null
                    ) {
                        activePackage =
                            packageName
                        activeSince =
                            event.timeStamp
                    } else if (
                        activePackage !=
                        packageName
                    ) {
                        addDuration(
                            packageName =
                                activePackage!!,
                            startTime =
                                activeSince,
                            endTime =
                                event.timeStamp,
                            dayStart =
                                dayStart,
                            dayEnd =
                                dayEnd,
                            distractingPackages =
                                distractingPackages,
                            totals =
                                totals
                        )

                        activePackage =
                            packageName
                        activeSince =
                            event.timeStamp
                    }
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (
                        activePackage ==
                        packageName
                    ) {
                        addDuration(
                            packageName =
                                activePackage!!,
                            startTime =
                                activeSince,
                            endTime =
                                event.timeStamp,
                            dayStart =
                                dayStart,
                            dayEnd =
                                dayEnd,
                            distractingPackages =
                                distractingPackages,
                            totals =
                                totals
                        )

                        activePackage =
                            null
                        activeSince =
                            0L
                    }
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.SCREEN_INTERACTIVE,
                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    if (
                        activePackage != null
                    ) {
                        addDuration(
                            packageName =
                                activePackage!!,
                            startTime =
                                activeSince,
                            endTime =
                                event.timeStamp,
                            dayStart =
                                dayStart,
                            dayEnd =
                                dayEnd,
                            distractingPackages =
                                distractingPackages,
                            totals =
                                totals
                        )

                        activePackage =
                            null
                        activeSince =
                            0L
                    }
                }
            }
        }

        if (
            activePackage != null &&
            activeSince > 0L
        ) {
            addDuration(
                packageName =
                    activePackage!!,
                startTime =
                    activeSince,
                endTime =
                    now,
                dayStart =
                    dayStart,
                dayEnd =
                    dayEnd,
                distractingPackages =
                    distractingPackages,
                totals =
                    totals
            )
        }

        latestHourlyTotals =
            List(24) {
                    hour ->

                HourUsageTotals(
                    totalActiveMs =
                        totals[hour]
                            .totalActiveMs,
                    distractingMs =
                        totals[hour]
                            .distractingMs
                )
            }

        return List(24) {
                hour ->

            classifier.classify(
                totals =
                    latestHourlyTotals[hour],
                isFutureHour =
                    hour > currentHour
            )
        }
    }

    fun loadCurrentHourDiagnostics(): CurrentHourUsageDiagnostics? {
        if (
            !hasUsageAccess()
        ) {
            return null
        }

        val statuses =
            loadTodayHourlyStatuses()

        val currentHour =
            Calendar.getInstance()
                .get(
                    Calendar.HOUR_OF_DAY
                )

        val totals =
            latestHourlyTotals
                .getOrElse(
                    currentHour
                ) {
                    HourUsageTotals()
                }

        val sharePercent =
            if (
                totals.totalActiveMs > 0L
            ) {
                (
                        totals.distractingMs
                            .toDouble() /
                                totals.totalActiveMs
                                    .toDouble() *
                                100.0
                        )
                    .toInt()
                    .coerceIn(
                        0,
                        100
                    )
            } else {
                0
            }

        return CurrentHourUsageDiagnostics(
            totalActiveMs =
                totals.totalActiveMs,
            distractingMs =
                totals.distractingMs,
            distractingSharePercent =
                sharePercent,
            status =
                statuses
                    .getOrElse(
                        currentHour
                    ) {
                        HourProductivityStatus.GRAY
                    }
        )
    }

    private fun addDuration(
        packageName: String,
        startTime: Long,
        endTime: Long,
        dayStart: Long,
        dayEnd: Long,
        distractingPackages: Set<String>,
        totals: MutableList<MutableHourUsageTotals>
    ) {
        var cursor =
            max(
                startTime,
                dayStart
            )

        val finalTime =
            min(
                endTime,
                dayEnd
            )

        if (
            finalTime <= cursor
        ) {
            return
        }

        while (
            cursor < finalTime
        ) {
            val hour =
                ((cursor - dayStart) /
                        HOUR_MS)
                    .toInt()
                    .coerceIn(
                        0,
                        23
                    )

            val hourEnd =
                dayStart +
                        (hour + 1L) *
                        HOUR_MS

            val sliceEnd =
                min(
                    finalTime,
                    hourEnd
                )

            val duration =
                sliceEnd -
                        cursor

            totals[hour]
                .totalActiveMs +=
                duration

            if (
                classifier.isDistracting(
                    packageName,
                    distractingPackages
                )
            ) {
                totals[hour]
                    .distractingMs +=
                    duration
            }

            cursor =
                sliceEnd
        }
    }

    private data class MutableHourUsageTotals(
        var totalActiveMs: Long = 0L,
        var distractingMs: Long = 0L
    )
}
