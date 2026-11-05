--liquibase formatted sql

--changeset biller:012-pending-payments
-- PENDING: поставщик принял оплату, но проведёт позже (подписка через партнёра); итог дозапрашиваем у шлюза
ALTER TABLE biller_payments DROP CONSTRAINT biller_payments_status_check;
ALTER TABLE biller_payments ADD CONSTRAINT biller_payments_status_check CHECK (status IN ('ACCEPTED', 'REJECTED', 'PENDING'));

--changeset biller:012-method-limits
-- Сумма одного перевода за рубеж (₽ к списанию) у каждого способа своя: на карту — до 150 тыс., на счёт — до 1 млн
ALTER TABLE transfer_methods ADD COLUMN min_amount NUMERIC(19, 2);
ALTER TABLE transfer_methods ADD COLUMN max_amount NUMERIC(19, 2);
UPDATE transfer_methods SET min_amount = 100, max_amount = 150000 WHERE kind = 'CARD';
UPDATE transfer_methods SET min_amount = 100, max_amount = 100000 WHERE kind = 'PHONE';
UPDATE transfer_methods SET min_amount = 1000, max_amount = 1000000 WHERE kind = 'ACCOUNT';
UPDATE transfer_methods SET min_amount = 100, max_amount = 50000 WHERE kind = 'WALLET';
ALTER TABLE transfer_methods ALTER COLUMN min_amount SET NOT NULL;
ALTER TABLE transfer_methods ALTER COLUMN max_amount SET NOT NULL;
ALTER TABLE transfer_methods ADD CONSTRAINT transfer_methods_amount_range CHECK (min_amount > 0 AND max_amount >= min_amount);
