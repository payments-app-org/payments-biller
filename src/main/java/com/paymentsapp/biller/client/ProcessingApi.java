package com.paymentsapp.biller.client;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

// Процессинг: перевод фонду суммы, накопленной за день. Идемпотентен по settlementId.
@HttpExchange("/api/processing")
public interface ProcessingApi {

    @PostExchange("/charity-transfers")
    CharityTransferResult charityTransfer(@RequestBody CharityTransfer request);

    record CharityTransfer(UUID settlementId, String fundCode, String fundName, String inn, BigDecimal amount) {
    }

    enum TransferStatus {
        COMPLETED,
        DECLINED
    }

    record CharityTransferResult(UUID settlementId, TransferStatus status, String externalId, String reason) {

        public boolean completed() {
            return status == TransferStatus.COMPLETED;
        }
    }
}
