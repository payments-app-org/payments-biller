package com.paymentsapp.biller.gov;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Успешная оплата госуслуги → начисление помечаем оплаченным: в поиске по УИН его больше нет.
// Дальше здесь же будет квитирование в ГИС ГМП (сообщить поставщику об оплате).
@Component
public class PaidChargesListener {

    private static final Logger log = LoggerFactory.getLogger(PaidChargesListener.class);

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public PaidChargesListener(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @KafkaListener(topics = "payment.events")
    public void onPaymentEvent(String message) {
        JsonNode e = json.readTree(message);
        if (!"PaymentSucceeded".equals(e.path("eventType").asString("")) || !"BILL".equals(e.path("type").asString(""))) {
            return;
        }
        // billReference = «УИН/id начисления»
        String ref = e.path("billReference").asString("");
        int slash = ref.lastIndexOf('/');
        if (slash < 0) {
            return;
        }
        UUID chargeId = UUID.fromString(ref.substring(slash + 1));
        int updated = jdbc.sql("update gov_charges set status = 'PAID' where id = :id and status = 'UNPAID'")
                .param("id", chargeId).update();
        if (updated == 1) {
            log.info("Начисление {} оплачено (платёж {})", chargeId, e.path("paymentId").asString());
        }
    }
}
