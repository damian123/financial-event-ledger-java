package io.github.damian123.eventledger.support;

import org.testcontainers.containers.PostgreSQLContainer;

public final class SharedPostgres {

    public static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("eventledger")
            .withUsername("eventledger")
            .withPassword("eventledger");

    static {
        CONTAINER.start();
    }

    private SharedPostgres() {
    }
}
