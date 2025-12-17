package com.adamkvasny.flipcards

data class Card(
    val title: String,
    val description: String,
    val leftSwipes: Int = 0,
    val rightSwipes: Int = 0
)
