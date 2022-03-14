package io.tofpu.speedbridge2.domain.player;

import io.tofpu.speedbridge2.domain.player.object.BridgePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerService {
    public static final @NotNull PlayerService INSTANCE = new PlayerService();

    private final @NotNull PlayerHandler playerHandler;

    public PlayerService() {
        this.playerHandler = new PlayerHandler();
    }

    /**
     * It returns a CompletableFuture that will eventually return the player with the given
     * uniqueId
     *
     * @param uid The unique ID of the player.
     * @return A CompletableFuture<BridgePlayer>
     */
    public CompletableFuture<BridgePlayer> load(final UUID uid) {
        return playerHandler.load(uid);
    }

    /**
     * Returns the player with the given unique ID, or null if no such player exists
     *
     * @param uuid The unique ID of the player.
     * @return A BridgePlayer object.
     */
    public @Nullable BridgePlayer get(final @NotNull UUID uuid) {
        return this.playerHandler.get(uuid);
    }

    /**
     * Returns the player with the given unique ID, or get a dummy object
     *
     * @param uuid The unique ID of the player.
     * @return A BridgePlayer object.
     */
    public @NotNull BridgePlayer getOrDefault(final @NotNull UUID uuid) {
        return this.playerHandler.getOrDefault(uuid);
    }

    /**
     * Remove a player from the player map
     *
     * @param uniqueId The unique ID of the player to remove.
     * @return The BridgePlayer object that was removed from the map.
     */
    public @Nullable BridgePlayer remove(final @NotNull UUID uniqueId) {
        return playerHandler.remove(uniqueId);
    }

    /**
     * If the player is in the
     * database, update the name and refresh the player
     *
     * @param name The name of the player.
     * @param uniqueId The unique ID of the player.
     * @return A BridgePlayer object.
     */
    public @Nullable BridgePlayer internalRefresh(final @NotNull Player player) {
        return playerHandler.internalRefresh(player.getName(), player.getUniqueId());
    }

    /**
     * This function invalidates a player by removing them from the bridge and the island
     * setup manager
     *
     * @param uniqueId The unique ID of the player to invalidate.
     * @return The bridge player that was invalidated.
     */
    public @Nullable BridgePlayer invalidate(final @NotNull Player player) {
        return playerHandler.invalidate(player.getUniqueId());
    }

    /**
     * Returns a collection of all the players in the game
     *
     * @return A collection of all the players in the game.
     */
    public Collection<BridgePlayer> getBridgePlayers() {
        return playerHandler.getBridgePlayers();
    }

    /**
     * Resets the player's data
     *
     * @param uuid The UUID of the player to reset.
     */
    public void reset(final UUID uuid) {
        playerHandler.reset(uuid);
    }

    public void shutdown() {
        playerHandler.shutdown();
    }
}
