package com.paymentsapp.biller.gov;

import com.paymentsapp.biller.gov.GisGmpClient.ExternalCharge;
import com.paymentsapp.biller.provider.GovDetails;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Начисления госуслуг: спрашиваем ГИС ГМП и сохраняем у себя (повторный поиск обновит неоплаченные)
@Service
public class GovChargeService {

    private final GisGmpClient gisGmp;
    private final JdbcClient jdbc;

    public GovChargeService(GisGmpClient gisGmp, JdbcClient jdbc) {
        this.gisGmp = gisGmp;
        this.jdbc = jdbc;
    }

    // ids — уже проверенные идентификаторы поиска (uin | sts, vu | inn | passport, inn)
    @Transactional
    public List<GovCharge> find(int providerId, GovDetails gov, Map<String, String> ids) {
        Map<String, String> query = new LinkedHashMap<>(ids);
        query.put("kind", gov.chargeKind());
        List<String> found = new ArrayList<>();
        for (ExternalCharge c : gisGmp.charges(query)) {
            jdbc.sql("""
                            insert into gov_charges (id, provider_id, external_id, uin, purpose, payee_name, payee_inn,
                                payee_kpp, payee_account, payee_bik, kbk, oktmo, amount, discounted_amount, discount_until, issued_at)
                            values (:id, :provider, :ext, :uin, :purpose, :payee, :inn, :kpp, :account, :bik, :kbk, :oktmo,
                                :amount, :discounted, :until, :issued)
                            on conflict (external_id) do update set amount = excluded.amount,
                                discounted_amount = excluded.discounted_amount, discount_until = excluded.discount_until,
                                fetched_at = now()
                            where gov_charges.status = 'UNPAID'
                            """)
                    .param("id", UUID.randomUUID()).param("provider", providerId).param("ext", c.externalId())
                    .param("uin", c.uin()).param("purpose", c.purpose()).param("payee", c.payeeName())
                    .param("inn", c.payeeInn()).param("kpp", c.payeeKpp()).param("account", c.payeeAccount())
                    .param("bik", c.payeeBik()).param("kbk", c.kbk()).param("oktmo", c.oktmo())
                    .param("amount", c.amount()).param("discounted", c.discountedAmount())
                    .param("until", c.discountUntil()).param("issued", c.issuedAt())
                    .update();
            found.add(c.externalId());
        }
        if (found.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select * from gov_charges where external_id in (:ids) and status = 'UNPAID' order by issued_at desc")
                .param("ids", found).query(GovChargeService::map).list();
    }

    public Optional<GovCharge> get(UUID id) {
        return jdbc.sql("select * from gov_charges where id = :id").param("id", id).query(GovChargeService::map).optional();
    }

    private static GovCharge map(ResultSet rs, int n) throws SQLException {
        BigDecimal amount = rs.getBigDecimal("amount");
        BigDecimal discounted = rs.getBigDecimal("discounted_amount");
        LocalDate until = rs.getObject("discount_until", LocalDate.class);
        BigDecimal payable = discounted != null && until != null && !LocalDate.now().isAfter(until) ? discounted : amount;
        return new GovCharge(rs.getObject("id", UUID.class), rs.getString("uin"), rs.getString("purpose"),
                rs.getString("payee_name"), rs.getString("payee_inn"), rs.getString("payee_kpp"),
                rs.getString("payee_account"), rs.getString("payee_bik"), rs.getString("kbk"), rs.getString("oktmo"),
                amount, discounted, until, rs.getObject("issued_at", LocalDate.class), rs.getString("status"), payable);
    }
}
