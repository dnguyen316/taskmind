package com.taskmind.relay.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RelayJdbcParametersTest {
    @Test
    void timestampConvertsInstantForJdbcDriversThatDoNotInferInstantTypes() {
        Instant instant = Instant.parse("2026-06-21T16:14:14.117Z");

        Timestamp timestamp = RelayJdbcParameters.timestamp(instant);

        assertEquals(Timestamp.from(instant), timestamp);
        assertEquals(instant, timestamp.toInstant());
    }

    @Test
    void timestampPreservesNullValues() {
        assertNull(RelayJdbcParameters.timestamp(null));
    }
}
