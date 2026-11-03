package com.paymentsapp.biller.catalog;

import java.util.List;

// Модель справочника переводов за рубеж
public final class Catalog {

    private Catalog() {
    }

    public record Country(String code, String nameRu, String nameNative, String dialCode, boolean popular,
                          List<String> currencies) {
    }

    // kindLabel — подпись для интерфейса: «Пополнение карты», «По номеру телефона»…
    public record Method(int id, String countryCode, String name, String kind, String kindLabel, String color,
                         String templateCode) {
    }

    public record CountryDetails(Country country, List<Method> methods) {
    }

    // Поле данных получателя: что спросить и как проверить
    public record Field(String key, String label, String type, String pattern, String placeholder) {
    }

    public record MethodDetails(Method method, List<String> currencies, List<Field> fields) {
    }

    static String kindLabel(String kind) {
        return switch (kind) {
            case "CARD" -> "Пополнение карты";
            case "PHONE" -> "По номеру телефона";
            case "ACCOUNT" -> "На банковский счёт";
            case "WALLET" -> "Электронный кошелёк";
            default -> kind;
        };
    }
}
