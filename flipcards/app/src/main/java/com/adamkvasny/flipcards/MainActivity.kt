package com.adamkvasny.flipcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adamkvasny.flipcards.ui.theme.FlipcardsTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import kotlin.random.Random

enum class DragAnchors {
    Left, Center, Right
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlipcardsTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "decks",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("decks") {
                DeckSelectionScreen(navController = navController)
            }
            composable(
                "swipe/{deckName}",
                arguments = listOf(navArgument("deckName") { type = NavType.StringType })
            ) {
                CardSwipeScreen(
                    navController = navController,
                    deckName = it.arguments?.getString("deckName") ?: ""
                )
            }
            composable(
                "create/{deckName}",
                arguments = listOf(navArgument("deckName") { type = NavType.StringType })
            ) {
                CreateCardScreen(
                    navController = navController,
                    deckName = it.arguments?.getString("deckName") ?: "",
                    onCardSaved = { navController.popBackStack() })
            }
            composable(
                "stats/{deckName}",
                arguments = listOf(navArgument("deckName") { type = NavType.StringType })
            ) {
                StatsScreen(navController = navController, deckName = it.arguments?.getString("deckName") ?: "")
            }
        }
    }
}

private fun selectNextCardIndex(cards: List<Card>, currentCardIndex: Int): Int {
    if (cards.size <= 1) return 0

    val weights = cards.map { card ->
        val totalSwipes = card.leftSwipes + card.rightSwipes
        val rememberScore = if (totalSwipes == 0) {
            50
        } else {
            (((card.leftSwipes + 1).toFloat() / (totalSwipes + 2).toFloat()) * 100).roundToInt()
        }
        101 - rememberScore // Invert the score to get a weight
    }

    val totalWeight = weights.sum()
    if (totalWeight == 0) return (currentCardIndex + 1) % cards.size

    var random = Random.nextInt(totalWeight)

    var newIndex = 0
    for (i in cards.indices) {
        random -= weights[i]
        if (random < 0) {
            newIndex = i
            break
        }
    }

    // Ensure we don't show the same card twice in a row if there are other options
    if (newIndex == currentCardIndex) {
        return (currentCardIndex + 1) % cards.size
    }

    return newIndex
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardSwipeScreen(navController: NavController, deckName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cardRepository = remember { CardRepository(context) }
    var decks by remember { mutableStateOf(cardRepository.loadDecks()) }
    val deck = decks.find { it.name == deckName } ?: return
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var nextCardIndex by remember {
        mutableIntStateOf(
            if (deck.cards.size > 1) 1 else 0
        )
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenwidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val endAnchor = screenwidth * 1.5f

    val positionalThreshold = { distance: Float -> distance * 0.5f }
    val velocityThreshold = { with(density) { 100.dp.toPx() } }
    val decaySpec = remember { splineBasedDecay<Float>(density) }

    val state = remember(currentCardIndex) {
        AnchoredDraggableState(
            initialValue = DragAnchors.Center,
            anchors = DraggableAnchors {
                DragAnchors.Left at -endAnchor
                DragAnchors.Center at 0f
                DragAnchors.Right at endAnchor
            },
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            snapAnimationSpec = tween(300),
            decayAnimationSpec = decaySpec
        )
    }

    LaunchedEffect(state.settledValue) {
        if (state.settledValue != DragAnchors.Center) {
            val swipedCard = deck.cards.getOrNull(currentCardIndex)
            if (swipedCard != null) {
                val updatedCard = when (state.settledValue) {
                    DragAnchors.Right -> swipedCard.copy(rightSwipes = swipedCard.rightSwipes + 1)
                    DragAnchors.Left -> swipedCard.copy(leftSwipes = swipedCard.leftSwipes + 1)
                    else -> swipedCard
                }
                val updatedCards = deck.cards.toMutableList().also {
                    if (currentCardIndex < it.size) {
                        it[currentCardIndex] = updatedCard
                    }
                }
                val updatedDeck = deck.copy(cards = updatedCards)
                val updatedDecks = decks.toMutableList().also {
                    val index = it.indexOfFirst { d -> d.name == deckName }
                    if (index != -1) {
                        it[index] = updatedDeck
                    }
                }
                cardRepository.saveDecks(updatedDecks)
                decks = updatedDecks // Make sure to update the local state to get the latest weights
            }

            snapshotFlow { state.isAnimationRunning }
                .filter { !it }
                .first()

            if (deck.cards.isNotEmpty()) {
                currentCardIndex = nextCardIndex
                nextCardIndex = selectNextCardIndex(deck.cards, currentCardIndex)
                state.snapTo(DragAnchors.Center)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (deck.cards.isEmpty()) {
            Text("No cards in this deck.")
        } else {
            Box(contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.7f)
                        .graphicsLayer {
                            val dragProgress =
                                (kotlin.math.abs(state.requireOffset()) / endAnchor).coerceIn(0f, 1f)
                            val scale = 0.9f + (dragProgress * 0.1f)
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = deck.cards.getOrNull(nextCardIndex)?.title ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                key(currentCardIndex) {
                    var isFlipped by remember { mutableStateOf(false) }
                    val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f, label = "")
                    var showDeleteConfirmation by remember { mutableStateOf(false) }

                    if (showDeleteConfirmation) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmation = false },
                            title = { Text("Delete Card") },
                            text = { Text("Are you sure you want to delete this card?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val cardToDelete = deck.cards.getOrNull(currentCardIndex)
                                        if (cardToDelete != null) {
                                            val updatedCards =
                                                deck.cards.toMutableList().also { it.remove(cardToDelete) }
                                            val updatedDeck = deck.copy(cards = updatedCards)
                                            val updatedDecks = decks.toMutableList().also {
                                                val index = it.indexOfFirst { d -> d.name == deckName }
                                                if (index != -1) {
                                                    it[index] = updatedDeck
                                                }
                                            }
                                            cardRepository.saveDecks(updatedDecks)
                                            decks = updatedDecks
                                            if (currentCardIndex >= updatedCards.size) {
                                                currentCardIndex = (updatedCards.size - 1).coerceAtLeast(0)
                                            }
                                        }
                                        showDeleteConfirmation = false
                                    }
                                ) {
                                    Text("Confirm")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showDeleteConfirmation = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.7f)
                        .offset {
                            IntOffset(
                                x = state
                                    .requireOffset()
                                    .roundToInt(),
                                y = 0,
                            )
                        }
                        .anchoredDraggable(state, Orientation.Horizontal)
                        .graphicsLayer {
                            val dragProgress = (state.requireOffset() / endAnchor).coerceIn(-1f, 1f)
                            rotationZ = dragProgress * 15f
                            val scale = 1f - (kotlin.math.abs(dragProgress) * 0.1f)
                            scaleX = scale
                            scaleY = scale
                        }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationY = rotation
                                    cameraDistance = 8 * density.density
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .combinedClickable(
                                        onClick = { isFlipped = !isFlipped },
                                        onLongClick = { showDeleteConfirmation = true }
                                    )
                            ) {
                                if (rotation < 90f) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = deck.cards.getOrNull(currentCardIndex)?.title
                                                ?: "",
                                            style = MaterialTheme.typography.headlineMedium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { rotationY = 180f }
                                            .padding(vertical = 40.dp, horizontal = 16.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = deck.cards.getOrNull(currentCardIndex)?.description
                                                ?: "",
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                 }

                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        if (rotation >= 90f) {
                                            rotationY = 180f
                                        }
                                    }
                                ) {
                                    val dragProgress =
                                        (state.requireOffset() / endAnchor).coerceIn(-1f, 1f)

                                    Text(
                                        "Not Remembered",
                                        Modifier
                                            .align(Alignment.TopStart)
                                            .padding(16.dp)
                                            .graphicsLayer { alpha = (dragProgress * 2.5f).coerceIn(0f, 1f) },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Light
                                    )

                                    Text(
                                        "Remembered",
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp)
                                            .graphicsLayer { alpha = (-dragProgress * 2.5f).coerceIn(0f, 1f) },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomCenter)) {
            Button(
                onClick = { navController.navigate("create/${deck.name}") },
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text("New Card")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardSwipeScreenPreview() {
    FlipcardsTheme {
        CardSwipeScreen(navController = rememberNavController(), deckName = "Preview Deck")
    }
}
