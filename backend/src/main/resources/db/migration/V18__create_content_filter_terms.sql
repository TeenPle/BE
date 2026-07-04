create table content_filter_term (
    id bigint not null auto_increment primary key,
    language varchar(10) not null,
    term varchar(255) not null,
    normalized_term varchar(255) not null,
    severity varchar(20) not null,
    category varchar(40) not null,
    source varchar(40) not null,
    source_key varchar(500),
    enabled boolean not null default true,
    created_at timestamp,
    updated_at timestamp,
    constraint uk_content_filter_term_language_normalized unique (language, normalized_term)
);

create index idx_content_filter_term_enabled_language
    on content_filter_term (enabled, language);

create index idx_content_filter_term_source
    on content_filter_term (source, source_key);
