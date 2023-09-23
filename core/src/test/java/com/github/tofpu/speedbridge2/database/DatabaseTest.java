package com.github.tofpu.speedbridge2.database;

import com.github.tofpu.speedbridge2.database.driver.type.H2DriverOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class DatabaseTest {

    private final AsyncDatabase asyncDatabase = DatabaseBuilder.create(
            "com.github.tofpu.speedbridge2")
        .build(H2DriverOptions.create(), DatabaseFactoryMaker.async(Executors.newSingleThreadExecutor()));

    private final Database syncDatabase = DatabaseBuilder.create("com.github.tofpu.speedbridge2")
        .build(H2DriverOptions.create(), DatabaseFactoryMaker.sync());


    @AfterEach
    void tearDown() {
        this.asyncDatabase.shutdown();
        this.syncDatabase.shutdown();
    }

    @Test
    void async_sanity_check() throws InterruptedException, ExecutionException, TimeoutException {
        // I know this looks like insanity, but basically this test
        // verifies that database's compute is actually working
        // and not bogus
        Thread mainThread = Thread.currentThread();

        AtomicReference<Throwable> exceptionThrown = new AtomicReference<>();
        mainThread.setUncaughtExceptionHandler((t, e) -> exceptionThrown.set(e));

        asyncDatabase.executeAsync(session -> mainThread.getUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), new RuntimeException()))
            .get(5, TimeUnit.SECONDS);

        Assertions.assertNotNull(exceptionThrown.get());
    }

    @Test
    void sync_sanity_check() {
        // I know this looks like insanity, but basically this test
        // verifies that database's compute is actually working
        // and not bogus
        Thread mainThread = Thread.currentThread();

        AtomicReference<Throwable> exceptionThrown = new AtomicReference<>();
        mainThread.setUncaughtExceptionHandler((t, e) -> exceptionThrown.set(e));

        asyncDatabase.execute(session -> mainThread.getUncaughtExceptionHandler()
            .uncaughtException(Thread.currentThread(), new RuntimeException()));

        Assertions.assertNotNull(exceptionThrown.get());
    }

    @Test
    void async_operation_test() throws ExecutionException, InterruptedException, TimeoutException {
        final Thread mainThread = Thread.currentThread();
        asyncDatabase.executeAsync(
                session -> Assertions.assertNotEquals(mainThread.getId(),
                    Thread.currentThread().getId()))
            .get(5, TimeUnit.SECONDS);
    }

    @Test
    void test_file_based_database() {
        syncDatabase.execute(session -> Assertions.assertTrue(session.isConnected()));
    }

    @Test
    void basic_persist_and_modify_operation_test() {
        UUID id = UUID.randomUUID();
        syncDatabase.execute(session -> {
            System.out.println("insert entity with id: " + id);
            session.persist(new DemoEntity(id, 2));
        });
        syncDatabase.execute(session -> session.get(DemoEntity.class, id).number(20));
        syncDatabase.execute(session -> {
            DemoEntity demoEntity = session.get(DemoEntity.class, id);
            Assertions.assertEquals(20, demoEntity.getNumber());
        });
    }

    @Test
    void async_operation_in_sync_db_test() {
        Assertions.assertFalse(syncDatabase.supportsAsync());
    }

    @Test
    void mysql_driver_test() {

    }
}
