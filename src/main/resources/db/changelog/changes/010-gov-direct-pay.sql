--liquibase formatted sql

--changeset biller:010-gov-direct-pay
-- Шаблоны и автоплатежи госуслуг: сумма плательщика без найденного начисления. Платим на реквизиты администратора:
-- налоги — единым налоговым платежом, штрафы ГИБДД и долги ФССП — в УФК, УИН — в Федеральное казначейство.
UPDATE gov_providers g SET payee_name = v.name, payee_inn = v.inn, payee_kpp = v.kpp, payee_account = v.account,
    payee_bik = v.bik, kbk = v.kbk, oktmo = '45000000', purpose = v.purpose
FROM providers p, (VALUES
    ('gibdd',   'УФК по г. Москве (ГУ МВД России по г. Москве)',  '7707089101', '770731005', '03100643000000017300', '004525988', '18811601123010001140', 'Оплата штрафов ГИБДД'),
    ('uin',     'Федеральное казначейство',                        '7710568760', '771001001', '03100643000000019500', '004525988', '10011301991010000130', 'Оплата по УИН'),
    ('fns-uin', 'Казначейство России (ФНС России)',                '7727406020', '770801001', '03100643000000018500', '017003983', '18201061201010000510', 'Единый налоговый платёж'),
    ('fns-inn', 'Казначейство России (ФНС России)',                '7727406020', '770801001', '03100643000000018500', '017003983', '18201061201010000510', 'Единый налоговый платёж'),
    ('fssp',    'УФК по г. Москве (ГУФССП России по г. Москве)',  '7704270863', '770401001', '03100643000000017300', '004525988', '32211701010016000180', 'Погашение задолженности по исполнительному производству')
) AS v (code, name, inn, kpp, account, bik, kbk, purpose)
WHERE p.id = g.provider_id AND p.code = v.code;

-- Поля для шаблонов и автоплатежей у тех госпоставщиков, где их ещё нет
INSERT INTO provider_fields (provider_id, key, label, type, required, placeholder, hint, info, pattern, stage, prefill, sort_order)
SELECT p.id, f.key, f.label, f.type, f.required, f.placeholder, f.hint, NULL, f.pattern, f.stage, f.prefill, f.sort_order
FROM providers p JOIN (VALUES
    ('uin',       'uin',          'УИН',                 'DIGITS', true, '20 или 25 цифр', 'Уникальный идентификатор начисления', '\d{20}|\d{25}', 'SEARCH', NULL, 10),
    ('fns-uin',   'uin',          'УИН',                 'DIGITS', true, '20 или 25 цифр', 'Из налогового уведомления', '\d{20}|\d{25}', 'SEARCH', NULL, 10),
    ('rosreestr', 'uin',          'УИН',                 'DIGITS', true, '321…', '20 или 25 цифр из квитанции, начинается с 321', '321\d{17}|321\d{22}', 'PAY', NULL, 10),
    ('rosreestr', 'payerName',    'ФИО плательщика',     'TEXT',   true, 'ФИО плательщика', NULL, '[А-Яа-яЁё -]{5,150}', 'PAY', 'USER_FULL_NAME', 20),
    ('rosreestr', 'payerAddress', 'Адрес плательщика',   'TEXT',   true, 'Адрес плательщика', NULL, '.{5,255}', 'PAY', NULL, 30),
    ('rosreestr', 'payerPhone',   'Телефон плательщика', 'PHONE',  true, '+7', NULL, NULL, 'PAY', 'USER_PHONE', 40)
) AS f (code, key, label, type, required, placeholder, hint, pattern, stage, prefill, sort_order) ON f.code = p.code;
