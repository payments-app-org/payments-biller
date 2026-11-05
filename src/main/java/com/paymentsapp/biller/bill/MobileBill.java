package com.paymentsapp.biller.bill;

import static com.paymentsapp.biller.bill.FieldValidator.require;
import static com.paymentsapp.biller.error.BillerError.*;

import com.paymentsapp.biller.bill.BillCheck.BillField;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderGroup;
import com.paymentsapp.biller.provider.ProviderRepository;
import com.paymentsapp.platform.error.DomainException;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Мобильная связь: номер должен принадлежать этому оператору (реестр нумерации)
@Component
@Order(1)
class MobileBill implements BillMethod {

    static final String PHONE = "phone";
    private static final String PHONE_LABEL = "Номер телефона";
    private static final String PURPOSE = "Пополнение счёта %s";
    // +7 DEF XXXXXXX
    private static final int DEF_FROM = 2;
    private static final int DEF_TO = 5;

    private final ProviderRepository providers;

    MobileBill(ProviderRepository providers) {
        this.providers = providers;
    }

    @Override
    public boolean supports(ProviderDetails provider, BillCheckRequest request) {
        return provider.provider().groupCode() == ProviderGroup.MOBILE;
    }

    @Override
    public BillCheck check(ProviderDetails details, BillCheckRequest req) {
        Provider p = details.provider();
        String phone = req.fieldsOrEmpty().getOrDefault(PHONE, "").trim();
        require(FieldValidator.RU_PHONE.matcher(phone).matches(), PHONE_FORMAT);
        Provider operator = providers.mobileOperator(phone.substring(DEF_FROM, DEF_TO), Integer.parseInt(phone.substring(DEF_TO)))
                .orElseThrow(() -> new DomainException(OPERATOR_UNKNOWN));
        if (operator.id() != p.id()) {
            throw new DomainException(OPERATOR_MISMATCH, operator.name());
        }
        String display = BillTexts.ruPhone(phone);
        return new BillCheck(p.code(), p.groupCode(), p.name(), PURPOSE.formatted(display),
                List.of(new BillField(PHONE, PHONE_LABEL, phone, display)), null, null, phone);
    }
}
