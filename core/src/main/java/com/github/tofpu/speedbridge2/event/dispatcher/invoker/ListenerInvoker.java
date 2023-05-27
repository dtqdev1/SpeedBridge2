package com.github.tofpu.speedbridge2.event.dispatcher.invoker;

import com.github.tofpu.speedbridge2.event.Event;

public interface ListenerInvoker {

    void invoke(final Event event);

    Class<?> type();

    String name();
}
