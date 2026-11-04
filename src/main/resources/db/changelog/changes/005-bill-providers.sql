--liquibase formatted sql

--changeset biller:005-bill-providers
-- Транспорт, игры и сервисы, зарубежная сотовая связь. У каждого поставщика свой набор полей (provider_fields),
-- у группы — своя таблица особенностей. Оплаты у поставщиков — в biller_payments.

INSERT INTO provider_groups (code, name, sort_order) VALUES
    ('TRANSPORT',      'Транспорт',            35),
    ('FOREIGN_MOBILE', 'Зарубежные операторы', 45);

-- Связь платим через шлюз операторов (заглушка в providers), а не через процессинг
UPDATE providers SET integration = 'OPERATOR_GATE' WHERE group_code = 'MOBILE';

-- Страна поставщика — флаг вместо логотипа; границы суммы одного платежа у поставщика
ALTER TABLE providers ADD COLUMN country_code VARCHAR(2);
ALTER TABLE providers ADD COLUMN min_amount   NUMERIC(19, 2);
ALTER TABLE providers ADD COLUMN max_amount   NUMERIC(19, 2);

-- Поля формы оплаты поставщику.
-- TEXT, DIGITS — по pattern; EMAIL; PHONE — российский +7; LOCAL_PHONE — номер страны из foreign_mobile_providers;
-- CHOICE — выбор из provider_field_options; NOMINAL — номинал из game_nominals (depends_on — поле с регионом).
CREATE TABLE provider_fields (
    id                SERIAL       PRIMARY KEY,
    provider_id       INTEGER      NOT NULL REFERENCES providers (id),
    key               VARCHAR(30)  NOT NULL,
    label             VARCHAR(100) NOT NULL,
    type              VARCHAR(12)  NOT NULL CHECK (type IN ('TEXT', 'DIGITS', 'EMAIL', 'PHONE', 'LOCAL_PHONE', 'CHOICE', 'NOMINAL')),
    required          BOOLEAN      NOT NULL DEFAULT true,
    placeholder       VARCHAR(100),
    -- Подсказка под полем и пояснение по значку «i»
    hint              VARCHAR(255),
    info              VARCHAR(255),
    pattern           VARCHAR(100),
    depends_on        VARCHAR(30),
    sort_order        INTEGER      NOT NULL,
    UNIQUE (provider_id, key)
);

CREATE TABLE provider_field_options (
    id                SERIAL       PRIMARY KEY,
    field_id          INTEGER      NOT NULL REFERENCES provider_fields (id),
    value             VARCHAR(30)  NOT NULL,
    label             VARCHAR(100) NOT NULL,
    country_code      VARCHAR(2),
    sort_order        INTEGER      NOT NULL,
    UNIQUE (field_id, value)
);

-- Группа «Транспорт»: что пополняем и в каком регионе
CREATE TABLE transport_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    kind              VARCHAR(20)  NOT NULL CHECK (kind IN ('TRANSPORT_CARD', 'PARKING', 'TOLL_ROAD', 'TAXI', 'TICKETS')),
    region            VARCHAR(100) NOT NULL
);

-- Группа «Игры и сервисы»: пополнение аккаунта, ваучер на почту, подписка или игровая валюта
CREATE TABLE game_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    kind              VARCHAR(20)  NOT NULL CHECK (kind IN ('ACCOUNT_TOPUP', 'VOUCHER', 'SUBSCRIPTION', 'IN_GAME_CURRENCY')),
    platform          VARCHAR(30),
    -- Что и когда получит покупатель: «Отправим ваучер в течение 15 минут после оплаты»
    delivery_note     VARCHAR(255)
);

-- Номиналы с фиксированной ценой в рублях (уже с комиссией поставщика). region — значение поля depends_on.
CREATE TABLE game_nominals (
    id                SERIAL         PRIMARY KEY,
    provider_id       INTEGER        NOT NULL REFERENCES game_providers (provider_id),
    region            VARCHAR(10),
    name              VARCHAR(100)   NOT NULL,
    price             NUMERIC(19, 2) NOT NULL CHECK (price > 0),
    sort_order        INTEGER        NOT NULL
);

-- Группа «Зарубежные операторы»: формат номера страны
CREATE TABLE foreign_mobile_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    dial_code         VARCHAR(4)   NOT NULL,
    number_length     INTEGER      NOT NULL,
    -- С чего может начинаться номер (через запятую); пусто — с чего угодно
    prefixes          VARCHAR(100),
    operators         VARCHAR(255) NOT NULL,
    currency          VARCHAR(3)   NOT NULL
);

-- Оплата у поставщика: одна запись на платёж payment, ответ шлюза — принят или отклонён
CREATE TABLE biller_payments (
    payment_id        UUID           PRIMARY KEY,
    provider_id       INTEGER        NOT NULL REFERENCES providers (id),
    amount            NUMERIC(19, 2) NOT NULL,
    fields            JSONB          NOT NULL,
    status            VARCHAR(10)    NOT NULL CHECK (status IN ('ACCEPTED', 'REJECTED')),
    external_id       VARCHAR(60),
    voucher_code      VARCHAR(60),
    message           VARCHAR(255),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- ===== Транспорт =====
INSERT INTO providers (group_code, code, name, title, legal_name, domain, integration, color, sort_order, min_amount, max_amount) VALUES
    ('TRANSPORT', 'troika',              'Карта Тройка',                   'Карта Тройка',             'ГКУ «Организатор перевозок»',        'troika.mos.ru',      'TRANSPORT_GATE', '#2b7bb9', 10, 10, 3500),
    ('TRANSPORT', 'parking-russia',      'Парковки России (Москва и СПб)', 'Парковки России',          'Парковочный счёт в Москве и СПб',    'parking.mos.ru',     'TRANSPORT_GATE', '#7cc242', 20, 10, 30000),
    ('TRANSPORT', 'strelka',             'Стрелка',                        'Карта Стрелка',            'Транспортная карта Подмосковья',     'strelkacard.ru',     'TRANSPORT_GATE', '#1d4f91', 30, 10, 3000),
    ('TRANSPORT', 'avtodor-account',     'Автодор-лицевой счет',           'Автодор-лицевой счет',     'ГК «Автодор» — платные дороги',      'avtodor-tr.ru',      'TRANSPORT_GATE', '#f26b3a', 40, 100, 50000),
    ('TRANSPORT', 'avtodor-phone',       'Автодор-номер телефона',         'Автодор-номер телефона',   'ГК «Автодор» — платные дороги',      'avtodor-tr.ru',      'TRANSPORT_GATE', '#f26b3a', 50, 100, 50000),
    ('TRANSPORT', 'avtodor-transponder', 'Автодор-транспондер',            'Автодор-транспондер',      'ГК «Автодор» — платные дороги',      'avtodor-tr.ru',      'TRANSPORT_GATE', '#f26b3a', 60, 100, 50000),
    ('TRANSPORT', 'taxi-maxim',          'Такси Максим',                   'Такси Максим',             'Пополнение баланса в приложении',    'taximaxim.ru',       'TRANSPORT_GATE', '#ffd400', 70, 50, 15000),
    ('TRANSPORT', 'kupibilet',           'Kupibilet.ru',                   'Kupibilet.ru',             'Оплата заказа билетов',              'kupibilet.ru',       'TRANSPORT_GATE', '#4fc3f7', 80, 100, 600000),
    ('TRANSPORT', 'podorozhnik',         'Подорожник',                     'Карта Подорожник',         'Транспортная карта Санкт-Петербурга', 'podorozhnik.spb.ru', 'TRANSPORT_GATE', '#2c9a46', 90, 10, 3000);

INSERT INTO transport_providers (provider_id, kind, region)
SELECT p.id, t.kind, t.region FROM providers p JOIN (VALUES
    ('troika',              'TRANSPORT_CARD', 'Москва и Московская область'),
    ('parking-russia',      'PARKING',        'Москва и Санкт-Петербург'),
    ('strelka',             'TRANSPORT_CARD', 'Московская область'),
    ('avtodor-account',     'TOLL_ROAD',      'Россия'),
    ('avtodor-phone',       'TOLL_ROAD',      'Россия'),
    ('avtodor-transponder', 'TOLL_ROAD',      'Россия'),
    ('taxi-maxim',          'TAXI',           'Россия'),
    ('kupibilet',           'TICKETS',        'Россия'),
    ('podorozhnik',         'TRANSPORT_CARD', 'Санкт-Петербург')
) AS t (code, kind, region) ON t.code = p.code;

-- ===== Игры и сервисы =====
INSERT INTO providers (group_code, code, name, title, legal_name, domain, integration, color, sort_order, min_amount, max_amount) VALUES
    ('GAMES', 'chatgpt',        'ChatGPT',                 'ChatGPT',                 'Подписка через партнёра',       'chatgpt.com',               'GAMES_GATE', '#10a37f', 10, NULL, NULL),
    ('GAMES', 'steam',          'Steam',                   'Steam',                   'Пополнение кошелька Steam',     'store.steampowered.com',    'GAMES_GATE', '#1b2838', 20, 100, 15000),
    ('GAMES', 'steam-voucher',  'Steam - покупка ваучера', 'Steam - покупка ваучера', 'Код пополнения кошелька',       'store.steampowered.com',    'GAMES_GATE', '#1b2838', 30, NULL, NULL),
    ('GAMES', 'psn',            'Playstation Network',     'Playstation Network',     'Карты пополнения PSN',          'playstation.com',           'GAMES_GATE', '#0070d1', 40, NULL, NULL),
    ('GAMES', 'xbox',           'Xbox',                    'Xbox',                    'Пополнение аккаунта Microsoft', 'xbox.com',                  'GAMES_GATE', '#107c10', 50, NULL, NULL),
    ('GAMES', 'xbox-game-pass', 'Xbox Game Pass',          'Xbox Game Pass',          'Подписка Game Pass',            'xbox.com',                  'GAMES_GATE', '#0e7a0d', 60, NULL, NULL),
    ('GAMES', 'ea-games',       'EA Games',                'EA Games',                'Пополнение EA App',             'ea.com',                    'GAMES_GATE', '#ff4747', 70, 300, 10000),
    ('GAMES', 'apex-xbox',      'Apex Legends XBOX',       'Apex Legends XBOX',       'Apex Coins для Xbox',           NULL,                        'GAMES_GATE', '#cd3333', 80, NULL, NULL),
    ('GAMES', 'genshin',        'Genshin Impact',          'Genshin Impact',          'Кристаллы Сотворения',          'genshin.hoyoverse.com',     'GAMES_GATE', '#5d6bd6', 90, NULL, NULL),
    ('GAMES', 'pubg-mobile',    'PUBG Mobile',             'PUBG Mobile',             'Unknown Cash (UC)',             'pubgmobile.com',            'GAMES_GATE', '#f2a900', 100, NULL, NULL);

INSERT INTO game_providers (provider_id, kind, platform, delivery_note)
SELECT p.id, g.kind, g.platform, g.note FROM providers p JOIN (VALUES
    ('chatgpt',        'VOUCHER',          'Web',         'Отправим ваучер в течение 15 минут после оплаты'),
    ('steam',          'ACCOUNT_TOPUP',    'PC',          'Деньги поступят на аккаунт в течение 15 минут'),
    ('steam-voucher',  'VOUCHER',          'PC',          'Код ваучера придёт на почту в течение 15 минут'),
    ('psn',            'VOUCHER',          'PlayStation', 'Код придёт на почту. Подходит только для аккаунта выбранного региона'),
    ('xbox',           'ACCOUNT_TOPUP',    'Xbox',        'Баланс аккаунта Microsoft пополним в течение 15 минут'),
    ('xbox-game-pass', 'SUBSCRIPTION',     'Xbox',        'Код активации подписки придёт на почту'),
    ('ea-games',       'ACCOUNT_TOPUP',    'PC',          'Деньги поступят в EA App в течение 30 минут'),
    ('apex-xbox',      'IN_GAME_CURRENCY', 'Xbox',        'Apex Coins поступят на аккаунт в течение часа'),
    ('genshin',        'IN_GAME_CURRENCY', 'Mobile/PC',   'Кристаллы поступят на аккаунт после перезахода в игру'),
    ('pubg-mobile',    'IN_GAME_CURRENCY', 'Mobile',      'UC поступят на аккаунт в течение 15 минут')
) AS g (code, kind, platform, note) ON g.code = p.code;

INSERT INTO game_nominals (provider_id, region, name, price, sort_order)
SELECT p.id, n.region, n.name, n.price, n.sort_order FROM providers p JOIN (VALUES
    ('chatgpt', 'US', 'Plus 1 месяц (ваучер)', 2490, 10), ('chatgpt', 'US', 'Plus 1 месяц', 2390, 20),
    ('chatgpt', 'US', 'Go 1 месяц', 990, 30),             ('chatgpt', 'US', 'Pro 1 месяц', 23900, 40),
    ('chatgpt', 'EU', 'Plus 1 месяц (ваучер)', 2690, 10), ('chatgpt', 'EU', 'Plus 1 месяц', 2590, 20),
    ('chatgpt', 'EU', 'Go 1 месяц', 1090, 30),            ('chatgpt', 'EU', 'Pro 1 месяц', 25900, 40),
    ('steam-voucher', NULL, '5 $', 590, 10), ('steam-voucher', NULL, '10 $', 1150, 20),
    ('steam-voucher', NULL, '20 $', 2250, 30), ('steam-voucher', NULL, '50 $', 5500, 40),
    ('psn', 'TR', '250 TL', 950, 10),  ('psn', 'TR', '500 TL', 1850, 20), ('psn', 'TR', '1000 TL', 3650, 30),
    ('psn', 'IN', '1000 INR', 1350, 10), ('psn', 'IN', '2000 INR', 2650, 20),
    ('psn', 'PL', '50 PLN', 1450, 10), ('psn', 'PL', '100 PLN', 2850, 20),
    ('xbox', 'US', '10 $', 1190, 10), ('xbox', 'US', '25 $', 2890, 20), ('xbox', 'US', '50 $', 5690, 30),
    ('xbox', 'TR', '200 TL', 790, 10), ('xbox', 'TR', '500 TL', 1890, 20),
    ('xbox-game-pass', NULL, 'Core 1 месяц', 790, 10), ('xbox-game-pass', NULL, 'Standard 1 месяц', 1290, 20),
    ('xbox-game-pass', NULL, 'Ultimate 1 месяц', 1990, 30), ('xbox-game-pass', NULL, 'Ultimate 3 месяца', 5490, 40),
    ('apex-xbox', NULL, '1000 Apex Coins', 990, 10), ('apex-xbox', NULL, '2150 Apex Coins', 1990, 20),
    ('apex-xbox', NULL, '4350 Apex Coins', 3890, 30), ('apex-xbox', NULL, '11500 Apex Coins', 9590, 40),
    ('genshin', NULL, '60 кристаллов', 99, 10), ('genshin', NULL, '300 + 30 кристаллов', 490, 20),
    ('genshin', NULL, '980 + 110 кристаллов', 1490, 30), ('genshin', NULL, '1980 + 260 кристаллов', 2990, 40),
    ('genshin', NULL, 'Благословение полой луны', 490, 50),
    ('pubg-mobile', NULL, '60 UC', 99, 10), ('pubg-mobile', NULL, '325 UC', 449, 20),
    ('pubg-mobile', NULL, '660 UC', 899, 30), ('pubg-mobile', NULL, '1800 UC', 2290, 40)
) AS n (code, region, name, price, sort_order) ON n.code = p.code;

-- ===== Зарубежные операторы =====
INSERT INTO providers (group_code, code, name, title, legal_name, domain, country_code, integration, color, sort_order, min_amount, max_amount) VALUES
    ('FOREIGN_MOBILE', 'mobile-ge', 'Сотовая связь Грузии',      'Сотовая связь Грузии',      NULL, NULL,           'GE', 'OPERATOR_GATE', '#e8112d', 10, 50, 15000),
    ('FOREIGN_MOBILE', 'mobile-kz', 'Сотовая связь Казахстана',  'Сотовая связь Казахстана',  NULL, NULL,           'KZ', 'OPERATOR_GATE', '#00afca', 20, 50, 15000),
    ('FOREIGN_MOBILE', 'mobile-kg', 'Сотовая связь Кыргызстана', 'Сотовая связь Кыргызстана', NULL, NULL,           'KG', 'OPERATOR_GATE', '#e8112d', 30, 50, 15000),
    ('FOREIGN_MOBILE', 'mobile-tj', 'Сотовая связь Таджикистана', 'Сотовая связь Таджикистана', NULL, NULL,         'TJ', 'OPERATOR_GATE', '#006600', 40, 50, 15000),
    ('FOREIGN_MOBILE', 'mobile-uz', 'Сотовая связь Узбекистана', 'Сотовая связь Узбекистана', NULL, NULL,           'UZ', 'OPERATOR_GATE', '#1eb53a', 50, 50, 15000),
    ('FOREIGN_MOBILE', 'mobile-am', 'Сотовая связь Армении',     'Сотовая связь Армении',     NULL, NULL,           'AM', 'OPERATOR_GATE', '#f2a800', 60, 50, 15000),
    ('FOREIGN_MOBILE', 'azercell',  'Azercell Азербайджан',      'Azercell Азербайджан',      NULL, 'azercell.com', NULL, 'OPERATOR_GATE', '#5b2c86', 70, 50, 15000),
    ('FOREIGN_MOBILE', 'bakcell',   'Bakcell Азербайджан',       'Bakcell Азербайджан',       NULL, 'bakcell.com',  NULL, 'OPERATOR_GATE', '#e4002b', 80, 50, 15000),
    ('FOREIGN_MOBILE', 'nar',       'Nar Азербайджан',           'Nar Азербайджан',           NULL, 'nar.az',       NULL, 'OPERATOR_GATE', '#e6007e', 90, 50, 15000);

INSERT INTO foreign_mobile_providers (provider_id, dial_code, number_length, prefixes, operators, currency)
SELECT p.id, f.dial_code, f.number_length, f.prefixes, f.operators, f.currency FROM providers p JOIN (VALUES
    ('mobile-ge', '995', 9,  '5',                                        'Magti; GeoCell; Beeline',                  'GEL'),
    ('mobile-kz', '7',   10, '70,74,75,76,77,78',                        'Kcell; Activ; Beeline; Tele2; Altel',      'KZT'),
    ('mobile-kg', '996', 9,  '2,5,7,9',                                  'Beeline; MegaCom; O!',                     'KGS'),
    ('mobile-tj', '992', 9,  NULL,                                       'Tcell; Babilon-M; MegaFon; ZET-Mobile',    'TJS'),
    ('mobile-uz', '998', 9,  '33,50,77,88,90,91,93,94,95,97,98,99',      'Beeline; Ucell; Mobiuz; Uzmobile',         'UZS'),
    ('mobile-am', '374', 8,  '33,41,43,44,47,49,55,77,91,93,94,95,96,97,98,99', 'Team; Viva; Ucom',              'AMD'),
    ('azercell',  '994', 9,  '10,50,51',                                 'Azercell',                                 'AZN'),
    ('bakcell',   '994', 9,  '55,99',                                    'Bakcell',                                  'AZN'),
    ('nar',       '994', 9,  '70,77',                                    'Nar',                                      'AZN')
) AS f (code, dial_code, number_length, prefixes, operators, currency) ON f.code = p.code;

-- Поле телефона у каждого зарубежного оператора: подсказка — формат номера и операторы
INSERT INTO provider_fields (provider_id, key, label, type, placeholder, hint, info, sort_order)
SELECT f.provider_id, 'phone', 'Номер телефона', 'LOCAL_PHONE', 'Номер телефона',
       f.number_length || ' цифр, без кода страны ' || f.dial_code || ', Операторы ' || f.operators,
       CASE WHEN f.prefixes IS NULL THEN NULL ELSE 'Номер начинается с ' || replace(f.prefixes, ',', ', ') END, 10
FROM foreign_mobile_providers f;

-- ===== Поля транспорта и игр =====
INSERT INTO provider_fields (provider_id, key, label, type, required, placeholder, hint, info, pattern, depends_on, sort_order)
SELECT p.id, f.key, f.label, f.type, f.required, f.placeholder, f.hint, f.info, f.pattern, f.depends_on, f.sort_order
FROM providers p JOIN (VALUES
    ('troika',              'card',        'Номер карты',               'DIGITS', true,  'Номер карты',         '10 цифр', 'Номер напечатан на обратной стороне карты', '\d{10}', NULL, 10),
    ('parking-russia',      'city',        'Город',                     'CHOICE', true,  NULL,                  NULL, NULL, NULL, NULL, 10),
    ('parking-russia',      'phone',       'Номер телефона',            'PHONE',  true,  '+7',                  'Номер, к которому привязан парковочный счёт', NULL, NULL, NULL, 20),
    ('strelka',             'card',        'Номер карты',               'DIGITS', true,  'Номер карты',         '11 цифр', 'Номер напечатан на обратной стороне карты', '\d{11}', NULL, 10),
    ('avtodor-account',     'account',     'Номер лицевого счёта',      'DIGITS', true,  'Номер лицевого счёта', '10 цифр, есть в личном кабинете «Автодор — Платные дороги»', NULL, '\d{10}', NULL, 10),
    ('avtodor-account',     'email',       'Email для чека',            'EMAIL',  false, 'Email для чека',      NULL, NULL, NULL, NULL, 20),
    ('avtodor-phone',       'phone',       'Номер телефона',            'PHONE',  true,  '+7',                  'Номер, указанный в договоре с «Автодор — Платные дороги»', NULL, NULL, NULL, 10),
    ('avtodor-phone',       'email',       'Email для чека',            'EMAIL',  false, 'Email для чека',      NULL, NULL, NULL, NULL, 20),
    ('avtodor-transponder', 'transponder', 'Номер транспондера',        'DIGITS', true,  'Номер транспондера',  '19 цифр', 'Номер на наклейке транспондера, под штрихкодом', '\d{19}', NULL, 10),
    ('avtodor-transponder', 'email',       'Email для чека',            'EMAIL',  false, 'Email для чека',      NULL, NULL, NULL, NULL, 20),
    ('taxi-maxim',          'phone',       'Номер телефона в приложении', 'PHONE', true, '+7',                  'Пополним баланс аккаунта, привязанного к этому номеру', NULL, NULL, NULL, 10),
    ('taxi-maxim',          'city',        'Город',                     'TEXT',   true,  'Например, Казань',    NULL, NULL, '[А-Яа-яЁё -]{2,40}', NULL, 20),
    ('kupibilet',           'order',       'Номер заказа',              'DIGITS', true,  'Номер заказа',        '9 цифр из письма с подтверждением', NULL, '\d{9}', NULL, 10),
    ('kupibilet',           'email',       'Email, указанный в заказе', 'EMAIL',  true,  'mail@example.ru',     NULL, NULL, NULL, NULL, 20),
    ('podorozhnik',         'card',        'Номер карты',               'DIGITS', true,  'Номер карты',         '19 цифр', 'Номер напечатан на обратной стороне карты', '\d{19}', NULL, 10),

    ('chatgpt',        'email',    'Электронная почта',                      'EMAIL',  true, 'mail@example.ru', 'Ваучер придет на этот адрес электронной почты', NULL, NULL, NULL, 10),
    ('chatgpt',        'region',   'Выберите страну регистрации аккаунта',   'CHOICE', true, NULL, NULL, NULL, NULL, NULL, 20),
    ('chatgpt',        'nominal',  'Выберите номинал',                       'NOMINAL', true, NULL, NULL, NULL, NULL, 'region', 30),
    ('steam',          'login',    'Введите логин Steam',                    'TEXT',   true, 'Введите логин Steam',
        'Аккаунты РФ и стран СНГ. Комиссия за конвертацию до 10%. Деньги поступят на аккаунт в течение 15 мин. В редких случаях — до 2 часов.',
        'Логин — имя для входа в Steam, а не никнейм в профиле', '[A-Za-z0-9_]{3,32}', NULL, 10),
    ('steam-voucher',  'email',    'Электронная почта',                      'EMAIL',  true, 'mail@example.ru', 'Код ваучера придёт на этот адрес', NULL, NULL, NULL, 10),
    ('steam-voucher',  'nominal',  'Выберите номинал',                       'NOMINAL', true, NULL, 'Код подходит для аккаунтов с валютой USD', NULL, NULL, NULL, 20),
    ('psn',            'region',   'Регион аккаунта PlayStation',            'CHOICE', true, NULL, NULL, NULL, NULL, NULL, 10),
    ('psn',            'nominal',  'Выберите номинал',                       'NOMINAL', true, NULL, NULL, NULL, NULL, 'region', 20),
    ('psn',            'email',    'Электронная почта',                      'EMAIL',  true, 'mail@example.ru', 'Код пополнения придёт на этот адрес', NULL, NULL, NULL, 30),
    ('xbox',           'email',    'Email аккаунта Microsoft',               'EMAIL',  true, 'mail@example.ru', NULL, 'Почта, с которой вы входите в Xbox', NULL, NULL, 10),
    ('xbox',           'region',   'Регион аккаунта',                        'CHOICE', true, NULL, NULL, NULL, NULL, NULL, 20),
    ('xbox',           'nominal',  'Выберите номинал',                       'NOMINAL', true, NULL, NULL, NULL, NULL, 'region', 30),
    ('xbox-game-pass', 'email',    'Электронная почта',                      'EMAIL',  true, 'mail@example.ru', 'Код активации придёт на этот адрес', NULL, NULL, NULL, 10),
    ('xbox-game-pass', 'nominal',  'Выберите подписку',                      'NOMINAL', true, NULL, NULL, NULL, NULL, NULL, 20),
    ('ea-games',       'eaId',     'EA ID',                                  'TEXT',   true, 'EA ID', 'Имя аккаунта в EA App', 'EA ID видно в профиле EA App', '[A-Za-z0-9_-]{4,16}', NULL, 10),
    ('ea-games',       'platform', 'Платформа',                              'CHOICE', true, NULL, NULL, NULL, NULL, NULL, 20),
    ('ea-games',       'email',    'Email для чека',                         'EMAIL',  false, 'Email для чека', NULL, NULL, NULL, NULL, 30),
    ('apex-xbox',      'gamertag', 'Gamertag Xbox',                          'TEXT',   true, 'Gamertag', 'Имя игрока в Xbox, до 15 символов', NULL, '[A-Za-z0-9 ]{1,15}', NULL, 10),
    ('apex-xbox',      'nominal',  'Выберите номинал',                       'NOMINAL', true, NULL, NULL, NULL, NULL, NULL, 20),
    ('genshin',        'uid',      'UID игрока',                             'DIGITS', true, 'UID', '9 цифр', 'UID — в правом нижнем углу меню Паймон', '\d{9}', NULL, 10),
    ('genshin',        'server',   'Сервер',                                 'CHOICE', true, NULL, NULL, NULL, NULL, NULL, 20),
    ('genshin',        'nominal',  'Выберите номинал',                       'NOMINAL', true, NULL, NULL, NULL, NULL, NULL, 30),
    ('pubg-mobile',    'playerId', 'Player ID',                              'DIGITS', true, 'Player ID', '10–12 цифр из профиля в игре', NULL, '\d{10,12}', NULL, 10),
    ('pubg-mobile',    'nominal',  'Выберите номинал',                       'NOMINAL', true, NULL, NULL, NULL, NULL, NULL, 20)
) AS f (code, key, label, type, required, placeholder, hint, info, pattern, depends_on, sort_order) ON f.code = p.code;

INSERT INTO provider_field_options (field_id, value, label, country_code, sort_order)
SELECT pf.id, o.value, o.label, o.country, o.sort_order
FROM provider_fields pf JOIN providers p ON p.id = pf.provider_id JOIN (VALUES
    ('parking-russia', 'city',     'MSK',  'Москва',          NULL, 10), ('parking-russia', 'city', 'SPB', 'Санкт-Петербург', NULL, 20),
    ('chatgpt',        'region',   'US',   'США',             'US', 10), ('chatgpt', 'region', 'EU', 'Европа', 'EU', 20),
    ('psn',            'region',   'TR',   'Турция',          'TR', 10), ('psn', 'region', 'IN', 'Индия', 'IN', 20),
    ('psn',            'region',   'PL',   'Польша',          'PL', 30),
    ('xbox',           'region',   'US',   'США',             'US', 10), ('xbox', 'region', 'TR', 'Турция', 'TR', 20),
    ('ea-games',       'platform', 'PC',   'PC',              NULL, 10), ('ea-games', 'platform', 'PS', 'PlayStation', NULL, 20),
    ('ea-games',       'platform', 'XBOX', 'Xbox',            NULL, 30),
    ('genshin',        'server',   'EU',   'Европа',          NULL, 10), ('genshin', 'server', 'AM', 'Америка', NULL, 20),
    ('genshin',        'server',   'AS',   'Азия',            NULL, 30)
) AS o (code, key, value, label, country, sort_order) ON o.code = p.code AND o.key = pf.key;
