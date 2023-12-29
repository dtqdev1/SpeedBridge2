package com.github.tofpu.speedbridge2.bridge.game;

import com.github.tofpu.speedbridge2.ArenaAdapter;
import com.github.tofpu.speedbridge2.bridge.IslandSchematic;
import com.github.tofpu.speedbridge2.bridge.Land;
import com.github.tofpu.speedbridge2.bridge.LandController;
import com.github.tofpu.speedbridge2.bridge.core.GameHandler;
import com.github.tofpu.speedbridge2.bridge.core.state.StartGameState;
import com.github.tofpu.speedbridge2.bridge.core.state.StopGameState;
import com.github.tofpu.speedbridge2.bridge.game.state.BasicStateProvider;
import com.github.tofpu.speedbridge2.bridge.game.state.GameStateHandler;
import com.github.tofpu.speedbridge2.bridge.core.Game;
import com.github.tofpu.speedbridge2.bridge.core.GameState;
import com.github.tofpu.speedbridge2.island.Island;
import com.github.tofpu.speedbridge2.object.player.OnlinePlayer;
import com.github.tofpu.speedbridge2.schematic.Schematic;
import com.github.tofpu.speedbridge2.schematic.SchematicHandler;

import java.util.UUID;

import static com.github.tofpu.speedbridge2.util.ProgramCorrectness.requireState;

@SuppressWarnings({"rawtypes", "unchecked"})
public class IslandGameHandler extends GameHandler<OnlinePlayer, IslandGameData> {
    private final ArenaAdapter arenaAdapter;
    private final LandController landController;
    private final SchematicHandler schematicHandler;
    private final GameStateHandler gameStateHandler;
    private final BasicStateProvider basicStateProvider;

    public static IslandGameHandler create(BasicStateProvider basicStateProvider, GameStateHandler gameStateHandler, ArenaAdapter arenaAdapter, SchematicHandler schematicHandler) {
        return new IslandGameHandler(basicStateProvider, gameStateHandler, arenaAdapter, schematicHandler);
    }

    private IslandGameHandler(BasicStateProvider basicStateProvider, GameStateHandler gameStateHandler, ArenaAdapter arenaAdapter, SchematicHandler schematicHandler) {
        this.basicStateProvider = basicStateProvider;
        this.gameStateHandler = gameStateHandler;
        this.arenaAdapter = arenaAdapter;
        this.landController = new LandController(new IslandArenaManager(arenaAdapter));
        this.schematicHandler = schematicHandler;
    }

    public void createAndStart(final OnlinePlayer player, final Island island) {
        assertPlayerIsNotInGame(player);

        IslandGamePlayer gamePlayer = new IslandGamePlayer(player);

        Schematic schematic = schematicHandler.resolve(island.getSchematicName());
        IslandSchematic islandSchematic = new IslandSchematic(island.getSlot(), schematic, island.getAbsolute());

        Land land = this.landController.reserveSpot(player.id(), islandSchematic, arenaAdapter.gameWorld());
        IslandGame game = new IslandGame(new IslandGameData(gamePlayer, island, land));
        prepareAndRegister(player, game);
    }

    public void resetGame(UUID playerId) {
        requirePlayerToBeInGame(playerId, "%s must already be in a game to reset it");

        Game<IslandGameData> game = gameRegistry.getByPlayer(playerId);
        if (game != null) {
            gameStateHandler.triggerResetState(game);
        }
    }

    public void scoredGame(UUID playerId) {
        requirePlayerToBeInGame(playerId, "%s must already be in a game to score");

        Game<IslandGameData> game = gameRegistry.getByPlayer(playerId);
        if (game != null) {
            gameStateHandler.triggerScoreState(game);
        }
    }

    private void requirePlayerToBeInGame(UUID playerId, String template) {
        requireState(gameRegistry.isInGame(playerId), template, playerId);
    }

    public LandController landController() {
        return landController;
    }

    @Override
    protected GameState<IslandGameData> createPrepareState() {
        return basicStateProvider.prepareState();
    }

    @Override
    protected StartGameState createStartState() {
        return basicStateProvider.startedState();
    }

    @Override
    protected StopGameState createStopState() {
        return basicStateProvider.stopState();
    }

    public GameStateHandler gameStateHandler() {
        return gameStateHandler;
    }
}
