package com.adamkvasny.flipcards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckSelectionScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cardRepository = remember { CardRepository(context) }
    var decks by remember { mutableStateOf(cardRepository.loadDecks()) }
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }

    if (showCreateDeckDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDeckDialog = false },
            title = { Text("Create Deck") },
            text = {
                OutlinedTextField(
                    value = newDeckName,
                    onValueChange = { newDeckName = it },
                    label = { Text("Deck Name") })
            },
            confirmButton = {
                Button(onClick = {
                    if (newDeckName.isNotBlank()) {
                        val newDeck = if (newDeckName.equals("lorem", ignoreCase = true)) {
                            val loremCards = listOf(
                                Card("Lorem ipsum dolor sit amet", "Consectetur adipiscing elit."),
                                Card("Sed do eiusmod tempor incididunt", "Ut labore et dolore magna aliqua."),
                                Card("Ut enim ad minim veniam", "Quis nostrud exercitation ullamco laboris."),
                                Card("Nisi ut aliquip ex ea commodo consequat", "Duis aute irure dolor in reprehenderit."),
                                Card("In voluptate velit esse cillum dolore", "Eu fugiat nulla pariatur.")
                            )
                            Deck(newDeckName, loremCards)
                        } else {
                            Deck(newDeckName, emptyList())
                        }
                        val updatedDecks = decks.toMutableList().also { it.add(newDeck) }
                        cardRepository.saveDecks(updatedDecks)
                        decks = updatedDecks
                        showCreateDeckDialog = false
                        newDeckName = ""
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showCreateDeckDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(decks) { deck ->
                Card(
                    modifier = Modifier
                        .aspectRatio(0.7f)
                        .combinedClickable(
                            onClick = { navController.navigate("swipe/${deck.name}") },
                            onLongClick = { navController.navigate("stats/${deck.name}") }
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = deck.name,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${deck.cards.size} cards")
                    }
                }
            }
        }

        Button(
            onClick = { showCreateDeckDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Create Deck")
        }
    }
}
