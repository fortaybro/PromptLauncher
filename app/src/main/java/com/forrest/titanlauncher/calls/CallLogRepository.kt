package com.forrest.titanlauncher.calls

import android.content.Context
import android.provider.CallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CallLogEntry(
    val id: Long,
    val phoneNumber: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val type: CallType
)

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    OTHER
}

class CallLogRepository(
    private val context: Context
) {

    suspend fun loadRecentCalls(
        limit: Int = 30
    ): List<CallLogEntry> {

        return withContext(Dispatchers.IO) {

            val calls =
                mutableListOf<CallLogEntry>()

            val projection =
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.TYPE
                )

            try {

                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )?.use { cursor ->

                    val idIndex =
                        cursor.getColumnIndexOrThrow(
                            CallLog.Calls._ID
                        )

                    val numberIndex =
                        cursor.getColumnIndexOrThrow(
                            CallLog.Calls.NUMBER
                        )

                    val dateIndex =
                        cursor.getColumnIndexOrThrow(
                            CallLog.Calls.DATE
                        )

                    val durationIndex =
                        cursor.getColumnIndexOrThrow(
                            CallLog.Calls.DURATION
                        )

                    val typeIndex =
                        cursor.getColumnIndexOrThrow(
                            CallLog.Calls.TYPE
                        )

                    while (
                        cursor.moveToNext() &&
                        calls.size < limit
                    ) {

                        val androidType =
                            cursor.getInt(
                                typeIndex
                            )

                        val type =
                            when (androidType) {

                                CallLog.Calls.INCOMING_TYPE ->
                                    CallType.INCOMING

                                CallLog.Calls.OUTGOING_TYPE ->
                                    CallType.OUTGOING

                                CallLog.Calls.MISSED_TYPE ->
                                    CallType.MISSED

                                CallLog.Calls.REJECTED_TYPE ->
                                    CallType.REJECTED

                                else ->
                                    CallType.OTHER
                            }

                        calls.add(
                            CallLogEntry(
                                id =
                                    cursor.getLong(
                                        idIndex
                                    ),
                                phoneNumber =
                                    cursor.getString(
                                        numberIndex
                                    )
                                        ?: "Unknown",
                                timestamp =
                                    cursor.getLong(
                                        dateIndex
                                    ),
                                durationSeconds =
                                    cursor.getLong(
                                        durationIndex
                                    ),
                                type =
                                    type
                            )
                        )
                    }
                }

            } catch (
                _: SecurityException
            ) {

                return@withContext emptyList<CallLogEntry>()
            }

            calls
        }
    }
}
