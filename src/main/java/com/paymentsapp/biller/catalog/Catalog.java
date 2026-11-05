package com.paymentsapp.biller.catalog;

import java.io.Serializable;
import java.util.List;

// Модель справочника переводов за рубеж. Serializable — справочник лежит в кэше Redis.
public final class Catalog {

    private Catalog() {
    }

    public record Country(String code, String nameRu, String nameNative, String dialCode, boolean popular,
                          List<String> currencies) implements Serializable {
    }

    // kindLabel — подпись для интерфейса: «Пополнение карты», «По номеру телефона»…
    public record Method(int id, String countryCode, String name, MethodKind kind, String kindLabel, String color,
                         String templateCode) implements Serializable {
    }

    public record CountryDetails(Country country, List<Method> methods) implements Serializable {
    }

    // Поле данных получателя: что спросить и как проверить
    public record Field(String key, String label, String type, String pattern, String placeholder) implements Serializable {
    }

    public record MethodDetails(Method method, List<String> currencies, List<Field> fields) implements Serializable {
    }

    // Вид способа перевода (transfer_methods.kind) и подпись для интерфейса
    public enum MethodKind {
        CARD("Пополнение карты"),
        PHONE("По номеру телефона"),
        ACCOUNT("На банковский счёт"),
        WALLET("Электронный кошелёк");

        private final String label;

        MethodKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
