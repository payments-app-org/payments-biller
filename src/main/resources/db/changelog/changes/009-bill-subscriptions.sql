--liquibase formatted sql

--changeset biller:009-bill-subscriptions
-- «Счета на оплату»: пользователь сохраняет шаблон (поставщик + данные плательщика), а мы по нему спрашиваем
-- поставщика, есть ли неоплаченные счета. Только у поставщиков, которые выставляют счета (bills_order не пусто).
ALTER TABLE providers ADD COLUMN bills_order INTEGER;

UPDATE providers p SET bills_order = v.ord FROM (VALUES
    ('zhkh-moscow', 10), ('gis-zhkh', 20), ('zhkh-mo', 30), ('zhkh-spb', 35), ('mosenergosbyt', 36), ('mosobleirc', 37),
    ('petroelektrosbyt', 38), ('vodokanal-spb', 39),
    ('fns-inn', 40), ('fssp', 50), ('gibdd', 55),
    ('mts-home', 60), ('mgts', 70), ('mts-satellite', 80), ('mts-satellite-iptv', 90), ('rostelecom', 100),
    ('beeline-home', 110), ('tricolor', 120), ('telekarta', 130),
    ('edu-moscow', 140)
) AS v (code, ord) WHERE p.code = v.code;

-- Поля, по которым поставщик находит счета: у госуслуг — те же, что в поиске начислений,
-- у остальных поставщиков без поиска — номер счёта/договора (stage = BOTH: и для поиска, и для оплаты)
INSERT INTO provider_fields (provider_id, key, label, type, required, placeholder, hint, info, pattern, stage, prefill, sort_order)
SELECT p.id, f.key, f.label, f.type, f.required, f.placeholder, f.hint, NULL, f.pattern, 'SEARCH', NULL, f.sort_order
FROM providers p JOIN (VALUES
    ('gibdd', 'sts', 'Свидетельство о регистрации ТС', 'TEXT', false, 'XX XX XXXXXX', '10 символов', '\d{2}[0-9А-ЯЁA-Zа-яёa-z]{2}\d{6}', 10),
    ('gibdd', 'vu', 'Водительское удостоверение', 'TEXT', false, 'XX XX XXXXXX', '10 символов', '\d{2}[0-9А-ЯЁA-Zа-яёa-z]{2}\d{6}', 20),
    ('fns-inn', 'inn', 'ИНН', 'DIGITS', true, 'ИНН', '12 цифр', '\d{12}', 10),
    ('fssp', 'passport', 'Серия и номер паспорта', 'DIGITS', false, 'Серия и номер паспорта', '10 цифр', '\d{10}', 10),
    ('fssp', 'inn', 'ИНН', 'DIGITS', false, 'ИНН', '12 цифр', '\d{12}', 20)
) AS f (code, key, label, type, required, placeholder, hint, pattern, sort_order) ON f.code = p.code;

UPDATE provider_fields pf SET stage = 'BOTH'
FROM providers p
WHERE p.id = pf.provider_id AND pf.stage = 'PAY' AND pf.key IN ('account', 'receiver', 'contract')
  AND p.code IN ('mosenergosbyt', 'mosobleirc', 'petroelektrosbyt', 'vodokanal-spb', 'mts-home', 'mgts', 'mts-satellite',
                 'mts-satellite-iptv', 'rostelecom', 'beeline-home', 'tricolor', 'telekarta');

CREATE TABLE bill_subscriptions (
    id                UUID           PRIMARY KEY,
    user_id           UUID           NOT NULL,
    provider_id       INTEGER        NOT NULL REFERENCES providers (id),
    name              VARCHAR(50)    NOT NULL,
    -- Данные плательщика по полям поставщика: {key: value}
    fields            JSONB          NOT NULL,
    -- Последняя проверка у поставщика: когда и сколько неоплаченных счетов
    checked_at        TIMESTAMPTZ,
    unpaid_count      INTEGER        NOT NULL DEFAULT 0,
    unpaid_total      NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX bill_subscriptions_user_idx ON bill_subscriptions (user_id, created_at);
