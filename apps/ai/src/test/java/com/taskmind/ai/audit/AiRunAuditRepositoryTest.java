package com.taskmind.ai.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.AiProviderId;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

class AiRunAuditRepositoryTest {
    private final CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiRunAuditRepository repository = new AiRunAuditRepository(jdbcTemplate, objectMapper);

    @Test
    void startBindsInstantsAsOffsetDateTimesWithExplicitTimestampWithTimezoneType() throws Exception {
        AiRunRecord record =
                new AiRunRecord(
                        UUID.randomUUID(),
                        "workspace-a",
                        new AiCapabilityId("chat"),
                        new AiProviderId("mock"),
                        "mock-model",
                        "request-hash",
                        objectMapper.createObjectNode().put("message", "hello"),
                        "chat.v1",
                        "VALID",
                        "corr-1");

        repository.start(record);

        List<TypedObjectBinding> bindings = new ArrayList<>();
        PreparedStatement preparedStatement = capturingPreparedStatement(bindings);
        jdbcTemplate.setter.setValues(preparedStatement);

        assertThat(bindings)
                .filteredOn(binding -> binding.sqlType() == Types.TIMESTAMP_WITH_TIMEZONE)
                .extracting(TypedObjectBinding::parameterIndex)
                .containsExactly(13, 14);
        assertThat(bindings)
                .filteredOn(binding -> binding.sqlType() == Types.TIMESTAMP_WITH_TIMEZONE)
                .extracting(TypedObjectBinding::value)
                .allSatisfy(value -> assertThat(value).isInstanceOf(OffsetDateTime.class));
    }

    private PreparedStatement capturingPreparedStatement(List<TypedObjectBinding> bindings) {
        return (PreparedStatement)
                Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[] {PreparedStatement.class},
                        (proxy, method, args) -> {
                            if ("setObject".equals(method.getName()) && args.length == 3) {
                                bindings.add(
                                        new TypedObjectBinding(
                                                (Integer) args[0], args[1], (Integer) args[2]));
                            }
                            return null;
                        });
    }

    private record TypedObjectBinding(int parameterIndex, Object value, int sqlType) {}

    private static class CapturingJdbcTemplate extends JdbcTemplate {
        private PreparedStatementSetter setter;

        @Override
        public int update(String sql, PreparedStatementSetter pss) {
            this.setter = pss;
            return 1;
        }
    }
}
