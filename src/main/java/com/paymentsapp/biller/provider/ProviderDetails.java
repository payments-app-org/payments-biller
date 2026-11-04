package com.paymentsapp.biller.provider;

import java.math.BigDecimal;
import java.util.List;

// Карточка поставщика: общие поля, поля формы и особенности его группы (заполнена только своя)
public record ProviderDetails(Provider provider, List<Field> fields, List<Nominal> nominals, GovDetails gov,
                              Transport transport, Game game, ForeignMobile foreignMobile, EMoney emoney,
                              Education education, Shop shop, Travel travel, Insurance insurance) {

    // stage: SEARCH — поле поиска счетов у поставщика («Найти»), PAY — поле оплаты.
    // prefill: USER_FULL_NAME / USER_PHONE — подставить данные клиента.
    public record Field(String key, String label, String type, boolean required, String placeholder, String hint,
                        String info, String pattern, String dependsOn, String stage, String prefill, List<Option> options) {
    }

    public record EMoney(boolean acceptsPhone, String accountPrefix) {
    }

    public record Education(String kind, boolean invoiceLookup) {
    }

    public record Shop(String salesModel) {
    }

    public record Travel(String kind, String registryNumber) {
    }

    public record Insurance(String kind, String license) {
    }

    public record Option(String value, String label, String countryCode) {
    }

    // Номинал с фиксированной ценой; region — значение поля, от которого он зависит
    public record Nominal(int id, String region, String name, BigDecimal price) {
    }

    public record Transport(String kind, String region) {
    }

    public record Game(String kind, String platform, String deliveryNote) {
    }

    public record ForeignMobile(String dialCode, int numberLength, List<String> prefixes, String operators, String currency) {
    }
}
