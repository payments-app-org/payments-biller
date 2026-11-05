package com.paymentsapp.biller.bill;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

// Что хочет оплатить пользователь: поставщик, поля формы, сумма; для найденного начисления или счёта — его id
public record BillCheckRequest(String providerCode, Map<String, String> fields, BigDecimal amount, UUID chargeId) {

    public Map<String, String> fieldsOrEmpty() {
        return fields == null ? Map.of() : fields;
    }

    public BigDecimal amountOrZero() {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
