package com.paymentsapp.biller.subscription;

import static com.paymentsapp.biller.bill.BillException.Code.INVALID_REQUISITES;

import com.paymentsapp.biller.bill.BillException;
import com.paymentsapp.biller.bill.BillService;
import com.paymentsapp.biller.bill.InvoiceService;
import com.paymentsapp.biller.gov.GovChargeService;
import com.paymentsapp.biller.gov.PayerIds;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderDetails.Field;
import com.paymentsapp.biller.provider.ProviderRepository;
import com.paymentsapp.biller.subscription.Subscription.Bill;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

// «Счета на оплату»: шаблоны пользователя и проверка неоплаченных счетов у поставщика.
// Госуслуги — начисления ГИС ГМП по документам плательщика, остальные — счета из шлюза поставщика.
@Service
public class SubscriptionService {

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final ProviderRepository providers;
    private final BillService bills;
    private final InvoiceService invoices;
    private final GovChargeService charges;

    public SubscriptionService(JdbcClient jdbc, JsonMapper json, ProviderRepository providers, BillService bills,
                               InvoiceService invoices, GovChargeService charges) {
        this.jdbc = jdbc;
        this.json = json;
        this.providers = providers;
        this.bills = bills;
        this.invoices = invoices;
        this.charges = charges;
    }

    // Поставщики, у которых можно подписаться на счета (с учётом региона)
    public List<Provider> eligible(String region) {
        String r = region == null ? "" : region.trim();
        List<String> codes = jdbc.sql("""
                        select p.code from providers p where p.enabled and p.bills_order is not null
                          and (:r = '' or not exists (select 1 from provider_regions pr where pr.provider_id = p.id)
                               or exists (select 1 from provider_regions pr where pr.provider_id = p.id and pr.region_code = :r))
                        order by p.bills_order
                        """)
                .param("r", r).query(String.class).list();
        return codes.stream().map(c -> providers.byCode(c).orElseThrow()).toList();
    }

    // Список шаблонов; каждый заново проверяем у поставщика
    public List<Subscription> list(UUID userId) {
        return ids(userId).stream().map(id -> withBills(userId, id).subscription()).toList();
    }

    public Subscription.WithBills create(UUID userId, String providerCode, String name, Map<String, String> fields) {
        ProviderDetails d = eligibleDetails(providerCode);
        Map<String, String> values = checkFields(d, fields);
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                        insert into bill_subscriptions (id, user_id, provider_id, name, fields)
                        values (:id, :user, :provider, :name, cast(:fields as jsonb))
                        """)
                .param("id", id).param("user", userId).param("provider", d.provider().id())
                .param("name", checkName(name)).param("fields", json.writeValueAsString(values))
                .update();
        return withBills(userId, id);
    }

    public Subscription.WithBills update(UUID userId, UUID id, String name, Map<String, String> fields) {
        Subscription s = get(userId, id);
        ProviderDetails d = eligibleDetails(s.provider().code());
        Map<String, String> values = checkFields(d, fields);
        jdbc.sql("update bill_subscriptions set name = :name, fields = cast(:fields as jsonb), updated_at = now() where id = :id")
                .param("id", id).param("name", checkName(name)).param("fields", json.writeValueAsString(values))
                .update();
        return withBills(userId, id);
    }

    public void delete(UUID userId, UUID id) {
        get(userId, id);
        jdbc.sql("delete from bill_subscriptions where id = :id").param("id", id).update();
    }

    // Шаблон и его неоплаченные счета у поставщика прямо сейчас; итог проверки запоминаем
    public Subscription.WithBills withBills(UUID userId, UUID id) {
        Subscription s = get(userId, id);
        ProviderDetails d = providers.details(s.provider().code())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Поставщик не найден"));
        List<Bill> found = "GOV".equals(d.provider().groupCode())
                ? charges.find(d.provider().id(), d.gov(), PayerIds.check(d.gov().lookup(), d.gov().uinPrefix(), s.fields()))
                        .stream().map(c -> new Bill(c.id(), "CHARGE", c.purpose(), c.payeeName() + " · УИН " + c.uin(),
                                c.payableAmount(), c.amount(), c.discountUntil() == null ? null : c.discountUntil().toString(),
                                null)).toList()
                : invoices.fetch(d, s.fields()).stream().map(i -> new Bill(i.id(), "INVOICE", i.title(), i.description(),
                        i.amount(), i.amount(), null, i.dueDate() == null ? null : i.dueDate().toString())).toList();
        BigDecimal total = found.stream().map(Bill::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        jdbc.sql("update bill_subscriptions set checked_at = now(), unpaid_count = :count, unpaid_total = :total where id = :id")
                .param("id", id).param("count", found.size()).param("total", total).update();
        return new Subscription.WithBills(get(userId, id), found);
    }

    public Subscription get(UUID userId, UUID id) {
        return jdbc.sql("select * from bill_subscriptions where id = :id and user_id = :user")
                .param("id", id).param("user", userId).query(this::map).optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Шаблон не найден"));
    }

    private List<UUID> ids(UUID userId) {
        return jdbc.sql("select id from bill_subscriptions where user_id = :user order by created_at")
                .param("user", userId).query(UUID.class).list();
    }

    private ProviderDetails eligibleDetails(String code) {
        Optional<Provider> p = eligible("").stream().filter(x -> x.code().equals(code)).findFirst();
        if (p.isEmpty()) {
            throw new BillException(INVALID_REQUISITES, "У этого поставщика нет счетов на оплату");
        }
        return providers.details(code).orElseThrow();
    }

    // Поля, по которым поставщик находит счета: у госуслуг — правила поиска начислений, у остальных — шаблон поставщика
    private Map<String, String> checkFields(ProviderDetails d, Map<String, String> input) {
        Map<String, String> raw = input == null ? Map.of() : input;
        List<Field> ident = d.fields().stream().filter(f -> !"PAY".equals(f.stage())).toList();
        Map<String, String> values = new LinkedHashMap<>();
        if ("GOV".equals(d.provider().groupCode())) {
            PayerIds.check(d.gov().lookup(), d.gov().uinPrefix(), raw).forEach((k, v) -> {
                if (v != null) {
                    values.put(k, v);
                }
            });
        } else {
            bills.validate(d, ident, raw).fields().forEach(f -> values.put(f.key(), f.value()));
        }
        return values;
    }

    private static String checkName(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty() || n.length() > 50) {
            throw new BillException(INVALID_REQUISITES, "Название шаблона — от 1 до 50 символов");
        }
        return n;
    }

    private Subscription map(ResultSet rs, int n) throws SQLException {
        int providerId = rs.getInt("provider_id");
        String code = jdbc.sql("select code from providers where id = :id").param("id", providerId).query(String.class).single();
        Map<String, String> fields = json.readValue(rs.getString("fields"), new TypeReference<Map<String, String>>() { });
        return new Subscription(rs.getObject("id", UUID.class), rs.getString("name"), providers.byCode(code).orElseThrow(),
                fields, rs.getObject("checked_at", OffsetDateTime.class), rs.getInt("unpaid_count"),
                rs.getBigDecimal("unpaid_total"));
    }
}
