package com.github.tofpu.speedbridge2.bukkit.command;

import com.github.tofpu.speedbridge2.bukkit.BukkitMessages;
import com.github.tofpu.speedbridge2.bukkit.plugin.BukkitPlugin;
import com.github.tofpu.speedbridge2.common.game.BridgeSystem;
import com.github.tofpu.speedbridge2.common.game.score.BridgeScoreService;
import com.github.tofpu.speedbridge2.common.game.score.Score;
import com.github.tofpu.speedbridge2.common.island.Island;
import com.github.tofpu.speedbridge2.common.island.IslandService;
import com.github.tofpu.speedbridge2.common.lobby.LobbyService;
import com.github.tofpu.speedbridge2.common.setup.GameSetupSystem;
import com.github.tofpu.speedbridge2.configuration.message.ConfigurableMessageService;
import com.github.tofpu.speedbridge2.object.player.OnlinePlayer;
import revxrsal.commands.annotation.AutoComplete;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.exception.CommandErrorException;

import java.util.Map;


@Command({"speedbridge", "sb"})
public class PluginCommandHolder {
    private final LobbyService lobbyService;
    private final GameSetupSystem setupSystem;
    private final IslandService islandService;
    private final BridgeSystem bridgeSystem;
    private final BridgeScoreService scoreService;
    private final ConfigurableMessageService configurableMessageService;

    public PluginCommandHolder(LobbyService lobbyService, GameSetupSystem setupSystem, IslandService islandService, BridgeSystem bridgeSystem, BridgeScoreService scoreService, ConfigurableMessageService configurableMessageService) {
        this.lobbyService = lobbyService;
        this.setupSystem = setupSystem;
        this.islandService = islandService;
        this.bridgeSystem = bridgeSystem;
        this.scoreService = scoreService;
        this.configurableMessageService = configurableMessageService;
    }

    public PluginCommandHolder(BukkitPlugin bukkitPlugin) {
        this(bukkitPlugin.getService(LobbyService.class), bukkitPlugin.setupSystem(), bukkitPlugin.getService(IslandService.class), bukkitPlugin.bridgeSystem(), bukkitPlugin.getService(BridgeScoreService.class), bukkitPlugin.getService(ConfigurableMessageService.class));
    }

    @Subcommand("lobby")
    public void lobby(final OnlinePlayer player) {
        if (!lobbyService.isLobbyAvailable()) {
            player.sendMessage(BukkitMessages.LOBBY_NOT_AVAILABLE);
            return;
        }
        player.teleport(lobbyService.position());
    }

    @Subcommand("setlobby")
    public void setLobby(final OnlinePlayer player) {
        lobbyService.updateLocation(player.getPosition());
        player.sendMessage(BukkitMessages.LOBBY_SET);
    }

    @Subcommand("game setup create")
    @AutoComplete("* @schematics")
    public void gameSetup(final OnlinePlayer player, final int slot, final String schematicName) {
        requireLobbyToBeAvailable();
        if (setupSystem.isInSetup(player.id())) {
            player.sendMessage(BukkitMessages.GAME_SETUP_PLAYER_BUSY);
            return;
        }
        setupSystem.startSetup(player, slot, schematicName);
        player.sendMessage(BukkitMessages.GAME_SETUP_CREATED, slot);
    }

    @Subcommand("game setup cancel")
    public void gameSetupCancel(final OnlinePlayer player) {
        requireLobbyToBeAvailable();
        int setupSlot = setupSystem.getSetupSlot(player.id());
        if (setupSlot == -1) {
            player.sendMessage(BukkitMessages.GAME_SETUP_PLAYER_MISSING);
            return;
        }

        setupSystem.cancelSetup(player);
        player.sendMessage(BukkitMessages.GAME_SETUP_CANCELLED, setupSlot);
    }

    @Subcommand("game join")
    public void gameJoin(final OnlinePlayer player, final int slot) {
        requireLobbyToBeAvailable();
        Island island = islandService.get(slot);
        if (island == null) {
            player.sendMessage(BukkitMessages.LOBBY_UNKNOWN, slot);
            return;
        }
        bridgeSystem.joinGame(player, island);
    }

    @Subcommand("game end")
    public void gameEnd(final OnlinePlayer player) {
        requireLobbyToBeAvailable();
        if (!bridgeSystem.isInGame(player.id())) {
            player.sendMessage(BukkitMessages.NOT_IN_GAME);
            return;
        }
        bridgeSystem.leaveGame(player.id());
        player.sendMessage(BukkitMessages.LEFT_GAME);
    }

    @Subcommand("score")
    public void score(final OnlinePlayer player, @Optional Integer islandSlot) {
        String message;
        if (islandSlot == null) {
            message = bestScoresOnAllIslands(player);
        } else {
            message = specificBestScore(player, islandSlot);
        }

        if (message == null || message.isEmpty()) {
            player.sendMessage(BukkitMessages.NO_PERSONAL_BEST);
            return;
        }

        player.sendMessage(message);
    }

    private String specificBestScore(OnlinePlayer player, Integer islandSlot) {
        Score bestScore = scoreService.getBestScore(player.id(), islandSlot);
        if (bestScore == null) {
            return BukkitMessages.NO_PERSONAL_BEST_ON_ISLAND.defaultMessage(islandSlot);
        }
        return BukkitMessages.PERSONAL_BEST.defaultMessage(bestScore.textSeconds(), bestScore.getIslandSlot());
    }

    private String bestScoresOnAllIslands(OnlinePlayer player) {
        Map<Integer, Score> bestScores = scoreService.getBestScoresFromAllIslands(player.id());
        if (bestScores.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(BukkitMessages.PERSONAL_BEST_GLOBAL_TITLE.defaultMessage())
                .append("\n");

        bestScores.forEach((slot, score) -> {
            builder.append(BukkitMessages.PERSONAL_BEST_GLOBAL_BODY.defaultMessage(score.textSeconds(), slot))
                    .append("\n");
        });

        return builder.toString();
    }

    @Subcommand("reload")
    public void reload(final OnlinePlayer player) {
        try {
            configurableMessageService.reload();
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(BukkitMessages.RELOAD_ERROR);
            return;
        }
        player.sendMessage(BukkitMessages.SUCCESSFUL_RELOAD);
    }

    public void requireLobbyToBeAvailable() {
        if (!lobbyService.isLobbyAvailable()) {
            throw new CommandErrorException(BukkitMessages.LOBBY_NOT_AVAILABLE.defaultMessage());
        }
    }
}
