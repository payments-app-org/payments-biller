package com.paymentsapp.biller.bill;

import static com.paymentsapp.biller.error.BillerError.*;

import com.paymentsapp.biller.bill.BillCheck.BillField;
import com.paymentsapp.biller.gov.GovCharge;
import com.paymentsapp.biller.gov.GovChargeService;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderGroup;
import com.paymentsapp.platform.error.DomainException;
import com.paymentsapp.platform.text.Rubles;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Найденное начисление ГИС ГМП: сумма и получатель — из начисления, а не из запроса
@Component
@Order(3)
class GovChargeBill implements BillMethod {

    private static final String UIN = "uin";
    private static final String UIN_LABEL = "УИН";

    private final GovChargeService charges;

    GovChargeBill(GovChargeService charges) {
        this.charges = charges;
    }

    @Override
    public boolean supports(ProviderDetails provider, BillCheckRequest request) {
        return provider.provider().groupCode() == ProviderGroup.GOV && request.chargeId() != null;
    }

    @Override
    public BillCheck check(ProviderDetails details, BillCheckRequest req) {
        Provider p = details.provider();
        GovCharge c = charges.get(req.chargeId()).orElseThrow(() -> new DomainException(CHARGE_NOT_FOUND));
        if (c.status() != BillStatus.UNPAID) {
            throw new DomainException(CHARGE_PAID);
        }
        if (c.payableAmount().compareTo(req.amountOrZero()) != 0) {
            throw new DomainException(CHARGE_AMOUNT, Rubles.format(c.payableAmount()));
        }
        Map<String, String> requisites = BillTexts.budgetRequisites(c.uin(), c.payeeName(), c.payeeInn(), c.payeeKpp(),
                c.payeeAccount(), c.payeeBik(), c.kbk(), c.oktmo());
        return new BillCheck(p.code(), p.groupCode(), c.payeeName(), c.purpose(),
                List.of(new BillField(UIN, UIN_LABEL, c.uin(), c.uin())), requisites, BillReference.charge(c.uin(), c.id()), null);
    }
}
