--liquibase formatted sql

--changeset biller:001-init
-- Справочники для переводов за рубеж. Меняются редко, поэтому живут в отдельном сервисе-справочнике.

-- Страны, куда можно перевести. Названия — по-русски и на основном языке страны.
CREATE TABLE countries (
    code              VARCHAR(2)   PRIMARY KEY,
    name_ru           VARCHAR(100) NOT NULL,
    name_native       VARCHAR(100) NOT NULL,
    dial_code         VARCHAR(6),
    popular           BOOLEAN      NOT NULL DEFAULT false,
    enabled           BOOLEAN      NOT NULL DEFAULT true,
    sort_order        INTEGER      NOT NULL
);

-- В какой валюте может получить деньги получатель в этой стране
CREATE TABLE country_currencies (
    country_code      VARCHAR(2)   NOT NULL REFERENCES countries (code),
    currency          VARCHAR(3)   NOT NULL,
    sort_order        INTEGER      NOT NULL,
    PRIMARY KEY (country_code, currency)
);

-- Шаблоны данных получателя: какие поля просить у пользователя для способа перевода
CREATE TABLE field_templates (
    code              VARCHAR(30)  PRIMARY KEY,
    name              VARCHAR(100) NOT NULL
);

CREATE TABLE template_fields (
    template_code     VARCHAR(30)  NOT NULL REFERENCES field_templates (code),
    field_key         VARCHAR(30)  NOT NULL,
    label             VARCHAR(100) NOT NULL,
    -- CARD_NUMBER, TEXT, PHONE, IBAN, ACCOUNT, SWIFT, IFSC — подсказка интерфейсу, как форматировать ввод
    field_type        VARCHAR(20)  NOT NULL,
    -- Проверка значения (регулярное выражение; пробелы перед проверкой убираются)
    pattern           VARCHAR(200) NOT NULL,
    placeholder       VARCHAR(100),
    sort_order        INTEGER      NOT NULL,
    PRIMARY KEY (template_code, field_key)
);

-- Способы перевода в стране: местные банки, кошельки, Visa/Mastercard, SWIFT
CREATE TABLE transfer_methods (
    id                SERIAL       PRIMARY KEY,
    country_code      VARCHAR(2)   NOT NULL REFERENCES countries (code),
    name              VARCHAR(100) NOT NULL,
    kind              VARCHAR(20)  NOT NULL CHECK (kind IN ('CARD', 'PHONE', 'ACCOUNT', 'WALLET')),
    template_code     VARCHAR(30)  NOT NULL REFERENCES field_templates (code),
    color             VARCHAR(7)   NOT NULL,
    enabled           BOOLEAN      NOT NULL DEFAULT true,
    sort_order        INTEGER      NOT NULL
);

CREATE INDEX transfer_methods_country_idx ON transfer_methods (country_code, sort_order);
