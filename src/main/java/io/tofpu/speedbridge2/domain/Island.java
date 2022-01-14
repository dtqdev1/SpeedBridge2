package io.tofpu.speedbridge2.domain;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import io.tofpu.speedbridge2.database.Databases;
import io.tofpu.speedbridge2.domain.game.GameIsland;
import io.tofpu.speedbridge2.domain.game.GamePlayer;
import io.tofpu.speedbridge2.domain.schematic.IslandSchematic;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public final class Island {
    private final int slot;
    private final IslandSchematic islandSchematic = new IslandSchematic();

    private final Map<GamePlayer, GameIsland> islandMap = new HashMap<>();
    private String category;

    public Island(final int slot, final String category) {
        this.slot = slot;
        this.category = category;
    }

    public Map.Entry<GamePlayer, GameIsland> generateGame(final Player player) {
        if (islandSchematic.getSchematicClipboard() == null) {
            return null;
        }
        final GamePlayer gamePlayer = GamePlayer.of(player);
        final GameIsland gameIsland = new GameIsland(this, gamePlayer);

        this.islandMap.put(gamePlayer, gameIsland);
        return new AbstractMap.SimpleImmutableEntry<>(gamePlayer, gameIsland);
    }

    public GameIsland findGameByPlayer(final GamePlayer gamePlayer) {
        return this.islandMap.get(gamePlayer);
    }

    public void setCategory(final String anotherCategory) {
        this.category = anotherCategory;
        update();
    }

    public boolean selectSchematic(final String schematicName) {
        final boolean successful = this.islandSchematic.selectSchematic(schematicName);
        // if the operation was successful, update the database
        if (successful) {
            update();
        }
        return successful;
    }

    public String getSchematicName() {
        return this.islandSchematic.getSchematicName();
    }

    public Clipboard getSchematicClipboard() {
        return this.islandSchematic.getSchematicClipboard();
    }

    private void update() {
        Databases.ISLAND_DATABASE.update(this);
    }

    public int getSlot() {
        return slot;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Island{");
        sb.append("slot=").append(slot);
        sb.append(", islandSchematic=").append(islandSchematic);
        sb.append(", islandMap=").append(islandMap);
        sb.append(", category='").append(category).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
