package com.paymentsapp.biller.provider;

import com.paymentsapp.biller.provider.ProviderDetails.Field;
import com.paymentsapp.biller.provider.ProviderDetails.ForeignMobile;
import com.paymentsapp.biller.provider.ProviderDetails.Game;
import com.paymentsapp.biller.provider.ProviderDetails.Nominal;
import com.paymentsapp.biller.provider.ProviderDetails.Option;
import com.paymentsapp.biller.provider.ProviderDetails.Transport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProviderRepository {

    private static final String SELECT = "select p.* from providers p where p.enabled ";

    private final JdbcClient jdbc;

    public ProviderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Поиск по всем поставщикам: по названию, заголовку формы и юрлицу. group — только одна группа.
    public List<Provider> search(String query, String group) {
        String q = query == null ? "" : query.trim();
        String g = group == null ? "" : group.trim();
        return jdbc.sql(SELECT + """
                        and (:g = '' or p.group_code = :g)
                        and (:q = '' or p.name ilike '%' || :q || '%' or p.title ilike '%' || :q || '%'
                             or p.legal_name ilike '%' || :q || '%')
                        order by (p.name ilike :q || '%') desc,
                                 (select sort_order from provider_groups pg where pg.code = p.group_code), p.sort_order, p.name
                        limit 30
                        """)
                .param("q", q).param("g", g)
                .query(ProviderRepository::provider).list();
    }

    public Optional<Provider> byCode(String code) {
        return jdbc.sql(SELECT + "and p.code = :code").param("code", code).query(ProviderRepository::provider).optional();
    }

    public Optional<ProviderDetails> details(String code) {
        return byCode(code).map(p -> new ProviderDetails(p, fields(p.id()), nominals(p.id()), gov(p.id()).orElse(null),
                transport(p.id()), game(p.id()), foreignMobile(p.id())));
    }

    public Optional<GovDetails> gov(int providerId) {
        return jdbc.sql("select * from gov_providers where provider_id = :id").param("id", providerId)
                .query((rs, n) -> new GovDetails(rs.getString("lookup"), rs.getString("charge_kind"),
                        rs.getString("uin_prefix"), rs.getString("description"), rs.getString("payee_name"),
                        rs.getString("payee_inn"), rs.getString("payee_kpp"), rs.getString("payee_account"),
                        rs.getString("payee_bik"), rs.getString("kbk"), rs.getString("oktmo"), rs.getString("purpose")))
                .optional();
    }

    private List<Field> fields(int providerId) {
        Map<Integer, List<Option>> options = jdbc.sql("""
                        select o.* from provider_field_options o join provider_fields f on f.id = o.field_id
                        where f.provider_id = :id order by o.sort_order
                        """)
                .param("id", providerId)
                .query((rs, n) -> Map.entry(rs.getInt("field_id"),
                        new Option(rs.getString("value"), rs.getString("label"), rs.getString("country_code"))))
                .list().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        return jdbc.sql("select * from provider_fields where provider_id = :id order by sort_order").param("id", providerId)
                .query((rs, n) -> new Field(rs.getString("key"), rs.getString("label"), rs.getString("type"),
                        rs.getBoolean("required"), rs.getString("placeholder"), rs.getString("hint"), rs.getString("info"),
                        rs.getString("pattern"), rs.getString("depends_on"), options.getOrDefault(rs.getInt("id"), List.of())))
                .list();
    }

    private List<Nominal> nominals(int providerId) {
        return jdbc.sql("select * from game_nominals where provider_id = :id order by region nulls first, sort_order")
                .param("id", providerId)
                .query((rs, n) -> new Nominal(rs.getInt("id"), rs.getString("region"), rs.getString("name"), rs.getBigDecimal("price")))
                .list();
    }

    private Transport transport(int providerId) {
        return jdbc.sql("select * from transport_providers where provider_id = :id").param("id", providerId)
                .query((rs, n) -> new Transport(rs.getString("kind"), rs.getString("region"))).optional().orElse(null);
    }

    private Game game(int providerId) {
        return jdbc.sql("select * from game_providers where provider_id = :id").param("id", providerId)
                .query((rs, n) -> new Game(rs.getString("kind"), rs.getString("platform"), rs.getString("delivery_note")))
                .optional().orElse(null);
    }

    private ForeignMobile foreignMobile(int providerId) {
        return jdbc.sql("select * from foreign_mobile_providers where provider_id = :id").param("id", providerId)
                .query((rs, n) -> new ForeignMobile(rs.getString("dial_code"), rs.getInt("number_length"),
                        rs.getString("prefixes") == null ? List.of() : Arrays.asList(rs.getString("prefixes").split(",")),
                        rs.getString("operators"), rs.getString("currency")))
                .optional().orElse(null);
    }

    // Оператор по номеру: +7 DEF XXXXXXX → диапазон из реестра нумерации
    public Optional<Provider> mobileOperator(String defCode, int number) {
        return jdbc.sql("""
                        select p.* from mobile_number_ranges r join providers p on p.id = r.provider_id
                        where p.enabled and r.def_code = :def and :number between r.number_from and r.number_to
                        """)
                .param("def", defCode).param("number", number)
                .query(ProviderRepository::provider).optional();
    }

    private static Provider provider(ResultSet rs, int n) throws SQLException {
        return new Provider(rs.getInt("id"), rs.getString("code"), rs.getString("group_code"), rs.getString("name"),
                rs.getString("title"), rs.getString("legal_name"), rs.getString("domain"), rs.getString("color"),
                rs.getString("country_code"), rs.getBigDecimal("min_amount"), rs.getBigDecimal("max_amount"));
    }
}
