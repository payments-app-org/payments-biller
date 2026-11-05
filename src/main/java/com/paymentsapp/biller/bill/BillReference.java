package com.paymentsapp.biller.bill;

import java.util.Optional;
import java.util.UUID;

// Что нельзя оплатить дважды (payment хранит в платеже): счёт у поставщика — «inv/<id счёта>»,
// начисление ГИС ГМП — «<УИН>/<id начисления>», квитанция без начисления — УИН
public sealed interface BillReference {

    String INVOICE_PREFIX = "inv/";
    String SEPARATOR = "/";

    record Invoice(UUID id) implements BillReference {
    }

    record Charge(UUID id) implements BillReference {
    }

    static String invoice(UUID invoiceId) {
        return INVOICE_PREFIX + invoiceId;
    }

    static String charge(String uin, UUID chargeId) {
        return uin + SEPARATOR + chargeId;
    }

    static Optional<BillReference> parse(String reference) {
        if (reference == null || reference.isEmpty()) {
            return Optional.empty();
        }
        if (reference.startsWith(INVOICE_PREFIX)) {
            return Optional.of(new Invoice(UUID.fromString(reference.substring(INVOICE_PREFIX.length()))));
        }
        int slash = reference.lastIndexOf(SEPARATOR);
        return slash < 0 ? Optional.empty() : Optional.of(new Charge(UUID.fromString(reference.substring(slash + 1))));
    }
}
