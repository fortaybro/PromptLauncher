package com.forrest.titanlauncher.messages

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {

    @Insert
    suspend fun insert(
        message: SmsMessage
    ): Long

    @Query(
        """
        SELECT * FROM sms_messages
        ORDER BY id DESC
        LIMIT 10
        """
    )
    fun observeRecentMessages():
            Flow<List<SmsMessage>>

    @Query(
        """
        SELECT * FROM sms_messages
        ORDER BY id DESC
        """
    )
    fun observeAllMessages():
            Flow<List<SmsMessage>>

    @Query(
        """
        SELECT COUNT(*) FROM sms_messages
        WHERE incoming = 1
        AND isRead = 0
        """
    )
    fun observeUnreadCount():
            Flow<Int>

    /*
     * One-shot read used by the import to work out what is already
     * stored. The observing queries are wrong for that: the importer
     * needs a snapshot, not a stream.
     */
    @Query(
        """
        SELECT * FROM sms_messages
        """
    )
    suspend fun getAllOnce(): List<SmsMessage>

    @Query(
        """
        SELECT COUNT(*) FROM sms_messages
        """
    )
    suspend fun getMessageCount():
            Int

    @Query(
        """
        SELECT * FROM sms_messages
        WHERE
            SUBSTR(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(
                                    REPLACE(
                                        phoneNumber,
                                        '+',
                                        ''
                                    ),
                                    '-',
                                    ''
                                ),
                                '(',
                                ''
                            ),
                            ')',
                            ''
                        ),
                        ' ',
                        ''
                    ),
                    '.',
                    ''
                ),
                -10
            )
            =
            SUBSTR(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(
                                    REPLACE(
                                        :phoneNumber,
                                        '+',
                                        ''
                                    ),
                                    '-',
                                    ''
                                ),
                                '(',
                                ''
                            ),
                            ')',
                            ''
                        ),
                        ' ',
                        ''
                    ),
                    '.',
                    ''
                ),
                -10
            )
        ORDER BY id ASC
        """
    )
    fun observeConversation(
        phoneNumber: String
    ): Flow<List<SmsMessage>>

    /*
     * Thread-keyed lookup, which is what group conversations need:
     * a group has no single "other number" to match on. One-to-one
     * threads work here too, since their key is the other person's
     * normalized number.
     */
    @Query(
        """
        SELECT * FROM sms_messages
        WHERE threadKey = :threadKey
        ORDER BY id ASC
        """
    )
    fun observeThread(
        threadKey: String
    ): Flow<List<SmsMessage>>

    @Query(
        """
        UPDATE sms_messages
        SET isRead = 1
        WHERE incoming = 1
        AND threadKey = :threadKey
        """
    )
    suspend fun markThreadRead(
        threadKey: String
    )

    @Query(
        """
        UPDATE sms_messages
        SET isRead = 1
        WHERE incoming = 1
        AND
            SUBSTR(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(
                                    REPLACE(
                                        phoneNumber,
                                        '+',
                                        ''
                                    ),
                                    '-',
                                    ''
                                ),
                                '(',
                                ''
                            ),
                            ')',
                            ''
                        ),
                        ' ',
                        ''
                    ),
                    '.',
                    ''
                ),
                -10
            )
            =
            SUBSTR(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(
                                    REPLACE(
                                        :phoneNumber,
                                        '+',
                                        ''
                                    ),
                                    '-',
                                    ''
                                ),
                                '(',
                                ''
                            ),
                            ')',
                            ''
                        ),
                        ' ',
                        ''
                    ),
                    '.',
                    ''
                ),
                -10
            )
        """
    )
    suspend fun markConversationRead(
        phoneNumber: String
    )
}