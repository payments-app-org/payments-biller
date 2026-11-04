package com.paymentsapp.biller.subscription;

import com.paymentsapp.biller.provider.Provider;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Шаблон «Счета на оплату»: поставщик и данные плательщика. unpaidCount / unpaidTotal — итог последней проверки.
public record Subscription(UUID id, String name, Provider provider, Map<String, String> fields, OffsetDateTime checkedAt,
                           int unpaidCount, BigDecimal unpaidTotal) {

    // Неоплаченный счёт у поставщика: начисление ГИС ГМП (CHARGE) или счёт поставщика (INVOICE).
    // id — его передаём в payment как chargeId.
    public record Bill(UUID id, String kind, String title, String description, BigDecimal amount, BigDecimal fullAmount,
                       String discountUntil, String dueDate) {
    }

    public record WithBills(Subscription subscription, List<Bill> bills) {
    }
}
