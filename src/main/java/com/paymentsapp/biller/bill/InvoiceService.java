package com.paymentsapp.biller.bill;

import static com.paymentsapp.biller.bill.BillException.Code.INVALID_REQUISITES;

import com.paymentsapp.biller.bill.InvoiceRepository.Invoice;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderDetails.Field;
import com.paymentsapp.biller.provider.ProviderRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// «Найти» у поставщика: проверяем поля поиска, спрашиваем шлюз поставщика и сохраняем счета у себя
@Service
public class InvoiceService {

    private final ProviderRepository providers;
    private final BillService bills;
    private final GatewayClient gateway;
    private final InvoiceRepository invoices;

    public InvoiceService(ProviderRepository providers, BillService bills, GatewayClient gateway, InvoiceRepository invoices) {
        this.providers = providers;
        this.bills = bills;
        this.gateway = gateway;
        this.invoices = invoices;
    }

    @Transactional
    public List<Invoice> search(String providerCode, Map<String, String> input) {
        ProviderDetails d = providers.details(providerCode == null ? "" : providerCode)
                .orElseThrow(() -> new BillException(INVALID_REQUISITES, "Поставщик не найден"));
        List<Field> searchFields = d.fields().stream().filter(f -> !"PAY".equals(f.stage())).toList();
        if (searchFields.stream().noneMatch(f -> "SEARCH".equals(f.stage()))) {
            throw new BillException(INVALID_REQUISITES, "У этого поставщика счета не ищутся");
        }
        Map<String, String> values = new LinkedHashMap<>();
        bills.validate(d, searchFields, input == null ? Map.of() : input).fields().forEach(f -> values.put(f.key(), f.value()));
        return fetch(d, values);
    }

    // Счета у поставщика по уже проверенным данным плательщика (поиск на форме или шаблон «Счета на оплату»)
    @Transactional
    public List<Invoice> fetch(ProviderDetails d, Map<String, String> values) {
        List<String> ids = new ArrayList<>();
        for (GatewayClient.ExternalInvoice e : gateway.invoices(d.provider().code(), d.provider().groupCode(), values)) {
            invoices.upsert(d.provider().id(), e);
            ids.add(e.externalId());
        }
        return invoices.unpaid(ids);
    }
}
