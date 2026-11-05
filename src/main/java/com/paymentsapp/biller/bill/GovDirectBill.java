package com.paymentsapp.biller.bill;

import static com.paymentsapp.biller.error.BillerError.*;

import com.paymentsapp.biller.bill.BillCheck.BillField;
import com.paymentsapp.biller.gov.PayerIds;
import com.paymentsapp.biller.provider.GovDetails;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderGroup;
import com.paymentsapp.platform.error.DomainException;
import com.paymentsapp.platform.text.Rubles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Госуслуга без начисления — сумма плательщика на реквизиты администратора (шаблоны и автоплатежи:
// «налоги — 5000 ₽ каждый месяц»). Идентификаторы плательщика — для назначения платежа.
@Component
@Order(4)
class GovDirectBill implements BillMethod {

    private static final String UIN = "uin";
    private static final String MAIN_SEPARATOR = " · ";
    private static final Map<String, String> LABELS = Map.of(
            "uin", "УИН",
            "sts", "СТС",
            "vu", "Водительское удостоверение",
            "inn", "ИНН",
            "passport", "Паспорт");

    private final BillerProperties properties;

    GovDirectBill(BillerProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(ProviderDetails provider, BillCheckRequest request) {
        return provider.provider().groupCode() == ProviderGroup.GOV;
    }

    @Override
    public BillCheck check(ProviderDetails details, BillCheckRequest req) {
        Provider p = details.provider();
        GovDetails gov = details.gov();
        if (gov.payeeName() == null) {
            throw new DomainException(DIRECT_PAY_UNAVAILABLE);
        }
        if (req.amountOrZero().compareTo(properties.govMinAmount()) < 0) {
            throw new DomainException(MIN_AMOUNT, Rubles.format(properties.govMinAmount()));
        }
        Map<String, String> ids = PayerIds.check(gov.lookup(), gov.uinPrefix(), req.fieldsOrEmpty());
        List<BillField> fields = new ArrayList<>();
        ids.forEach((key, value) -> {
            if (value != null) {
                fields.add(new BillField(key, LABELS.getOrDefault(key, key), value, value));
            }
        });
        String main = fields.isEmpty() ? null : fields.getFirst().display();
        Map<String, String> requisites = BillTexts.budgetRequisites(ids.getOrDefault(UIN, ""), gov.payeeName(), gov.payeeInn(),
                gov.payeeKpp(), gov.payeeAccount(), gov.payeeBik(), gov.kbk(), gov.oktmo());
        return new BillCheck(p.code(), p.groupCode(), gov.payeeName(), gov.purpose() + (main != null ? MAIN_SEPARATOR + main : ""),
                fields, requisites, null, null);
    }
}
