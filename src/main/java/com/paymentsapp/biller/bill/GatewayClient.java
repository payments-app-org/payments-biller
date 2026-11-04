package com.paymentsapp.biller.bill;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

// Шлюзы поставщиков (заглушка в providers): операторы связи, транспорт, игры, ГИС ГМП (квитирование).
// Вызов идемпотентен по paymentId: повтор вернёт тот же ответ.
@Component
public class GatewayClient {

    private final RestClient rest;

    public GatewayClient(RestClient.Builder builder, @Value("${integrations.gateway.url}") String url) {
        this.rest = builder.baseUrl(url).build();
    }

    public GatewayResult pay(UUID paymentId, String integration, String providerCode, BigDecimal amount,
                             Map<String, String> fields, boolean issueVoucher) {
        return rest.post().uri("/gateway/payments")
                .body(Map.of("paymentId", paymentId, "integration", integration, "providerCode", providerCode,
                        "amount", amount, "fields", fields, "issueVoucher", issueVoucher))
                .retrieve().body(GatewayResult.class);
    }

    public Optional<GatewayResult> find(UUID paymentId) {
        try {
            return Optional.ofNullable(rest.get().uri("/gateway/payments/{id}", paymentId).retrieve().body(GatewayResult.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    // Счета у поставщика по данным плательщика (Образование Москвы: СНИЛС ребёнка, паспорт родителя…)
    public java.util.List<ExternalInvoice> invoices(String providerCode, String group, Map<String, String> fields) {
        return rest.post().uri("/gateway/invoices").body(Map.of("providerCode", providerCode, "group", group, "fields", fields))
                .retrieve().body(new org.springframework.core.ParameterizedTypeReference<java.util.List<ExternalInvoice>>() { });
    }

    public record ExternalInvoice(String externalId, String title, String description, BigDecimal amount,
                                  java.time.LocalDate dueDate) {
    }

    // status: ACCEPTED — поставщик зачислил, REJECTED — отказал (message — почему)
    public record GatewayResult(UUID paymentId, String status, String externalId, String voucherCode, String message) {
    }
}
