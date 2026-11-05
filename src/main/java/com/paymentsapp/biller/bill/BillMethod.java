package com.paymentsapp.biller.bill;

import com.paymentsapp.biller.provider.ProviderDetails;

// Способ проверки платежа поставщику. BillService берёт первый подходящий по @Order: новый способ — новый класс.
interface BillMethod {

    boolean supports(ProviderDetails provider, BillCheckRequest request);

    BillCheck check(ProviderDetails provider, BillCheckRequest request);
}
