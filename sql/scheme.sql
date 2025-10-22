-- Schema reference for hiring task
-- partner master
create table if not exists partner (
  id bigint auto_increment primary key,
  code varchar(64) not null unique,
  name varchar(255) not null,
  active boolean not null default true
);

create table if not exists partner_fee_policy (
                                                  id bigint auto_increment primary key,
                                                  partner_id bigint not null,
                                                  effective_from timestamp not null,
                                                  percentage decimal(10,6) not null,
                                                  fixed_fee decimal(15,0)
);

create index if not exists idx_fee_partner_from
    on partner_fee_policy (partner_id, effective_from desc);

create table if not exists payment (
                                       id bigint auto_increment primary key,
                                       partner_id bigint not null,
                                       amount decimal(15,0) not null,
                                       applied_fee_rate decimal(10,6) not null,
                                       fee_amount decimal(15,0) not null,
                                       net_amount decimal(15,0) not null,
                                       card_bin varchar(8),
                                       card_last4 varchar(4),
                                       approval_code varchar(32) not null,
                                       approved_at timestamp not null,
                                       status varchar(20) not null,
                                       created_at timestamp not null,
                                       updated_at timestamp not null
);

create index if not exists idx_payment_created
    on payment (created_at desc, id desc);

create index if not exists idx_payment_partner_created
    on payment (partner_id, created_at desc);