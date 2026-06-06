create table task_attachments (
    id uuid primary key,
    version bigint not null default 0,
    task_id uuid not null references tasks(id),
    owner_user_id uuid not null,
    object_key varchar(1024) not null unique,
    file_name varchar(255) not null,
    content_type varchar(255) not null,
    size_bytes bigint not null,
    media_kind varchar(32) not null,
    deleted_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint chk_task_attachments_size_positive check (size_bytes > 0)
);

create index idx_task_attachments_task_active on task_attachments(task_id, deleted_at, created_at desc);
create index idx_task_attachments_owner on task_attachments(owner_user_id);
