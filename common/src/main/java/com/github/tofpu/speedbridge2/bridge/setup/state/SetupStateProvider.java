package com.github.tofpu.speedbridge2.bridge.setup.state;

import com.github.tofpu.speedbridge2.game.land.LandController;
import com.github.tofpu.speedbridge2.game.state.StartGameState;
import com.github.tofpu.speedbridge2.island.IslandService;
import com.github.tofpu.speedbridge2.lobby.LobbyService;
import com.github.tofpu.speedbridge2.object.Location;

public class SetupStateProvider {
    private final IslandService islandService;
    private final LobbyService lobbyService;
    private final LandController landController;

    public SetupStateProvider(IslandService islandService, LobbyService lobbyService, LandController landController) {
        this.islandService = islandService;
        this.lobbyService = lobbyService;
        this.landController = landController;
    }

    public BeginSetupState startState() {
        return new BeginSetupState();
    }

    public SetOriginState originState(Location location) {
        return new SetOriginState(this, location);
    }

    public EndSetupState stopState() {
        return new EndSetupState(islandService, lobbyService, landController);
    }
}
