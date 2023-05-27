package com.github.tofpu.speedbridge2;

import com.github.tofpu.speedbridge2.object.World;

public interface WorldAdapter {

    World provideWorld(final String worldName);

    boolean isLoadedWorld(final String worldName);
}