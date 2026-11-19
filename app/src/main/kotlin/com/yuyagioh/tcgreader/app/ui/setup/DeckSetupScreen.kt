package com.yuyagioh.tcgreader.app.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuyagioh.tcgreader.app.R
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardType

@Composable
fun DeckSetupScreen(
    onStartGame: () -> Unit,
    onBack: () -> Unit,
    viewModel: DeckSetupViewModel = hiltViewModel(),
) {
    val quantities by viewModel.quantities.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.deck_setup_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.deck_setup_hint, viewModel.minimumDeckSize),
            style = MaterialTheme.typography.bodySmall,
        )

        viewModel.cardCatalog.forEach { card ->
            CardRow(
                card = card,
                quantity = quantities[card.id] ?: 0,
                onIncrement = { viewModel.increment(card) },
                onDecrement = { viewModel.decrement(card) },
            )
        }

        Text(
            text = stringResource(R.string.deck_count, viewModel.totalCards, viewModel.minimumDeckSize),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        OutlinedButton(
            onClick = { viewModel.useSampleDeck() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.use_sample_deck))
        }

        Button(
            onClick = {
                viewModel.startGame()
                onStartGame()
            },
            enabled = viewModel.canStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.start_duel))
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.back))
        }
    }
}

@Composable
private fun CardRow(
    card: CardInfo,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = card.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = cardStats(card),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onDecrement, enabled = quantity > 0) {
                    Text("-")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(onClick = onIncrement) {
                    Text("+")
                }
            }
        }
    }
}

private fun cardStats(card: CardInfo): String {
    val kind = if (card.type == CardType.UNKNOWN) "" else card.type
    val power = when {
        card.manaCost != null -> "Custo: ${card.manaCost}"
        card.level != null -> "Nível: ${card.level}"
        else -> ""
    }
    val combat = when {
        card.attack != null && card.defense != null -> "ATK ${card.attack} / DEF ${card.defense}"
        else -> ""
    }
    return listOf(kind, power, combat).filter { it.isNotBlank() }.joinToString(" · ")
}
