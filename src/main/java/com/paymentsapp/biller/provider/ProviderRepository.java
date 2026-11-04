package com.paymentsapp.biller.provider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
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

    public Optional<GovDetails> gov(int providerId) {
        return jdbc.sql("select * from gov_providers where provider_id = :id").param("id", providerId)
                .query((rs, n) -> new GovDetails(rs.getString("lookup"), rs.getString("charge_kind"),
                        rs.getString("uin_prefix"), rs.getString("description"), rs.getString("payee_name"),
                        rs.getString("payee_inn"), rs.getString("payee_kpp"), rs.getString("payee_account"),
                        rs.getString("payee_bik"), rs.getString("kbk"), rs.getString("oktmo"), rs.getString("purpose")))
                .optional();
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
                rs.getString("title"), rs.getString("legal_name"), rs.getString("domain"), rs.getString("color"));
    }
}
