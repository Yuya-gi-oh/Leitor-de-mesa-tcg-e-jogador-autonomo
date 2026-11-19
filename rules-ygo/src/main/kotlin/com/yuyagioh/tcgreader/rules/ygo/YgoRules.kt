package com.yuyagioh.tcgreader.rules.ygo

import com.yuyagioh.tcgreader.core.action.GameAction
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardInstance
import com.yuyagioh.tcgreader.core.model.CardType
import com.yuyagioh.tcgreader.core.model.GameFormat
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.Phase
import com.yuyagioh.tcgreader.core.model.PlayerId
import com.yuyagioh.tcgreader.core.model.PlayerState
import com.yuyagioh.tcgreader.core.model.findCard
import com.yuyagioh.tcgreader.core.model.withPlayer
import com.yuyagioh.tcgreader.core.rules.GameRules
import com.yuyagioh.tcgreader.core.setup.GameSetup

class YgoRules(private val format: GameFormat = GameFormat.STANDARD) : GameRules {

    override val game: GameId = GameId.YUGIOH

    override fun initialState(deckYou: List<CardInfo>, deckOpponent: List<CardInfo>): GameState {
        val handSize = if (format == GameFormat.SPEED_DUEL) SPEED_DUEL_HAND else HAND_SIZE
        val dealtYou = GameSetup.deal(deckYou, handSize)
        val dealtOpp = GameSetup.deal(deckOpponent, handSize)
        return GameState(
            game = game,
            turnNumber = 1,
            phase = Phase.MAIN_ONE,
            activePlayer = PlayerId.YOU,
            players = mapOf(
                PlayerId.YOU to PlayerState(PlayerId.YOU, STARTING_LIFE, deck = dealtYou.deck, hand = dealtYou.hand),
                PlayerId.OPPONENT to PlayerState(PlayerId.OPPONENT, STARTING_LIFE, deck = dealtOpp.deck, hand = dealtOpp.hand),
            ),
        )
    }

    override fun nextActor(state: GameState): PlayerId? {
        if (state.result != null) return null
        return when (state.phase) {
            Phase.MAIN_ONE, Phase.MAIN_TWO, Phase.BATTLE, Phase.END -> state.activePlayer
            else -> null
        }
    }

    override fun legalActions(state: GameState, playerId: PlayerId): List<GameAction> {
        if (state.result != null || playerId != state.activePlayer) return emptyList()
        val me = state.player(playerId)
        return when {
            (state.phase == Phase.MAIN_ONE || state.phase == Phase.MAIN_TWO) -> {
                val options = mutableListOf<GameAction>()
                options += summonOptions(state, me)
                if (state.phase == Phase.MAIN_ONE && battleAllowed(state)) options += GameAction.EnterBattle
                options += GameAction.EndPhase
                options
            }

            state.phase == Phase.BATTLE -> attackOptions(state, me) + GameAction.EndPhase

            state.phase == Phase.END && state.forcedDiscards > 0 ->
                me.hand.map { GameAction.Discard(it.uid) }

            else -> emptyList()
        }
    }

    override fun applyAction(state: GameState, action: GameAction): GameState {
        check(state.result == null) { "O jogo já terminou." }
        val next = when (action) {
            is GameAction.PlayCard -> playCard(state, action)
            is GameAction.Attack -> attack(state, action)
            is GameAction.EnterBattle -> enterBattle(state)
            is GameAction.Discard -> discard(state, action)
            is GameAction.EndPhase -> endPhase(state)
            is GameAction.Block -> throw IllegalArgumentException("Bloqueio não existe em Yu-Gi-Oh!")
            is GameAction.CommitBattle -> throw IllegalArgumentException("CommitBattle não é legal em Yu-Gi-Oh!")
        }
        return normalize(next)
    }

    override fun winner(state: GameState): PlayerId? {
        state.result?.let { return it }
        val dead = state.players.values.filter { it.life <= 0 }
        return if (dead.size == 1) dead.first().id.opponent else null
    }

    override fun evaluate(state: GameState, playerId: PlayerId): Double {
        when (winner(state)) {
            playerId -> return 1e9
            playerId.opponent -> return -1e9
        }
        val me = state.player(playerId)
        val op = state.player(playerId.opponent)
        var score = (me.life - op.life) * 2.0
        for (m in me.field) score += monsterScore(m)
        for (m in op.field) score -= monsterScore(m)
        score += me.hand.size * 0.4
        score += me.supportZone.count { it.faceDown } * 1.0
        score -= op.supportZone.size * 1.0
        return score
    }

    override fun describe(action: GameAction, state: GameState): String = when (action) {
        is GameAction.PlayCard -> {
            val card = state.findCard(action.cardUid)
            val name = card?.card?.name ?: "?"
            when {
                action.faceDown -> "Colocar $name virado para baixo (set)"
                action.defensePosition -> "Invocar $name em posição de defesa"
                else -> "Invocar $name em posição de ataque"
            }
        }
        is GameAction.Attack -> {
            val attacker = state.findCard(action.cardUid)
            val target = action.targetCardUid?.let { state.findCard(it) }
            when {
                target == null -> "Ataque direto com ${attacker?.card?.name ?: "?"}"
                else -> "${attacker?.card?.name ?: "?"} ataca ${target.card.name}"
            }
        }
        is GameAction.EnterBattle -> "Entrar na fase de batalha"
        is GameAction.Discard -> {
            val card = state.findCard(action.cardUid)
            "Descartar ${card?.card?.name ?: "?"} (limite de mão)"
        }
        is GameAction.EndPhase -> "Passar para a fase seguinte"
        else -> "Ação indisponível"
    }

    private fun monsterScore(m: CardInstance): Double = when {
        m.faceDown -> 1.5
        m.attackPosition -> (m.card.attack ?: 0) * 1.5 + (m.card.defense ?: 0) * 0.3
        else -> (m.card.defense ?: 0) * 0.8
    }

    private fun battleAllowed(state: GameState): Boolean = state.turnNumber > 1

    private fun summonOptions(state: GameState, me: PlayerState): List<GameAction> {
        if (state.onePerTurnPlayUsed || me.field.size >= MAX_MONSTERS) return emptyList()
        val options = mutableListOf<GameAction>()
        val faceUp = me.field.filter { !it.faceDown }
        for (card in me.hand) {
            if (card.card.type != CardType.MONSTER) continue
            val level = card.card.level ?: 4
            val tributeCount = when {
                level <= TRIBUTE_FREE_LEVEL -> 0
                level <= TRIBUTE_ONE_MAX -> 1
                else -> 2
            }
            if (tributeCount == 0) {
                options += GameAction.PlayCard(card.uid)
                options += GameAction.PlayCard(card.uid, defensePosition = true)
                options += GameAction.PlayCard(card.uid, faceDown = true)
            } else {
                tributes(faceUp, tributeCount).forEach { combo ->
                    options += GameAction.PlayCard(card.uid, tributeUids = combo)
                    options += GameAction.PlayCard(card.uid, defensePosition = true, tributeUids = combo)
                }
            }
        }
        return options
    }

    private fun tributes(faceUp: List<CardInstance>, count: Int): List<List<Long>> = when (count) {
        0 -> emptyList()
        1 -> faceUp.map { listOf(it.uid) }
        else -> {
            val combos = mutableListOf<List<Long>>()
            for (i in faceUp.indices) for (j in i + 1 until faceUp.size) {
                combos += listOf(faceUp[i].uid, faceUp[j].uid)
            }
            combos
        }
    }

    private fun attackOptions(state: GameState, me: PlayerState): List<GameAction> {
        val defender = state.activePlayer.opponent
        val defenderState = state.player(defender)
        val attackers = me.monsters.filter { !it.faceDown && it.attackPosition && !state.tappedCardUids.contains(it.uid) }
        val options = mutableListOf<GameAction>()
        for (attacker in attackers) {
            if (defenderState.field.isEmpty()) {
                options += GameAction.Attack(attacker.uid, targetPlayer = defender)
            } else {
                defenderState.field.forEach { target ->
                    options += GameAction.Attack(attacker.uid, targetCardUid = target.uid)
                }
            }
        }
        return options
    }

    private fun playCard(state: GameState, action: GameAction.PlayCard): GameState {
        val player = state.activePlayer
        val me = state.player(player)
        val instance = me.hand.find { it.uid == action.cardUid }
            ?: throw IllegalArgumentException("Carta não está na mão.")
        val card = instance.card
        require(state.phase == Phase.MAIN_ONE || state.phase == Phase.MAIN_TWO) { "Invocação fora da fase principal." }
        require(!state.onePerTurnPlayUsed) { "A invocação normal/set já foi usada neste turno." }
        require(me.field.size - action.tributeUids.size + 1 <= MAX_MONSTERS) { "Zonas de monstro insuficientes." }
        if (action.tributeUids.isNotEmpty()) {
            require(card.card.level ?: 0 > TRIBUTE_FREE_LEVEL) { "Esta carta não exige tributos." }
            for (t in action.tributeUids) {
                val tribute = me.field.find { it.uid == t } ?: throw IllegalArgumentException("Tributo inválido.")
                require(!tribute.faceDown) { "Não é possível tributar monstros virados para baixo." }
            }
        } else {
            require(card.card.level ?: 4 <= TRIBUTE_FREE_LEVEL) { "Esta carta exige tributos." }
        }
        val tributeInstances = me.field.filter { action.tributeUids.contains(it.uid) }
        require(tributeInstances.size == action.tributeUids.size) { "Tributos inválidos." }
        val moved = instance.copy(enteredTurn = state.turnNumber, faceDown = action.faceDown, attackPosition = !action.faceDown && !action.defensePosition)
        return state
            .withPlayer(player) {
                it.copy(
                    hand = it.hand - instance,
                    field = it.field - tributeInstances.toSet() + moved,
                    graveyard = it.graveyard + tributeInstances,
                )
            }
            .copy(onePerTurnPlayUsed = true)
    }

    private fun attack(state: GameState, action: GameAction.Attack): GameState {
        require(state.phase == Phase.BATTLE) { "Só se ataca na fase de batalha." }
        val attackerId = state.activePlayer
        val defenderId = attackerId.opponent
        val me = state.player(attackerId)
        val defender = state.player(defenderId)
        val attacker = me.monsters.find {
            it.uid == action.cardUid && !it.faceDown && it.attackPosition && !state.tappedCardUids.contains(it.uid)
        } ?: throw IllegalArgumentException("Este monstro não pode atacar.")
        var result = state.copy(tappedCardUids = state.tappedCardUids + attacker.uid)
        val target = action.targetCardUid?.let { uid -> defender.monsters.find { it.uid == uid } }
        if (target == null) {
            require(defender.field.isEmpty()) { "Ataque direto só é possível com o campo do adversário vazio." }
            return result.withPlayer(defenderId) { it.copy(life = it.life - (attacker.card.attack ?: 0)) }
        }
        val revealed = if (target.faceDown) target.copy(faceDown = false, attackPosition = false) else target
        result = result.withPlayer(defenderId) { d -> d.copy(field = d.field - target + revealed) }
        val atk = attacker.card.attack ?: 0
        return when {
            revealed.attackPosition -> {
                val targetAtk = revealed.card.attack ?: 0
                when {
                    atk > targetAtk -> result
                        .withPlayer(defenderId) { it.copy(life = it.life - (atk - targetAtk)) }
                        .destroyCreature(revealed)
                    atk == targetAtk -> result
                        .destroyCreature(revealed)
                        .destroyCreature(attacker, owner = attackerId)
                    else -> result.withPlayer(attackerId) { it.copy(life = it.life - (targetAtk - atk)) }
                        .destroyCreature(attacker, owner = attackerId)
                }
            }

            else -> {
                val targetDef = revealed.card.defense ?: 0
                if (atk > targetDef) result.destroyCreature(revealed) else result
            }
        }
    }

    private fun GameState.destroyCreature(creature: CardInstance, owner: PlayerId = activePlayer.opponent): GameState =
        withPlayer(owner) {
            if (field.contains(creature)) {
                copy(field = field - creature, graveyard = graveyard + creature)
            } else {
                this
            }
        }

    private fun enterBattle(state: GameState): GameState {
        require(battleAllowed(state)) { "Não é possível atacar no primeiro turno." }
        require(state.phase == Phase.MAIN_ONE) { "Entrar em batalha só é permitido na fase principal 1." }
        return state.copy(phase = Phase.BATTLE)
    }

    private fun discard(state: GameState, action: GameAction.Discard): GameState {
        require(state.forcedDiscards > 0) { "Descarte não é obrigatório." }
        val player = state.activePlayer
        val instance = state.player(player).hand.find { it.uid == action.cardUid }
            ?: throw IllegalArgumentException("Carta não está na mão.")
        return state
            .withPlayer(player) { it.copy(hand = it.hand - instance, graveyard = it.graveyard + instance) }
            .copy(forcedDiscards = state.forcedDiscards - 1)
    }

    private fun endPhase(state: GameState): GameState = when (state.phase) {
        Phase.MAIN_ONE -> state.copy(phase = Phase.MAIN_TWO)
        Phase.MAIN_TWO, Phase.BATTLE -> state.copy(phase = Phase.END)
        else -> throw IllegalArgumentException("Passo de fase ilegal na fase ${state.phase}.")
    }

    private fun normalize(state: GameState): GameState {
        if (state.result != null) return state
        if (state.phase == Phase.END) {
            val me = state.player(state.activePlayer)
            val discards = me.hand.size - HAND_LIMIT
            if (discards > 0) return state.copy(forcedDiscards = discards)
            val nextPlayer = state.activePlayer.opponent
            return startTurn(state.copy(turnNumber = state.turnNumber + 1), nextPlayer)
        }
        return state
    }

    private fun startTurn(state: GameState, player: PlayerId): GameState {
        val p = state.player(player)
        if (p.deck.isEmpty()) return state.copy(result = player.opponent)
        val drawn = p.deck.first()
        return state.copy(
            activePlayer = player,
            phase = Phase.MAIN_ONE,
            battle = com.yuyagioh.tcgreader.core.model.BattleContext(),
            onePerTurnPlayUsed = false,
            forcedDiscards = 0,
            tappedCardUids = emptySet(),
            players = state.players + (player to p.copy(deck = p.deck.drop(1), hand = p.hand + drawn)),
        )
    }

    val cardPool: List<CardInfo>
        get() = when (format) {
            GameFormat.SPEED_DUEL -> YgoSampleDecks.speedDuelDeck()
            GameFormat.STANDARD -> YgoSampleDecks.starterDeck()
        }

    val minimumDeckSize: Int
        get() = if (format == GameFormat.SPEED_DUEL) SPEED_DUEL_MIN_DECK else STANDARD_MIN_DECK

    companion object {
        const val STARTING_LIFE = 8000
        const val HAND_SIZE = 5
        const val HAND_LIMIT = 6
        const val SPEED_DUEL_HAND = 4
        const val STANDARD_MIN_DECK = 40
        const val SPEED_DUEL_MIN_DECK = 20
        const val MAX_MONSTERS = 5
        const val TRIBUTE_FREE_LEVEL = 4
        const val TRIBUTE_ONE_MAX = 6
    }
}
