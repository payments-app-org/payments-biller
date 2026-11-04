--liquibase formatted sql

--changeset biller:011-charity
-- Полное ФИО в сохранённых шаблонах «Счетов на оплату» (раньше в профиле была только буква фамилии)
UPDATE bill_subscriptions SET fields = replace(fields::text, '"Р Артур Константинович"', '"Руднев Артур Константинович"')::jsonb
WHERE fields::text LIKE '%Р Артур Константинович%';

-- Копилка фондов: округления покупок и пожертвования за день. Каждый платёж — одна строка, деньги уже списаны с клиента.
CREATE TABLE charity_accruals (
    id                UUID           PRIMARY KEY,
    provider_id       INTEGER        NOT NULL REFERENCES providers (id),
    source            VARCHAR(10)    NOT NULL CHECK (source IN ('ROUNDUP', 'DONATION')),
    payment_id        UUID           NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    -- День по Москве, за который копим
    day               DATE           NOT NULL,
    settlement_id     UUID,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    UNIQUE (payment_id, source)
);
CREATE INDEX charity_accruals_unsettled_idx ON charity_accruals (provider_id, day) WHERE settlement_id IS NULL;

-- Перевод фонду: один на фонд за прогон (обычно — раз в день). Процессинг принимает его идемпотентно по id.
CREATE TABLE charity_settlements (
    id                UUID           PRIMARY KEY,
    provider_id       INTEGER        NOT NULL REFERENCES providers (id),
    day               DATE           NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    accruals          INTEGER        NOT NULL,
    status            VARCHAR(10)    NOT NULL CHECK (status IN ('SENT', 'DONE', 'FAILED')),
    processing_ref    VARCHAR(60),
    message           VARCHAR(255),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    finished_at       TIMESTAMPTZ
);
