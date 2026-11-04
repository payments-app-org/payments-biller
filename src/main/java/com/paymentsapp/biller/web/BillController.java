package com.paymentsapp.biller.web;

import com.paymentsapp.biller.bill.BillCheck;
import com.paymentsapp.biller.bill.BillCheckRequest;
import com.paymentsapp.biller.bill.BillPaymentService;
import com.paymentsapp.biller.bill.BillService;
import com.paymentsapp.biller.bill.GatewayClient.GatewayResult;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Платежи поставщикам (тип BILL в payment): проверка перед созданием платежа и оплата у поставщика — шаг саги
@RestController
@RequestMapping("/api/biller")
public class BillController {

    private final BillService bills;
    private final BillPaymentService payments;

    public BillController(BillService bills, BillPaymentService payments) {
        this.bills = bills;
        this.payments = payments;
    }

    @PostMapping("/bills/check")
    public BillCheck check(@RequestBody BillCheckRequest req) {
        return bills.check(req);
    }

    @PostMapping("/payments")
    public GatewayResult pay(@RequestBody PayRequest req) {
        return payments.pay(req.paymentId(), req.providerCode(), req.amount(), req.fields() == null ? Map.of() : req.fields());
    }

    @GetMapping("/payments/{paymentId}")
    public GatewayResult find(@PathVariable UUID paymentId) {
        return payments.find(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Оплата у поставщика не найдена"));
    }

    public record PayRequest(UUID paymentId, String providerCode, BigDecimal amount, Map<String, String> fields) {
    }
}
