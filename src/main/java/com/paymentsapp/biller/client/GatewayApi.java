package com.paymentsapp.biller.client;

import com.paymentsapp.biller.bill.GatewayClient.ExternalInvoice;
import com.paymentsapp.biller.bill.GatewayClient.GatewayResult;
import com.paymentsapp.biller.provider.ProviderGroup;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

// Шлюзы поставщиков (заглушка в providers): операторы связи, транспорт, игры, счета у поставщика
@HttpExchange("/gateway")
public interface GatewayApi {

    @PostExchange("/payments")
    GatewayResult pay(@RequestBody PayRequest request);

    @GetExchange("/payments/{paymentId}")
    GatewayResult payment(@PathVariable UUID paymentId);

    @PostExchange("/invoices")
    List<ExternalInvoice> invoices(@RequestBody InvoicesRequest request);

    record PayRequest(UUID paymentId, String integration, String providerCode, BigDecimal amount, Map<String, String> fields,
                      boolean issueVoucher) {
    }

    record InvoicesRequest(String providerCode, ProviderGroup group, Map<String, String> fields) {
    }
}
