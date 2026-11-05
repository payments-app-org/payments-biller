package com.paymentsapp.biller.charity;

import com.paymentsapp.biller.client.Backends;
import com.paymentsapp.biller.client.ProcessingApi;
import com.paymentsapp.biller.client.ProcessingApi.CharityTransfer;
import com.paymentsapp.biller.client.ProcessingApi.CharityTransferResult;
import com.paymentsapp.platform.resilience.ResilientCalls;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

// Фондам — одним переводом за день на фонд (округления + пожертвования), а не тысячами мелких.
// Запускается в конце дня по расписанию; неудавшиеся переводы повторяем при следующем прогоне.
@Service
public class CharitySettlementService {

    private static final Logger log = LoggerFactory.getLogger(CharitySettlementService.class);
    private static final String PROCESSING_DOWN = "Процессинг недоступен";
    private static final int RECENT_LIMIT = 50;

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final ProcessingApi processing;
    private final ResilientCalls calls;
    private final Clock clock;

    public CharitySettlementService(JdbcClient jdbc, TransactionTemplate tx, ProcessingApi processing, ResilientCalls calls,
                                    Clock clock) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.processing = processing;
        this.calls = calls;
        this.clock = clock;
    }

    // При нескольких подах расчёт выполняет только один (ShedLock)
    @Scheduled(cron = "${charity.settle-cron}", zone = "${platform.time.zone}")
    @SchedulerLock(name = "charity-settlement")
    public void endOfDay() {
        settle(LocalDate.now(clock));
    }

    // Всё накопленное по день включительно: по одному переводу на фонд. Возвращает созданные и повторённые переводы.
    public List<Settlement> settle(LocalDate upTo) {
        List<Integer> funds = jdbc.sql("select distinct provider_id from charity_accruals where settlement_id is null and day <= :day")
                .param("day", upTo).query(Integer.class).list();
        for (Integer fund : funds) {
            tx.executeWithoutResult(s -> createSettlement(fund, upTo));
        }
        // Отправляем новые и повторяем те, что не дошли
        List<UUID> toSend = jdbc.sql("select id from charity_settlements where status in (:statuses) order by created_at")
                .param("statuses", List.of(SettlementStatus.SENT.name(), SettlementStatus.FAILED.name()))
                .query(UUID.class).list();
        toSend.forEach(this::send);
        return recent();
    }

    private void createSettlement(int fund, LocalDate upTo) {
        Totals totals = jdbc.sql("""
                        select coalesce(sum(amount), 0) as amount, count(*) as accruals from charity_accruals
                        where provider_id = :fund and settlement_id is null and day <= :day
                        """)
                .param("fund", fund).param("day", upTo).query(Totals.class).single();
        if (totals.amount().signum() <= 0) {
            return;
        }
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                        insert into charity_settlements (id, provider_id, day, amount, accruals, status)
                        values (:id, :fund, :day, :amount, :accruals, :status)
                        """)
                .param("id", id).param("fund", fund).param("day", upTo).param("amount", totals.amount())
                .param("accruals", totals.accruals()).param("status", SettlementStatus.SENT.name()).update();
        jdbc.sql("update charity_accruals set settlement_id = :id where provider_id = :fund and settlement_id is null and day <= :day")
                .param("id", id).param("fund", fund).param("day", upTo).update();
    }

    private void send(UUID settlementId) {
        Fund fund = jdbc.sql("""
                        select s.amount, p.name, p.code, c.inn from charity_settlements s
                        join providers p on p.id = s.provider_id join charity_providers c on c.provider_id = p.id
                        where s.id = :id
                        """)
                .param("id", settlementId).query(Fund.class).single();
        CharityTransfer transfer = new CharityTransfer(settlementId, fund.code(), fund.name(), fund.inn(), fund.amount());
        try {
            // Перевод идемпотентен по settlementId — повтор при сбое связи безопасен
            CharityTransferResult r = calls.call(Backends.PROCESSING, () -> processing.charityTransfer(transfer));
            SettlementStatus status = r != null && r.completed() ? SettlementStatus.DONE : SettlementStatus.FAILED;
            jdbc.sql("update charity_settlements set status = :status, processing_ref = :ref, message = :msg, finished_at = :now where id = :id")
                    .param("id", settlementId).param("status", status.name())
                    .param("ref", r == null ? null : r.externalId()).param("msg", r == null ? null : r.reason())
                    .param("now", OffsetDateTime.now(clock))
                    .update();
            log.info("фонд {}: перевод {} ₽ — {}", fund.name(), fund.amount(), status);
        } catch (RestClientException e) {
            log.warn("фонд {}: процессинг недоступен, повторим при следующем прогоне: {}", fund.name(), e.getMessage());
            jdbc.sql("update charity_settlements set status = :status, message = :msg where id = :id")
                    .param("id", settlementId).param("status", SettlementStatus.FAILED.name()).param("msg", PROCESSING_DOWN)
                    .update();
        }
    }

    // Копилка и переводы по фондам — для проверки и для экрана
    public List<Settlement> recent() {
        return jdbc.sql("""
                        select s.id, p.code as fund_code, p.name as fund_name, s.day, s.amount, s.accruals, s.status,
                               s.processing_ref
                        from charity_settlements s join providers p on p.id = s.provider_id
                        order by s.created_at desc limit :limit
                        """)
                .param("limit", RECENT_LIMIT)
                .query(Settlement.class).list();
    }

    public List<Pending> pending() {
        return jdbc.sql("""
                        select p.code as fund_code, p.name as fund_name, a.source, sum(a.amount) as amount, count(*) as payments
                        from charity_accruals a join providers p on p.id = a.provider_id
                        where a.settlement_id is null group by p.code, p.name, a.source order by p.name, a.source
                        """)
                .query(Pending.class).list();
    }

    record Totals(BigDecimal amount, int accruals) {
    }

    record Fund(BigDecimal amount, String name, String code, String inn) {
    }

    public record Settlement(UUID id, String fundCode, String fundName, LocalDate day, BigDecimal amount, int accruals,
                             SettlementStatus status, String processingRef) {
    }

    public record Pending(String fundCode, String fundName, AccrualSource source, BigDecimal amount, int payments) {
    }
}
