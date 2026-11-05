package com.paymentsapp.biller.provider;

// Чем ищем начисления госпоставщика в ГИС ГМП. FREE — не ищем: плательщик вводит УИН и реквизиты квитанции сам.
public enum LookupMethod {
    UIN,
    STS_VU,
    INN,
    PASSPORT_INN,
    FREE
}
