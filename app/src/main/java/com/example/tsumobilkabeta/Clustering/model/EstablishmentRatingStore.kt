package com.example.tsumobilkabeta.Clustering.model

import android.content.Context

object EstablishmentRatingStore {
    private const val PREFS_NAME = "establishment_ratings"

    fun ratingFor(context: Context, establishmentId: Int): Int {
        return preferences(context).getInt(key(establishmentId), 0)
    }

    fun saveRating(context: Context, establishmentId: Int, rating: Int) {
        preferences(context)
            .edit()
            .putInt(key(establishmentId), rating.coerceIn(0, 9))
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(establishmentId: Int) = "rating_$establishmentId"
}

