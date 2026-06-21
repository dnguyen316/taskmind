package com.taskmind.relay.jdbc;

import java.sql.Timestamp;
import java.time.Instant;

public final class RelayJdbcParameters {
    private RelayJdbcParameters() {}

    public static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
