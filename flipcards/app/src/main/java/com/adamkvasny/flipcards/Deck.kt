package com.adamkvasny.flipcards

data class Deck(
    val name: String,
    val cards: List<Card> = emptyList()
)
