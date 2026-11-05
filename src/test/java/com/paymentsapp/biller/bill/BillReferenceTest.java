package com.paymentsapp.biller.bill;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BillReferenceTest {

    @Test
    void invoiceAndChargeReferencesRoundTrip() {
        UUID id = UUID.randomUUID();
        assertThat(BillReference.parse(BillReference.invoice(id))).contains(new BillReference.Invoice(id));
        assertThat(BillReference.parse(BillReference.charge("18810177230000123456", id))).contains(new BillReference.Charge(id));
        assertThat(BillReference.parse("32100000000000000001")).isEmpty();
        assertThat(BillReference.parse(null)).isEmpty();
    }
}
