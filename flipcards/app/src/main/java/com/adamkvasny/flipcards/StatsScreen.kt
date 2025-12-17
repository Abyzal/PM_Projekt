package com.adamkvasny.flipcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.math.roundToInt

@Composable
fun StatsScreen(navController: NavController, deckName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cardRepository = remember { CardRepository(context) }
    var decks by remember { mutableStateOf(cardRepository.loadDecks()) }
    val deck = decks.find { it.name == deckName }
    var showDeleteDeckConfirmation by remember { mutableStateOf(false) }
    var showResetStatsConfirmation by remember { mutableStateOf(false) }

    if (deck != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(deck.cards) { card ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                val totalSwipes = card.leftSwipes + card.rightSwipes
                                val rememberScore = if (totalSwipes == 0) {
                                    50
                                } else {
                                    (((card.leftSwipes + 1).toFloat() / (totalSwipes + 2).toFloat()) * 100).roundToInt()
                                }
                                Text(text = card.title)
                                Text(text = "Remember Score: $rememberScore%")
                            }
                            TextButton(onClick = {
                                val updatedCards = deck.cards.filter { it != card }
                                val updatedDeck = deck.copy(cards = updatedCards)
                                val updatedDecks =
                                    decks.map { if (it.name == deck.name) updatedDeck else it }
                                cardRepository.saveDecks(updatedDecks)
                                decks = updatedDecks
                            }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
            Row {
                Button(onClick = { showDeleteDeckConfirmation = true }) {
                    Text("Delete Deck")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { showResetStatsConfirmation = true }) {
                    Text("Reset Stats")
                }
            }
        }
    }

    if (showDeleteDeckConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteDeckConfirmation = false },
            title = { Text("Delete Deck") },
            text = { Text("Are you sure you want to delete the deck '${deck?.name}'?") },
            confirmButton = {
                Button(onClick = {
                    val updatedDecks = decks.filter { it.name != deck?.name }
                    cardRepository.saveDecks(updatedDecks)
                    navController.popBackStack()
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDeckConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetStatsConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetStatsConfirmation = false },
            title = { Text("Reset Stats") },
            text = { Text("Are you sure you want to reset the stats for all cards in '${deck?.name}'?") },
            confirmButton = {
                Button(onClick = {
                    if (deck != null) {
                        val resetCards = deck.cards.map { it.copy(leftSwipes = 0, rightSwipes = 0) }
                        val updatedDeck = deck.copy(cards = resetCards)
                        val updatedDecks = decks.map { if (it.name == deck.name) updatedDeck else it }
                        cardRepository.saveDecks(updatedDecks)
                        decks = updatedDecks
                    }
                    showResetStatsConfirmation = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = { showResetStatsConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
