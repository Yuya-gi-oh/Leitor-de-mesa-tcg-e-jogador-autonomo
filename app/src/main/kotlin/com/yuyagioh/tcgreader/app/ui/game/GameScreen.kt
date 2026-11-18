package com.yuyagioh.tcgreader.app.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuyagioh.tcgreader.app.R
import com.yuyagioh.tcgreader.app.ui.camera.CameraViewModel
import com.yuyagioh.tcgreader.core.model.CardInstance
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.PlayerId
import com.yuyagioh.tcgreader.core.model.PlayerState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen(
    gameId: String,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    gameViewModel: GameViewModel = hiltViewModel(),
    cameraViewModel: CameraViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val state by gameViewModel.state.collectAsStateWithLifecycle()
    val log by gameViewModel.log.collectAsStateWithLifecycle()
    val thinking by gameViewModel.thinking.collectAsStateWithLifecycle()
    val selectedCard by cameraViewModel.selectedCard.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        gameViewModel.startGameIfIdle()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Button(onClick = onOpenCamera) { Text(stringResource(R.string.scan_table)) }
        }

        val gameState = state
        if (gameState == null) {
            Text(text = stringResource(R.string.scanning))
            return@Column
        }

        HeaderRow(gameState, thinking)

        gameState.result?.let { winner ->
            val message = if (winner == PlayerId.YOU) {
                stringResource(R.string.you_win)
            } else {
                stringResource(R.string.you_lose)
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(onClick = { gameViewModel.startGame() }) { Text(stringResource(R.string.new_game)) }
                }
            }
        }

        PlayerPanel(
            title = stringResource(R.string.opponent_panel),
            player = gameState.player(PlayerId.OPPONENT),
            state = gameState,
        )
        PlayerPanel(
            title = stringResource(R.string.you_panel),
            player = gameState.player(PlayerId.YOU),
            state = gameState,
        )

        selectedCard?.let { card ->
            ManualAdjustCard(
                cardName = card.name,
                onZone = { zone ->
                    gameViewModel.addCardToZone(card, zone)
                    cameraViewModel.consumeSelection()
                },
            )
        }

        val isMyTurn = gameState.result == null && gameViewModel.rules.nextActor(gameState) == PlayerId.YOU
        Text(
            text = if (isMyTurn) stringResource(R.string.actions_title) else stringResource(R.string.ai_turn),
            style = MaterialTheme.typography.titleSmall,
        )
        if (isMyTurn) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val actions = gameViewModel.rules
                    .legalActions(gameState, PlayerId.YOU)
                    .take(SCREEN_ACTION_LIMIT)
                actions.forEach { action ->
                    Button(onClick = { gameViewModel.submit(action) }) {
                        Text(
                            gameViewModel.rules.describe(action, gameState),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = stringResource(R.string.log_title), style = MaterialTheme.typography.titleSmall)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(log) { entry ->
                Text("• $entry", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ManualAdjustCard(cardName: String, onZone: (ManualZone) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.manual_adjust_hint) + " $cardName",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { onZone(ManualZone.FIELD) }) { Text(stringResource(R.string.add_to_field)) }
                Button(onClick = { onZone(ManualZone.HAND) }) { Text(stringResource(R.string.add_to_hand)) }
                Button(onClick = { onZone(ManualZone.GRAVEYARD) }) { Text(stringResource(R.string.add_to_graveyard)) }
            }
        }
    }
}

@Composable
private fun HeaderRow(state: GameState, thinking: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Turno ${state.turnNumber}", fontWeight = FontWeight.Bold)
        Text("Fase: ${state.phase.displayNamePt}")
        Text(
            text = stringResource(
                if (thinking) com.yuyagioh.tcgreader.app.R.string.ai_turn else com.yuyagioh.tcgreader.app.R.string.your_turn,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerPanel(title: String, player: PlayerState, state: GameState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.life, player.life))
                Text(stringResource(R.string.deck, player.deck.size))
                Text(stringResource(R.string.hand, player.hand.size))
                Text(stringResource(R.string.graveyard, player.graveyard.size))
                if (player.mana > 0) {
                    Text(stringResource(R.string.mana, player.mana))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                player.field.forEach { instance ->
                    CardChip(instance, state.tappedCardUids.contains(instance.uid))
                }
            }
            if (player.supportZone.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    player.supportZone.forEach { instance -> CardChip(instance, tapped = false) }
                }
            }
        }
    }
}

@Composable
private fun CardChip(instance: CardInstance, tapped: Boolean) {
    Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.small) {
        Column(modifier = Modifier.padding(6.dp)) {
            val suffix = buildString {
                instance.card.attack?.let { append(" $it/") }
                instance.card.defense?.let { append("$it") }
                if (instance.faceDown) append(" (boca abaixo)")
                if (tapped) append(" •")
            }
            Text(
                text = instance.card.name + " " + suffix.trim(),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private const val SCREEN_ACTION_LIMIT = 40
