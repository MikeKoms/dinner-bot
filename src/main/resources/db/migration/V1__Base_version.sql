create table "USERS" (
  "TELEGRAMID" VARCHAR NOT NULL,
  "PRIVATECHATID" VARCHAR NOT NULL,
  "ID" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT);

create table "VOTES" (
  "POOL_ID" BIGINT NOT NULL,
  "USER_ID" BIGINT NOT NULL,
  "CHOICE" VARCHAR DEFAULT 'empty' NOT NULL,
  "ISVOTED" BOOLEAN NOT NULL,
  "ID" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT)

create table "POLLS" (
  "USER_ID" BIGINT NOT NULL,
  "CHATID" VARCHAR NOT NULL,
  "ISFINISHED" BOOLEAN DEFAULT false NOT NULL,
  "ID" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT);

alter table "VOTES" add constraint "FK_POOL" foreign key("POOL_ID") references "POLLS"("ID") on update NO ACTION on delete NO ACTION;
alter table "VOTES" add constraint "FK_USER" foreign key("USER_ID") references "USERS"("ID") on update NO ACTION on delete NO ACTION;
alter table "POLLS" add constraint "FK_CREATOR" foreign key("USER_ID") references "USERS"("ID") on update NO ACTION on delete NO ACTION;
