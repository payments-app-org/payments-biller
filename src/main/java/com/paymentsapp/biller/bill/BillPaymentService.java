package com.paymentsapp.biller.bill;

import com.paymentsapp.biller.bill.GatewayClient.GatewayResult;
import com.paymentsapp.biller.bill.GatewayClient.GatewayStatus;
import com.paymentsapp.biller.error.BillerError;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderRepository;
import com.paymentsapp.platform.error.DomainException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

// Шаг саги payment «оплата у поставщика»: передаём платёж в шлюз поставщика и запоминаем ответ.
// Повтор с тем же paymentId не уходит в шлюз второй раз.
@Service
public class BillPaymentService {

    private final ProviderRepository providers;
    private final GatewayClient gateway;
    private final JdbcClient jdbc;
    private final JsonMapper json;

    public BillPaymentService(ProviderRepository providers, GatewayClient gateway, JdbcClient jdbc, JsonMapper json) {
        this.providers = providers;
        this.gateway = gateway;
        this.jdbc = jdbc;
        this.json = json;
    }

    public GatewayResult pay(UUID paymentId, String providerCode, BigDecimal amount, Map<String, String> fields) {
        Optional<GatewayResult> known = stored(paymentId);
        if (known.isPresent()) {
            return known.get();
        }
        ProviderDetails d = providers.details(providerCode)
                .orElseThrow(() -> new DomainException(BillerError.PROVIDER_NOT_FOUND));
        Provider p = d.provider();
        boolean voucher = d.game() != null && d.game().kind().issuesVoucher();
        String integration = jdbc.sql("select integration from providers where id = :id").param("id", p.id())
                .query(String.class).single();
        GatewayResult r = gateway.pay(paymentId, integration, p.code(), amount, fields, voucher);
        save(paymentId, p.id(), amount, fields, r);
        return stored(paymentId).orElse(r);
    }

    // Чем закончилась оплата: у себя, а если ответ шлюза до нас не дошёл или оплата долгая (PENDING) — спрашиваем шлюз.
    // Окончательный итог запоминаем.
    public Optional<GatewayResult> find(UUID paymentId) {
        Optional<GatewayResult> known = stored(paymentId);
        if (known.isPresent() && known.get().status() != GatewayStatus.PENDING) {
            return known;
        }
        Optional<GatewayResult> fresh = gateway.find(paymentId);
        fresh.filter(r -> r.status() != GatewayStatus.PENDING).filter(r -> known.isPresent()).ifPresent(this::complete);
        return fresh.or(() -> known);
    }

    private void complete(GatewayResult r) {
        jdbc.sql("""
                        update biller_payments set status = :status, external_id = :ext, voucher_code = :voucher, message = :message
                        where payment_id = :id and status = 'PENDING'
                        """)
                .param("id", r.paymentId()).param("status", r.status().name()).param("ext", r.externalId())
                .param("voucher", r.voucherCode()).param("message", r.message())
                .update();
    }

    private void save(UUID paymentId, int providerId, BigDecimal amount, Map<String, String> fields, GatewayResult r) {
        jdbc.sql("""
                        insert into biller_payments (payment_id, provider_id, amount, fields, status, external_id, voucher_code, message)
                        values (:id, :provider, :amount, cast(:fields as jsonb), :status, :ext, :voucher, :message)
                        on conflict (payment_id) do nothing
                        """)
                .param("id", paymentId).param("provider", providerId).param("amount", amount)
                .param("fields", json.writeValueAsString(fields)).param("status", r.status().name())
                .param("ext", r.externalId()).param("voucher", r.voucherCode()).param("message", r.message())
                .update();
    }

    private Optional<GatewayResult> stored(UUID paymentId) {
        return jdbc.sql("select * from biller_payments where payment_id = :id").param("id", paymentId)
                .query((rs, n) -> new GatewayResult(paymentId, GatewayStatus.valueOf(rs.getString("status")), rs.getString("external_id"),
                        rs.getString("voucher_code"), rs.getString("message")))
                .optional();
    }
}
