create table ad_banner (
    id bigint not null auto_increment primary key,
    placement varchar(30) not null,
    title varchar(80) not null,
    subtitle varchar(160) not null,
    image_url varchar(500),
    link_url varchar(500),
    active boolean not null default true,
    priority integer not null default 100,
    start_at timestamp,
    end_at timestamp,
    created_at timestamp,
    updated_at timestamp
);

create index idx_ad_banner_active_placement
    on ad_banner (placement, active, priority);

create index idx_ad_banner_period
    on ad_banner (start_at, end_at);
