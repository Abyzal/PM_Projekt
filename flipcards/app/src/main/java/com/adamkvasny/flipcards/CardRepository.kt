package com.adamkvasny.flipcards

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CardRepository(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("decks", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDecks(decks: List<Deck>) {
        val json = gson.toJson(decks)
        sharedPreferences.edit().putString("decks_list", json).apply()
    }

    fun loadDecks(): List<Deck> {
        val json = sharedPreferences.getString("decks_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Deck>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}
