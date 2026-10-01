package com.forrest.titanlauncher.todoist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TodoistTaskResult(
    val success: Boolean,
    val message: String
)

class TodoistRepository(
    private val tokenStore: TodoistTokenStore
) {

    suspend fun createTask(
        title: String
    ): TodoistTaskResult {

        val cleanedTitle =
            title.trim()

        if (
            cleanedTitle.isBlank()
        ) {
            return TodoistTaskResult(
                success = false,
                message = "TASK IS EMPTY"
            )
        }

        return when (
            tokenStore.getTaskProvider()
        ) {

            TaskProvider.GOOGLE_TASKS -> {
                createGoogleTask(
                    cleanedTitle
                )
            }

            TaskProvider.TODOIST -> {
                createTodoistTask(
                    cleanedTitle
                )
            }

            TaskProvider.NONE -> {
                TodoistTaskResult(
                    success = false,
                    message = "RUN TASKSSETUP TO CONNECT TASKS"
                )
            }
        }
    }

    // ---------------------------------------------------------
    // GOOGLE TASKS
    // ---------------------------------------------------------

    private suspend fun createGoogleTask(
        title: String
    ): TodoistTaskResult {

        /*
         * We ask AuthorizationClient for a fresh/current access
         * token each time. Google caches grants, so this should
         * not present another consent screen once the user has
         * connected Google Tasks during onboarding.
         */
        val accessToken =
            tokenStore
                .getGoogleTasksAccessToken()

        if (
            accessToken.isNullOrBlank()
        ) {
            return TodoistTaskResult(
                success = false,
                message =
                    "RECONNECT GOOGLE TASKS"
            )
        }

        return withContext(
            Dispatchers.IO
        ) {

            var connection:
                    HttpURLConnection? =
                null

            try {

                /*
                 * @default is Google's special identifier for
                 * the user's default Google Tasks list.
                 */
                val url =
                    URL(
                        "https://tasks.googleapis.com/tasks/v1/lists/@default/tasks"
                    )

                connection =
                    url.openConnection() as
                            HttpURLConnection

                connection.requestMethod =
                    "POST"

                connection.connectTimeout =
                    15_000

                connection.readTimeout =
                    15_000

                connection.doOutput =
                    true

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $accessToken"
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val requestBody =
                    JSONObject()
                        .apply {
                            put(
                                "title",
                                title
                            )
                        }
                        .toString()

                connection
                    .outputStream
                    .bufferedWriter(
                        Charsets.UTF_8
                    )
                    .use {
                            writer ->

                        writer.write(
                            requestBody
                        )

                        writer.flush()
                    }

                val responseCode =
                    connection.responseCode

                when (
                    responseCode
                ) {

                    in 200..299 -> {
                        TodoistTaskResult(
                            success = true,
                            message =
                                "✓ ADDED TO GOOGLE TASKS"
                        )
                    }

                    401 -> {
                        TodoistTaskResult(
                            success = false,
                            message =
                                "GOOGLE TASKS AUTH EXPIRED"
                        )
                    }

                    403 -> {
                        TodoistTaskResult(
                            success = false,
                            message =
                                "GOOGLE TASKS ACCESS DENIED"
                        )
                    }

                    else -> {
                        TodoistTaskResult(
                            success = false,
                            message =
                                "GOOGLE TASKS ERROR $responseCode"
                        )
                    }
                }

            } catch (
                _: Exception
            ) {

                TodoistTaskResult(
                    success = false,
                    message =
                        "GOOGLE TASKS UNAVAILABLE"
                )

            } finally {

                connection
                    ?.disconnect()
            }
        }
    }

    // ---------------------------------------------------------
    // TODOIST
    // ---------------------------------------------------------

    private suspend fun createTodoistTask(
        title: String
    ): TodoistTaskResult {

        val token =
            tokenStore
                .getToken()

        if (
            token.isBlank()
        ) {
            return TodoistTaskResult(
                success = false,
                message =
                    "TODOIST NOT CONNECTED"
            )
        }

        return withContext(
            Dispatchers.IO
        ) {

            var connection:
                    HttpURLConnection? =
                null

            try {

                /*
                 * Todoist's current API endpoint is /api/v1/tasks.
                 * This also updates the old Todoist integration
                 * while we're here.
                 */
                val url =
                    URL(
                        "https://api.todoist.com/api/v1/tasks"
                    )

                connection =
                    url.openConnection() as
                            HttpURLConnection

                connection.requestMethod =
                    "POST"

                connection.connectTimeout =
                    15_000

                connection.readTimeout =
                    15_000

                connection.doOutput =
                    true

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val requestBody =
                    JSONObject()
                        .apply {
                            put(
                                "content",
                                title
                            )
                        }
                        .toString()

                connection
                    .outputStream
                    .bufferedWriter(
                        Charsets.UTF_8
                    )
                    .use {
                            writer ->

                        writer.write(
                            requestBody
                        )

                        writer.flush()
                    }

                val responseCode =
                    connection.responseCode

                when (
                    responseCode
                ) {

                    in 200..299 -> {
                        TodoistTaskResult(
                            success = true,
                            message =
                                "✓ ADDED TO TODOIST"
                        )
                    }

                    401 -> {
                        TodoistTaskResult(
                            success = false,
                            message =
                                "TODOIST TOKEN INVALID"
                        )
                    }

                    403 -> {
                        TodoistTaskResult(
                            success = false,
                            message =
                                "TODOIST ACCESS DENIED"
                        )
                    }

                    else -> {
                        TodoistTaskResult(
                            success = false,
                            message =
                                "TODOIST ERROR $responseCode"
                        )
                    }
                }

            } catch (
                _: Exception
            ) {

                TodoistTaskResult(
                    success = false,
                    message =
                        "TODOIST UNAVAILABLE"
                )

            } finally {

                connection
                    ?.disconnect()
            }
        }
    }
}