package com.paymentsapp.biller.charity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Перевод фондам: копилку за день (округления + пожертвования) отправляем в процессинг одним переводом на фонд,
// а не тысячами мелких. Запускается в конце дня по расписанию; неудавшиеся переводы повторяем при следующем прогоне.
@Service
public class CharitySettlementService {

    private static final Logger log = LoggerFactory.getLogger(CharitySettlementService.class);
    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final RestClient processing;

    public CharitySettlementService(JdbcClient jdbc, TransactionTemplate tx, RestClient.Builder builder,
                                    @Value("${integrations.processing.url}") String processingUrl) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.processing = builder.baseUrl(processingUrl).build();
    }

    @Scheduled(cron = "${charity.settle-cron}", zone = "Europe/Moscow")
    public void endOfDay() {
        settle(LocalDate.now(MOSCOW));
    }

    // Всё накопленное по день включительно: по одному переводу на фонд. Возвращает созданные и повторённые переводы.
    public List<Settlement> settle(LocalDate upTo) {
        List<Integer> funds = jdbc.sql("select distinct provider_id from charity_accruals where settlement_id is null and day <= :day")
                .param("day", upTo).query(Integer.class).list();
        for (Integer fund : funds) {
            tx.executeWithoutResult(s -> {
                UUID id = UUID.randomUUID();
                Map<String, Object> sum = jdbc.sql("""
                                select coalesce(sum(amount), 0) as amount, count(*) as cnt from charity_accruals
                                where provider_id = :fund and settlement_id is null and day <= :day
                                """)
                        .param("fund", fund).param("day", upTo).query().singleRow();
                BigDecimal amount = (BigDecimal) sum.get("amount");
                if (amount.signum() <= 0) {
                    return;
                }
                jdbc.sql("""
                                insert into charity_settlements (id, provider_id, day, amount, accruals, status)
                                values (:id, :fund, :day, :amount, :cnt, 'SENT')
                                """)
                        .param("id", id).param("fund", fund).param("day", upTo).param("amount", amount)
                        .param("cnt", ((Number) sum.get("cnt")).intValue()).update();
                jdbc.sql("update charity_accruals set settlement_id = :id where provider_id = :fund and settlement_id is null and day <= :day")
                        .param("id", id).param("fund", fund).param("day", upTo).update();
            });
        }
        // Отправляем новые и повторяем те, что не дошли
        List<UUID> toSend = jdbc.sql("select id from charity_settlements where status in ('SENT', 'FAILED') order by created_at")
                .query(UUID.class).list();
        for (UUID id : toSend) {
            send(id);
        }
        return recent();
    }

    private void send(UUID settlementId) {
        Map<String, Object> s = jdbc.sql("""
                        select s.amount, p.name, p.code, c.inn from charity_settlements s
                        join providers p on p.id = s.provider_id join charity_providers c on c.provider_id = p.id
                        where s.id = :id
                        """)
                .param("id", settlementId).query().singleRow();
        try {
            Map<?, ?> r = processing.post().uri("/api/processing/charity-transfers")
                    .body(Map.of("settlementId", settlementId, "fundCode", s.get("code"), "fundName", s.get("name"),
                            "inn", s.get("inn"), "amount", s.get("amount")))
                    .retrieve().body(Map.class);
            boolean done = r != null && "COMPLETED".equals(r.get("status"));
            jdbc.sql("update charity_settlements set status = :status, processing_ref = :ref, message = :msg, finished_at = now() where id = :id")
                    .param("id", settlementId).param("status", done ? "DONE" : "FAILED")
                    .param("ref", r == null ? null : r.get("externalId")).param("msg", r == null ? null : r.get("reason"))
                    .update();
            log.info("фонд {}: перевод {} ₽ — {}", s.get("name"), s.get("amount"), done ? "отправлен" : "отклонён");
        } catch (RestClientException e) {
            log.warn("фонд {}: процессинг недоступен, повторим при следующем прогоне: {}", s.get("name"), e.getMessage());
            jdbc.sql("update charity_settlements set status = 'FAILED', message = 'Процессинг недоступен' where id = :id")
                    .param("id", settlementId).update();
        }
    }

    // Копилка и переводы по фондам — для проверки и для экрана
    public List<Settlement> recent() {
        return jdbc.sql("""
                        select s.id, p.code, p.name, s.day, s.amount, s.accruals, s.status, s.processing_ref
                        from charity_settlements s join providers p on p.id = s.provider_id
                        order by s.created_at desc limit 50
                        """)
                .query((rs, n) -> new Settlement(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                        rs.getObject("day", LocalDate.class), rs.getBigDecimal("amount"), rs.getInt("accruals"),
                        rs.getString("status"), rs.getString("processing_ref")))
                .list();
    }

    public List<Pending> pending() {
        return jdbc.sql("""
                        select p.code, p.name, a.source, sum(a.amount) as amount, count(*) as cnt
                        from charity_accruals a join providers p on p.id = a.provider_id
                        where a.settlement_id is null group by p.code, p.name, a.source order by p.name, a.source
                        """)
                .query((rs, n) -> new Pending(rs.getString("code"), rs.getString("name"), rs.getString("source"),
                        rs.getBigDecimal("amount"), rs.getInt("cnt")))
                .list();
    }

    public record Settlement(UUID id, String fundCode, String fundName, LocalDate day, BigDecimal amount, int accruals,
                             String status, String processingRef) {
    }

    public record Pending(String fundCode, String fundName, String source, BigDecimal amount, int payments) {
    }
}
