package com.paymentsapp.biller.messaging;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// Итог платежа из топика payment.events — только поля, нужные biller (остальные Jackson пропускает)
public record PaymentEvent(String eventType, UUID paymentId, String type, String billReference, String providerCode,
                           BigDecimal amount, OffsetDateTime createdAt, BigDecimal roundUpAmount, String roundUpFundCode,
                           Boolean charitySettlement) {

    private static final String SUCCEEDED = "PaymentSucceeded";
    private static final String BILL = "BILL";

    public boolean succeeded() {
        return SUCCEEDED.equals(eventType);
    }

    public boolean isBill() {
        return BILL.equals(type);
    }

    public boolean hasRoundUp() {
        return roundUpAmount != null && roundUpAmount.signum() > 0;
    }

    // Пожертвование, которое payment провёл без шлюза: перевести фонду должен biller
    public boolean deferredDonation() {
        return Boolean.TRUE.equals(charitySettlement);
    }
}
