package com.github.tofpu.speedbridge2.common.game.event;

import com.github.tofpu.speedbridge2.common.game.IslandGame;
import com.github.tofpu.speedbridge2.common.game.IslandGamePlayer;

public class IslandGameStopEvent extends IslandGameEvent {
    public IslandGameStopEvent(IslandGame game, IslandGamePlayer player) {
        super(game, player);
    }
}
