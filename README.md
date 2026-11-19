# Leitor de mesa TCG e jogador autónomo

App Android em **Kotlin + Jetpack Compose** que usa a câmara para reconhecer cartas
físicas de jogos de cartas colecionáveis (TCGs) na mesa, conhece as regras desses
jogos e joga como adversário autónomo, em quatro níveis de dificuldade.

> **Nota de transparência (MVP):** o reconhecimento de cartas e as regras completas
> de TCGs comerciais são difíceis por natureza. Esta versão implementa
> **motores de regras simplificados, mas autoconsistentes e testados** para
> *Magic: The Gathering* e *Yu-Gi-Oh!*, e uma pipeline de visão
> (CameraX + deteção de regiões + OCR do ML Kit + *fuzzy matching*) cuja
> precisão depende de luz, qualidade de impressão e estado da carta.
> Toda a jogada da IA é explicada em texto e o utilizador pode corrigir
> manualmente o estado da mesa.

## Funcionalidades

- **Escolha de jogo** (Magic: The Gathering, Yu-Gi-Oh!) e de dificuldade (Fácil, Médio, Difícil, Mestre).
- **Formato Speed Duel** para Yu-Gi-Oh!: baralhos de 20 cartas e mão inicial
de 4 (além do modo Padrão com 40+ cartas).
- **Definição do teu baralho**: antes da partida, a app pergunta "Quais são
as tuas cartas?" e deixa ajustar as quantidades de cada carta (ou usas um
baralho de exemplo com um toque).
- **Motor de regras modular**: interface comum `GameRules` em `core`; um módulo por jogo
  (`rules-mtg`, `rules-ygo`), cada novo jogo é adicionável como módulo Gradle.
- **Jogador autónomo com IA explicável**: aleatório com regras legais (Fácil),
  heurística de avaliação (Médio), minimax com poda alfa-beta (Difícil) e
  Monte Carlo Tree Search (Mestre).
- **Reconhecimento de mesa**: CameraX + OCR (ML Kit Text Recognition) + segmentação
  de regiões de cartas + correspondência difusa contra a base de cartas, com
  correção manual de qualquer carta reconhecida.
- **Base de cartas com cache offline**: APIs públicas reais (Scryfall e
  YGOPRODeck) com cache local em Room.
- **Baralhos de exemplo com cartas reais**, para jogar offline desde o primeiro minuto.

## Instalação

1. Abre o Android Studio (Ladybug ou superior) e escolhe **File → Open** no diretório do projeto.
2. Deixa o Gradle sincronizar (a distribuição é a versão 8.9 indicada em
   `gradle/wrapper/gradle-wrapper.properties`; se o Android Studio pedir para
   gerar o wrapper, aceita — ou executa `gradle wrapper` com o Gradle instalado).
3. Corre em qualquer dispositivo/emulador com **API 26+** (minSdk 26, targetSdk 35).
   Por linha de comandos: `./gradlew assembleDebug`.

Permissões: a app pede `CAMERA` quando abres o leitor de mesa; `INTERNET` é usada
apenas para a base de cartas online (o jogo funciona offline com cache e baralhos de exemplo).

## Arquitetura

MVVM + Clean Architecture, módulos Gradle independentes:

```
app        UI Compose, navegação, ViewModels (MVVM), DI (Hilt)
core       domínio partilhado: GameState, GameAction, GameRules, utilitários
rules-mtg  implementação de GameRules para Magic (regras simplificadas)
rules-ygo  implementação de GameRules para Yu-Gi-Oh! (regras simplificadas)
ai         agentes de IA (RandomAgent, HeuristicAgent, SearchAgent, MctsAgent)
data       Retrofit (Scryfall, YGOPRODeck) + Room (cache) + CardRepository
vision     CameraX, conversão YUV→Bitmap, segmentação, OCR, CardScanner
```

Fluxo: `vision` produz cartas reconhecidas → são aplicadas ao `GameState` (com
correção manual possível) → `rules-*` valida e aplica as ações legais → `ai`
escolhe jogadas sempre dentro de `legalActions()` e explica-as.

## Como adicionar um novo jogo

1. Cria um novo módulo Gradle `rules-<jogo>` (copiando a estrutura de `rules-ygo`).
2. Implementa `GameRules` (definida em `core`): `initialState`, `nextActor`,
   `legalActions`, `applyAction`, `winner`, `evaluate` (heurística usada pela IA)
   e `describe` (explicação da jogada). Os estados são imutáveis: `applyAction`
   devolve sempre um novo estado.
3. Registra a implementação em `app/di/AppModule.kt` (map do `rulesRegistry`).
4. Adiciona o valor a `GameId` (em `core`) e, se precisares de dados online,
   um serviço Retrofit em `data` seguindo o padrão de Scryfall/YGOPRODeck.
5. Escreve testes unitários em `rules-<jogo>/src/test` — a IA só consegue jogar
   ações legais, por isso testes de regras protegem também a IA.

## Testes

```bash
./gradlew :core:test :rules-mtg:test :rules-ygo:test :ai:test
```

## Limitações conhecidas e roadmap

- **Regras simplificadas**: no MTG não há stack/prioridades/cartas de artefato;
  no YGO não há cadeias, invocações especiais nem resolução de efeitos de
  feitiços/traps (podem ser postos boca a baixo, sem efeitos).
- **Visão**: OCR do nome + fuzzy matching; não há ainda reconhecimento por
  imagem (embeddings/TFLite) — extensão natural do módulo `vision`.
- **Skill Cards**: o formato Speed Duel usa Cartas de Habilidade; ainda não
  estão implementadas (roadmap).
- **Roadmap sugerido**: stack do MTG e mais tipos de feitiço; invocações
  especiais e cadeias no YGO; Skill Cards do Speed Duel; reconhecimento
  TFLite; construtor de baralhos com base na API; Pokémon TCG como
  terceiro módulo de regras.

## Licença

MIT — ver [LICENSE](LICENSE).
