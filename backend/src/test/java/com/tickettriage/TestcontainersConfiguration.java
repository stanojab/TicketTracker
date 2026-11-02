package com.tickettriage;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a real PostgreSQL container for integration tests.
 * {@code @ServiceConnection} makes Spring Boot point the DataSource at it automatically.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        // Same major version as docker-compose.yml and Azure Flexible Server.
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }
}
