package com.yuyagioh.tcgreader.data.repository

import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.data.db.CardDao
import com.yuyagioh.tcgreader.data.mapper.toCardInfo
import com.yuyagioh.tcgreader.data.mapper.toEntity
import com.yuyagioh.tcgreader.data.remote.ScryfallService
import com.yuyagioh.tcgreader.data.remote.YgoProDeckService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CardRepository(
    private val scryfall: ScryfallService,
    private val ygoProDeck: YgoProDeckService,
    private val dao: CardDao,
) {

    suspend fun searchByName(game: GameId, query: String, limit: Int = 20): List<CardInfo> =
        withContext(Dispatchers.IO) {
            val cached = dao.search(game.name, "%$query%", limit).map { it.toCardInfo() }
            if (cached.isNotEmpty()) return@withContext cached
            val fresh = runCatching { fetchRemote(game, query, limit) }.getOrDefault(emptyList())
            if (fresh.isNotEmpty()) cache(fresh)
            fresh
        }

    suspend fun cardByName(game: GameId, name: String): CardInfo? =
        withContext(Dispatchers.IO) {
            dao.byName(game.name, name)?.toCardInfo() ?: run {
                val fresh = runCatching { fetchByName(game, name) }.getOrNull()
                fresh?.let { cache(listOf(it)) }
                fresh
            }
        }

    suspend fun cache(deck: List<CardInfo>) {
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            dao.insertAll(deck.map { it.toEntity(now) })
        }
    }

    private suspend fun fetchRemote(game: GameId, query: String, limit: Int): List<CardInfo> = when (game) {
        GameId.MAGIC -> {
            val response = scryfall.search("name:\"$query\"")
            response.data.take(limit).mapNotNull { it.toCardInfo() }
        }

        GameId.YUGIOH -> {
            val response = ygoProDeck.searchByNamePrefix(query)
            response.data.take(limit).mapNotNull { it.toCardInfo() }
        }
    }

    private suspend fun fetchByName(game: GameId, name: String): CardInfo? = when (game) {
        GameId.MAGIC -> runCatching { scryfall.fuzzyNamed(name).toCardInfo() }.getOrNull()
        GameId.YUGIOH -> runCatching { ygoProDeck.byExactName(name).data.firstOrNull()?.toCardInfo() }.getOrNull()
    }
}
