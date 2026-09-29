package com.forrest.titanlauncher.usage

enum class HourProductivityStatus {
    GRAY,
    PRODUCTIVE,
    UNPRODUCTIVE
}

data class HourUsageTotals(
    val totalActiveMs: Long = 0L,
    val distractingMs: Long = 0L
)

class ProductivityClassifier {

    companion object {
        /*
         * The whole rule: an hour is unproductive once more than this
         * much of it went to apps marked distracting in settings.
         * Nothing else is considered.
         */
        private const val DISTRACTING_LIMIT_MS =
            20L * 60L * 1000L
    }


    fun isDistracting(
        packageName: String,
        distractingPackages: Set<String>
    ): Boolean {
        return packageName in
                distractingPackages
    }

    fun classify(
        totals: HourUsageTotals,
        isFutureHour: Boolean
    ): HourProductivityStatus {

        /*
         * Hours later than the current one have not happened yet and
         * stay pending rather than being scored as productive.
         */
        if (
            isFutureHour
        ) {
            return HourProductivityStatus.GRAY
        }

        return if (
            totals.distractingMs >
            DISTRACTING_LIMIT_MS
        ) {
            HourProductivityStatus.UNPRODUCTIVE
        } else {
            HourProductivityStatus.PRODUCTIVE
        }
    }
}