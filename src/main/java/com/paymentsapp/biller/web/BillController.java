package com.paymentsapp.biller.web;

import com.paymentsapp.biller.bill.BillCheck;
import com.paymentsapp.biller.bill.BillCheckRequest;
import com.paymentsapp.biller.bill.BillPaymentService;
import com.paymentsapp.biller.bill.BillService;
import com.paymentsapp.biller.bill.GatewayClient.GatewayResult;
import com.paymentsapp.biller.bill.InvoiceRepository.Invoice;
import com.paymentsapp.biller.bill.InvoiceService;
import com.paymentsapp.biller.error.BillerError;
import com.paymentsapp.platform.error.DomainException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Платежи поставщикам (тип BILL в payment): проверка перед созданием платежа и оплата у поставщика — шаг саги
@RestController
@RequestMapping("/api/biller")
public class BillController {

    private final BillService bills;
    private final BillPaymentService payments;
    private final InvoiceService invoices;

    public BillController(BillService bills, BillPaymentService payments, InvoiceService invoices) {
        this.bills = bills;
        this.payments = payments;
        this.invoices = invoices;
    }

    // «Найти»: счета у поставщика по полям поиска (stage = SEARCH)
    @PostMapping("/invoices/search")
    public List<Invoice> searchInvoices(@RequestBody SearchRequest req) {
        return invoices.search(req.providerCode(), req.fields());
    }

    public record SearchRequest(String providerCode, Map<String, String> fields) {
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
                .orElseThrow(() -> new DomainException(BillerError.BILL_PAYMENT_NOT_FOUND));
    }

    public record PayRequest(UUID paymentId, String providerCode, BigDecimal amount, Map<String, String> fields) {
    }
}
