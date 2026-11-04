package com.paymentsapp.biller.gov;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Адаптер интеграции GIS_GMP: запрос начислений по УИН или по идентификаторам плательщика (СТС, ВУ, ИНН, паспорт)
@Component
public class GisGmpClient {

    private final RestClient rest;

    public GisGmpClient(RestClient.Builder builder, @Value("${integrations.gis-gmp.url}") String url) {
        this.rest = builder.baseUrl(url).build();
    }

    // query: uin | sts, vu | inn | passport, inn; kind — вид начислений (GIBDD, TAX, FSSP), если важен
    public List<ExternalCharge> charges(Map<String, String> query) {
        return rest.get().uri(b -> {
                    b.path("/gis-gmp/charges");
                    query.forEach((k, v) -> {
                        if (v != null && !v.isBlank()) {
                            b.queryParam(k, "{" + k + "}");
                        }
                    });
                    return b.build(query);
                })
                .retrieve()
                .body(new ParameterizedTypeReference<List<ExternalCharge>>() { });
    }

    public record ExternalCharge(String externalId, String uin, String purpose, String payeeName, String payeeInn,
                                 String payeeKpp, String payeeAccount, String payeeBik, String kbk, String oktmo,
                                 BigDecimal amount, BigDecimal discountedAmount, LocalDate discountUntil,
                                 LocalDate issuedAt) {
    }
}
