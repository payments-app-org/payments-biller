package com.paymentsapp.biller.provider;

// Тип поля формы поставщика (biller.provider_fields.type): как проверять и показывать значение
public enum FieldType {
    TEXT,
    DIGITS,
    EMAIL,
    PHONE,
    // Номер без кода страны — по формату страны зарубежного оператора
    LOCAL_PHONE,
    CHOICE,
    NOMINAL,
    // Период оплаты: 2026-11
    MONTH;

    // Подходит ли значение поля для назначения платежа: номер карты, счёта, телефона, логин — не почта и не выбор из списка
    public boolean describesPayment() {
        return this != EMAIL && this != CHOICE && this != NOMINAL && this != MONTH;
    }
}
