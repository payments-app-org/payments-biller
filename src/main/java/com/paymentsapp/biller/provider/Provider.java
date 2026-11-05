package com.paymentsapp.biller.provider;

import java.math.BigDecimal;

// Поставщик на витрине: code — для ссылок, title — заголовок формы, legalName — строка под ним, domain — откуда логотип,
// countryCode — флаг вместо логотипа.
// minAmount / maxAmount — границы одного платежа у поставщика.
public record Provider(int id, String code, ProviderGroup groupCode, String name, String title, String legalName,
                       String domain, String color, String countryCode, BigDecimal minAmount, BigDecimal maxAmount) {
}
