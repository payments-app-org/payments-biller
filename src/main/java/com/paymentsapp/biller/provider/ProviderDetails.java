package com.paymentsapp.biller.provider;

import java.math.BigDecimal;
import java.util.List;

// Карточка поставщика: общие поля, поля формы и особенности его группы (заполнена только своя)
public record ProviderDetails(Provider provider, List<Field> fields, List<Nominal> nominals, GovDetails gov,
                              Transport transport, Game game, ForeignMobile foreignMobile) {

    public record Field(String key, String label, String type, boolean required, String placeholder, String hint,
                        String info, String pattern, String dependsOn, List<Option> options) {
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
