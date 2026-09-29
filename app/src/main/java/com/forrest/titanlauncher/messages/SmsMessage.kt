package com.forrest.titanlauncher.messages

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_messages")
data class SmsMessage(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val phoneNumber: String,

    val body: String,

    val timestamp: Long,

    val incoming: Boolean,

    val isRead: Boolean = false,

    /*
     * Identifies the conversation. For a one-to-one thread this is
     * the other person's normalized number, which is what every
     * existing query already assumes. For a group it is every
     * participant's normalized number, sorted and joined, so the same
     * set of people always resolves to the same thread regardless of
     * who sent a given message.
     */
    val threadKey: String = "",

    /*
     * Who sent this message, for attributing bubbles in a group. Null
     * on a one-to-one thread, where phoneNumber already says.
     */
    val senderNumber: String? = null,

    val isGroup: Boolean = false
)