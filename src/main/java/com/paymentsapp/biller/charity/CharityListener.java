package com.paymentsapp.biller.charity;

import com.paymentsapp.biller.messaging.PaymentEvent;
import com.paymentsapp.biller.provider.ProviderGroup;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

// Копим деньги фондов за день: из успешных платежей — округление на пользу и пожертвования.
// Своя группа потребителя: события те же, что у PaidChargesListener, но читать их нужно независимо.
// Повтор события безопасен: начисление уникально по платежу и источнику.
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
        PaymentEvent e = json.readValue(message, PaymentEvent.class);
        if (!e.succeeded()) {
            return;
        }
        LocalDate day = e.createdAt() != null ? e.createdAt().atZoneSameInstant(clock.getZone()).toLocalDate() : LocalDate.now(clock);
        if (e.hasRoundUp()) {
            accrue(e.roundUpFundCode(), AccrualSource.ROUNDUP, e.paymentId(), e.roundUpAmount(), day);
        }
        if (e.deferredDonation()) {
            accrue(e.providerCode(), AccrualSource.DONATION, e.paymentId(), e.amount(), day);
        }
    }

    private void accrue(String fundCode, AccrualSource source, UUID paymentId, BigDecimal amount, LocalDate day) {
        Integer providerId = jdbc.sql("select id from providers where code = :code and group_code = :group")
                .param("code", fundCode).param("group", ProviderGroup.DONATIONS.name())
                .query(Integer.class).optional().orElse(null);
        if (providerId == null) {
            log.warn("платёж {}: фонд {} не найден, {} {} ₽ не учтены", paymentId, fundCode, source, amount);
            return;
        }
        jdbc.sql("""
                        insert into charity_accruals (id, provider_id, source, payment_id, amount, day)
                        values (:id, :provider, :source, :payment, :amount, :day)
                        on conflict (payment_id, source) do nothing
                        """)
                .param("id", UUID.randomUUID()).param("provider", providerId).param("source", source.name())
                .param("payment", paymentId).param("amount", amount).param("day", day)
                .update();
    }
}
