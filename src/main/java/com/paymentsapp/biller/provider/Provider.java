package com.paymentsapp.biller.provider;

// Поставщик на витрине: code — для ссылок, title — заголовок формы, legalName — строка под ним, domain — откуда логотип
public record Provider(int id, String code, String groupCode, String name, String title, String legalName,
                       String domain, String color) {
}
