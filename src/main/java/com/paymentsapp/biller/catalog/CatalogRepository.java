package com.paymentsapp.biller.catalog;

import com.paymentsapp.biller.cache.CacheNames;
import com.paymentsapp.biller.catalog.Catalog.Country;
import com.paymentsapp.biller.catalog.Catalog.Field;
import com.paymentsapp.biller.catalog.Catalog.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {

    private static final String COUNTRY_SELECT = """
            select c.*, (select string_agg(cc.currency, ',' order by cc.sort_order)
                         from country_currencies cc where cc.country_code = c.code) as currencies
            from countries c where c.enabled
            """;

    private final JdbcClient jdbc;

    public CatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Популярные сверху, дальше по алфавиту; поиск по русскому и местному названию и по коду
    @Cacheable(CacheNames.COUNTRIES)
    public List<Country> countries(String query) {
        String q = query == null ? "" : query.trim();
        return jdbc.sql(COUNTRY_SELECT + """
                        and (:q = '' or c.name_ru ilike '%' || :q || '%' or c.name_native ilike '%' || :q || '%' or c.code = upper(:q))
                        order by c.sort_order
                        """)
                .param("q", q)
                .query(CatalogRepository::country)
                .list();
    }

    @Cacheable(value = CacheNames.COUNTRY, unless = "#result == null")
    public Optional<Country> country(String code) {
        return jdbc.sql(COUNTRY_SELECT + " and c.code = :code").param("code", code.toUpperCase())
                .query(CatalogRepository::country).optional();
    }

    @Cacheable(CacheNames.METHODS)
    public List<Method> methods(String countryCode) {
        return jdbc.sql("select * from transfer_methods where country_code = :code and enabled order by sort_order")
                .param("code", countryCode.toUpperCase())
                .query(CatalogRepository::method)
                .list();
    }

    @Cacheable(value = CacheNames.METHOD, unless = "#result == null")
    public Optional<Method> method(int id) {
        return jdbc.sql("select * from transfer_methods where id = :id and enabled").param("id", id)
                .query(CatalogRepository::method).optional();
    }

    @Cacheable(CacheNames.METHOD_FIELDS)
    public List<Field> fields(String templateCode) {
        return jdbc.sql("select * from template_fields where template_code = :t order by sort_order")
                .param("t", templateCode)
                .query((rs, n) -> new Field(rs.getString("field_key"), rs.getString("label"), rs.getString("field_type"),
                        rs.getString("pattern"), rs.getString("placeholder")))
                .list();
    }

    private static Country country(ResultSet rs, int n) throws SQLException {
        String currencies = rs.getString("currencies");
        return new Country(rs.getString("code"), rs.getString("name_ru"), rs.getString("name_native"),
                rs.getString("dial_code"), rs.getBoolean("popular"),
                currencies == null ? List.of() : Arrays.asList(currencies.split(",")));
    }

    private static Method method(ResultSet rs, int n) throws SQLException {
        Catalog.MethodKind kind = Catalog.MethodKind.valueOf(rs.getString("kind"));
        return new Method(rs.getInt("id"), rs.getString("country_code"), rs.getString("name"), kind,
                kind.label(), rs.getString("color"), rs.getString("template_code"));
    }
}
