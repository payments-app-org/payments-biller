package com.paymentsapp.biller.provider;


// Особенности госпоставщика: чем ищем начисления (lookup) и какого они вида. Для FREE — получатель платежа.
public record GovDetails(LookupMethod lookup, String chargeKind, String uinPrefix, String description, String payeeName,
                         String payeeInn, String payeeKpp, String payeeAccount, String payeeBik, String kbk,
                         String oktmo, String purpose) {
}
