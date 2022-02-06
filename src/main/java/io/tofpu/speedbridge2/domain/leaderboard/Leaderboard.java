package io.tofpu.speedbridge2.domain.leaderboard;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.tofpu.speedbridge2.domain.common.PluginExecutor;
import io.tofpu.speedbridge2.domain.common.config.manager.ConfigurationManager;
import io.tofpu.speedbridge2.domain.common.database.wrapper.DatabaseQuery;
import io.tofpu.speedbridge2.domain.common.util.BridgeUtil;
import io.tofpu.speedbridge2.domain.leaderboard.loader.IslandLoader;
import io.tofpu.speedbridge2.domain.leaderboard.loader.PersonalBoardLoader;
import io.tofpu.speedbridge2.domain.leaderboard.wrapper.BoardPlayer;
import io.tofpu.speedbridge2.domain.leaderboard.wrapper.IslandBoardPlayer;

import java.util.*;
import java.util.concurrent.*;

public final class Leaderboard {
    public static final Leaderboard INSTANCE = new Leaderboard();

    private final Map<Integer, BoardPlayer> globalMap;
    private final LoadingCache<UUID, BoardPlayer> playerCache;
    private final LoadingCache<UUID, IslandBoardPlayer> islandPositionMap;

    private final ScheduledExecutorService executorService;

    private Leaderboard() {
        this.globalMap = new ConcurrentHashMap<>();

        // player's personal global position
        this.playerCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .build(PersonalBoardLoader.INSTANCE);

        // player's global position that based on an island
        this.islandPositionMap = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .build(IslandLoader.INSTANCE);

        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void load() {
        executorService.scheduleWithFixedDelay(() -> {
            BridgeUtil.debug("refreshing!");
            for (final UUID uuid : playerCache.asMap()
                    .keySet()) {
                this.playerCache.refresh(uuid);
            }

            try (final DatabaseQuery databaseQuery = new DatabaseQuery(
                    "SELECT DISTINCT * FROM scores ORDER BY score")) {
                final List<UUID> uuidList = new ArrayList<>();
                final Map<Integer, BoardPlayer> globalBoardMap = new HashMap<>();

                databaseQuery.executeQuery(resultSet -> {
                    while (resultSet.next()) {
                        // if we reached the 10 limit, break the loop
                        if (globalBoardMap.size() == 10) {
                            break;
                        }

                        final UUID uuid = UUID.fromString(resultSet.getString("uid"));
                        // if we already have the given uuid, continue through the loop!
                        if (uuidList.contains(uuid)) {
                            continue;
                        }

                        final BoardPlayer value =
                                BridgeUtil.resultToBoardPlayer(true, resultSet);

                        uuidList.add(uuid);
                        globalBoardMap.put(value.getPosition(), value);
                    }
                });

                this.globalMap.clear();
                this.globalMap.putAll(globalBoardMap);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }, 1, ConfigurationManager.INSTANCE.getLeaderboardCategory()
                .getUpdateInterval(), TimeUnit.SECONDS);
    }

    public CompletableFuture<BoardPlayer> retrieve(final UUID uniqueId) {
        final BoardPlayer player = playerCache.asMap()
                .get(uniqueId);

        // if the board player is found, return the completed value
        if (player != null) {
            return CompletableFuture.completedFuture(player);
        }

        // otherwise, attempt to load the player board async
        return CompletableFuture.supplyAsync(() -> playerCache.getUnchecked(uniqueId));
    }

    public BoardPlayer retrieve(final int position) {
        return globalMap.get(position);
    }

    public CompletableFuture<IslandBoardPlayer.IslandBoard> retrieve(final UUID uniqueId, final int islandSlot) {
        final IslandBoardPlayer player = islandPositionMap.asMap()
                .get(uniqueId);
        final IslandBoardPlayer.IslandBoard islandBoard =
                player == null ? null : player.findDefault(islandSlot);

        // if an island board is found, return the completed value
        if (islandBoard != null) {
            return CompletableFuture.completedFuture(islandBoard);
        }

        // otherwise, attempt to retrieve the board async
        return PluginExecutor.supply(() -> islandPositionMap.getUnchecked(uniqueId)
                .retrieve(islandSlot));
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}



