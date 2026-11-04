--liquibase formatted sql

--changeset biller:003-providers
-- Поставщики услуг. Одна общая таблица на всех (госуслуги, ЖКХ, связь, игры…): кто это и через какую интеграцию
-- с ним работаем. Особенности группы — в отдельной таблице группы (gov_providers и т. д.).
CREATE TABLE provider_groups (
    code              VARCHAR(30)  PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    sort_order        INTEGER      NOT NULL
);

INSERT INTO provider_groups (code, name, sort_order) VALUES
    ('GOV',       'Госуслуги, штрафы, налоги', 10),
    ('UTILITIES', 'Услуги ЖКХ',                20),
    ('MOBILE',    'Мобильная связь',           30),
    ('INTERNET',  'Интернет и ТВ',             40),
    ('GAMES',     'Игры и сервисы',            50);

CREATE TABLE providers (
    id                SERIAL       PRIMARY KEY,
    group_code        VARCHAR(30)  NOT NULL REFERENCES provider_groups (code),
    name              VARCHAR(150) NOT NULL,
    inn               VARCHAR(12),
    -- Код адаптера интеграции: как biller ходит к поставщику
    integration       VARCHAR(30)  NOT NULL,
    color             VARCHAR(7)   NOT NULL DEFAULT '#7884a8',
    enabled           BOOLEAN      NOT NULL DEFAULT true
);

-- Группа «Госуслуги»: оплата по УИН через ГИС ГМП
CREATE TABLE gov_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    -- Что ищем: начисления по УИН
    lookup            VARCHAR(20)  NOT NULL CHECK (lookup IN ('UIN')),
    description       VARCHAR(255)
);

INSERT INTO providers (group_code, name, inn, integration, color) VALUES
    ('GOV', 'Госуслуги — оплата по УИН', NULL, 'GIS_GMP', '#0d4cd3');
INSERT INTO gov_providers (provider_id, lookup, description)
SELECT id, 'UIN', 'Оплата по уникальному идентификатору начисления' FROM providers WHERE integration = 'GIS_GMP';

-- Начисления, полученные из ГИС ГМП. Храним у себя: payment проверяет сумму и получателя по этой записи,
-- а не по тому, что прислал фронтенд.
CREATE TABLE gov_charges (
    id                UUID           PRIMARY KEY,
    provider_id       INTEGER        NOT NULL REFERENCES providers (id),
    external_id       VARCHAR(60)    NOT NULL UNIQUE,
    uin               VARCHAR(25)    NOT NULL,
    purpose           VARCHAR(255)   NOT NULL,
    payee_name        VARCHAR(255)   NOT NULL,
    payee_inn         VARCHAR(12)    NOT NULL,
    payee_kpp         VARCHAR(9)     NOT NULL,
    payee_account     VARCHAR(20)    NOT NULL,
    payee_bik         VARCHAR(9)     NOT NULL,
    kbk               VARCHAR(20)    NOT NULL,
    oktmo             VARCHAR(11)    NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL,
    -- Сумма со скидкой (например, −50% на штрафы ГИБДД первые 20 дней) и до какого дня она действует
    discounted_amount NUMERIC(19, 2),
    discount_until    DATE,
    issued_at         DATE           NOT NULL,
    status            VARCHAR(10)    NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID')),
    fetched_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX gov_charges_uin_idx ON gov_charges (uin);
