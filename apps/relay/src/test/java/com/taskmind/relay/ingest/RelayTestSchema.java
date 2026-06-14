package com.taskmind.relay.ingest;

import org.springframework.jdbc.core.JdbcTemplate;

public final class RelayTestSchema {
    private RelayTestSchema() {}

    public static void create(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("create schema if not exists analytics");
        jdbcTemplate.execute("drop table if exists analytics.event_store");
        jdbcTemplate.execute("drop table if exists analytics.user_daily_metrics");
        jdbcTemplate.execute("drop table if exists analytics.project_daily_metrics");
        jdbcTemplate.execute("drop table if exists analytics.task_projection");
        jdbcTemplate.execute("drop table if exists analytics.ai_funnel_daily_metrics");
        jdbcTemplate.execute("drop table if exists analytics.project_projection");
        jdbcTemplate.execute("drop table if exists analytics.relay_dlq");
        jdbcTemplate.execute("create table analytics.event_store (event_id uuid primary key, event_type varchar(120) not null, actor_user_id uuid not null, entity_type varchar(80) not null, entity_id uuid not null, project_id uuid, occurred_at timestamp with time zone not null, payload text not null, context text not null, received_at timestamp with time zone not null)");
        jdbcTemplate.execute("create table analytics.user_daily_metrics (user_id uuid not null, metric_date date not null, tasks_created int not null default 0, tasks_completed int not null default 0, projects_created int not null default 0, events_ingested int not null default 0, updated_at timestamp with time zone not null, primary key (user_id, metric_date))");
        jdbcTemplate.execute("create table analytics.project_daily_metrics (project_id uuid not null, metric_date date not null, tasks_created int not null default 0, tasks_completed int not null default 0, projects_updated int not null default 0, events_ingested int not null default 0, updated_at timestamp with time zone not null, primary key (project_id, metric_date))");
        jdbcTemplate.execute("create table analytics.task_projection (task_id uuid primary key, user_id uuid not null, project_id uuid, title varchar(500) not null, status varchar(80) not null, updated_at timestamp with time zone not null)");
        jdbcTemplate.execute("create table analytics.ai_funnel_daily_metrics (user_id uuid not null, metric_date date not null, captures_submitted int not null default 0, suggestions_accepted int not null default 0, suggestions_rejected int not null default 0, events_ingested int not null default 0, updated_at timestamp with time zone not null, primary key (user_id, metric_date))");
        jdbcTemplate.execute("create table analytics.project_projection (project_id uuid primary key, owner_user_id uuid not null, name varchar(255) not null, project_key varchar(32) not null, archived boolean not null default false, updated_at timestamp with time zone not null)");
        jdbcTemplate.execute("create table analytics.relay_dlq (id uuid primary key, event_id uuid, event_type varchar(120), payload text not null, error_message text not null, failed_at timestamp with time zone not null)");
    }
}
