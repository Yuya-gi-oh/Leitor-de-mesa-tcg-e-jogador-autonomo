package com.yuyagioh.tcgreader.rules.mtg

import com.yuyagioh.tcgreader.core.action.GameAction
import com.yuyagioh.tcgreader.core.model.BattleContext
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardInstance
import com.yuyagioh.tcgreader.core.model.CardType
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.Phase
import com.yuyagioh.tcgreader.core.model.PlayerId
import com.yuyagioh.tcgreader.core.model.PlayerState
import com.yuyagioh.tcgreader.core.model.findCard
import com.yuyagioh.tcgreader.core.model.withPlayer
import com.yuyagioh.tcgreader.core.rules.GameRules
import com.yuyagioh.tcgreader.core.setup.GameSetup

class MtgRules : GameRules {

    override val game: GameId = GameId.MAGIC

    override fun initialState(deckYou: List<CardInfo>, deckOpponent: List<CardInfo>): GameState {
        val dealtYou = GameSetup.deal(deckYou, HAND_SIZE)
        val dealtOpp = GameSetup.deal(deckOpponent, HAND_SIZE)
        val state = GameState(
            game = game,
            turnNumber = 1,
            phase = Phase.MAIN_ONE,
            activePlayer = PlayerId.YOU,
            players = mapOf(
                PlayerId.YOU to PlayerState(PlayerId.YOU, STARTING_LIFE, deck = dealtYou.deck, hand = dealtYou.hand),
                PlayerId.OPPONENT to PlayerState(PlayerId.OPPONENT, STARTING_LIFE, deck = dealtOpp.deck, hand = dealtOpp.hand),
            ),
        )
        return startTurn(state, PlayerId.YOU)
    }

    override fun nextActor(state: GameState): PlayerId? {
        if (state.result != null) return null
        return when (state.phase) {
            Phase.BATTLE ->
                if (!state.battle.attackersCommitted) state.activePlayer else state.activePlayer.opponent
            Phase.MAIN_ONE, Phase.MAIN_TWO, Phase.END -> state.activePlayer
            else -> null
        }
    }

    override fun legalActions(state: GameState, playerId: PlayerId): List<GameAction> {
        if (state.result != null) return emptyList()
        val me = state.player(playerId)
        return when {
            state.phase == Phase.BATTLE && !state.battle.attackersCommitted && playerId == state.activePlayer ->
                readyCreatures(state, me)
                    .map { GameAction.Attack(it.uid, targetPlayer = playerId.opponent) } +
                    GameAction.CommitBattle

            state.phase == Phase.BATTLE && state.battle.attackersCommitted && playerId != state.activePlayer -> {
                val blockers = readyCreatures(state, me).map { it.uid }
                val blocks = state.battle.attackers.keys
                    .flatMap { attacker -> blockers.filter { b -> !state.battle.blocks.containsValue(b) }
                        .map { blocker -> GameAction.Block(blocker, attacker) } }
                blocks + GameAction.CommitBattle
            }

            (state.phase == Phase.MAIN_ONE || state.phase == Phase.MAIN_TWO) && playerId == state.activePlayer ->
                cardPlays(state, me) + GameAction.EndPhase

            state.phase == Phase.END && state.forcedDiscards > 0 && playerId == state.activePlayer ->
                me.hand.map { GameAction.Discard(it.uid) }

            else -> emptyList()
        }
    }

    override fun applyAction(state: GameState, action: GameAction): GameState {
        check(state.result == null) { "O jogo já terminou." }
        val next = when (action) {
            is GameAction.PlayCard -> playCard(state, action)
            is GameAction.Attack -> declareAttack(state, action)
            is GameAction.Block -> declareBlock(state, action)
            is GameAction.CommitBattle -> commitBattle(state)
            is GameAction.Discard -> discard(state, action)
            is GameAction.EndPhase -> endPhase(state)
            is GameAction.EnterBattle -> throw IllegalArgumentException("EnterBattle não é legal em Magic.")
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
        score += me.hand.size * 0.3
        score += me.field.count { it.card.type == CardType.LAND } * 0.5
        score += me.mana * 0.2
        me.creatures.forEach { c ->
            score += (c.card.attack ?: 0) * 1.5 + (c.card.defense ?: 0) * 0.5
            if (c.enteredTurn < state.turnNumber) score += 0.3
        }
        op.creatures.forEach { c ->
            score -= (c.card.attack ?: 0) * 1.5 + (c.card.defense ?: 0) * 0.5
        }
        return score
    }

    override fun describe(action: GameAction, state: GameState): String = when (action) {
        is GameAction.PlayCard -> {
            val card = state.findCard(action.cardUid)
            when {
                card == null -> "Jogar uma carta"
                card.card.type == CardType.LAND -> "Jogar a terra ${card.card.name}"
                else -> "Lançar ${card.card.name} (custo ${card.card.manaCost ?: 0})"
            }
        }
        is GameAction.Attack -> {
            val card = state.findCard(action.cardUid)
            "Atacar com ${card?.card?.name ?: "?"}"
        }
        is GameAction.Block -> {
            val blocker = state.findCard(action.blockerUid)
            val attacker = state.findCard(action.attackerUid)
            "Bloquear ${attacker?.card?.name ?: "?"} com ${blocker?.card?.name ?: "?"}"
        }
        is GameAction.CommitBattle ->
            if (!state.battle.attackersCommitted) "Concluir a declaração de atacantes" else "Concluir a declaração de bloqueios"
        is GameAction.Discard -> {
            val card = state.findCard(action.cardUid)
            "Descartar ${card?.card?.name ?: "?"}"
        }
        is GameAction.EndPhase -> "Passar para a fase seguinte"
        is GameAction.EnterBattle -> "Entrar na fase de combate"
    }

    private fun readyCreatures(state: GameState, me: PlayerState): List<CardInstance> =
        me.field.filter {
            it.card.type == CardType.CREATURE &&
                !state.tappedCardUids.contains(it.uid) &&
                it.enteredTurn < state.turnNumber
        }

    private fun cardPlays(state: GameState, me: PlayerState): List<GameAction> {
        val opponent = state.activePlayer.opponent
        val options = mutableListOf<GameAction>()
        for (card in me.hand) {
            val cost = card.card.manaCost ?: 0
            when (card.card.type) {
                CardType.LAND ->
                    if (!state.onePerTurnPlayUsed) options += GameAction.PlayCard(card.uid)
                CardType.CREATURE ->
                    if (me.mana >= cost) options += GameAction.PlayCard(card.uid)
                CardType.SORCERY ->
                    if (me.mana >= cost) {
                        options += GameAction.PlayCard(card.uid, targetPlayer = opponent)
                        state.player(opponent).field.forEach { target ->
                            options += GameAction.PlayCard(card.uid, targetCardUid = target.uid)
                        }
                    }
                else -> Unit
            }
        }
        return options
    }

    private fun playCard(state: GameState, action: GameAction.PlayCard): GameState {
        val player = state.activePlayer
        val me = state.player(player)
        val instance = requireHand(me, action.cardUid)
        val card = instance.card
        return when {
            card.type == CardType.LAND -> {
                require(!state.onePerTurnPlayUsed) { "Só é possível jogar uma terra por turno." }
                state
                    .withPlayer(player) {
                        it.copy(
                            hand = it.hand - instance,
                            field = it.field + instance.copy(enteredTurn = state.turnNumber),
                            mana = it.mana + 1,
                        )
                    }
                    .copy(onePerTurnPlayUsed = true)
            }

            card.type == CardType.CREATURE -> {
                val cost = card.manaCost ?: 0
                require(me.mana >= cost) { "Mana insuficiente." }
                state.withPlayer(player) {
                    it.copy(
                        hand = it.hand - instance,
                        field = it.field + instance.copy(enteredTurn = state.turnNumber),
                        mana = it.mana - cost,
                    )
                }
            }

            else -> castSpell(state, action, instance)
        }
    }

    private fun castSpell(state: GameState, action: GameAction.PlayCard, instance: CardInstance): GameState {
        val player = state.activePlayer
        val opponent = player.opponent
        val damage = instance.card.attack ?: 0
        val afterPay = state.withPlayer(player) {
            it.copy(
                mana = it.mana - (instance.card.manaCost ?: 0),
                hand = it.hand - instance,
                graveyard = it.graveyard + instance,
            )
        }
        val target = action.targetCardUid
        return when {
            target != null -> afterPay.withPlayer(opponent) { defending ->
                val creature = defending.field.find { it.uid == target }
                    ?: throw IllegalArgumentException("Alvo inválido para o feitiço.")
                if ((creature.card.defense ?: 0) <= damage) {
                    defending.copy(field = defending.field - creature, graveyard = defending.graveyard + creature)
                } else {
                    defending
                }
            }

            action.targetPlayer == opponent -> afterPay.withPlayer(opponent) { it.copy(life = it.life - damage) }

            else -> throw IllegalArgumentException("Feitiço exige um alvo.")
        }
    }

    private fun declareAttack(state: GameState, action: GameAction.Attack): GameState {
        require(state.phase == Phase.BATTLE) { "Só se ataca na fase de combate." }
        require(!state.battle.attackersCommitted) { "Os atacantes já foram declarados." }
        val attacker = state.player(state.activePlayer).field.find { it.uid == action.cardUid }
            ?: throw IllegalArgumentException("Atacante inexistente.")
        require(
            attacker.card.type == CardType.CREATURE &&
                !state.tappedCardUids.contains(attacker.uid) &&
                attacker.enteredTurn < state.turnNumber,
        ) { "A criatura não pode atacar." }
        val target = action.targetPlayer ?: state.activePlayer.opponent
        return state.copy(
            battle = state.battle.copy(attackers = state.battle.attackers + (attacker.uid to target)),
            tappedCardUids = state.tappedCardUids + attacker.uid,
        )
    }

    private fun declareBlock(state: GameState, action: GameAction.Block): GameState {
        require(state.phase == Phase.BATTLE && state.battle.attackersCommitted) { "Sem atacantes a bloquear." }
        require(!state.battle.blocks.containsValue(action.blockerUid)) { "Bloqueador já em uso." }
        require(state.battle.attackers.containsKey(action.attackerUid)) { "Atacante inexistente." }
        return state.copy(battle = state.battle.copy(blocks = state.battle.blocks + (action.blockerUid to action.attackerUid)))
    }

    private fun commitBattle(state: GameState): GameState {
        require(state.phase == Phase.BATTLE) { "Não está em combate." }
        if (!state.battle.attackersCommitted) {
            return state.copy(battle = state.battle.copy(attackersCommitted = true))
        }
        return resolveBattle(state)
    }

    private fun resolveBattle(state: GameState): GameState {
        val me = state.player(state.activePlayer)
        val opponentId = state.activePlayer.opponent
        val op = state.player(opponentId)
        val destroyed = mutableSetOf<Long>()
        val lifeDamage = mutableMapOf(opponentId to 0, state.activePlayer to 0)
        for ((attackerUid, targetId) in state.battle.attackers) {
            val attacker = me.field.find { it.uid == attackerUid } ?: continue
            val blockerUid = state.battle.blocks[attackerUid]
            if (blockerUid != null) {
                val blocker = op.field.find { it.uid == blockerUid } ?: continue
                if ((blocker.card.defense ?: 0) <= (attacker.card.attack ?: 0)) destroyed += blockerUid
                if ((attacker.card.defense ?: 0) <= (blocker.card.attack ?: 0)) destroyed += attackerUid
            } else {
                lifeDamage[targetId] = (lifeDamage[targetId] ?: 0) + (attacker.card.attack ?: 0)
            }
        }
        var players = state.players
        for (id in listOf(state.activePlayer, opponentId)) {
            val p = players.getValue(id)
            val dead = p.field.filter { destroyed.contains(it.uid) }
            if (dead.isNotEmpty() || (lifeDamage[id] ?: 0) > 0) {
                players = players + (id to p.copy(
                    life = p.life - (lifeDamage[id] ?: 0),
                    field = p.field - dead.toSet(),
                    graveyard = p.graveyard + dead,
                ))
            }
        }
        return state.copy(
            players = players,
            battle = BattleContext(),
            phase = Phase.MAIN_TWO,
        )
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
        Phase.MAIN_ONE -> state.copy(phase = Phase.BATTLE)
        Phase.MAIN_TWO -> state.copy(phase = Phase.END)
        else -> throw IllegalArgumentException("Passo de fase ilegal na fase ${state.phase}.")
    }

    private fun normalize(state: GameState): GameState {
        if (state.result != null) return state
        if (state.phase == Phase.END) {
            val me = state.player(state.activePlayer)
            val discards = me.hand.size - MAX_HAND_SIZE
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
        val updated = p.copy(
            mana = p.field.count { it.card.type == CardType.LAND },
            deck = p.deck.drop(1),
            hand = p.hand + drawn,
        )
        return state.copy(
            activePlayer = player,
            phase = Phase.MAIN_ONE,
            battle = BattleContext(),
            onePerTurnPlayUsed = false,
            forcedDiscards = 0,
            tappedCardUids = emptySet(),
            players = state.players + (player to updated),
        )
    }

    private fun requireHand(player: PlayerState, uid: Long): CardInstance =
        player.hand.find { it.uid == uid } ?: throw IllegalArgumentException("Carta não está na mão.")

    companion object {
        const val STARTING_LIFE = 20
        const val HAND_SIZE = 7
        const val MAX_HAND_SIZE = 7
    }
}
