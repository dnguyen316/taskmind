package com.taskmind.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                .contains("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
                        "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28",
                        "30", "31", "32");
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
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM information_schema.columns "
                                        + "WHERE LOWER(table_schema) = 'analytics' AND LOWER(table_name) = 'task_projection' AND LOWER(column_name) = 'task_type_key'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void rejectsDuplicateGlobalTaskTypeKeys() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                                INSERT INTO task_types (id, project_id, type_key, name, system, active, default_task_level, allowed_task_levels, is_container, allow_children, created_at, updated_at, version)
                                VALUES ('00000000-0000-0000-0000-000000000901', NULL, 'TASK', 'Duplicate Task', FALSE, TRUE, 'TASK', 'TASK', FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                                """))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class)
                .hasMessageContaining("UX_TASK_TYPES_GLOBAL_TYPE_KEY");
    }

}
