package com.taskmind.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties =
                "spring.datasource.url=jdbc:h2:mem:flyway-startup;MODE=PostgreSQL;NON_KEYWORDS=KEY,VALUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
class FlywayStartupIntegrationTest {

    @Autowired private Flyway flyway;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void startsWithCompleteMigrationChain() {
        var appliedMigrations = Arrays.stream(flyway.info().applied()).toList();

        assertThat(appliedMigrations)
                .extracting(migration -> migration.getVersion().getVersion())
                .containsExactly(
                        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
                        "15", "16", "17");
        assertThat(appliedMigrations)
                .allMatch(migration -> migration.getState() == MigrationState.SUCCESS);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM information_schema.columns "
                                        + "WHERE LOWER(table_name) = 'projects' AND LOWER(column_name) = 'project_key'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM information_schema.tables "
                                        + "WHERE LOWER(table_schema) = 'analytics' AND LOWER(table_name) = 'ai_funnel_daily_metrics'",
                                Integer.class))
                .isEqualTo(1);
    }
}
