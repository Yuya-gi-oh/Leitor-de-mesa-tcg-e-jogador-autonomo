package com.yuyagioh.tcgreader.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuyagioh.tcgreader.app.R
import com.yuyagioh.tcgreader.core.model.Difficulty
import com.yuyagioh.tcgreader.core.model.GameFormat
import com.yuyagioh.tcgreader.core.model.GameId

@Composable
fun HomeScreen(
    onStartGame: (GameId, Difficulty, GameFormat) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val game by viewModel.selectedGame.collectAsStateWithLifecycle()
    val difficulty by viewModel.selectedDifficulty.collectAsStateWithLifecycle()
    val format by viewModel.selectedFormat.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = stringResource(R.string.choose_game),
            style = MaterialTheme.typography.titleMedium,
        )
        GameId.entries.forEach { candidate ->
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = candidate == game,
                    onClick = { viewModel.selectGame(candidate) },
                    label = { Text(candidate.displayNamePt) },
                )
            }
        }

        if (game == GameId.YUGIOH) {
            Text(
                text = stringResource(R.string.choose_format),
                style = MaterialTheme.typography.titleMedium,
            )
            GameFormat.entries.forEach { candidate ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = candidate == format,
                        onClick = { viewModel.selectFormat(candidate) },
                        label = { Text(candidate.displayNamePt) },
                    )
                    Text(
                        text = candidate.descriptionPt,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.choose_difficulty),
            style = MaterialTheme.typography.titleMedium,
        )
        Difficulty.entries.forEach { candidate ->
            Column(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = candidate == difficulty,
                    onClick = { viewModel.selectDifficulty(candidate) },
                    label = { Text(candidate.displayNamePt) },
                )
                Text(
                    text = candidate.descriptionPt,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onStartGame(game, difficulty, format) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.start_game))
        }
    }
}
