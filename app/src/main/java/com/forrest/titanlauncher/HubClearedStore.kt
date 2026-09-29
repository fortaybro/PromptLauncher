package com.forrest.titanlauncher

import android.content.Context


/*
 * HUB CLEARED ITEMS
 *
 * The hub shows recent activity, not just unread activity, so marking
 * a thread read does not remove it from the list. Clearing is
 * therefore its own piece of state: a set of item ids the user has
 * dismissed.
 *
 * Ids carry the underlying record's identity — the latest message id,
 * the call id, the mail id, the notification key — so a new message
 * from the same person produces a new id and reappears rather than
 * staying hidden behind an old dismissal.
 */

private const val HubClearedPrefsName =
    "prompt_launcher_hub_cleared"

private const val HubClearedKey =
    "cleared_ids"

/*
 * Dismissals are pruned to this many so the set cannot grow without
 * bound over the life of the install.
 */
private const val HubClearedLimit =
    400


class HubClearedStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            HubClearedPrefsName,
            Context.MODE_PRIVATE
        )

    fun load(): Set<String> {

        return preferences
            .getStringSet(
                HubClearedKey,
                emptySet()
            )
            ?.toSet()
            ?: emptySet()
    }

    fun clear(
        ids: Collection<String>
    ): Set<String> {

        if (
            ids.isEmpty()
        ) {
            return load()
        }

        /*
         * Newest dismissals are kept at the end, so pruning drops the
         * oldest ones first.
         */
        val merged =
            (load() + ids)
                .toList()
                .takeLast(
                    HubClearedLimit
                )
                .toSet()

        preferences
            .edit()
            .putStringSet(
                HubClearedKey,
                merged
            )
            .apply()

        return merged
    }

    fun reset(): Set<String> {

        preferences
            .edit()
            .remove(
                HubClearedKey
            )
            .apply()

        return emptySet()
    }
}