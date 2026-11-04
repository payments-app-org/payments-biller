package com.paymentsapp.biller.bill;

import java.util.List;
import java.util.Map;

// Результат проверки платежа поставщику — из этого payment создаёт платёж типа BILL.
// fields — что ввёл плательщик (value — для поставщика, display — для людей), requisites — получатель в бюджете,
// billReference — что нельзя оплатить дважды (УИН), recipientPhone — номер для связи.
public record BillCheck(String providerCode, String providerGroup, String recipientName, String purpose,
                        List<BillField> fields, Map<String, String> requisites, String billReference,
                        String recipientPhone) {

    public record BillField(String key, String label, String value, String display) {
    }
}
