package com.paymentsapp.biller.gov;

import com.paymentsapp.biller.client.Backends;
import com.paymentsapp.biller.client.GisGmpApi;
import com.paymentsapp.platform.resilience.ResilientCalls;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

// Адаптер интеграции GIS_GMP: начисления по УИН или по идентификаторам плательщика (СТС, ВУ, ИНН, паспорт).
// Запрос только читает — повторять безопасно.
@Component
public class GisGmpClient {

    private final GisGmpApi api;
    private final ResilientCalls calls;

    public GisGmpClient(GisGmpApi api, ResilientCalls calls) {
        this.api = api;
        this.calls = calls;
    }

    // query: uin | sts, vu | inn | passport, inn; kind — вид начислений (GIBDD, TAX, FSSP), если важен
    public List<ExternalCharge> charges(Map<String, String> query) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        query.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                params.add(key, value);
            }
        });
        return calls.call(Backends.GIS_GMP, () -> api.charges(params));
    }

    public record ExternalCharge(String externalId, String uin, String purpose, String payeeName, String payeeInn,
                                 String payeeKpp, String payeeAccount, String payeeBik, String kbk, String oktmo,
                                 BigDecimal amount, BigDecimal discountedAmount, LocalDate discountUntil,
                                 LocalDate issuedAt) {
    }
}
