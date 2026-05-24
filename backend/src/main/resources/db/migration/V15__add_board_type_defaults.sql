alter table board
    add column type varchar(30) null,
    add column default_board boolean not null default false,
    add column sort_order integer not null default 999;

update board
set type = 'FREE',
    default_board = true,
    sort_order = 10
where scope = 'SCHOOL'
  and title = '자유게시판';

create index idx_board_school_scope_active_sort
    on board (school_id, scope, active, sort_order);

alter table board
    add constraint uq_board_school_type unique (school_id, type);
