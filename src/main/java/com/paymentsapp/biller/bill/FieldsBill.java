package com.paymentsapp.biller.bill;

import static com.paymentsapp.biller.error.BillerError.*;

import com.paymentsapp.biller.bill.BillCheck.BillField;
import com.paymentsapp.biller.bill.InvoiceRepository.Invoice;
import com.paymentsapp.biller.provider.FieldStage;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderDetails.Field;
import com.paymentsapp.biller.provider.ProviderDetails.Nominal;
import com.paymentsapp.platform.error.DomainException;
import com.paymentsapp.platform.text.Rubles;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Остальные группы (транспорт, игры, зарубежная связь, ЖКХ, образование, …): поля по шаблону поставщика.
// Найденный у поставщика счёт (chargeId = id счёта) платим ровно на его сумму; номинал — по цене номинала;
// иначе — сумма плательщика в границах поставщика.
@Component
@Order(Integer.MAX_VALUE)
class FieldsBill implements BillMethod {

    private static final String PHONE = MobileBill.PHONE;
    private static final String INVOICE_PURPOSE = "%s — %s";
    private static final String MAIN_PURPOSE = "%s · %s";
    private static final String NO_LIMIT = "—";
    // Телефон из поля формы — номер для связи в платеже (не длиннее колонки)
    private static final int PHONE_MAX = 20;

    private final FieldValidator validator;
    private final InvoiceRepository invoices;

    FieldsBill(FieldValidator validator, InvoiceRepository invoices) {
        this.validator = validator;
        this.invoices = invoices;
    }

    @Override
    public boolean supports(ProviderDetails provider, BillCheckRequest request) {
        return true;
    }

    @Override
    public BillCheck check(ProviderDetails d, BillCheckRequest req) {
        Provider p = d.provider();
        BigDecimal amount = req.amountOrZero();
        UUID invoiceId = req.chargeId();
        boolean hasSearch = d.fields().stream().anyMatch(f -> f.stage() == FieldStage.SEARCH);
        // Без найденного счёта у поставщика с поиском — оплата суммы плательщика. У ЖКХ Москвы («Оплатить») —
        // только поля оплаты, у остальных (ГИС ЖКХ, Образование Москвы; шаблоны и автоплатежи) — все поля.
        boolean payFieldsOnly = hasSearch && invoiceId == null && d.utility() != null && d.utility().directPay();
        List<Field> checked = payFieldsOnly ? d.fields().stream().filter(Field::payable).toList()
                : invoiceId != null && !hasSearch ? d.fields().stream().filter(Field::searchable).toList()
                : d.fields();
        FieldValidator.Validated v = validator.validate(d, checked, req.fieldsOrEmpty());
        Nominal nominal = v.nominal();
        String billReference = null;
        String purpose;
        if (invoiceId != null) {
            Invoice inv = invoices.get(invoiceId).filter(i -> i.providerId() == p.id())
                    .orElseThrow(() -> new DomainException(INVOICE_NOT_FOUND));
            if (inv.status() != BillStatus.UNPAID) {
                throw new DomainException(INVOICE_PAID);
            }
            if (inv.amount().compareTo(amount) != 0) {
                throw new DomainException(INVOICE_AMOUNT, Rubles.format(inv.amount()));
            }
            billReference = BillReference.invoice(inv.id());
            purpose = INVOICE_PURPOSE.formatted(p.name(), inv.title());
        } else if (nominal != null) {
            if (nominal.price().compareTo(amount) != 0) {
                throw new DomainException(NOMINAL_PRICE, Rubles.format(nominal.price()));
            }
            // «Genshin Impact — 300 + 30 кристаллов»
            purpose = INVOICE_PURPOSE.formatted(p.name(), nominal.name());
        } else {
            boolean tooLow = p.minAmount() != null && amount.compareTo(p.minAmount()) < 0;
            boolean tooHigh = p.maxAmount() != null && amount.compareTo(p.maxAmount()) > 0;
            if (tooLow || tooHigh) {
                throw new DomainException(AMOUNT_RANGE, rubOrDash(p.minAmount()), rubOrDash(p.maxAmount()));
            }
            // «Карта Тройка · 1234567890»
            purpose = v.main() != null ? MAIN_PURPOSE.formatted(p.name(), v.main()) : p.name();
        }
        String phone = v.fields().stream().filter(x -> x.key().equals(PHONE)).map(BillField::display).findFirst().orElse(null);
        return new BillCheck(p.code(), p.groupCode(), p.name(), purpose, v.fields(), null, billReference,
                phone != null && phone.length() <= PHONE_MAX ? phone.replace(" ", "").replace("-", "") : null);
    }

    private static String rubOrDash(BigDecimal value) {
        return value == null ? NO_LIMIT : Rubles.format(value);
    }
}
