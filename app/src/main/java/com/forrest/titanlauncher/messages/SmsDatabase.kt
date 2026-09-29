package com.forrest.titanlauncher.messages

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SmsMessage::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SmsDatabase :
    RoomDatabase() {

    abstract fun smsDao():
            SmsDao

    companion object {

        /*
         * Adds the group columns and backfills threadKey for every
         * existing row from its phone number, so old one-to-one
         * threads keep working untouched.
         *
         * The normalisation here mirrors normalizePhoneNumber: strip
         * everything but digits, then keep the last ten.
         */
        private val Migration1To2 =
            object : Migration(
                1,
                2
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        "ALTER TABLE sms_messages ADD COLUMN threadKey TEXT NOT NULL DEFAULT ''"
                    )

                    database.execSQL(
                        "ALTER TABLE sms_messages ADD COLUMN senderNumber TEXT"
                    )

                    database.execSQL(
                        "ALTER TABLE sms_messages ADD COLUMN isGroup INTEGER NOT NULL DEFAULT 0"
                    )

                    database.execSQL(
                        """
                        UPDATE sms_messages
                        SET threadKey =
                            SUBSTR(
                                REPLACE(
                                    REPLACE(
                                        REPLACE(
                                            REPLACE(
                                                REPLACE(phoneNumber, ' ', ''),
                                                '-', ''
                                            ),
                                            '(', ''
                                        ),
                                        ')', ''
                                    ),
                                    '+', ''
                                ),
                                -10
                            )
                        """
                    )
                }
            }

        @Volatile
        private var INSTANCE:
                SmsDatabase? = null

        fun getInstance(
            context: Context
        ): SmsDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    INSTANCE
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            SmsDatabase::class.java,
                            "titan_sms.db"
                        )
                            .addMigrations(
                                Migration1To2
                            )
                            .build()
                            .also {
                                INSTANCE = it
                            }
                }
        }
    }
}