package com.adamkvasny.flipcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun CreateCardScreen(
    navController: NavController,
    deckName: String,
    onCardSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardRepository = remember { CardRepository(context) }
    var decks by remember { mutableStateOf(cardRepository.loadDecks()) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (title.isNotBlank() && description.isNotBlank()) {
                    val newCard = Card(title, description)
                    val deck = decks.find { it.name == deckName }
                    if (deck != null) {
                        val updatedCards = deck.cards.toMutableList().also { it.add(newCard) }
                        val updatedDeck = deck.copy(cards = updatedCards)
                        val updatedDecks = decks.toMutableList().also {
                            val index = it.indexOfFirst { d -> d.name == deckName }
                            if (index != -1) {
                                it[index] = updatedDeck
                            }
                        }
                        cardRepository.saveDecks(updatedDecks)
                        decks = updatedDecks
                        onCardSaved()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateCardScreenPreview() {
    CreateCardScreen(
        navController = NavController(LocalContext.current),
        deckName = "Preview Deck",
        onCardSaved = {}
    )
}
