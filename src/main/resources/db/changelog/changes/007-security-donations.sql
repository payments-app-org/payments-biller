--liquibase formatted sql

--changeset biller:007-security-donations
-- Охранные системы (номер договора / устройства и сумма) и пожертвования (ФИО отправителя и сумма)

INSERT INTO provider_groups (code, name, sort_order) VALUES
    ('SECURITY',  'Охранные системы', 110),
    ('DONATIONS', 'Пожертвования',    120);

-- Группа «Охранные системы»: что охраняет поставщик и где работает
CREATE TABLE security_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    kind              VARCHAR(20)  NOT NULL CHECK (kind IN ('CAR_ALARM', 'GPS_TRACKING', 'PROPERTY_GUARD')),
    region            VARCHAR(100) NOT NULL
);

-- Группа «Пожертвования»: фонд, ИНН и направление помощи
CREATE TABLE charity_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    inn               VARCHAR(12)  NOT NULL,
    cause             VARCHAR(20)  NOT NULL CHECK (cause IN ('CHILDREN', 'FOOD', 'HEALTH', 'ECOLOGY', 'SOCIAL'))
);

INSERT INTO providers (group_code, code, name, title, legal_name, domain, integration, color, sort_order, min_amount, max_amount) VALUES
    ('SECURITY', 'bumerang',          'BUMERANG',                        'BUMERANG',                        'Спутниковая охрана автомобилей', NULL,             'SECURITY_GATE', '#1e2a5a', 10, 100, 100000),
    ('SECURITY', 'autoconnex-spb',    'Автоконнекс (Санкт-Петербург)',   'Автоконнекс (Санкт-Петербург)',   'ООО «Автоконнекс»',              'autoconnex.ru',  'SECURITY_GATE', '#e0262b', 20, 100, 100000),
    ('SECURITY', 'megapage',          'Автолокатор Мегапейдж',           'Автолокатор Мегапейдж',           'ООО «Мегапейдж»',                'megapage.ru',    'SECURITY_GATE', '#1d8bd1', 30, 100, 100000),
    ('SECURITY', 'golfstream',        'ГОЛЬФСТРИМ',                      'ГОЛЬФСТРИМ',                      'ООО «Гольфстрим Охранные системы»', 'golfstream.ru', 'SECURITY_GATE', '#1f3f8f', 40, 100, 300000),
    ('SECURITY', 'delta',             'Дельта',                          'Дельта',                          'ООО «Дельта»',                   'delta.ru',       'SECURITY_GATE', '#3ba6ea', 50, 100, 300000),
    ('SECURITY', 'delta-moscow-1',    'Дельта (Дельта-Москва 1)',        'Дельта (Дельта-Москва 1)',        'ООО «Дельта-Москва 1»',          'delta.ru',       'SECURITY_GATE', '#3ba6ea', 60, 100, 300000),
    ('SECURITY', 'delta-moscow-2',    'Дельта (Дельта-Москва 2)',        'Дельта (Дельта-Москва 2)',        'ООО «Дельта-Москва 2»',          'delta.ru',       'SECURITY_GATE', '#3ba6ea', 70, 100, 300000),
    ('SECURITY', 'delta-moscow-3',    'Дельта (Дельта-Москва 3)',        'Дельта (Дельта-Москва 3)',        'ООО «Дельта-Москва 3»',          'delta.ru',       'SECURITY_GATE', '#3ba6ea', 80, 100, 300000),

    ('DONATIONS', 'volunteers-orphans', 'Волонтеры в помощь детям-сиротам', 'Волонтеры в помощь детям-сиротам', 'БФ «Волонтёры в помощь детям-сиротам»', 'otkazniki.ru', 'CHARITY_GATE', '#7cc242', 10, 10, 600000),
    ('DONATIONS', 'dari-edu',           'Дари еду',                      'Дари еду',                        'БФ «Дари еду»',                  'darieda.ru',     'CHARITY_GATE', '#2d2d2d', 20, 10, 600000),
    ('DONATIONS', 'onkologika',         'Онкологика',                    'Онкологика',                      'БФ «Онкологика»',                'oncologika.ru',  'CHARITY_GATE', '#ff5a1f', 30, 10, 600000),
    ('DONATIONS', 'eco-12-15',          'Центр чистой природы 12-15',    'Центр чистой природы 12-15',      'АНО «Центр чистой природы 12-15»', NULL,           'CHARITY_GATE', '#3fa34d', 40, 10, 600000),
    ('DONATIONS', 'sistema-fund',       'Благотворительный фонд Система', 'Благотворительный фонд Система', 'БФ «Система»',                   'bf.sistema.ru',  'CHARITY_GATE', '#1d4fa0', 50, 10, 600000),
    ('DONATIONS', 'big-brothers',       'МОО Старшие Братья Старшие Сестры', 'МОО Старшие Братья Старшие Сестры', 'МОО «Старшие Братья Старшие Сёстры»', 'nastavniki.org', 'CHARITY_GATE', '#f26f21', 60, 10, 600000),
    ('DONATIONS', 'children-hearts',    'Детские сердца',                'Детские сердца',                  'БФ «Детские сердца»',            'detskieserdca.ru', 'CHARITY_GATE', '#c62828', 70, 10, 600000),
    ('DONATIONS', 'perspektiva',        'Перспектива',                   'Перспектива',                     'РООИ «Перспектива»',             'perspektiva-inva.ru', 'CHARITY_GATE', '#1d5aa8', 80, 10, 600000);

INSERT INTO security_providers (provider_id, kind, region)
SELECT p.id, s.kind, s.region FROM providers p JOIN (VALUES
    ('bumerang', 'CAR_ALARM', 'Москва и Московская область'), ('autoconnex-spb', 'CAR_ALARM', 'Санкт-Петербург'),
    ('megapage', 'GPS_TRACKING', 'Москва и Московская область'), ('golfstream', 'PROPERTY_GUARD', 'Москва и Московская область'),
    ('delta', 'PROPERTY_GUARD', 'Москва'), ('delta-moscow-1', 'PROPERTY_GUARD', 'Москва'),
    ('delta-moscow-2', 'PROPERTY_GUARD', 'Москва'), ('delta-moscow-3', 'PROPERTY_GUARD', 'Москва')
) AS s (code, kind, region) ON s.code = p.code;

INSERT INTO charity_providers (provider_id, inn, cause)
SELECT p.id, c.inn, c.cause FROM providers p JOIN (VALUES
    ('volunteers-orphans', '7714099750', 'CHILDREN'), ('dari-edu', '7751161940', 'FOOD'), ('onkologika', '7704453478', 'HEALTH'),
    ('eco-12-15', '7720476316', 'ECOLOGY'), ('sistema-fund', '7702338735', 'SOCIAL'), ('big-brothers', '7701374049', 'CHILDREN'),
    ('children-hearts', '7743157080', 'HEALTH'), ('perspektiva', '7709140524', 'SOCIAL')
) AS c (code, inn, cause) ON c.code = p.code;

-- Охрана: номер договора или устройства, у каждого поставщика своей длины
INSERT INTO provider_fields (provider_id, key, label, type, required, placeholder, hint, info, pattern, stage, prefill, sort_order)
SELECT p.id, 'number', 'Номер', 'DIGITS', true, 'Номер', f.hint, f.info, f.pattern, 'PAY', NULL, 10
FROM providers p JOIN (VALUES
    ('bumerang',       '6 цифр',       'Номер договора — в личном кабинете BUMERANG', '\d{6}'),
    ('autoconnex-spb', '7 цифр',       'Номер договора с Автоконнекс',               '\d{7}'),
    ('megapage',       '8 цифр',       'Номер автолокатора — на корпусе устройства',  '\d{8}'),
    ('golfstream',     'от 5 до 8 цифр', 'Номер договора охраны',                     '\d{5,8}'),
    ('delta',          '10 цифр',      'Номер лицевого счёта из квитанции Дельты',    '\d{10}'),
    ('delta-moscow-1', '10 цифр',      'Номер лицевого счёта из квитанции Дельты',    '\d{10}'),
    ('delta-moscow-2', '10 цифр',      'Номер лицевого счёта из квитанции Дельты',    '\d{10}'),
    ('delta-moscow-3', '10 цифр',      'Номер лицевого счёта из квитанции Дельты',    '\d{10}')
) AS f (code, hint, info, pattern) ON f.code = p.code;

-- Пожертвования: ФИО отправителя (подставляем из профиля)
INSERT INTO provider_fields (provider_id, key, label, type, required, placeholder, hint, info, pattern, stage, prefill, sort_order)
SELECT p.id, 'senderName', 'ФИО отправителя', 'TEXT', true, 'ФИО отправителя', NULL, NULL, '[А-Яа-яЁё -]{5,150}', 'PAY', 'USER_FULL_NAME', 10
FROM providers p WHERE p.group_code = 'DONATIONS';
