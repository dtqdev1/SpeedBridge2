package com.github.tofpu.speedbridge2.configuration;

import com.github.tofpu.speedbridge2.configuration.service.PluginConfigTypes;
import com.github.tofpu.speedbridge2.configuration.service.ConfigurationService;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.github.tofpu.dynamicconfiguration.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// todo: figure out a way to write & fetch nested keys and values
public class ConfigurationTest {

    private final File runtimeDirectory = new File("test-resources/runtime");
    private final File defaultDirectory = new File("test-resources/default/configuration");
    private final ConfigurationService service = new ConfigurationService(runtimeDirectory);

    @BeforeEach
    void setUp() {
        cleanRuntime();
        service.load();
    }

    @SuppressWarnings("all")
    private void cleanRuntime() {
        File[] files = runtimeDirectory.listFiles();
        if (files != null && files.length != 0) {
            Arrays.stream(files).forEach(File::delete);
        }
        runtimeDirectory.delete();
    }

    @AfterEach
    void tearDown() {
        service.unload();
//        cleanRuntime();
    }

    @Test
    void name() {
        assertNotNull(service.config());
    }
}
