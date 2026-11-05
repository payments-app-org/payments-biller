package com.paymentsapp.biller.gov;

import com.paymentsapp.biller.bill.BillReference;
import com.paymentsapp.biller.bill.BillStatus;
import com.paymentsapp.biller.bill.InvoiceRepository;
import com.paymentsapp.biller.messaging.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

// Успешная оплата начисления или счёта у поставщика — помечаем его оплаченным: второй раз не заплатят
@Component
public class PaidChargesListener {

    private static final Logger log = LoggerFactory.getLogger(PaidChargesListener.class);

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final InvoiceRepository invoices;

    public PaidChargesListener(JdbcClient jdbc, JsonMapper json, InvoiceRepository invoices) {
        this.jdbc = jdbc;
        this.json = json;
        this.invoices = invoices;
    }

    @KafkaListener(topics = "${messaging.payment-events.topic}")
    public void onPaymentEvent(String message) {
        PaymentEvent e = json.readValue(message, PaymentEvent.class);
        if (!e.succeeded() || !e.isBill()) {
            return;
        }
        BillReference.parse(e.billReference()).ifPresent(ref -> {
            boolean marked = switch (ref) {
                case BillReference.Invoice invoice -> invoices.markPaid(invoice.id()) == 1;
                case BillReference.Charge charge -> jdbc.sql("update gov_charges set status = :paid where id = :id and status = :unpaid")
                        .param("id", charge.id()).param("paid", BillStatus.PAID.name()).param("unpaid", BillStatus.UNPAID.name())
                        .update() == 1;
            };
            if (marked) {
                log.info("{} оплачен (платёж {})", e.billReference(), e.paymentId());
            }
        });
    }
}
