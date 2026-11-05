package com.paymentsapp.biller.gov;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paymentsapp.biller.provider.LookupMethod;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PayerIdsTest {

    @Test
    void stsAndVuAreNormalized() {
        Map<String, String> ids = PayerIds.check(LookupMethod.STS_VU, null, Map.of("sts", "77 ак 123456"));
        assertThat(ids).containsEntry("sts", "77АК123456").containsEntry("vu", null);
    }

    @Test
    void invalidInputsAreRejected() {
        assertThatThrownBy(() -> PayerIds.check(LookupMethod.UIN, "321", Map.of("uin", "18810177230000123456")))
                .hasMessage("УИН этого получателя начинается с 321");
        assertThatThrownBy(() -> PayerIds.check(LookupMethod.INN, null, Map.of("inn", "771234567850")))
                .hasMessage("Некорректный ИНН физического лица");
        assertThatThrownBy(() -> PayerIds.check(LookupMethod.FREE, null, Map.of())).hasMessage("У этого получателя начисления не ищутся");
    }
}
