package com.paymentsapp.biller.provider;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

// Карточка поставщика: общие поля, поля формы и особенности его группы (заполнена только своя)
public record ProviderDetails(Provider provider, List<Field> fields, List<Nominal> nominals, GovDetails gov,
                              Transport transport, Game game, ForeignMobile foreignMobile, EMoney emoney,
                              Education education, Shop shop, Travel travel, Insurance insurance,
                              Security security, Charity charity, Utility utility, Internet internet, Misc misc) implements Serializable {

    // stage: SEARCH — поле поиска счетов у поставщика («Найти»), PAY — поле оплаты, BOTH — нужно в обоих режимах.
    // prefill: USER_FULL_NAME / USER_PHONE — подставить данные клиента.
    public record Field(String key, String label, FieldType type, boolean required, String placeholder, String hint,
                        String info, String pattern, String dependsOn, FieldStage stage, Prefill prefill, List<Option> options)
            implements Serializable {

        public boolean searchable() {
            return stage != FieldStage.PAY;
        }

        public boolean payable() {
            return stage != FieldStage.SEARCH;
        }
    }

    public record EMoney(boolean acceptsPhone, String accountPrefix) implements Serializable {
    }

    public record Education(String kind, boolean invoiceLookup) implements Serializable {
    }

    public record Shop(String salesModel) implements Serializable {
    }

    public record Travel(String kind, String registryNumber) implements Serializable {
    }

    public record Insurance(String kind, String license) implements Serializable {
    }

    public record Security(String kind, String region) implements Serializable {
    }

    public record Charity(String inn, String cause) implements Serializable {
    }

    // directPay — у поставщика с поиском счетов можно и заплатить произвольную сумму («Найти / Оплатить»)
    public record Utility(String kind, boolean directPay, String intro) implements Serializable {
    }

    public record Internet(String kind) implements Serializable {
    }

    public record Misc(String owner) implements Serializable {
    }

    public record Option(String value, String label, String countryCode) implements Serializable {
    }

    // Номинал с фиксированной ценой; region — значение поля, от которого он зависит
    public record Nominal(int id, String region, String name, BigDecimal price) implements Serializable {
    }

    public record Transport(String kind, String region) implements Serializable {
    }

    public record Game(GameKind kind, String platform, String deliveryNote) implements Serializable {
    }

    public record ForeignMobile(String dialCode, int numberLength, List<String> prefixes, String operators, String currency) implements Serializable {
    }
}
