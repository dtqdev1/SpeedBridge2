package io.tofpu.speedbridge2.command.subcommand;


import cloud.commandframework.annotations.*;
import com.sk89q.minecraft.util.commands.CommandAlias;
import io.tofpu.speedbridge2.SpeedBridge;
import io.tofpu.speedbridge2.command.parser.IslandArgument;
import io.tofpu.speedbridge2.domain.blockmenu.BlockMenuManager;
import io.tofpu.speedbridge2.domain.common.Message;
import io.tofpu.speedbridge2.domain.common.config.manager.ConfigurationManager;
import io.tofpu.speedbridge2.domain.common.util.BridgeUtil;
import io.tofpu.speedbridge2.domain.common.util.MessageUtil;
import io.tofpu.speedbridge2.domain.island.IslandHandler;
import io.tofpu.speedbridge2.domain.island.IslandService;
import io.tofpu.speedbridge2.domain.island.object.Island;
import io.tofpu.speedbridge2.domain.island.setup.IslandSetup;
import io.tofpu.speedbridge2.domain.island.setup.IslandSetupManager;
import io.tofpu.speedbridge2.domain.player.misc.score.Score;
import io.tofpu.speedbridge2.domain.player.object.BridgePlayer;
import io.tofpu.speedbridge2.domain.player.object.extra.CommonBridgePlayer;
import io.tofpu.speedbridge2.plugin.SpeedBridgePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.tofpu.speedbridge2.domain.common.Message.INSTANCE;
import static io.tofpu.speedbridge2.domain.common.util.MessageUtil.Symbols.ARROW_RIGHT;
import static io.tofpu.speedbridge2.domain.common.util.MessageUtil.Symbols.CROSS;

public final class SpeedBridgeCommand {
    private final IslandService islandService = IslandService.INSTANCE;

    @CommandMethod("speedbridge|sb setlobby")
    @CommandDescription("Sets the lobby location")
    @CommandPermission("speedbridge.lobby.set")
    public void onLobbySet(final BridgePlayer bridgePlayer) {
        ConfigurationManager.INSTANCE.getLobbyCategory().setLobbyLocation(bridgePlayer.getPlayer().getLocation()).whenComplete((unused, throwable) -> {
            BridgeUtil.sendMessage(bridgePlayer, INSTANCE.LOBBY_SET_LOCATION);
        });
    }

    @CommandMethod("speedbridge|sb create <slot> <schematic>")
    @CommandDescription("Create an island with a defined slot")
    @CommandPermission("speedbridge.island.create")
    public void onIslandCreate(final CommonBridgePlayer<?> player, final @Argument("slot")
            int slot, final @Argument("schematic") String schematic,
            @Flag("c") String category) {
        final CommandSender sender = player.getPlayer();

        if (category == null || category.isEmpty()) {
            category = ConfigurationManager.INSTANCE.getGeneralCategory()
                    .getDefaultIslandCategory();
        }

        final IslandHandler.IslandCreationResult result = islandService.createIsland(slot, category, schematic);
        if (result == IslandHandler.IslandCreationResult.ISLAND_ALREADY_EXISTS) {
            BridgeUtil.sendMessage(sender, String.format(INSTANCE.ISLAND_ALREADY_EXISTS,
                    slot + ""));
            return;
        }

        final String message;
        if (result == IslandHandler.IslandCreationResult.UNKNOWN_SCHEMATIC) {
            message = String.format(INSTANCE.UNKNOWN_SCHEMATIC, schematic);
        } else {
            message = String.format(INSTANCE.ISLAND_HAS_BEEN_CREATED_SCHEMATIC,
                    slot + "", schematic) + "\n" + String.format(INSTANCE.ISLAND_SETUP_NOTIFICATION, slot, slot);
        }
        BridgeUtil.sendMessage(sender, message);
    }

    @CommandMethod("speedbridge|sb delete <slot>")
    @CommandDescription("delete an island")
    @CommandPermission("speedbridge.island.delete")
    public void onIslandDelete(final CommonBridgePlayer<?> player, final @Argument(
            "slot") int slot) {
        final Island island = islandService.deleteIsland(slot);

        final String message;
        if (island == null) {
            message = String.format(INSTANCE.INVALID_ISLAND, slot + "");
        } else {
            message = String.format(INSTANCE.DELETED_AN_ISLAND, slot);
        }
        BridgeUtil.sendMessage(player, message);
    }

    @CommandMethod("speedbridge|sb select <slot>")
    @CommandDescription("select an island to modify their properties")
    @CommandPermission("island.island.select")
    public void onIslandSelect(final CommonBridgePlayer<?> bridgePlayer, final @Argument("slot")
            int slot, final @Flag(value = "c", description = "category") String category,
            final @Flag(value = "s", description = "schematic")
            String schematic) {
        final CommandSender sender = bridgePlayer.getPlayer();
        final Island island = islandService.findIslandBy(slot);

        String message = INSTANCE.EMPTY_SELECT;
        boolean successful = false;
        if (island == null) {
            message = String.format(INSTANCE.INVALID_ISLAND, slot + "");
        } else {
            String selectType = "";
            if (category != null && !category.isEmpty()) {
                selectType = "category";

                island.setCategory(category);
            } else if (schematic != null && !schematic.isEmpty()) {
                selectType = "schematic";

                successful = island.selectSchematic(schematic);
            }

            switch (selectType) {
                case "category":
                    message = String.format(INSTANCE.VALID_SELECT,
                            slot + "", category, selectType);
                    break;
                case "schematic":
                    if (successful) {
                        message = String.format(INSTANCE.VALID_SELECT,
                                slot + "", schematic, selectType);
                        break;
                    }
                    message = String.format(INSTANCE.UNKNOWN_SCHEMATIC, schematic);
                    break;
            }
        }

        if (!message.isEmpty()) {
            BridgeUtil.sendMessage(sender, message);
        }
    }

    @ProxiedBy("join")
    @CommandMethod("speedbridge|sb join [island]")
    @CommandDescription("Join an island")
    public void onIslandJoin(final BridgePlayer bridgePlayer, final @Argument("island")
            String category) {
        if (!isGeneralSetupComplete(bridgePlayer)) {
            return;
        }

        // /join 2
        // /join default

        // /randomjoin

        int slot;
        try {
            slot = Integer.parseInt(category);
        } catch (NumberFormatException exception) {
            slot = -1;
        }

        Island island = null;
        if (slot != -1) {
            island = islandService.findIslandBy(slot);
        } else if (category != null && !category.isEmpty()) {
            island = islandService.findIslandBy(category);
        }

        if (island != null) {
            slot = island.getSlot();
        }

        final String message;
        if (island == null || !island.isReady()) {
            if (slot == -1) {
                message = INSTANCE.INVALID_ISLAND_ARGUMENT;
            } else {
                message = String.format(INSTANCE.INVALID_ISLAND, slot + "");
            }
        } else if (bridgePlayer.isPlaying()) {
            message = INSTANCE.ALREADY_IN_A_ISLAND;
        } else {
            message = String.format(INSTANCE.JOINED_AN_ISLAND, slot + "");
            island.generateGame(bridgePlayer);
        }

        if (!message.isEmpty()) {
            BridgeUtil.sendMessage(bridgePlayer, message);
        }
    }

    private boolean isGeneralSetupComplete(final BridgePlayer bridgePlayer) {
        final boolean isLobbyProcessComplete =
                ConfigurationManager.INSTANCE.getLobbyCategory()
                        .getLobbyLocation() != null;

        // if the lobby process is complete, return true
        if (isLobbyProcessComplete) {
            return true;
        }

        final Player player = bridgePlayer.getPlayer();
        // if the player is an operator, or has the "speedbridge.lobby.set" permission
        // node
        if (player.isOp() || player.hasPermission("speedbridge.lobby.set")) {
            BridgeUtil.sendMessage(bridgePlayer, INSTANCE.LOBBY_MISSING);
        } else {
            BridgeUtil.sendMessage(bridgePlayer, INSTANCE.GENERAL_SETUP_INCOMPLETE);
            // forwarding the message to console
            BridgeUtil.sendMessage(Bukkit.getConsoleSender(), INSTANCE.LOBBY_MISSING);
        }

        return false;
    }

    @ProxiedBy("leave")
    @CommandMethod("speedbridge|sb leave")
    @CommandDescription("Leave an island")
    @IslandArgument
    public void onIslandLeave(final BridgePlayer bridgePlayer, final @NotNull Island island) {
        island.leaveGame(bridgePlayer);
    }

    @ProxiedBy("score")
    @CommandMethod("speedbridge|sb score")
    @CommandAlias("speedbridge|sb score")
    @CommandDescription("Shows a list of your scores")
    public void onScore(final BridgePlayer bridgePlayer) {
        final Player player = bridgePlayer.getPlayer();
        final List<String> scoreList = new ArrayList<>();
        final String message;

        for (final Score score : bridgePlayer.getScores()) {
            if (scoreList.isEmpty()) {
                scoreList.add(INSTANCE.SCORE_TITLE);
            }
            // Your scores:
            // Island X scored X seconds;
            final String formattedScore = " <gold><bold>" + CROSS.getSymbol() + " " + "<reset><yellow>Island " + "<gold>%s</gold>" + " " + ARROW_RIGHT
                    .getSymbol() + " <gold>%s</gold> seconds";
            scoreList.add(String.format(formattedScore, score.getScoredOn(), BridgeUtil.formatNumber(score
                    .getScore())));
        }

        if (scoreList.isEmpty()) {
            message = "<red>You haven't scored anything yet";
        } else {
            scoreList.add(MessageUtil.MENU_BAR);

            message = String.join("\n", scoreList);
        }

        BridgeUtil.sendMessage(player, message);
    }

    @ProxiedBy("choose")
    @CommandMethod("speedbridge|sb choose")
    @CommandAlias("speedbridge|sb choose")
    @CommandDescription("Lets you choose a block")
    public void chooseBlock(final BridgePlayer bridgePlayer) {
        BlockMenuManager.INSTANCE.showInventory(bridgePlayer);
    }

    @CommandMethod("speedbridge|sb reload")
    @CommandDescription("Reloads the config")
    @CommandPermission("speedbridge.reload")
    public void pluginReload(final CommonBridgePlayer<?> player) {
        final CompletableFuture<?>[] completableFutures = new CompletableFuture[2];
        completableFutures[0] = Message.load(SpeedBridgePlugin.getPlugin(SpeedBridgePlugin.class).getDataFolder());
        completableFutures[1] = ConfigurationManager.INSTANCE.reload();

        CompletableFuture.allOf(completableFutures).whenComplete((unused, throwable) -> {
            // reloading the blocks
            BlockMenuManager.INSTANCE.reload();

            if (player.getPlayer() != null) {
                BridgeUtil.sendMessage(player, INSTANCE.RELOADED);
            }
        });
    }

    @CommandMethod("speedbridge|sb")
    @CommandDescription("Shows a list of commands")
    @CommandPermission("speedbridge.help")
    @Hidden
    public void onNoArgument(final CommonBridgePlayer<?> bridgePlayer) {
        final CommandSender player = bridgePlayer.getPlayer();
        BridgeUtil.sendMessage(player, INSTANCE.NO_ARGUMENT);
    }

    @CommandMethod("speedbridge|sb help")
    @CommandPermission("speedbridge.help")
    @CommandDescription("Shows a list of commands")
    public void onHelpCommand(final CommonBridgePlayer<?> bridgePlayer) {
        final CommandSender player = bridgePlayer.getPlayer();
        HelpCommandGenerator.showHelpMessage(player);
    }

    @ProxiedBy("randomjoin")
    @CommandMethod("speedbridge|sb randomjoin")
    @CommandDescription("Chooses a random island for you")
    public void onRandomJoin(final BridgePlayer bridgePlayer) {
        if (!isGeneralSetupComplete(bridgePlayer)) {
            return;
        }

        final Optional<Island> optionalIsland = islandService.getAllIslands()
                .stream()
                .parallel()
                .filter(Island::isReady)
                .findAny();

        final String message;

        if (bridgePlayer.isPlaying()) {
            message = INSTANCE.ALREADY_IN_A_ISLAND;
        } else if (!optionalIsland.isPresent()) {
            message = INSTANCE.NO_AVAILABLE_ISLAND;
        } else {
            final Island island = optionalIsland.get();
            island.generateGame(bridgePlayer);

            message = String.format(INSTANCE.JOINED_AN_ISLAND, island.getSlot() + "");
        }

        BridgeUtil.sendMessage(bridgePlayer, message);
    }

    @CommandMethod("speedbridge|sb setup <slot>")
    @CommandDescription("Creates an island setup")
    @CommandPermission("speedbridge.setup.admin")
    public void onStartSetup(final BridgePlayer bridgePlayer,
            final @Argument("slot") int slot) {
        if (!isGeneralSetupComplete(bridgePlayer)) {
            return;
        }

        final Island island = IslandService.INSTANCE.findIslandBy(slot);

        final String message;
        if (island == null) {
            message = INSTANCE.INVALID_ISLAND;
        } else {
            message = String.format(INSTANCE.STARTING_SETUP_PROCESS, slot);
            IslandSetupManager.INSTANCE.startSetup(bridgePlayer, island);
        }
        BridgeUtil.sendMessage(bridgePlayer, message);
    }

    @CommandMethod("speedbridge|sb setup setspawn")
    @CommandDescription("Sets the island's spawnpoint")
    @CommandPermission("speedbridge.setup.admin")
    public void setupSetSpawn(final BridgePlayer bridgePlayer) {
        final IslandSetup islandSetup =
                IslandSetupManager.INSTANCE.findSetupBy(bridgePlayer.getPlayerUid());

        final String message;
        if (islandSetup == null) {
            message = INSTANCE.NOT_IN_A_SETUP;
        } else {
            final Location playerLocation = bridgePlayer.getPlayer()
                    .getLocation();

            // if the location given was not valid
            if (!islandSetup.isLocationValid(playerLocation)) {
                message = INSTANCE.INVALID_SPAWN_POINT;
            } else {
                // otherwise, set the location point
                message = INSTANCE.SET_SPAWN_POINT + "\n" + INSTANCE.COMPLETE_NOTIFICATION;
                islandSetup.setPlayerSpawnPoint(playerLocation);
            }
        }
        BridgeUtil.sendMessage(bridgePlayer, message);
    }

    @CommandMethod("speedbridge|sb setup finish")
    @CommandDescription("Completes the island's setup")
    @CommandPermission("speedbridge.setup.admin")
    public void setupFinish(final BridgePlayer bridgePlayer) {
        final IslandSetup islandSetup = IslandSetupManager.INSTANCE.findSetupBy(bridgePlayer.getPlayerUid());

        final String message;
        if (islandSetup == null) {
            message = INSTANCE.NOT_IN_A_SETUP;
        } else if (!islandSetup.isReady()) {
            message = INSTANCE.SETUP_INCOMPLETE;
        } else {
            message = INSTANCE.SETUP_COMPLETE;
            islandSetup.finish();
        }

        BridgeUtil.sendMessage(bridgePlayer, message);
    }

    @CommandMethod("speedbridge|sb setup cancel")
    @CommandDescription("Cancels the island's setup")
    @CommandPermission("speedbridge.setup.admin")
    public void cancelSetup(final BridgePlayer bridgePlayer) {
        final IslandSetup islandSetup = IslandSetupManager.INSTANCE.findSetupBy(bridgePlayer.getPlayerUid());

        final String message;
        if (islandSetup == null) {
            message = INSTANCE.NOT_IN_A_SETUP;
        } else {
            message = INSTANCE.SETUP_CANCELLED;

            islandSetup.cancel();
        }

        BridgeUtil.sendMessage(bridgePlayer, message);
    }
}
