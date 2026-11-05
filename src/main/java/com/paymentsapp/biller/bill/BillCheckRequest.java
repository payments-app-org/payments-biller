package com.paymentsapp.biller.bill;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

// Что хочет оплатить пользователь: поставщик, поля формы, сумма; для найденного начисления или счёта — его id
// Формат запроса; правила поставщика (обязательные поля, шаблоны, пределы суммы) — в BillService
public record BillCheckRequest(@NotBlank @Size(max = 40) String providerCode,
                               @Size(max = 20) Map<String, @Size(max = 200) String> fields,
                               @PositiveOrZero @Digits(integer = 15, fraction = 2) BigDecimal amount, UUID chargeId) {

    public Map<String, String> fieldsOrEmpty() {
        return fields == null ? Map.of() : fields;
    }

    public BigDecimal amountOrZero() {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
