package com.forrest.titanlauncher.commands

object CommandParser {

    fun parse(
        input: String
    ): Command {

        val cleanedInput =
            input.trim()

        if (
            cleanedInput.isEmpty()
        ) {

            return Command.Empty
        }

        val lowercaseInput =
            cleanedInput.lowercase()

        when (
            lowercaseInput
        ) {

            "m",
            "messages" -> {

                return Command.OpenMessages
            }

            "p",
            "phone" -> {

                return Command.OpenPhone
            }

            "g",
            "maps" -> {

                return Command.OpenMaps
            }

            "c",
            "chrome",
            "browser",
            "web" -> {

                return Command.OpenChrome
            }

            "s",
            "settings" -> {

                return Command.OpenSettings
            }

            "smssetup",
            "sms setup",
            "default sms" -> {

                return Command.RequestSmsRole
            }

            "keys",
            "keyprobe",
            "key probe",
            "keytest" -> {

                return Command.OpenKeyProbe
            }

            "todoist",
            "todoist status" -> {

                return Command.TodoistStatus
            }

            "todoist disconnect" -> {

                return Command.DisconnectTodoist
            }
        }

        /*
            TODOIST SETUP

            todoistsetup YOUR_TOKEN
        */

        if (
            lowercaseInput.startsWith(
                "todoistsetup "
            )
        ) {

            val token =
                cleanedInput
                    .substringAfter(" ")
                    .trim()

            if (
                token.isNotEmpty()
            ) {

                return Command.SetupTodoist(
                    token
                )
            }
        }

        /*
            MESSAGE

            @shannon

            @shannon I'm on my way
        */

        if (
            cleanedInput.startsWith("@")
        ) {

            return parseMessageCommand(
                cleanedInput
            )
        }

        /*
            CALL

            #shannon
        */

        if (
            cleanedInput.startsWith("#")
        ) {

            val contactName =
                cleanedInput
                    .removePrefix("#")
                    .trim()

            if (
                contactName.isNotEmpty()
            ) {

                return Command.CallContact(
                    contactName
                )
            }

            return Command.Unknown(
                cleanedInput
            )
        }

        /*
            TODOIST TASK

            -buy milk
        */

        if (
            cleanedInput.startsWith("-")
        ) {

            val title =
                cleanedInput
                    .removePrefix("-")
                    .trim()

            if (
                title.isNotEmpty()
            ) {

                return Command.AddTodoistTask(
                    title
                )
            }

            return Command.Unknown(
                cleanedInput
            )
        }

        /*
            Legacy call command
        */

        if (
            lowercaseInput.startsWith(
                "call "
            )
        ) {

            val contactName =
                cleanedInput
                    .substringAfter(" ")
                    .trim()

            if (
                contactName.isNotEmpty()
            ) {

                return Command.CallContact(
                    contactName
                )
            }
        }

        /*
            Navigation
        */

        if (
            lowercaseInput.startsWith(
                "nav "
            ) ||
            lowercaseInput.startsWith(
                "navigate "
            )
        ) {

            val destination =
                cleanedInput
                    .substringAfter(" ")
                    .trim()

            if (
                destination.isNotEmpty()
            ) {

                return Command.Navigate(
                    destination
                )
            }
        }

        return Command.Unknown(
            cleanedInput
        )
    }

    private fun parseMessageCommand(
        input: String
    ): Command {

        val withoutAt =
            input
                .removePrefix("@")
                .trim()

        if (
            withoutAt.isEmpty()
        ) {

            return Command.Unknown(
                input
            )
        }

        val firstSpaceIndex =
            withoutAt.indexOf(" ")

        if (
            firstSpaceIndex == -1
        ) {

            return Command.OpenConversation(
                contactName =
                    withoutAt
            )
        }

        val contactName =
            withoutAt
                .substring(
                    0,
                    firstSpaceIndex
                )
                .trim()

        val message =
            withoutAt
                .substring(
                    firstSpaceIndex + 1
                )
                .trim()

        if (
            message.isEmpty()
        ) {

            return Command.OpenConversation(
                contactName =
                    contactName
            )
        }

        return Command.SendMessage(
            contactName =
                contactName,
            message =
                message
        )
    }
}