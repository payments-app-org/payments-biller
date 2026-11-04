--liquibase formatted sql

--changeset biller:004-mobile-gov
-- Общие поля поставщика для витрины: код (для ссылок), заголовок формы, юрлицо под заголовком, сайт (логотип), порядок
ALTER TABLE providers ADD COLUMN code       VARCHAR(40);
ALTER TABLE providers ADD COLUMN title      VARCHAR(255);
ALTER TABLE providers ADD COLUMN legal_name VARCHAR(255);
ALTER TABLE providers ADD COLUMN domain     VARCHAR(100);
ALTER TABLE providers ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 100;

UPDATE providers SET code = 'uin', name = 'Оплата по УИН', title = 'Оплата по УИН',
    legal_name = 'Оплата по уникальному идентификатору начисления', domain = 'gosuslugi.ru', sort_order = 20
WHERE integration = 'GIS_GMP';

ALTER TABLE providers ALTER COLUMN code SET NOT NULL;
ALTER TABLE providers ALTER COLUMN title SET NOT NULL;
ALTER TABLE providers ADD CONSTRAINT providers_code_uq UNIQUE (code);

-- Госуслуги: чем ищем начисления в ГИС ГМП и какого они вида.
-- FREE — начисления не ищем, плательщик сам вводит УИН и сумму (госпошлины Росреестра), получатель — из этой таблицы.
ALTER TABLE gov_providers DROP CONSTRAINT gov_providers_lookup_check;
ALTER TABLE gov_providers ADD CONSTRAINT gov_providers_lookup_check
    CHECK (lookup IN ('UIN', 'STS_VU', 'INN', 'PASSPORT_INN', 'FREE'));
-- Вид начислений: штрафы ГИБДД, налоги, долги ФССП; пусто — любые
ALTER TABLE gov_providers ADD COLUMN charge_kind   VARCHAR(10) CHECK (charge_kind IN ('GIBDD', 'TAX', 'FSSP'));
-- С чего начинается УИН этого администратора (321 — Росреестр)
ALTER TABLE gov_providers ADD COLUMN uin_prefix    VARCHAR(5);
ALTER TABLE gov_providers ADD COLUMN payee_name    VARCHAR(255);
ALTER TABLE gov_providers ADD COLUMN payee_inn     VARCHAR(12);
ALTER TABLE gov_providers ADD COLUMN payee_kpp     VARCHAR(9);
ALTER TABLE gov_providers ADD COLUMN payee_account VARCHAR(20);
ALTER TABLE gov_providers ADD COLUMN payee_bik     VARCHAR(9);
ALTER TABLE gov_providers ADD COLUMN kbk           VARCHAR(20);
ALTER TABLE gov_providers ADD COLUMN oktmo         VARCHAR(11);
ALTER TABLE gov_providers ADD COLUMN purpose       VARCHAR(255);
ALTER TABLE gov_providers ADD CONSTRAINT gov_providers_free_payee CHECK (lookup <> 'FREE' OR
    (payee_name IS NOT NULL AND payee_inn IS NOT NULL AND payee_kpp IS NOT NULL AND payee_account IS NOT NULL
     AND payee_bik IS NOT NULL AND kbk IS NOT NULL AND oktmo IS NOT NULL AND purpose IS NOT NULL));

INSERT INTO providers (group_code, code, name, title, legal_name, domain, integration, color, sort_order) VALUES
    ('GOV', 'gibdd',     'Штрафы ГАИ (ГИБДД)',    'Штрафы ГАИ (ГИБДД)',    'Федеральное казначейство',             'gibdd.ru',        'GIS_GMP', '#4a90e2', 10),
    ('GOV', 'fns-uin',   'Оплата налогов по УИН', 'Оплата налогов по УИН', 'Оплата по уникальному идентификатору начисления', 'nalog.gov.ru', 'GIS_GMP', '#2a5caa', 30),
    ('GOV', 'fns-inn',   'Налоги и сборы',        'Налоги и сборы',        'Оплата в бюджет по реквизитам (ФНС)',  'nalog.gov.ru',    'GIS_GMP', '#2a5caa', 40),
    ('GOV', 'fssp',      'Штрафы ФССП',           'Штрафы ФССП',           'Федеральная служба судебных приставов', 'fssp.gov.ru',    'GIS_GMP', '#c9a227', 50),
    ('GOV', 'rosreestr', 'Росреестр',             'Оплата госпошлины за услуги Росреестра',
        'Федеральная служба государственной регистрации, кадастра и картографии',                    'rosreestr.gov.ru', 'GIS_GMP', '#3a7bc8', 60);

INSERT INTO gov_providers (provider_id, lookup, charge_kind, description)
SELECT id, l.lookup, l.kind, l.description FROM providers p JOIN (VALUES
    ('gibdd',   'STS_VU',       'GIBDD', 'Штрафы по свидетельству о регистрации ТС и водительскому удостоверению'),
    ('fns-uin', 'UIN',          'TAX',   'Налоги по УИН из уведомления ФНС'),
    ('fns-inn', 'INN',          'TAX',   'Задолженность по налогам по ИНН'),
    ('fssp',    'PASSPORT_INN', 'FSSP',  'Задолженность по исполнительным производствам')
) AS l (code, lookup, kind, description) ON l.code = p.code;

INSERT INTO gov_providers (provider_id, lookup, uin_prefix, description, payee_name, payee_inn, payee_kpp, payee_account,
                           payee_bik, kbk, oktmo, purpose)
SELECT id, 'FREE', '321', 'Госпошлина по УИН из квитанции Росреестра',
       'УФК по г. Москве (Управление Росреестра по Москве)', '7726639745', '772601001', '03100643000000017300',
       '004525988', '32110807020018000110', '45000000', 'Государственная пошлина за услуги Росреестра'
FROM providers WHERE code = 'rosreestr';

-- Группа «Мобильная связь»: операторы и их диапазоны номеров (по реестру системы нумерации Россвязи)
CREATE TABLE mobile_providers (
    provider_id       INTEGER      PRIMARY KEY REFERENCES providers (id),
    -- Код сети: 250 — Россия, дальше номер оператора
    mnc               VARCHAR(6)   NOT NULL
);

INSERT INTO providers (group_code, code, name, title, legal_name, domain, integration, color, sort_order) VALUES
    ('MOBILE', 'mts',      'МТС',     'Пополнить счет МТС',               'МТС ПАО',          'mts.ru',     'PROCESSING', '#e30611', 10),
    ('MOBILE', 'megafon',  'МегаФон', 'Пополнить счет МегаФон',           'МегаФон ПАО',      'megafon.ru', 'PROCESSING', '#00b956', 20),
    ('MOBILE', 'beeline',  'Билайн',  'Пополнить счет Билайн',            'ВымпелКом ПАО',    'beeline.ru', 'PROCESSING', '#ffc800', 30),
    ('MOBILE', 't2',       'Т2',      'Пополнить счет Т2',                'Т2 Мобайл ООО',    't2.ru',      'PROCESSING', '#1f2229', 40),
    ('MOBILE', 'yota',     'Yota',    'Пополнить счет Yota сотовая связь', 'Скартел ООО',     'yota.ru',    'PROCESSING', '#3fb6e8', 50),
    ('MOBILE', 't-mobile', 'Т-Мобайл', 'Пополнить счет Т-Мобайл',         'Т-Мобайл АО',      'tbank.ru',   'PROCESSING', '#ffdd2d', 60);

INSERT INTO mobile_providers (provider_id, mnc)
SELECT id, m.mnc FROM providers p JOIN (VALUES
    ('mts', '250-01'), ('megafon', '250-02'), ('beeline', '250-99'), ('t2', '250-20'), ('yota', '250-11'), ('t-mobile', '250-62')
) AS m (code, mnc) ON m.code = p.code;

-- Диапазон номеров +7 DEF XXXXXXX: number_from..number_to — семь цифр после кода
CREATE TABLE mobile_number_ranges (
    id                SERIAL       PRIMARY KEY,
    def_code          VARCHAR(3)   NOT NULL,
    number_from       INTEGER      NOT NULL,
    number_to         INTEGER      NOT NULL,
    provider_id       INTEGER      NOT NULL REFERENCES mobile_providers (provider_id),
    CHECK (number_from <= number_to)
);

CREATE INDEX mobile_number_ranges_idx ON mobile_number_ranges (def_code, number_from);

INSERT INTO mobile_number_ranges (def_code, number_from, number_to, provider_id)
SELECT r.def_code, r.number_from, r.number_to, p.id
FROM (
    SELECT unnest(ARRAY['910','911','912','913','914','915','916','917','918','919','980','981','982','983','984','985','987','988','989']) AS def_code,
           0 AS number_from, 9999999 AS number_to, 'mts' AS code
    UNION ALL
    SELECT unnest(ARRAY['920','921','922','923','924','925','926','927','928','929','930','931','932','933','934','936','937','938','939','997','999']),
           0, 9999999, 'megafon'
    UNION ALL
    SELECT unnest(ARRAY['903','905','906','909','960','961','962','963','964','965','966','967','968','969']), 0, 9999999, 'beeline'
    UNION ALL
    SELECT unnest(ARRAY['900','901','902','904','908','950','951','952','953','977','991','992','993','994','995']), 0, 9999999, 't2'
    UNION ALL
    SELECT '958', 0, 4999999, 't2'
    UNION ALL
    SELECT '958', 5000000, 9999999, 't-mobile'
    UNION ALL
    SELECT '996', 0, 4999999, 'yota'
    UNION ALL
    SELECT '996', 5000000, 9999999, 'megafon'
) r JOIN providers p ON p.code = r.code;
