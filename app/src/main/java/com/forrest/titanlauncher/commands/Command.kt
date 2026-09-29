package com.forrest.titanlauncher.commands

sealed class Command {

    data object OpenMessages :
        Command()

    data object OpenPhone :
        Command()

    data object OpenMaps :
        Command()

    data object OpenChrome :
        Command()

    data object OpenSettings :
        Command()

    data object RequestSmsRole :
        Command()

    data object OpenKeyProbe :
        Command()

    data class SendMessage(
        val contactName: String,
        val message: String
    ) : Command()

    data class OpenConversation(
        val contactName: String
    ) : Command()

    data class CallContact(
        val contactName: String
    ) : Command()

    data class Navigate(
        val destination: String
    ) : Command()

    data class AddTodoistTask(
        val title: String
    ) : Command()

    data class SetupTodoist(
        val token: String
    ) : Command()

    data object TodoistStatus :
        Command()

    data object DisconnectTodoist :
        Command()

    data class Unknown(
        val rawInput: String
    ) : Command()

    data object Empty :
        Command()
}