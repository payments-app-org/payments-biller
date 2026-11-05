package com.paymentsapp.biller.bill;

import static com.paymentsapp.biller.error.BillerError.UNKNOWN_PROVIDER;

import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderRepository;
import com.paymentsapp.platform.error.DomainException;
import java.util.List;
import org.springframework.stereotype.Service;

// Проверка платежа поставщику перед созданием платежа в payment. Как проверять — решает первый подходящий
// BillMethod: связь — оператор по номеру, госуслуги — начисление, квитанция или реквизиты, остальное — поля по шаблону.
@Service
public class BillService {

    private final ProviderRepository providers;
    private final List<BillMethod> methods;

    public BillService(ProviderRepository providers, List<BillMethod> methods) {
        this.providers = providers;
        this.methods = methods;
    }

    public BillCheck check(BillCheckRequest req) {
        ProviderDetails details = providers.details(req.providerCode() == null ? "" : req.providerCode())
                .orElseThrow(() -> new DomainException(UNKNOWN_PROVIDER));
        return methods.stream().filter(m -> m.supports(details, req)).findFirst().orElseThrow().check(details, req);
    }
}
