package com.warehouse.wms.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.Flyway;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs only where Docker is available; validates compatibility with real MySQL rather than H2 emulation. */
@Testcontainers(disabledWithoutDocker = true)
class MySqlContainerSmokeTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4").withDatabaseName("wms_test");

    @Test
    void startsRealMySqlDatabaseAndAppliesMigrations() throws Exception {
        Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()).load().migrate();
        try (var connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'wms_test' AND table_name = 'products'")) {
            assertTrue(result.next());
            assertTrue(result.getInt(1) == 1);
        }
    }
}
