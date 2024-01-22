package com.github.tofpu.speedbridge2.common.bridge.setup;

import io.github.tofpu.speedbridge.gameengine.GameStateType;

public class IslandSetupStates {
    public static GameStateType<IslandSetupData> START = (game) -> game.stateType() != IslandSetupStates.STOP;
    public static GameStateType<IslandSetupData> STOP = (game) -> game.stateType() != IslandSetupStates.STOP;
    public static GameStateType<IslandSetupData> SET_ORIGIN = (game) -> game.stateType() != IslandSetupStates.START;
}
