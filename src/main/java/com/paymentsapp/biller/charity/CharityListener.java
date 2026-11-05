package com.paymentsapp.biller.charity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Копим деньги фондов за день: из успешных платежей — округление на пользу и пожертвования.
// Своя группа потребителя: события те же, что у PaidChargesListener, но читать их нужно независимо.
@Component
public class CharityListener {

    private static final Logger log = LoggerFactory.getLogger(CharityListener.class);

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final Clock clock;

    public CharityListener(JdbcClient jdbc, JsonMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    @KafkaListener(topics = "${messaging.payment-events.topic}", groupId = "${messaging.payment-events.charity-group}")
    public void onPaymentEvent(String message) {
        JsonNode e = json.readTree(message);
        if (!"PaymentSucceeded".equals(e.path("eventType").asString(""))) {
            return;
        }
        UUID paymentId = UUID.fromString(e.path("paymentId").asString());
        LocalDate day = e.hasNonNull("createdAt")
                ? OffsetDateTime.parse(e.path("createdAt").asString()).atZoneSameInstant(clock.getZone()).toLocalDate()
                : LocalDate.now(clock);
        BigDecimal roundUp = e.hasNonNull("roundUpAmount") ? e.path("roundUpAmount").decimalValue() : BigDecimal.ZERO;
        if (roundUp.signum() > 0) {
            accrue(e.path("roundUpFundCode").asString(""), "ROUNDUP", paymentId, roundUp, day);
        }
        // Пожертвование, которое payment провёл без шлюза: перевести фонду должны мы
        if (e.path("charitySettlement").asBoolean(false)) {
            accrue(e.path("providerCode").asString(""), "DONATION", paymentId, e.path("amount").decimalValue(), day);
        }
    }

    private void accrue(String fundCode, String source, UUID paymentId, BigDecimal amount, LocalDate day) {
        Integer providerId = jdbc.sql("select id from providers where code = :code and group_code = 'DONATIONS'")
                .param("code", fundCode).query(Integer.class).optional().orElse(null);
        if (providerId == null) {
            log.warn("платёж {}: фонд {} не найден, {} {} ₽ не учтены", paymentId, fundCode, source, amount);
            return;
        }
        jdbc.sql("""
                        insert into charity_accruals (id, provider_id, source, payment_id, amount, day)
                        values (:id, :provider, :source, :payment, :amount, :day)
                        on conflict (payment_id, source) do nothing
                        """)
                .param("id", UUID.randomUUID()).param("provider", providerId).param("source", source)
                .param("payment", paymentId).param("amount", amount).param("day", day)
                .update();
    }
}
