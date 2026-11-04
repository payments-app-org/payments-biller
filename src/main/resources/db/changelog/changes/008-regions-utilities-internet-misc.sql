--liquibase formatted sql

--changeset biller:008-regions-utilities-internet-misc
-- Регионы: поставщик с привязкой к региону показываем только там; без привязки — федеральный, виден везде.
CREATE TABLE regions (
    code              VARCHAR(20)  PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    sort_order        INTEGER      NOT NULL
);

INSERT INTO regions (code, name, sort_order) VALUES
    ('moscow',      'Москва и Московская область',                 10),
    ('spb',         'Санкт-Петербург и Ленинградская область',     20),
    ('tatarstan',   'Республика Татарстан',                        30),
    ('sverdlovsk',  'Свердловская область',                        40),
    ('novosibirsk', 'Новосибирская область',                       50),
    ('krasnodar',   'Краснодарский край',                          60);

CREATE TABLE provider_regions (
    provider_id       INTEGER      NOT NULL REFERENCES providers (id),
    region_code       VARCHAR(20)  NOT NULL REFERENCES regions (code),
    PRIMARY KEY (provider_id, region_code)
);

-- Поле может быть только для поиска счетов (SEARCH), только для оплаты (PAY) или для обоих режимов (BOTH):
-- у ЖКХ Москвы код плательщика нужен и в «Найти», и в «Оплатить»
ALTER TABLE provider_fields DROP CONSTRAINT provider_fields_stage_check;
ALTER TABLE provider_fields ADD CONSTRAINT provider_fields_stage_check CHECK (stage IN ('SEARCH', 'PAY', 'BOTH'));

INSERT INTO provider_groups (code, name, sort_order) VALUES ('MISC', 'Разное', 130);

-- Группа «Услуги ЖКХ»: вид поставщика, можно ли платить без поиска счёта, пояснение над формой
CREATE TABLE utility_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    kind              VARCHAR(20)  NOT NULL CHECK (kind IN ('HOUSING', 'ELECTRICITY', 'GAS', 'WATER', 'HEATING', 'AGGREGATOR')),
    direct_pay        BOOLEAN      NOT NULL DEFAULT false,
    intro             VARCHAR(255)
);

-- Группа «Интернет и ТВ, Телефония»
CREATE TABLE internet_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    kind              VARCHAR(20)  NOT NULL CHECK (kind IN ('INTERNET', 'HOME_PHONE', 'CABLE_TV', 'SATELLITE_TV'))
);

-- Группа «Разное»: чьи это услуги — банка или партнёра
CREATE TABLE misc_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    owner             VARCHAR(10)  NOT NULL CHECK (owner IN ('BANK', 'PARTNER'))
);

INSERT INTO providers (group_code, code, name, title, legal_name, domain, integration, color, sort_order, min_amount, max_amount) VALUES
    ('UTILITIES', 'zhkh-moscow',      'ЖКХ Москвы',                    'ЖКХ Москвы',                    'ГБУ «Многофункциональные центры» — ЕПД', 'mos.ru',  'UTILITIES_GATE', '#e4352b', 10, 10, 300000),
    ('UTILITIES', 'gis-zhkh',         'ГИС ЖКХ',                       'ГИС ЖКХ',                       'Государственная информационная система ЖКХ', 'dom.gosuslugi.ru', 'UTILITIES_GATE', '#2f6fc2', 20, NULL, NULL),
    ('UTILITIES', 'zhkh-mo',          'ЖКХ Московская область',        'ЖКХ Московская область',        'Единый платёжный документ Подмосковья', 'mosreg.ru',  'UTILITIES_GATE', '#c62828', 30, 10, 300000),
    ('UTILITIES', 'mosenergosbyt',    'Мосэнергосбыт',                 'Мосэнергосбыт',                 'ПАО «Мосэнергосбыт»',            'mosenergosbyt.ru',  'UTILITIES_GATE', '#f39200', 40, 10, 300000),
    ('UTILITIES', 'mosoblgaz-odintsovo', 'МОСОБЛГАЗ Запад - Одинцово', 'МОСОБЛГАЗ Запад - Одинцово',    'АО «Мособлгаз», филиал «Запад»', 'mosoblgaz.ru',      'UTILITIES_GATE', '#1d5aa8', 50, 10, 300000),
    ('UTILITIES', 'mosobleirc',       'МосОблЕИРЦ (без страховки)',    'МосОблЕИРЦ (без страховки)',    'ООО «МосОблЕИРЦ»',               'mosobleirc.ru',     'UTILITIES_GATE', '#f26f21', 60, 10, 300000),
    ('UTILITIES', 'kvartplata-24',    'Квартплата 24',                 'Квартплата 24',                 'ООО «Квартплата 24»',            'kvp24.ru',          'UTILITIES_GATE', '#1f8fd6', 70, 10, 300000),
    ('UTILITIES', 'alcon',            'Алкон Менеджмент',              'Алкон Менеджмент',              'ООО «Алкон Менеджмент»',         NULL,                'UTILITIES_GATE', '#2d2f36', 80, 10, 300000),
    ('UTILITIES', 'zhkh-spb',         'ЖКХ Санкт-Петербурга',          'ЖКХ Санкт-Петербурга',          'ВЦКП «Жилищное хозяйство»',      'vckp.ru',           'UTILITIES_GATE', '#1d4f91', 90, 10, 300000),
    ('UTILITIES', 'petroelektrosbyt', 'Петроэлектросбыт',              'Петроэлектросбыт',              'АО «Петербургская сбытовая компания»', 'pesc.ru',     'UTILITIES_GATE', '#e2001a', 100, 10, 300000),
    ('UTILITIES', 'vodokanal-spb',    'Водоканал Санкт-Петербурга',    'Водоканал Санкт-Петербурга',    'ГУП «Водоканал Санкт-Петербурга»', 'vodokanal.spb.ru', 'UTILITIES_GATE', '#0077c8', 110, 10, 300000),
    ('UTILITIES', 'erc-kazan',        'ЕРЦ Казани',                    'ЕРЦ Казани',                    'ООО «Единый расчётный центр»',   NULL,                'UTILITIES_GATE', '#1b9e4b', 120, 10, 300000),
    ('UTILITIES', 'erc-ekb',          'ЕРЦ Екатеринбург',              'ЕРЦ Екатеринбург',              'АО «Расчётный центр Урала»',     'ercekb.ru',         'UTILITIES_GATE', '#2a5caa', 130, 10, 300000),

    ('INTERNET',  'mts-home',         'Интернет, ТВ и Телефония МТС',  'Интернет, ТВ и Телефония МТС',  'МТС ПАО',                        'mts.ru',            'INTERNET_GATE',  '#e30611', 10, 10, 100000),
    ('INTERNET',  'mgts',             'МГТС',                          'МГТС',                          'ПАО «МГТС»',                     'mgts.ru',           'INTERNET_GATE',  '#e30611', 20, 10, 100000),
    ('INTERNET',  'mts-satellite',    'Спутниковое ТВ МТС',            'Спутниковое ТВ МТС',            'МТС ПАО',                        'mts.ru',            'INTERNET_GATE',  '#2d2f36', 30, 10, 100000),
    ('INTERNET',  'mts-satellite-iptv', 'Интерактивное спутник. ТВ МТС', 'Интерактивное спутниковое ТВ МТС', 'МТС ПАО',                  'mts.ru',            'INTERNET_GATE',  '#e30611', 40, 10, 100000),
    ('INTERNET',  'rostelecom',       'Ростелеком',                    'Ростелеком',                    'ПАО «Ростелеком»',               'rt.ru',             'INTERNET_GATE',  '#7700ff', 50, 10, 100000),
    ('INTERNET',  'beeline-home',     'билайн - Интернет, ТВ, Телефония', 'билайн - Интернет, ТВ, Телефония', 'ВымпелКом ПАО',            'beeline.ru',        'INTERNET_GATE',  '#ffc800', 60, 10, 100000),
    ('INTERNET',  'tricolor',         'Триколор ТВ',                   'Триколор ТВ',                   'ООО «Триколор»',                 'tricolor.ru',       'INTERNET_GATE',  '#1d4f91', 70, 10, 100000),
    ('INTERNET',  'telekarta',        'Телекарта',                     'Телекарта',                     'ООО «Орион»',                    'telekarta.tv',      'INTERNET_GATE',  '#2d2f36', 80, 10, 100000),

    ('MISC',      'biglion',          'Биглион',                       'Биглион',                       'ООО «Биглион»',                  'biglion.ru',        'MISC_GATE',      '#f26f21', 10, 10, 100000),
    ('MISC',      'bank-dismissal-debt', 'Возврат долга при увольнении', 'Возврат долга при увольнении', 'Банк Pay — услуги банка',        NULL,                'MISC_GATE',      '#e30611', 20, 10, 1000000),
    ('MISC',      'bank-info-services', 'Оплата за информационно-консультационные услуги', 'Оплата за информационно-консультационные услуги', 'Банк Pay — услуги банка', NULL, 'MISC_GATE', '#e30611', 30, 10, 1000000),
    ('MISC',      'bank-foreign-card', 'Комиссия за выпуск зарубежной карты', 'Комиссия за выпуск зарубежной карты', 'Банк Pay — услуги банка', NULL,           'MISC_GATE',      '#1a1f71', 40, 1000, 100000);

INSERT INTO utility_providers (provider_id, kind, direct_pay, intro)
SELECT p.id, u.kind, u.direct, u.intro FROM providers p JOIN (VALUES
    ('zhkh-moscow',         'HOUSING',     true,  'Найдите начисления по коду плательщика из ЕПД или оплатите произвольную сумму'),
    ('gis-zhkh',            'AGGREGATOR',  false, 'Здесь вы можете совершить оплату по номеру Единого лицевого счета (ЕЛС)'),
    ('zhkh-mo',             'HOUSING',     true,  'Начисления по лицевому счёту из единого платёжного документа Подмосковья'),
    ('mosenergosbyt',       'ELECTRICITY', false, NULL),
    ('mosoblgaz-odintsovo', 'GAS',         false, NULL),
    ('mosobleirc',          'HOUSING',     false, NULL),
    ('kvartplata-24',       'HOUSING',     false, NULL),
    ('alcon',               'HOUSING',     false, NULL),
    ('zhkh-spb',            'HOUSING',     true,  'Начисления ВЦКП по номеру лицевого счёта'),
    ('petroelektrosbyt',    'ELECTRICITY', false, NULL),
    ('vodokanal-spb',       'WATER',       false, NULL),
    ('erc-kazan',           'HOUSING',     false, NULL),
    ('erc-ekb',             'HOUSING',     false, NULL)
) AS u (code, kind, direct, intro) ON u.code = p.code;

INSERT INTO internet_providers (provider_id, kind)
SELECT p.id, i.kind FROM providers p JOIN (VALUES
    ('mts-home', 'INTERNET'), ('mgts', 'HOME_PHONE'), ('mts-satellite', 'SATELLITE_TV'), ('mts-satellite-iptv', 'SATELLITE_TV'),
    ('rostelecom', 'INTERNET'), ('beeline-home', 'INTERNET'), ('tricolor', 'SATELLITE_TV'), ('telekarta', 'SATELLITE_TV')
) AS i (code, kind) ON i.code = p.code;

INSERT INTO misc_providers (provider_id, owner)
SELECT p.id, m.owner FROM providers p JOIN (VALUES
    ('biglion', 'PARTNER'), ('bank-dismissal-debt', 'BANK'), ('bank-info-services', 'BANK'), ('bank-foreign-card', 'BANK')
) AS m (code, owner) ON m.code = p.code;

INSERT INTO provider_fields (provider_id, key, label, type, required, placeholder, hint, info, pattern, stage, prefill, sort_order)
SELECT p.id, f.key, f.label, f.type, f.required, f.placeholder, f.hint, f.info, f.pattern, f.stage, f.prefill, f.sort_order
FROM providers p JOIN (VALUES
    ('zhkh-moscow', 'payerName', 'ФИО плательщика', 'TEXT', true, 'ФИО плательщика', NULL, NULL, '[А-Яа-яЁё -]{5,150}', 'SEARCH', 'USER_FULL_NAME', 10),
    ('zhkh-moscow', 'payerCode', 'Код плательщика', 'DIGITS', true, 'Код плательщика', '10 цифр', 'Код плательщика — в верхней части ЕПД', '\d{10}', 'BOTH', NULL, 20),
    ('gis-zhkh', 'els', 'Единый лицевой счет (ЕЛС)', 'TEXT', true, 'Единый лицевой счет (ЕЛС)', '10 знаков, например 60ЫЩ123456',
        'ЕЛС присваивается в ГИС ЖКХ и указан в платёжном документе. Как получить ЕЛС — в личном кабинете dom.gosuslugi.ru', '\d{2}[А-Яа-яЁё]{2}\d{6}', 'SEARCH', NULL, 10),
    ('zhkh-mo', 'account', 'Лицевой счёт', 'DIGITS', true, 'Лицевой счёт', '10 цифр', 'Номер лицевого счёта — в ЕПД Подмосковья', '\d{10}', 'BOTH', NULL, 10),
    ('mosenergosbyt', 'account', 'Номер лицевого счёта', 'DIGITS', true, 'Номер лицевого счёта', '10 цифр', NULL, '\d{10}', 'PAY', NULL, 10),
    ('mosenergosbyt', 'meter', 'Показания счётчика', 'DIGITS', false, 'Показания, кВт·ч', 'Необязательно — передадим вместе с оплатой', NULL, '\d{1,7}', 'PAY', NULL, 20),
    ('mosoblgaz-odintsovo', 'account', 'Номер лицевого счёта', 'DIGITS', true, 'Номер лицевого счёта', '12 цифр', NULL, '\d{12}', 'PAY', NULL, 10),
    ('mosobleirc', 'account', 'Номер лицевого счёта', 'DIGITS', true, 'Номер лицевого счёта', '10 цифр', NULL, '\d{10}', 'PAY', NULL, 10),
    ('mosobleirc', 'period', 'Период оплаты', 'MONTH', true, NULL, 'Месяц', NULL, NULL, 'PAY', NULL, 20),
    ('kvartplata-24', 'account', 'Лицевой счёт', 'DIGITS', true, 'Лицевой счёт', 'от 6 до 12 цифр', NULL, '\d{6,12}', 'PAY', NULL, 10),
    ('kvartplata-24', 'email', 'Email для чека', 'EMAIL', false, 'Email для чека', NULL, NULL, NULL, 'PAY', NULL, 20),
    ('alcon', 'account', 'Лицевой счёт', 'DIGITS', true, 'Лицевой счёт', '8 цифр', NULL, '\d{8}', 'PAY', NULL, 10),
    ('alcon', 'address', 'Адрес', 'TEXT', true, 'Улица, дом, квартира', NULL, NULL, '.{5,200}', 'PAY', NULL, 20),
    ('zhkh-spb', 'account', 'Номер лицевого счёта', 'DIGITS', true, 'Номер лицевого счёта', '9 цифр', 'Номер лицевого счёта — в квитанции ВЦКП', '\d{9}', 'BOTH', NULL, 10),
    ('petroelektrosbyt', 'account', 'Номер договора', 'DIGITS', true, 'Номер договора', '12 цифр', NULL, '\d{12}', 'PAY', NULL, 10),
    ('petroelektrosbyt', 'meter', 'Показания счётчика', 'DIGITS', false, 'Показания, кВт·ч', 'Необязательно', NULL, '\d{1,7}', 'PAY', NULL, 20),
    ('vodokanal-spb', 'account', 'Лицевой счёт', 'DIGITS', true, 'Лицевой счёт', '7 цифр', NULL, '\d{7}', 'PAY', NULL, 10),
    ('erc-kazan', 'account', 'Лицевой счёт', 'DIGITS', true, 'Лицевой счёт', '10 цифр', NULL, '\d{10}', 'PAY', NULL, 10),
    ('erc-ekb', 'account', 'Лицевой счёт', 'DIGITS', true, 'Лицевой счёт', '10 цифр', NULL, '\d{10}', 'PAY', NULL, 10),
    ('erc-ekb', 'period', 'Период оплаты', 'MONTH', true, NULL, 'Месяц', NULL, NULL, 'PAY', NULL, 20),

    ('mts-home', 'account', 'Номер лицевого счёта', 'DIGITS', true, 'Номер лицевого счёта', '12 цифр', NULL, '\d{12}', 'PAY', NULL, 10),
    ('mgts', 'account', 'Номер телефона/Счета', 'DIGITS', true, 'Номер телефона/Счета', 'Телефон — 10 цифр. Счёт — 11 цифр', NULL, '\d{10,11}', 'PAY', NULL, 10),
    ('mts-satellite', 'account', 'Номер договора', 'DIGITS', true, 'Номер договора', '12 цифр', NULL, '\d{12}', 'PAY', NULL, 10),
    ('mts-satellite-iptv', 'account', 'Номер договора', 'DIGITS', true, 'Номер договора', '12 цифр', NULL, '\d{12}', 'PAY', NULL, 10),
    ('rostelecom', 'account', 'Номер телефона/Счета', 'DIGITS', true, 'Номер телефона/Счета', 'Телефон - 10 цифр. Счет от 5 до 12 цифр', NULL, '\d{5,12}', 'PAY', NULL, 10),
    ('beeline-home', 'account', 'Номер договора', 'DIGITS', true, 'Номер договора', '10 цифр, начинается с 08', NULL, '08\d{8}', 'PAY', NULL, 10),
    ('tricolor', 'receiver', 'ID приёмника', 'DIGITS', true, 'ID приёмника', '12 или 14 цифр', 'ID — в меню приёмника или на наклейке', '\d{12}|\d{14}', 'PAY', NULL, 10),
    ('telekarta', 'contract', 'Номер договора', 'DIGITS', true, 'Номер договора', '10 цифр', NULL, '\d{10}', 'PAY', NULL, 10),

    ('biglion', 'order', 'Номер заказа или номер телефона', 'DIGITS', true, 'Номер заказа или номер телефона', 'от 8 до 10 цифр', NULL, '\d{8,10}', 'PAY', NULL, 10),
    ('bank-dismissal-debt', 'phone', 'Номер телефона', 'PHONE', true, '+7', '10 цифр', NULL, NULL, 'PAY', 'USER_PHONE', 10),
    ('bank-info-services', 'phone', 'Телефон отправителя', 'PHONE', true, '+7', '10 цифр', NULL, NULL, 'PAY', 'USER_PHONE', 10),
    ('bank-foreign-card', 'phone', 'Номер телефона', 'PHONE', true, '+7', 'Оплата производится после консультации с менеджером cards@paybank.ru',
        'Номер, который вы указали менеджеру при заказе карты', NULL, 'PAY', 'USER_PHONE', 10),
    ('bank-foreign-card', 'lastName', 'Фамилия заказчика карты', 'TEXT', true, 'Фамилия заказчика карты', NULL, NULL, '[А-Яа-яЁё-]{2,50}', 'PAY', NULL, 20),
    ('bank-foreign-card', 'firstName', 'Имя заказчика карты', 'TEXT', true, 'Имя заказчика карты', NULL, NULL, '[А-Яа-яЁё-]{2,50}', 'PAY', NULL, 30),
    ('bank-foreign-card', 'middleName', 'Отчество заказчика карты', 'TEXT', false, 'Отчество заказчика карты', NULL, NULL, '[А-Яа-яЁё-]{2,50}', 'PAY', NULL, 40)
) AS f (code, key, label, type, required, placeholder, hint, info, pattern, stage, prefill, sort_order) ON f.code = p.code;

-- Привязка к регионам: только региональные поставщики, федеральные (ГИС ЖКХ, Ростелеком, игры…) — без привязки
INSERT INTO provider_regions (provider_id, region_code)
SELECT p.id, r.region FROM providers p JOIN (VALUES
    ('zhkh-moscow', 'moscow'), ('zhkh-mo', 'moscow'), ('mosenergosbyt', 'moscow'), ('mosoblgaz-odintsovo', 'moscow'),
    ('mosobleirc', 'moscow'), ('kvartplata-24', 'moscow'), ('alcon', 'moscow'),
    ('zhkh-spb', 'spb'), ('petroelektrosbyt', 'spb'), ('vodokanal-spb', 'spb'),
    ('erc-kazan', 'tatarstan'), ('erc-ekb', 'sverdlovsk'),
    ('mgts', 'moscow'),
    ('troika', 'moscow'), ('strelka', 'moscow'), ('podorozhnik', 'spb'),
    ('parking-russia', 'moscow'), ('parking-russia', 'spb'),
    ('edu-moscow', 'moscow'), ('moskvenok', 'moscow'), ('gymnasium-podolsk', 'moscow'), ('lyceum-19-korolev', 'moscow'), ('mshp', 'moscow'),
    ('bumerang', 'moscow'), ('autoconnex-spb', 'spb'), ('megapage', 'moscow'), ('golfstream', 'moscow'),
    ('delta', 'moscow'), ('delta-moscow-1', 'moscow'), ('delta-moscow-2', 'moscow'), ('delta-moscow-3', 'moscow')
) AS r (code, region) ON r.code = p.code;
