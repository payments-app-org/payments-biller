package com.paymentsapp.biller.bill;

import com.paymentsapp.biller.client.Backends;
import com.paymentsapp.biller.client.GatewayApi;
import com.paymentsapp.biller.provider.ProviderGroup;
import com.paymentsapp.platform.resilience.ResilientCalls;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

// Шлюзы поставщиков. Вызов оплаты идемпотентен по paymentId: повтор вернёт тот же ответ.
@Component
public class GatewayClient {

    private final GatewayApi api;
    private final ResilientCalls calls;

    public GatewayClient(GatewayApi api, ResilientCalls calls) {
        this.api = api;
        this.calls = calls;
    }

    public GatewayResult pay(UUID paymentId, String integration, String providerCode, BigDecimal amount,
                             Map<String, String> fields, boolean issueVoucher) {
        GatewayApi.PayRequest request = new GatewayApi.PayRequest(paymentId, integration, providerCode, amount, fields, issueVoucher);
        return calls.call(Backends.GATEWAY, Backends.GATEWAY_CONNECT_ONLY, () -> api.pay(request));
    }

    public Optional<GatewayResult> find(UUID paymentId) {
        return calls.call(Backends.GATEWAY, () -> {
            try {
                return Optional.ofNullable(api.payment(paymentId));
            } catch (HttpClientErrorException.NotFound e) {
                return Optional.empty();
            }
        });
    }

    // Счета у поставщика по данным плательщика (Образование Москвы: СНИЛС ребёнка, паспорт родителя…)
    public List<ExternalInvoice> invoices(String providerCode, ProviderGroup group, Map<String, String> fields) {
        return calls.call(Backends.GATEWAY, () -> api.invoices(new GatewayApi.InvoicesRequest(providerCode, group, fields)));
    }

    public record ExternalInvoice(String externalId, String title, String description, BigDecimal amount, LocalDate dueDate) {
    }

    // ACCEPTED — поставщик зачислил, REJECTED — отказал (message — почему), PENDING — принял, проведёт позже
    public enum GatewayStatus {
        ACCEPTED,
        REJECTED,
        PENDING
    }

    public record GatewayResult(UUID paymentId, GatewayStatus status, String externalId, String voucherCode, String message) {
    }
}
