package com.paymentsapp.biller.bill;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// Счета, найденные у поставщика (Образование Москвы и др.). Повторный поиск обновляет неоплаченные.
@Repository
public class InvoiceRepository {

    private final JdbcClient jdbc;

    public InvoiceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert(int providerId, GatewayClient.ExternalInvoice e) {
        jdbc.sql("""
                        insert into provider_invoices (id, provider_id, external_id, title, description, amount, due_date)
                        values (:id, :provider, :ext, :title, :description, :amount, :due)
                        on conflict (external_id) do update set title = excluded.title, description = excluded.description,
                            amount = excluded.amount, due_date = excluded.due_date, fetched_at = now()
                        where provider_invoices.status = 'UNPAID'
                        """)
                .param("id", UUID.randomUUID()).param("provider", providerId).param("ext", e.externalId())
                .param("title", e.title()).param("description", e.description()).param("amount", e.amount())
                .param("due", e.dueDate())
                .update();
    }

    public List<Invoice> unpaid(List<String> externalIds) {
        if (externalIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select * from provider_invoices where external_id in (:ids) and status = 'UNPAID' order by due_date nulls last, title")
                .param("ids", externalIds).query(InvoiceRepository::map).list();
    }

    public Optional<Invoice> get(UUID id) {
        return jdbc.sql("select * from provider_invoices where id = :id").param("id", id).query(InvoiceRepository::map).optional();
    }

    public int markPaid(UUID id) {
        return jdbc.sql("update provider_invoices set status = 'PAID' where id = :id and status = 'UNPAID'").param("id", id).update();
    }

    private static Invoice map(ResultSet rs, int n) throws SQLException {
        return new Invoice(rs.getObject("id", UUID.class), rs.getInt("provider_id"), rs.getString("title"),
                rs.getString("description"), rs.getBigDecimal("amount"), rs.getObject("due_date", LocalDate.class),
                rs.getString("status"));
    }

    public record Invoice(UUID id, int providerId, String title, String description, BigDecimal amount, LocalDate dueDate,
                          String status) {
    }
}
