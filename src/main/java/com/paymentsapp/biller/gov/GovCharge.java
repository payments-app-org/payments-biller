package com.paymentsapp.biller.gov;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Начисление по УИН. payableAmount — сколько платить сегодня (со скидкой, если она ещё действует).
public record GovCharge(UUID id, String uin, String purpose, String payeeName, String payeeInn, String payeeKpp,
                        String payeeAccount, String payeeBik, String kbk, String oktmo, BigDecimal amount,
                        BigDecimal discountedAmount, LocalDate discountUntil, LocalDate issuedAt, String status,
                        BigDecimal payableAmount) {
}
