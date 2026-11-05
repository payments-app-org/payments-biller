package com.paymentsapp.biller.bill;

import com.paymentsapp.biller.provider.FieldStage;
import com.paymentsapp.biller.provider.FieldType;
import com.paymentsapp.biller.provider.GovDetails;
import com.paymentsapp.biller.provider.LookupMethod;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderDetails.Field;
import com.paymentsapp.biller.provider.ProviderGroup;
import java.math.BigDecimal;
import java.util.List;

// Карточки поставщиков для тестов
final class Providers {

    private Providers() {
    }

    static Provider provider(int id, String code, ProviderGroup group) {
        return new Provider(id, code, group, code, code, null, null, null, null, null, null);
    }

    static Field field(String key, FieldType type, boolean required, String pattern) {
        return new Field(key, key, type, required, null, null, null, pattern, null, FieldStage.BOTH, null, List.of());
    }

    static ProviderDetails details(Provider p, List<Field> fields, GovDetails gov) {
        return new ProviderDetails(p, fields, List.of(), gov, null, null, null, null, null, null, null, null, null, null,
                null, null, null);
    }

    static GovDetails gov(LookupMethod lookup, String payee) {
        return new GovDetails(lookup, null, null, null, payee, "7702070139", "770201001", "03100643000000017300",
                "024501901", "18811601123010001140", "45000000", "Штраф");
    }

    static BigDecimal rub(String value) {
        return new BigDecimal(value);
    }
}
