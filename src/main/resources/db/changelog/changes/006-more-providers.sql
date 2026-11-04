--liquibase formatted sql

--changeset biller:006-more-providers
-- Электронные деньги, образование, интернет-магазины, путешествия, страхование.
-- Новое в шаблонах полей: поиск счетов у поставщика (stage = SEARCH — поля для «Найти», дальше оплата найденного счёта),
-- поле-месяц (MONTH) и подстановка данных клиента (prefill: ФИО, телефон).

INSERT INTO provider_groups (code, name, sort_order) VALUES
    ('EMONEY',    'Электронные деньги',  60),
    ('EDUCATION', 'Образование',         70),
    ('SHOPS',     'Интернет-магазины',   80),
    ('TRAVEL',    'Путешествие',         90),
    ('INSURANCE', 'Страхование',        100);

ALTER TABLE provider_fields DROP CONSTRAINT provider_fields_type_check;
ALTER TABLE provider_fields ADD CONSTRAINT provider_fields_type_check
    CHECK (type IN ('TEXT', 'DIGITS', 'EMAIL', 'PHONE', 'LOCAL_PHONE', 'CHOICE', 'NOMINAL', 'MONTH'));
ALTER TABLE provider_fields ADD COLUMN stage   VARCHAR(6)  NOT NULL DEFAULT 'PAY' CHECK (stage IN ('SEARCH', 'PAY'));
ALTER TABLE provider_fields ADD COLUMN prefill VARCHAR(20) CHECK (prefill IN ('USER_FULL_NAME', 'USER_PHONE'));

-- Счета, найденные у поставщика по полям поиска (Образование Москвы: питание, продлёнка, кружки).
-- Сумма и назначение — отсюда, а не из запроса.
CREATE TABLE provider_invoices (
    id                UUID           PRIMARY KEY,
    provider_id       INTEGER        NOT NULL REFERENCES providers (id),
    external_id       VARCHAR(60)    NOT NULL UNIQUE,
    title             VARCHAR(255)   NOT NULL,
    description       VARCHAR(255),
    amount            NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    due_date          DATE,
    status            VARCHAR(10)    NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID')),
    fetched_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Особенности групп
CREATE TABLE emoney_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    -- Кошелёк можно указать номером телефона или номером счёта
    accepts_phone     BOOLEAN      NOT NULL,
    account_prefix    VARCHAR(10)
);

CREATE TABLE education_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    kind              VARCHAR(20)  NOT NULL CHECK (kind IN ('CITY_SERVICES', 'SCHOOL_CARD', 'SCHOOL', 'COURSES')),
    -- Ищем выставленные счета (stage = SEARCH) или платим по реквизитам ученика
    invoice_lookup    BOOLEAN      NOT NULL DEFAULT false
);

CREATE TABLE shop_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    sales_model       VARCHAR(20)  NOT NULL CHECK (sales_model IN ('DIRECT_SALES', 'ONLINE_STORE'))
);

CREATE TABLE travel_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    kind              VARCHAR(20)  NOT NULL CHECK (kind IN ('AIR_TICKETS', 'TOUR_OPERATOR', 'TRAVEL_AGENCY')),
    -- Номер в едином федеральном реестре туроператоров
    registry_number   VARCHAR(20)
);

CREATE TABLE insurance_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    kind              VARCHAR(20)  NOT NULL CHECK (kind IN ('INVOICE', 'POLICY', 'CREDIT', 'ACCIDENT', 'PROPERTY', 'OSAGO')),
    license           VARCHAR(30)
);

INSERT INTO providers (group_code, code, name, title, legal_name, domain, integration, color, sort_order, min_amount, max_amount) VALUES
    ('EMONEY',    'yoomoney',          'ЮMoney',                                'ЮMoney',                               'НКО «ЮМани»',                      'yoomoney.ru',       'EMONEY_GATE',    '#8b3ffd', 10, 10, 60000),

    ('EDUCATION', 'edu-moscow',        'Образование Москвы',                    'Образование Москвы',                   'Департамент образования и науки Москвы', 'mos.ru',      'EDUCATION_GATE', '#2f73c9', 10, NULL, NULL),
    ('EDUCATION', 'moskvenok',         'Москвенок - пополнение карты',          'Москвенок - пополнение карты',         'Карта школьника «Москвёнок»',      'shkolamoskva.ru',   'EDUCATION_GATE', '#f26b5b', 20, 10, 5000),
    ('EDUCATION', 'gymnasium-podolsk', 'Гимназия имени Подольских курсантов',   'Гимназия имени Подольских курсантов',  'МБОУ «Гимназия им. Подольских курсантов»', NULL,        'EDUCATION_GATE', '#2d2f36', 30, 10, 100000),
    ('EDUCATION', 'lyceum-19-korolev', 'МАОУ Лицей № 19. Королев',              'МАОУ Лицей № 19. Королев',             'МАОУ «Лицей № 19», г. Королёв',    NULL,                'EDUCATION_GATE', '#1e3a8a', 40, 10, 100000),
    ('EDUCATION', 'mshp',              'Московская школа программистов',        'Московская школа программистов',       'АНО ДО «МШП»',                     'informatics.ru',    'EDUCATION_GATE', '#1d9bd6', 50, 100, 300000),

    ('SHOPS',     'oriflame',          'Орифлэйм',                              'Орифлэйм',                             'ООО «Орифлэйм Косметикс»',         'oriflame.ru',       'SHOPS_GATE',     '#1f1f1f', 10, 10, 100000),
    ('SHOPS',     'amway',             'Amway',                                 'Amway',                                'ООО «Амвэй»',                      'amway.ru',          'SHOPS_GATE',     '#2a4c9b', 20, 10, 100000),

    ('TRAVEL',    'airbooking',        'Airbooking',                            'Airbooking',                           'ООО «Эйрбукинг»',                  'airbooking.ru',     'TRAVEL_GATE',    '#8fc1c3', 10, 100, 600000),
    ('TRAVEL',    'pegas',             'PEGAS TOURISTIK',                       'PEGAS TOURISTIK',                      'ООО «Пегас Туристик»',             'pegast.ru',         'TRAVEL_GATE',    '#1a5fa8', 20, 100, 600000),
    ('TRAVEL',    'azimut-360',        'АЗИМУТ 360',                            'АЗИМУТ 360',                           'ООО «Азимут 360»',                 'azimut360.ru',      'TRAVEL_GATE',    '#2bb5d8', 30, 100, 600000),
    ('TRAVEL',    'itm-sever',         'АЙ ТИ ЭМ - СЕВЕР',                      'АЙ ТИ ЭМ - СЕВЕР',                     'ООО «АЙ ТИ ЭМ — Север»',           NULL,                'TRAVEL_GATE',    '#e2672b', 40, 100, 600000),
    ('TRAVEL',    'appeks',            'АППЕКС',                                'АППЕКС',                               'ООО «Аппекс-тур»',                 NULL,                'TRAVEL_GATE',    '#c62828', 50, 100, 600000),
    ('TRAVEL',    'alem-tour',         'Алем-Тур',                              'Алем-Тур',                             'ООО «Алем-Тур»',                   NULL,                'TRAVEL_GATE',    '#e53935', 60, 100, 600000),
    ('TRAVEL',    'aerotour',          'АэроТур',                               'АэроТур',                              'ООО «АэроТур»',                    NULL,                'TRAVEL_GATE',    '#283593', 70, 100, 600000),
    ('TRAVEL',    'breezel',           'Бриз Лайн (BreezeL)',                   'Бриз Лайн (BreezeL)',                  'ООО «Бриз Лайн»',                  NULL,                'TRAVEL_GATE',    '#1e5aa8', 80, 100, 600000),

    ('INSURANCE', 'vsk-invoice',       'ВСК САО. По выставленному счету',       'ВСК САО. По выставленному счету',      'САО «ВСК»',                        'vsk.ru',            'INSURANCE_GATE', '#1d6fb8', 10, 10, 600000),
    ('INSURANCE', 'vsk-osago',         'ВСК САО. ОСАГО',                        'ВСК САО. Продление ОСАГО',             'САО «ВСК»',                        'vsk.ru',            'INSURANCE_GATE', '#1d6fb8', 20, 500, 100000),
    ('INSURANCE', 'hc-insurance',      'Хоум Кредит Страхование',               'Хоум Кредит Страхование',              'ООО «Хоум Кредит Страхование»',    'homecredit.ru',     'INSURANCE_GATE', '#d71920', 30, 10, 300000),
    ('INSURANCE', 'hc-credit',         'Хоум Кредит Страхование-кредит',        'Хоум Кредит Страхование-кредит',       'ООО «Хоум Кредит Страхование»',    'homecredit.ru',     'INSURANCE_GATE', '#d71920', 40, 10, 300000),
    ('INSURANCE', 'hc-accident',       'Хоум Кредит Страхование-от несчастного случая', 'Хоум Кредит Страхование-от несчастного случая', 'ООО «Хоум Кредит Страхование»', 'homecredit.ru', 'INSURANCE_GATE', '#d71920', 50, 10, 300000),
    ('INSURANCE', 'hc-property',       'Хоум Кредит Страхование-страхование имущества', 'Хоум Кредит Страхование-страхование имущества', 'ООО «Хоум Кредит Страхование»', 'homecredit.ru', 'INSURANCE_GATE', '#d71920', 60, 10, 300000);

INSERT INTO emoney_providers (provider_id, accepts_phone, account_prefix)
SELECT id, true, '4100' FROM providers WHERE code = 'yoomoney';

INSERT INTO education_providers (provider_id, kind, invoice_lookup)
SELECT p.id, e.kind, e.lookup FROM providers p JOIN (VALUES
    ('edu-moscow', 'CITY_SERVICES', true), ('moskvenok', 'SCHOOL_CARD', false), ('gymnasium-podolsk', 'SCHOOL', false),
    ('lyceum-19-korolev', 'SCHOOL', false), ('mshp', 'COURSES', false)
) AS e (code, kind, lookup) ON e.code = p.code;

INSERT INTO shop_providers (provider_id, sales_model)
SELECT id, 'DIRECT_SALES' FROM providers WHERE code IN ('oriflame', 'amway');

INSERT INTO travel_providers (provider_id, kind, registry_number)
SELECT p.id, t.kind, t.reg FROM providers p JOIN (VALUES
    ('airbooking', 'AIR_TICKETS', NULL), ('pegas', 'TOUR_OPERATOR', 'РТО 016634'), ('azimut-360', 'TRAVEL_AGENCY', NULL),
    ('itm-sever', 'TRAVEL_AGENCY', NULL), ('appeks', 'TOUR_OPERATOR', 'РТО 003958'), ('alem-tour', 'TOUR_OPERATOR', 'РТО 022211'),
    ('aerotour', 'AIR_TICKETS', NULL), ('breezel', 'TOUR_OPERATOR', 'РТО 019702')
) AS t (code, kind, reg) ON t.code = p.code;

INSERT INTO insurance_providers (provider_id, kind, license)
SELECT p.id, i.kind, i.lic FROM providers p JOIN (VALUES
    ('vsk-invoice', 'INVOICE', 'СИ № 0621'), ('vsk-osago', 'OSAGO', 'СИ № 0621'), ('hc-insurance', 'POLICY', 'СЛ № 4180'),
    ('hc-credit', 'CREDIT', 'СЛ № 4180'), ('hc-accident', 'ACCIDENT', 'СЛ № 4180'), ('hc-property', 'PROPERTY', 'СЛ № 4180')
) AS i (code, kind, lic) ON i.code = p.code;

INSERT INTO provider_fields (provider_id, key, label, type, required, placeholder, hint, info, pattern, stage, prefill, sort_order)
SELECT p.id, f.key, f.label, f.type, f.required, f.placeholder, f.hint, f.info, f.pattern, f.stage, f.prefill, f.sort_order
FROM providers p JOIN (VALUES
    ('yoomoney', 'wallet', 'Номер телефона или счета', 'DIGITS', true, 'Номер телефона или счета', 'до 25 цифр',
        'Номер кошелька начинается с 4100 — он есть в приложении ЮMoney. Телефон — 11 цифр, начиная с 7', '(7\d{10}|4100\d{7,21})', 'PAY', NULL, 10),

    ('edu-moscow', 'payerName', 'ФИО плательщика', 'TEXT', true, 'ФИО плательщика', NULL, NULL, '[А-Яа-яЁё -]{5,150}', 'SEARCH', 'USER_FULL_NAME', 10),
    ('edu-moscow', 'snils', 'СНИЛС ребёнка', 'DIGITS', true, 'СНИЛС ребёнка', 'XXX-XXX-XXX XX', NULL, '\d{11}', 'SEARCH', NULL, 20),
    ('edu-moscow', 'birthCert', 'Свидетельство о рождении', 'TEXT', false, 'Свидетельство о рождении', 'Например, IV-МЮ 123456', NULL,
        '[IVXLCivxlc]{1,4}-?[А-Яа-яЁё]{2} ?\d{6}', 'SEARCH', NULL, 30),
    ('edu-moscow', 'parentPassport', 'Паспорт родителя', 'DIGITS', true, 'Паспорт родителя', 'XX XX XXXXXX', NULL, '\d{10}', 'SEARCH', NULL, 40),

    ('moskvenok', 'card', 'Номер карты «Москвёнок»', 'DIGITS', true, 'Номер карты', '10 цифр', 'Номер напечатан на обороте карты', '\d{10}', 'PAY', NULL, 10),
    ('moskvenok', 'balance', 'Что пополнить', 'CHOICE', true, NULL, NULL, NULL, NULL, 'PAY', NULL, 20),

    ('gymnasium-podolsk', 'pupil', 'ФИО ученика', 'TEXT', true, 'ФИО ученика', NULL, NULL, '[А-Яа-яЁё -]{5,150}', 'PAY', NULL, 10),
    ('gymnasium-podolsk', 'class', 'Класс', 'TEXT', true, 'Например, 5А', NULL, NULL, '(1[01]|[1-9]) ?[А-Яа-я]', 'PAY', NULL, 20),
    ('gymnasium-podolsk', 'purpose', 'Назначение платежа', 'CHOICE', true, NULL, NULL, NULL, NULL, 'PAY', NULL, 30),

    ('lyceum-19-korolev', 'account', 'Лицевой счёт ученика', 'DIGITS', true, 'Лицевой счёт', '8 цифр', 'Лицевой счёт выдаёт бухгалтерия лицея', '\d{8}', 'PAY', NULL, 10),
    ('lyceum-19-korolev', 'payerName', 'ФИО плательщика', 'TEXT', true, 'ФИО плательщика', NULL, NULL, '[А-Яа-яЁё -]{5,150}', 'PAY', 'USER_FULL_NAME', 20),

    ('mshp', 'purpose', 'Назначение платежа', 'TEXT', true, 'Назначение платежа', 'Например, курс Python, группа 12', NULL, '.{3,140}', 'PAY', NULL, 10),
    ('mshp', 'period', 'Период оплаты', 'MONTH', true, NULL, 'Месяц', NULL, NULL, 'PAY', NULL, 20),
    ('mshp', 'senderName', 'ФИО отправителя', 'TEXT', true, 'ФИО отправителя', NULL, NULL, '[А-Яа-яЁё -]{5,150}', 'PAY', 'USER_FULL_NAME', 30),

    ('oriflame', 'client', 'Номер клиента', 'DIGITS', true, 'Номер клиента', 'до 8 цифр', NULL, '\d{1,8}', 'PAY', NULL, 10),
    ('amway', 'account', 'Счёт клиента', 'DIGITS', true, 'Счёт клиента', 'от 6 до 10 цифр', NULL, '\d{6,10}', 'PAY', NULL, 10),
    ('amway', 'order', 'Номер заказа', 'DIGITS', true, 'Номер заказа', '9 или 10 цифр', NULL, '\d{9,10}', 'PAY', NULL, 20),

    ('airbooking', 'order', 'Номер заказа', 'TEXT', true, 'AB', '8 символов', NULL, '[A-Za-z0-9]{8}', 'PAY', NULL, 10),
    ('airbooking', 'phone', 'Номер телефона', 'PHONE', true, '+7', '10 цифр', NULL, NULL, 'PAY', 'USER_PHONE', 20),
    ('pegas', 'claim', 'Номер заявки', 'DIGITS', true, 'Номер заявки', '7 цифр', 'Номер заявки — в договоре с турагентством', '\d{7}', 'PAY', NULL, 10),
    ('pegas', 'tourist', 'ФИО туриста', 'TEXT', true, 'ФИО туриста', 'Как в заявке', NULL, '[А-Яа-яЁёA-Za-z -]{5,150}', 'PAY', NULL, 20),
    ('azimut-360', 'pnr', 'Номер бронирования', 'TEXT', true, 'Номер бронирования', '6 символов, латиница и цифры', NULL, '[A-Za-z0-9]{6}', 'PAY', NULL, 10),
    ('azimut-360', 'surname', 'Фамилия пассажира', 'TEXT', true, 'IVANOV', 'Латиницей, как в бронировании', NULL, '[A-Za-z -]{2,40}', 'PAY', NULL, 20),
    ('itm-sever', 'order', 'Номер заказа', 'DIGITS', true, 'Номер заказа', '12 цифр', NULL, '\d{12}', 'PAY', NULL, 10),
    ('itm-sever', 'phone', 'Номер телефона', 'PHONE', true, '+7', '10 цифр', NULL, NULL, 'PAY', 'USER_PHONE', 20),
    ('appeks', 'contract', 'Номер договора', 'TEXT', true, 'А-123456', 'Буква А и 6 цифр', NULL, '[АAаa]-?\d{6}', 'PAY', NULL, 10),
    ('appeks', 'payerName', 'ФИО плательщика', 'TEXT', true, 'ФИО плательщика', NULL, NULL, '[А-Яа-яЁё -]{5,150}', 'PAY', 'USER_FULL_NAME', 20),
    ('alem-tour', 'claim', 'Номер заявки', 'DIGITS', true, 'Номер заявки', '8 цифр', NULL, '\d{8}', 'PAY', NULL, 10),
    ('alem-tour', 'email', 'Email для подтверждения', 'EMAIL', true, 'mail@example.ru', 'Сюда придут документы на тур', NULL, NULL, 'PAY', NULL, 20),
    ('aerotour', 'order', 'Номер заказа', 'DIGITS', true, 'Номер заказа', '12 цифр', NULL, '\d{12}', 'PAY', NULL, 10),
    ('aerotour', 'phone', 'Номер телефона', 'PHONE', true, '+7', '10 цифр', NULL, NULL, 'PAY', 'USER_PHONE', 20),
    ('breezel', 'account', 'Номер счета', 'TEXT', true, 'Номер счета', '10 символов', 'Номер счёта — в письме от Бриз Лайн', '[A-Za-z0-9]{10}', 'PAY', NULL, 10),
    ('breezel', 'phone', 'Номер телефона', 'PHONE', true, '+7', '10 цифр', 'Номер, указанный при бронировании', NULL, 'PAY', 'USER_PHONE', 20),
    ('breezel', 'lastName', 'Фамилия плательщика', 'TEXT', true, 'Фамилия плательщика', 'ФИО', NULL, '[А-Яа-яЁё-]{2,50}', 'PAY', NULL, 30),
    ('breezel', 'firstName', 'Имя плательщика', 'TEXT', true, 'Имя плательщика', 'Имя', NULL, '[А-Яа-яЁё-]{2,50}', 'PAY', NULL, 40),
    ('breezel', 'middleName', 'Отчество плательщика', 'TEXT', false, 'Отчество плательщика', 'Отчество', NULL, '[А-Яа-яЁё-]{2,50}', 'PAY', NULL, 50),

    ('vsk-invoice', 'invoice', 'Номер выставленного счета', 'TEXT', true, 'Номер выставленного счета', 'до 25 символов', NULL, '[A-Za-zА-Яа-я0-9/-]{1,25}', 'PAY', NULL, 10),
    ('vsk-invoice', 'senderName', 'ФИО отправителя', 'TEXT', true, 'ФИО отправителя', NULL, NULL, '[А-Яа-яЁё -]{5,150}', 'PAY', 'USER_FULL_NAME', 20),
    ('vsk-osago', 'series', 'Серия полиса', 'CHOICE', true, NULL, NULL, NULL, NULL, 'PAY', NULL, 10),
    ('vsk-osago', 'policy', 'Номер полиса ОСАГО', 'DIGITS', true, 'Номер полиса', '10 цифр', 'Серия и номер — в правом верхнем углу полиса', '\d{10}', 'PAY', NULL, 20),
    ('vsk-osago', 'phone', 'Номер телефона', 'PHONE', true, '+7', 'Пришлём новый полис в СМС', NULL, NULL, 'PAY', 'USER_PHONE', 30),
    ('hc-insurance', 'policy', 'Номер страхового полиса', 'DIGITS', true, 'Номер страхового полиса', '10 цифр', NULL, '\d{10}', 'PAY', NULL, 10),
    ('hc-credit', 'contract', 'Номер кредитного договора', 'DIGITS', true, 'Номер кредитного договора', '10 цифр', 'Полис привязан к кредитному договору', '\d{10}', 'PAY', NULL, 10),
    ('hc-credit', 'payerName', 'ФИО заёмщика', 'TEXT', true, 'ФИО заёмщика', NULL, NULL, '[А-Яа-яЁё -]{5,150}', 'PAY', 'USER_FULL_NAME', 20),
    ('hc-accident', 'policy', 'Номер страхового полиса', 'DIGITS', true, 'Номер страхового полиса', '10 цифр', NULL, '\d{10}', 'PAY', NULL, 10),
    ('hc-accident', 'phone', 'Телефон застрахованного', 'PHONE', true, '+7', NULL, NULL, NULL, 'PAY', 'USER_PHONE', 20),
    ('hc-property', 'policy', 'Номер страхового полиса', 'DIGITS', true, 'Номер страхового полиса', '10 цифр', NULL, '\d{10}', 'PAY', NULL, 10)
) AS f (code, key, label, type, required, placeholder, hint, info, pattern, stage, prefill, sort_order) ON f.code = p.code;

INSERT INTO provider_field_options (field_id, value, label, country_code, sort_order)
SELECT pf.id, o.value, o.label, NULL, o.sort_order
FROM provider_fields pf JOIN providers p ON p.id = pf.provider_id JOIN (VALUES
    ('moskvenok', 'balance', 'MEALS', 'Горячее питание', 10), ('moskvenok', 'balance', 'BUFFET', 'Буфет', 20),
    ('gymnasium-podolsk', 'purpose', 'MEALS', 'Питание', 10), ('gymnasium-podolsk', 'purpose', 'CLUBS', 'Кружки и секции', 20),
    ('gymnasium-podolsk', 'purpose', 'DONATION', 'Добровольное пожертвование', 30),
    ('vsk-osago', 'series', 'XXX', 'ХХХ', 10), ('vsk-osago', 'series', 'TTT', 'ТТТ', 20), ('vsk-osago', 'series', 'AAK', 'ААК', 30)
) AS o (code, key, value, label, sort_order) ON o.code = p.code AND o.key = pf.key;
