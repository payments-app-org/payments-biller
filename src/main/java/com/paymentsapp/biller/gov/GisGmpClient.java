package com.paymentsapp.biller.gov;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Адаптер интеграции GIS_GMP: запрос начислений по УИН у ГИС ГМП
@Component
public class GisGmpClient {

    private final RestClient rest;

    public GisGmpClient(RestClient.Builder builder, @Value("${integrations.gis-gmp.url}") String url) {
        this.rest = builder.baseUrl(url).build();
    }

    public List<ExternalCharge> charges(String uin) {
        return rest.get().uri("/gis-gmp/charges?uin={uin}", uin).retrieve()
                .body(new ParameterizedTypeReference<List<ExternalCharge>>() { });
    }

    public record ExternalCharge(String externalId, String uin, String purpose, String payeeName, String payeeInn,
                                 String payeeKpp, String payeeAccount, String payeeBik, String kbk, String oktmo,
                                 BigDecimal amount, BigDecimal discountedAmount, LocalDate discountUntil,
                                 LocalDate issuedAt) {
    }
}
